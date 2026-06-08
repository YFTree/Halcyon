package com.ella.music.data

import java.io.File

object LibraryNormalizer {
    fun cleanedTagText(value: String?): String {
        val text = value?.trim().orEmpty()
        if (text.isBlank() || isSystemUnknownPlaceholder(text) || text.looksLikeBrokenEncoding()) return ""
        return text
    }

    fun isUsableTagText(value: String?): Boolean =
        cleanedTagText(value).isNotBlank()

    fun isMissingTag(value: String?, fileName: String? = null): Boolean {
        val text = cleanedTagText(value)
        if (text.isBlank()) return true
        return fileName != null && text == fileName.substringBeforeLast('.')
    }

    fun isSystemUnknownPlaceholder(value: String?): Boolean =
        value?.trim()?.equals("<unknown>", ignoreCase = true) == true

    fun looksLikeLastFolderName(value: String, path: String): Boolean {
        val folderName = path.parentFolderName()
        return folderName.isNotBlank() && value.trim().equals(folderName, ignoreCase = true)
    }

    private fun String.looksLikeBrokenEncoding(): Boolean =
        '\uFFFD' in this || "锟斤拷" in this || Regex("""(?:锟|斤|拷){3,}""").containsMatchIn(this)

    private fun String.parentFolderName(): String =
        runCatching {
            if (isHttpAudioSource()) {
                java.net.URI(this).path.orEmpty().trim('/').substringBeforeLast('/', "")
                    .substringAfterLast('/')
            } else {
                File(this).parentFile?.name.orEmpty()
            }
        }
            .getOrDefault("")
            .trim()
}
