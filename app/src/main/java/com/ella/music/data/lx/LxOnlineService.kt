package com.ella.music.data.lx

import com.ella.music.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class LxOnlineSong(
    val song: Song,
    val source: String,
    val songmid: String,
    val quality: String
)

class LxOnlineService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun importSource(url: String): Pair<String, String> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url.trim())
            .header("User-Agent", USER_AGENT)
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("导入失败: HTTP ${response.code}")
            val script = response.body?.string().orEmpty()
            importSourceScript(script)
        }
    }

    fun importSourceScript(script: String): Pair<String, String> {
        if (script.length !in 50..2_000_000) error("源脚本内容异常")
        if (!script.contains("lx.") && !script.contains("lx_")) error("这不像落雪 Music API 脚本")
        return extractSourceName(script) to script
    }

    suspend fun search(keyword: String, page: Int = 1): List<LxOnlineSong> = withContext(Dispatchers.IO) {
        val encoded = URLEncoder.encode(keyword.trim(), "UTF-8")
        val url = "http://search.kuwo.cn/r.s?client=kt&all=$encoded&pn=${(page - 1).coerceAtLeast(0)}&rn=30" +
            "&uid=794762570&ver=kwplayer_ar_9.2.2.1&vipver=1&show_copyright_off=1&newver=1" +
            "&ft=music&cluster=0&strategy=2012&encoding=utf8&rformat=json&vermerge=1&mobi=1&issubtitle=1"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("搜索失败: HTTP ${response.code}")
            val body = response.body?.string().orEmpty()
            val root = JSONObject(body)
            val list = root.optJSONArray("abslist") ?: return@withContext emptyList()
            List(list.length()) { index ->
                val item = list.getJSONObject(index)
                val mid = item.optString("MUSICRID").removePrefix("MUSIC_").ifBlank { item.optString("DC_TARGETID") }
                val title = decodeHtml(item.optString("SONGNAME"))
                val artist = decodeHtml(item.optString("ARTIST")).replace("&", "、")
                val album = decodeHtml(item.optString("ALBUM")).ifBlank { "在线音乐" }
                val durationMs = item.optLong("DURATION", 0L) * 1000L
                val id = "lx_kw_$mid".hashCode().toLong()
                LxOnlineSong(
                    song = Song(
                        id = id,
                        title = title,
                        artist = artist,
                        album = album,
                        albumId = 0L,
                        duration = durationMs,
                        path = "",
                        fileName = "$title-$artist.mp3",
                        mimeType = "audio/mpeg"
                    ),
                    source = "kw",
                    songmid = mid,
                    quality = pickQuality(item.optString("N_MINFO"))
                )
            }.filter { it.songmid.isNotBlank() && it.song.title.isNotBlank() }
        }
    }

    suspend fun resolvePlayableSong(item: LxOnlineSong): Song = withContext(Dispatchers.IO) {
        val format = when (item.quality) {
            "flac", "flac24bit" -> "flac"
            else -> "mp3"
        }
        val url = "http://antiserver.kuwo.cn/anti.s?type=convert_url&rid=MUSIC_${item.songmid}&format=$format&response=url"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .build()
        val playableUrl = client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("解析播放地址失败: HTTP ${response.code}")
            response.body?.string()?.trim().orEmpty()
        }
        if (!playableUrl.startsWith("http")) error("解析播放地址失败")
        item.song.copy(path = playableUrl, fileName = "${item.song.title}.${if (format == "flac") "flac" else "mp3"}")
    }

    private fun extractSourceName(script: String): String {
        val currentInfoName = Regex("""name\s*:\s*['"]([^'"]+)['"]""").find(script)?.groupValues?.getOrNull(1)
        val commentName = Regex("""@name\s+(.+)""").find(script)?.groupValues?.getOrNull(1)?.trim()
        return currentInfoName ?: commentName ?: "落雪源"
    }

    private fun pickQuality(raw: String): String {
        return when {
            "bitrate:4000" in raw -> "flac24bit"
            "bitrate:2000" in raw -> "flac"
            "bitrate:320" in raw -> "320k"
            else -> "128k"
        }
    }

    private fun decodeHtml(value: String): String = value
        .replace("&amp;", "&")
        .replace("&nbsp;", " ")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .trim()

    companion object {
        private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 EllaMusic/1.0"
    }
}
