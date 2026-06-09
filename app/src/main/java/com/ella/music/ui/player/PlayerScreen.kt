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
fun PlayerScreen(
    mainViewModel: MainViewModel,
    playerViewModel: PlayerViewModel,
    onBack: () -> Unit,
    onNavigateToAlbum: (Long) -> Unit = {},
    onNavigateToArtist: (String) -> Unit = {},
    onNavigateToMetadataCategory: (String, String) -> Unit = { _, _ -> },
    onDismissProgressChange: (Float) -> Unit = {},
    openToken: Int = 0
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val view = LocalView.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val settingsManager = remember { SettingsManager.getInstance(context) }
    val lyricFontPath by settingsManager.lyricFontPath.collectAsState(initial = "")
    val lyricFontWeightValue by settingsManager.lyricFontWeight.collectAsState(initial = 800)
    val lyricFontScaleValue by settingsManager.lyricFontScale.collectAsState(initial = 100)
    val lyricShareUseLyricFont by settingsManager.lyricShareUseLyricFont.collectAsState(initial = false)
    val playerTapSeekEnabled by settingsManager.playerTapSeekEnabled.collectAsState(initial = true)
    val playerShowTotalDuration by settingsManager.playerShowTotalDuration.collectAsState(initial = false)
    val lyricSourceMode by settingsManager.lyricSourceMode.collectAsState(initial = SettingsManager.LYRIC_SOURCE_AUTO)
    val transportButtonOutlines by settingsManager.transportButtonOutlines.collectAsState(initial = false)
    val bundledDefaultLyricFontPath = remember(context) { ensureBundledMiSansSemiboldPath(context) }
    val preferBundledLyricFontByDefault = remember { !isXiaomiFamilyPlayerDevice() }
    val defaultLyricFontPath = remember(preferBundledLyricFontByDefault, bundledDefaultLyricFontPath) {
        bundledDefaultLyricFontPath.takeIf { preferBundledLyricFontByDefault }
    }
    val effectiveLyricFontPath = remember(lyricFontPath, defaultLyricFontPath) {
        lyricFontPath.ifBlank { defaultLyricFontPath.orEmpty() }
    }
    val effectiveLyricFontWeightValue = remember(lyricFontWeightValue, lyricFontPath, defaultLyricFontPath) {
        when {
            lyricFontPath.isNotBlank() -> lyricFontWeightValue
            defaultLyricFontPath != null -> 800
            else -> lyricFontWeightValue
        }
    }
    val defaultLyricFontFamily = remember(preferBundledLyricFontByDefault) {
        if (!preferBundledLyricFontByDefault) {
            null
        } else {
            FontFamily(
                Font(
                    resId = R.font.misans_semibold,
                    weight = FontWeight(800)
                )
            )
        }
    }
    val lyricFontFamily = remember(effectiveLyricFontPath, effectiveLyricFontWeightValue, defaultLyricFontFamily) {
        effectiveLyricFontPath.toPlayerLyricFontFamily(
            weight = effectiveLyricFontWeightValue,
            italic = false
        ) ?: defaultLyricFontFamily
    }
    val lyricFontWeight = remember(effectiveLyricFontWeightValue) {
        FontWeight(effectiveLyricFontWeightValue.coerceIn(100, 900))
    }
    val lyricFontScale = remember(lyricFontScaleValue) { lyricFontScaleValue.coerceIn(75, 130) / 100f }
    val lyricShareTypeface = remember(lyricShareUseLyricFont, effectiveLyricFontPath, effectiveLyricFontWeightValue) {
        if (lyricShareUseLyricFont) {
            effectiveLyricFontPath.toPlayerLyricTypeface(effectiveLyricFontWeightValue)
        } else {
            null
        }
    }
    val currentSong by playerViewModel.currentSong.collectAsState()
    val isPlaying by playerViewModel.isPlaying.collectAsState()
    val currentPosition = rememberThrottledPlayerPosition(
        positionFlow = playerViewModel.currentPosition,
        isPlaying = isPlaying,
        anchorKey = currentSong?.id
    )
    val duration by playerViewModel.duration.collectAsState()
    val shuffleEnabled by playerViewModel.shuffleEnabled.collectAsState()
    val repeatMode by playerViewModel.repeatMode.collectAsState()
    val playbackSpeed by playerViewModel.playbackSpeed.collectAsState()
    val playbackPitch by playerViewModel.playbackPitch.collectAsState()
    val audioSessionId by PlaybackAudioSession.audioSessionId.collectAsState()
    val audioVisualizerEnabled by settingsManager.audioVisualizerEnabled.collectAsState(initial = false)
    val dynamicCoverEnabled by settingsManager.dynamicCoverEnabled.collectAsState(initial = false)
    val immersiveAlbumCover by settingsManager.playerImmersiveCover.collectAsState(initial = true)
    val playerBackgroundEnabled by settingsManager.playerBackgroundEnabled.collectAsState(initial = false)
    val playerBackgroundUri by settingsManager.playerBackgroundUri.collectAsState(initial = "")
    val hiResLogoEnabled by settingsManager.hiResLogoEnabled.collectAsState(initial = false)
    val hiResLogoUri by settingsManager.hiResLogoUri.collectAsState(initial = "")
    val lyricShareCustomInfo by settingsManager.lyricShareCustomInfo.collectAsState(initial = "")
    val metadataEditorId by settingsManager.metadataEditorId.collectAsState(initial = TagEditorOptionIds.ASK_EACH_TIME)
    val lyricTimingEditorId by settingsManager.lyricTimingEditorId.collectAsState(initial = TagEditorOptionIds.ASK_EACH_TIME)
    val sleepTimerCustomMinutes by settingsManager.sleepTimerCustomMinutes.collectAsState(initial = 45)
    val sleepTimerStopAfterCurrent by settingsManager.sleepTimerStopAfterCurrent.collectAsState(initial = false)
    val playlists by mainViewModel.playlists.collectAsState()
    val playlist by playerViewModel.playlist.collectAsState()
    val lyrics by playerViewModel.lyrics.collectAsState()
    val lyricFormatAvailability by playerViewModel.lyricFormatAvailability.collectAsState()
    val preferTtmlLyrics by playerViewModel.preferTtmlLyrics.collectAsState()
    val currentLyricIndex by playerViewModel.currentLyricIndex.collectAsState()
    val showLyrics by playerViewModel.showLyrics.collectAsState()
    val showLyricTranslation by playerViewModel.showLyricTranslation.collectAsState()
    val showLyricPronunciation by playerViewModel.showLyricPronunciation.collectAsState()
    val lyricPageKeepScreenOn by settingsManager.lyricPageKeepScreenOn.collectAsState(initial = false)
    val smoothLyricView by settingsManager.smoothLyricView.collectAsState(initial = false)
    val lyricPerspectiveEffect by settingsManager.lyricPerspectiveEffect.collectAsState(initial = false)
    val favoriteSongKeys by playerViewModel.favoriteSongKeys.collectAsState()
    val sleepTimerEndRealtimeMs by playerViewModel.sleepTimerEndRealtimeMs.collectAsState()
    val stopAfterCurrentEnabled by playerViewModel.stopAfterCurrentEnabled.collectAsState()
    val currentLyricLine = lyrics.getOrNull(currentLyricIndex)
    val miniLyricLine = currentLyricLine
        ?.takeIf { it.hasMiniLyric() }
        ?: lyrics.firstOrNull { it.hasMiniLyric() }
    var menuExpanded by remember { mutableStateOf(false) }
    var dynamicCoverSheetSong by remember { mutableStateOf<Song?>(null) }
    var songInfoExpanded by remember { mutableStateOf(false) }
    var queueExpanded by remember { mutableStateOf(false) }
    var artistChoices by remember { mutableStateOf<List<String>>(emptyList()) }
    var playlistPickerSong by remember { mutableStateOf<Song?>(null) }
    var playlistPickerSongs by remember { mutableStateOf<List<Song>?>(null) }
    var createPlaylistSong by remember { mutableStateOf<Song?>(null) }
    var createPlaylistSongs by remember { mutableStateOf<List<Song>?>(null) }
    var ratingSheetSong by remember { mutableStateOf<Song?>(null) }
    var aiSheetSong by remember { mutableStateOf<Song?>(null) }
    var deleteConfirmSong by remember { mutableStateOf<Song?>(null) }
    var pendingWriteRetry by remember { mutableStateOf<(suspend () -> Unit)?>(null) }
    var landscapeExpanded by rememberSaveable { mutableStateOf(false) }
    var landscapeCoverMode by rememberSaveable { mutableStateOf(false) }
    var dynamicCoverFailedPath by remember { mutableStateOf<String?>(null) }
    var hasVisualizerPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }
    val effectiveAudioVisualizerEnabled = immersiveAlbumCover &&
        audioVisualizerEnabled &&
        hasVisualizerPermission &&
        isPlaying &&
        !showLyrics &&
        !landscapeExpanded
    val visualizerPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasVisualizerPermission = granted
        scope.launch {
            settingsManager.setAudioVisualizerEnabled(granted)
        }
        if (!granted) Toast.makeText(context, context.getString(R.string.player_need_record_audio_permission), Toast.LENGTH_SHORT).show()
    }
    fun setAudioVisualizerEnabled(enabled: Boolean) {
        if (enabled && !immersiveAlbumCover) {
            Toast.makeText(context, context.getString(R.string.player_visualizer_immersive_only), Toast.LENGTH_SHORT).show()
            return
        }
        if (enabled && !hasVisualizerPermission) {
            visualizerPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        } else {
            scope.launch {
                settingsManager.setAudioVisualizerEnabled(enabled)
            }
        }
    }
    val deletePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            pendingWriteRetry?.let { retry ->
                scope.launch { retry() }
            }
            pendingWriteRetry = null
        } else {
            pendingWriteRetry = null
        }
    }
    val dragDismissOffset = remember { Animatable(0f) }
    var dismissingPlayer by remember { mutableStateOf(false) }
    val topDragLimitPx = with(density) { 132.dp.toPx() }
    val dismissThresholdPx = with(density) { 240.dp.toPx() }
    val dismissVelocityThresholdPx = with(density) { 1250.dp.toPx() }
    val dismissTargetPx = remember(view.height) {
        view.height.takeIf { it > 0 }?.toFloat() ?: with(density) { 760.dp.toPx() }
    }
    LaunchedEffect(landscapeExpanded) {
        setPlayerSystemBars(context.findActivity(), view)
    }
    DisposableEffect(view, showLyrics, lyricPageKeepScreenOn) {
        val previousKeepScreenOn = view.keepScreenOn
        view.keepScreenOn = previousKeepScreenOn || (showLyrics && lyricPageKeepScreenOn)
        onDispose {
            view.keepScreenOn = previousKeepScreenOn
        }
    }
    val dismissProgress = (dragDismissOffset.value / dismissThresholdPx).coerceIn(0f, 1f)
    val dismissInProgress = dismissProgress > 0.001f || dismissingPlayer
    val dragCornerRadius = 30.dp * dismissProgress
    LaunchedEffect(openToken) {
        dismissingPlayer = false
        dragDismissOffset.snapTo(0f)
        onDismissProgressChange(0f)
    }
    SideEffect {
        onDismissProgressChange(dismissProgress)
    }
    DisposableEffect(Unit) {
        onDispose { onDismissProgressChange(0f) }
    }
    fun dismissWithPlayerMotion() {
        if (dismissingPlayer) return
        scope.launch {
            if (dismissingPlayer) return@launch
            dismissingPlayer = true
            dragDismissOffset.stop()
            dragDismissOffset.animateTo(
                targetValue = dismissTargetPx,
                animationSpec = tween(durationMillis = 260, easing = LinearOutSlowInEasing)
            )
            playerViewModel.setShowLyrics(false)
            onBack()
        }
    }

    val song = currentSong
    val isCurrentSongFavorite = song?.playlistIdentityKey()?.let { it in favoriteSongKeys } == true
    fun requestDeleteSong(targetSong: Song) {
        deleteConfirmSong = targetSong
    }
    val embeddedCover by produceState<Bitmap?>(initialValue = null, song?.id, song?.dateModified, song?.fileSize) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                CoverLoadLimiter.run { song?.takeIf { it.coverUrl.isBlank() }?.let(playerViewModel::getCoverArtBitmap) }
            }.getOrNull()
        }
    }
    val paletteBitmap by produceState<Bitmap?>(initialValue = null, song?.id, song?.albumId, song?.coverUrl, song?.dateModified, song?.fileSize, embeddedCover) {
        value = withContext(Dispatchers.IO) {
            embeddedCover ?: song?.let { loadPaletteCoverBitmap(context, it) }
        }
    }
    val palette by produceState(initialValue = PlayerPalette.Default, paletteBitmap) {
        value = withContext(Dispatchers.Default) { PlayerPalette.from(paletteBitmap) }
    }
    val lyricPalette by produceState(initialValue = PlayerPalette.Default, paletteBitmap) {
        value = withContext(Dispatchers.Default) { PlayerPalette.fromLyricBackground(paletteBitmap) }
    }
    val audioInfo by produceState<AudioInfo?>(initialValue = null, song?.id, song?.dateModified, song?.fileSize) {
        value = withContext(Dispatchers.IO) { song?.let(playerViewModel::getAudioInfo) }
    }
    val tagInfo by produceState<SongTagInfo?>(initialValue = null, song?.id, song?.dateModified, song?.fileSize) {
        value = withContext(Dispatchers.IO) { song?.let(playerViewModel::getSongTagInfo) }
    }
    val songAnnotation = tagInfo?.displayComment.orEmpty()
    var lyricShareInitialLine by remember { mutableStateOf<LyricLine?>(null) }
    fun openLyricSharePicker(line: LyricLine) {
        lyricShareInitialLine = line
    }
    fun shareSelectedLyrics(lines: List<LyricLine>, includeTranslation: Boolean) {
        shareLyricCard(
            context = context,
            song = song,
            lines = lines,
            cover = embeddedCover ?: paletteBitmap,
            backgroundColors = listOf(
                palette.top.toArgb(),
                palette.middle.toArgb(),
                palette.bottom.toArgb()
            ),
            annotation = songAnnotation,
            customInfo = lyricShareCustomInfo,
            shareTypeface = lyricShareTypeface,
            includeTranslation = includeTranslation
        )
        lyricShareInitialLine = null
    }
    val neteaseInfo = remember(tagInfo?.neteaseKey) { decodeNeteaseKey(tagInfo?.neteaseKey.orEmpty()) }
    fun navigateToArtistOrChoose(artistText: String) {
        val artists = splitArtistNames(artistText)
            .distinctBy { it.tagIdentityKey() }
        when (artists.size) {
            0 -> Toast.makeText(context, context.getString(R.string.player_no_artist_jump), Toast.LENGTH_SHORT).show()
            1 -> onNavigateToArtist(artists.first())
            else -> artistChoices = artists
        }
    }
    fun openNetease(url: String?) {
        if (url.isNullOrBlank()) {
            Toast.makeText(context, context.getString(R.string.player_no_netease_jump), Toast.LENGTH_SHORT).show()
        } else {
            uriHandler.openUri(url)
        }
    }
    val playerPagerState = rememberPagerState(
        initialPage = PLAYER_PAGE_COVER,
        pageCount = { PLAYER_PAGE_COUNT }
    )
    LaunchedEffect(showLyrics) {
        if (immersiveAlbumCover) return@LaunchedEffect
        val target = if (showLyrics) PLAYER_PAGE_LYRICS else PLAYER_PAGE_COVER
        if (showLyrics && playerPagerState.currentPage != target && !playerPagerState.isScrollInProgress) {
            playerPagerState.animateScrollToPage(target)
        } else if (!showLyrics && playerPagerState.currentPage == PLAYER_PAGE_LYRICS && !playerPagerState.isScrollInProgress) {
            playerPagerState.animateScrollToPage(target)
        }
    }
    LaunchedEffect(playerPagerState.currentPage) {
        if (immersiveAlbumCover) return@LaunchedEffect
        val lyricPageVisible = playerPagerState.currentPage == PLAYER_PAGE_LYRICS
        if (showLyrics != lyricPageVisible) {
            playerViewModel.setShowLyrics(lyricPageVisible)
        }
    }
    LaunchedEffect(immersiveAlbumCover) {
        if (immersiveAlbumCover && playerPagerState.currentPage != PLAYER_PAGE_COVER) {
            playerViewModel.setShowLyrics(false)
            playerPagerState.scrollToPage(PLAYER_PAGE_COVER)
        }
    }
    BackHandler { dismissWithPlayerMotion() }

    @Composable
    fun CoverPageContent(
        onShowLyrics: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        var actionMenuInitialPage by remember { mutableStateOf(PlayerActionSheetPage.Main) }
        CoverPlayerPage(
            context = context,
            mainViewModel = mainViewModel,
            playerViewModel = playerViewModel,
            song = song,
            embeddedCover = embeddedCover,
            annotation = songAnnotation,
            dynamicCoverFailedPath = dynamicCoverFailedPath,
            dynamicCoverEnabled = dynamicCoverEnabled,
            immersiveAlbumCover = immersiveAlbumCover,
            playerBackgroundEnabled = playerBackgroundEnabled,
            playerBackgroundUri = playerBackgroundUri,
            hiResLogoEnabled = hiResLogoEnabled,
            hiResLogoUri = hiResLogoUri,
            isPlaying = isPlaying,
            currentPosition = currentPosition,
            duration = duration,
            shuffleEnabled = shuffleEnabled,
            repeatMode = repeatMode,
            audioInfo = audioInfo,
            palette = if (immersiveAlbumCover) palette else lyricPalette,
            flowEffectMode = SettingsManager.PLAYER_FLOW_EFFECT_DARK,
            dynamicFlowEnabled = false,
            lyrics = lyrics,
            currentLyricIndex = currentLyricIndex,
            miniLyricLine = miniLyricLine,
            showTranslation = showLyricTranslation,
            showPronunciation = showLyricPronunciation,
            fontFamily = lyricFontFamily,
            fontPath = effectiveLyricFontPath,
            fontWeight = lyricFontWeight,
            fontScale = lyricFontScale,
            playerTapSeekEnabled = playerTapSeekEnabled,
            playerShowTotalDuration = playerShowTotalDuration,
            menuExpanded = menuExpanded,
            queueExpanded = queueExpanded,
            playlist = playlist,
            sleepTimerEndRealtimeMs = sleepTimerEndRealtimeMs,
            stopAfterCurrentEnabled = stopAfterCurrentEnabled,
            sleepTimerCustomMinutes = sleepTimerCustomMinutes,
            sleepTimerStopAfterCurrent = sleepTimerStopAfterCurrent,
            onDynamicCoverFailed = { dynamicCoverFailedPath = it },
            onMatchDynamicCover = {
                menuExpanded = false
                dynamicCoverSheetSong = song
            },
            onToggleMenu = {
                actionMenuInitialPage = PlayerActionSheetPage.Main
                menuExpanded = !menuExpanded
            },
            onToggleFavorite = { playerViewModel.toggleCurrentSongFavorite() },
            onDismissMenu = { menuExpanded = false },
            onToggleQueue = { queueExpanded = !queueExpanded },
            onDismissQueue = { queueExpanded = false },
            onShowLyrics = onShowLyrics,
            onLyricLineClick = { line -> playerViewModel.seekTo(line.timeMs) },
            onLyricLineLongClick = ::openLyricSharePicker,
            onSeek = { fraction -> playerViewModel.seekTo((fraction * duration).toLong()) },
            onCyclePlaybackMode = { playerViewModel.cyclePlaybackMode() },
            onPrevious = { playerViewModel.skipToPrevious() },
            onPlayPause = { playerViewModel.togglePlayPause() },
            onNext = { playerViewModel.skipToNext() },
            onQueueSongClick = { index ->
                queueExpanded = false
                playerViewModel.playQueueIndex(index)
            },
            onRemoveQueueSong = { index ->
                playerViewModel.removeFromPlaylist(index)
            },
            onMoveQueueSong = { fromIndex, toIndex ->
                playerViewModel.movePlaylistItem(fromIndex, toIndex)
            },
            onAddQueueToPlaylist = {
                queueExpanded = false
                playlistPickerSongs = playlist
            },
            onClearQueue = {
                queueExpanded = false
                playerViewModel.clearPlaylist()
            },
            onAlbum = {
                menuExpanded = false
                val albumId = song?.albumIdentityId() ?: 0L
                if (albumId > 0L) onNavigateToAlbum(albumId)
                else Toast.makeText(context, context.getString(R.string.player_no_album_jump), Toast.LENGTH_SHORT).show()
            },
            onArtist = {
                menuExpanded = false
                navigateToArtistOrChoose(song?.artist.orEmpty())
            },
            onNavigateToAlbumId = onNavigateToAlbum,
            onNavigateToArtistName = onNavigateToArtist,
            onDownload = {
                menuExpanded = false
                val current = song
                if (current != null) {
                    enqueuePlayerDownload(context, current)
                    Toast.makeText(context, context.getString(R.string.player_download_started), Toast.LENGTH_SHORT).show()
                }
            },
            onLandscape = {
                menuExpanded = false
                landscapeCoverMode = false
                landscapeExpanded = true
            },
            onSongInfo = {
                menuExpanded = false
                songInfoExpanded = true
            },
            onAddToPlaylist = {
                val current = song
                if (current != null) {
                    menuExpanded = false
                    playlistPickerSong = current
                } else {
                    Toast.makeText(context, context.getString(R.string.player_no_song_playing), Toast.LENGTH_SHORT).show()
                }
            },
            onShareSong = {
                val current = song
                if (current != null) shareLocalSong(context, current)
                else Toast.makeText(context, context.getString(R.string.player_no_share_song), Toast.LENGTH_SHORT).show()
            },
            onAddToQueue = {
                val current = song
                if (current != null) {
                    playerViewModel.addToPlaylist(current)
                    menuExpanded = false
                    Toast.makeText(context, context.getString(R.string.song_more_added_to_queue), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, context.getString(R.string.player_no_song_playing), Toast.LENGTH_SHORT).show()
                }
            },
            onPlayNext = {
                val current = song
                if (current != null) {
                    playerViewModel.playNext(current)
                    menuExpanded = false
                    Toast.makeText(context, context.getString(R.string.song_more_added_to_play_next), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, context.getString(R.string.player_no_song_playing), Toast.LENGTH_SHORT).show()
                }
            },
            onSetRating = {
                val current = song
                if (current != null) {
                    menuExpanded = false
                    ratingSheetSong = current
                } else {
                    Toast.makeText(context, context.getString(R.string.player_no_song_playing), Toast.LENGTH_SHORT).show()
                }
            },
            onAiInterpret = {
                val current = song
                if (current != null) {
                    menuExpanded = false
                    aiSheetSong = current
                } else {
                    Toast.makeText(context, context.getString(R.string.player_no_song_playing), Toast.LENGTH_SHORT).show()
                }
            },
            onSpectrum = {
                val current = song
                if (current != null) {
                    menuExpanded = false
                    openSongSpectrumWithAspectPro(context, current)
                } else {
                    Toast.makeText(context, context.getString(R.string.player_no_song_playing), Toast.LENGTH_SHORT).show()
                }
            },
            onDeleteSong = {
                val current = song
                if (current != null) {
                    menuExpanded = false
                    requestDeleteSong(current)
                } else {
                    Toast.makeText(context, context.getString(R.string.player_no_song_playing), Toast.LENGTH_SHORT).show()
                }
            },
            onOpenTimer = {
                actionMenuInitialPage = PlayerActionSheetPage.Timer
                menuExpanded = true
            },
            onOpenMetadataEditor = {
                val metadataOptions = song
                    ?.let { buildTagEditorOptions(context, it) }
                    .orEmpty()
                    .filter { it.kind == TagEditorOptionKind.Metadata }
                val preferredOption = metadataEditorId
                    .takeIf { it.isNotBlank() }
                    ?.let { id -> metadataOptions.firstOrNull { it.id == id } }
                if (preferredOption != null) {
                    launchTagEditorOption(context, preferredOption)
                    menuExpanded = false
                } else {
                    actionMenuInitialPage = PlayerActionSheetPage.MetadataEditor
                    menuExpanded = true
                }
            },
            onStopAfterCurrent = {
                scope.launch { settingsManager.setSleepTimerStopAfterCurrent(it) }
                if (sleepTimerEndRealtimeMs == null) {
                    playerViewModel.setStopAfterCurrentEnabled(it)
                } else if (!it) {
                    playerViewModel.setStopAfterCurrentEnabled(false)
                }
                Toast.makeText(
                    context,
                    if (it) context.getString(R.string.player_pause_after_current_on) else context.getString(R.string.player_pause_after_current_off),
                    Toast.LENGTH_SHORT
                ).show()
            },
            onTimer = { minutes ->
                scope.launch { settingsManager.setSleepTimerCustomMinutes(minutes) }
                playerViewModel.setStopAfterCurrentEnabled(false)
                playerViewModel.startSleepTimer(
                    minutes = minutes,
                    stopAfterCurrentWhenExpired = sleepTimerStopAfterCurrent
                )
                Toast.makeText(context, context.getString(R.string.player_sleep_timer_minutes, minutes), Toast.LENGTH_SHORT).show()
            },
            onCustomTimerMinutes = { minutes ->
                scope.launch { settingsManager.setSleepTimerCustomMinutes(minutes) }
            },
            onCancelTimer = {
                playerViewModel.cancelSleepTimer()
                Toast.makeText(context, context.getString(R.string.player_sleep_timer_cancelled), Toast.LENGTH_SHORT).show()
            },
            onSpeed = { playerViewModel.setPlaybackSpeed(it) },
            onPitch = { playerViewModel.setPlaybackPitch(it) },
            playbackSpeed = playbackSpeed,
            playbackPitch = playbackPitch,
            isFavorite = isCurrentSongFavorite,
            audioSessionId = audioSessionId,
            visualizerEnabled = audioVisualizerEnabled,
            metadataEditorId = metadataEditorId,
            lyricTimingEditorId = lyricTimingEditorId,
            onVisualizerEnabled = ::setAudioVisualizerEnabled,
            actionMenuInitialPage = actionMenuInitialPage,
            modifier = modifier
        )
    }

    @Composable
    fun LyricsPageContent(
        onDismissLyrics: () -> Unit,
        enableSwipeDismiss: Boolean,
        modifier: Modifier = Modifier
    ) {
        LyricsPlayerPage(
            song = song,
            embeddedCover = embeddedCover,
            annotation = songAnnotation,
            lyrics = lyrics,
            currentLyricIndex = currentLyricIndex,
            currentPosition = currentPosition,
            showTranslation = showLyricTranslation,
            showPronunciation = showLyricPronunciation,
            keepScreenOn = lyricPageKeepScreenOn,
            lyricFormatAvailability = lyricFormatAvailability,
            preferTtmlLyrics = preferTtmlLyrics,
            lyricSourceMode = lyricSourceMode,
            fontFamily = lyricFontFamily,
            fontPath = effectiveLyricFontPath,
            fontWeight = lyricFontWeight,
            italic = false,
            fontScale = lyricFontScale,
            perspectiveEffect = lyricPerspectiveEffect,
            palette = lyricPalette,
            flowEffectMode = SettingsManager.PLAYER_FLOW_EFFECT_DARK,
            currentPositionMs = currentPosition,
            isPlaying = isPlaying,
            playerBackgroundEnabled = playerBackgroundEnabled,
            playerBackgroundUri = playerBackgroundUri,
            isFavorite = isCurrentSongFavorite,
            audioSessionId = audioSessionId,
            visualizerEnabled = effectiveAudioVisualizerEnabled,
            onLineClick = { line -> playerViewModel.seekTo(line.timeMs) },
            onLineDoubleClick = { playerViewModel.togglePlayPause() },
            onLineLongClick = ::openLyricSharePicker,
            onDismissLyrics = onDismissLyrics,
            onTogglePronunciation = {
                playerViewModel.setLyricPagePronunciation(!showLyricPronunciation)
            },
            onToggleTranslation = {
                playerViewModel.setLyricPageTranslation(!showLyricTranslation)
            },
            onToggleKeepScreenOn = {
                scope.launch { settingsManager.setLyricPageKeepScreenOn(!lyricPageKeepScreenOn) }
            },
            onToggleFavorite = { playerViewModel.toggleCurrentSongFavorite() },
            onFontScale = { scale ->
                scope.launch { settingsManager.setLyricFontScale((scale * 100f).toInt()) }
            },
            onLyricSourceMode = { mode ->
                playerViewModel.setLyricSourceMode(mode)
            },
            onLyricFormatPreference = { preferTtml ->
                playerViewModel.setLyricFormatPreference(preferTtml)
            },
            onArtist = {
                navigateToArtistOrChoose(song?.artist.orEmpty())
            },
            enableSwipeDismiss = enableSwipeDismiss,
            useBlurBackground = immersiveAlbumCover,
            modifier = modifier
        )
    }

    @Composable
    fun DetailPageContent(modifier: Modifier = Modifier) {
        PlayerDetailPage(
            song = song,
            tagInfo = tagInfo,
            neteaseInfo = neteaseInfo,
            customBackgroundUri = playerBackgroundUri.takeIf {
                !immersiveAlbumCover && playerBackgroundEnabled && playerBackgroundUri.isNotBlank()
            }.orEmpty(),
            onAlbum = {
                val albumId = song?.albumIdentityId() ?: 0L
                if (albumId > 0L) onNavigateToAlbum(albumId)
                else Toast.makeText(context, context.getString(R.string.player_no_album_jump), Toast.LENGTH_SHORT).show()
            },
            onArtist = { name -> onNavigateToArtist(name) },
            onComposer = { name -> onNavigateToMetadataCategory("composer", name) },
            onLyricist = { name -> onNavigateToMetadataCategory("lyricist", name) },
            onNeteaseSong = { openNetease(neteaseInfo?.musicId?.takeIf { it.isNotBlank() }?.let(::neteaseSongUrl)) },
            onNeteaseArtist = { id -> openNetease(neteaseArtistUrl(id)) },
            onNeteaseAlbum = { openNetease(neteaseInfo?.albumId?.takeIf { it.isNotBlank() }?.let(::neteaseAlbumUrl)) },
            modifier = modifier
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .pointerInput(showLyrics, dismissingPlayer, dismissTargetPx, dismissThresholdPx) {
                var closeGesture = false
                var gestureOffset = 0f
                val velocityTracker = VelocityTracker()
                detectDragGestures(
                    onDragStart = { offset ->
                        closeGesture = !dismissingPlayer && offset.y <= topDragLimitPx
                        gestureOffset = dragDismissOffset.value
                        velocityTracker.resetTracking()
                        velocityTracker.addPosition(SystemClock.uptimeMillis(), offset)
                        if (closeGesture) {
                            scope.launch { dragDismissOffset.stop() }
                        }
                    },
                    onDrag = { change, dragAmount ->
                        if (!closeGesture) return@detectDragGestures
                        gestureOffset = (gestureOffset + if (dragAmount.y > 0f) {
                            dragAmount.y
                        } else {
                            dragAmount.y * 0.36f
                        }).coerceIn(0f, dismissTargetPx)
                        velocityTracker.addPosition(change.uptimeMillis, change.position)
                        scope.launch { dragDismissOffset.snapTo(gestureOffset) }
                        if (gestureOffset > 0f) change.consume()
                    },
                    onDragCancel = {
                        closeGesture = false
                        scope.launch {
                            dragDismissOffset.animateTo(
                                targetValue = 0f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            )
                        }
                    },
                    onDragEnd = {
                        if (!closeGesture) return@detectDragGestures
                        closeGesture = false
                        val velocityY = velocityTracker.calculateVelocity().y
                        scope.launch {
                            if (gestureOffset >= dismissThresholdPx || velocityY >= dismissVelocityThresholdPx) {
                                if (!dismissingPlayer) {
                                    dismissingPlayer = true
                                    dragDismissOffset.animateTo(
                                        targetValue = dismissTargetPx,
                                        animationSpec = tween(durationMillis = 260, easing = LinearOutSlowInEasing)
                                    )
                                    playerViewModel.setShowLyrics(false)
                                    onBack()
                                }
                            } else {
                                dragDismissOffset.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                )
                            }
                        }
                    }
                )
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationY = dragDismissOffset.value
                    scaleX = 1f
                    scaleY = 1f
                    transformOrigin = TransformOrigin(0.5f, 0f)
                    alpha = 1f
                }
                .clip(
                    RoundedCornerShape(
                        topStart = dragCornerRadius,
                        topEnd = dragCornerRadius
                    )
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                palette.top,
                                palette.middle,
                                palette.bottom
                            )
                        )
                    )
            )
            if (immersiveAlbumCover) {
                ImmersiveCoverBackground(
                    palette = palette,
                    flowEffectMode = SettingsManager.PLAYER_FLOW_EFFECT_DARK,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                ImmersiveCoverBackground(
                    palette = palette,
                    flowEffectMode = SettingsManager.PLAYER_FLOW_EFFECT_DARK,
                    modifier = Modifier.fillMaxSize()
                )
            }

            if (immersiveAlbumCover) {
                if (showLyrics) {
                    LyricsPageContent(
                        onDismissLyrics = { playerViewModel.setShowLyrics(false) },
                        enableSwipeDismiss = true,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    CoverPageContent(
                        onShowLyrics = { playerViewModel.setShowLyrics(true) },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else {
                HorizontalPager(
                    state = playerPagerState,
                    modifier = Modifier.fillMaxSize(),
                    userScrollEnabled = !dismissingPlayer,
                    beyondViewportPageCount = 1
                ) { page ->
                    when (page) {
                        PLAYER_PAGE_COVER -> CoverPageContent(
                            onShowLyrics = {
                                scope.launch { playerPagerState.animateScrollToPage(PLAYER_PAGE_LYRICS) }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                        PLAYER_PAGE_LYRICS -> LyricsPageContent(
                            onDismissLyrics = {
                                scope.launch { playerPagerState.animateScrollToPage(PLAYER_PAGE_COVER) }
                            },
                            enableSwipeDismiss = false,
                            modifier = Modifier.fillMaxSize()
                        )
                        PLAYER_PAGE_DETAILS -> DetailPageContent(modifier = Modifier.fillMaxSize())
                    }
                }
            }

            if (landscapeExpanded) {
                ForceLandscapePlayerBars(
                    onDismiss = {
                        landscapeExpanded = false
                        landscapeCoverMode = false
                    }
                )
                if (landscapeCoverMode) {
                    val landscapeDynamicCoverSource = if (dynamicCoverEnabled) {
                        song
                            ?.dynamicCoverSource(context)
                            ?.takeUnless { it.failureKey == dynamicCoverFailedPath }
                    } else {
                        null
                    }
                    LandscapeCoverPlaybackOverlay(
                        song = song,
                        embeddedCover = embeddedCover,
                        annotation = songAnnotation,
                        dynamicCoverSource = landscapeDynamicCoverSource,
                        isPlaying = isPlaying,
                        currentPosition = currentPosition,
                        duration = duration,
                        shuffleEnabled = shuffleEnabled,
                        repeatMode = repeatMode,
                        audioInfo = audioInfo,
                        palette = lyricPalette,
                        lyrics = lyrics,
                        currentLyricIndex = currentLyricIndex,
                        showTranslation = showLyricTranslation,
                        showPronunciation = showLyricPronunciation,
                        fontFamily = lyricFontFamily,
                        fontPath = effectiveLyricFontPath,
                        fontWeight = lyricFontWeight,
                        fontScale = lyricFontScale,
                        queueExpanded = queueExpanded,
                        playlist = playlist,
                        audioSessionId = audioSessionId,
                        visualizerEnabled = effectiveAudioVisualizerEnabled,
                        flowEffectMode = SettingsManager.PLAYER_FLOW_EFFECT_DARK,
                        onDynamicCoverFailed = { dynamicCoverFailedPath = it },
                        isFavorite = isCurrentSongFavorite,
                        onToggleFavorite = { playerViewModel.toggleCurrentSongFavorite() },
                        onToggleQueue = { queueExpanded = !queueExpanded },
                        onDismissQueue = { queueExpanded = false },
                        onShowLyrics = { landscapeCoverMode = false },
                        onLyricLineClick = { line -> playerViewModel.seekTo(line.timeMs) },
                        onLyricLineLongClick = ::openLyricSharePicker,
                        onSeek = { progress ->
                            if (duration > 0L) playerViewModel.seekTo((duration * progress).toLong())
                        },
                        onCyclePlaybackMode = { playerViewModel.cyclePlaybackMode() },
                        onPrevious = { playerViewModel.skipToPrevious() },
                        onPlayPause = { playerViewModel.togglePlayPause() },
                        onNext = { playerViewModel.skipToNext() },
                        onQueueSongClick = { index ->
                            queueExpanded = false
                            playerViewModel.playQueueIndex(index)
                        },
                        onRemoveQueueSong = { index -> playerViewModel.removeFromPlaylist(index) },
                        onMoveQueueSong = { fromIndex, toIndex ->
                            playerViewModel.movePlaylistItem(fromIndex, toIndex)
                        },
                        onAddQueueToPlaylist = {
                            queueExpanded = false
                            playlistPickerSongs = playlist
                        },
                        onClearQueue = {
                            queueExpanded = false
                            playerViewModel.clearPlaylist()
                        },
                        onArtist = {
                            navigateToArtistOrChoose(song?.artist.orEmpty())
                        },
                        onDismiss = {
                            landscapeExpanded = false
                            landscapeCoverMode = false
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    LandscapeLyricsOverlay(
                        song = song,
                        embeddedCover = embeddedCover,
                        annotation = songAnnotation,
                        lyrics = lyrics,
                        currentLyricIndex = currentLyricIndex,
                        currentPosition = currentPosition,
                        duration = duration,
                        shuffleEnabled = shuffleEnabled,
                        repeatMode = repeatMode,
                        showTranslation = showLyricTranslation,
                        showPronunciation = showLyricPronunciation,
                        fontFamily = lyricFontFamily,
                        fontPath = effectiveLyricFontPath,
                        fontWeight = lyricFontWeight,
                        fontScale = lyricFontScale,
                        showTotalDuration = playerShowTotalDuration,
                        palette = lyricPalette,
                        flowEffectMode = SettingsManager.PLAYER_FLOW_EFFECT_DARK,
                        isPlaying = isPlaying,
                        audioSessionId = audioSessionId,
                        visualizerEnabled = effectiveAudioVisualizerEnabled,
                        onLineClick = { line -> playerViewModel.seekTo(line.timeMs) },
                        onLineLongClick = ::openLyricSharePicker,
                        onSeek = { progress ->
                            if (duration > 0L) playerViewModel.seekTo((duration * progress).toLong())
                        },
                        onCyclePlaybackMode = { playerViewModel.cyclePlaybackMode() },
                        onPrevious = { playerViewModel.skipToPrevious() },
                        onPlayPause = { playerViewModel.togglePlayPause() },
                        onNext = { playerViewModel.skipToNext() },
                        onShowCoverPlayer = { landscapeCoverMode = true },
                        onDismiss = {
                            landscapeExpanded = false
                            landscapeCoverMode = false
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            PlayerScreenSheetHost(
                context = context,
                scope = scope,
                mainViewModel = mainViewModel,
                playerViewModel = playerViewModel,
                song = song,
                playlists = playlists,
                artistChoices = artistChoices,
                onArtistChoicesChange = { artistChoices = it },
                onNavigateToArtist = onNavigateToArtist,
                songInfoExpanded = songInfoExpanded,
                onSongInfoExpandedChange = { songInfoExpanded = it },
                dynamicCoverSheetSong = dynamicCoverSheetSong,
                onDynamicCoverSheetSongChange = { dynamicCoverSheetSong = it },
                ratingSheetSong = ratingSheetSong,
                onRatingSheetSongChange = { ratingSheetSong = it },
                aiSheetSong = aiSheetSong,
                onAiSheetSongChange = { aiSheetSong = it },
                deleteConfirmSong = deleteConfirmSong,
                onDeleteConfirmSongChange = { deleteConfirmSong = it },
                onWritePermissionRequired = { error, retry ->
                    pendingWriteRetry = retry
                    deletePermissionLauncher.launch(
                        IntentSenderRequest.Builder(error.intentSender).build()
                    )
                },
                playlistPickerSong = playlistPickerSong,
                onPlaylistPickerSongChange = { playlistPickerSong = it },
                playlistPickerSongs = playlistPickerSongs,
                onPlaylistPickerSongsChange = { playlistPickerSongs = it },
                createPlaylistSong = createPlaylistSong,
                onCreatePlaylistSongChange = { createPlaylistSong = it },
                createPlaylistSongs = createPlaylistSongs,
                onCreatePlaylistSongsChange = { createPlaylistSongs = it }
            )
        }

        PlayerLyricShareHost(
            song = song,
            lyrics = lyrics,
            initialLine = lyricShareInitialLine,
            embeddedCover = embeddedCover,
            paletteBitmap = paletteBitmap,
            palette = palette,
            annotation = songAnnotation,
            customInfo = lyricShareCustomInfo,
            shareTypeface = lyricShareTypeface,
            onDismiss = { lyricShareInitialLine = null },
            onShare = ::shareSelectedLyrics
        )
    }
}

private const val PLAYER_PAGE_DETAILS = 0
private const val PLAYER_PAGE_COVER = 1
private const val PLAYER_PAGE_LYRICS = 2
private const val PLAYER_PAGE_COUNT = 3
