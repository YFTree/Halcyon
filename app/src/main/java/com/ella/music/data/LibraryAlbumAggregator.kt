package com.ella.music.data

import com.ella.music.data.model.Album
import com.ella.music.data.model.Song
import com.ella.music.data.model.albumIdentityId

object LibraryAlbumAggregator {
    fun toAlbums(songs: List<Song>): List<Album> {
        return songs
            .groupBy { it.albumIdentityId() }
            .map { (albumIdentityId, albumSongs) ->
                val first = albumSongs.first()
                val albumOwner = first.albumArtist
                    .takeIf(LibraryNormalizer::isUsableTagText)
                    ?: ""
                Album(
                    id = albumIdentityId,
                    name = first.album.takeIf(LibraryNormalizer::isUsableTagText) ?: "Unknown Album",
                    artist = albumOwner,
                    songCount = albumSongs.size,
                    year = albumSongs.mapNotNull { s -> s.year.takeIf { it.isNotBlank() } }.minByOrNull { it } ?: "",
                    artAlbumId = first.albumId,
                    albumArtist = first.albumArtist.takeIf(LibraryNormalizer::isUsableTagText).orEmpty()
                )
            }
            .sortedWith(
                compareBy<Album> { it.name.lowercase() }
                    .thenBy { it.artist.lowercase() }
                    .thenBy { it.id }
            )
    }

    fun durationsByAlbumIdentity(songs: List<Song>): Map<Long, Long> {
        return songs
            .groupBy { it.albumIdentityId() }
            .mapValues { (_, albumSongs) -> albumSongs.sumOf { it.duration } }
    }

    fun representativeSongsByAlbumIdentity(songs: List<Song>): Map<Long, Song?> {
        return songs
            .groupBy { it.albumIdentityId() }
            .mapValues { (_, albumSongs) -> albumSongs.firstOrNull() }
    }

    fun toAlbumsForSongs(
        songs: List<Song>,
        libraryAlbums: List<Album>,
        unknownAlbumName: String = "Unknown Album"
    ): List<Album> {
        val albumById = libraryAlbums.associateBy { it.id }
        return songs
            .groupBy { it.albumIdentityId() }
            .map { (albumId, albumSongs) ->
                val first = albumSongs.first()
                albumById[albumId] ?: Album(
                    id = albumId,
                    name = first.album.takeIf(LibraryNormalizer::isUsableTagText) ?: unknownAlbumName,
                    artist = first.artist.takeIf(LibraryNormalizer::isUsableTagText).orEmpty(),
                    songCount = albumSongs.size,
                    year = albumSongs.mapNotNull { s -> s.year.takeIf { it.isNotBlank() } }.minByOrNull { it } ?: "",
                    artAlbumId = first.albumId,
                    albumArtist = first.albumArtist.takeIf(LibraryNormalizer::isUsableTagText).orEmpty()
                )
            }
    }
}
