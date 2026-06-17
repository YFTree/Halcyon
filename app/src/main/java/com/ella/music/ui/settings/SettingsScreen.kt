package com.ella.music.ui.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ella.music.BuildConfig
import com.ella.music.R
import com.ella.music.ui.components.EllaSmallTopAppBar
import com.ella.music.viewmodel.MainViewModel
import com.ella.music.viewmodel.PlayerViewModel
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun SettingsScreen(
    onNavigateToAbout: () -> Unit,
    onNavigateToAppearanceSettings: () -> Unit,
    onNavigateToLibrarySettings: () -> Unit,
    onNavigateToIntegrationSettings: () -> Unit,
    onNavigateToLyricSettings: () -> Unit,
    onNavigateToAudioSettings: () -> Unit,
    onNavigateToBackupSettings: () -> Unit,
    onNavigateToLogs: () -> Unit,
    onBack: () -> Unit = {},
    showBackButton: Boolean = true,
    mainViewModel: MainViewModel? = null,
    playerViewModel: PlayerViewModel? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isDark = MiuixTheme.colorScheme.background.luminance() < 0.5f
    val pageBackground = if (isDark) Color(0xFF101014) else Color(0xFFF4F4F7)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(pageBackground)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        EllaSmallTopAppBar(
            title = stringResource(R.string.settings),
            color = pageBackground,
            centeredTitle = true,
            navigationIcon = {
                if (showBackButton) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = MiuixIcons.Regular.Back,
                            contentDescription = stringResource(R.string.common_back)
                        )
                    }
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            SmallTitle(text = stringResource(R.string.settings_customize))

            SettingsCardGroup {
                Column {
                    ArrowPreference(
                        title = stringResource(R.string.settings_appearance_home),
                        summary = stringResource(R.string.settings_appearance_home_summary),
                        onClick = onNavigateToAppearanceSettings
                    )
                    ArrowPreference(
                        title = stringResource(R.string.settings_lyrics),
                        summary = stringResource(R.string.settings_lyrics_summary),
                        onClick = onNavigateToLyricSettings
                    )
                }
            }

            SmallTitle(text = stringResource(R.string.settings_music_playback))

            SettingsCardGroup {
                Column {
                    ArrowPreference(
                        title = stringResource(R.string.settings_audio),
                        summary = stringResource(R.string.settings_audio_summary),
                        onClick = onNavigateToAudioSettings
                    )
                    ArrowPreference(
                        title = stringResource(R.string.settings_library_scan),
                        summary = stringResource(R.string.settings_library_scan_summary),
                        onClick = onNavigateToLibrarySettings
                    )
                }
            }

            SmallTitle(text = stringResource(R.string.settings_services))

            SettingsCardGroup {
                Column {
                    ArrowPreference(
                        title = stringResource(R.string.settings_integrations),
                        summary = stringResource(R.string.settings_integrations_summary),
                        onClick = onNavigateToIntegrationSettings
                    )
                    ArrowPreference(
                        title = stringResource(R.string.settings_backup),
                        summary = stringResource(R.string.settings_backup_summary),
                        onClick = onNavigateToBackupSettings
                    )
                }
            }

            SmallTitle(text = stringResource(R.string.settings_maintenance))

            SettingsCardGroup {
                Column {
                    ArrowPreference(
                        title = stringResource(R.string.settings_clear_online_cache),
                        summary = stringResource(R.string.settings_clear_online_cache_summary),
                        onClick = {
                            scope.launch {
                                mainViewModel?.clearOnlineMetadataCache()
                                playerViewModel?.clearOnlineMetadataCache()
                                Toast.makeText(context, context.getString(R.string.settings_clear_online_cache_done), Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                    ArrowPreference(
                        title = stringResource(R.string.settings_clear_library_snapshot_cache),
                        summary = stringResource(R.string.settings_clear_library_snapshot_cache_summary),
                        onClick = {
                            mainViewModel?.clearLibrarySnapshotCache()
                            Toast.makeText(context, context.getString(R.string.settings_clear_library_snapshot_cache_done), Toast.LENGTH_SHORT).show()
                        }
                    )
                    ArrowPreference(
                        title = stringResource(R.string.settings_logs),
                        summary = stringResource(R.string.settings_logs_summary),
                        onClick = onNavigateToLogs
                    )
                    ArrowPreference(
                        title = stringResource(R.string.about),
                        summary = "${context.getString(R.string.app_name)} v${BuildConfig.VERSION_NAME}",
                        onClick = onNavigateToAbout
                    )
                }
            }

            Spacer(modifier = Modifier.height(160.dp))
        }
    }
}
