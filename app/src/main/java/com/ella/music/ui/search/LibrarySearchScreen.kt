package com.ella.music.ui.search

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ella.music.R
import com.ella.music.data.model.Album
import com.ella.music.data.model.Artist
import com.ella.music.data.model.Song
import com.ella.music.ui.components.EllaSearchBar
import com.ella.music.ui.components.SafeCoverImage
import com.ella.music.ui.components.SongItem
import com.ella.music.ui.components.SongMoreActionHost
import com.ella.music.ui.components.ellaPageBackground
import com.ella.music.viewmodel.MainViewModel
import com.ella.music.viewmodel.PlayerViewModel
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun LibrarySearchScreen(
    mainViewModel: MainViewModel,
    playerViewModel: PlayerViewModel,
    onBack: () -> Unit,
    onNavigateToAlbum: (Long) -> Unit,
    onNavigateToArtist: (String) -> Unit,
    onNavigateToPlayer: () -> Unit
) {
    val context = LocalContext.current
    val songs by mainViewModel.songs.collectAsState()
    val albums by mainViewModel.albums.collectAsState()
    val currentSong by playerViewModel.currentSong.collectAsState()
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(SearchFilter.All) }
    var actionSong by remember { mutableStateOf<Song?>(null) }
    var history by remember { mutableStateOf(loadSearchHistory(context)) }

    val trimmedQuery = query.trim()
    val duplicateSongs = remember(songs) { songs.duplicateTitleAlbumSongs() }
    val songResults = remember(songs, trimmedQuery, filter, duplicateSongs) {
        when {
            filter == SearchFilter.Duplicates -> duplicateSongs
            trimmedQuery.isBlank() || filter !in listOf(SearchFilter.All, SearchFilter.Songs) -> emptyList()
            else -> songs.filter { it.matchesLibrarySearch(trimmedQuery) }.take(80)
        }
    }
    val albumResults = remember(albums, trimmedQuery, filter) {
        if (filter !in listOf(SearchFilter.All, SearchFilter.Albums) || trimmedQuery.isBlank()) emptyList()
        else albums.filter { it.matchesLibrarySearch(trimmedQuery) }.take(24)
    }
    val artistResults = remember(songs, trimmedQuery, filter) {
        if (filter !in listOf(SearchFilter.All, SearchFilter.Artists) || trimmedQuery.isBlank()) {
            emptyList()
        } else {
            songs.asSequence()
                .flatMap { song -> com.ella.music.data.splitArtistNames(song.artist).map { it to song } }
                .filter { (artist, _) -> artist.isNotBlank() && artist.contains(trimmedQuery, ignoreCase = true) }
                .groupBy({ it.first }, { it.second })
                .entries
                .sortedBy { it.key.lowercase() }
                .take(24)
                .map { (artist, artistSongs) ->
                    ArtistSearchResult(
                        artist = Artist(
                            name = artist,
                            songCount = artistSongs.size,
                            albumCount = artistSongs.map { it.album }.distinct().size
                        ),
                        representativeSong = artistSongs.firstOrNull()
                    )
                }
        }
    }

    fun commitSearch(text: String = query) {
        val value = text.trim()
        if (value.isBlank()) return
        history = saveSearchHistory(context, value)
    }

    BackHandler(onBack = onBack)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ellaPageBackground())
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = MiuixIcons.Regular.Back,
                    contentDescription = stringResource(R.string.common_back),
                    tint = MiuixTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp)
                )
            }
            EllaSearchBar(
                query = query,
                onQueryChange = {
                    query = it
                    if (filter == SearchFilter.Duplicates) filter = SearchFilter.All
                },
                onSearch = { commitSearch() },
                placeholder = stringResource(R.string.library_search_page_placeholder),
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SearchPill(
                text = stringResource(R.string.library_search_all),
                selected = filter == SearchFilter.All,
                onClick = { filter = SearchFilter.All }
            )
            SearchPill(
                text = stringResource(R.string.library_search_songs),
                selected = filter == SearchFilter.Songs,
                onClick = { filter = SearchFilter.Songs }
            )
            SearchPill(
                text = stringResource(R.string.library_search_albums),
                selected = filter == SearchFilter.Albums,
                onClick = { filter = SearchFilter.Albums }
            )
            SearchPill(
                text = stringResource(R.string.library_search_artists),
                selected = filter == SearchFilter.Artists,
                onClick = { filter = SearchFilter.Artists }
            )
            SearchPill(
                text = stringResource(R.string.library_search_duplicates),
                selected = filter == SearchFilter.Duplicates,
                onClick = {
                    filter = if (filter == SearchFilter.Duplicates) SearchFilter.All else SearchFilter.Duplicates
                    if (filter == SearchFilter.Duplicates) query = ""
                }
            )
            if (history.isNotEmpty()) {
                SearchPill(
                    text = stringResource(R.string.library_search_clear_history),
                    selected = false,
                    onClick = {
                        history = emptyList()
                        saveSearchHistory(context, emptyList())
                    }
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 128.dp)
        ) {
            if (trimmedQuery.isBlank() && filter != SearchFilter.Duplicates) {
                if (history.isNotEmpty()) {
                    item { SearchSectionHeader(stringResource(R.string.library_search_history)) }
                    items(history, key = { it }) { item ->
                        HistoryRow(
                            text = item,
                            onClick = {
                                query = item
                                filter = SearchFilter.All
                            },
                            onDelete = {
                                history = history - item
                                saveSearchHistory(context, history)
                            }
                        )
                    }
                } else {
                    item { EmptySearchHint(stringResource(R.string.library_search_empty_hint)) }
                }
            } else {
                if (filter == SearchFilter.Duplicates) {
                    item { SearchSectionHeader(stringResource(R.string.library_search_duplicates)) }
                }
                if (songResults.isNotEmpty()) {
                    item { SearchSectionHeader(stringResource(R.string.library_search_songs)) }
                    items(songResults, key = { "${it.id}:${it.path}" }) { song ->
                        SongItem(
                            song = song,
                            isCurrent = currentSong?.id == song.id,
                            loadCoverArt = mainViewModel::getCoverArtBitmap,
                            loadAudioInfo = mainViewModel::getAudioInfo,
                            onClick = {
                                val index = songResults.indexOfFirst { it.id == song.id && it.path == song.path }.coerceAtLeast(0)
                                playerViewModel.setPlaylist(songResults, index)
                                commitSearch()
                                onNavigateToPlayer()
                            },
                            onLongClick = { actionSong = song },
                            onMore = { actionSong = song }
                        )
                    }
                }
                if (albumResults.isNotEmpty()) {
                    item { SearchSectionHeader(stringResource(R.string.library_search_albums)) }
                    items(albumResults, key = { it.id }) { album ->
                        AlbumResultRow(
                            album = album,
                            coverModel = mainViewModel.getAlbumArtUri(album.artAlbumId),
                            onClick = {
                                commitSearch()
                                onNavigateToAlbum(album.id)
                            }
                        )
                    }
                }
                if (artistResults.isNotEmpty()) {
                    item { SearchSectionHeader(stringResource(R.string.library_search_artists)) }
                    items(artistResults, key = { it.artist.name }) { result ->
                        ArtistResultRow(
                            result = result,
                            coverModel = result.representativeSong?.coverUrl?.takeIf { it.isNotBlank() }
                                ?: result.representativeSong?.let { mainViewModel.getAlbumArtUri(it.albumId) },
                            onClick = {
                                commitSearch()
                                onNavigateToArtist(result.artist.name)
                            }
                        )
                    }
                }
                if (songResults.isEmpty() && albumResults.isEmpty() && artistResults.isEmpty()) {
                    item {
                        EmptySearchHint(
                            if (filter == SearchFilter.Duplicates) stringResource(R.string.library_search_no_duplicates)
                            else stringResource(R.string.library_search_no_results)
                        )
                    }
                }
            }
        }
    }

    SongMoreActionHost(
        actionSong = actionSong,
        mainViewModel = mainViewModel,
        playerViewModel = playerViewModel,
        onDismissAction = { actionSong = null },
        onNavigateToAlbum = onNavigateToAlbum,
        onNavigateToArtist = onNavigateToArtist
    )
}

private enum class SearchFilter {
    All,
    Songs,
    Albums,
    Artists,
    Duplicates
}

private data class ArtistSearchResult(
    val artist: Artist,
    val representativeSong: Song?
)

@Composable
private fun SearchPill(text: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = text,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        color = if (selected) MiuixTheme.colorScheme.onPrimary else MiuixTheme.colorScheme.onSurface,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.82f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    )
}

@Composable
private fun SearchSectionHeader(text: String) {
    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
    )
}

@Composable
private fun HistoryRow(text: String, onClick: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = text, fontSize = 15.sp, color = MiuixTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        Text(
            text = stringResource(R.string.common_delete),
            fontSize = 13.sp,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onDelete)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun AlbumResultRow(album: Album, coverModel: Any?, onClick: () -> Unit) {
    SearchResultRow(
        title = album.name,
        subtitle = "${album.artist} · ${album.songCount} ${stringResource(R.string.library_search_song_unit)}",
        coverModel = coverModel,
        onClick = onClick
    )
}

@Composable
private fun ArtistResultRow(result: ArtistSearchResult, coverModel: Any?, onClick: () -> Unit) {
    SearchResultRow(
        title = result.artist.name,
        subtitle = "${result.artist.songCount} ${stringResource(R.string.library_search_song_unit)}",
        coverModel = coverModel,
        roundCover = true,
        onClick = onClick
    )
}

@Composable
private fun SearchResultRow(
    title: String,
    subtitle: String,
    coverModel: Any?,
    roundCover: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(if (roundCover) CircleShape else RoundedCornerShape(10.dp))
                .background(MiuixTheme.colorScheme.surfaceContainer),
            contentAlignment = Alignment.Center
        ) {
            SafeCoverImage(
                model = coverModel,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                sizePx = 128
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp)
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MiuixTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun EmptySearchHint(text: String) {
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 56.dp), contentAlignment = Alignment.Center) {
        Text(text = text, fontSize = 14.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
    }
}

private fun Song.matchesLibrarySearch(query: String): Boolean =
    title.contains(query, ignoreCase = true) ||
        artist.contains(query, ignoreCase = true) ||
        album.contains(query, ignoreCase = true) ||
        fileName.contains(query, ignoreCase = true)

private fun Album.matchesLibrarySearch(query: String): Boolean =
    name.contains(query, ignoreCase = true) ||
        artist.contains(query, ignoreCase = true) ||
        albumArtist.contains(query, ignoreCase = true)

private fun List<Song>.duplicateTitleAlbumSongs(): List<Song> =
    groupBy { "${it.title.trim().lowercase()}|${it.album.trim().lowercase()}" }
        .values
        .filter { it.size > 1 }
        .flatten()
        .sortedWith(compareBy<Song> { it.album.lowercase() }.thenBy { it.title.lowercase() }.thenBy { it.artist.lowercase() })

private fun loadSearchHistory(context: Context): List<String> =
    context.getSharedPreferences(SEARCH_PREFS, Context.MODE_PRIVATE)
        .getString(SEARCH_HISTORY_KEY, "")
        .orEmpty()
        .split("\n")
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()

private fun saveSearchHistory(context: Context, query: String): List<String> {
    val next = (listOf(query.trim()) + loadSearchHistory(context))
        .filter { it.isNotBlank() }
        .distinct()
        .take(20)
    saveSearchHistory(context, next)
    return next
}

private fun saveSearchHistory(context: Context, history: List<String>) {
    context.getSharedPreferences(SEARCH_PREFS, Context.MODE_PRIVATE)
        .edit()
        .putString(SEARCH_HISTORY_KEY, history.joinToString("\n"))
        .apply()
}

private const val SEARCH_PREFS = "library_search"
private const val SEARCH_HISTORY_KEY = "history"
