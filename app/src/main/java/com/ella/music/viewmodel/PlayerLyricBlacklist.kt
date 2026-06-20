package com.ella.music.viewmodel

import com.ella.music.data.model.LyricLine
import com.ella.music.data.parser.EllaLyricsParser

internal class LyricBlacklistRule(rawRule: String) {
    private val raw = rawRule.trim()
    private val regex = runCatching { Regex(raw, RegexOption.IGNORE_CASE) }.getOrNull()

    fun matches(text: String?): Boolean {
        if (raw.isEmpty() || text.isNullOrBlank()) return false
        return regex?.containsMatchIn(text) ?: text.contains(raw, ignoreCase = true)
    }
}

internal fun List<LyricLine>.filterBlacklistedLyricLines(rules: List<LyricBlacklistRule>): List<LyricLine> {
    if (isEmpty()) return this
    return mapNotNull { line -> line.withoutBlacklistedParts(rules) }
}

internal fun LyricLine.withoutBlacklistedParts(rules: List<LyricBlacklistRule>): LyricLine? {
    fun blocked(text: String?): Boolean =
        text?.let {
            EllaLyricsParser.isIgnorableRawLyricLine(it) || EllaLyricsParser.isPlaceholderOnlyLine(it)
        } == true || rules.any { it.matches(text) }
    val textBlocked = blocked(text)
    val translationBlocked = blocked(translation)
    val pronunciationBlocked = blocked(pronunciation)
    val backgroundBlocked = blocked(backgroundText)
    val backgroundTranslationBlocked = blocked(backgroundTranslation)

    val remainingText = text.takeUnless { textBlocked }.orEmpty()
    val remainingTranslation = translation.takeUnless { translationBlocked }
    val remainingPronunciation = pronunciation.takeUnless { pronunciationBlocked }
    val remainingBackgroundText = backgroundText.takeUnless { backgroundBlocked }
    val remainingBackgroundTranslation = backgroundTranslation.takeUnless { backgroundTranslationBlocked }

    val promotedText = remainingText.ifBlank {
        remainingTranslation
            ?.takeIf { it.isNotBlank() }
            ?: remainingPronunciation?.takeIf { it.isNotBlank() }
            ?: remainingBackgroundText?.takeIf { it.isNotBlank() }
            ?: ""
    }
    val promotedFromTranslation = remainingText.isBlank() && promotedText == remainingTranslation
    val promotedFromPronunciation = remainingText.isBlank() && promotedText == remainingPronunciation
    val promotedFromBackground = remainingText.isBlank() && promotedText == remainingBackgroundText

    val filtered = copy(
        text = promotedText,
        words = if (textBlocked || promotedText != text) emptyList() else words,
        translation = remainingTranslation.takeUnless { promotedFromTranslation },
        pronunciation = remainingPronunciation.takeUnless { promotedFromPronunciation },
        pronunciationWords = if (pronunciationBlocked || promotedFromPronunciation) emptyList() else pronunciationWords,
        backgroundText = remainingBackgroundText.takeUnless { promotedFromBackground },
        backgroundWords = if (backgroundBlocked || promotedFromBackground) emptyList() else backgroundWords,
        backgroundTranslation = remainingBackgroundTranslation
    )
    return filtered.takeIf {
        it.text.isNotBlank() ||
            !it.translation.isNullOrBlank() ||
            !it.pronunciation.isNullOrBlank() ||
            !it.backgroundText.isNullOrBlank() ||
            !it.backgroundTranslation.isNullOrBlank()
    }
}

internal fun List<LyricLine>.preparedForDisplay(rules: List<LyricBlacklistRule>): List<LyricLine> =
    filterBlacklistedLyricLines(rules).withImplicitLineEndTimes()

internal fun List<LyricLine>.withImplicitLineEndTimes(): List<LyricLine> {
    if (isEmpty()) return this
    return mapIndexed { index, line ->
        val nextStartMs = getOrNull(index + 1)?.timeMs
        if (
            !line.isTtml &&
            line.endMs == null &&
            nextStartMs != null &&
            nextStartMs > line.timeMs &&
            line.words.isEmpty() &&
            line.backgroundWords.isEmpty()
        ) {
            line.copy(endMs = nextStartMs)
        } else {
            line
        }
    }
}
