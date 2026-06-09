package com.ella.music.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ella.music.data.model.LyricLine
import com.ella.music.ui.components.SmoothLyricView

internal fun miniLyricsPreviewHeight(
    line: LyricLine?,
    showTranslation: Boolean,
    showPronunciation: Boolean,
    compact: Boolean = false
) = when (line?.miniVisiblePartCount(showTranslation, showPronunciation) ?: 1) {
    0, 1 -> if (compact) 132.dp else 168.dp
    2 -> if (compact) 150.dp else 190.dp
    3 -> if (compact) 162.dp else 206.dp
    else -> if (compact) 170.dp else 214.dp
}

@Composable
internal fun MiniLyricsPreview(
    songId: Long,
    songTitle: String,
    songArtist: String,
    lyrics: List<LyricLine>,
    currentIndex: Int,
    showTranslation: Boolean,
    showPronunciation: Boolean,
    currentPositionMs: Long,
    isPlaying: Boolean,
    fontPath: String = "",
    fontWeight: FontWeight = FontWeight.ExtraBold,
    fontScale: Float = 1f,
    onLineClick: (LyricLine) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val safeIndex = currentIndex.takeIf { it in lyrics.indices }
        ?: lyrics.indexOfFirst { it.hasMiniLyric() }.takeIf { it >= 0 }
        ?: return
    SmoothLyricView(
        songId = songId,
        songTitle = songTitle,
        songArtist = songArtist,
        lyrics = lyrics,
        currentIndex = safeIndex,
        currentPositionMs = currentPositionMs,
        isPlaying = isPlaying,
        showTranslation = showTranslation,
        showPronunciation = showPronunciation,
        fontScale = fontScale * 0.92f,
        fontPath = fontPath,
        fontWeight = fontWeight,
        primaryTextSizeSp = 19f,
        secondaryTextSizeSp = 13.5f,
        anchorOffsetRatio = -0.01f,
        topContentPadding = 0.dp,
        onLineClick = onLineClick,
        nonCurrentLineBlurEnabled = false,
        nonCurrentLineBlurDistance = Int.MAX_VALUE,
        autoScrollResumeEnabled = true,
        modifier = modifier.fillMaxWidth()
    )
}

internal fun LyricLine.hasMiniLyric(): Boolean {
    return !pronunciation.isNullOrBlank() ||
        text.takeIf { it.isNotBlank() && !it.isMusicSymbolOnly() } != null ||
        !translation.isNullOrBlank() ||
        backgroundText?.takeIf { it.isNotBlank() && !it.isMusicSymbolOnly() } != null ||
        !backgroundTranslation.isNullOrBlank()
}

internal fun LyricLine.miniVisiblePartCount(
    showTranslation: Boolean,
    showPronunciation: Boolean
): Int {
    var count = 0
    if (showPronunciation && !pronunciation.isNullOrBlank()) count++
    if (text.isNotBlank() && !text.isMusicSymbolOnly()) count++
    if (showTranslation && !translation.isNullOrBlank()) count++
    if (!backgroundText.isNullOrBlank() && !backgroundText.isMusicSymbolOnly()) count++
    if (showTranslation && !backgroundTranslation.isNullOrBlank()) count++
    return count
}

internal fun String.isMusicSymbolOnly(): Boolean {
    val cleaned = trim()
    if (cleaned.isEmpty()) return true
    return cleaned.all { char ->
        char.isWhitespace() ||
            char in setOf('♪', '♫', '♬', '♩', '♭', '♮', '♯', '☆', '★', '·', '.', '。', '…')
    }
}
