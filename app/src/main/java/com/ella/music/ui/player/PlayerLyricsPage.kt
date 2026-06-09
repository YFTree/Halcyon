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
internal fun LyricsPlayerPage(
    song: Song?,
    embeddedCover: Bitmap?,
    annotation: String,
    lyrics: List<com.ella.music.data.model.LyricLine>,
    currentLyricIndex: Int,
    currentPosition: Long,
    showTranslation: Boolean,
    showPronunciation: Boolean,
    keepScreenOn: Boolean,
    lyricFormatAvailability: MusicRepository.LyricFormatAvailability,
    preferTtmlLyrics: Boolean?,
    lyricSourceMode: Int,
    fontFamily: FontFamily?,
    fontPath: String,
    fontWeight: FontWeight,
    italic: Boolean,
    fontScale: Float,
    perspectiveEffect: Boolean,
    palette: PlayerPalette,
    flowEffectMode: Int,
    currentPositionMs: Long,
    isPlaying: Boolean,
    playerBackgroundEnabled: Boolean,
    playerBackgroundUri: String,
    isFavorite: Boolean,
    audioSessionId: Int,
    visualizerEnabled: Boolean,
    onLineClick: (com.ella.music.data.model.LyricLine) -> Unit,
    onLineDoubleClick: () -> Unit,
    onLineLongClick: (com.ella.music.data.model.LyricLine) -> Unit,
    onDismissLyrics: () -> Unit,
    onTogglePronunciation: () -> Unit,
    onToggleTranslation: () -> Unit,
    onToggleKeepScreenOn: () -> Unit,
    onToggleFavorite: () -> Unit,
    onFontScale: (Float) -> Unit,
    onLyricSourceMode: (Int) -> Unit,
    onLyricFormatPreference: (Boolean) -> Unit,
    onArtist: () -> Unit,
    enableSwipeDismiss: Boolean,
    useBlurBackground: Boolean,
    modifier: Modifier = Modifier
) {
    var lyricMenuExpanded by remember { mutableStateOf(false) }
    var dismissDragX by remember { mutableFloatStateOf(0f) }

    val lyricBackgroundMotion = 0.42f

    val swipeDismissModifier = if (enableSwipeDismiss) {
        Modifier.pointerInput(onDismissLyrics) {
            detectDragGestures(
                onDragStart = { dismissDragX = 0f },
                onDrag = { change, dragAmount ->
                    if (abs(dragAmount.x) > abs(dragAmount.y)) {
                        dismissDragX += dragAmount.x
                        change.consume()
                    }
                },
                onDragCancel = { dismissDragX = 0f },
                onDragEnd = {
                    if (dismissDragX > 96.dp.toPx()) {
                        onDismissLyrics()
                    }
                    dismissDragX = 0f
                }
            )
        }
    } else {
        Modifier
    }

    Box(modifier = modifier.then(swipeDismissModifier)) {
        val useCustomPlayerBackground = playerBackgroundEnabled && playerBackgroundUri.isNotBlank() && !useBlurBackground
        if (useCustomPlayerBackground) {
            PlayerCustomBackground(
                uri = playerBackgroundUri,
                modifier = Modifier.fillMaxSize()
            )
        } else if (useBlurBackground) {
            PlayerBlurBackground(
                song = song,
                embeddedCover = embeddedCover,
                palette = palette,
                motion = lyricBackgroundMotion,
                isPlaying = isPlaying,
                modifier = Modifier.fillMaxSize()
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 28.dp)
        ) {
            LyricsPlayerHeader(
                song = song,
                embeddedCover = embeddedCover,
                annotation = annotation,
                isFavorite = isFavorite,
                onDismissLyrics = onDismissLyrics,
                onArtist = onArtist,
                onToggleFavorite = onToggleFavorite,
                onShowMenu = { lyricMenuExpanded = true },
                modifier = Modifier.padding(top = 28.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                SmoothLyricView(
                    songId = song?.id ?: 0L,
                    songTitle = song?.title.orEmpty(),
                    songArtist = song?.artist.orEmpty(),
                    lyrics = lyrics,
                    currentIndex = currentLyricIndex,
                    currentPositionMs = currentPositionMs,
                    isPlaying = isPlaying,
                    showTranslation = showTranslation,
                    showPronunciation = showPronunciation,
                    fontScale = fontScale,
                    fontPath = fontPath,
                    fontWeight = fontWeight,
                    italic = italic,
                    onLineClick = onLineClick,
                    onLineDoubleClick = onLineDoubleClick,
                    onLineLongClick = onLineLongClick,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        AudioVisualizer(
            enabled = visualizerEnabled,
            audioSessionId = audioSessionId,
            isPlaying = isPlaying,
            positionMs = currentPositionMs,
            accent = Color.White.copy(alpha = 0.86f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .fillMaxWidth()
                .height(42.dp)
        )

        LyricsPlayerMenuSheet(
            show = lyricMenuExpanded,
            showPronunciation = showPronunciation,
            showTranslation = showTranslation,
            keepScreenOn = keepScreenOn,
            lyricFormatAvailability = lyricFormatAvailability,
            preferTtmlLyrics = preferTtmlLyrics,
            lyricSourceMode = lyricSourceMode,
            fontScale = fontScale,
            onDismiss = { lyricMenuExpanded = false },
            onTogglePronunciation = {
                lyricMenuExpanded = false
                onTogglePronunciation()
            },
            onToggleTranslation = {
                lyricMenuExpanded = false
                onToggleTranslation()
            },
            onToggleKeepScreenOn = {
                lyricMenuExpanded = false
                onToggleKeepScreenOn()
            },
            onLyricSourceMode = { mode ->
                lyricMenuExpanded = false
                onLyricSourceMode(mode)
            },
            onLyricFormatPreference = { preferTtml ->
                lyricMenuExpanded = false
                onLyricFormatPreference(preferTtml)
            },
            onFontScale = onFontScale,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
