package com.ella.music.ui.player

import android.content.Context
import android.graphics.Typeface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.ella.music.R
import com.ella.music.data.SettingsManager

internal data class PlayerLyricFontState(
    val fontFamily: FontFamily?,
    val fontPath: String,
    val fontWeight: FontWeight,
    val fontScale: Float,
    val secondaryFontScale: Float,
    val shareTypeface: Typeface?
)

@Composable
internal fun rememberPlayerLyricFontState(
    context: Context,
    settingsManager: SettingsManager
): PlayerLyricFontState {
    val lyricFontPath by settingsManager.lyricFontPath.collectAsState(initial = "")
    val lyricFontWeightValue by settingsManager.lyricFontWeight.collectAsState(initial = 800)
    val lyricFontScaleValue by settingsManager.lyricFontScale.collectAsState(initial = 100)
    val lyricSecondaryFontScaleValue by settingsManager.lyricSecondaryFontScale.collectAsState(initial = 100)
    val lyricShareUseLyricFont by settingsManager.lyricShareUseLyricFont.collectAsState(initial = false)
    val lyricFontApplyToPage by settingsManager.lyricFontApplyToPage.collectAsState(initial = true)
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
    val lyricSecondaryFontScale = remember(lyricSecondaryFontScaleValue) { lyricSecondaryFontScaleValue.coerceIn(70, 150) / 100f }
    val lyricShareTypeface = remember(lyricShareUseLyricFont, effectiveLyricFontPath, effectiveLyricFontWeightValue) {
        if (lyricShareUseLyricFont) {
            effectiveLyricFontPath.toPlayerLyricTypeface(effectiveLyricFontWeightValue)
        } else {
            null
        }
    }

    return PlayerLyricFontState(
        // fontFamily drives the PlayerSongMetaText group (song title + artist + annotation) on
        // the player/lyrics pages. When the "apply font to page" toggle is off, return null so
        // those texts fall back to the global app font, leaving the lyric body font untouched.
        fontFamily = if (lyricFontApplyToPage) lyricFontFamily else null,
        fontPath = effectiveLyricFontPath,
        fontWeight = lyricFontWeight,
        fontScale = lyricFontScale,
        secondaryFontScale = lyricSecondaryFontScale,
        shareTypeface = lyricShareTypeface
    )
}
