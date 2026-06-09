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
internal fun LandscapeLyricsOverlay(
    song: Song?,
    embeddedCover: Bitmap?,
    annotation: String,
    lyrics: List<com.ella.music.data.model.LyricLine>,
    currentLyricIndex: Int,
    currentPosition: Long,
    duration: Long,
    shuffleEnabled: Boolean,
    repeatMode: Int,
    showTranslation: Boolean,
    showPronunciation: Boolean,
    fontFamily: FontFamily?,
    fontPath: String,
    fontWeight: FontWeight,
    fontScale: Float,
    showTotalDuration: Boolean,
    palette: PlayerPalette,
    flowEffectMode: Int,
    isPlaying: Boolean,
    audioSessionId: Int,
    visualizerEnabled: Boolean,
    onLineClick: (com.ella.music.data.model.LyricLine) -> Unit,
    onLineLongClick: (com.ella.music.data.model.LyricLine) -> Unit,
    onSeek: (Float) -> Unit,
    onCyclePlaybackMode: () -> Unit,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onShowCoverPlayer: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.background(palette.middle)) {
        FluidLyricBackground(
            palette = palette,
            positionMs = currentPosition,
            isPlaying = isPlaying,
            flowEffectMode = flowEffectMode,
            modifier = Modifier.fillMaxSize()
        )
        Row(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(start = 34.dp, end = 78.dp, top = 22.dp, bottom = 28.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(0.56f)
                    .widthIn(max = 360.dp),
                contentAlignment = Alignment.Center
            ) {
                AlbumArtView(
                    song = song,
                    embeddedCover = embeddedCover,
                    modifier = Modifier
                        .fillMaxHeight(0.72f)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(16.dp))
                )
            }
            Spacer(modifier = Modifier.width(34.dp))
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1.44f)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    LandscapeSongTitle(
                        song = song,
                        annotation = annotation,
                        modifier = Modifier.fillMaxWidth()
                    )
                    SmoothLyricView(
                        songId = song?.id ?: 0L,
                        songTitle = song?.title.orEmpty(),
                        songArtist = song?.artist.orEmpty(),
                        lyrics = lyrics,
                        currentIndex = currentLyricIndex,
                        currentPositionMs = currentPosition,
                        isPlaying = isPlaying,
                        showTranslation = showTranslation,
                        showPronunciation = showPronunciation,
                        fontScale = fontScale,
                        fontPath = fontPath,
                        fontWeight = fontWeight,
                        primaryTextSizeSp = 30f,
                        secondaryTextSizeSp = 15f,
                        anchorOffsetRatio = -0.06f,
                        topContentPadding = 12.dp,
                        onLineClick = onLineClick,
                        onLineLongClick = onLineLongClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                    LandscapeProgressRow(
                        currentPosition = currentPosition,
                        duration = duration,
                        palette = palette,
                        allowTapSeek = false,
                        showTotalDuration = showTotalDuration,
                        onSeek = onSeek
                    )
                    LandscapeTransportControls(
                        isPlaying = isPlaying,
                        shuffleEnabled = shuffleEnabled,
                        repeatMode = repeatMode,
                        palette = palette,
                        onCyclePlaybackMode = onCyclePlaybackMode,
                        onPrevious = onPrevious,
                        onPlayPause = onPlayPause,
                        onNext = onNext
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(top = 26.dp, end = 92.dp)
                .size(56.dp)
                .clip(CircleShape)
                .clickable(onClick = onShowCoverPlayer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = MiuixIcons.Regular.Photos,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.92f),
                modifier = Modifier.size(26.dp)
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(top = 26.dp, end = 28.dp)
                .size(56.dp)
                .clip(CircleShape)
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            CloseIcon(
                color = Color.White.copy(alpha = 0.92f),
                modifier = Modifier.size(26.dp)
            )
        }
    }

}

@Composable
internal fun ForceLandscapePlayerBars(onDismiss: () -> Unit) {
    val activity = LocalContext.current.findActivity()
    val view = LocalView.current
    DisposableEffect(activity) {
        val oldOrientation = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        setPlayerSystemBars(activity, view)
        onDispose {
            if (oldOrientation != null) {
                activity.requestedOrientation = oldOrientation
            }
            setPlayerSystemBars(activity, view)
            view.post { setPlayerSystemBars(activity, view) }
        }
    }
    BackHandler(onBack = onDismiss)
}

@Composable
internal fun LandscapeSongTitle(
    song: Song?,
    annotation: String,
    modifier: Modifier = Modifier
) {
    PlayerSongMetaText(
        song = song,
        annotation = annotation,
        titleFontSize = 28.sp,
        artistFontSize = 16.sp,
        artistAlpha = 0.50f,
        fallbackTitle = stringResource(R.string.app_name),
        modifier = modifier.padding(end = 16.dp)
    )
}

@Composable
internal fun PlayerSongMetaText(
    song: Song?,
    annotation: String,
    titleFontSize: TextUnit,
    artistFontSize: TextUnit,
    artistAlpha: Float,
    modifier: Modifier = Modifier,
    fallbackTitle: String? = null,
    onArtistClick: (() -> Unit)? = null
) {
    val artist = song?.artist.orEmpty()
    val artistModifier = if (onArtistClick != null && artist.isNotBlank()) {
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onArtistClick)
    } else {
        Modifier.fillMaxWidth()
    }
    Column(modifier = modifier) {
        PlayerSongTitleText(
            text = song?.title?.takeIf { it.isNotBlank() }
                ?: fallbackTitle?.takeIf { it.isNotBlank() }
                ?: stringResource(R.string.player_not_playing),
            fontSize = titleFontSize,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White.copy(alpha = 0.96f),
            modifier = Modifier.fillMaxWidth()
        )
        if (annotation.isNotBlank()) {
            PlayerMarqueeText(
                text = annotation,
                fontSize = (artistFontSize.value * 0.82f).sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = (artistAlpha + 0.16f).coerceAtMost(0.82f)),
                modifier = Modifier.fillMaxWidth()
            )
        }
        PlayerMarqueeText(
            text = artist,
            fontSize = artistFontSize,
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = artistAlpha),
            modifier = artistModifier
        )
    }
}

@Composable
internal fun PlayerSongTitleText(
    text: String,
    fontSize: TextUnit,
    fontWeight: FontWeight,
    color: Color,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        fontSize = fontSize,
        fontWeight = fontWeight,
        color = color,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Clip,
        modifier = modifier.basicMarquee(iterations = Int.MAX_VALUE)
    )
}

@Composable
internal fun PlayerMarqueeText(
    text: String,
    fontSize: TextUnit,
    fontWeight: FontWeight,
    color: Color,
    modifier: Modifier = Modifier
) {
    LyriconStyleMarqueeText(
        text = AnnotatedString(text),
        style = TextStyle(
            fontSize = fontSize,
            fontWeight = fontWeight,
            color = color
        ),
        enabled = true,
        textAlign = TextAlign.Start,
        modifier = modifier
    )
}

@Composable
internal fun LyriconStyleMarqueeText(
    text: AnnotatedString,
    style: TextStyle,
    enabled: Boolean,
    textAlign: TextAlign,
    modifier: Modifier = Modifier,
    ghostSpacing: androidx.compose.ui.unit.Dp = 70.dp,
    speedDpPerSecond: Float = 40f,
    initialDelayMs: Long = 300L,
    loopDelayMs: Long = 700L
) {
    val density = LocalDensity.current
    val spacingPx = with(density) { ghostSpacing.toPx() }
    val speedPxPerMs = with(density) { speedDpPerSecond.dp.toPx() } / 1000f
    var elapsedMs by remember(text, enabled) { mutableFloatStateOf(0f) }

    LaunchedEffect(text, enabled) {
        elapsedMs = 0f
        if (!enabled) return@LaunchedEffect
        var lastFrameNanos = 0L
        while (isActive) {
            withFrameNanos { frameNanos ->
                if (lastFrameNanos != 0L) {
                    elapsedMs += (frameNanos - lastFrameNanos) / 1_000_000f
                }
                lastFrameNanos = frameNanos
            }
        }
    }

    Layout(
        content = {
            BasicText(
                text = text,
                style = style.copy(textAlign = TextAlign.Start),
                maxLines = 1,
                overflow = TextOverflow.Visible
            )
            BasicText(
                text = text,
                style = style.copy(textAlign = TextAlign.Start),
                maxLines = 1,
                overflow = TextOverflow.Visible
            )
        },
        modifier = modifier
            .fillMaxWidth()
            .clipToBounds()
    ) { measurables, constraints ->
        val textConstraints = constraints.copy(minWidth = 0, maxWidth = Constraints.Infinity)
        val primary = measurables[0].measure(textConstraints)
        val ghost = measurables[1].measure(textConstraints)
        val width = if (constraints.hasBoundedWidth) constraints.maxWidth else primary.width
        val height = primary.height
        val overflowPx = (primary.width - width).coerceAtLeast(0)
        val shouldScroll = enabled && overflowPx > 0
        val x = if (shouldScroll) {
            -lyriconMarqueeOffsetPx(
                elapsedMs = elapsedMs,
                textWidthPx = primary.width.toFloat(),
                spacingPx = spacingPx,
                speedPxPerMs = speedPxPerMs,
                initialDelayMs = initialDelayMs,
                loopDelayMs = loopDelayMs
            )
        } else {
            when (textAlign) {
                TextAlign.Center -> ((width - primary.width) / 2).coerceAtLeast(0).toFloat()
                TextAlign.End, TextAlign.Right -> (width - primary.width).coerceAtLeast(0).toFloat()
                else -> 0f
            }
        }
        val unit = primary.width + spacingPx

        layout(width, height) {
            primary.placeRelativeWithLayer(0, 0) {
                translationX = x
            }
            if (shouldScroll) {
                ghost.placeRelativeWithLayer(0, 0) {
                    translationX = x + unit
                }
            }
        }
    }
}

internal fun lyriconMarqueeOffsetPx(
    elapsedMs: Float,
    textWidthPx: Float,
    spacingPx: Float,
    speedPxPerMs: Float,
    initialDelayMs: Long,
    loopDelayMs: Long
): Float {
    val activeMs = (elapsedMs - initialDelayMs).coerceAtLeast(0f)
    if (activeMs <= 0f) return 0f
    val unit = textWidthPx + spacingPx
    val travelMs = unit / speedPxPerMs.coerceAtLeast(0.001f)
    val cycleMs = travelMs + loopDelayMs
    val cycleTime = activeMs % cycleMs
    if (cycleTime >= travelMs) return 0f
    return (cycleTime * speedPxPerMs).coerceIn(0f, unit)
}

@Composable
internal fun LandscapeLyricShowcase(
    lyrics: List<com.ella.music.data.model.LyricLine>,
    currentIndex: Int,
    currentPositionMs: Long,
    showTranslation: Boolean,
    showPronunciation: Boolean,
    fontFamily: FontFamily?,
    fontWeight: FontWeight,
    onLineClick: (com.ella.music.data.model.LyricLine) -> Unit,
    onLineLongClick: (com.ella.music.data.model.LyricLine) -> Unit,
    modifier: Modifier = Modifier
) {
    if (lyrics.isEmpty()) {
        Box(
            modifier = modifier.padding(top = 14.dp, bottom = 10.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            LandscapeLyricLine(
                line = null,
                currentPositionMs = currentPositionMs,
                showTranslation = showTranslation,
                showPronunciation = showPronunciation,
                fontFamily = fontFamily,
                fontWeight = fontWeight,
                primary = true,
                alpha = 0.9f,
                scale = 1f,
                onLineClick = onLineClick,
                onLineLongClick = onLineLongClick
            )
        }
        return
    }

    val safeIndex = currentIndex.coerceIn(0, lyrics.lastIndex)
    val listState = rememberLazyListState()
    LaunchedEffect(safeIndex, lyrics.size) {
        listState.animateScrollToItem((safeIndex - 1).coerceAtLeast(0))
    }

    LazyColumn(
        state = listState,
        modifier = modifier.padding(top = 8.dp, bottom = 4.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 22.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        itemsIndexed(
            items = lyrics,
            key = { index, line -> "${line.timeMs}_$index" }
        ) { index, line ->
            val distance = abs(index - safeIndex)
            LandscapeLyricLine(
                line = line,
                currentPositionMs = currentPositionMs,
                showTranslation = showTranslation,
                showPronunciation = showPronunciation,
                fontFamily = fontFamily,
                fontWeight = fontWeight,
                primary = index == safeIndex,
                alpha = when (distance) {
                    0 -> 0.98f
                    1 -> 0.42f
                    2 -> 0.24f
                    else -> 0.14f
                },
                scale = when (distance) {
                    0 -> 1f
                    1 -> 0.86f
                    2 -> 0.78f
                    else -> 0.72f
                },
                onLineClick = onLineClick,
                onLineLongClick = onLineLongClick
            )
        }
    }
}
