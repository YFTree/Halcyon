package com.ella.music.ui.settings

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ella.music.R
import com.ella.music.data.SettingsManager
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Slider
import com.ella.music.ui.components.EllaSmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.Check
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.Download
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun LyricFontScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsManager = remember { SettingsManager.getInstance(context) }
    val selectedFontPath by settingsManager.lyricFontPath.collectAsState(initial = "")
    val lyricFontWeight by settingsManager.lyricFontWeight.collectAsState(initial = 800)
    val lyricFontItalic by settingsManager.lyricFontItalic.collectAsState(initial = false)
    var fonts by remember { mutableStateOf<List<FontChoice>>(emptyList()) }
    val isDark = MiuixTheme.colorScheme.background.luminance() < 0.5f
    val pageBackground = if (isDark) Color(0xFF101014) else Color(0xFFF4F4F7)
    LaunchedEffect(Unit) {
        fonts = withContext(Dispatchers.IO) { collectFontChoices(context) }
    }
    LaunchedEffect(lyricFontItalic) {
        if (lyricFontItalic) settingsManager.setLyricFontItalic(false)
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) { copyImportedFont(context, uri) }
            }.onSuccess { font ->
                settingsManager.setLyricFont(font.name, font.path)
                fonts = withContext(Dispatchers.IO) { collectFontChoices(context) }
                Toast.makeText(
                    context,
                    context.getString(R.string.settings_lyric_font_applied, font.name),
                    Toast.LENGTH_SHORT
                ).show()
            }.onFailure {
                Toast.makeText(context, context.getString(R.string.settings_lyric_font_import_failed), Toast.LENGTH_SHORT).show()
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
            title = stringResource(R.string.settings_lyric_font),
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = MiuixIcons.Regular.Back,
                        contentDescription = stringResource(R.string.common_back),
                        tint = MiuixTheme.colorScheme.onSurface
                    )
                }
            },
            actions = {
                IconButton(
                    onClick = {
                        importLauncher.launch(
                            arrayOf(
                                "font/ttf",
                                "font/otf",
                                "font/ttc",
                                "application/x-font-ttf",
                                "application/x-font-otf",
                                "application/vnd.ms-opentype",
                                "application/octet-stream"
                            )
                        )
                    }
                ) {
                    Icon(
                        imageVector = MiuixIcons.Regular.Download,
                        contentDescription = stringResource(R.string.settings_lyric_font_import),
                        tint = MiuixTheme.colorScheme.onSurface
                    )
                }
            },
            color = pageBackground
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.padding(vertical = 4.dp),
                    onClick = {
                        scope.launch {
                            settingsManager.clearLyricFont()
                            Toast.makeText(
                                context,
                                context.getString(R.string.settings_lyric_font_system_default_applied),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                ) {
                    BasicComponent(
                        title = stringResource(R.string.settings_system_default),
                        summary = stringResource(R.string.settings_lyric_font_system_default_summary),
                        endActions = {
                            if (selectedFontPath.isBlank()) {
                                Icon(
                                    imageVector = MiuixIcons.Basic.Check,
                                    contentDescription = null,
                                    tint = MiuixTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    )
                }
                Card(
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.settings_lyric_font_weight),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MiuixTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(
                                        R.string.settings_lyric_font_weight_current,
                                        lyricFontWeight.coerceIn(100, 900)
                                    ),
                                    fontSize = 12.sp,
                                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                            Text(
                                text = stringResource(R.string.settings_lyric_font_preview_sample),
                                fontSize = 18.sp,
                                fontWeight = FontWeight(lyricFontWeight.coerceIn(100, 900)),
                                fontFamily = previewFontFamily(selectedFontPath, lyricFontWeight, false),
                                color = MiuixTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Slider(
                            value = (lyricFontWeight.coerceIn(100, 900) - 100) / 800f,
                            onValueChange = { fraction ->
                                val weight = (100 + fraction.coerceIn(0f, 1f) * 800f).toInt()
                                scope.launch { settingsManager.setLyricFontWeight(weight) }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = stringResource(R.string.settings_lyric_font_weight_light),
                                fontSize = 11.sp,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = stringResource(R.string.settings_lyric_font_weight_heavy),
                                fontSize = 11.sp,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                            )
                        }
                    }
                }
                Text(
                    text = stringResource(R.string.settings_lyric_font_list),
                    fontSize = 14.sp,
                    color = MiuixTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 4.dp)
                )
            }

            items(fonts, key = { it.path }) { font ->
                FontChoiceItem(
                    font = font,
                    currentWeight = lyricFontWeight,
                    italic = false,
                    selected = selectedFontPath == font.path,
                    onClick = {
                        scope.launch {
                            settingsManager.setLyricFont(font.name, font.path)
                            Toast.makeText(
                                context,
                                context.getString(R.string.settings_lyric_font_applied, font.name),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    onDelete = if (font.sourceRank == 2) {
                        {
                            scope.launch {
                                val deleted = withContext(Dispatchers.IO) {
                                    deleteImportedFont(font)
                                }

                                if (selectedFontPath == font.path) {
                                    settingsManager.clearLyricFont()
                                }

                                fonts = withContext(Dispatchers.IO) { collectFontChoices(context) }

                                Toast.makeText(
                                    context,
                                    if (deleted) {
                                        context.getString(R.string.settings_lyric_font_deleted)
                                    } else {
                                        context.getString(R.string.settings_lyric_font_delete_failed)
                                    },
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    } else {
                        null
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun FontChoiceItem(
    font: FontChoice,
    currentWeight: Int,
    italic: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val fontFamily = remember(font.path, currentWeight, italic) {
        font.path.toFontFamilyOrNull(currentWeight, italic)
    }

    Card(
        modifier = Modifier.padding(vertical = 4.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = font.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = MiuixTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.settings_lyric_font_preview_sample),
                    fontSize = 18.sp,
                    fontFamily = fontFamily,
                    fontWeight = FontWeight(currentWeight.coerceIn(100, 900)),
                    color = MiuixTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 6.dp)
                )
                Text(
                    text = font.source,
                    fontSize = 12.sp,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            if (selected) {
                Icon(
                    imageVector = MiuixIcons.Basic.Check,
                    contentDescription = null,
                    tint = MiuixTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }

            if (onDelete != null) {
                Spacer(modifier = Modifier.width(12.dp))
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = MiuixIcons.Regular.Delete,
                        contentDescription = stringResource(R.string.common_delete),
                        tint = MiuixTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

private data class FontChoice(
    val name: String,
    val path: String,
    val source: String,
    val sourceRank: Int
)

private fun collectFontChoices(context: Context): List<FontChoice> {
    val bundledFonts = ensureBundledFontChoices(context)
    val systemFont = listOf(
        FontChoice(
            name = context.getString(R.string.settings_lyric_font_system_default),
            path = SYSTEM_FONT_PATH,
            source = context.getString(R.string.settings_lyric_font_source_system),
            sourceRank = 1
        )
    )
    val importedDir = File(context.filesDir, IMPORTED_FONT_DIR)
    val importedFonts = importedDir.listFiles()
        ?.asSequence()
        ?.filter { it.isFile && it.extension.lowercase() in SUPPORTED_FONT_EXTENSIONS && it.canRead() }
        ?.map { file -> FontChoice(file.nameWithoutExtension.cleanFontName(context), file.absolutePath, context.getString(R.string.settings_lyric_font_source_imported), 2) }
        ?.toList()
        .orEmpty()
    return (bundledFonts + systemFont + importedFonts)
        .distinctBy { it.path }
        .sortedWith(compareBy<FontChoice> { it.sourceRank }.thenBy { it.name.lowercase() })
}

private fun ensureBundledFontChoices(context: Context): List<FontChoice> {
    val bundledDir = File(context.filesDir, BUNDLED_FONT_DIR).apply { mkdirs() }
    val legacyTarget = File(bundledDir, "MiSansVF.ttf")
    if (legacyTarget.exists()) {
        runCatching { legacyTarget.delete() }
    }
    val target = File(bundledDir, "MiSans-Semibold.ttf")
    runCatching {
        if (!target.exists() || target.length() <= 0L) {
            context.assets.open(MISANS_VF_ASSET_PATH).use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            }
        }
    }.onFailure {
        if (target.exists() && target.length() <= 0L) target.delete()
    }
    return if (target.exists() && target.canRead() && target.length() > 0L) {
        listOf(
            FontChoice(
                name = "MiSans SemiBold",
                path = target.absolutePath,
                source = context.getString(R.string.settings_lyric_font_source_builtin),
                sourceRank = 0
            )
        )
    } else {
        emptyList()
    }
}

private fun String.toFontFamilyOrNull(weight: Int, italic: Boolean): FontFamily? {
    if (this == SYSTEM_FONT_PATH) {
        return runCatching {
            FontFamily(Typeface.create(Typeface.DEFAULT, weight.coerceIn(100, 900), italic))
        }.getOrNull()
    }
    val file = File(this)
    if (!file.exists() || !file.canRead()) return null
    return runCatching {
        val base = Typeface.createFromFile(file)
        FontFamily(Typeface.create(base, weight.coerceIn(100, 900), italic))
    }.getOrNull()
}

private fun copyImportedFont(context: Context, uri: Uri): FontChoice {
    val rawName = context.resolveDisplayName(uri).ifBlank { "lyric_font.ttf" }
    val safeName = rawName.sanitizeFileName().ensureFontExtension()
    val dir = File(context.filesDir, IMPORTED_FONT_DIR).apply { mkdirs() }
    val target = File(dir, "${System.currentTimeMillis()}_$safeName")
    context.contentResolver.openInputStream(uri)?.use { input ->
        target.outputStream().use { output -> input.copyTo(output) }
    } ?: error("Unable to open font")
    return FontChoice(
        name = target.nameWithoutExtension.cleanFontName(context),
        path = target.absolutePath,
        source = context.getString(R.string.settings_lyric_font_source_imported),
        sourceRank = 1
    )
}

private fun Context.resolveDisplayName(uri: Uri): String {
    return contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) cursor.getString(0).orEmpty() else ""
    }.orEmpty().ifBlank {
        uri.lastPathSegment.orEmpty().substringAfterLast('/')
    }
}

private fun deleteImportedFont(font: FontChoice): Boolean {
    if (font.sourceRank != 1) return false

    val file = File(font.path)
    if (!file.exists()) return true

    return runCatching {
        file.delete()
    }.getOrDefault(false)
}

private fun String.sanitizeFileName(): String {
    return replace(Regex("""[\\/:*?"<>|]+"""), "_").trim().ifBlank { "lyric_font.ttf" }
}

private fun String.ensureFontExtension(): String {
    return if (substringAfterLast('.', "").lowercase() in SUPPORTED_FONT_EXTENSIONS) this else "$this.ttf"
}

private fun String.cleanFontName(context: Context): String {
    return replace('_', ' ').replace('-', ' ').trim().ifBlank { context.getString(R.string.settings_lyric_font) }
}

@Composable
private fun previewFontFamily(path: String, weight: Int, italic: Boolean): FontFamily? {
    return remember(path, weight, italic) { path.toFontFamilyOrNull(weight, italic) }
}

private const val IMPORTED_FONT_DIR = "lyric_fonts"
private const val BUNDLED_FONT_DIR = "lyric_builtin_fonts"
private const val MISANS_VF_ASSET_PATH = "fonts/MiSans-Semibold.ttf"
const val SYSTEM_FONT_PATH = "__system_default__"
private val SUPPORTED_FONT_EXTENSIONS = setOf("ttf", "otf", "ttc")
