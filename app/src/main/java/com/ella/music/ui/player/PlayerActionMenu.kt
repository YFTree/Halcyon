package com.ella.music.ui.player

import android.os.SystemClock
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ella.music.R
import com.ella.music.data.model.Song
import com.ella.music.data.model.formatPlaybackDuration
import com.ella.music.ui.components.TagEditorOption
import com.ella.music.ui.components.TagEditorOptionKind
import com.ella.music.ui.components.buildTagEditorOptions
import com.ella.music.ui.components.launchTagEditorOption
import kotlin.math.round
import kotlinx.coroutines.delay
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

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

@Composable
private fun TimerSheetContent(
    onBack: () -> Unit,
    sleepTimerEndRealtimeMs: Long?,
    stopAfterCurrentEnabled: Boolean,
    sleepTimerCustomMinutes: Int,
    sleepTimerStopAfterCurrent: Boolean,
    onStopAfterCurrent: (Boolean) -> Unit,
    onTimer: (Int) -> Unit,
    onCustomTimerMinutes: (Int) -> Unit,
    onCancelTimer: () -> Unit
) {
    var customMinutes by remember(sleepTimerCustomMinutes) {
        mutableFloatStateOf(sleepTimerCustomMinutes.coerceIn(5, 120).toFloat())
    }
    var nowRealtimeMs by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    val remainingMs = sleepTimerEndRealtimeMs
        ?.minus(nowRealtimeMs)
        ?.coerceAtLeast(0L)
    val timerActive = remainingMs != null && remainingMs > 0L

    LaunchedEffect(sleepTimerEndRealtimeMs) {
        while (sleepTimerEndRealtimeMs != null) {
            nowRealtimeMs = SystemClock.elapsedRealtime()
            delay(1000L)
        }
    }

    HalfSheetTitle(title = stringResource(R.string.player_sleep_timer_title), onBack = onBack)
    Spacer(modifier = Modifier.height(18.dp))

    if (timerActive) {
        TimerStatusCard(
            title = stringResource(R.string.player_sleep_timer_running),
            subtitle = stringResource(R.string.player_sleep_timer_remaining, remainingMs.formatPlaybackDuration())
        )
        Spacer(modifier = Modifier.height(12.dp))
    } else {
        listOf(10, 15, 20, 30, 40, 60).chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { minutes ->
                    HalfSheetPill(
                        text = stringResource(R.string.player_minutes_value, minutes),
                        onClick = { onTimer(minutes) },
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat(3 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.player_custom_duration),
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MiuixTheme.colorScheme.onSurface
        )
        DottedValueSlider(
            value = customMinutes,
            valueRange = 5f..120f,
            steps = 23,
            label = stringResource(R.string.player_minutes_value, customMinutes.toInt()),
            onValueChange = {
                customMinutes = it
                onCustomTimerMinutes(it.toInt().coerceIn(5, 120))
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(92.dp)
        )
        HalfSheetPill(
            text = stringResource(R.string.player_start_timer_minutes, customMinutes.toInt()),
            selected = true,
            onClick = { onTimer(customMinutes.toInt().coerceAtLeast(1)) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(10.dp))
    }

    StopAfterCurrentRow(
        checked = stopAfterCurrentEnabled || sleepTimerStopAfterCurrent,
        onCheckedChange = onStopAfterCurrent
    )
    if (timerActive) {
        Spacer(modifier = Modifier.height(8.dp))
        PlayerActionMenuItem(stringResource(R.string.player_cancel_sleep_timer), onCancelTimer)
    }
}

@Composable
private fun TimerStatusCard(title: String, subtitle: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.72f))
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MiuixTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subtitle,
            fontSize = 13.sp,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
        )
    }
}

@Composable
private fun StopAfterCurrentRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.72f))
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(
                    if (checked) MiuixTheme.colorScheme.primary
                    else MiuixTheme.colorScheme.onSurfaceVariantSummary.copy(alpha = 0.18f)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (checked) {
                Text(
                    text = "✓",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MiuixTheme.colorScheme.onPrimary
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = stringResource(R.string.player_pause_after_current_song),
            fontSize = 14.sp,
            color = MiuixTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SpeedPitchSheetContent(
    speed: Float,
    pitch: Float,
    onBack: () -> Unit,
    onSpeed: (Float) -> Unit,
    onPitch: (Float) -> Unit
) {
    HalfSheetTitle(title = stringResource(R.string.player_speed_pitch), onBack = onBack)
    Spacer(modifier = Modifier.height(22.dp))
    SpeedPitchHeader(title = stringResource(R.string.player_speed_playback))
    DottedValueSlider(
        value = speed,
        valueRange = 0.5f..2f,
        steps = 30,
        label = speed.formatPlaybackStep(),
        onValueChange = onSpeed,
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
    )
    SpeedPitchHeader(title = stringResource(R.string.player_pitch_playback))
    DottedValueSlider(
        value = pitch,
        valueRange = 0.5f..2f,
        steps = 30,
        label = pitch.formatPlaybackStep(),
        onValueChange = onPitch,
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
    )
}

@Composable
private fun SpeedPitchHeader(title: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MiuixTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun VisualizerSheetContent(
    enabled: Boolean,
    onBack: () -> Unit,
    onEnabledChange: (Boolean) -> Unit
) {
    HalfSheetTitle(title = stringResource(R.string.player_visualizer_settings), onBack = onBack)
    Spacer(modifier = Modifier.height(22.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.72f))
            .clickable { onEnabledChange(!enabled) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.player_music_visualizer),
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MiuixTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier
                .width(56.dp)
                .height(32.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(
                    if (enabled) MiuixTheme.colorScheme.primary
                    else MiuixTheme.colorScheme.onSurfaceVariantSummary.copy(alpha = 0.26f)
                )
                .padding(4.dp),
            contentAlignment = if (enabled) Alignment.CenterEnd else Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(MiuixTheme.colorScheme.background)
            )
        }
    }
    Spacer(modifier = Modifier.height(20.dp))
    Text(
        text = stringResource(R.string.player_visualizer_permission_summary),
        fontSize = 13.sp,
        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        modifier = Modifier.padding(horizontal = 4.dp)
    )
}

@Composable
private fun TagEditorSheetContent(
    song: Song?,
    title: String,
    kind: TagEditorOptionKind,
    onBack: () -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val options = remember(song?.id, song?.path, song?.mimeType, kind) {
        song?.let { buildTagEditorOptions(context, it) }
            .orEmpty()
            .filter { it.kind == kind }
    }

    HalfSheetTitle(title = title, onBack = onBack)
    Spacer(modifier = Modifier.height(18.dp))

    if (song == null) {
        TagEditorEmptyState(stringResource(R.string.player_no_song_playing))
        return
    }

    if (song.path.startsWith("http://") || song.path.startsWith("https://")) {
        TagEditorEmptyState(stringResource(R.string.player_external_editor_not_supported_for_remote))
        return
    }

    if (options.isEmpty()) {
        TagEditorEmptyState(
            if (kind == TagEditorOptionKind.Metadata) {
                stringResource(R.string.player_no_metadata_editor_found)
            } else {
                stringResource(R.string.player_no_lyric_timing_editor_found)
            }
        )
        return
    }

    options.forEach { option ->
        TagEditorOptionRow(
            option = option,
            onClick = {
                launchTagEditorOption(context, option)
                onClose()
            }
        )
        Spacer(modifier = Modifier.height(10.dp))
    }

    Text(
        text = stringResource(R.string.player_editor_launch_hint),
        fontSize = 13.sp,
        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
    )
}

@Composable
private fun TagEditorOptionRow(
    option: TagEditorOption,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.72f))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(MiuixTheme.colorScheme.primary.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = option.label.first().toString(),
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MiuixTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = option.label,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MiuixTheme.colorScheme.onSurface
            )
            Text(
                text = option.summary,
                fontSize = 13.sp,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun TagEditorEmptyState(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.72f))
            .padding(horizontal = 18.dp, vertical = 28.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun HalfSheetTitle(title: String, onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "‹",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .clip(CircleShape)
                .clickable(onClick = onBack)
                .padding(horizontal = 12.dp, vertical = 2.dp)
        )
        Text(
            text = title,
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MiuixTheme.colorScheme.onSurface,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
private fun HalfSheetPill(
    text: String,
    selected: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (selected) MiuixTheme.colorScheme.primary.copy(alpha = 0.16f)
                else MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.72f)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (selected) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
internal fun DottedValueSlider(
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null
) {
    val safeValue = value.coerceIn(valueRange.start, valueRange.endInclusive)
    val fraction = ((safeValue - valueRange.start) / (valueRange.endInclusive - valueRange.start)).coerceIn(0f, 1f)
    val activeDotColor = MiuixTheme.colorScheme.primary.copy(alpha = 0.72f)
    val inactiveDotColor = MiuixTheme.colorScheme.onSurfaceVariantSummary.copy(alpha = 0.28f)
    val activeLineColor = MiuixTheme.colorScheme.primary.copy(alpha = 0.88f)
    val activeKnobColor = MiuixTheme.colorScheme.primary.copy(alpha = 0.92f)

    fun update(width: Float, x: Float) {
        val raw = valueRange.start + (x / width.coerceAtLeast(1f)).coerceIn(0f, 1f) *
            (valueRange.endInclusive - valueRange.start)
        val stepped = if (steps > 0) {
            val stepSize = (valueRange.endInclusive - valueRange.start) / steps
            (round(raw / stepSize) * stepSize).coerceIn(valueRange.start, valueRange.endInclusive)
        } else {
            raw
        }
        onValueChange(stepped)
    }

    BoxWithConstraints(modifier = modifier) {
        val knobOffset = (maxWidth - 46.dp) * fraction
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(valueRange, steps) {
                    detectTapGestures { offset -> update(size.width.toFloat(), offset.x) }
                }
                .pointerInput(valueRange, steps) {
                    detectDragGestures { change, _ -> update(size.width.toFloat(), change.position.x) }
                }
        ) {
            val centerY = size.height * 0.60f
            val dotCount = 44
            val gap = size.width / (dotCount - 1).coerceAtLeast(1)
            for (index in 0 until dotCount) {
                val dotFraction = index.toFloat() / (dotCount - 1).coerceAtLeast(1)
                drawCircle(
                    color = if (dotFraction <= fraction) activeDotColor else inactiveDotColor,
                    radius = if (index % 5 == 0) 4.2f else 3.2f,
                    center = Offset(x = gap * index, y = centerY)
                )
            }
            val knobX = size.width * fraction
            drawLine(
                color = activeLineColor,
                start = Offset(knobX, centerY - 36f),
                end = Offset(knobX, centerY + 36f),
                strokeWidth = 6f,
                cap = StrokeCap.Round
            )
            drawCircle(
                color = activeKnobColor,
                radius = 24f,
                center = Offset(knobX, centerY - 54f)
            )
        }
        label?.let {
            Text(
                text = it,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MiuixTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = knobOffset)
                    .padding(top = 2.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(MiuixTheme.colorScheme.primary)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
    }
}

@Composable
internal fun PlayerActionMenuItem(
    text: String,
    onClick: () -> Unit,
    danger: Boolean = false
) {
    Text(
        text = text,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = if (danger) Color(0xFFE5484D) else MiuixTheme.colorScheme.onSurface,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 13.dp)
    )
}

private fun Float.formatPlaybackStep(): String = "%.2f".format(this.coerceIn(0.5f, 2f))
