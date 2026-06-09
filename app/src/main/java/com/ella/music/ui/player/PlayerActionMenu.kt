package com.ella.music.ui.player

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ella.music.R
import com.ella.music.data.model.Song
import com.ella.music.ui.components.TagEditorOptionKind
import com.ella.music.ui.components.buildTagEditorOptions
import com.ella.music.ui.components.launchTagEditorOption

@Composable
internal fun PlayerActionMenu(
    song: Song?,
    speed: Float,
    pitch: Float,
    visualizerEnabled: Boolean,
    visualizerAvailable: Boolean,
    metadataEditorId: String,
    lyricTimingEditorId: String,
    sleepTimerEndRealtimeMs: Long?,
    stopAfterCurrentEnabled: Boolean,
    sleepTimerCustomMinutes: Int,
    sleepTimerStopAfterCurrent: Boolean,
    onClose: () -> Unit,
    onAlbum: () -> Unit,
    onArtist: () -> Unit,
    onDownload: () -> Unit,
    onLandscape: () -> Unit,
    onSongInfo: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onAddToQueue: () -> Unit,
    onPlayNext: () -> Unit,
    onShare: () -> Unit,
    onSetRating: () -> Unit,
    onAiInterpret: () -> Unit,
    onSpectrum: () -> Unit,
    onDeleteSong: () -> Unit,
    onMatchDynamicCover: () -> Unit,
    onStopAfterCurrent: (Boolean) -> Unit,
    onTimer: (Int) -> Unit,
    onCustomTimerMinutes: (Int) -> Unit,
    onCancelTimer: () -> Unit,
    onSpeed: (Float) -> Unit,
    onPitch: (Float) -> Unit,
    onVisualizerEnabled: (Boolean) -> Unit,
    initialPage: PlayerActionSheetPage = PlayerActionSheetPage.Main,
    modifier: Modifier = Modifier
) {
    var page by remember(initialPage) { mutableStateOf(initialPage) }
    val context = LocalContext.current
    val artistEntryLabel = remember(song?.artist) {
        context.getString(
            R.string.player_view_artist_named,
            song?.artist?.ifBlank { context.getString(R.string.player_unknown_artist) }
                ?: context.getString(R.string.player_unknown_artist)
        )
    }
    val albumEntryLabel = remember(song?.album) {
        context.getString(
            R.string.player_view_album_named,
            song?.album?.ifBlank { context.getString(R.string.player_unknown_album) }
                ?: context.getString(R.string.player_unknown_album)
        )
    }
    val metadataOptions = remember(song?.id, song?.path, song?.mimeType) {
        song?.let { buildTagEditorOptions(context, it) }
            .orEmpty()
            .filter { it.kind == TagEditorOptionKind.Metadata }
    }
    val lyricTimingOptions = remember(song?.id, song?.path, song?.mimeType) {
        song?.let { buildTagEditorOptions(context, it) }
            .orEmpty()
            .filter { it.kind == TagEditorOptionKind.LyricTiming }
    }

    fun openEditorPage(kind: TagEditorOptionKind, preferredId: String) {
        val options = if (kind == TagEditorOptionKind.Metadata) metadataOptions else lyricTimingOptions
        val preferredOption = preferredId
            .takeIf { it.isNotBlank() }
            ?.let { id -> options.firstOrNull { it.id == id } }
        if (preferredOption != null) {
            launchTagEditorOption(context, preferredOption)
            onClose()
        } else {
            page = if (kind == TagEditorOptionKind.Metadata) {
                PlayerActionSheetPage.MetadataEditor
            } else {
                PlayerActionSheetPage.LyricTimingEditor
            }
        }
    }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 10.dp)
    ) {
        when (page) {
            PlayerActionSheetPage.Main -> {
                PlayerActionMenuItem(stringResource(R.string.player_landscape_lyrics), onLandscape)
                PlayerActionMenuItem(albumEntryLabel, onAlbum)
                PlayerActionMenuItem(artistEntryLabel, onArtist)
                PlayerActionMenuItem(stringResource(R.string.player_add_to_playlist), onAddToPlaylist)
                PlayerActionMenuItem(stringResource(R.string.common_add_to_queue), onAddToQueue)
                PlayerActionMenuItem(stringResource(R.string.song_more_play_next), onPlayNext)
                PlayerActionMenuItem(stringResource(R.string.common_share), onShare)
                PlayerActionMenuItem(stringResource(R.string.song_more_view_spectrum), onSpectrum)
                PlayerActionMenuItem(stringResource(R.string.song_more_ai_title), onAiInterpret)
                PlayerActionMenuItem(stringResource(R.string.player_song_info), onSongInfo)
                PlayerActionMenuItem(stringResource(R.string.song_more_set_rating), onSetRating)
                PlayerActionMenuItem(stringResource(R.string.player_match_dynamic_cover), onMatchDynamicCover)
                PlayerActionMenuItem(stringResource(R.string.player_edit_metadata), { openEditorPage(TagEditorOptionKind.Metadata, metadataEditorId) })
                PlayerActionMenuItem(stringResource(R.string.player_lyric_timing), { openEditorPage(TagEditorOptionKind.LyricTiming, lyricTimingEditorId) })
                if (song?.onlineSource == "kw" && song.path.startsWith("http")) {
                    PlayerActionMenuItem(stringResource(R.string.player_download_lx_song), onDownload)
                }
                PlayerActionMenuItem(stringResource(R.string.player_sleep_timer), { page = PlayerActionSheetPage.Timer })
                PlayerActionMenuItem(stringResource(R.string.player_speed_pitch), { page = PlayerActionSheetPage.Speed })
                if (visualizerAvailable) {
                    PlayerActionMenuItem(stringResource(R.string.player_visualizer_settings), { page = PlayerActionSheetPage.Visualizer })
                }
                if (song != null && !song.path.startsWith("http://", ignoreCase = true) && !song.path.startsWith("https://", ignoreCase = true)) {
                    PlayerActionMenuItem(stringResource(R.string.song_more_delete_permanently), onDeleteSong, danger = true)
                }
            }
            PlayerActionSheetPage.Timer -> {
                TimerSheetContent(
                    onBack = { page = PlayerActionSheetPage.Main },
                    sleepTimerEndRealtimeMs = sleepTimerEndRealtimeMs,
                    stopAfterCurrentEnabled = stopAfterCurrentEnabled,
                    sleepTimerCustomMinutes = sleepTimerCustomMinutes,
                    sleepTimerStopAfterCurrent = sleepTimerStopAfterCurrent,
                    onStopAfterCurrent = onStopAfterCurrent,
                    onTimer = onTimer,
                    onCustomTimerMinutes = onCustomTimerMinutes,
                    onCancelTimer = onCancelTimer
                )
            }
            PlayerActionSheetPage.Speed -> {
                SpeedPitchSheetContent(
                    speed = speed,
                    pitch = pitch,
                    onBack = { page = PlayerActionSheetPage.Main },
                    onSpeed = onSpeed,
                    onPitch = onPitch
                )
            }
            PlayerActionSheetPage.Visualizer -> {
                VisualizerSheetContent(
                    enabled = visualizerEnabled,
                    onBack = { page = PlayerActionSheetPage.Main },
                    onEnabledChange = onVisualizerEnabled
                )
            }
            PlayerActionSheetPage.MetadataEditor -> {
                TagEditorSheetContent(
                    song = song,
                    title = stringResource(R.string.player_choose_metadata_editor),
                    kind = TagEditorOptionKind.Metadata,
                    onBack = { page = PlayerActionSheetPage.Main },
                    onClose = onClose
                )
            }
            PlayerActionSheetPage.LyricTimingEditor -> {
                TagEditorSheetContent(
                    song = song,
                    title = stringResource(R.string.player_choose_lyric_timing_editor),
                    kind = TagEditorOptionKind.LyricTiming,
                    onBack = { page = PlayerActionSheetPage.Main },
                    onClose = onClose
                )
            }
        }
    }
}

internal enum class PlayerActionSheetPage {
    Main,
    Timer,
    Speed,
    Visualizer,
    MetadataEditor,
    LyricTimingEditor
}
