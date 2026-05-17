package com.ella.music.ui.components

import android.content.ClipData
import android.content.ComponentName
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import com.ella.music.data.model.Song
import java.io.File

data class TagEditorOption(
    val label: String,
    val summary: String,
    val intents: List<Intent>
)

fun buildTagEditorOptions(context: Context, song: Song): List<TagEditorOption> {
    if (song.path.startsWith("http://") || song.path.startsWith("https://")) {
        return emptyList()
    }

    if (song.id <= 0L) {
        return emptyList()
    }

    val mediaStoreUri = ContentUris.withAppendedId(
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
        song.id
    )
    val songFile = File(song.path)
    val fileUri = songFile.takeIf { it.exists() && it.isFile }?.let { file ->
        runCatching {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        }.getOrNull()
    }
    val editUri = fileUri ?: mediaStoreUri

    val mimeType = song.mimeType
        .takeIf { it.startsWith("audio/") }
        ?: "audio/*"

    fun tagEditorIntent(
        label: String,
        action: String? = null,
        component: ComponentName? = null,
        packageName: String? = null,
        dataUri: Uri = editUri,
        streamUri: Uri = editUri
    ): Intent {
        return Intent(action ?: Intent.ACTION_EDIT).apply {
            component?.let { setComponent(it) }
            packageName?.let { setPackage(it) }
            setDataAndType(dataUri, mimeType)
            putExtra(Intent.EXTRA_STREAM, streamUri)
            putExtra(Intent.EXTRA_TITLE, "${song.title} - ${song.artist}")
            putExtra("title", song.title)
            putExtra("artist", song.artist)
            putExtra("album", song.album)
            putExtra("path", song.path)
            putExtra("filePath", song.path)
            putExtra("id", song.id)
            putExtra("songId", song.id)
            putExtra("mediaId", song.id)
            putExtra("uri", dataUri.toString())
            putExtra("mediaStoreUri", mediaStoreUri.toString())
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            clipData = ClipData.newUri(context.contentResolver, label, dataUri)
        }
    }

    fun Intent.canOpen(): Boolean {
        return resolveActivity(context.packageManager) != null ||
            component?.packageName?.let { context.isPackageInstalled(it) } == true ||
            `package`?.let { context.isPackageInstalled(it) } == true
    }

    return listOf(
        TagEditorOption(
            label = "Lyrico",
            summary = "使用 Lyrico 打开当前歌曲标签编辑",
            intents = listOf(
                tagEditorIntent(
                    label = "Lyrico",
                    action = "com.lonx.lyrico.action.EDIT_TAG",
                    packageName = "com.lonx.lyrico"
                )
            )
        ),
        TagEditorOption(
            label = "LunaBeat",
            summary = "跳转到 LunaBeat 歌曲元数据编辑",
            intents = listOf(
                tagEditorIntent(
                    label = "LunaBeat",
                    component = ComponentName("com.example.LyricBox", "com.example.LyricBox.SongMetadataEditActivity")
                ),
                tagEditorIntent(
                    label = "LunaBeat",
                    component = ComponentName("com.example.lyricbox", "com.example.LyricBox.SongMetadataEditActivity")
                )
            )
        ),
        TagEditorOption(
            label = "音乐标签",
            summary = "通过 content Uri 交给音乐标签编辑",
            intents = listOf(
                tagEditorIntent(
                    label = "音乐标签",
                    action = Intent.ACTION_EDIT,
                    packageName = "com.xjcheng.musictageditor"
                ),
                tagEditorIntent(
                    label = "音乐标签",
                    action = Intent.ACTION_VIEW,
                    packageName = "com.xjcheng.musictageditor"
                ),
                tagEditorIntent(
                    label = "音乐标签",
                    action = Intent.ACTION_SEND,
                    packageName = "com.xjcheng.musictageditor"
                )
            )
        )
    ).mapNotNull { option ->
        option.copy(intents = option.intents.filter { it.canOpen() })
            .takeIf { it.intents.isNotEmpty() }
    }
}

fun launchTagEditorOption(context: Context, option: TagEditorOption) {
    val launched = option.intents.any { intent ->
        runCatching {
            val targetPackage = intent.component?.packageName ?: intent.`package`
            targetPackage?.let { packageName ->
                context.grantUriPermission(
                    packageName,
                    intent.data,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                val streamUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_STREAM) as? Uri
                }
                streamUri?.let { uri ->
                    context.grantUriPermission(
                        packageName,
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                }
            }
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }
    if (!launched) {
        Toast.makeText(context, "无法打开标签编辑器", Toast.LENGTH_SHORT).show()
    }
}

private fun Context.isPackageInstalled(packageName: String): Boolean {
    return runCatching {
        packageManager.getPackageInfo(packageName, 0)
        true
    }.getOrDefault(false)
}
