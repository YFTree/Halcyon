package com.ella.music.ui.folder

import android.content.Intent
import android.provider.DocumentsContract
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ella.music.ui.LibrarySortUiState
import com.ella.music.data.webdav.WebDavClient
import com.ella.music.data.webdav.WebDavItem
import com.ella.music.ui.components.DoubleTapScrollOverlay
import com.ella.music.ui.components.EllaSearchBar
import com.ella.music.ui.components.FolderOutlineIcon
import com.ella.music.ui.components.ConfirmDangerDialog
import com.ella.music.ui.components.EllaMiuixDialog
import com.ella.music.ui.components.ellaPageBackground
import com.ella.music.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import com.ella.music.viewmodel.PlayerViewModel
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import com.ella.music.ui.components.EllaSmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import com.ella.music.R
import com.ella.music.data.model.albumIdentityId
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.ArrowRight
import top.yukonga.miuix.kmp.icon.basic.Check
import top.yukonga.miuix.kmp.icon.basic.Search
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Folder
import top.yukonga.miuix.kmp.icon.extended.Play
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.icon.extended.Sort
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowBottomSheet
import java.util.Locale

@Composable
fun ScanSettingsScreen(
    mainViewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isScanning by mainViewModel.isScanning.collectAsState()
    val scanProgress by mainViewModel.scanProgress.collectAsState()
    val scanIncludeFolders by mainViewModel.settingsManager.scanIncludeFolders.collectAsState(initial = "")
    val scanExcludeFolders by mainViewModel.settingsManager.scanExcludeFolders.collectAsState(initial = "")
    val useAndroidMediaLibrary by mainViewModel.settingsManager.useAndroidMediaLibrary.collectAsState(initial = true)
    val savedFolders = remember(scanIncludeFolders) { scanIncludeFolders.toFolderSettingList() }
    val blockedFolders = remember(scanExcludeFolders) { scanExcludeFolders.toFolderSettingList() }
    val blockedFolderKeys = remember(blockedFolders) {
        blockedFolders.map { it.normalizeFolderPath().lowercase(Locale.ROOT) }.toSet()
    }
    val showManualScanHint = remember(context) {
        {
            Toast.makeText(context, R.string.folder_scan_manual_needed, Toast.LENGTH_SHORT).show()
        }
    }
    var showBlockedDialog by remember { mutableStateOf(false) }
    var pendingRemoveScanFolder by remember { mutableStateOf<String?>(null) }
    var pendingRemoveUsbUri by remember { mutableStateOf<String?>(null) }

    val usbFolderUrisRaw by mainViewModel.settingsManager.usbFolderUris.collectAsState(initial = "")
    val usbFolderUris = remember(usbFolderUrisRaw) {
        usbFolderUrisRaw.split('\n').map { it.trim() }.filter { it.isNotBlank() }
    }

    val folderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            val readOnly = Intent.FLAG_GRANT_READ_URI_PERMISSION
            val readWrite = readOnly or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, readWrite)
            }.recoverCatching {
                context.contentResolver.takePersistableUriPermission(uri, readOnly)
            }
            val folderPath = uri.toPrimaryStoragePath()
            if (folderPath == null) {
                // Non-primary storage (e.g. USB drive) -- store the SAF URI instead
                scope.launch {
                    mainViewModel.settingsManager.setUseAndroidMediaLibrary(false)
                    mainViewModel.settingsManager.addUsbFolderUri(uri.toString())
                }
                Toast.makeText(context, R.string.folder_usb_added, Toast.LENGTH_SHORT).show()
            } else {
                scope.launch {
                    mainViewModel.settingsManager.setUseAndroidMediaLibrary(false)
                    mainViewModel.settingsManager.setScanIncludeFolders(
                        (savedFolders + folderPath).distinct().joinToString("；")
                    )
                }
                showManualScanHint()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ellaPageBackground())
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        EllaSmallTopAppBar(
            title = stringResource(R.string.folder_scan_settings),
            color = ellaPageBackground(),
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
                IconButton(onClick = { if (!isScanning) mainViewModel.scanMusic() }) {
                    Icon(
                        imageVector = MiuixIcons.Regular.Refresh,
                        contentDescription = stringResource(R.string.folder_full_scan),
                        tint = MiuixTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                }
                IconButton(onClick = { folderPicker.launch(null) }) {
                    Icon(
                        imageVector = MiuixIcons.Regular.Add,
                        contentDescription = stringResource(R.string.folder_add_custom_directory),
                        tint = MiuixTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            if (isScanning) {
                item { ScanStatusCard(scanProgress = scanProgress) }
            }

            item {
                MediaSourceModeCard(
                    useAndroidMediaLibrary = useAndroidMediaLibrary,
                    customFolderCount = savedFolders.size,
                    onUseAndroidMediaLibraryChange = { enabled ->
                        scope.launch {
                            mainViewModel.settingsManager.setUseAndroidMediaLibrary(enabled)
                            mainViewModel.scanMusic()
                        }
                    }
                )
            }

            item {
                SavedScanFoldersCard(
                    folders = savedFolders,
                    hiddenFolders = blockedFolderKeys,
                    onVisibilityChange = { folderPath, visible ->
                        scope.launch {
                            val normalizedPath = folderPath.normalizeFolderPath()
                            val nextBlockedFolders = if (visible) {
                                blockedFolders.filterNot {
                                    it.normalizeFolderPath().equals(normalizedPath, ignoreCase = true)
                                }
                            } else {
                                (blockedFolders + normalizedPath).distinctBy {
                                    it.normalizeFolderPath().lowercase(Locale.ROOT)
                                }
                            }
                            mainViewModel.settingsManager.setScanExcludeFolders(nextBlockedFolders.joinToString("；"))
                        }
                        showManualScanHint()
                    },
                    onRemove = { folderPath ->
                        pendingRemoveScanFolder = folderPath
                    },
                    onScan = {
                        if (!isScanning) mainViewModel.scanMusic()
                    }
                )
            }

            item {
                BlockedFoldersEntryCard(
                    count = blockedFolders.size,
                    onClick = { showBlockedDialog = true }
                )
            }

            if (usbFolderUris.isNotEmpty()) {
                item {
                    UsbFoldersCard(
                        usbFolderUris = usbFolderUris,
                        onRemove = { uri -> pendingRemoveUsbUri = uri },
                        onScan = { if (!isScanning) mainViewModel.scanMusic() }
                    )
                }
            }
        }

        if (showBlockedDialog) {
            BlockedFoldersDialog(
                folders = blockedFolders,
                onDismiss = { showBlockedDialog = false },
                onRemove = { folderPath ->
                    scope.launch {
                        mainViewModel.settingsManager.setScanExcludeFolders(
                            blockedFolders.filterNot { it == folderPath }.joinToString("；")
                        )
                    }
                    showManualScanHint()
                },
                onClear = {
                    scope.launch {
                        mainViewModel.settingsManager.setScanExcludeFolders("")
                    }
                    showManualScanHint()
                    showBlockedDialog = false
                }
            )
        }

        pendingRemoveScanFolder?.let { folderPath ->
            ConfirmDangerDialog(
                show = true,
                title = stringResource(R.string.folder_remove_scan_folder_title),
                message = stringResource(R.string.folder_remove_scan_folder_message, folderPath),
                confirmText = stringResource(R.string.common_remove),
                onDismiss = { pendingRemoveScanFolder = null },
                onConfirm = {
                    scope.launch {
                        val normalizedPath = folderPath.normalizeFolderPath()
                        mainViewModel.settingsManager.setScanIncludeFolders(
                            savedFolders.filterNot {
                                it.normalizeFolderPath().equals(normalizedPath, ignoreCase = true)
                            }.joinToString("；")
                        )
                        mainViewModel.settingsManager.setScanExcludeFolders(
                            blockedFolders.filterNot {
                                it.normalizeFolderPath().equals(normalizedPath, ignoreCase = true)
                            }.joinToString("；")
                        )
                    }
                    showManualScanHint()
                    pendingRemoveScanFolder = null
                }
            )
        }

        pendingRemoveUsbUri?.let { uri ->
            ConfirmDangerDialog(
                show = true,
                title = stringResource(R.string.folder_remove_usb_folder_title),
                message = stringResource(R.string.folder_remove_usb_folder_message),
                confirmText = stringResource(R.string.common_remove),
                onDismiss = { pendingRemoveUsbUri = null },
                onConfirm = {
                    scope.launch {
                        mainViewModel.settingsManager.removeUsbFolderUri(uri)
                    }
                    Toast.makeText(context, R.string.folder_usb_removed, Toast.LENGTH_SHORT).show()
                    pendingRemoveUsbUri = null
                }
            )
        }
    }
}

@Composable
internal fun ScanStatusCard(scanProgress: Int) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(
            text = if (scanProgress > 0) {
                stringResource(R.string.library_scanning_count, scanProgress)
            } else {
                stringResource(R.string.folder_scanning_library)
            },
            fontSize = 14.sp,
            color = MiuixTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )
    }
}

@Composable
internal fun MediaSourceModeCard(
    useAndroidMediaLibrary: Boolean,
    customFolderCount: Int,
    onUseAndroidMediaLibraryChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            SwitchPreference(
                title = stringResource(R.string.folder_use_android_media_library),
                summary = if (useAndroidMediaLibrary) {
                    stringResource(R.string.folder_scan_android_media_summary)
                } else {
                    stringResource(R.string.folder_scan_custom_folders_summary, customFolderCount)
                },
                checked = useAndroidMediaLibrary,
                onCheckedChange = onUseAndroidMediaLibraryChange
            )
        }
    }
}

@Composable
internal fun UsbFoldersCard(
    usbFolderUris: List<String>,
    onRemove: (String) -> Unit,
    onScan: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.folder_usb_directories),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = MiuixTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.folder_usb_directories_summary, usbFolderUris.size),
                        fontSize = 13.sp,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                    )
                }
                IconButton(onClick = onScan) {
                    Icon(
                        imageVector = MiuixIcons.Regular.Refresh,
                        contentDescription = stringResource(R.string.folder_full_scan),
                        tint = MiuixTheme.colorScheme.onSurface,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            usbFolderUris.forEach { uri ->
                val displayName = runCatching {
                    val docUri = android.net.Uri.parse(uri)
                    val docId = android.provider.DocumentsContract.getTreeDocumentId(docUri)
                    docId.substringAfterLast('/').ifBlank { docId.substringBeforeLast('/') }
                }.getOrDefault(uri.substringAfterLast('/').ifBlank { uri })
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FolderOutlineIcon(
                        tint = MiuixTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = displayName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MiuixTheme.colorScheme.onSurface
                        )
                        Text(
                            text = uri,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = { onRemove(uri) }) {
                        Icon(
                            imageVector = MiuixIcons.Regular.Close,
                            contentDescription = stringResource(R.string.common_remove),
                            tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun SavedScanFoldersCard(
    folders: List<String>,
    hiddenFolders: Set<String>,
    onVisibilityChange: (String, Boolean) -> Unit,
    onRemove: (String) -> Unit,
    onScan: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.folder_local_scan_directories),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = MiuixTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.folder_local_scan_directories_summary, folders.size),
                        fontSize = 13.sp,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                    )
                }
                IconButton(onClick = onScan) {
                    Icon(
                        imageVector = MiuixIcons.Regular.Refresh,
                        contentDescription = stringResource(R.string.folder_full_scan),
                        tint = MiuixTheme.colorScheme.onSurface,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            folders.forEach { folder ->
                val isVisible = folder.normalizeFolderPath().lowercase(Locale.ROOT) !in hiddenFolders
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FolderVisibilityCheckbox(
                        checked = isVisible,
                        onCheckedChange = { onVisibilityChange(folder, it) }
                    )
                    FolderOutlineIcon(
                        tint = if (isVisible) {
                            MiuixTheme.colorScheme.primary
                        } else {
                            MiuixTheme.colorScheme.onSurfaceVariantSummary
                        },
                        modifier = Modifier.size(24.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = folder.substringAfterLast('/').ifBlank { folder },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isVisible) {
                                MiuixTheme.colorScheme.onSurface
                            } else {
                                MiuixTheme.colorScheme.onSurfaceVariantSummary
                            }
                        )
                        Text(
                            text = folder,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = { onRemove(folder) }) {
                        Icon(
                            imageVector = MiuixIcons.Regular.Close,
                            contentDescription = stringResource(R.string.common_remove),
                            tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
internal fun FolderVisibilityCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Box(
        modifier = Modifier
            .size(26.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(
                if (checked) {
                    MiuixTheme.colorScheme.primary
                } else {
                    MiuixTheme.colorScheme.surfaceContainer
                }
            )
            .combinedClickable(onClick = { onCheckedChange(!checked) }),
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Icon(
                imageVector = MiuixIcons.Basic.Check,
                contentDescription = stringResource(R.string.folder_show_folder),
                tint = MiuixTheme.colorScheme.onPrimary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
internal fun BlockedFoldersEntryCard(
    count: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = MiuixIcons.Regular.Folder,
                contentDescription = null,
                tint = MiuixTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.folder_blocked_folders),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MiuixTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.folder_blocked_folders_summary, count),
                    fontSize = 13.sp,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                )
            }

            Icon(
                imageVector = MiuixIcons.Basic.ArrowRight,
                contentDescription = null,
                tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
internal fun FolderBlockDialog(
    folderPath: String,
    onDismiss: () -> Unit,
    onBlock: () -> Unit
) {
    EllaMiuixDialog(
        show = true,
        title = stringResource(R.string.folder_block_folder),
        onDismissRequest = onDismiss
    ) {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = folderPath, fontSize = 13.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Button(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = onBlock) { Text(stringResource(R.string.folder_block)) }
            }
        }
    }
}

@Composable
internal fun BlockedFoldersDialog(
    folders: List<String>,
    onDismiss: () -> Unit,
    onRemove: (String) -> Unit,
    onClear: () -> Unit
) {
    EllaMiuixDialog(
        show = true,
        title = stringResource(R.string.folder_blocked_folders),
        onDismissRequest = onDismiss
    ) {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            folders.forEach { folder ->
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = folder,
                        fontSize = 13.sp,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        modifier = Modifier.weight(1f)
                    )
                    Button(onClick = { onRemove(folder) }) { Text(stringResource(R.string.common_remove)) }
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Button(onClick = onClear) { Text(stringResource(R.string.common_clear)) }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = onDismiss) { Text(stringResource(R.string.common_done)) }
            }
        }
    }
}

internal fun Uri.toPrimaryStoragePath(): String? {
    val documentId = runCatching { DocumentsContract.getTreeDocumentId(this) }.getOrNull() ?: return null
    val parts = documentId.split(':', limit = 2)
    val volume = parts.firstOrNull().orEmpty()
    val path = parts.getOrNull(1).orEmpty().trim('/')
    return when {
        volume.equals("primary", ignoreCase = true) && path.isBlank() -> "/storage/emulated/0"
        volume.equals("primary", ignoreCase = true) -> "/storage/emulated/0/$path"
        else -> null
    }
}
