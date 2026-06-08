package com.ella.music.ui.components

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import com.ella.music.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class ArtworkUsage {
    ListThumbnail,
    ArtistImage,
    MiniPlayer
}

data class SongArtworkState(
    val model: Any?,
    val showDefaultCover: Boolean
)

@Composable
fun rememberSongArtworkState(
    song: Song?,
    albumArtUri: Uri?,
    loadCoverArt: ((Song) -> Bitmap?)?,
    usage: ArtworkUsage,
    showDefaultWhenMissing: Boolean = true
): SongArtworkState {
    val coverUrl = song?.coverUrl?.takeIf { it.isNotBlank() }
    val preferEmbedded = song?.prefersEmbeddedArtwork() == true
    val shouldTryEmbedded = song != null &&
        coverUrl == null &&
        loadCoverArt != null &&
        when (usage) {
            ArtworkUsage.ListThumbnail -> true
            ArtworkUsage.ArtistImage -> true
            ArtworkUsage.MiniPlayer -> albumArtUri == null || preferEmbedded
        }
    val initialModel = when {
        usage == ArtworkUsage.ListThumbnail && shouldTryEmbedded -> coverUrl
        else -> coverUrl ?: albumArtUri
    }

    val state by produceState(
        initialValue = SongArtworkState(
            model = initialModel,
            showDefaultCover = showDefaultWhenMissing && initialModel == null && !shouldTryEmbedded
        ),
        song?.id,
        song?.path,
        song?.dateModified,
        song?.fileSize,
        coverUrl,
        albumArtUri,
        loadCoverArt,
        usage,
        shouldTryEmbedded
    ) {
        val currentSong = song
        value = if (currentSong == null) {
            SongArtworkState(null, showDefaultWhenMissing)
        } else if (!shouldTryEmbedded) {
            SongArtworkState(initialModel, showDefaultWhenMissing && initialModel == null)
        } else {
            val embeddedCover = withContext(Dispatchers.IO) {
                runCatching {
                    CoverLoadLimiter.run { loadCoverArt.invoke(currentSong) }
                }.getOrNull()
            }
            val resolved = coverUrl ?: when {
                usage == ArtworkUsage.ListThumbnail -> embeddedCover
                usage == ArtworkUsage.ArtistImage -> embeddedCover ?: albumArtUri
                preferEmbedded -> embeddedCover ?: albumArtUri
                else -> albumArtUri ?: embeddedCover
            }
            SongArtworkState(
                model = resolved,
                showDefaultCover = showDefaultWhenMissing && resolved == null
            )
        }
    }
    return state
}

fun Song.prefersEmbeddedArtwork(): Boolean =
    fileName.substringAfterLast('.', path.substringAfterLast('.'))
        .lowercase() in embeddedArtworkExtensions

private val embeddedArtworkExtensions = setOf(
    "m4a",
    "mp4",
    "alac",
    "flac",
    "wav",
    "wave",
    "aif",
    "aiff"
)
