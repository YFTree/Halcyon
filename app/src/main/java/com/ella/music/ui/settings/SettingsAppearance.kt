package com.ella.music.ui.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.ella.music.R
import com.ella.music.data.BottomBarGlassEffect
import com.ella.music.data.SettingsManager
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.preference.WindowSpinnerPreference

@Composable
internal fun SettingsAppearanceSection() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsManager = remember { SettingsManager.getInstance(context) }

    val themeMode by settingsManager.themeMode.collectAsState(initial = 0)
    val appLanguage by settingsManager.appLanguage.collectAsState(initial = SettingsManager.APP_LANGUAGE_SYSTEM)
    val bottomBarGlassEffect by settingsManager.bottomBarGlassEffect.collectAsState(initial = BottomBarGlassEffect.LiquidGlass)
    val startupPosterEnabled by settingsManager.startupPosterEnabled.collectAsState(initial = false)
    val startupPosterUri by settingsManager.startupPosterUri.collectAsState(initial = "")
    val appWallpaperEnabled by settingsManager.appWallpaperEnabled.collectAsState(initial = false)
    val appWallpaperUri by settingsManager.appWallpaperUri.collectAsState(initial = "")
    val playerBackgroundEnabled by settingsManager.playerBackgroundEnabled.collectAsState(initial = false)
    val playerBackgroundUri by settingsManager.playerBackgroundUri.collectAsState(initial = "")
    val dynamicCoverEnabled by settingsManager.dynamicCoverEnabled.collectAsState(initial = false)
    val hiResLogoEnabled by settingsManager.hiResLogoEnabled.collectAsState(initial = false)
    val hiResLogoUri by settingsManager.hiResLogoUri.collectAsState(initial = "")
    val playerImmersiveCover by settingsManager.playerImmersiveCover.collectAsState(initial = true)
    val transportButtonOutlines by settingsManager.transportButtonOutlines.collectAsState(initial = false)
    val playerTapSeekEnabled by settingsManager.playerTapSeekEnabled.collectAsState(initial = true)
    val playerShowTotalDuration by settingsManager.playerShowTotalDuration.collectAsState(initial = false)
    val playlistSpecialEntriesVisible by settingsManager.playlistSpecialEntriesVisible.collectAsState(initial = false)
    val showPlayNextInLists by settingsManager.showPlayNextInLists.collectAsState(initial = false)
    val showAlbumArtists by settingsManager.showAlbumArtists.collectAsState(initial = false)
    val openPlayerOnPlay by settingsManager.openPlayerOnPlay.collectAsState(initial = false)
    val categoryGridColumns by settingsManager.categoryGridColumns.collectAsState(initial = 2)

    val themeLabels = listOf(
        stringResource(R.string.theme_follow_system),
        stringResource(R.string.theme_light),
        stringResource(R.string.theme_dark)
    )
    val selectedThemeMode = themeMode.coerceIn(themeLabels.indices)
    val themeEntries = remember(themeLabels) { themeLabels.map { DropdownItem(title = it) } }

    val languageOptions = listOf(
        SettingsManager.APP_LANGUAGE_SYSTEM to stringResource(R.string.settings_language_system),
        SettingsManager.APP_LANGUAGE_ZH_CN to stringResource(R.string.settings_language_simplified_chinese),
        SettingsManager.APP_LANGUAGE_ZH_TW to stringResource(R.string.settings_language_traditional_chinese),
        SettingsManager.APP_LANGUAGE_EN to stringResource(R.string.settings_language_english),
        SettingsManager.APP_LANGUAGE_JA to stringResource(R.string.settings_language_japanese)
    )
    val selectedLanguageIndex = languageOptions.indexOfFirst { it.first == appLanguage }.takeIf { it >= 0 } ?: 0
    val languageEntries = remember(languageOptions) {
        languageOptions.map { (_, label) -> DropdownItem(title = label) }
    }
    val languageSummary = when (languageOptions.getOrNull(selectedLanguageIndex)?.first) {
        SettingsManager.APP_LANGUAGE_ZH_CN -> stringResource(R.string.settings_language_summary_simplified_chinese)
        SettingsManager.APP_LANGUAGE_ZH_TW -> stringResource(R.string.settings_language_summary_traditional_chinese)
        SettingsManager.APP_LANGUAGE_EN -> stringResource(R.string.settings_language_summary_english)
        SettingsManager.APP_LANGUAGE_JA -> stringResource(R.string.settings_language_summary_japanese)
        else -> stringResource(R.string.settings_language_summary_system)
    }

    val bottomBarGlassEffects = remember {
        listOf(BottomBarGlassEffect.Blur, BottomBarGlassEffect.LiquidGlass)
    }
    val bottomBarGlassBlurLabel = stringResource(R.string.bottom_bar_glass_effect_blur)
    val bottomBarGlassLiquidLabel = stringResource(R.string.bottom_bar_glass_effect_liquid)
    val bottomBarGlassEntries = remember(bottomBarGlassBlurLabel, bottomBarGlassLiquidLabel) {
        listOf(
            DropdownItem(title = bottomBarGlassBlurLabel),
            DropdownItem(title = bottomBarGlassLiquidLabel)
        )
    }
    val selectedBottomBarGlassEffectIndex =
        bottomBarGlassEffects.indexOf(bottomBarGlassEffect).takeIf { it >= 0 } ?: 0
    val bottomBarGlassSummary = when (bottomBarGlassEffect) {
        BottomBarGlassEffect.Blur -> stringResource(R.string.settings_bottom_bar_glass_effect_summary_blur)
        BottomBarGlassEffect.LiquidGlass -> stringResource(R.string.settings_bottom_bar_glass_effect_summary_liquid)
    }

    val isTabletDevice = context.resources.configuration.smallestScreenWidthDp >= 600
    val categoryGridRange = if (isTabletDevice) 5..8 else 1..4
    val categoryGridEntries = remember(context, isTabletDevice) {
        categoryGridRange.map { columns ->
            DropdownItem(
                title = context.getString(R.string.settings_category_grid_columns_option, columns),
                summary = when (columns) {
                    1 -> context.getString(R.string.settings_category_grid_columns_option_summary_single)
                    4, 8 -> context.getString(R.string.settings_category_grid_columns_option_summary_dense)
                    else -> context.getString(R.string.settings_category_grid_columns_option_summary_default)
                }
            )
        }
    }

    val startupPosterPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        context.persistImageReadPermission(uri)
        scope.launch {
            val persisted = context.copyCustomImageIntoApp(uri, "startup_poster")
            if (persisted == null) {
                Toast.makeText(context, context.getString(R.string.settings_custom_image_save_failed), Toast.LENGTH_SHORT).show()
            } else {
                context.deletePersistedCustomImage(startupPosterUri)
                settingsManager.setStartupPosterUri(persisted)
            }
        }
    }
    val appWallpaperPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        context.persistImageReadPermission(uri)
        scope.launch {
            val persisted = context.copyCustomImageIntoApp(uri, "app_wallpaper")
            if (persisted == null) {
                Toast.makeText(context, context.getString(R.string.settings_custom_image_save_failed), Toast.LENGTH_SHORT).show()
            } else {
                context.deletePersistedCustomImage(appWallpaperUri)
                settingsManager.setAppWallpaperUri(persisted)
            }
        }
    }
    val playerBackgroundPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        context.persistImageReadPermission(uri)
        scope.launch {
            val persisted = context.copyCustomImageIntoApp(uri, "player_background")
            if (persisted == null) {
                Toast.makeText(context, context.getString(R.string.settings_custom_image_save_failed), Toast.LENGTH_SHORT).show()
            } else {
                context.deletePersistedCustomImage(playerBackgroundUri)
                settingsManager.setPlayerBackgroundUri(persisted)
            }
        }
    }
    val hiResLogoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        context.persistImageReadPermission(uri)
        scope.launch {
            val persisted = context.copyCustomImageIntoApp(uri, "hi_res_logo")
            if (persisted == null) {
                Toast.makeText(context, context.getString(R.string.settings_custom_image_save_failed), Toast.LENGTH_SHORT).show()
            } else {
                context.deletePersistedCustomImage(hiResLogoUri)
                settingsManager.setHiResLogoUri(persisted)
            }
        }
    }

    val dynamicCoverPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        scope.launch { settingsManager.setDynamicCoverEnabled(granted) }
        if (granted) {
            Toast.makeText(context, context.getString(R.string.settings_dynamic_cover_enabled), Toast.LENGTH_SHORT).show()
        } else {
            val activity = context as? android.app.Activity
            val shouldShowRationale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && activity != null) {
                ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.READ_MEDIA_VIDEO)
            } else {
                true
            }
            if (!shouldShowRationale && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Toast.makeText(context, context.getString(R.string.settings_dynamic_cover_permission_grant), Toast.LENGTH_LONG).show()
                runCatching {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:${context.packageName}")
                        )
                    )
                }
            } else {
                Toast.makeText(context, context.getString(R.string.settings_dynamic_cover_permission_denied), Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun setDynamicCoverEnabled(enabled: Boolean) {
        if (!enabled) {
            scope.launch { settingsManager.setDynamicCoverEnabled(false) }
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_MEDIA_VIDEO
            ) == PackageManager.PERMISSION_GRANTED
            if (granted) {
                scope.launch { settingsManager.setDynamicCoverEnabled(true) }
            } else {
                scope.launch { settingsManager.setDynamicCoverEnabled(false) }
                dynamicCoverPermissionLauncher.launch(Manifest.permission.READ_MEDIA_VIDEO)
            }
        } else {
            scope.launch { settingsManager.setDynamicCoverEnabled(true) }
        }
    }

    SmallTitle(text = stringResource(R.string.settings_appearance))

    SettingsCardGroup {
        Column {
            WindowSpinnerPreference(
                title = stringResource(R.string.settings_theme_mode),
                summary = stringResource(R.string.settings_theme_mode_summary),
                items = themeEntries,
                selectedIndex = selectedThemeMode,
                onSelectedIndexChange = { index ->
                    scope.launch { settingsManager.setThemeMode(index) }
                }
            )
            WindowSpinnerPreference(
                title = stringResource(R.string.settings_language),
                summary = languageSummary,
                items = languageEntries,
                selectedIndex = selectedLanguageIndex,
                onSelectedIndexChange = { index ->
                    languageOptions.getOrNull(index)?.first?.let { language ->
                        scope.launch { settingsManager.setAppLanguage(language) }
                    }
                }
            )
            WindowSpinnerPreference(
                title = stringResource(R.string.settings_bottom_bar_glass_effect),
                summary = bottomBarGlassSummary,
                items = bottomBarGlassEntries,
                selectedIndex = selectedBottomBarGlassEffectIndex,
                onSelectedIndexChange = { index ->
                    bottomBarGlassEffects.getOrNull(index)?.let { effect ->
                        scope.launch { settingsManager.setBottomBarGlassEffect(effect) }
                    }
                }
            )
            SwitchPreference(
                title = stringResource(R.string.settings_startup_poster),
                summary = stringResource(R.string.settings_startup_poster_summary),
                checked = startupPosterEnabled,
                onCheckedChange = {
                    scope.launch { settingsManager.setStartupPosterEnabled(it) }
                }
            )
            ArrowPreference(
                title = stringResource(R.string.settings_startup_poster_image),
                summary = if (startupPosterUri.isBlank()) {
                    stringResource(R.string.settings_custom_image_not_selected)
                } else {
                    stringResource(R.string.settings_custom_image_selected)
                },
                onClick = { startupPosterPicker.launch(arrayOf("image/*")) }
            )
            if (startupPosterUri.isNotBlank()) {
                ArrowPreference(
                    title = stringResource(R.string.settings_custom_image_remove),
                    summary = stringResource(R.string.settings_custom_image_remove_summary),
                    onClick = {
                        scope.launch {
                            context.deletePersistedCustomImage(startupPosterUri)
                            settingsManager.setStartupPosterUri("")
                        }
                    }
                )
            }
            SwitchPreference(
                title = stringResource(R.string.settings_app_wallpaper),
                summary = stringResource(R.string.settings_app_wallpaper_summary),
                checked = appWallpaperEnabled,
                onCheckedChange = {
                    scope.launch { settingsManager.setAppWallpaperEnabled(it) }
                }
            )
            ArrowPreference(
                title = stringResource(R.string.settings_app_wallpaper_image),
                summary = if (appWallpaperUri.isBlank()) {
                    stringResource(R.string.settings_custom_image_not_selected)
                } else {
                    stringResource(R.string.settings_custom_image_selected)
                },
                onClick = { appWallpaperPicker.launch(arrayOf("image/*")) }
            )
            if (appWallpaperUri.isNotBlank()) {
                ArrowPreference(
                    title = stringResource(R.string.settings_custom_image_remove),
                    summary = stringResource(R.string.settings_custom_image_remove_summary),
                    onClick = {
                        scope.launch {
                            context.deletePersistedCustomImage(appWallpaperUri)
                            settingsManager.setAppWallpaperUri("")
                        }
                    }
                )
            }
            SwitchPreference(
                title = stringResource(R.string.settings_player_background),
                summary = stringResource(R.string.settings_player_background_summary),
                checked = playerBackgroundEnabled,
                onCheckedChange = {
                    scope.launch { settingsManager.setPlayerBackgroundEnabled(it) }
                }
            )
            ArrowPreference(
                title = stringResource(R.string.settings_player_background_image),
                summary = if (playerBackgroundUri.isBlank()) {
                    stringResource(R.string.settings_custom_image_not_selected)
                } else {
                    stringResource(R.string.settings_custom_image_selected)
                },
                onClick = { playerBackgroundPicker.launch(arrayOf("image/*")) }
            )
            if (playerBackgroundUri.isNotBlank()) {
                ArrowPreference(
                    title = stringResource(R.string.settings_custom_image_remove),
                    summary = stringResource(R.string.settings_custom_image_remove_summary),
                    onClick = {
                        scope.launch {
                            context.deletePersistedCustomImage(playerBackgroundUri)
                            settingsManager.setPlayerBackgroundUri("")
                        }
                    }
                )
            }
            WindowSpinnerPreference(
                title = stringResource(R.string.settings_category_grid_columns),
                summary = stringResource(
                    R.string.settings_category_grid_columns_summary,
                    categoryGridColumns.coerceIn(categoryGridRange.first, categoryGridRange.last)
                ),
                items = categoryGridEntries,
                selectedIndex = (categoryGridColumns - categoryGridRange.first).coerceIn(categoryGridEntries.indices),
                onSelectedIndexChange = { index ->
                    scope.launch { settingsManager.setCategoryGridColumns(categoryGridRange.first + index) }
                }
            )
            SwitchPreference(
                title = stringResource(R.string.settings_show_album_artists),
                summary = stringResource(R.string.settings_show_album_artists_summary),
                checked = showAlbumArtists,
                onCheckedChange = {
                    scope.launch { settingsManager.setShowAlbumArtists(it) }
                }
            )
            SwitchPreference(
                title = stringResource(R.string.settings_open_player_on_play),
                summary = stringResource(R.string.settings_open_player_on_play_summary),
                checked = openPlayerOnPlay,
                onCheckedChange = {
                    scope.launch { settingsManager.setOpenPlayerOnPlay(it) }
                }
            )
            SwitchPreference(
                title = stringResource(R.string.settings_show_play_next_in_lists),
                summary = stringResource(R.string.settings_show_play_next_in_lists_summary),
                checked = showPlayNextInLists,
                onCheckedChange = {
                    scope.launch { settingsManager.setShowPlayNextInLists(it) }
                }
            )
            SwitchPreference(
                title = stringResource(R.string.settings_playlist_special_entries),
                summary = stringResource(R.string.settings_playlist_special_entries_summary),
                checked = playlistSpecialEntriesVisible,
                onCheckedChange = {
                    scope.launch { settingsManager.setPlaylistSpecialEntriesVisible(it) }
                }
            )
            SwitchPreference(
                title = stringResource(R.string.settings_dynamic_cover),
                summary = stringResource(R.string.settings_dynamic_cover_summary),
                checked = dynamicCoverEnabled,
                onCheckedChange = ::setDynamicCoverEnabled
            )
            SwitchPreference(
                title = stringResource(R.string.settings_hi_res_logo),
                summary = stringResource(R.string.settings_hi_res_logo_summary),
                checked = hiResLogoEnabled,
                onCheckedChange = {
                    scope.launch { settingsManager.setHiResLogoEnabled(it) }
                }
            )
            ArrowPreference(
                title = stringResource(R.string.settings_hi_res_logo_image),
                summary = if (hiResLogoUri.isBlank()) {
                    stringResource(R.string.settings_hi_res_logo_default)
                } else {
                    stringResource(R.string.settings_custom_image_selected)
                },
                onClick = { hiResLogoPicker.launch(arrayOf("image/*")) }
            )
            if (hiResLogoUri.isNotBlank()) {
                ArrowPreference(
                    title = stringResource(R.string.settings_custom_image_remove),
                    summary = stringResource(R.string.settings_custom_image_remove_summary),
                    onClick = {
                        scope.launch {
                            context.deletePersistedCustomImage(hiResLogoUri)
                            settingsManager.setHiResLogoUri("")
                        }
                    }
                )
            }
            SwitchPreference(
                title = stringResource(R.string.settings_player_immersive_cover),
                summary = stringResource(R.string.settings_player_immersive_cover_summary),
                checked = playerImmersiveCover,
                onCheckedChange = {
                    scope.launch { settingsManager.setPlayerImmersiveCover(it) }
                }
            )
            SwitchPreference(
                title = stringResource(R.string.settings_transport_button_outlines),
                summary = stringResource(R.string.settings_transport_button_outlines_summary),
                checked = transportButtonOutlines,
                onCheckedChange = {
                    scope.launch { settingsManager.setTransportButtonOutlines(it) }
                }
            )
            SwitchPreference(
                title = stringResource(R.string.settings_player_tap_seek),
                summary = stringResource(R.string.settings_player_tap_seek_summary),
                checked = playerTapSeekEnabled,
                onCheckedChange = {
                    scope.launch { settingsManager.setPlayerTapSeekEnabled(it) }
                }
            )
            SwitchPreference(
                title = stringResource(R.string.settings_player_show_total_duration),
                summary = stringResource(R.string.settings_player_show_total_duration_summary),
                checked = playerShowTotalDuration,
                onCheckedChange = {
                    scope.launch { settingsManager.setPlayerShowTotalDuration(it) }
                }
            )
        }
    }
}
