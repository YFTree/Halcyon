package com.ella.music.ui.player

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player as Media3Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.ella.music.data.model.Song
import java.io.File

internal data class DynamicCoverSource(
    val uri: Uri,
    val failureKey: String
)

@Composable
internal fun DynamicCoverVideo(
    source: DynamicCoverSource,
    isPlaying: Boolean,
    onPlaybackError: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val exoPlayer = remember(source.failureKey) {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Media3Player.REPEAT_MODE_ALL
            volume = 0f
            setMediaItem(MediaItem.fromUri(source.uri))
            prepare()
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Media3Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                onPlaybackError()
            }
        }

        exoPlayer.addListener(listener)

        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    DisposableEffect(isPlaying, exoPlayer) {
        exoPlayer.playWhenReady = isPlaying
        onDispose { }
    }

    AndroidView(
        modifier = modifier,
        factory = { viewContext ->
            PlayerView(viewContext).apply {
                useController = false
                controllerAutoShow = false
                controllerHideOnTouch = false
                resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                findViewById<View>(androidx.media3.ui.R.id.exo_controller)?.visibility = View.GONE
                player = exoPlayer
                hideController()
            }
        },
        update = { view ->
            view.useController = false
            view.controllerAutoShow = false
            view.controllerHideOnTouch = false
            view.findViewById<View>(androidx.media3.ui.R.id.exo_controller)?.visibility = View.GONE
            view.player = exoPlayer
            view.hideController()
            exoPlayer.playWhenReady = isPlaying
        }
    )
}

internal fun Song.dynamicCoverSource(context: Context): DynamicCoverSource? {
    dynamicCoverVideoFile(context)?.let { file ->
        return DynamicCoverSource(uri = Uri.fromFile(file), failureKey = file.absolutePath)
    }
    return embeddedDynamicVideoSource(context)
}

private fun Song.embeddedDynamicVideoSource(context: Context): DynamicCoverSource? {
    val mediaUri = dynamicCoverMediaUri() ?: return null
    if (!hasPlayableEmbeddedVideoTrack(context, mediaUri)) return null
    return DynamicCoverSource(
        uri = mediaUri,
        failureKey = "embedded-video:$path:${dateModified}:${fileSize}"
    )
}

private fun Song.dynamicCoverMediaUri(): Uri? {
    val trimmedPath = path.trim()
    if (trimmedPath.isBlank() || trimmedPath.startsWith("http://") || trimmedPath.startsWith("https://")) return null
    return if (trimmedPath.startsWith("content://", ignoreCase = true)) {
        Uri.parse(trimmedPath)
    } else {
        File(trimmedPath)
            .takeIf { it.exists() && it.isFile && it.length() > 0L }
            ?.let(Uri::fromFile)
    }
}

private fun Song.hasPlayableEmbeddedVideoTrack(context: Context, uri: Uri): Boolean {
    return runCatching {
        val extractor = MediaExtractor()
        try {
            if (uri.scheme.equals("content", ignoreCase = true)) {
                extractor.setDataSource(context, uri, null)
            } else {
                extractor.setDataSource(uri.path.orEmpty())
            }
            (0 until extractor.trackCount).any { index ->
                val format = extractor.getTrackFormat(index)
                val mime = format.getString(MediaFormat.KEY_MIME).orEmpty().lowercase()
                mime.startsWith("video/") &&
                    mime != "video/mjpeg" &&
                    !mime.startsWith("image/")
            }
        } finally {
            extractor.release()
        }
    }.getOrElse { error ->
        Log.d("PlayerScreen", "Embedded dynamic cover video unavailable for ${title.ifBlank { fileName }}", error)
        false
    }
}

private fun Song.dynamicCoverVideoFile(context: Context): File? {
    val songFile = path
        .takeUnless { it.startsWith("http://") || it.startsWith("https://") }
        ?.let { File(it) }

    val songFolder = songFile?.parentFile

    val albumName = album.ifBlank {
        songFolder?.name.orEmpty()
    }.ifBlank {
        "Unknown"
    }

    val albumKey = albumName.toSafeDynamicCoverName()

    val artistAlbumKey = listOf(artist, albumName)
        .filter { it.isNotBlank() }
        .joinToString(" - ")
        .toSafeDynamicCoverName()

    val songKey = listOf(artist, title)
        .filter { it.isNotBlank() }
        .joinToString(" - ")
        .toSafeDynamicCoverName()

    val songNameCandidates = listOf(
        songFile?.nameWithoutExtension.orEmpty(),
        title,
        songKey,
        listOf(artist, title).filter { it.isNotBlank() }.joinToString("-"),
        listOf(artist, title).filter { it.isNotBlank() }.joinToString(" -")
    )
        .filter { it.isNotBlank() }
        .map { it.toSafeDynamicCoverName() }
        .filter { it.isNotBlank() }
        .distinct()

    val folderCandidates = songFolder
        ?.takeIf { it.exists() && it.isDirectory }
        ?.let { folder ->
            songNameCandidates.map { File(folder, "$it.mp4") } + listOf(
                File(folder, "cover.mp4"),
                File(folder, "${folder.name}.mp4"),
                File(folder, "$albumName.mp4"),
                File(folder, "$albumKey.mp4"),
                File(folder, "$artistAlbumKey.mp4")
            )
        }
        .orEmpty()

    val publicMovieDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
    val publicDirs = listOf(
        File(publicMovieDir, "Halcyon/DynamicCovers"),
        File(publicMovieDir, "Ella/DynamicCovers")
    )

    val appDir = File(
        context.getExternalFilesDir(Environment.DIRECTORY_MOVIES),
        "DynamicCovers"
    )

    val roots = publicDirs + appDir

    val libraryCandidates = roots.flatMap { root ->
        listOf(
            File(root, "Song/$songKey.mp4"),
            File(root, "Album/$albumKey.mp4"),
            File(root, "Album/$artistAlbumKey.mp4"),
            File(root, "cover.mp4")
        )
    }

    val candidates = folderCandidates + libraryCandidates

    candidates.firstOrNull { it.exists() && it.isFile && it.length() > 0L }?.let { return it }

    val fuzzySongTokens = songNameCandidates.mapTo(mutableSetOf()) { it.toDynamicCoverMatchToken() }
    return songFolder
        ?.takeIf { it.exists() && it.isDirectory }
        ?.listFiles { file ->
            file.isFile &&
                file.extension.equals("mp4", ignoreCase = true) &&
                file.length() > 0L &&
                file.nameWithoutExtension.toDynamicCoverMatchToken() in fuzzySongTokens
        }
        ?.firstOrNull()
}

private fun String.toSafeDynamicCoverName(): String {
    return trim()
        .replace("""[\\/:*?"<>|]""".toRegex(), "_")
        .replace("\\s+".toRegex(), " ")
        .ifBlank { "Unknown" }
}

private fun String.toDynamicCoverMatchToken(): String =
    lowercase()
        .replace(Regex("""[\s_\-–—]+"""), "")
        .replace(Regex("""[\\/:*?"<>|.,，。'’`~!！()\[\]{}]+"""), "")
