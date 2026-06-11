package com.ella.music.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ella.music.R
import com.ella.music.viewmodel.PlayerViewModel
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.basic.SmallTitle

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
            SettingsMiniLyricsControls()
            SettingsLyriconControls(playerViewModel = playerViewModel)
            SettingsDesktopLyricControls(playerViewModel = playerViewModel)
            SettingsLyricOutputControls(playerViewModel = playerViewModel)
        }
    }
}
