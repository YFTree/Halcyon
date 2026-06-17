package com.ella.music.player

import android.os.Bundle
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.ella.music.data.SettingsManager
import com.ella.music.data.model.LyricLine
import com.ella.music.data.model.Song
import com.ella.music.data.model.shiftedBy
import com.ella.music.data.repository.MusicRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.Locale

internal class OPlusLyricHandler(
    private val settingsManager: SettingsManager,
    private val musicRepository: MusicRepository,
    private val serviceScope: CoroutineScope,
    private val playerProvider: () -> Player?
) {
    companion object {
        private const val TAG = "PlaybackService"
        private const val TIMING_TAG = "EllaPlaybackTiming"
        const val OPLUS_LYRIC_INFO_KEY = "lyricInfo"
    }

    private var oplusLyricInfoJob: Job? = null
    private var oplusLyricInfoReapplyJob: Job? = null
    private var oplusLyricInfoRefreshJob: Job? = null
    private var oplusLyricInfoPendingSongKey: String? = null
    private var oplusLyricInfoSongKey: String? = null
    private var oplusLyricInfoJson: String? = null
    private val oplusLyricInfoPrefetchJobs = mutableMapOf<String, Job>()
    private val oplusLyricInfoCache = object : LinkedHashMap<String, String?>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String?>): Boolean = size > 24
    }
    @Volatile
    var colorOsLockScreenLyricEnabled = false

    fun refreshCurrentOplusLyricInfo(player: Player? = playerProvider()) {
        val currentPlayer = player ?: return
        val currentItem = currentPlayer.currentMediaItem
        val song = currentItem?.toSongFromMediaItemExtras()

        if (!colorOsLockScreenLyricEnabled) {
            clearCurrentOplusLyricInfo(currentPlayer)
            return
        }

        if (currentItem == null || song == null) {
            clearOplusLyricInfoState()
            return
        }

        val songKey = song.playbackStackKey()
        val existingLyricInfoJson = currentItem.oplusLyricInfoJsonFor(song)
        if (existingLyricInfoJson != null) {
            oplusLyricInfoCache[songKey] = existingLyricInfoJson
            oplusLyricInfoSongKey = songKey
            oplusLyricInfoJson = existingLyricInfoJson
            prefetchAdjacentOplusLyricInfo(currentPlayer)
            return
        }

        if (oplusLyricInfoSongKey == songKey) {
            applyCurrentOplusLyricInfo(currentPlayer, song, oplusLyricInfoJson)
            prefetchAdjacentOplusLyricInfo(currentPlayer)
            return
        }

        if (oplusLyricInfoCache.containsKey(songKey)) {
            val cachedJson = oplusLyricInfoCache[songKey]
            oplusLyricInfoSongKey = songKey
            oplusLyricInfoJson = cachedJson
            applyCurrentOplusLyricInfo(currentPlayer, song, cachedJson)
            prefetchAdjacentOplusLyricInfo(currentPlayer)
            return
        }
        if (oplusLyricInfoPendingSongKey == songKey) return

        oplusLyricInfoJob?.cancel()
        oplusLyricInfoPendingSongKey = songKey
        oplusLyricInfoJob = serviceScope.launch {
            try {
                val lyricInfoJson = runCatching {
                    loadOplusLyricInfoJson(song)
                }.getOrElse { error ->
                    Log.w(TAG, "Failed to prepare OPlus lyricInfo for ${song.title}", error)
                    null
                }

                val latestPlayer = playerProvider() ?: return@launch
                val latestSong = latestPlayer.currentMediaItem?.toSongFromMediaItemExtras()
                if (latestSong?.playbackStackKey() != songKey) return@launch

                oplusLyricInfoSongKey = songKey
                oplusLyricInfoJson = lyricInfoJson
                oplusLyricInfoCache[songKey] = lyricInfoJson
                applyCurrentOplusLyricInfo(latestPlayer, latestSong, lyricInfoJson)
                scheduleOplusLyricInfoReapply(songKey)
                prefetchAdjacentOplusLyricInfo(latestPlayer)
            } finally {
                if (oplusLyricInfoPendingSongKey == songKey) {
                    oplusLyricInfoPendingSongKey = null
                }
            }
        }
    }

    fun scheduleOplusLyricInfoRefreshBurst(player: Player? = playerProvider()) {
        if (!colorOsLockScreenLyricEnabled) {
            clearCurrentOplusLyricInfo(player)
            return
        }

        refreshCurrentOplusLyricInfo(player)
        oplusLyricInfoRefreshJob?.cancel()
        oplusLyricInfoRefreshJob = serviceScope.launch {
            for (delayMs in listOf(120L, 420L, 1_000L, 2_200L)) {
                delay(delayMs)
                refreshCurrentOplusLyricInfo()
            }
        }
    }

    fun clearCurrentOplusLyricInfo(player: Player? = playerProvider()) {
        oplusLyricInfoJob?.cancel()
        oplusLyricInfoReapplyJob?.cancel()
        oplusLyricInfoRefreshJob?.cancel()
        cancelOplusLyricInfoPrefetchJobs()
        oplusLyricInfoPendingSongKey = null
        oplusLyricInfoSongKey = null
        oplusLyricInfoJson = null

        val currentPlayer = player ?: return
        val song = currentPlayer.currentMediaItem?.toSongFromMediaItemExtras() ?: return
        applyCurrentOplusLyricInfo(currentPlayer, song, null)
    }

    private fun scheduleOplusLyricInfoReapply(songKey: String) {
        oplusLyricInfoReapplyJob?.cancel()
        if (oplusLyricInfoJson.isNullOrBlank()) return

        oplusLyricInfoReapplyJob = serviceScope.launch {
            for (delayMs in listOf(600L, 1_600L, 3_500L, 7_000L, 12_000L)) {
                delay(delayMs)
                if (!colorOsLockScreenLyricEnabled || oplusLyricInfoSongKey != songKey) return@launch
                val player = playerProvider() ?: return@launch
                val song = player.currentMediaItem?.toSongFromMediaItemExtras() ?: return@launch
                if (song.playbackStackKey() != songKey) return@launch
                applyCurrentOplusLyricInfo(player, song, oplusLyricInfoJson)
            }
        }
    }

    private fun clearOplusLyricInfoState() {
        oplusLyricInfoJob?.cancel()
        oplusLyricInfoReapplyJob?.cancel()
        oplusLyricInfoRefreshJob?.cancel()
        cancelOplusLyricInfoPrefetchJobs()
        oplusLyricInfoPendingSongKey = null
        oplusLyricInfoSongKey = null
        oplusLyricInfoJson = null
    }

    private fun cancelOplusLyricInfoPrefetchJobs() {
        oplusLyricInfoPrefetchJobs.values.forEach { it.cancel() }
        oplusLyricInfoPrefetchJobs.clear()
    }

    private fun prefetchAdjacentOplusLyricInfo(player: Player? = playerProvider()) {
        val currentPlayer = player ?: return
        if (!colorOsLockScreenLyricEnabled || currentPlayer.mediaItemCount < 2) return

        for (targetIndex in currentPlayer.oplusLyricPrefetchIndices()) {
            val targetItem = currentPlayer.getMediaItemAt(targetIndex)
            val targetSong = targetItem.toSongFromMediaItemExtras() ?: continue
            val targetSongKey = targetSong.playbackStackKey()

            if (targetItem.oplusLyricInfoJsonFor(targetSong) != null) continue
            if (oplusLyricInfoCache.containsKey(targetSongKey)) {
                oplusLyricInfoCache[targetSongKey]?.let { cachedJson ->
                    applyOplusLyricInfoToQueueItem(currentPlayer, targetIndex, targetSong, cachedJson)
                }
                continue
            }
            if (oplusLyricInfoPrefetchJobs.containsKey(targetSongKey)) continue

            lateinit var prefetchJob: Job
            prefetchJob = serviceScope.launch(start = CoroutineStart.LAZY) {
                try {
                    val lyricInfoJson = runCatching {
                        loadOplusLyricInfoJson(targetSong)
                    }.getOrElse { error ->
                        Log.w(TAG, "Failed to prefetch OPlus lyricInfo for ${targetSong.title}", error)
                        null
                    }
                    oplusLyricInfoCache[targetSongKey] = lyricInfoJson
                    if (lyricInfoJson.isNullOrBlank()) return@launch

                    val latestPlayer = playerProvider() ?: return@launch
                    if (targetIndex >= latestPlayer.mediaItemCount) return@launch
                    val latestItem = latestPlayer.getMediaItemAt(targetIndex)
                    if (!latestItem.matchesSong(targetSong)) return@launch
                    applyOplusLyricInfoToQueueItem(latestPlayer, targetIndex, targetSong, lyricInfoJson)
                } finally {
                    if (oplusLyricInfoPrefetchJobs[targetSongKey] === prefetchJob) {
                        oplusLyricInfoPrefetchJobs.remove(targetSongKey)
                    }
                }
            }
            oplusLyricInfoPrefetchJobs[targetSongKey] = prefetchJob
            prefetchJob.start()
        }
    }

    @OptIn(UnstableApi::class)
    private fun Player.oplusLyricPrefetchIndices(): List<Int> {
        val currentIndex = currentMediaItemIndex
        if (currentIndex == C.INDEX_UNSET || mediaItemCount <= 1) return emptyList()
        val previousIndex = when {
            currentIndex - 1 >= 0 -> currentIndex - 1
            repeatMode == Player.REPEAT_MODE_ALL -> mediaItemCount - 1
            else -> null
        }
        val nextIndex = when {
            currentIndex + 1 < mediaItemCount -> currentIndex + 1
            repeatMode == Player.REPEAT_MODE_ALL -> 0
            else -> null
        }
        return listOfNotNull(previousIndex, nextIndex)
            .filter { it != currentIndex }
            .distinct()
    }

    private fun applyCurrentOplusLyricInfo(player: Player, song: Song, lyricInfoJson: String?) {
        val index = player.currentMediaItemIndex
        val currentItem = player.currentMediaItem ?: return
        if (index == C.INDEX_UNSET || !currentItem.matchesSong(song)) return

        val extras = Bundle(currentItem.mediaMetadata.extras ?: Bundle.EMPTY)
        val currentJson = extras.getString(OPLUS_LYRIC_INFO_KEY)
        if (lyricInfoJson.isNullOrBlank()) {
            if (!extras.containsKey(OPLUS_LYRIC_INFO_KEY)) return
            extras.remove(OPLUS_LYRIC_INFO_KEY)
        } else {
            if (currentJson == lyricInfoJson) return
            extras.putString(OPLUS_LYRIC_INFO_KEY, lyricInfoJson)
        }

        val updatedMetadata = currentItem.mediaMetadata.buildUpon()
            .setExtras(extras)
            .build()
        val updatedItem = currentItem.buildUpon()
            .setMediaMetadata(updatedMetadata)
            .build()

        runCatching {
            player.replaceMediaItem(index, updatedItem)
            Log.d(TIMING_TAG, "OPlus lyricInfo metadata updated mediaId=${song.id} hasLyric=${!lyricInfoJson.isNullOrBlank()}")
        }.onFailure { error ->
            Log.w(TAG, "Failed to update OPlus lyricInfo metadata for ${song.title}", error)
        }
    }

    private fun applyOplusLyricInfoToQueueItem(player: Player, index: Int, song: Song, lyricInfoJson: String) {
        if (index < 0 || index >= player.mediaItemCount) return
        val mediaItem = player.getMediaItemAt(index)
        if (!mediaItem.matchesSong(song)) return
        val extras = Bundle(mediaItem.mediaMetadata.extras ?: Bundle.EMPTY)
        if (extras.getString(OPLUS_LYRIC_INFO_KEY) == lyricInfoJson) return
        extras.putString(OPLUS_LYRIC_INFO_KEY, lyricInfoJson)

        val updatedItem = mediaItem.buildUpon()
            .setMediaMetadata(
                mediaItem.mediaMetadata.buildUpon()
                    .setExtras(extras)
                    .build()
            )
            .build()

        runCatching {
            player.replaceMediaItem(index, updatedItem)
            Log.d(TIMING_TAG, "OPlus lyricInfo prefetched mediaId=${song.id} index=$index")
        }.onFailure { error ->
            Log.w(TAG, "Failed to prefetch OPlus lyricInfo metadata for ${song.title}", error)
        }
    }

    private fun MediaItem.matchesSong(song: Song): Boolean {
        val itemSong = toSongFromMediaItemExtras()
        if (itemSong != null) return itemSong.isSamePlaybackIdentity(song)
        if (song.path.isNotBlank() && localConfiguration?.uri?.toString().orEmpty() == song.path) return true
        if (song.id > 0L && mediaId == song.id.toString()) return true
        return localConfiguration?.uri?.toString().orEmpty() == song.path
    }

    private fun MediaItem.oplusLyricInfoJsonFor(song: Song): String? {
        val raw = mediaMetadata.extras
            ?.getString(OPLUS_LYRIC_INFO_KEY)
            ?.takeIf { it.isNotBlank() }
            ?: return null
        return raw.takeIf { it.matchesOplusLyricSong(song) }
    }

    private suspend fun loadOplusLyricInfoJson(song: Song): String? {
        val sourceMode = settingsManager.lyricSourceMode.first()
        val offsetMs = settingsManager.lyricOffsetOverrides.first()[song.oplusLyricOffsetKey()] ?: 0L
        return musicRepository.getLyrics(song, sourceMode)
            .shiftedBy(offsetMs)
            .toOplusLyricInfoJson(song)
    }

    private fun List<LyricLine>.toOplusLyricInfoJson(song: Song): String? {
        val lrc = toOplusLrc().takeIf { it.isNotBlank() } ?: return null
        return JSONObject()
            .put("songName", song.title)
            .put("artist", song.artist)
            .put("songId", song.oplusLyricSongId())
            .put("lyric", lrc)
            .toString()
    }

    private fun String.matchesOplusLyricSong(song: Song): Boolean {
        return runCatching {
            val json = JSONObject(this)
            val songId = json.optString("songId").takeIf { it.isNotBlank() }
            when {
                songId != null -> songId == song.oplusLyricSongId()
                else -> {
                    val songName = json.optString("songName")
                    val artist = json.optString("artist")
                    songName == song.title && artist == song.artist
                }
            }
        }.getOrDefault(false)
    }

    private fun List<LyricLine>.toOplusLrc(): String {
        return mapNotNull { line ->
            val primaryText = listOf(line.text, line.translation.orEmpty())
                .mapNotNull { it.toOplusLrcTextOrNull() }
                .distinct()
                .joinToString(" / ")
                .takeIf { it.isNotBlank() }
                ?: return@mapNotNull null
            line.timeMs.coerceAtLeast(0L) to primaryText
        }
            .sortedBy { it.first }
            .joinToString("\n") { (timeMs, text) ->
                "${timeMs.toOplusLrcTimestamp()}$text"
            }
    }

    private fun String.toOplusLrcTextOrNull(): String? {
        return lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString(" ")
            .stripInlineOplusLrcTimestamps()
            .takeIf { it.isNotBlank() }
    }

    private fun String.stripInlineOplusLrcTimestamps(): String {
        fun String.isOplusTimestampMarker(): Boolean {
            return matches(Regex("""\d{1,3}:\d{1,2}(?:[.:]\d{1,6})?"""))
        }

        return replace(Regex("""\[([^\]]+)]|<([^>]+)>""")) { match ->
            val marker = match.groupValues.getOrNull(1).orEmpty()
                .ifBlank { match.groupValues.getOrNull(2).orEmpty() }
                .trim()
                .replace(',', '.')
            if (marker.isOplusTimestampMarker()) "" else match.value
        }
            .replace(Regex("""[ \t\r\n]+"""), " ")
            .trim()
    }

    private fun Long.toOplusLrcTimestamp(): String {
        val safeMs = coerceAtLeast(0L)
        val minutes = safeMs / 60_000L
        val seconds = (safeMs % 60_000L) / 1_000L
        val centiseconds = (safeMs % 1_000L) / 10L
        return "[%02d:%02d.%02d]".format(Locale.US, minutes, seconds, centiseconds)
    }

    private fun Song.oplusLyricSongId(): String = when {
        onlineSource.isNotBlank() && onlineId.isNotBlank() -> "$onlineSource:$onlineId"
        id > 0L -> id.toString()
        path.isNotBlank() -> path
        else -> "$title|$artist|$album"
    }

    private fun Song.oplusLyricOffsetKey(): String {
        return when {
            onlineSource.isNotBlank() || onlineId.isNotBlank() -> "online:$onlineSource:$onlineId:$path"
            path.isNotBlank() -> "path:$path"
            else -> "id:$id"
        }
    }
}
