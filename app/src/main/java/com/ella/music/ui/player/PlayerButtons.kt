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
