package com.ella.music.ui.theme

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.ella.music.R
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.theme.defaultTextStyles

const val THEME_FOLLOW_SYSTEM = 0
const val THEME_LIGHT = 1
const val THEME_DARK = 2

@Composable
fun EllaTheme(
    themeMode: Int = THEME_FOLLOW_SYSTEM,
    content: @Composable () -> Unit
) {
    val colorSchemeMode = when (themeMode) {
        THEME_LIGHT -> ColorSchemeMode.Light
        THEME_DARK -> ColorSchemeMode.Dark
        else -> ColorSchemeMode.System
    }

    val controller = remember(colorSchemeMode) {
        ThemeController(colorSchemeMode = colorSchemeMode)
    }
    val appFontFamily = remember {
        FontFamily(
            Font(
                resId = R.font.misans_semibold,
                weight = FontWeight(800)
            )
        )
    }
    val preferMiSansByDefault = remember {
        !isXiaomiFamilyDevice()
    }
    val textStyles = remember(appFontFamily, preferMiSansByDefault) {
        val defaults = defaultTextStyles()
        if (!preferMiSansByDefault) {
            defaults
        } else {
            defaults.copy(
                main = defaults.main.copy(fontFamily = appFontFamily),
                paragraph = defaults.paragraph.copy(fontFamily = appFontFamily),
                body1 = defaults.body1.copy(fontFamily = appFontFamily),
                body2 = defaults.body2.copy(fontFamily = appFontFamily),
                button = defaults.button.copy(fontFamily = appFontFamily),
                footnote1 = defaults.footnote1.copy(fontFamily = appFontFamily),
                footnote2 = defaults.footnote2.copy(fontFamily = appFontFamily),
                headline1 = defaults.headline1.copy(fontFamily = appFontFamily),
                headline2 = defaults.headline2.copy(fontFamily = appFontFamily),
                subtitle = defaults.subtitle.copy(fontFamily = appFontFamily),
                title1 = defaults.title1.copy(fontFamily = appFontFamily),
                title2 = defaults.title2.copy(fontFamily = appFontFamily),
                title3 = defaults.title3.copy(fontFamily = appFontFamily),
                title4 = defaults.title4.copy(fontFamily = appFontFamily)
            )
        }
    }

    MiuixTheme(
        controller = controller,
        textStyles = textStyles
    ) { content() }
}

private fun isXiaomiFamilyDevice(): Boolean {
    val brand = Build.BRAND.orEmpty()
    val manufacturer = Build.MANUFACTURER.orEmpty()
    return listOf(brand, manufacturer).any { value ->
        value.contains("xiaomi", ignoreCase = true) ||
            value.contains("redmi", ignoreCase = true) ||
            value.contains("poco", ignoreCase = true)
    }
}
