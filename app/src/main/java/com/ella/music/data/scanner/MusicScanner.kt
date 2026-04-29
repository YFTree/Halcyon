package com.ella.music.data.scanner

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.util.Log
import com.ella.music.data.model.Album
import com.ella.music.data.model.Song
import com.kyant.taglib.TagLib
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class MusicScanner(private val context: Context) {

    companion object {
        private const val TAG = "MusicScanner"
    }

    suspend fun scanAllSongs(minDurationMs: Long = 0): List<Song> = withContext(Dispatchers.IO) {
        val songs = mutableListOf<Song>()
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.MIME_TYPE
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        context.contentResolver.query(
            collection, projection, selection, null, sortOrder
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                var title = cursor.getString(titleCol) ?: ""
                var artist = cursor.getString(artistCol) ?: ""
                var album = cursor.getString(albumCol) ?: ""
                val albumId = cursor.getLong(albumIdCol)
                var duration = cursor.getLong(durationCol)
                val path = cursor.getString(dataCol) ?: ""
                val fileName = cursor.getString(nameCol) ?: ""
                val size = cursor.getLong(sizeCol)
                val mime = cursor.getString(mimeCol) ?: ""

                if (path.isEmpty()) continue
                val file = File(path)
                if (!file.exists()) continue

                val needsTagLib = title.isBlank() || artist.isBlank() || album.isBlank() || duration <= 0

                if (needsTagLib) {
                    try {
                        val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                        val audioProps = TagLib.getAudioProperties(fd.fd)
                        val metadata = TagLib.getMetadata(fd.fd, false)
                        val props = metadata?.propertyMap

                        if (title.isBlank()) title = props?.get("TITLE")?.firstOrNull() ?: ""
                        if (artist.isBlank()) artist = props?.get("ARTIST")?.firstOrNull() ?: ""
                        if (album.isBlank()) album = props?.get("ALBUM")?.firstOrNull() ?: ""
                        if (duration <= 0) duration = ((audioProps?.length ?: 0) * 1000L)

                        fd.close()
                    } catch (e: Exception) {
                        Log.w(TAG, "TagLib failed for $path", e)
                        try {
                            val retriever = MediaMetadataRetriever()
                            retriever.setDataSource(path)
                            if (title.isBlank()) title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: ""
                            if (artist.isBlank()) artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: ""
                            if (album.isBlank()) album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM) ?: ""
                            if (duration <= 0) duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                            retriever.release()
                        } catch (_: Exception) {}
                    }
                }

                if (title.isBlank()) title = fileName.substringBeforeLast('.')
                if (artist.isBlank()) artist = "Unknown"
                if (album.isBlank()) album = "Unknown"

                if (duration > 0 && duration >= minDurationMs) {
                    songs.add(Song(id, title, artist, album, albumId, duration, path, fileName, size, mime))
                }
            }
        }
        songs
    }

    suspend fun scanAlbums(): List<Album> = withContext(Dispatchers.IO) {
        val albums = mutableListOf<Album>()
        val collection = MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Albums._ID,
            MediaStore.Audio.Albums.ALBUM,
            MediaStore.Audio.Albums.ARTIST,
            MediaStore.Audio.Albums.NUMBER_OF_SONGS,
            MediaStore.Audio.Albums.FIRST_YEAR
        )
        context.contentResolver.query(collection, projection, null, null, "${MediaStore.Audio.Albums.ALBUM} ASC")?.use { cursor ->
            while (cursor.moveToNext()) {
                albums.add(Album(
                    cursor.getLong(0),
                    cursor.getString(1) ?: "Unknown",
                    cursor.getString(2) ?: "Unknown",
                    cursor.getInt(3),
                    cursor.getInt(4)
                ))
            }
        }
        albums
    }

    fun extractEmbeddedLyrics(path: String): String? {
        return try {
            val file = File(path)
            if (!file.exists()) return null
            val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val lyrics = TagLib.getMetadataPropertyValues(fd.fd, "LYRICS")?.firstOrNull()
            fd.close()
            lyrics?.ifBlank { null }
        } catch (e: Exception) {
            Log.w(TAG, "TagLib lyrics extraction failed for $path", e)
            null
        }
    }

    fun extractReplayGain(path: String): Float? {
        return try {
            val file = File(path)
            if (!file.exists()) return null
            val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val gainStr = TagLib.getMetadataPropertyValues(fd.fd, "REPLAYGAIN_TRACK_GAIN")?.firstOrNull()
            fd.close()
            if (!gainStr.isNullOrBlank()) {
                Regex("([+-]?[0-9.]+)").find(gainStr)?.groupValues?.get(1)?.toFloatOrNull()
            } else null
        } catch (e: Exception) {
            Log.w(TAG, "TagLib ReplayGain extraction failed for $path", e)
            null
        }
    }

    fun extractCoverArt(path: String): ByteArray? {
        return try {
            val file = File(path)
            if (!file.exists()) return null
            val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val pictures = TagLib.getPictures(fd.fd)
            fd.close()
            val frontCover = pictures?.firstOrNull { it.pictureType == "Front Cover" } ?: pictures?.firstOrNull()
            frontCover?.data
        } catch (e: Exception) {
            Log.w(TAG, "TagLib cover art extraction failed for $path", e)
            null
        }
    }

    fun getAlbumArtUri(albumId: Long): Uri =
        ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId)
}
