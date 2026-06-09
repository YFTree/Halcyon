package com.ella.music.data.parser

import android.os.ParcelFileDescriptor
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction

object LrcParser {

    data class LrcResult(
        val lyrics: List<com.ella.music.data.model.LyricLine>,
        val title: String? = null,
        val artist: String? = null,
        val album: String? = null,
        val offset: Long = 0L
    )

    fun parse(
        lrcContent: String,
        ignoreSplMetadataLines: Boolean = false
    ): LrcResult = EllaLyricsParser.parse(lrcContent, ignoreSplMetadataLines)

    private val lyricExtensions = listOf("lrc", "ttml", "elrc", "spl")

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
