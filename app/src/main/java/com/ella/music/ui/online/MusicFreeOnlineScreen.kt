package com.ella.music.ui.online

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ella.music.data.MusicFreePluginConfig
import com.ella.music.data.SettingsManager
import com.ella.music.data.musicfree.MusicFreePluginService
import com.ella.music.viewmodel.MusicFreeOnlineViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.InputField
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun MusicFreeOnlineScreen(
    onBack: () -> Unit,
    state: MusicFreeOnlineViewModel = viewModel()
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val settingsManager = remember { SettingsManager(context) }
    val service = remember { MusicFreePluginService() }
    val scope = rememberCoroutineScope()

    val plugins by settingsManager.musicFreePlugins.collectAsState(initial = emptyList())
    val selectedPluginId by settingsManager.selectedMusicFreePluginId.collectAsState(initial = "")
    val selectedPlugin = remember(plugins, selectedPluginId) {
        plugins.firstOrNull { it.id == selectedPluginId } ?: plugins.firstOrNull()
    }

    LaunchedEffect(plugins.isEmpty()) {
        if (plugins.isEmpty()) state.importExpanded = true
    }

    fun showToast(text: String) {
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
    }

    val localPluginLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            state.isBusy = true
            runCatching {
                val script = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        input.bufferedReader(Charsets.UTF_8).readText()
                    }.orEmpty()
                }
                val (name, normalizedScript) = service.importPluginScript(script)
                settingsManager.setMusicFreePlugin(uri.toString(), name, normalizedScript)
                state.message = "已导入 $name"
                state.importExpanded = false
            }.onFailure {
                state.message = it.localizedMessage ?: "本地导入失败"
                showToast(state.message)
            }
            state.isBusy = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        SmallTopAppBar(
            title = "MusicFree 在线音乐",
            color = MiuixTheme.colorScheme.background,
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = MiuixIcons.Regular.Back,
                        contentDescription = "返回",
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

            Card(
                modifier = Modifier.padding(vertical = 4.dp),
                onClick = { state.importExpanded = !state.importExpanded }
            ) {
                Column {
                    BasicComponent(
                        title = selectedPlugin?.name ?: "导入 MusicFree 插件",
                        summary = selectedPlugin?.url ?: "从本地 JS 文件或网络链接导入 MusicFree 插件"
                    )
                    AnimatedVisibility(
                        visible = state.importExpanded,
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                            InputField(
                                query = state.importUrl,
                                onQueryChange = { state.importUrl = it },
                                onSearch = {},
                                expanded = true,
                                onExpandedChange = {},
                                label = "https://.../plugin.js"
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Button(
                                    enabled = !state.isBusy,
                                    onClick = {
                                        localPluginLauncher.launch(
                                            arrayOf(
                                                "text/javascript",
                                                "application/javascript",
                                                "application/x-javascript",
                                                "text/*",
                                                "application/octet-stream"
                                            )
                                        )
                                    }
                                ) {
                                    Text(text = "本地 JS")
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    enabled = !state.isBusy && state.importUrl.isNotBlank(),
                                    onClick = {
                                        scope.launch {
                                            state.isBusy = true
                                            runCatching {
                                                val (name, script) = service.importPlugin(state.importUrl)
                                                settingsManager.setMusicFreePlugin(state.importUrl, name, script)
                                                state.importUrl = ""
                                                state.message = "已导入 $name"
                                                state.importExpanded = false
                                            }.onFailure {
                                                state.message = it.localizedMessage ?: "导入失败"
                                                showToast(state.message)
                                            }
                                            state.isBusy = false
                                        }
                                    }
                                ) {
                                    Text(text = if (state.isBusy) "导入中" else "URL 导入")
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    enabled = !state.isBusy,
                                    onClick = { uriHandler.openUri("https://musicfree.catcat.work/plugin/introduction.html") }
                                ) {
                                    Text(text = "文档")
                                }
                            }
                        }
                    }
                }
            }

            Text(
                text = if (state.isBusy) "处理中..." else state.message,
                fontSize = 13.sp,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
            )

            if (plugins.isEmpty()) {
                Text(
                    text = "还没有导入插件。插件脚本会保存在设置中，后续在线搜索和播放解析会基于这里的插件列表接入。",
                    fontSize = 14.sp,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 12.dp)
                )
            } else {
                plugins.forEach { plugin ->
                    MusicFreePluginRow(
                        plugin = plugin,
                        selected = plugin.id == selectedPlugin?.id,
                        enabled = !state.isBusy,
                        onSelect = {
                            scope.launch {
                                settingsManager.selectMusicFreePlugin(plugin.id)
                                state.message = "已切换到 ${plugin.name}"
                            }
                        },
                        onRemove = {
                            scope.launch {
                                settingsManager.removeMusicFreePlugin(plugin.id)
                                state.message = "已移除 ${plugin.name}"
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(120.dp))
        }
    }
}

@Composable
private fun MusicFreePluginRow(
    plugin: MusicFreePluginConfig,
    selected: Boolean,
    enabled: Boolean,
    onSelect: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (selected) "${plugin.name}（当前）" else plugin.name,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = plugin.url,
                fontSize = 12.sp,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Button(
            enabled = enabled && !selected,
            onClick = onSelect
        ) {
            Text("使用")
        }
        Spacer(modifier = Modifier.width(8.dp))
        Button(
            enabled = enabled,
            onClick = onRemove
        ) {
            Text("移除")
        }
    }
}
