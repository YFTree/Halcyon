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
internal fun LandscapeCoverPlayerPage(
    song: Song?,
    embeddedCover: Bitmap?,
    annotation: String,
    dynamicCoverSource: DynamicCoverSource?,
    isPlaying: Boolean,
    currentPosition: Long,
    duration: Long,
    shuffleEnabled: Boolean,
    repeatMode: Int,
    audioInfo: AudioInfo?,
    palette: PlayerPalette,
    lyrics: List<com.ella.music.data.model.LyricLine>,
    currentLyricIndex: Int,
    showTranslation: Boolean,
    showPronunciation: Boolean,
    fontFamily: FontFamily?,
    fontPath: String,
    fontWeight: FontWeight,
    fontScale: Float,
    showTotalDuration: Boolean,
    queueExpanded: Boolean,
    playlist: List<Song>,
    audioSessionId: Int,
    visualizerEnabled: Boolean,
    flowEffectMode: Int,
    dynamicFlowEnabled: Boolean,
    customBackgroundUri: String,
    onDynamicCoverFailed: (String) -> Unit,
    isFavorite: Boolean,
    onToggleMenu: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleQueue: () -> Unit,
    onDismissQueue: () -> Unit,
    onShowLyrics: () -> Unit,
    onLyricLineClick: (com.ella.music.data.model.LyricLine) -> Unit,
    onLyricLineLongClick: (com.ella.music.data.model.LyricLine) -> Unit,
    onSeek: (Float) -> Unit,
    onCyclePlaybackMode: () -> Unit,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onQueueSongClick: (Int) -> Unit,
    onRemoveQueueSong: (Int) -> Unit,
    onMoveQueueSong: (Int, Int) -> Unit,
    onAddQueueToPlaylist: () -> Unit,
    onClearQueue: () -> Unit,
    onLineClick: () -> Unit,
    onArtist: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bluetoothDeviceName = rememberBluetoothOutputName()
    Box(modifier = modifier.background(palette.middle)) {
        LandscapeCoverModeBackground(
            palette = palette,
            currentPosition = currentPosition,
            isPlaying = isPlaying,
            flowEffectMode = flowEffectMode,
            dynamicFlowEnabled = dynamicFlowEnabled,
            visualizerEnabled = visualizerEnabled,
            customBackgroundUri = customBackgroundUri,
            modifier = Modifier.fillMaxSize()
        )
        Row(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 32.dp, vertical = 22.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(0.62f),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight(0.82f)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (dynamicCoverSource != null) {
                        DynamicCoverVideo(
                            source = dynamicCoverSource,
                            isPlaying = isPlaying,
                            onPlaybackError = { onDynamicCoverFailed(dynamicCoverSource.failureKey) },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        AlbumArtView(
                            song = song,
                            embeddedCover = embeddedCover,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(28.dp))
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1.38f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PlayerSongMetaText(
                        song = song,
                        annotation = annotation,
                        titleFontSize = 24.sp,
                        artistFontSize = 14.sp,
                        artistAlpha = 0.56f,
                        onArtistClick = onArtist,
                        modifier = Modifier.weight(1f)
                    )
                    PlayerHeaderAction(
                        kind = PlayerHeaderActionKind.Favorite,
                        selected = isFavorite,
                        onClick = onToggleFavorite
                    )
                    PlayerHeaderAction(kind = PlayerHeaderActionKind.More, onClick = onToggleMenu)
                }
                Spacer(modifier = Modifier.height(8.dp))
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
                    primaryTextSizeSp = 28f,
                    secondaryTextSizeSp = 14f,
                    anchorOffsetRatio = -0.08f,
                    topContentPadding = 8.dp,
                    onLineClick = onLyricLineClick,
                    onLineLongClick = onLyricLineLongClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
                PlayerProgressBlock(
                    currentPosition = currentPosition,
                    duration = duration,
                    audioInfo = audioInfo,
                    bluetoothDeviceName = bluetoothDeviceName,
                    palette = palette,
                    allowTapSeek = false,
                    showTotalDuration = showTotalDuration,
                    onSeek = onSeek
                )
                Spacer(modifier = Modifier.height(8.dp))
                PlayerTransportControls(
                    isPlaying = isPlaying,
                    shuffleEnabled = shuffleEnabled,
                    repeatMode = repeatMode,
                    palette = palette,
                    queueExpanded = queueExpanded,
                    playlist = playlist,
                    currentSongId = song?.id,
                    onCyclePlaybackMode = onCyclePlaybackMode,
                    onPrevious = onPrevious,
                    onPlayPause = onPlayPause,
                    onNext = onNext,
                    onToggleQueue = onToggleQueue,
                    onDismissQueue = onDismissQueue,
                    onQueueSongClick = onQueueSongClick,
                    onRemoveQueueSong = onRemoveQueueSong,
                    onMoveQueueSong = onMoveQueueSong,
                    onAddQueueToPlaylist = onAddQueueToPlaylist,
                    onClearQueue = onClearQueue
                )
            }
        }
        AudioVisualizer(
            enabled = visualizerEnabled,
            audioSessionId = audioSessionId,
            isPlaying = isPlaying,
            positionMs = currentPosition,
            accent = Color.White.copy(alpha = 0.72f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .fillMaxWidth()
                .height(68.dp)
        )
    }
}

@Composable
internal fun LandscapeCoverModeBackground(
    palette: PlayerPalette,
    currentPosition: Long,
    isPlaying: Boolean,
    flowEffectMode: Int,
    dynamicFlowEnabled: Boolean,
    visualizerEnabled: Boolean,
    customBackgroundUri: String,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.background(palette.middle)) {
        if (customBackgroundUri.isNotBlank()) {
            PlayerCustomBackground(
                uri = customBackgroundUri,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            val animate = dynamicFlowEnabled && !isPlaying && !visualizerEnabled
            PlayerFlowBackground(
                palette = palette,
                flowEffectMode = flowEffectMode,
                animate = animate,
                modifier = Modifier.fillMaxSize()
            )
            FluidLyricBackground(
                palette = palette,
                positionMs = currentPosition,
                isPlaying = isPlaying,
                flowEffectMode = flowEffectMode,
                animate = animate,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
internal fun LandscapeCoverPlaybackOverlay(
    song: Song?,
    embeddedCover: Bitmap?,
    annotation: String,
    dynamicCoverSource: DynamicCoverSource?,
    isPlaying: Boolean,
    currentPosition: Long,
    duration: Long,
    shuffleEnabled: Boolean,
    repeatMode: Int,
    audioInfo: AudioInfo?,
    palette: PlayerPalette,
    lyrics: List<com.ella.music.data.model.LyricLine>,
    currentLyricIndex: Int,
    showTranslation: Boolean,
    showPronunciation: Boolean,
    fontFamily: FontFamily?,
    fontPath: String,
    fontWeight: FontWeight,
    fontScale: Float,
    queueExpanded: Boolean,
    playlist: List<Song>,
    audioSessionId: Int,
    visualizerEnabled: Boolean,
    flowEffectMode: Int,
    onDynamicCoverFailed: (String) -> Unit,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onToggleQueue: () -> Unit,
    onDismissQueue: () -> Unit,
    onShowLyrics: () -> Unit,
    onLyricLineClick: (com.ella.music.data.model.LyricLine) -> Unit,
    onLyricLineLongClick: (com.ella.music.data.model.LyricLine) -> Unit,
    onSeek: (Float) -> Unit,
    onCyclePlaybackMode: () -> Unit,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onQueueSongClick: (Int) -> Unit,
    onRemoveQueueSong: (Int) -> Unit,
    onMoveQueueSong: (Int, Int) -> Unit,
    onAddQueueToPlaylist: () -> Unit,
    onClearQueue: () -> Unit,
    onArtist: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coverItems = remember(playlist, song?.id) {
        val source = playlist.takeIf { it.isNotEmpty() } ?: listOfNotNull(song)
        val centerIndex = source.indexOfFirst { it.id == song?.id }.takeIf { it >= 0 } ?: 0
        listOf(-3, -2, -1, 0, 1, 2, 3)
            .mapNotNull { offset -> source.getOrNull(centerIndex + offset)?.let { offset to it } }
            .ifEmpty { listOfNotNull(song?.let { 0 to it }) }
    }
    val swipeThresholdPx = with(LocalDensity.current) { 92.dp.toPx() }
    var swipeDragX by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .background(palette.middle)
            .pointerInput(onPrevious, onNext) {
                detectDragGestures(
                    onDragStart = { swipeDragX = 0f },
                    onDragCancel = { swipeDragX = 0f },
                    onDragEnd = {
                        when {
                            swipeDragX > swipeThresholdPx -> onPrevious()
                            swipeDragX < -swipeThresholdPx -> onNext()
                        }
                        swipeDragX = 0f
                    },
                    onDrag = { change, dragAmount ->
                        if (abs(dragAmount.x) > abs(dragAmount.y)) {
                            swipeDragX += dragAmount.x
                            change.consume()
                        }
                    }
                )
            }
    ) {
        LandscapeCoverModeBackground(
            palette = palette,
            currentPosition = currentPosition,
            isPlaying = isPlaying,
            flowEffectMode = flowEffectMode,
            dynamicFlowEnabled = false,
            visualizerEnabled = visualizerEnabled,
            customBackgroundUri = "",
            modifier = Modifier.fillMaxSize()
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                LandscapeCoverStack(
                    currentSong = song,
                    embeddedCover = embeddedCover,
                    dynamicCoverSource = dynamicCoverSource,
                    isPlaying = isPlaying,
                    coverItems = coverItems,
                    onDynamicCoverFailed = onDynamicCoverFailed,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = song?.title?.takeIf { it.isNotBlank() } ?: stringResource(R.string.app_name),
                color = Color.White.copy(alpha = 0.96f),
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 34.dp)
            )
            Text(
                text = song?.artist?.takeIf { it.isNotBlank() } ?: stringResource(R.string.player_unknown_artist),
                color = Color.White.copy(alpha = 0.52f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 34.dp, vertical = 2.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(top = 26.dp, end = 92.dp)
                .size(56.dp)
                .clip(CircleShape)
                .clickable(onClick = onShowLyrics),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = MiuixIcons.Regular.Music,
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
internal fun LandscapeCoverStack(
    currentSong: Song?,
    embeddedCover: Bitmap?,
    dynamicCoverSource: DynamicCoverSource?,
    isPlaying: Boolean,
    coverItems: List<Pair<Int, Song>>,
    onDynamicCoverFailed: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val visibleItems = remember(coverItems) {
        coverItems.sortedByDescending { abs(it.first) }
    }
    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        val coverSize = minOf(maxHeight * 0.84f, maxWidth * 0.30f).coerceAtLeast(118.dp)
        val maxDistance = visibleItems.maxOfOrNull { abs(it.first) } ?: 0
        val outerScale = (1f - maxDistance * 0.13f).coerceAtLeast(0.58f)
        val horizontalStep = if (maxDistance > 0) {
            (((maxWidth - coverSize * outerScale) / 2f) - 4.dp)
                .coerceAtLeast(126.dp) / maxDistance.toFloat()
        } else {
            126.dp
        }
        visibleItems.forEach { (offsetIndex, itemSong) ->
            val distance = abs(offsetIndex)
            val isCenter = offsetIndex == 0
            val xOffset = horizontalStep * offsetIndex.toFloat()
            val scale = (1f - distance * 0.13f).coerceAtLeast(0.58f)
            val cardAlpha = (1f - distance * 0.14f).coerceAtLeast(0.34f)
            val rotation = -offsetIndex * 13f
            val coverModifier = Modifier
                .size(coverSize)
                .offset(x = xOffset, y = (distance * 8).dp)
                .zIndex(10f - distance)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    alpha = cardAlpha
                    rotationY = rotation
                    cameraDistance = 18f * density
                }

            Box(
                modifier = coverModifier,
                contentAlignment = Alignment.Center
            ) {
                LandscapeCoverReflection(
                    song = itemSong,
                    embeddedCover = embeddedCover.takeIf { isCenter && itemSong.id == currentSong?.id },
                    cornerRadius = if (isCenter) 14.dp else 10.dp,
                    alpha = if (isCenter) 0.34f else 0.18f
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(if (isCenter) 14.dp else 10.dp))
                        .background(Color.White.copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isCenter && dynamicCoverSource != null) {
                        DynamicCoverVideo(
                            source = dynamicCoverSource,
                            isPlaying = isPlaying,
                            onPlaybackError = { onDynamicCoverFailed(dynamicCoverSource.failureKey) },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        LandscapeStackCoverImage(
                            song = itemSong,
                            embeddedCover = embeddedCover.takeIf { isCenter && itemSong.id == currentSong?.id },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    if (!isCenter) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.16f + distance * 0.05f))
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun BoxScope.LandscapeCoverReflection(
    song: Song,
    embeddedCover: Bitmap?,
    cornerRadius: androidx.compose.ui.unit.Dp,
    alpha: Float
) {
    Box(
        modifier = Modifier
            .matchParentSize()
            .align(Alignment.BottomCenter)
            .offset(y = 18.dp)
            .graphicsLayer {
                scaleY = -0.52f
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f)
                this.alpha = alpha
            }
            .clip(RoundedCornerShape(cornerRadius))
    ) {
        LandscapeStackCoverImage(
            song = song,
            embeddedCover = embeddedCover,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black,
                        0.52f to Color.Black.copy(alpha = 0.36f),
                        1f to Color.Transparent
                    )
                )
        )
    }
}

@Composable
internal fun LandscapeStackCoverImage(
    song: Song,
    embeddedCover: Bitmap?,
    modifier: Modifier = Modifier
) {
    val uri = if (song.albumId > 0L) {
        Uri.parse("content://media/external/audio/albumart/${song.albumId}")
    } else {
        null
    }
    val coverModel = embeddedCover ?: song.coverUrl.takeIf { it.isNotBlank() } ?: uri
    if (coverModel != null) {
        SafeCoverImage(
            model = coverModel,
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Fit,
            sizePx = 512,
            showDefaultPlaceholder = false
        )
    } else {
        DefaultAlbumCover(modifier = modifier)
    }
}
