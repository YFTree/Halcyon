package com.ella.music.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ella.music.R
import com.ella.music.data.SettingsManager
import com.ella.music.player.DesktopLyricService
import com.ella.music.viewmodel.PlayerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.preference.WindowSpinnerPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun SettingsDesktopLyricControls(
    playerViewModel: PlayerViewModel?
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsManager = remember { SettingsManager.getInstance(context) }
    val desktopLyricEnabled by settingsManager.desktopLyricEnabled.collectSettingsState(initialValue = false)
    val desktopLyricHideWhenPaused by settingsManager.desktopLyricHideWhenPaused.collectSettingsState(initialValue = false)
    val desktopLyricStatusBarMode by settingsManager.desktopLyricStatusBarMode.collectSettingsState(initialValue = false)
    val desktopLyricStatusBarTopOffset by settingsManager.desktopLyricStatusBarTopOffset.collectSettingsState(initialValue = 16)
    val desktopLyricStatusBarPosition by settingsManager.desktopLyricStatusBarPosition.collectSettingsState(initialValue = SettingsManager.DESKTOP_LYRIC_STATUS_POSITION_CENTER)
    val desktopLyricStatusBarSecondary by settingsManager.desktopLyricStatusBarSecondary.collectSettingsState(initialValue = SettingsManager.DESKTOP_LYRIC_STATUS_SECONDARY_OFF)
    val desktopLyricLocked by settingsManager.desktopLyricLocked.collectSettingsState(initialValue = false)
    val desktopLyricFontScale by settingsManager.desktopLyricFontScale.collectSettingsState(initialValue = 100)
    val desktopLyricTranslationScale by settingsManager.desktopLyricTranslationScale.collectSettingsState(initialValue = 110)
    val desktopLyricOpacity by settingsManager.desktopLyricOpacity.collectSettingsState(initialValue = 100)
    val desktopLyricTextColor by settingsManager.desktopLyricTextColor.collectSettingsState(initialValue = -1)
    val desktopLyricShadowStrength by settingsManager.desktopLyricShadowStrength.collectSettingsState(initialValue = 100)

    val desktopLyricColorPresets = listOf(
        stringResource(R.string.settings_color_white) to android.graphics.Color.WHITE,
        stringResource(R.string.settings_color_silver_gray) to android.graphics.Color.rgb(191, 191, 191),
        stringResource(R.string.settings_color_light_blue) to android.graphics.Color.rgb(145, 205, 255),
        stringResource(R.string.settings_color_sky_blue) to android.graphics.Color.rgb(3, 169, 244),
        stringResource(R.string.settings_color_soft_pink) to android.graphics.Color.rgb(255, 188, 214),
        stringResource(R.string.settings_color_mint_green) to android.graphics.Color.rgb(166, 235, 203),
        stringResource(R.string.settings_color_neon_green) to android.graphics.Color.rgb(26, 201, 125),
        stringResource(R.string.settings_color_light_purple) to android.graphics.Color.rgb(179, 136, 255),
        stringResource(R.string.settings_color_soft_red) to android.graphics.Color.rgb(255, 112, 112),
        stringResource(R.string.settings_color_warm_yellow) to android.graphics.Color.rgb(255, 224, 150),
        stringResource(R.string.settings_color_orange) to android.graphics.Color.rgb(255, 87, 34)
    )
    val desktopLyricColorEntries = remember(desktopLyricColorPresets) {
        desktopLyricColorPresets.map { DropdownItem(title = it.first) }
    }
    val selectedDesktopLyricColorIndex =
        desktopLyricColorPresets.indexOfFirst { it.second == desktopLyricTextColor }.takeIf { it >= 0 } ?: 0
    val statusLyricPositionLeft = stringResource(R.string.settings_status_position_left)
    val statusLyricPositionCenter = stringResource(R.string.settings_status_position_center)
    val statusLyricPositionRight = stringResource(R.string.settings_status_position_right)
    val statusLyricPositionLabels = remember(
        statusLyricPositionLeft,
        statusLyricPositionCenter,
        statusLyricPositionRight
    ) {
        listOf(statusLyricPositionLeft, statusLyricPositionCenter, statusLyricPositionRight)
    }
    val statusLyricPositionEntries = remember(statusLyricPositionLabels) {
        statusLyricPositionLabels.map { DropdownItem(title = it) }
    }
    val statusLyricSecondaryLabels = listOf(
        stringResource(R.string.settings_status_secondary_off),
        stringResource(R.string.settings_status_secondary_translation),
        stringResource(R.string.settings_status_secondary_pronunciation)
    )
    val statusLyricSecondaryEntries = remember(statusLyricSecondaryLabels) {
        statusLyricSecondaryLabels.map { DropdownItem(title = it) }
    }

    fun applyDesktopLyricSettings() {
        playerViewModel?.applyDesktopLyricSettings()
    }

    SwitchPreference(
        title = stringResource(R.string.settings_enable_desktop_lyric),
        summary = stringResource(R.string.settings_enable_desktop_lyric_summary),
        checked = desktopLyricEnabled,
        onCheckedChange = { enabled ->
            if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                Toast.makeText(context, context.getString(R.string.desktop_lyric_overlay_permission_required), Toast.LENGTH_SHORT).show()
                context.startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    )
                )
            } else {
                playerViewModel?.setDesktopLyricEnabled(enabled)
                    ?: scope.launch { settingsManager.setDesktopLyricEnabled(enabled) }
            }
        }
    )

    SwitchPreference(
        title = stringResource(R.string.desktop_lyric_status_bar_mode),
        summary = stringResource(R.string.desktop_lyric_status_bar_mode_summary),
        enabled = desktopLyricEnabled,
        checked = desktopLyricStatusBarMode,
        onCheckedChange = { enabled ->
            scope.launch {
                settingsManager.setDesktopLyricStatusBarMode(enabled)
                if (enabled) settingsManager.resetDesktopLyricPosition()
                applyDesktopLyricSettings()
            }
        }
    )

    SwitchPreference(
        title = stringResource(R.string.settings_floating_lyric_hide_when_paused),
        summary = stringResource(R.string.settings_floating_lyric_hide_when_paused_summary),
        enabled = desktopLyricEnabled,
        checked = desktopLyricHideWhenPaused,
        onCheckedChange = { enabled ->
            playerViewModel?.setDesktopLyricHideWhenPaused(enabled)
                ?: scope.launch { settingsManager.setDesktopLyricHideWhenPaused(enabled) }
        }
    )

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(
            text = stringResource(R.string.settings_status_lyric_top_offset_value, desktopLyricStatusBarTopOffset),
            fontSize = 15.sp,
            color = MiuixTheme.colorScheme.onSurface
        )
        Text(
            text = stringResource(R.string.settings_status_lyric_top_offset_summary),
            fontSize = 13.sp,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Slider(
            value = (desktopLyricStatusBarTopOffset.coerceIn(0, 120).toFloat() / 120f).coerceIn(0f, 1f),
            onValueChange = { fraction ->
                val offset = (fraction * 120f).toInt().coerceIn(0, 120)
                scope.launch {
                    settingsManager.setDesktopLyricStatusBarTopOffset(offset)
                    applyDesktopLyricSettings()
                }
            },
            enabled = desktopLyricEnabled && desktopLyricStatusBarMode,
            modifier = Modifier.fillMaxWidth()
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(text = "0dp", fontSize = 11.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
            Spacer(modifier = Modifier.weight(1f))
            Text(text = "120dp", fontSize = 11.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
        }
    }

    WindowSpinnerPreference(
        title = stringResource(R.string.settings_status_bar_lyric_position),
        summary = stringResource(
            R.string.settings_current_value,
            statusLyricPositionLabels[desktopLyricStatusBarPosition.coerceIn(0, 2)]
        ),
        enabled = desktopLyricEnabled && desktopLyricStatusBarMode,
        items = statusLyricPositionEntries,
        selectedIndex = desktopLyricStatusBarPosition.coerceIn(0, 2),
        onSelectedIndexChange = { index ->
            scope.launch {
                settingsManager.setDesktopLyricStatusBarPosition(index)
                applyDesktopLyricSettings()
            }
        }
    )

    WindowSpinnerPreference(
        title = stringResource(R.string.settings_status_bar_lyric_secondary),
        summary = stringResource(
            R.string.settings_current_value,
            statusLyricSecondaryLabels[desktopLyricStatusBarSecondary.coerceIn(0, 2)]
        ),
        enabled = desktopLyricEnabled && desktopLyricStatusBarMode,
        items = statusLyricSecondaryEntries,
        selectedIndex = desktopLyricStatusBarSecondary.coerceIn(0, 2),
        onSelectedIndexChange = { index ->
            scope.launch {
                settingsManager.setDesktopLyricStatusBarSecondary(index)
                applyDesktopLyricSettings()
            }
        }
    )

    SwitchPreference(
        title = stringResource(R.string.settings_lock_desktop_lyric),
        summary = stringResource(R.string.settings_lock_desktop_lyric_summary),
        enabled = desktopLyricEnabled,
        checked = desktopLyricLocked,
        onCheckedChange = { enabled ->
            scope.launch {
                settingsManager.setDesktopLyricLocked(enabled)
                applyDesktopLyricSettings()
            }
        }
    )

    ArrowPreference(
        title = stringResource(R.string.desktop_lyric_reset_position),
        summary = stringResource(R.string.desktop_lyric_reset_position_summary),
        enabled = desktopLyricEnabled,
        onClick = {
            scope.launch {
                settingsManager.resetDesktopLyricPosition()
                withContext(Dispatchers.Main) {
                    context.startService(
                        Intent(context, DesktopLyricService::class.java)
                            .setAction(DesktopLyricService.ACTION_RESET_POSITION)
                    )
                    Toast.makeText(context, context.getString(R.string.desktop_lyric_reset_position_done), Toast.LENGTH_SHORT).show()
                }
            }
        }
    )

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(
            text = stringResource(R.string.settings_desktop_lyric_font_scale, desktopLyricFontScale),
            fontSize = 15.sp,
            color = MiuixTheme.colorScheme.onSurface
        )
        Text(
            text = stringResource(R.string.settings_desktop_lyric_font_scale_summary),
            fontSize = 13.sp,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Slider(
            value = ((desktopLyricFontScale.coerceIn(80, 220) - 80).toFloat() / 140f).coerceIn(0f, 1f),
            onValueChange = { fraction ->
                val scale = (80 + fraction * 140f).toInt().coerceIn(80, 220)
                scope.launch {
                    settingsManager.setDesktopLyricFontScale(scale)
                    applyDesktopLyricSettings()
                }
            },
            enabled = desktopLyricEnabled,
            modifier = Modifier.fillMaxWidth()
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(text = "80%", fontSize = 11.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
            Spacer(modifier = Modifier.weight(1f))
            Text(text = "220%", fontSize = 11.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
        }
    }

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(
            text = stringResource(R.string.settings_desktop_lyric_translation_scale, desktopLyricTranslationScale),
            fontSize = 15.sp,
            color = MiuixTheme.colorScheme.onSurface
        )
        Text(
            text = stringResource(R.string.settings_desktop_lyric_translation_scale_summary),
            fontSize = 13.sp,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Slider(
            value = ((desktopLyricTranslationScale.coerceIn(80, 220) - 80).toFloat() / 140f).coerceIn(0f, 1f),
            onValueChange = { fraction ->
                val scale = (80 + fraction * 140f).toInt().coerceIn(80, 220)
                scope.launch {
                    settingsManager.setDesktopLyricTranslationScale(scale)
                    applyDesktopLyricSettings()
                }
            },
            enabled = desktopLyricEnabled,
            modifier = Modifier.fillMaxWidth()
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(text = "80%", fontSize = 11.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
            Spacer(modifier = Modifier.weight(1f))
            Text(text = "220%", fontSize = 11.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
        }
    }

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(
            text = stringResource(R.string.settings_desktop_lyric_opacity, desktopLyricOpacity),
            fontSize = 15.sp,
            color = MiuixTheme.colorScheme.onSurface
        )
        Text(
            text = stringResource(R.string.settings_desktop_lyric_opacity_summary),
            fontSize = 13.sp,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Slider(
            value = ((desktopLyricOpacity.coerceIn(35, 100) - 35).toFloat() / 65f).coerceIn(0f, 1f),
            onValueChange = { fraction ->
                val opacity = (35 + fraction * 65f).toInt().coerceIn(35, 100)
                scope.launch {
                    settingsManager.setDesktopLyricOpacity(opacity)
                    applyDesktopLyricSettings()
                }
            },
            enabled = desktopLyricEnabled,
            modifier = Modifier.fillMaxWidth()
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(text = "35%", fontSize = 11.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
            Spacer(modifier = Modifier.weight(1f))
            Text(text = "100%", fontSize = 11.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
        }
    }

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(
            text = stringResource(R.string.settings_desktop_lyric_shadow_strength, desktopLyricShadowStrength),
            fontSize = 15.sp,
            color = MiuixTheme.colorScheme.onSurface
        )
        Text(
            text = stringResource(R.string.settings_desktop_lyric_shadow_strength_summary),
            fontSize = 13.sp,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Slider(
            value = (desktopLyricShadowStrength.coerceIn(0, 160).toFloat() / 160f).coerceIn(0f, 1f),
            onValueChange = { fraction ->
                val strength = (fraction * 160f).toInt().coerceIn(0, 160)
                scope.launch {
                    settingsManager.setDesktopLyricShadowStrength(strength)
                    applyDesktopLyricSettings()
                }
            },
            enabled = desktopLyricEnabled,
            modifier = Modifier.fillMaxWidth()
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(text = "0%", fontSize = 11.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
            Spacer(modifier = Modifier.weight(1f))
            Text(text = "160%", fontSize = 11.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
        }
    }

    WindowSpinnerPreference(
        title = stringResource(R.string.settings_desktop_lyric_color),
        summary = stringResource(
            R.string.settings_current_value,
            desktopLyricColorPresets[selectedDesktopLyricColorIndex].first
        ),
        enabled = desktopLyricEnabled,
        items = desktopLyricColorEntries,
        selectedIndex = selectedDesktopLyricColorIndex,
        onSelectedIndexChange = { index ->
            val color = desktopLyricColorPresets.getOrNull(index)?.second ?: android.graphics.Color.WHITE
            scope.launch {
                settingsManager.setDesktopLyricTextColor(color)
                applyDesktopLyricSettings()
            }
        }
    )
}
