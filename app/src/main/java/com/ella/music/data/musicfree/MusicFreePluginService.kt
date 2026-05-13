package com.ella.music.data.musicfree

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class MusicFreePluginService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun importPlugin(url: String): Pair<String, String> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url.trim())
            .header("User-Agent", USER_AGENT)
            .header("Cache-Control", "no-cache")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("导入失败: HTTP ${response.code}")
            importPluginScript(response.body?.string().orEmpty())
        }
    }

    fun importPluginScript(script: String): Pair<String, String> {
        if (script.length !in 50..9_000_000) error("插件脚本内容异常")
        val name = extractPlatform(script)
        if (name.isBlank()) error("没有识别到 MusicFree 插件 platform")
        val supportsMusic = "search" in script || "getMediaSource" in script || "importMusicItem" in script
        if (!supportsMusic) error("插件未声明搜索或播放能力")
        return name to script
    }

    private fun extractPlatform(script: String): String {
        val patterns = listOf(
            Regex("""platform\s*:\s*['"]([^'"]+)['"]"""),
            Regex("""platform\s*=\s*['"]([^'"]+)['"]"""),
            Regex("""['"]platform['"]\s*:\s*['"]([^'"]+)['"]""")
        )
        return patterns.firstNotNullOfOrNull { regex ->
            regex.find(script)?.groupValues?.getOrNull(1)
        }.orEmpty().trim()
    }

    companion object {
        private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 EllaMusic/1.0"
    }
}
