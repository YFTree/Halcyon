package com.ella.music.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import com.ella.music.data.SettingsManager
import com.ella.music.ui.components.TagEditorOptionIds
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * All DataStore-backed settings the player screen needs, collected through a single combined
 * flow instead of ~17 independent [collectAsState] subscriptions.
 *
 * PlayerScreen reads every one of these at the top of the same composable, so any single
 * setting change already recomposes the whole body — bundling them does not widen the
 * recomposition scope, it only collapses the burst of collectors (and their initial
 * emissions) that used to spin up each time the player surface entered composition.
 */
internal data class PlayerScreenSettings(
    val playerTapSeekEnabled: Boolean = true,
    val playerShowTotalDuration: Boolean = false,
    val lyricSourceMode: Int = SettingsManager.LYRIC_SOURCE_AUTO,
    val audioVisualizerEnabled: Boolean = false,
    val dynamicCoverEnabled: Boolean = false,
    val immersiveAlbumCover: Boolean = true,
    val playerBackgroundEnabled: Boolean = false,
    val playerBackgroundUri: String = "",
    val hiResLogoEnabled: Boolean = false,
    val hiResLogoUri: String = "",
    val lyricShareCustomInfo: String = "",
    val metadataEditorId: String = TagEditorOptionIds.ASK_EACH_TIME,
    val lyricTimingEditorId: String = TagEditorOptionIds.ASK_EACH_TIME,
    val sleepTimerCustomMinutes: Int = 45,
    val sleepTimerStopAfterCurrent: Boolean = false,
    val lyricPageKeepScreenOn: Boolean = false,
    val lyricPerspectiveEffect: Boolean = false
)

private data class PlayerSettingsGroupA(
    val playerTapSeekEnabled: Boolean,
    val playerShowTotalDuration: Boolean,
    val lyricSourceMode: Int,
    val audioVisualizerEnabled: Boolean,
    val dynamicCoverEnabled: Boolean
)

private data class PlayerSettingsGroupB(
    val immersiveAlbumCover: Boolean,
    val playerBackgroundEnabled: Boolean,
    val playerBackgroundUri: String,
    val hiResLogoEnabled: Boolean,
    val hiResLogoUri: String
)

private data class PlayerSettingsGroupC(
    val lyricShareCustomInfo: String,
    val metadataEditorId: String,
    val lyricTimingEditorId: String,
    val sleepTimerCustomMinutes: Int,
    val sleepTimerStopAfterCurrent: Boolean
)

private data class PlayerSettingsGroupD(
    val lyricPageKeepScreenOn: Boolean,
    val lyricPerspectiveEffect: Boolean
)

@Composable
internal fun rememberPlayerScreenSettings(settingsManager: SettingsManager): PlayerScreenSettings {
    val flow: Flow<PlayerScreenSettings> = remember(settingsManager) {
        val groupA = combine(
            settingsManager.playerTapSeekEnabled,
            settingsManager.playerShowTotalDuration,
            settingsManager.lyricSourceMode,
            settingsManager.audioVisualizerEnabled,
            settingsManager.dynamicCoverEnabled
        ) { tapSeek, showTotal, lyricSource, visualizer, dynamicCover ->
            PlayerSettingsGroupA(tapSeek, showTotal, lyricSource, visualizer, dynamicCover)
        }
        val groupB = combine(
            settingsManager.playerImmersiveCover,
            settingsManager.playerBackgroundEnabled,
            settingsManager.playerBackgroundUri,
            settingsManager.hiResLogoEnabled,
            settingsManager.hiResLogoUri
        ) { immersive, bgEnabled, bgUri, hiResEnabled, hiResUri ->
            PlayerSettingsGroupB(immersive, bgEnabled, bgUri, hiResEnabled, hiResUri)
        }
        val groupC = combine(
            settingsManager.lyricShareCustomInfo,
            settingsManager.metadataEditorId,
            settingsManager.lyricTimingEditorId,
            settingsManager.sleepTimerCustomMinutes,
            settingsManager.sleepTimerStopAfterCurrent
        ) { shareInfo, metadataId, timingId, customMinutes, stopAfterCurrent ->
            PlayerSettingsGroupC(shareInfo, metadataId, timingId, customMinutes, stopAfterCurrent)
        }
        val groupD = combine(
            settingsManager.lyricPageKeepScreenOn,
            settingsManager.lyricPerspectiveEffect
        ) { keepScreenOn, perspective ->
            PlayerSettingsGroupD(keepScreenOn, perspective)
        }
        combine(groupA, groupB, groupC, groupD) { a, b, c, d ->
            PlayerScreenSettings(
                playerTapSeekEnabled = a.playerTapSeekEnabled,
                playerShowTotalDuration = a.playerShowTotalDuration,
                lyricSourceMode = a.lyricSourceMode,
                audioVisualizerEnabled = a.audioVisualizerEnabled,
                dynamicCoverEnabled = a.dynamicCoverEnabled,
                immersiveAlbumCover = b.immersiveAlbumCover,
                playerBackgroundEnabled = b.playerBackgroundEnabled,
                playerBackgroundUri = b.playerBackgroundUri,
                hiResLogoEnabled = b.hiResLogoEnabled,
                hiResLogoUri = b.hiResLogoUri,
                lyricShareCustomInfo = c.lyricShareCustomInfo,
                metadataEditorId = c.metadataEditorId,
                lyricTimingEditorId = c.lyricTimingEditorId,
                sleepTimerCustomMinutes = c.sleepTimerCustomMinutes,
                sleepTimerStopAfterCurrent = c.sleepTimerStopAfterCurrent,
                lyricPageKeepScreenOn = d.lyricPageKeepScreenOn,
                lyricPerspectiveEffect = d.lyricPerspectiveEffect
            )
        }
    }
    val settings by flow.collectAsState(initial = PlayerScreenSettings())
    return settings
}
