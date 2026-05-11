package com.ella.music.player

import android.util.Log
import com.ella.music.data.model.LyricLine
import com.ella.music.data.model.Song
import com.hchen.superlyricapi.SuperLyricData
import com.hchen.superlyricapi.SuperLyricHelper
import com.hchen.superlyricapi.SuperLyricLine
import com.hchen.superlyricapi.SuperLyricWord

class SuperLyricBridge {
    private var enabled = false
    private var registered = false
    private var lastKey: String? = null
    private var lastSong: Song? = null

    fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
        lastKey = null
        if (enabled) {
            runCatching { register() }.onFailure {
                Log.w(TAG, "Failed to register SuperLyric", it)
                registered = false
            }
        } else {
            sendStop()
            unregister()
        }
    }

    fun isEnabled(): Boolean = enabled

    fun sendSong(song: Song) {
        if (!enabled) return
        lastSong = song
        register()
    }

    fun sendLyric(line: LyricLine?, positionMs: Long, showTranslation: Boolean) {
        if (!enabled || line == null) return
        val song = lastSong
        val key = "${song?.id}:${line.timeMs}:${positionMs / 80}"
        if (key == lastKey) return
        lastKey = key
        runCatching {
            register()
            val end = line.endMs ?: (line.words.maxOfOrNull { it.endMs } ?: (line.timeMs + 3000L))
            SuperLyricHelper.sendLyric(
                SuperLyricData()
                    .setTitle(song?.title)
                    .setArtist(song?.artist)
                    .setAlbum(song?.album)
                    .setLyric(SuperLyricLine(line.text.ifBlank { line.backgroundText.orEmpty() }, line.words.toSuperWords(), line.timeMs, end))
                    .setSecondary(line.backgroundText?.takeIf { it.isNotBlank() }?.let {
                        SuperLyricLine(it, line.backgroundWords.toSuperWords(), line.timeMs, end)
                    })
                    .setTranslation(
                        if (showTranslation) {
                            (line.translation ?: line.backgroundTranslation)?.takeIf { it.isNotBlank() }?.let {
                                SuperLyricLine(it, line.timeMs, end)
                            }
                        } else {
                            null
                        }
                    )
            )
        }.onFailure {
            Log.w(TAG, "Failed to send SuperLyric", it)
            registered = false
        }
    }

    fun sendStop() {
        if (!registered) return
        runCatching {
            SuperLyricHelper.sendStop(
                SuperLyricData()
                    .setTitle(lastSong?.title)
                    .setArtist(lastSong?.artist)
                    .setAlbum(lastSong?.album)
            )
        }
        lastKey = null
    }

    fun destroy() {
        sendStop()
        unregister()
        lastSong = null
        lastKey = null
    }

    private fun register() {
        if (registered || !SuperLyricHelper.isAvailable()) return
        SuperLyricHelper.registerPublisher()
        SuperLyricHelper.setSystemPlayStateListenerEnabled(true)
        registered = true
    }

    private fun unregister() {
        if (!registered) return
        runCatching { SuperLyricHelper.unregisterPublisher() }
        registered = false
    }

    private fun List<com.ella.music.data.model.LyricWord>.toSuperWords(): Array<SuperLyricWord>? {
        if (isEmpty()) return null
        return map { SuperLyricWord(it.text, it.startMs, it.endMs) }.toTypedArray()
    }

    private companion object {
        const val TAG = "SuperLyricBridge"
    }
}
