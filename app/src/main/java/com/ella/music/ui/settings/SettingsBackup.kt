package com.ella.music.ui.settings

import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.ella.music.R
import com.ella.music.data.PlaybackStatsStore
import com.ella.music.data.PlaylistStore
import com.ella.music.data.SettingsManager
import com.ella.music.data.webdav.WebDavClient
import com.ella.music.data.webdav.WebDavConfig
import com.ella.music.ui.components.EllaMiuixDialog
import com.ella.music.ui.components.EllaSmallTopAppBar
import com.ella.music.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
@Composable
fun BackupSettingsScreen(
    onBack: () -> Unit,
    mainViewModel: MainViewModel? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val backupScope = remember(context, scope) { context.findComponentActivity()?.lifecycleScope ?: scope }
    val settingsManager = remember { SettingsManager.getInstance(context) }
    val playbackStatsStore = remember { PlaybackStatsStore.getInstance(context) }
    val playlistStore = remember { PlaylistStore.getInstance(context) }
    val librarySongs by mainViewModel?.songs?.collectAsState(initial = emptyList()) ?: remember { mutableStateOf(emptyList()) }
    var showPlaybackExportFormatDialog by remember { mutableStateOf(false) }
    var playbackExportFormat by remember { mutableStateOf(PlaybackExportFormat.Ella) }
    val isDark = MiuixTheme.colorScheme.background.luminance() < 0.5f
    val pageBackground = if (isDark) Color(0xFF101014) else Color(0xFFF4F4F7)
    suspend fun writeBackupText(uri: Uri, text: String) = withContext(Dispatchers.IO) {
        context.contentResolver.openOutputStream(uri, "wt")?.use { output ->
            output.bufferedWriter(Charsets.UTF_8).use { writer ->
                writer.write(text)
                writer.flush()
            }
            output.flush()
        } ?: error(context.getString(R.string.settings_backup_open_failed))
    }
    val settingsExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        backupScope.launch {
            runCatching {
                val backup = JSONObject()
                    .put("version", 1)
                    .put("exportedAt", System.currentTimeMillis())
                    .put("settings", settingsManager.exportSettingsJson())
                    .put("playlists", playlistStore.exportJson())
                    .put("aiChat", exportAiChatBackupJson(context))
                writeBackupText(uri, backup.toString(2))
            }.onSuccess {
                Toast.makeText(context, context.getString(R.string.settings_backup_export_success), Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(context, context.getString(R.string.settings_backup_export_failed), Toast.LENGTH_SHORT).show()
            }
        }
    }
    val playbackExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        backupScope.launch {
            runCatching {
                val exported = playbackStatsStore.exportJson(librarySongs)
                val backup = when (playbackExportFormat) {
                    PlaybackExportFormat.Ella -> JSONObject()
                        .put("version", 1)
                        .put("exportedAt", System.currentTimeMillis())
                        .put("playback", exported)
                    PlaybackExportFormat.Sollin -> JSONObject()
                        .put("version", 1)
                        .put("exportedAtMs", System.currentTimeMillis())
                        .put("sessions", exported.optJSONArray("sessions") ?: org.json.JSONArray())
                }
                writeBackupText(uri, backup.toString(2))
            }.onSuccess {
                Toast.makeText(context, context.getString(R.string.settings_backup_export_success), Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(context, context.getString(R.string.settings_backup_export_failed), Toast.LENGTH_SHORT).show()
            }
        }
    }
    val settingsImportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        backupScope.launch {
            runCatching {
                val text = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        input.bufferedReader(Charsets.UTF_8).readText()
                    } ?: error(context.getString(R.string.settings_backup_read_failed))
                }
                val root = JSONObject(text)
                settingsManager.restoreSettingsJson(root.optJSONObject("settings") ?: root)
                val playlistPayload = root.optJSONObject("playlists")
                    ?: root.takeIf { it.has("playlists") }
                if (playlistPayload != null) {
                    playlistStore.restoreJson(playlistPayload)
                }
                root.optJSONObject("aiChat")?.let { restoreAiChatBackupJson(context, it) }
            }.onSuccess {
                Toast.makeText(context, context.getString(R.string.settings_backup_restore_success), Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(context, context.getString(R.string.settings_backup_restore_failed), Toast.LENGTH_SHORT).show()
            }
        }
    }
    val playbackImportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        backupScope.launch {
            runCatching {
                val text = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        input.bufferedReader(Charsets.UTF_8).readText()
                    } ?: error(context.getString(R.string.settings_backup_read_failed))
                }
                val root = JSONObject(text)
                playbackStatsStore.restoreJson(root.optJSONObject("playback") ?: root)
            }.onSuccess {
                Toast.makeText(context, context.getString(R.string.settings_backup_restore_success), Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(context, context.getString(R.string.settings_backup_restore_failed), Toast.LENGTH_SHORT).show()
            }
        }
    }

    // WebDAV backup state
    val savedWebDavUrl by settingsManager.webDavUrl.collectAsState(initial = "")
    val savedWebDavUser by settingsManager.webDavUsername.collectAsState(initial = "")
    val savedWebDavPassword by settingsManager.webDavPassword.collectAsState(initial = "")
    val savedWebDavBackupUrl by settingsManager.webDavBackupUrl.collectAsState(initial = "")
    val savedWebDavBackupPath by settingsManager.webDavBackupPath.collectAsState(initial = "")
    var webDavBackupUrl by remember { mutableStateOf(savedWebDavBackupUrl) }
    var webDavBackupPath by remember { mutableStateOf(savedWebDavBackupPath) }
    var webDavBackupUser by remember { mutableStateOf(savedWebDavUser) }
    var webDavBackupPassword by remember { mutableStateOf(savedWebDavPassword) }
    var webDavUploading by remember { mutableStateOf(false) }
    var webDavDownloading by remember { mutableStateOf(false) }
    var webDavBackupFiles by remember { mutableStateOf<List<com.ella.music.data.webdav.WebDavItem>>(emptyList()) }
    var webDavRestoreConfig by remember { mutableStateOf<com.ella.music.data.webdav.WebDavConfig?>(null) }
    LaunchedEffect(savedWebDavBackupUrl) {
        if (savedWebDavBackupUrl.isNotBlank() && webDavBackupUrl.isBlank()) {
            webDavBackupUrl = savedWebDavBackupUrl
        }
    }
    LaunchedEffect(savedWebDavBackupPath) {
        if (savedWebDavBackupPath.isNotBlank() && webDavBackupPath.isBlank()) {
            webDavBackupPath = savedWebDavBackupPath
        }
    }
    LaunchedEffect(savedWebDavUser) {
        if (savedWebDavUser.isNotBlank() && webDavBackupUser.isBlank()) {
            webDavBackupUser = savedWebDavUser
        }
    }
    LaunchedEffect(savedWebDavPassword) {
        if (savedWebDavPassword.isNotBlank() && webDavBackupPassword.isBlank()) {
            webDavBackupPassword = savedWebDavPassword
        }
    }

    suspend fun buildBackupJson(): String {
        val backup = JSONObject()
            .put("version", 1)
            .put("exportedAt", System.currentTimeMillis())
            .put("settings", settingsManager.exportSettingsJson())
            .put("playlists", playlistStore.exportJson())
            .put("aiChat", exportAiChatBackupJson(context))
        return backup.toString(2)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(pageBackground)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        EllaSmallTopAppBar(
            title = stringResource(R.string.settings_backup),
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
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            SmallTitle(text = stringResource(R.string.settings_backup_settings_section))

            SettingsCardGroup {
                Column {
                    ArrowPreference(
                        title = stringResource(R.string.settings_backup_export_settings_title),
                        summary = stringResource(R.string.settings_backup_export_settings_summary),
                        onClick = {
                            settingsExportLauncher.launch("ella_settings_${System.currentTimeMillis()}.json")
                        }
                    )
                    ArrowPreference(
                        title = stringResource(R.string.settings_backup_restore_settings_title),
                        summary = stringResource(R.string.settings_backup_restore_settings_summary),
                        onClick = {
                            settingsImportLauncher.launch(arrayOf("application/json", "text/json", "text/*"))
                        }
                    )
                }
            }

            SmallTitle(text = stringResource(R.string.settings_backup_playback_section))

            SettingsCardGroup {
                Column {
                    ArrowPreference(
                        title = stringResource(R.string.settings_backup_export_playback_title),
                        summary = stringResource(R.string.settings_backup_export_playback_summary),
                        onClick = {
                            showPlaybackExportFormatDialog = true
                        }
                    )
                    ArrowPreference(
                        title = stringResource(R.string.settings_backup_restore_playback_title),
                        summary = stringResource(R.string.settings_backup_restore_playback_summary),
                        onClick = {
                            playbackImportLauncher.launch(arrayOf("application/json", "text/json", "text/*"))
                        }
                    )
                }
            }

            SmallTitle(text = stringResource(R.string.settings_backup_webdav_section))

            SettingsCardGroup {
                Column {
                    SplitSettingTextField(
                        label = stringResource(R.string.settings_backup_webdav_url_label),
                        value = webDavBackupUrl,
                        summary = stringResource(R.string.settings_backup_webdav_url_summary),
                        onValueChange = { webDavBackupUrl = it }
                    )
                    SplitSettingTextField(
                        label = stringResource(R.string.settings_backup_webdav_username_label),
                        value = webDavBackupUser,
                        summary = stringResource(R.string.settings_backup_webdav_username_summary),
                        onValueChange = { webDavBackupUser = it }
                    )
                    SplitSettingTextField(
                        label = stringResource(R.string.settings_backup_webdav_password_label),
                        value = webDavBackupPassword,
                        summary = stringResource(R.string.settings_backup_webdav_password_summary),
                        singleLine = true,
                        isPassword = true,
                        onValueChange = { webDavBackupPassword = it }
                    )
                    SplitSettingTextField(
                        label = stringResource(R.string.settings_backup_webdav_path_label),
                        value = webDavBackupPath,
                        summary = stringResource(R.string.settings_backup_webdav_path_summary),
                        onValueChange = { webDavBackupPath = it }
                    )
                    BasicComponent(
                        title = stringResource(R.string.settings_backup_webdav_upload),
                        summary = stringResource(R.string.settings_backup_webdav_upload_summary),
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .clickable(enabled = !webDavUploading && !webDavDownloading) {
                                backupScope.launch {
                                    val effectiveUrl = webDavBackupUrl.trim().ifBlank { savedWebDavUrl }
                                    if (effectiveUrl.isBlank()) {
                                        Toast.makeText(context, context.getString(R.string.settings_backup_webdav_not_configured), Toast.LENGTH_SHORT).show()
                                        return@launch
                                    }
                                    val effectiveUser = webDavBackupUser.trim().ifBlank { savedWebDavUser }
                                    val effectivePassword = webDavBackupPassword.ifBlank { savedWebDavPassword }
                                    // Persist the backup URL and path
                                    settingsManager.setWebDavBackupUrl(effectiveUrl)
                                    settingsManager.setWebDavBackupPath(webDavBackupPath)
                                    webDavUploading = true
                                    runCatching {
                                        val config = WebDavConfig(
                                            url = effectiveUrl,
                                            username = effectiveUser,
                                            password = effectivePassword
                                        )
                                        val path = webDavBackupPath.trim().ifBlank { "halcyon_backup" }
                                        val fileName = "halcyon_backup_${System.currentTimeMillis()}.json"
                                        val fullUrl = "${effectiveUrl.trimEnd('/')}/$path/$fileName"
                                        val backupContent = withContext(Dispatchers.IO) { buildBackupJson() }
                                        withContext(Dispatchers.IO) {
                                            WebDavClient.uploadFileFromString(fullUrl, config, backupContent)
                                        }
                                    }.onSuccess {
                                        Toast.makeText(context, context.getString(R.string.settings_backup_webdav_upload_success), Toast.LENGTH_SHORT).show()
                                    }.onFailure {
                                        Toast.makeText(context, context.getString(R.string.settings_backup_webdav_upload_failed) + ": " + (it.message ?: ""), Toast.LENGTH_LONG).show()
                                    }
                                    webDavUploading = false
                                }
                            }
                    )
                    BasicComponent(
                        title = stringResource(R.string.settings_backup_webdav_download),
                        summary = stringResource(R.string.settings_backup_webdav_download_summary),
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .clickable(enabled = !webDavUploading && !webDavDownloading) {
                                backupScope.launch {
                                    val effectiveUrl = webDavBackupUrl.trim().ifBlank { savedWebDavUrl }
                                    if (effectiveUrl.isBlank()) {
                                        Toast.makeText(context, context.getString(R.string.settings_backup_webdav_not_configured), Toast.LENGTH_SHORT).show()
                                        return@launch
                                    }
                                    val effectiveUser = webDavBackupUser.trim().ifBlank { savedWebDavUser }
                                    val effectivePassword = webDavBackupPassword.ifBlank { savedWebDavPassword }
                                    // Persist the backup URL and path
                                    settingsManager.setWebDavBackupUrl(effectiveUrl)
                                    settingsManager.setWebDavBackupPath(webDavBackupPath)
                                    webDavDownloading = true
                                    runCatching {
                                        val config = WebDavConfig(
                                            url = effectiveUrl,
                                            username = effectiveUser,
                                            password = effectivePassword
                                        )
                                        val path = webDavBackupPath.trim().ifBlank { "halcyon_backup" }
                                        val backupDirUrl = "${effectiveUrl.trimEnd('/')}/$path/"
                                        val items = withContext(Dispatchers.IO) {
                                            WebDavClient.list(
                                                config,
                                                backupDirUrl,
                                                forceRefresh = true,
                                                includeNonAudioFiles = true
                                            )
                                        }
                                        val backupFiles = items
                                            .filterNot { it.isDirectory }
                                            .filter { it.name.endsWith(".json") && it.name.isHalcyonBackupFileName() }
                                            .sortedByDescending { it.name }
                                        if (backupFiles.isEmpty()) {
                                            throw IllegalStateException(context.getString(R.string.settings_backup_webdav_file_not_found))
                                        }
                                        webDavRestoreConfig = config
                                        webDavBackupFiles = backupFiles
                                    }.onFailure {
                                        webDavDownloading = false
                                        Toast.makeText(context, context.getString(R.string.settings_backup_webdav_download_failed) + ": " + (it.message ?: ""), Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                    )
                }
            }

            Spacer(modifier = Modifier.height(160.dp))
        }
    }

    if (showPlaybackExportFormatDialog) {
        EllaMiuixDialog(
            show = true,
            title = stringResource(R.string.settings_backup_export_format_title),
            onDismissRequest = { showPlaybackExportFormatDialog = false }
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BackupExportFormatRow(
                    title = stringResource(R.string.settings_backup_export_format_ella),
                    summary = stringResource(R.string.settings_backup_export_format_ella_summary),
                    onClick = {
                        playbackExportFormat = PlaybackExportFormat.Ella
                        showPlaybackExportFormatDialog = false
                        playbackExportLauncher.launch("ella_listening_stats_${System.currentTimeMillis()}.json")
                    }
                )
                BackupExportFormatRow(
                    title = stringResource(R.string.settings_backup_export_format_sollin),
                    summary = stringResource(R.string.settings_backup_export_format_sollin_summary),
                    onClick = {
                        playbackExportFormat = PlaybackExportFormat.Sollin
                        showPlaybackExportFormatDialog = false
                        playbackExportLauncher.launch("prism-listening-stats_${System.currentTimeMillis()}.json")
                    }
                )
            }
        }
    }

    if (webDavBackupFiles.isNotEmpty()) {
        EllaMiuixDialog(
            show = true,
            title = stringResource(R.string.settings_backup_webdav_download),
            onDismissRequest = {
                webDavBackupFiles = emptyList()
                webDavRestoreConfig = null
                webDavDownloading = false
            }
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                webDavBackupFiles.forEach { item ->
                    val displayName = item.name
                        .removePrefix("halcyon_backup_")
                        .removePrefix("ella_backup_")
                        .removeSuffix(".json")
                        .ifBlank { item.name }
                    BasicComponent(
                        title = item.name,
                        summary = displayName,
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                val selectedFile = item
                                val config = webDavRestoreConfig
                                webDavBackupFiles = emptyList()
                                webDavRestoreConfig = null
                                if (config != null) {
                                    backupScope.launch {
                                        runCatching {
                                            val tempFile = withContext(Dispatchers.IO) {
                                                val tmp = java.io.File(context.cacheDir, "webdav_restore.json")
                                                WebDavClient.downloadToFile(selectedFile.url, config, tmp)
                                                tmp
                                            }
                                            val text = withContext(Dispatchers.IO) {
                                                tempFile.readText(Charsets.UTF_8)
                                            }
                                            val root = JSONObject(text)
                                            settingsManager.restoreSettingsJson(root.optJSONObject("settings") ?: root)
                                            val playlistPayload = root.optJSONObject("playlists")
                                                ?: root.takeIf { it.has("playlists") }
                                            if (playlistPayload != null) {
                                                playlistStore.restoreJson(playlistPayload)
                                            }
                                            root.optJSONObject("aiChat")?.let { restoreAiChatBackupJson(context, it) }
                                            withContext(Dispatchers.IO) { tempFile.delete() }
                                        }.onSuccess {
                                            Toast.makeText(context, context.getString(R.string.settings_backup_webdav_download_success), Toast.LENGTH_SHORT).show()
                                        }.onFailure {
                                            Toast.makeText(context, context.getString(R.string.settings_backup_webdav_download_failed) + ": " + (it.message ?: ""), Toast.LENGTH_LONG).show()
                                        }
                                        webDavDownloading = false
                                    }
                                }
                            }
                    )
                }
            }
        }
    }
}

private enum class PlaybackExportFormat {
    Ella,
    Sollin
}

private tailrec fun Context.findComponentActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.findComponentActivity()
    else -> null
}

private fun aiChatSessionsDir(context: Context): File =
    File(context.filesDir, "ai_chat_sessions")

private fun exportAiChatBackupJson(context: Context): JSONObject {
    val dir = aiChatSessionsDir(context)
    val sessions = JSONArray()
    if (dir.exists()) {
        dir.listFiles()
            ?.filter { it.isFile && it.extension.equals("json", ignoreCase = true) && it.name != "index.json" }
            ?.sortedBy { it.name }
            ?.forEach { file ->
                runCatching {
                    sessions.put(
                        JSONObject()
                            .put("fileName", file.name)
                            .put("messages", JSONArray(file.readText(Charsets.UTF_8)))
                    )
                }
            }
    }
    val index = runCatching {
        val file = File(dir, "index.json")
        if (file.exists()) JSONArray(file.readText(Charsets.UTF_8)) else JSONArray()
    }.getOrDefault(JSONArray())
    return JSONObject()
        .put("version", 1)
        .put("index", index)
        .put("sessions", sessions)
}

private fun restoreAiChatBackupJson(context: Context, payload: JSONObject) {
    val dir = aiChatSessionsDir(context).apply { mkdirs() }
    dir.listFiles()
        ?.filter { it.isFile && it.extension.equals("json", ignoreCase = true) }
        ?.forEach { it.delete() }

    val index = payload.optJSONArray("index") ?: JSONArray()
    File(dir, "index.json").writeText(index.toString(), Charsets.UTF_8)

    val sessions = payload.optJSONArray("sessions") ?: JSONArray()
    for (i in 0 until sessions.length()) {
        val item = sessions.optJSONObject(i) ?: continue
        val fileName = item.optString("fileName").takeIf { it.isSafeAiChatBackupFileName() } ?: continue
        val messages = item.optJSONArray("messages") ?: continue
        File(dir, fileName).writeText(messages.toString(), Charsets.UTF_8)
    }
}

private fun String.isSafeAiChatBackupFileName(): Boolean =
    matches(Regex("""[A-Za-z0-9._-]+\.json""")) && this != "index.json"

@Composable
private fun BackupExportFormatRow(
    title: String,
    summary: String,
    onClick: () -> Unit
) {
    BasicComponent(
        title = title,
        summary = summary,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    )
}

private fun String.isHalcyonBackupFileName(): Boolean =
    startsWith("halcyon_backup_") || startsWith("ella_backup_")
