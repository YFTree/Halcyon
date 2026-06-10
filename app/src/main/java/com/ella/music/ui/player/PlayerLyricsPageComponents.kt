package com.ella.music.ui.player

import android.graphics.Bitmap
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ella.music.R
import com.ella.music.data.model.Song
import com.ella.music.data.repository.MusicRepository
import com.ella.music.ui.components.EllaMiuixBottomSheet

@Composable
internal fun LyricsPlayerHeader(
    song: Song?,
    embeddedCover: Bitmap?,
    annotation: String,
    isFavorite: Boolean,
    onDismissLyrics: () -> Unit,
    onArtist: () -> Unit,
    onToggleFavorite: () -> Unit,
    onShowMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SmallCover(
            song = song,
            embeddedCover = embeddedCover,
            modifier = Modifier
                .size(56.dp)
                .clickable(onClick = onDismissLyrics)
        )
        Spacer(modifier = Modifier.width(12.dp))
        PlayerSongMetaText(
            song = song,
            annotation = annotation,
            titleFontSize = 22.sp,
            artistFontSize = 14.sp,
            artistAlpha = 0.72f,
            onArtistClick = onArtist,
            modifier = Modifier
                .weight(1f)
                .widthIn(max = 230.dp)
        )
        Spacer(modifier = Modifier.width(20.dp))
        PlayerHeaderAction(
            kind = PlayerHeaderActionKind.Favorite,
            selected = isFavorite,
            onClick = onToggleFavorite
        )
        PlayerHeaderAction(kind = PlayerHeaderActionKind.More, onClick = onShowMenu)
    }
}

@Composable
internal fun LyricsPlayerMenuSheet(
    show: Boolean,
    showPronunciation: Boolean,
    showTranslation: Boolean,
    keepScreenOn: Boolean,
    lyricFormatAvailability: MusicRepository.LyricFormatAvailability,
    preferTtmlLyrics: Boolean?,
    lyricSourceMode: Int,
    fontScale: Float,
    onDismiss: () -> Unit,
    onTogglePronunciation: () -> Unit,
    onToggleTranslation: () -> Unit,
    onToggleKeepScreenOn: () -> Unit,
    onLyricSourceMode: (Int) -> Unit,
    onLyricFormatPreference: (Boolean) -> Unit,
    onFontScale: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!show) return
    EllaMiuixBottomSheet(
        show = true,
        enableNestedScroll = false,
        title = stringResource(R.string.player_lyrics_display),
        onDismissRequest = onDismiss
    ) {
        LyricActionMenu(
            showPronunciation = showPronunciation,
            showTranslation = showTranslation,
            keepScreenOn = keepScreenOn,
            lyricFormatAvailability = lyricFormatAvailability,
            preferTtmlLyrics = preferTtmlLyrics,
            lyricSourceMode = lyricSourceMode,
            fontScale = fontScale,
            onTogglePronunciation = onTogglePronunciation,
            onToggleTranslation = onToggleTranslation,
            onToggleKeepScreenOn = onToggleKeepScreenOn,
            onLyricSourceMode = onLyricSourceMode,
            onLyricFormatPreference = onLyricFormatPreference,
            onFontScale = onFontScale,
            modifier = modifier
        )
    }
}
