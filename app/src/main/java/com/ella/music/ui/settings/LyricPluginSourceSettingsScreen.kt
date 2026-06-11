package com.ella.music.ui.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ella.music.R
import com.ella.music.data.SettingsManager
import com.ella.music.plugin.source.BuiltInPluginSource
import com.ella.music.plugin.source.LyricoPluginManager
import com.ella.music.plugin.source.PluginSourceOrigin
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import com.ella.music.ui.components.EllaSmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Download
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun LyricPluginSourceSettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsManager = remember { SettingsManager.getInstance(context) }
    val pluginManager = remember(context) { LyricoPluginManager(context, settingsManager) }
    val enabledIds by settingsManager.lyricoPluginEnabledIds.collectAsState(initial = emptySet())
    var reloadToken by remember { mutableIntStateOf(0) }
    val sources by produceState(initialValue = emptyList<BuiltInPluginSource>(), context, reloadToken) {
        value = pluginManager.availableSources()
    }
    val isDark = MiuixTheme.colorScheme.background.luminance() < 0.5f
    val pageBackground = if (isDark) Color(0xFF101014) else Color(0xFFF4F4F7)
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching { pluginManager.importPluginZip(uri) }
                .onSuccess { manifest ->
                    settingsManager.setLyricoPluginEnabled(manifest.id, true)
                    reloadToken++
                    Toast.makeText(
                        context,
                        context.getString(R.string.settings_lyric_plugin_import_success, manifest.name),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                .onFailure {
                    Toast.makeText(context, R.string.settings_lyric_plugin_import_failed, Toast.LENGTH_SHORT).show()
                }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(pageBackground)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        EllaSmallTopAppBar(
            title = stringResource(R.string.settings_lyric_plugin_sources),
            color = pageBackground,
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = MiuixIcons.Regular.Back,
                        contentDescription = stringResource(R.string.common_back),
                        tint = MiuixTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                }
            },
            actions = {
                IconButton(onClick = { importLauncher.launch(PLUGIN_ZIP_MIME_TYPES) }) {
                    Icon(
                        imageVector = MiuixIcons.Regular.Download,
                        contentDescription = stringResource(R.string.settings_lyric_plugin_import),
                        tint = MiuixTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
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
            SettingsCardGroup {
                Column {
                    if (sources.isEmpty()) {
                        Text(
                            text = stringResource(R.string.settings_lyric_plugin_sources_empty),
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            modifier = Modifier.padding(18.dp)
                        )
                    } else {
                        sources.forEach { source ->
                            val manifest = source.manifest
                            SwitchPreference(
                                title = manifest.name,
                                summary = buildString {
                                    append(
                                        if (source.origin == PluginSourceOrigin.IMPORTED) {
                                            context.getString(R.string.settings_lyric_plugin_source_imported)
                                        } else {
                                            context.getString(R.string.settings_lyric_plugin_source_builtin)
                                        }
                                    )
                                    append(" · ")
                                    append(manifest.description.ifBlank { manifest.id })
                                    if (manifest.versionName.isNotBlank()) append(" · v${manifest.versionName}")
                                },
                                checked = manifest.id in enabledIds,
                                onCheckedChange = { enabled ->
                                    scope.launch {
                                        settingsManager.setLyricoPluginEnabled(manifest.id, enabled)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            Text(
                text = stringResource(R.string.settings_lyric_plugin_import_later),
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp)
            )
            Spacer(modifier = Modifier.height(160.dp))
        }
    }
}

private val PLUGIN_ZIP_MIME_TYPES = arrayOf(
    "application/zip",
    "application/x-zip-compressed",
    "application/octet-stream"
)
