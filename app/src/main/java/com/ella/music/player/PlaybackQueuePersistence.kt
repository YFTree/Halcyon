package com.ella.music.player

import androidx.media3.common.Player
import com.ella.music.data.model.Song
import org.json.JSONArray
import org.json.JSONObject

internal data class SavedQueue(
    val songs: List<Song>,
    val index: Int,
    val positionMs: Long,
    val repeatMode: Int,
    val shuffle: Boolean,
    val speed: Float,
    val pitch: Float
)

internal data class PlaybackStateSnapshot(
    val index: Int,
    val positionMs: Long,
    val repeatMode: Int,
    val shuffle: Boolean,
    val speed: Float,
    val pitch: Float
) {
    fun toJson(): JSONObject = JSONObject()
        .put("index", index)
        .put("positionMs", positionMs)
        .put("repeatMode", repeatMode)
        .put("shuffle", shuffle)
        .put("speed", speed)
        .put("pitch", pitch)
}

internal data class PendingPlaylist(
    val songs: List<Song>,
    val startIndex: Int
)

internal fun playbackQueueJson(snapshot: PlaybackStateSnapshot, songs: List<Song>): JSONObject =
    JSONObject()
        .put("index", snapshot.index)
        .put("positionMs", snapshot.positionMs)
        .put("repeatMode", snapshot.repeatMode)
        .put("shuffle", snapshot.shuffle)
        .put("speed", snapshot.speed)
        .put("pitch", snapshot.pitch)
        .put("songs", JSONArray().apply {
            songs.forEach { song -> put(song.toPlaybackQueueJson()) }
        })

internal fun parseSavedQueue(rawQueue: String, rawState: String?): SavedQueue? =
    runCatching {
        val payload = JSONObject(rawQueue)
        val songsArray = payload.optJSONArray("songs") ?: JSONArray()
        val songs = (0 until songsArray.length())
            .mapNotNull { songsArray.optJSONObject(it)?.toPlaybackQueueSongOrNull() }
        val state = rawState?.let { runCatching { JSONObject(it) }.getOrNull() }
        SavedQueue(
            songs = songs,
            index = state?.optInt("index", 0) ?: payload.optInt("index", 0),
            positionMs = state?.optLong("positionMs", 0L) ?: payload.optLong("positionMs", 0L),
            repeatMode = state?.optInt("repeatMode", Player.REPEAT_MODE_OFF)
                ?: payload.optInt("repeatMode", Player.REPEAT_MODE_OFF),
            shuffle = state?.optBoolean("shuffle", false) ?: payload.optBoolean("shuffle", false),
            speed = (state?.optDouble("speed", 1.0) ?: payload.optDouble("speed", 1.0)).toFloat(),
            pitch = (state?.optDouble("pitch", 1.0) ?: payload.optDouble("pitch", 1.0)).toFloat()
        )
    }.getOrNull()

internal fun Song.toPlaybackQueueJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("title", title)
    .put("artist", artist)
    .put("album", album)
    .put("albumId", albumId)
    .put("duration", duration)
    .put("path", path)
    .put("fileName", fileName)
    .put("fileSize", fileSize)
    .put("mimeType", mimeType)
    .put("dateAdded", dateAdded)
    .put("dateModified", dateModified)
    .put("trackNumber", trackNumber)
    .put("discNumber", discNumber)
    .put("albumArtist", albumArtist)
    .put("genre", genre)
    .put("year", year)
    .put("composer", composer)
    .put("lyricist", lyricist)
    .put("coverUrl", coverUrl)
    .put("onlineSource", onlineSource)
    .put("onlineId", onlineId)

internal fun JSONObject.toPlaybackQueueSongOrNull(): Song? {
    val path = optString("path").takeIf { it.isNotBlank() } ?: return null
    return Song(
        id = optLong("id", path.hashCode().toLong()),
        title = optString("title").ifBlank { optString("fileName").ifBlank { path.substringAfterLast('/') } },
        artist = optString("artist").ifBlank { "Unknown" },
        album = optString("album").ifBlank { "Music" },
        albumId = optLong("albumId", 0L),
        duration = optLong("duration", 0L),
        path = path,
        fileName = optString("fileName").ifBlank { path.substringAfterLast('/') },
        fileSize = optLong("fileSize", 0L),
        mimeType = optString("mimeType"),
        dateAdded = optLong("dateAdded", 0L),
        dateModified = optLong("dateModified", 0L),
        trackNumber = optInt("trackNumber", 0),
        discNumber = optInt("discNumber", 0),
        albumArtist = optString("albumArtist"),
        genre = optString("genre"),
        year = optString("year"),
        composer = optString("composer"),
        lyricist = optString("lyricist"),
        coverUrl = optString("coverUrl"),
        onlineSource = optString("onlineSource"),
        onlineId = optString("onlineId")
    )
}
