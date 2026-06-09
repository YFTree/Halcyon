package com.ella.music.ui.player

import android.content.Context
import android.app.Activity
import android.app.DownloadManager
import android.Manifest
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.content.Intent
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import androidx.compose.runtime.DisposableEffect
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.zIndex
import androidx.media3.common.Player
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.ella.music.R
import com.ella.music.data.SettingsManager
import com.ella.music.data.NeteaseKeyInfo
import com.ella.music.data.audioQualitySummary
import com.ella.music.data.decodeNeteaseKey
import com.ella.music.data.exception.WritePermissionRequiredException
import com.ella.music.data.neteaseAlbumUrl
import com.ella.music.data.neteaseArtistUrl
import com.ella.music.data.neteaseSongUrl
import com.ella.music.data.splitArtistNames
import com.ella.music.data.tagIdentityKey
import com.ella.music.data.model.AudioInfo
import com.ella.music.data.model.FAVORITES_PLAYLIST_ID
import com.ella.music.data.model.LyricLine
import com.ella.music.data.model.Song
import com.ella.music.data.model.SongTagInfo
import com.ella.music.data.model.albumIdentityId
import com.ella.music.data.model.playlistIdentityKey
import com.ella.music.data.repository.MusicRepository
import com.ella.music.player.PlaybackAudioSession
import com.ella.music.ui.components.ArtistPickerSheet
import com.ella.music.ui.components.ConfirmDangerDialog
import com.ella.music.ui.components.DefaultAlbumCover
import com.ella.music.ui.components.SmoothLyricView
import com.ella.music.ui.components.SafeCoverImage
import com.ella.music.ui.components.CoverLoadLimiter
import com.ella.music.ui.components.LyricSharePicker
import com.ella.music.ui.components.RatingSheet
import com.ella.music.ui.components.SongAiInterpretationSheet
import com.ella.music.ui.components.TagEditorOptionIds
import com.ella.music.ui.components.TagEditorOptionKind
import com.ella.music.ui.components.AddToPlaylistSheet
import com.ella.music.ui.components.CreatePlaylistAndAddSheet
import com.ella.music.ui.components.buildTagEditorOptions
import com.ella.music.ui.components.launchTagEditorOption
import com.ella.music.ui.components.SongInfoSheet
import com.ella.music.ui.components.openSongSpectrumWithAspectPro
import com.ella.music.ui.components.shareLyricCard
import com.ella.music.ui.components.shareLocalSong
import com.ella.music.viewmodel.MainViewModel
import com.ella.music.viewmodel.PlayerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.ln
import kotlin.math.sqrt
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.window.WindowBottomSheet
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Music
import top.yukonga.miuix.kmp.icon.extended.Photos
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun LandscapeLyricLine(
    line: com.ella.music.data.model.LyricLine?,
    currentPositionMs: Long,
    showTranslation: Boolean,
    showPronunciation: Boolean,
    fontFamily: FontFamily?,
    fontWeight: FontWeight,
    primary: Boolean,
    alpha: Float,
    scale: Float,
    onLineClick: (com.ella.music.data.model.LyricLine) -> Unit,
    onLineLongClick: (com.ella.music.data.model.LyricLine) -> Unit
) {
    if (line == null) {
        Text(
                    text = stringResource(R.string.player_no_lyrics),
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = alpha),
            fontFamily = fontFamily
        )
        return
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(line) {
                detectTapGestures(
                    onTap = { onLineClick(line) },
                    onLongPress = { onLineLongClick(line) }
                )
            },
        horizontalAlignment = Alignment.Start
    ) {
        val pronunciation = line.pronunciation.orEmpty()
        val translation = line.translation.orEmpty()
        if (showPronunciation && pronunciation.isNotBlank()) {
            Text(
                text = pronunciation,
                fontSize = (14 * scale).sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = alpha * 0.58f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontFamily = fontFamily
            )
        }
        Text(
            text = line.text.ifBlank { "♪" },
            fontSize = (if (primary) 26 else 22).sp * scale,
            lineHeight = (if (primary) 31 else 27).sp * scale,
            fontWeight = if (primary) FontWeight.ExtraBold else FontWeight.Bold,
            color = Color.White.copy(alpha = alpha),
            maxLines = if (primary) 3 else 2,
            overflow = TextOverflow.Ellipsis,
            fontFamily = fontFamily
        )
        if (showTranslation && translation.isNotBlank()) {
            Text(
                text = translation,
                fontSize = (if (primary) 17 else 14).sp * scale,
                lineHeight = (if (primary) 22 else 19).sp * scale,
                fontWeight = if (primary) FontWeight.Bold else FontWeight.SemiBold,
                color = Color.White.copy(alpha = if (primary) 0.74f else alpha * 0.72f),
                maxLines = if (primary) 2 else 1,
                overflow = TextOverflow.Ellipsis,
                fontFamily = fontFamily
            )
        }
    }
}

internal enum class PlayerHeaderActionKind {
    Favorite,
    More
}

@Composable
internal fun Modifier.playerNoIndicationClick(onClick: () -> Unit): Modifier =
    clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onClick
    )

@Composable
internal fun PlayerQuickActionRow(
    onSongInfo: () -> Unit,
    onShareSong: () -> Unit,
    onTimer: () -> Unit,
    onEditMetadata: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        PlayerQuickAction(stringResource(R.string.player_quick_info), PlayerQuickActionKind.Info, onSongInfo)
        PlayerQuickAction(stringResource(R.string.player_quick_share), PlayerQuickActionKind.Share, onShareSong)
        PlayerQuickAction(stringResource(R.string.player_quick_timer), PlayerQuickActionKind.Timer, onTimer)
        PlayerQuickAction(stringResource(R.string.player_quick_edit), PlayerQuickActionKind.Edit, onEditMetadata)
        PlayerQuickAction(stringResource(R.string.player_quick_more), PlayerQuickActionKind.More, onMore)
    }
}

internal enum class PlayerQuickActionKind {
    Info,
    Share,
    Timer,
    Edit,
    More
}

@Composable
internal fun PlayerQuickAction(
    label: String,
    kind: PlayerQuickActionKind,
    onClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(42.dp)
            .playerNoIndicationClick(onClick)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            QuickActionIcon(
                kind = kind,
                color = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.size(19.dp)
            )
        }
    }
}

@Composable
internal fun QuickActionIcon(
    kind: PlayerQuickActionKind,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val stroke = size.minDimension * 0.10f
        val cx = size.width / 2f
        val cy = size.height / 2f
        when (kind) {
            PlayerQuickActionKind.Info -> {
                drawCircle(color = color, radius = size.minDimension * 0.42f, style = androidx.compose.ui.graphics.drawscope.Stroke(stroke, cap = StrokeCap.Round))
                drawLine(color, Offset(cx, size.height * 0.46f), Offset(cx, size.height * 0.70f), stroke, cap = StrokeCap.Round)
                drawCircle(color = color, radius = stroke * 0.68f, center = Offset(cx, size.height * 0.30f))
            }
            PlayerQuickActionKind.Share -> {
                val a = Offset(size.width * 0.26f, size.height * 0.58f)
                val b = Offset(size.width * 0.68f, size.height * 0.30f)
                val c = Offset(size.width * 0.70f, size.height * 0.74f)
                drawLine(color, a, b, stroke, cap = StrokeCap.Round)
                drawLine(color, a, c, stroke, cap = StrokeCap.Round)
                listOf(a, b, c).forEach { drawCircle(color = color, radius = stroke * 1.35f, center = it) }
            }
            PlayerQuickActionKind.Timer -> {
                drawCircle(color = color, radius = size.minDimension * 0.40f, style = androidx.compose.ui.graphics.drawscope.Stroke(stroke, cap = StrokeCap.Round))
                drawLine(color, Offset(cx, cy), Offset(cx, size.height * 0.30f), stroke, cap = StrokeCap.Round)
                drawLine(color, Offset(cx, cy), Offset(size.width * 0.66f, size.height * 0.58f), stroke, cap = StrokeCap.Round)
            }
            PlayerQuickActionKind.Edit -> {
                drawLine(color, Offset(size.width * 0.28f, size.height * 0.72f), Offset(size.width * 0.72f, size.height * 0.28f), stroke * 1.3f, cap = StrokeCap.Round)
                drawLine(color, Offset(size.width * 0.22f, size.height * 0.78f), Offset(size.width * 0.40f, size.height * 0.72f), stroke, cap = StrokeCap.Round)
            }
            PlayerQuickActionKind.More -> {
                listOf(0.25f, 0.5f, 0.75f).forEach { x ->
                    drawCircle(color = color, radius = stroke * 0.95f, center = Offset(size.width * x, cy))
                }
            }
        }
    }
}

@Composable
internal fun PlayerHeaderAction(
    kind: PlayerHeaderActionKind,
    selected: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .playerNoIndicationClick(onClick),
        contentAlignment = Alignment.Center
    ) {
        when (kind) {
            PlayerHeaderActionKind.Favorite -> Icon(
                painter = painterResource(
                    id = if (selected) R.drawable.ic_notification_favorite_filled
                    else R.drawable.ic_notification_favorite
                ),
                contentDescription = if (selected) {
                    stringResource(R.string.common_unfavorite)
                } else {
                    stringResource(R.string.common_favorite)
                },
                tint = if (selected) Color(0xFFFF4D6D) else Color.White.copy(alpha = 0.92f),
                modifier = Modifier.size(25.dp)
            )
            PlayerHeaderActionKind.More -> MoreIcon(
                color = Color.White.copy(alpha = 0.90f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
internal fun HeartIcon(
    color: Color,
    filled: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.50f, h * 0.86f)
            cubicTo(w * 0.18f, h * 0.60f, w * 0.04f, h * 0.42f, w * 0.10f, h * 0.24f)
            cubicTo(w * 0.17f, h * 0.04f, w * 0.39f, h * 0.05f, w * 0.50f, h * 0.25f)
            cubicTo(w * 0.61f, h * 0.05f, w * 0.83f, h * 0.04f, w * 0.90f, h * 0.24f)
            cubicTo(w * 0.96f, h * 0.42f, w * 0.82f, h * 0.60f, w * 0.50f, h * 0.86f)
            close()
        }
        if (filled) {
            drawPath(path, color)
        } else {
            drawPath(
                path = path,
                color = color,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = size.minDimension * 0.09f,
                    cap = StrokeCap.Round
                )
            )
        }
    }
}

@Composable
internal fun MoreIcon(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val radius = size.minDimension * 0.09f
        val centerX = size.width / 2f
        listOf(0.25f, 0.50f, 0.75f).forEach { y ->
            drawCircle(color = color, radius = radius, center = Offset(centerX, size.height * y))
        }
    }
}

@Composable
internal fun CloseIcon(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val stroke = size.minDimension * 0.12f
        drawLine(
            color = color,
            start = Offset(size.width * 0.22f, size.height * 0.22f),
            end = Offset(size.width * 0.78f, size.height * 0.78f),
            strokeWidth = stroke
        )
        drawLine(
            color = color,
            start = Offset(size.width * 0.78f, size.height * 0.22f),
            end = Offset(size.width * 0.22f, size.height * 0.78f),
            strokeWidth = stroke
        )
    }
}

@Composable
internal fun LyricToggleButton(
    text: String,
    active: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = if (active) 0.24f else 0.10f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = if (active) 1f else 0.62f)
        )
    }
}

@Composable
internal fun LyricActionMenu(
    showPronunciation: Boolean,
    showTranslation: Boolean,
    keepScreenOn: Boolean,
    lyricFormatAvailability: MusicRepository.LyricFormatAvailability,
    preferTtmlLyrics: Boolean?,
    lyricSourceMode: Int,
    fontScale: Float,
    onTogglePronunciation: () -> Unit,
    onToggleTranslation: () -> Unit,
    onToggleKeepScreenOn: () -> Unit,
    onLyricSourceMode: (Int) -> Unit,
    onLyricFormatPreference: (Boolean) -> Unit,
    onFontScale: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 10.dp)
    ) {
        PlayerActionMenuItem(
            text = stringResource(if (showPronunciation) R.string.player_hide_pronunciation else R.string.player_show_pronunciation),
            onClick = onTogglePronunciation
        )
        PlayerActionMenuItem(
            text = stringResource(if (showTranslation) R.string.player_hide_translation else R.string.player_show_translation),
            onClick = onToggleTranslation
        )
        PlayerActionMenuItem(
            text = stringResource(if (keepScreenOn) R.string.player_disable_keep_screen_on else R.string.player_enable_keep_screen_on),
            onClick = onToggleKeepScreenOn
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.player_lyric_font_size),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
        )
        DottedValueSlider(
            value = fontScale.coerceIn(0.75f, 1.30f),
            valueRange = 0.75f..1.30f,
            steps = 11,
            label = "${(fontScale.coerceIn(0.75f, 1.30f) * 100f).toInt()}%",
            onValueChange = onFontScale,
            modifier = Modifier
                .fillMaxWidth()
                .height(82.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (lyricFormatAvailability.hasBoth) {
            Text(
                text = stringResource(R.string.player_lyric_format),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LyricSourceChip(
                    text = stringResource(R.string.player_lyric_format_ttml),
                    selected = preferTtmlLyrics != false,
                    onClick = { onLyricFormatPreference(true) },
                    modifier = Modifier.weight(1f)
                )
                LyricSourceChip(
                    text = stringResource(R.string.player_lyric_format_lrc),
                    selected = preferTtmlLyrics == false,
                    onClick = { onLyricFormatPreference(false) },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
        Text(
            text = stringResource(R.string.player_lyric_source),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LyricSourceChip(
                text = stringResource(R.string.player_lyric_source_auto),
                selected = lyricSourceMode == SettingsManager.LYRIC_SOURCE_AUTO,
                onClick = { onLyricSourceMode(SettingsManager.LYRIC_SOURCE_AUTO) },
                modifier = Modifier.weight(1f)
            )
            LyricSourceChip(
                text = stringResource(R.string.player_lyric_source_external),
                selected = lyricSourceMode == SettingsManager.LYRIC_SOURCE_EXTERNAL,
                onClick = { onLyricSourceMode(SettingsManager.LYRIC_SOURCE_EXTERNAL) },
                modifier = Modifier.weight(1f)
            )
            LyricSourceChip(
                text = stringResource(R.string.player_lyric_source_embedded),
                selected = lyricSourceMode == SettingsManager.LYRIC_SOURCE_EMBEDDED,
                onClick = { onLyricSourceMode(SettingsManager.LYRIC_SOURCE_EMBEDDED) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
internal fun LyricSourceChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(15.dp))
            .background(
                if (selected) MiuixTheme.colorScheme.primary.copy(alpha = 0.16f)
                else MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.72f)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 11.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (selected) "✓ $text" else text,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurfaceVariantSummary
        )
    }
}
