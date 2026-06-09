package com.ella.music.viewmodel

import com.ella.music.data.NameSplitConfigStore
import com.ella.music.data.PlaybackHistoryEntry
import com.ella.music.data.SongPlaybackStats
import com.ella.music.data.model.Album
import com.ella.music.data.model.Artist
import com.ella.music.data.model.Song
import com.ella.music.data.model.UserPlaylist
import com.ella.music.data.model.albumIdentityId
import com.ella.music.data.model.playlistIdentityKey
import com.ella.music.data.splitArtistNames
import com.ella.music.data.tagIdentityKey

internal fun buildArtists(
    songs: List<Song>,
    albums: List<Album>,
    includeAlbumArtists: Boolean
): List<Artist> {
    val counts = linkedMapOf<String, ArtistAccumulator>()
    val albumIdsByArtist = mutableMapOf<String, MutableSet<Long>>()

    songs.forEach { song ->
        splitArtistNames(song.artist).forEach { rawName ->
            val key = rawName.tagIdentityKey()
            val accumulator = counts.getOrPut(key) { ArtistAccumulator(rawName) }
            accumulator.songCount += 1
            albumIdsByArtist.getOrPut(key) { mutableSetOf() } += song.albumIdentityId()
        }
        if (includeAlbumArtists) {
            splitArtistNames(song.albumArtist).forEach { rawName ->
                val key = rawName.tagIdentityKey()
                counts.getOrPut(key) { ArtistAccumulator(rawName) }
                albumIdsByArtist.getOrPut(key) { mutableSetOf() } += song.albumIdentityId()
            }
        }
    }

    if (includeAlbumArtists) {
        albums.forEach { album ->
            splitArtistNames(album.albumArtist).forEach { rawName ->
                val key = rawName.tagIdentityKey()
                counts.getOrPut(key) { ArtistAccumulator(rawName) }
                if (album.id > 0L) {
                    albumIdsByArtist.getOrPut(key) { mutableSetOf() } += album.id
                }
            }
        }
    }

    return counts
        .map { (key, accumulator) ->
            Artist(
                name = accumulator.name,
                songCount = accumulator.songCount,
                albumCount = albumIdsByArtist[key]?.size ?: 0
            )
        }
        .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
}

internal fun buildMetadataCategoryItems(
    songs: List<Song>,
    type: String
): List<MetadataCategoryItem> {
    val groups = linkedMapOf<String, MutableList<Song>>()
    val displayNames = linkedMapOf<String, String>()
    songs.forEach { song ->
        song.metadataCategoryNames(type).forEach { name ->
            val key = name.tagIdentityKey()
            displayNames.putIfAbsent(key, name)
            groups.getOrPut(key) { mutableListOf() } += song
        }
    }
    return groups
        .map { (key, items) ->
            MetadataCategoryItem(
                name = displayNames[key] ?: key,
                songCount = items.size,
                albumCount = items.map { it.albumIdentityId() }.distinct().size,
                duration = items.sumOf { it.duration },
                dateModified = items.maxOfOrNull { it.dateModified } ?: 0L,
                coverAlbumIds = items
                    .mapNotNull { it.albumId.takeIf { albumId -> albumId > 0L } }
                    .distinct()
                    .take(3)
            )
        }
        .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
}

internal fun filterSongsForMetadataCategory(
    songs: List<Song>,
    type: String,
    name: String
): List<Song> {
    val target = name.trim()
    if (target.isBlank()) return emptyList()
    return songs
        .filter { song -> song.metadataCategoryNames(type).any { it.equals(target, ignoreCase = NameSplitConfigStore.tagIgnoreCase) } }
        .sortedWith(
            compareBy<Song, String>(String.CASE_INSENSITIVE_ORDER) { it.album }
                .thenBy { if (it.discNumber > 0) it.discNumber else Int.MAX_VALUE }
                .thenBy { if (it.trackNumber > 0) it.trackNumber else Int.MAX_VALUE }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { song -> song.title }
        )
}

internal fun containsMetadataCategory(
    songs: List<Song>,
    type: String,
    name: String
): Boolean {
    val target = name.trim()
    if (target.isBlank()) return false
    return songs.any { song ->
        song.metadataCategoryNames(type).any { it.equals(target, ignoreCase = NameSplitConfigStore.tagIgnoreCase) }
    }
}

internal fun String.toFolderFilterList(): List<String> {
    return split('\n', ';', '；')
        .map { it.trim() }
        .filter { it.isNotBlank() }
}

internal fun buildOpenAiRecommendationCandidates(
    library: List<Song>,
    stats: List<SongPlaybackStats>,
    history: List<PlaybackHistoryEntry>,
    maxCandidates: Int = 160
): List<Song> {
    if (library.size <= maxCandidates) return library.distinctBy { it.playlistIdentityKey() }

    val songsById = library.associateBy { it.id }
    val selected = linkedMapOf<String, Song>()

    fun add(song: Song) {
        if (selected.size >= maxCandidates) return
        selected.putIfAbsent(song.playlistIdentityKey(), song)
    }

    history
        .mapNotNull { entry -> songsById[entry.songId] }
        .take(60)
        .forEach(::add)

    stats
        .sortedWith(
            compareByDescending<SongPlaybackStats> { it.playCount }
                .thenByDescending { it.listenedMs }
                .thenByDescending { it.lastPlayedAt }
        )
        .mapNotNull { stat -> songsById[stat.songId] }
        .take(60)
        .forEach(::add)

    stats
        .sortedByDescending { it.lastPlayedAt }
        .mapNotNull { stat -> songsById[stat.songId] }
        .take(40)
        .forEach(::add)

    library
        .sortedByDescending { it.dateModified }
        .take(40)
        .forEach(::add)

    val remaining = maxCandidates - selected.size
    if (remaining > 0) {
        val sortedLibrary = library.sortedWith(
            compareBy<Song, String>(String.CASE_INSENSITIVE_ORDER) { it.artist.ifBlank { it.albumArtist } }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { it.album }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { it.title }
        )
        val step = (sortedLibrary.size / remaining.coerceAtLeast(1)).coerceAtLeast(1)
        sortedLibrary.forEachIndexed { index, song ->
            if (selected.size < maxCandidates && index % step == 0) add(song)
        }
    }

    return selected.values.toList().ifEmpty { library.take(maxCandidates) }
}

internal fun buildPlaylistCustomOrder(
    customPlaylists: List<UserPlaylist>,
    currentOrder: List<String>,
    newPlaylistIds: List<String>
): List<String> {
    val customIds = customPlaylists.mapTo(linkedSetOf()) { it.id }
    if (customIds.isEmpty()) return emptyList()

    val newIds = newPlaylistIds
        .filter { it in customIds }
        .distinct()
    return buildList {
        addAll(newIds)
        currentOrder.forEach { id ->
            if (id in customIds && id !in this) add(id)
        }
        customPlaylists
            .sortedWith(
                compareByDescending<UserPlaylist> { it.createdAt }
                    .thenByDescending { it.updatedAt }
                    .thenBy { it.name.lowercase() }
                    .thenBy { it.id }
            )
            .forEach { playlist ->
                if (playlist.id !in this) add(playlist.id)
            }
    }
}
