package com.ella.music.ui.player

import android.content.Context
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.media.MediaRouter2
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import com.ella.music.R
import com.ella.music.data.AudioQualitySummary
import com.ella.music.data.SettingsManager
import com.ella.music.data.audioQualitySummary
import com.ella.music.data.model.AudioInfo
import com.ella.music.data.model.Song
import com.ella.music.data.model.formatPlaybackDuration
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.window.WindowBottomSheet
import androidx.compose.ui.text.font.FontWeight

@Composable
internal fun LandscapeProgressRow(
    currentPosition: Long,
    duration: Long,
    palette: PlayerPalette,
    allowTapSeek: Boolean,
    showTotalDuration: Boolean,
    onSeek: (Float) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = formatTime(currentPosition),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = 0.72f)
        )
        GlowSeekBar(
            value = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f,
            onSeek = onSeek,
            accent = palette.accent,
            allowTapSeek = allowTapSeek,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        )
        Text(
            text = if (showTotalDuration) {
                formatTime(duration.coerceAtLeast(0L))
            } else {
                "-${formatTime((duration - currentPosition).coerceAtLeast(0L))}"
            },
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = 0.72f)
        )
    }
}

@Composable
internal fun LandscapeTransportControls(
    isPlaying: Boolean,
    shuffleEnabled: Boolean,
    repeatMode: Int,
    palette: PlayerPalette,
    onCyclePlaybackMode: () -> Unit,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        PlayerTransportIconButton(onClick = onCyclePlaybackMode) {
            PlaybackModeIcon(shuffleEnabled = shuffleEnabled, repeatMode = repeatMode, accent = palette.accent)
        }
        PlayerTransportIconButton(onClick = onPrevious) {
            Icon(
                painter = painterResource(id = R.drawable.ic_skip_previous),
                contentDescription = stringResource(R.string.common_previous),
                tint = Color.White.copy(alpha = 0.92f),
                modifier = Modifier.size(28.dp)
            )
        }
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .playerNoIndicationClick(onPlayPause),
            contentAlignment = Alignment.Center
        ) {
            CenteredPlayPauseGlyph(
                isPlaying = isPlaying,
                tint = Color.White.copy(alpha = 0.96f),
                modifier = Modifier.size(34.dp)
            )
        }
        PlayerTransportIconButton(onClick = onNext) {
            Icon(
                painter = painterResource(id = R.drawable.ic_skip_next),
                contentDescription = stringResource(R.string.common_next),
                tint = Color.White.copy(alpha = 0.92f),
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
internal fun PlayerProgressBlock(
    currentPosition: Long,
    duration: Long,
    audioInfo: AudioInfo?,
    bluetoothDeviceName: String?,
    palette: PlayerPalette,
    allowTapSeek: Boolean,
    showTotalDuration: Boolean,
    onSeek: (Float) -> Unit
) {
    val context = LocalContext.current
    var infoMode by remember(audioInfo, bluetoothDeviceName) { mutableStateOf(0) }
    val infoLabels = remember(audioInfo, bluetoothDeviceName) {
        buildList {
            audioInfo?.let {
                val quality = audioQualitySummary(it)
                add(quality.playerCompactText())
                quality.detailLabel.takeIf { text -> text.isNotBlank() }?.let(::add)
            }
            bluetoothDeviceName?.takeIf { it.isNotBlank() }?.let(::add)
        }.distinct()
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        GlowSeekBar(
            value = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f,
            onSeek = onSeek,
            accent = palette.accent,
            allowTapSeek = allowTapSeek,
            modifier = Modifier.fillMaxWidth()
        )
        Box(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = formatTime(currentPosition),
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.56f),
                modifier = Modifier.align(Alignment.CenterStart)
            )
            if (infoLabels.isNotEmpty()) {
                val infoText = infoLabels[infoMode % infoLabels.size]
                Text(
                    text = infoText,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.62f),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.10f))
                        .pointerInput(infoLabels, bluetoothDeviceName) {
                            detectTapGestures(
                                onTap = {
                                    if (infoLabels.size > 1) infoMode = (infoMode + 1) % infoLabels.size
                                },
                                onLongPress = {
                                    if (!bluetoothDeviceName.isNullOrBlank()) {
                                        openSystemOutputSwitcher(context)
                                    }
                                }
                            )
                        }
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                )
            }
            Text(
                text = if (showTotalDuration) {
                    formatTime(duration.coerceAtLeast(0L))
                } else {
                    "-${formatTime((duration - currentPosition).coerceAtLeast(0L))}"
                },
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.56f),
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }
    }
}

@Composable
internal fun PlayerTransportControls(
    isPlaying: Boolean,
    shuffleEnabled: Boolean,
    repeatMode: Int,
    palette: PlayerPalette,
    queueExpanded: Boolean,
    playlist: List<Song>,
    currentSongId: Long?,
    onCyclePlaybackMode: () -> Unit,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onToggleQueue: () -> Unit,
    onDismissQueue: () -> Unit,
    onQueueSongClick: (Int) -> Unit,
    onRemoveQueueSong: (Int) -> Unit,
    onMoveQueueSong: (Int, Int) -> Unit,
    onAddQueueToPlaylist: () -> Unit,
    onClearQueue: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager.getInstance(context) }
    val showOutlines by settingsManager.transportButtonOutlines.collectAsState(initial = false)
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        PlayerTransportIconButton(onClick = onCyclePlaybackMode) {
            PlaybackModeIcon(shuffleEnabled = shuffleEnabled, repeatMode = repeatMode, accent = palette.accent)
        }
        PlayerTransportIconButton(onClick = onPrevious) {
            Icon(
                painter = painterResource(id = R.drawable.ic_skip_previous),
                contentDescription = stringResource(R.string.common_previous),
                tint = Color.White.copy(alpha = 0.92f),
                modifier = Modifier.size(38.dp)
            )
        }
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .then(if (showOutlines) Modifier.background(Color.White.copy(alpha = 0.18f)) else Modifier)
                .playerNoIndicationClick(onPlayPause),
            contentAlignment = Alignment.Center
        ) {
            CenteredPlayPauseGlyph(
                isPlaying = isPlaying,
                tint = Color.White.copy(alpha = 0.96f),
                modifier = Modifier.size(if (isPlaying) 38.dp else 40.dp)
            )
        }
        PlayerTransportIconButton(onClick = onNext) {
            Icon(
                painter = painterResource(id = R.drawable.ic_skip_next),
                contentDescription = stringResource(R.string.common_next),
                tint = Color.White.copy(alpha = 0.92f),
                modifier = Modifier.size(38.dp)
            )
        }
        Box(contentAlignment = Alignment.Center) {
            PlayerTransportIconButton(onClick = onToggleQueue) {
                QueueListIcon(
                    color = Color.White.copy(alpha = 0.58f),
                    modifier = Modifier.size(28.dp)
                )
            }
            if (queueExpanded) {
                WindowBottomSheet(
                    show = true,
                    enableNestedScroll = false,
                    title = stringResource(R.string.player_queue_title),
                    onDismissRequest = onDismissQueue
                ) {
                    PlayerQueueMenu(
                        playlist = playlist,
                        currentSongId = currentSongId,
                        onSongClick = onQueueSongClick,
                        onRemoveSong = onRemoveQueueSong,
                        onMoveSong = onMoveQueueSong,
                        onAddQueueToPlaylist = onAddQueueToPlaylist,
                        onClearQueue = onClearQueue,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayerTransportIconButton(
    onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .playerNoIndicationClick(onClick),
        contentAlignment = Alignment.Center,
        content = content
    )
}

@Composable
private fun CenteredPlayPauseGlyph(
    isPlaying: Boolean,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Icon(
        painter = painterResource(id = if (isPlaying) R.drawable.ic_player_pause else R.drawable.ic_player_play),
        contentDescription = if (isPlaying) stringResource(R.string.common_pause) else stringResource(R.string.common_play),
        tint = tint,
        modifier = modifier
    )
}

@Composable
private fun QueueListIcon(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val stroke = 3.dp.toPx()
        val startX = size.width * 0.22f
        val endX = size.width * 0.78f
        listOf(0.30f, 0.50f, 0.70f).forEach { yFraction ->
            drawLine(
                color = color,
                start = Offset(startX, size.height * yFraction),
                end = Offset(endX, size.height * yFraction),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun PlaybackModeIcon(
    shuffleEnabled: Boolean,
    repeatMode: Int,
    accent: Color
) {
    val iconRes = when {
        shuffleEnabled -> R.drawable.ic_shuffle
        repeatMode == Player.REPEAT_MODE_ONE -> R.drawable.ic_repeat_one
        repeatMode == Player.REPEAT_MODE_ALL -> R.drawable.ic_repeat
        else -> R.drawable.ic_playback_order
    }
    val label = when {
        shuffleEnabled -> stringResource(R.string.player_playback_mode_shuffle)
        repeatMode == Player.REPEAT_MODE_ONE -> stringResource(R.string.player_playback_mode_repeat_one)
        repeatMode == Player.REPEAT_MODE_ALL -> stringResource(R.string.player_playback_mode_repeat_all)
        else -> stringResource(R.string.player_playback_mode_in_order)
    }
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = label,
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun GlowSeekBar(
    value: Float,
    onSeek: (Float) -> Unit,
    accent: Color,
    allowTapSeek: Boolean,
    modifier: Modifier = Modifier
) {
    val safeProgress = value.coerceIn(0f, 1f)
    var draggingProgress by remember { mutableStateOf<Float?>(null) }
    val displayProgress = draggingProgress ?: safeProgress

    fun progressAt(width: Float, x: Float): Float {
        return (x / width.coerceAtLeast(1f)).coerceIn(0f, 1f)
    }

    Box(
        modifier = modifier.height(30.dp)
    ) {
        AndroidView(
            factory = { context ->
                SuperIslandGlowProgressBar(context).apply {
                    shaderMode = SuperIslandGlowProgressBar.ShaderMode.HIGH_END
                    trackHeightPx = resources.displayMetrics.density * 4.5f
                    trackHorizontalPaddingPx = 0f
                    headGlowAlpha = 1f
                    trackColor = AndroidColor.argb(48, 255, 255, 255)
                }
            },
            update = { view ->
                view.progressFraction = displayProgress
                view.fallbackProgressColor = accent.copy(alpha = 0.82f).toArgb()
            },
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(allowTapSeek) {
                    if (!allowTapSeek) return@pointerInput
                    detectTapGestures { offset ->
                        onSeek(progressAt(size.width.toFloat(), offset.x))
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            draggingProgress = progressAt(size.width.toFloat(), offset.x)
                        },
                        onDragEnd = {
                            draggingProgress?.let(onSeek)
                            draggingProgress = null
                        },
                        onDragCancel = {
                            draggingProgress = null
                        }
                    ) { change, _ ->
                        draggingProgress = progressAt(size.width.toFloat(), change.position.x)
                    }
                }
        )
    }
}

private fun openSystemOutputSwitcher(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        val shown = runCatching {
            MediaRouter2.getInstance(context).showSystemOutputSwitcher()
        }.getOrDefault(false)
        if (shown) return
    }

    Toast.makeText(context, context.getString(R.string.player_media_output_unsupported), Toast.LENGTH_SHORT).show()
    runCatching {
        context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }.onFailure {
        context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

private fun formatTime(ms: Long): String {
    return ms.formatPlaybackDuration()
}

private fun AudioQualitySummary.playerCompactText(): String {
    return when {
        compactLabel == "MQ" -> "∞ Master"
        showMobius -> "∞ $compactLabel"
        else -> compactLabel
    }
}
