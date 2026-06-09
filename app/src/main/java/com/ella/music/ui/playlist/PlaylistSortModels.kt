package com.ella.music.ui.playlist

import android.net.Uri
import com.ella.music.R
import com.ella.music.data.model.Song
import com.ella.music.data.model.UserPlaylist
import com.ella.music.data.model.formatPlaybackDuration
import java.util.Locale

internal enum class PlaylistSortMode(val labelRes: Int) {
    Custom(R.string.playlist_sort_custom),
    CustomDesc(R.string.playlist_sort_custom_desc),
    UpdatedAt(R.string.playlist_sort_updated_at),
    CreatedAt(R.string.playlist_sort_created_at_desc),
    CreatedAtAsc(R.string.playlist_sort_created_at),
    Name(R.string.playlist_sort_name),
    SongCount(R.string.playlist_sort_song_count),
    Duration(R.string.playlist_sort_duration)
}

internal fun List<UserPlaylist>.sortedForPlaylistList(mode: PlaylistSortMode): List<UserPlaylist> {
    return when (mode) {
        PlaylistSortMode.Custom -> this
        PlaylistSortMode.CustomDesc -> asReversed()
        PlaylistSortMode.UpdatedAt -> sortedWith(
            compareByDescending<UserPlaylist> { it.updatedAt }
                .thenByDescending { it.createdAt }
                .thenBy { it.name.lowercase(Locale.ROOT) }
                .thenBy { it.id }
        )
        PlaylistSortMode.CreatedAt -> sortedWith(
            compareByDescending<UserPlaylist> { it.createdAt }
                .thenByDescending { it.updatedAt }
                .thenBy { it.name.lowercase(Locale.ROOT) }
                .thenBy { it.id }
        )
        PlaylistSortMode.CreatedAtAsc -> sortedWith(
            compareBy<UserPlaylist> { it.createdAt }
                .thenByDescending { it.updatedAt }
                .thenBy { it.name.lowercase(Locale.ROOT) }
                .thenBy { it.id }
        )
        PlaylistSortMode.Name -> sortedWith(
            compareBy<UserPlaylist> { it.name.lowercase(Locale.ROOT) }
                .thenByDescending { it.createdAt }
                .thenBy { it.id }
        )
        PlaylistSortMode.SongCount -> sortedWith(
            compareByDescending<UserPlaylist> { it.songs.size }
                .thenByDescending { it.updatedAt }
                .thenByDescending { it.createdAt }
                .thenBy { it.name.lowercase(Locale.ROOT) }
                .thenBy { it.id }
        )
        PlaylistSortMode.Duration -> sortedWith(
            compareByDescending<UserPlaylist> { playlist -> playlist.songs.sumOf { it.duration } }
                .thenByDescending { it.updatedAt }
                .thenByDescending { it.createdAt }
                .thenBy { it.name.lowercase(Locale.ROOT) }
                .thenBy { it.id }
        )
    }
}

internal fun List<UserPlaylist>.applyPlaylistCustomOrder(orderedIds: List<String>): List<UserPlaylist> {
    if (isEmpty()) return emptyList()
    val fallbackComparator =
        compareByDescending<UserPlaylist> { it.createdAt }
            .thenByDescending { it.updatedAt }
            .thenBy { it.name.lowercase(Locale.ROOT) }
            .thenBy { it.id }
    if (orderedIds.isEmpty()) return sortedWith(fallbackComparator)

    val playlistsById = associateBy(UserPlaylist::id)
    return buildList {
        val orderedIdSet = orderedIds.toSet()
        addAll(filterNot { it.id in orderedIdSet }.sortedWith(fallbackComparator))
        orderedIds.forEach { id ->
            playlistsById[id]?.let { playlist ->
                add(playlist)
            }
        }
    }
}

internal fun UserPlaylist.matchesPlaylistSearch(query: String): Boolean {
    if (query.isBlank()) return true
    return name.contains(query, ignoreCase = true) ||
        songs.any { song ->
            song.title.contains(query, ignoreCase = true) ||
                song.artist.contains(query, ignoreCase = true) ||
                song.album.contains(query, ignoreCase = true)
        }
}

internal enum class PlaylistSongSortMode(val labelRes: Int) {
    Custom(R.string.playlist_song_sort_custom),
    CustomDesc(R.string.playlist_song_sort_custom_desc),
    AddedAt(R.string.playlist_song_sort_added_at),
    Title(R.string.playlist_song_sort_title),
    FileName(R.string.playlist_song_sort_file_name),
    Duration(R.string.playlist_song_sort_duration),
    YearAsc(R.string.playlist_song_sort_year_asc),
    YearDesc(R.string.playlist_song_sort_year_desc),
    DateAdded(R.string.playlist_song_sort_date_added),
    DateAddedAsc(R.string.playlist_song_sort_date_added_asc),
    DateModified(R.string.playlist_song_sort_date_modified),
    DateModifiedAsc(R.string.playlist_song_sort_date_modified_asc)
}

internal fun List<Song>.sortedForPlaylistDetail(mode: PlaylistSongSortMode): List<Song> {
    return when (mode) {
        PlaylistSongSortMode.Custom -> this
        PlaylistSongSortMode.CustomDesc -> asReversed()
        PlaylistSongSortMode.AddedAt -> this
        PlaylistSongSortMode.Title -> sortedBy { it.title.lowercase() }
        PlaylistSongSortMode.FileName -> sortedBy { song ->
            song.fileName.ifBlank { song.path.substringAfterLast('/') }.lowercase()
        }
        PlaylistSongSortMode.Duration -> sortedByDescending { it.duration }
        PlaylistSongSortMode.YearAsc -> sortedByReleaseDate(ascending = true)
        PlaylistSongSortMode.YearDesc -> sortedByReleaseDate(ascending = false)
        PlaylistSongSortMode.DateAdded -> sortedByDescending { it.dateAdded }
        PlaylistSongSortMode.DateAddedAsc -> sortedBy { it.dateAdded }
        PlaylistSongSortMode.DateModified -> sortedByDescending { it.dateModified }
        PlaylistSongSortMode.DateModifiedAsc -> sortedBy { it.dateModified }
    }
}

private fun List<Song>.sortedByReleaseDate(ascending: Boolean): List<Song> {
    val comparator = if (ascending) {
        compareBy<Song> { it.releaseYearOrNull() == null }
            .thenBy { it.releaseYearOrNull() ?: Int.MAX_VALUE }
    } else {
        compareBy<Song> { it.releaseYearOrNull() == null }
            .thenByDescending { it.releaseYearOrNull() ?: Int.MIN_VALUE }
    }
    return sortedWith(
        comparator
            .thenBy { it.album.lowercase() }
            .thenBy { if (it.discNumber > 0) it.discNumber else Int.MAX_VALUE }
            .thenBy { if (it.trackNumber > 0) it.trackNumber else Int.MAX_VALUE }
            .thenBy { it.title.lowercase() }
    )
}

private fun Song.releaseYearOrNull(): Int? =
    Regex("""\d{4}""").find(year)?.value?.toIntOrNull()

internal fun Long.formatPlaylistDuration(): String {
    return formatPlaybackDuration()
}

internal fun Song?.playlistCoverModel(): Any? {
    val song = this ?: return null
    return song.coverUrl.takeIf { it.isNotBlank() }
        ?: song.albumId.takeIf { it > 0L }?.let { Uri.parse("content://media/external/audio/albumart/$it") }
}

internal fun String.safePlaylistFileName(): String =
    replace(Regex("[\\\\/:*?\"<>|]"), "_").trim().ifBlank { "Halcyon Playlist" }
