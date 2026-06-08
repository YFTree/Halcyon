package com.ella.music.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import com.ella.music.data.SettingsManager
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun ellaPageBackground(): Color {
    val context = LocalContext.current
    val settingsManager = androidx.compose.runtime.remember(context) { SettingsManager.getInstance(context) }
    val appWallpaperEnabled by settingsManager.appWallpaperEnabled.collectAsState(initial = false)
    val appWallpaperUri by settingsManager.appWallpaperUri.collectAsState(initial = "")
    if (appWallpaperEnabled && appWallpaperUri.isNotBlank()) return Color.Transparent

    val isDark = MiuixTheme.colorScheme.background.luminance() < 0.5f
    return if (isDark) Color(0xFF101014) else Color(0xFFF4F4F7)
}
