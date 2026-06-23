package com.ella.music.data.parser

import android.os.ParcelFileDescriptor
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction

object LrcParser {

    /** Lyric parser engine selector — switchable at runtime from UI. */
    const val PARSER_ENGINE_AUTO = 0   // AccompanistLyricsParser first, then EllaLyricsParser fallback
    const val PARSER_ENGINE_ELLA = 1   // EllaLyricsParser only (project's own parser)

    @Volatile
    var parserEngine: Int = PARSER_ENGINE_ELLA

    data class LrcResult(
        val lyrics: List<com.ella.music.data.model.LyricLine>,
        val title: String? = null,
        val artist: String? = null,
        val album: String? = null,
        val offset: Long = 0L
    )

    fun parse(lrcContent: String, ignoreHeaderTags: Boolean = false): LrcResult {
        val raw = when (parserEngine) {
            PARSER_ENGINE_ELLA -> EllaLyricsParser.parse(lrcContent, ignoreHeaderTags)
            else -> AccompanistLyricsParser.parse(lrcContent)
                ?: EllaLyricsParser.parse(lrcContent, ignoreHeaderTags)
        }
        // Post-process: fix x-bg/accompaniment text that appears as a concatenated
        // Latin blob (e.g. "Andthere'salotofcoolchicksoutthere"). This runs for BOTH
        // parser engines so behavior is consistent regardless of which engine is active.
        val fixedLyrics = raw.lyrics.map(::fixBackgroundLineSpacing)
        return raw.copy(lyrics = fixedLyrics)
    }

    /**
     * Fixes background/accompaniment line text that appears as a concatenated Latin blob.
     *
     * Two strategies:
     * 1. If the line has [backgroundWords] with proper per-word text, rebuild the display
     *    text by joining the words' text with spaces.
     * 2. If there are no usable words, attempt to split the concatenated text using
     *    case boundaries (camelCase) and apostrophes.
     */
    private fun fixBackgroundLineSpacing(line: com.ella.music.data.model.LyricLine): com.ella.music.data.model.LyricLine {
        val bgText = line.backgroundText ?: return line
        if (bgText.isBlank()) return line
        // CJK text doesn't use inter-word spaces — skip
        if (bgText.any { isCjk(it) }) return line
        // Already well-spaced (at least 1 space per ~10 letters)
        val spaceCount = bgText.count { it == ' ' }
        val letterCount = bgText.count { it.isLetter() }
        if (letterCount < 8 || (spaceCount > 0 && letterCount.toFloat() / spaceCount < 9f)) return line

        // Strategy 1: rebuild from words if they have proper per-word text
        val bgWords = line.backgroundWords
        if (bgWords.isNotEmpty()) {
            val wordTexts = bgWords.map { it.text.trim() }.filter { it.isNotBlank() }
            if (wordTexts.size > 1) {
                val rebuilt = wordTexts.joinToString(" ")
                if (rebuilt != bgText.trim()) {
                    return line.copy(backgroundText = rebuilt)
                }
            }
        }

        // Strategy 2: split concatenated text using case boundaries + apostrophes
        val splitWords = splitConcatenatedLatin(bgText)
        if (splitWords.size <= 1) return line

        // Rebuild display text
        val fixedText = splitWords.joinToString(" ")

        // Rebuild per-word timing if we have timing info
        val bgStart = line.backgroundStartMs ?: line.backgroundWords.minOfOrNull { it.startMs } ?: line.timeMs
        val bgEnd = line.backgroundEndMs ?: line.backgroundWords.maxOfOrNull { it.endMs }
            ?: line.endMs ?: (bgStart + 3000L)
        val totalDuration = (bgEnd - bgStart).coerceAtLeast(splitWords.size * 150L)
        val weights = splitWords.map { it.filter { c -> c.isLetterOrDigit() }.length.toFloat().coerceAtLeast(1f) }
        val totalWeight = weights.sum().coerceAtLeast(1f)
        var cursor = bgStart
        val fixedWords = splitWords.mapIndexed { idx, word ->
            val weight = weights[idx]
            val duration = (totalDuration * weight / totalWeight).toLong().coerceAtLeast(150L)
            val start = cursor
            val end = cursor + duration
            cursor = end
            com.ella.music.data.model.LyricWord(text = word, startMs = start, endMs = end)
        }

        return line.copy(backgroundText = fixedText, backgroundWords = fixedWords)
    }

    /**
     * Splits concatenated Latin text like "Andthere'salotofcoolchicksoutthere" into
     * individual words using case boundaries, apostrophes, and common English short-word
     * patterns. Returns the original text as a single-element list if splitting fails.
     */
    private fun splitConcatenatedLatin(text: String): List<String> {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return emptyList()

        // Step 1: Split at lowercase→Uppercase boundaries (camelCase)
        // e.g. "coolChicks" → "cool" "Chicks"
        val caseSplit = StringBuilder()
        for (i in trimmed.indices) {
            val ch = trimmed[i]
            if (i > 0 && ch.isUpperCase() && trimmed[i - 1].isLowerCase()) {
                caseSplit.append(' ')
            }
            caseSplit.append(ch)
        }
        val caseSplitText = caseSplit.toString()

        // Step 2: Split at apostrophe boundaries
        // e.g. "there'salot" → "there's" "a" "lot"
        val apostropheSplit = caseSplitText
            .replace(Regex("""([a-z]'[a-z]+)([A-Za-z])"""), "$1 $2")
            .replace(Regex("""([a-z])'(a|s|t|ll|re|ve|d|m)\b"""), "$1'$2")

        // Step 3: If we already have multiple words, try to further split long runs
        // using common English short words as boundaries
        var words = apostropheSplit.split(Regex("""\s+""")).filter { it.isNotBlank() }.toMutableList()

        // Only proceed with short-word splitting if we have very long unsplit runs
        val hasLongRun = words.any { it.length > 10 }
        if (hasLongRun) {
            words = splitByCommonShortWords(words).toMutableList()
        }

        return words
    }

    /** Common English short words used as splitting boundaries for concatenated text. */
    private val commonShortWords = setOf(
        "a", "an", "the", "and", "or", "but", "if", "of", "to", "in", "on", "at",
        "is", "it", "as", "be", "by", "do", "go", "he", "me", "my", "no", "so",
        "up", "us", "we", "am", "ax", "ex", "oh", "ok",
        "lot", "out", "not", "all", "can", "get", "got", "has", "her", "him",
        "his", "how", "let", "now", "one", "our", "she", "say", "see", "too",
        "was", "way", "who", "why", "yes", "you", "your", "they", "them",
        "are", "did", "for", "had", "has", "have", "here", "just", "know",
        "want", "what", "when", "will", "with", "this", "that", "from",
        "there", "where", "which", "been", "were", "some", "more", "only",
        "back", "come", "could", "would", "should", "than", "then", "them",
        "also", "into", "over", "after", "never", "every", "make", "like",
        "take", "give", "call", "feel", "keep", "tell", "find", "turn",
        "look", "good", "well", "even", "still", "down", "about", "around"
    )

    /**
     * Attempts to split long runs of concatenated text by matching common English
     * short words at word boundaries. e.g. "alotofcoolchicksoutthere" →
     * "a" "lot" "of" "cool" "chicks" "out" "there"
     */
    private fun splitByCommonShortWords(words: List<String>): List<String> {
        val result = mutableListOf<String>()
        for (word in words) {
            if (word.length <= 10) {
                result.add(word)
                continue
            }
            val split = trySplitLongRun(word)
            if (split.size > 1) result.addAll(split) else result.add(word)
        }
        return result
    }

    private fun trySplitLongRun(text: String): List<String> {
        // Greedy approach: scan left-to-right, try to match known short words,
        // then treat remaining text as longer words.
        val result = mutableListOf<String>()
        var i = 0
        val lower = text.lowercase()
        while (i < text.length) {
            var matched = false
            // Try to match a known short word (longest first for better accuracy)
            val maxWordLen = minOf(7, text.length - i)
            for (len in maxWordLen downTo 2) {
                val sub = lower.substring(i, i + len)
                if (sub in commonShortWords) {
                    result.add(text.substring(i, i + len))
                    i += len
                    matched = true
                    break
                }
            }
            if (!matched) {
                // No known short word matches at this position.
                // Accumulate characters until we can match a known word ahead.
                var j = i + 1
                while (j < text.length) {
                    val remaining = lower.substring(j)
                    val hasKnownWordAhead = (2..minOf(7, remaining.length)).any { len ->
                        remaining.substring(0, len) in commonShortWords
                    }
                    if (hasKnownWordAhead) break
                    j++
                }
                result.add(text.substring(i, j))
                i = j
            }
        }
        return result
    }

    private fun isCjk(ch: Char): Boolean {
        val block = Character.UnicodeBlock.of(ch)
        return block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS ||
            block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A ||
            block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B ||
            block == Character.UnicodeBlock.HIRAGANA ||
            block == Character.UnicodeBlock.KATAKANA ||
            block == Character.UnicodeBlock.HANGUL_SYLLABLES
    }

    private val lyricExtensions = listOf("lrc", "ttml", "elrc")

    fun findLrcFile(songPath: String): String? {
        val baseName = songPath.substringBeforeLast('.')
        for (ext in lyricExtensions) {
            readViaFd("$baseName.$ext")?.let { return it }
        }

        val parentDir = File(songPath).parentFile ?: return null
        val songName = File(songPath).nameWithoutExtension
        return try {
            parentDir.listFiles()
                ?.filter { file -> file.extension.lowercase() in lyricExtensions }
                ?.sortedWith(
                    compareBy<File> { lyricExtensions.indexOf(it.extension.lowercase()) }
                        .thenBy { it.name }
                )
                ?.firstNotNullOfOrNull { file ->
                    file.takeIf { it.nameWithoutExtension.contains(songName, ignoreCase = true) }
                        ?.let { readViaFd(it.absolutePath) }
                }
        } catch (_: Exception) {
            null
        }
    }

    private fun readViaFd(path: String): String? {
        return try {
            val file = File(path)
            if (!file.exists()) return null
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                FileInputStream(pfd.fileDescriptor).use { fis ->
                    val bytes = fis.readBytes()
                    if (bytes.isEmpty()) return null
                    readTextWithFallback(bytes)
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun readTextWithFallback(bytes: ByteArray): String {
        val charsets = listOf("UTF-8", "GB18030", "UTF-16LE", "UTF-16BE")
        for (charsetName in charsets) {
            val charset = Charset.forName(charsetName)
            try {
                val decoder = charset.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                return decoder.decode(ByteBuffer.wrap(bytes)).toString()
            } catch (_: CharacterCodingException) {
            }
        }
        return String(bytes, Charsets.UTF_8)
    }
}
