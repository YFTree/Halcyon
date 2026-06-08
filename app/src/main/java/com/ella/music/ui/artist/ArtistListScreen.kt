package com.ella.music.ui.artist

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.annotation.StringRes
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ella.music.R
import com.ella.music.data.model.FAVORITES_PLAYLIST_ID
import com.ella.music.data.model.Artist
import com.ella.music.data.model.Song
import com.ella.music.data.model.UserPlaylist
import com.ella.music.data.model.formatPlaybackDuration
import com.ella.music.data.splitArtistNames
import com.ella.music.data.tagIdentityKey
import com.ella.music.ui.LibrarySortUiState
import com.ella.music.ui.components.AddToPlaylistSheet
import com.ella.music.ui.components.CreatePlaylistAndAddSheet
import com.ella.music.ui.components.DoubleTapScrollOverlay
import com.ella.music.ui.components.EllaSearchBar
import com.ella.music.ui.components.FastIndexBar
import com.ella.music.ui.components.LazyListScrollIndicator
import com.ella.music.ui.components.SafeCoverImage
import com.ella.music.ui.components.ArtworkUsage
import com.ella.music.ui.components.ellaPageBackground
import com.ella.music.ui.components.rememberSongArtworkState
import com.ella.music.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import com.ella.music.ui.components.EllaSmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.Search
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Music
import top.yukonga.miuix.kmp.icon.extended.SelectAll
import top.yukonga.miuix.kmp.icon.extended.Sort
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowBottomSheet

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun ArtistListScreen(
    mainViewModel: MainViewModel,
    onBack: () -> Unit,
    onArtistClick: (String) -> Unit
) {
    val context = LocalContext.current
    val songs by mainViewModel.songs.collectAsState()
    val albums by mainViewModel.albums.collectAsState()
    val playlists by mainViewModel.playlists.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var searchExpanded by remember { mutableStateOf(false) }
    var sortExpanded by remember { mutableStateOf(false) }
    var selectionMode by remember { mutableStateOf(false) }
    var selectedArtistKeys by remember { mutableStateOf(setOf<String>()) }
    var playlistPickerSongs by remember { mutableStateOf<List<Song>?>(null) }
    var createPlaylistSongs by remember { mutableStateOf<List<Song>?>(null) }
    val sortIndex by mainViewModel.settingsManager.artistListSortIndex.collectAsState(initial = LibrarySortUiState.artistListSortIndex)
    val showAlbumArtists by mainViewModel.settingsManager.showAlbumArtists.collectAsState(initial = false)
    val tagIgnoreCase by mainViewModel.settingsManager.tagIgnoreCase.collectAsState(initial = false)
    val sortMode = ArtistSortMode.entries.getOrElse(sortIndex) { ArtistSortMode.Name }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var scrollToTopRequest by remember { mutableStateOf(0) }
    var listCoversEnabled by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(220L)
        listCoversEnabled = true
    }

    val artists = remember(songs, albums, showAlbumArtists, tagIgnoreCase) { mainViewModel.getArtists(showAlbumArtists) }
    val representativeSongsByArtist = remember(songs, showAlbumArtists, tagIgnoreCase) {
        buildMap {
            songs.forEach { song ->
                val names = if (showAlbumArtists) {
                    splitArtistNames(song.artist) + splitArtistNames(song.albumArtist)
                } else {
                    splitArtistNames(song.artist)
                }
                names.forEach { artistName ->
                    putIfAbsent(artistName.tagIdentityKey(), song)
                }
            }
        }
    }
    val artistDurations = remember(songs, tagIgnoreCase) {
        buildMap {
            songs.forEach { song ->
                splitArtistNames(song.artist).forEach { artistName ->
                    val key = artistName.tagIdentityKey()
                    put(key, (get(key) ?: 0L) + song.duration)
                }
            }
        }
    }
    val releaseAlbumCounts = remember(albums, showAlbumArtists, tagIgnoreCase) {
        buildMap {
            if (!showAlbumArtists) return@buildMap
            albums.forEach { album ->
                splitArtistNames(album.albumArtist).forEach { artistName ->
                    val key = artistName.tagIdentityKey()
                    put(key, (get(key) ?: 0) + 1)
                }
            }
        }
    }
    val filteredArtists = remember(artists, searchQuery, sortMode, artistDurations, releaseAlbumCounts) {
        val filtered = if (searchQuery.isBlank()) {
            artists
        } else {
            artists.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
        when (sortMode) {
            ArtistSortMode.Name -> filtered.sortedBy { it.name.lowercase() }
            ArtistSortMode.SongCount -> filtered.sortedByDescending { it.songCount }
            ArtistSortMode.AlbumCount -> filtered.sortedByDescending { it.albumCount }
            ArtistSortMode.ReleaseAlbumCount -> filtered.sortedByDescending { releaseAlbumCounts[it.name.tagIdentityKey()] ?: 0 }
            ArtistSortMode.Duration -> filtered.sortedByDescending { artistDurations[it.name.tagIdentityKey()] ?: 0L }
        }
    }

    fun finishSelectionMode() {
        selectionMode = false
        selectedArtistKeys = emptySet()
    }
    fun toggleArtistSelection(artist: Artist) {
        val key = artist.name.tagIdentityKey()
        val next = if (key in selectedArtistKeys) selectedArtistKeys - key else selectedArtistKeys + key
        selectedArtistKeys = next
        if (next.isEmpty()) selectionMode = false
    }
    fun selectedArtistSongs(): List<Song> {
        if (selectedArtistKeys.isEmpty()) return emptyList()
        return songs.filter { song ->
            val names = if (showAlbumArtists) splitArtistNames(song.artist) + splitArtistNames(song.albumArtist)
            else splitArtistNames(song.artist)
            names.any { it.tagIdentityKey() in selectedArtistKeys }
        }.distinctBy { it.id }
    }

    BackHandler(enabled = selectionMode || searchExpanded || sortExpanded) {
        when {
            selectionMode -> finishSelectionMode()
            searchExpanded -> {
                searchExpanded = false
                searchQuery = ""
            }
            sortExpanded -> sortExpanded = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ellaPageBackground())
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Box {
            EllaSmallTopAppBar(
                title = if (selectionMode) stringResource(R.string.library_selected_count, selectedArtistKeys.size) else stringResource(R.string.category_artist),
                color = ellaPageBackground(),
                navigationIcon = {
                    IconButton(onClick = { if (selectionMode) finishSelectionMode() else onBack() }) {
                        Icon(
                            imageVector = MiuixIcons.Regular.Back,
                            contentDescription = stringResource(R.string.common_back),
                            tint = MiuixTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                actions = {
                    if (selectionMode) {
                        IconButton(onClick = {
                            val selectedSongs = selectedArtistSongs()
                            if (selectedSongs.isNotEmpty()) playlistPickerSongs = selectedSongs
                        }) {
                            Icon(
                                imageVector = MiuixIcons.Regular.Add,
                                contentDescription = stringResource(R.string.player_add_to_playlist),
                                tint = MiuixTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    } else {
                        IconButton(onClick = {
                            selectionMode = true
                            selectedArtistKeys = filteredArtists.mapTo(mutableSetOf()) { it.name.tagIdentityKey() }
                        }) {
                            Icon(
                                imageVector = MiuixIcons.Regular.SelectAll,
                                contentDescription = stringResource(R.string.common_multi_select),
                                tint = MiuixTheme.colorScheme.onSurface,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        IconButton(onClick = { sortExpanded = !sortExpanded }) {
                            Icon(
                                imageVector = MiuixIcons.Regular.Sort,
                                contentDescription = stringResource(R.string.common_sort),
                                tint = MiuixTheme.colorScheme.onSurface,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        IconButton(onClick = { searchExpanded = !searchExpanded }) {
                            Icon(
                                imageVector = MiuixIcons.Basic.Search,
                                contentDescription = stringResource(R.string.common_search),
                                tint = MiuixTheme.colorScheme.onSurface,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            )
            DoubleTapScrollOverlay(
                onDoubleTap = { scrollToTopRequest++ },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            )
        }

        AnimatedVisibility(
            visible = sortExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                ArtistSortMode.entries.forEach { mode ->
                    Text(
                        text = stringResource(mode.labelRes),
                        fontSize = 14.sp,
                        fontWeight = if (sortMode == mode) FontWeight.Bold else FontWeight.Normal,
                        color = if (sortMode == mode) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                LibrarySortUiState.artistListSortIndex = mode.ordinal
                                scope.launch { mainViewModel.settingsManager.setArtistListSortIndex(mode.ordinal) }
                                scrollToTopRequest++
                                sortExpanded = false
                            }
                            .padding(vertical = 10.dp)
                    )
                }
            }
        }

        if (searchExpanded) {
            EllaSearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                onSearch = { searchExpanded = false },
                placeholder = stringResource(R.string.artist_list_search_placeholder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }

        if (filteredArtists.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = stringResource(R.string.artist_list_empty), color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
            }
        } else {
            val listState = rememberLazyListState()
            var fastScrollJob by remember { mutableStateOf<Job?>(null) }
            LaunchedEffect(scrollToTopRequest) {
                if (scrollToTopRequest > 0) listState.animateScrollToItem(0)
            }
            val fastIndexTargets = remember(filteredArtists) {
                filteredArtists
                    .mapIndexed { index, artist -> artist.indexLetter() to index + 1 }
                    .distinctBy { it.first }
                    .toMap()
            }
            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 160.dp)
                ) {
                    item {
                        Text(
                            text = stringResource(R.string.artist_list_summary, filteredArtists.size, stringResource(sortMode.labelRes)),
                            fontSize = 13.sp,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    items(filteredArtists, key = { it.name }) { artist ->
                        val artistKey = artist.name.tagIdentityKey()
                        val selected = artistKey in selectedArtistKeys
                        ArtistRow(
                            artist = artist,
                            representativeSong = representativeSongsByArtist[artistKey],
                            mainViewModel = mainViewModel,
                            coversEnabled = listCoversEnabled,
                            selectionMode = selectionMode,
                            selected = selected,
                            summary = artist.summaryForSort(
                                sortMode = sortMode,
                                duration = artistDurations[artistKey] ?: 0L,
                                releaseAlbumCount = releaseAlbumCounts[artistKey] ?: 0,
                                stringResolver = { resId, args -> context.getString(resId, *args) }
                            ),
                            onClick = {
                                if (selectionMode) toggleArtistSelection(artist) else onArtistClick(artist.name)
                            },
                            onLongClick = {
                                if (selectionMode) {
                                    toggleArtistSelection(artist)
                                    return@ArtistRow
                                }
                                selectionMode = true
                                selectedArtistKeys = selectedArtistKeys + artistKey
                            }
                        )
                    }
                }

                if (sortMode == ArtistSortMode.Name && filteredArtists.size > 30) {
                    FastIndexBar(
                        letters = filteredArtists.map { it.indexLetter() },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight()
                            .padding(end = 2.dp),
                        onLetterClick = { letter ->
                            val index = fastIndexTargets[letter]
                            if (index != null) {
                                fastScrollJob?.cancel()
                                fastScrollJob = scope.launch { listState.scrollToItem(index) }
                            }
                        }
                    )
                } else if (filteredArtists.size > 30) {
                    LazyListScrollIndicator(
                        state = listState,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight()
                    )
                }
            }
        }
    }

    playlistPickerSongs?.let { songsToAdd ->
        WindowBottomSheet(
            show = true,
            enableNestedScroll = false,
            title = stringResource(R.string.player_add_to_playlist),
            onDismissRequest = { playlistPickerSongs = null }
        ) {
            AddToPlaylistSheet(
                playlists = playlists.sortedWith(
                    compareByDescending<UserPlaylist> { it.id == FAVORITES_PLAYLIST_ID }
                        .thenByDescending { it.createdAt }
                ),
                songCount = songsToAdd.size,
                onDismiss = { playlistPickerSongs = null },
                onCreatePlaylist = {
                    createPlaylistSongs = songsToAdd
                    playlistPickerSongs = null
                },
                onPlaylistsConfirm = { selectedPlaylists, appendToEnd ->
                    selectedPlaylists.forEach { playlist ->
                        mainViewModel.addSongsToPlaylist(playlist.id, songsToAdd, appendToEnd)
                    }
                    playlistPickerSongs = null
                    finishSelectionMode()
                }
            )
        }
    }

    createPlaylistSongs?.let { songsToAdd ->
        CreatePlaylistAndAddSheet(
            onDismiss = { createPlaylistSongs = null },
            onCreate = { playlistName ->
                mainViewModel.createPlaylist(playlistName) { playlist ->
                    if (playlist != null) mainViewModel.addSongsToPlaylist(playlist.id, songsToAdd)
                }
                createPlaylistSongs = null
                finishSelectionMode()
            }
        )
    }
}

private fun Artist.indexLetter(): String {
    val first = name.trim().firstOrNull()?.uppercaseChar()
    return if (first != null && first in 'A'..'Z') first.toString() else "#"
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun ArtistRow(
    artist: Artist,
    representativeSong: Song?,
    mainViewModel: MainViewModel,
    coversEnabled: Boolean,
    selectionMode: Boolean,
    selected: Boolean,
    summary: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val albumArtUri = remember(coversEnabled, representativeSong?.albumId) {
        representativeSong
            ?.albumId
            ?.takeIf { coversEnabled && it > 0L }
            ?.let(mainViewModel::getAlbumArtUri)
    }
    val coverState = rememberSongArtworkState(
        song = representativeSong,
        albumArtUri = albumArtUri,
        loadCoverArt = mainViewModel::getAlbumCoverArtBitmap,
        usage = ArtworkUsage.ArtistImage,
        showDefaultWhenMissing = false
    )
    val coverModel: Any? = coverState.model

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) MiuixTheme.colorScheme.primary.copy(alpha = 0.10f) else androidx.compose.ui.graphics.Color.Transparent)
            .height(76.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (selectionMode) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (selected) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.surfaceContainer),
                contentAlignment = Alignment.Center
            ) {
                if (selected) Text(text = "✓", fontSize = 14.sp, color = androidx.compose.ui.graphics.Color.White)
            }
            Spacer(modifier = Modifier.size(12.dp))
        }
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(MiuixTheme.colorScheme.surfaceContainer),
            contentAlignment = Alignment.Center
        ) {
            if (coverModel != null) {
                SafeCoverImage(
                    model = coverModel,
                    contentDescription = null,
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                    sizePx = 128,
                    showDefaultPlaceholder = false
                )
            } else {
                Icon(
                    imageVector = MiuixIcons.Regular.Music,
                    contentDescription = null,
                    tint = MiuixTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(modifier = Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = artist.name.ifBlank { stringResource(R.string.player_unknown_artist) },
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MiuixTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = summary,
                fontSize = 12.sp,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private enum class ArtistSortMode(@StringRes val labelRes: Int) {
    Name(R.string.artist_list_sort_name),
    SongCount(R.string.artist_list_sort_song_count),
    AlbumCount(R.string.artist_list_sort_album_count),
    ReleaseAlbumCount(R.string.artist_list_sort_release_album_count),
    Duration(R.string.artist_list_sort_duration)
}

private fun Artist.summaryForSort(
    sortMode: ArtistSortMode,
    duration: Long,
    releaseAlbumCount: Int,
    stringResolver: (Int, Array<Any>) -> String
): String {
    return when (sortMode) {
        ArtistSortMode.Duration -> stringResolver(R.string.artist_list_summary_duration, arrayOf(duration.formatArtistDuration(), albumCount))
        ArtistSortMode.ReleaseAlbumCount -> stringResolver(R.string.artist_list_summary_release_album, arrayOf(songCount, releaseAlbumCount))
        else -> stringResolver(R.string.artist_list_summary_default, arrayOf(songCount, albumCount))
    }
}

private fun Long.formatArtistDuration(): String {
    return formatPlaybackDuration()
}
