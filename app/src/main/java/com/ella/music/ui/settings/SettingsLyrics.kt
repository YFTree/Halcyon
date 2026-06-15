package com.ella.music.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.ella.music.R
import com.ella.music.data.SettingsManager
import com.ella.music.viewmodel.PlayerViewModel
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.preference.WindowSpinnerPreference

@Composable
internal fun SettingsLyricsSection(
    playerViewModel: PlayerViewModel?,
    onNavigateToLyricPluginSources: () -> Unit = {}
) {
    SmallTitle(text = stringResource(R.string.settings_lyrics))

    SettingsCardGroup {
        Column {
            ArrowPreference(
                title = stringResource(R.string.settings_lyric_plugin_sources),
                summary = stringResource(R.string.settings_lyric_plugin_sources_summary),
                onClick = onNavigateToLyricPluginSources
            )
            SettingsPlayerLyricAlignmentPreference()
            SettingsMiniLyricsControls()
            SettingsLyriconControls(playerViewModel = playerViewModel)
            SettingsDesktopLyricControls(playerViewModel = playerViewModel)
            SettingsLyricOutputControls(playerViewModel = playerViewModel)
        }
    }
}

@Composable
private fun SettingsPlayerLyricAlignmentPreference() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsManager = remember { SettingsManager.getInstance(context) }
    val playerLyricTextAlign by settingsManager.playerLyricTextAlign.collectAsState(initial = SettingsManager.PLAYER_LYRIC_ALIGN_LEFT)
    val labels = listOf(
        stringResource(R.string.settings_status_align_left),
        stringResource(R.string.settings_status_align_center),
        stringResource(R.string.settings_status_align_right)
    )
    val entries = remember(labels) {
        labels.map { DropdownItem(title = it) }
    }

    WindowSpinnerPreference(
        title = stringResource(R.string.settings_player_lyric_text_align),
        summary = stringResource(
            R.string.settings_current_value,
            labels[playerLyricTextAlign.coerceIn(0, 2)]
        ),
        items = entries,
        selectedIndex = playerLyricTextAlign.coerceIn(0, 2),
        onSelectedIndexChange = { index ->
            scope.launch { settingsManager.setPlayerLyricTextAlign(index) }
        }
    )
}
