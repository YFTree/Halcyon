package com.ella.music.ui.category

import android.app.Activity
import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ella.music.R
import com.ella.music.data.LibraryAlbumAggregator
import com.ella.music.data.exception.WritePermissionRequiredException
import com.ella.music.data.model.Album
import com.ella.music.data.model.FAVORITES_PLAYLIST_ID
import com.ella.music.data.model.Song
import com.ella.music.data.model.UserPlaylist
import com.ella.music.data.model.formatPlaybackDuration
import com.ella.music.data.model.playlistIdentityKey
import com.ella.music.ui.LibrarySortUiState
import com.ella.music.viewmodel.MainViewModel
import com.ella.music.viewmodel.MetadataCategoryItem
import com.ella.music.viewmodel.PlayerViewModel
import com.ella.music.ui.components.ConfirmDangerDialog
import com.ella.music.ui.components.AddToPlaylistSheet
import com.ella.music.ui.components.DoubleTapScrollOverlay
import com.ella.music.ui.components.EllaSearchBar
import com.ella.music.ui.components.EllaMiuixBottomSheet
import com.ella.music.ui.components.EllaMiuixSheetActions
import com.ella.music.ui.components.EllaMiuixTextField
import com.ella.music.ui.components.FolderOutlineIcon
import com.ella.music.ui.components.LazyGridScrollIndicator
import com.ella.music.ui.components.LocateCurrentSongFloatingButton
import com.ella.music.ui.components.DefaultAlbumCover
import com.ella.music.ui.components.SafeCoverImage
import com.ella.music.ui.components.SongItem
import com.ella.music.ui.components.SongMoreActionHost
import com.ella.music.ui.components.ArtworkUsage
import com.ella.music.ui.components.ellaPageBackground
import com.ella.music.ui.components.rememberSongArtworkState
import com.ella.music.ui.components.requestPinnedEllaShortcut
import com.ella.music.ui.navigation.Screen
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import com.ella.music.ui.components.EllaSmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.Search
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.Music
import top.yukonga.miuix.kmp.icon.extended.SelectAll
import top.yukonga.miuix.kmp.icon.extended.Sort
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowBottomSheet
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun MetadataCategoryScreen(
    type: String,
    mainViewModel: MainViewModel,
    playerViewModel: PlayerViewModel,
    onBack: () -> Unit,
    onCategoryClick: (String) -> Unit
) {
    val context = LocalContext.current
    val songs by mainViewModel.songs.collectAsState()
    val items = remember(type, songs) { mainViewModel.getMetadataCategoryItems(type) }
    var sortExpanded by remember { mutableStateOf(false) }
    val sortIndexFlow = remember(type) { mainViewModel.settingsManager.metadataCategorySortIndex(type) }
    val sortIndex by sortIndexFlow.collectAsState(initial = 0)
    val availableSortModes = remember(type) { MetadataCategorySortMode.entries.filter { it.availableFor(type) } }
    val sortMode = availableSortModes.getOrElse(sortIndex) { MetadataCategorySortMode.Name }
    val sortedItems = remember(items, sortMode) { items.sortedForCategory(sortMode) }
    var searchExpanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var categoryMenuItem by remember { mutableStateOf<MetadataCategoryItem?>(null) }
    val displayedItems = remember(sortedItems, searchQuery, type) {
        val query = searchQuery.trim()
        if (query.isBlank()) {
            sortedItems
        } else {
            sortedItems.filter { it.matchesCategorySearch(query, type) }
        }
    }
    val representativeSongsByName = remember(type, items, songs) {
        items.associate { item ->
            item.name to mainViewModel.getSongsForMetadataCategory(type, item.name).firstOrNull()
        }
    }
    val albumArtUrisByName = remember(items) {
        items.associate { item ->
            item.name to item.coverAlbumIds.firstOrNull()?.let(mainViewModel::getAlbumArtUri)
        }
    }
    val gridColumns by mainViewModel.settingsManager.categoryGridColumns.collectAsState(initial = 2)
    val configuration = LocalConfiguration.current
    val safeGridColumns = if (type.usesSingleColumnCategory()) {
        1
    } else if (configuration.smallestScreenWidthDp >= 600) {
        gridColumns.coerceIn(5, 8)
    } else {
        gridColumns.coerceIn(1, 4)
    }
    val pageBackground = ellaPageBackground()
    val savedCategoryScroll = remember(type) {
        LibrarySortUiState.metadataCategoryScrollPositions[type] ?: (0 to 0)
    }
    val gridState = rememberLazyGridState(
        initialFirstVisibleItemIndex = savedCategoryScroll.first,
        initialFirstVisibleItemScrollOffset = savedCategoryScroll.second
    )
    val scope = rememberCoroutineScope()
    var skipInitialCategoryReset by remember(type) { mutableStateOf(true) }
    LaunchedEffect(type, sortMode, searchQuery, safeGridColumns) {
        if (skipInitialCategoryReset) {
            skipInitialCategoryReset = false
        } else {
            gridState.scrollToItem(0)
        }
    }
    LaunchedEffect(type, gridState) {
        snapshotFlow { gridState.firstVisibleItemIndex to gridState.firstVisibleItemScrollOffset }
            .collect { position ->
                LibrarySortUiState.metadataCategoryScrollPositions[type] = position
            }
    }
    BackHandler(enabled = sortExpanded || searchExpanded) {
        when {
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
            .background(pageBackground)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Box {
            EllaSmallTopAppBar(
                title = type.categoryTitle(),
                color = pageBackground,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = MiuixIcons.Regular.Back,
                            contentDescription = stringResource(R.string.common_back),
                            tint = MiuixTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { sortExpanded = !sortExpanded }) {
                        Icon(
                            imageVector = MiuixIcons.Regular.Sort,
                            contentDescription = stringResource(R.string.common_sort),
                            tint = MiuixTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    IconButton(onClick = {
                        searchExpanded = !searchExpanded
                        if (!searchExpanded) searchQuery = ""
                    }) {
                        Icon(
                            imageVector = MiuixIcons.Basic.Search,
                            contentDescription = stringResource(R.string.common_search),
                            tint = MiuixTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            )
            DoubleTapScrollOverlay(
                onDoubleTap = { scope.launch { gridState.animateScrollToItem(0) } },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                endPadding = 112.dp
            )
        }

        AnimatedVisibility(
            visible = searchExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            EllaSearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                onSearch = { searchExpanded = false },
                placeholder = stringResource(R.string.category_search_placeholder, type.categoryTitle()),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }

        AnimatedVisibility(
            visible = sortExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                availableSortModes.forEach { mode ->
                    Text(
                        text = mode.displayLabel(type),
                        fontSize = 14.sp,
                        fontWeight = if (sortMode == mode) FontWeight.Bold else FontWeight.Normal,
                        color = if (sortMode == mode) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                sortExpanded = false
                                scope.launch { mainViewModel.settingsManager.setMetadataCategorySortIndex(type, availableSortModes.indexOf(mode)) }
                                scope.launch { gridState.scrollToItem(0) }
                            }
                            .padding(vertical = 10.dp)
                    )
                }
            }
        }

        if (displayedItems.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = if (searchQuery.isBlank()) stringResource(R.string.category_empty_hint, type.categoryTitle()) else stringResource(R.string.category_no_match, type.categoryTitle()),
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    fontSize = 14.sp
                )
            }
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(safeGridColumns),
                    state = gridState,
                    contentPadding = PaddingValues(bottom = 120.dp)
                ) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            text = "${type.categoryCountSummary(displayedItems.size)} · ${sortMode.displayLabel(type)}",
                            fontSize = 13.sp,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    items(displayedItems, key = { it.name }) { item ->
                        MetadataCategoryCard(
                            type = type,
                            item = item,
                            sortMode = sortMode,
                            albumArtUri = albumArtUrisByName[item.name],
                            representativeSong = representativeSongsByName[item.name],
                            loadCoverArt = if (type.prefersEmbeddedCategoryCardCover()) mainViewModel::getAlbumCoverArtBitmap else null,
                            onClick = { onCategoryClick(item.name) },
                            onLongClick = { categoryMenuItem = item }
                        )
                    }
                }
                if (displayedItems.size > 30) {
                    LazyGridScrollIndicator(
                        state = gridState,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight()
                    )
                }
            }
        }
    }

    categoryMenuItem?.let { item ->
        WindowBottomSheet(
            show = true,
            enableNestedScroll = false,
            title = item.name.substringAfterLast('/').ifBlank { item.name },
            onDismissRequest = { categoryMenuItem = null }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(MiuixTheme.colorScheme.background.copy(alpha = 0.98f))
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CategorySheetItem(stringResource(R.string.song_more_play_next)) {
                    val selectedSongs = mainViewModel.getSongsForMetadataCategory(type, item.name)
                    selectedSongs.asReversed().forEach(playerViewModel::playNext)
                    Toast.makeText(context, context.getString(R.string.song_more_added_to_queue), Toast.LENGTH_SHORT).show()
                    categoryMenuItem = null
                }
                CategorySheetItem(stringResource(R.string.common_add_desktop_shortcut)) {
                    val ok = requestPinnedEllaShortcut(
                        context = context,
                        id = "category_${type}_${item.name}",
                        label = item.name,
                        route = Screen.MetadataCategoryDetail.createRoute(type, item.name)
                    )
                    Toast.makeText(
                        context,
                        if (ok) context.getString(R.string.playlist_shortcut_requested, item.name) else context.getString(R.string.playlist_shortcut_unsupported),
                        Toast.LENGTH_SHORT
                    ).show()
                    categoryMenuItem = null
                }
                CategorySheetItem(stringResource(R.string.common_cancel)) {
                    categoryMenuItem = null
                }
            }
        }
    }
}

@Composable
fun MetadataCategoryDetailScreen(
    type: String,
    name: String,
    mainViewModel: MainViewModel,
    playerViewModel: PlayerViewModel,
    onBack: () -> Unit,
    onAlbumClick: (Long) -> Unit = {},
    onArtistClick: (String) -> Unit = {},
    onMetadataCategoryClick: (String, String) -> Unit = { _, _ -> },
    onNavigateToPlayer: () -> Unit
) {
    val context = LocalContext.current
    val librarySongs by mainViewModel.songs.collectAsState()
    val libraryAlbums by mainViewModel.albums.collectAsState()
    val playlists by mainViewModel.playlists.collectAsState()
    val currentSong by playerViewModel.currentSong.collectAsState()
    val favoriteSongKeys by playerViewModel.favoriteSongKeys.collectAsState()
    val locateCurrentSongRequest by playerViewModel.locateCurrentSongRequest.collectAsState()
    val openPlayerOnPlay by mainViewModel.settingsManager.openPlayerOnPlay.collectAsState(initial = false)
    val showPlayNextInLists by mainViewModel.settingsManager.showPlayNextInLists.collectAsState(initial = false)
    val songs = remember(type, name, librarySongs) { mainViewModel.getSongsForMetadataCategory(type, name) }
    var sortExpanded by remember { mutableStateOf(false) }
    val detailSongSortIndexFlow = remember(type) { mainViewModel.settingsManager.metadataCategoryDetailSongSortIndex(type) }
    val detailAlbumSortIndexFlow = remember(type) { mainViewModel.settingsManager.metadataCategoryDetailAlbumSortIndex(type) }
    val sortIndex by detailSongSortIndexFlow.collectAsState(initial = 0)
    val albumSortIndex by detailAlbumSortIndexFlow.collectAsState(initial = 0)
    val sortMode = MetadataDetailSongSortMode.entries.getOrElse(sortIndex) { MetadataDetailSongSortMode.AlbumTrack }
    val albumSortMode = MetadataDetailAlbumSortMode.entries.getOrElse(albumSortIndex) { MetadataDetailAlbumSortMode.YearAsc }
    var selectedTab by rememberSaveable(type, name) { mutableStateOf(MetadataDetailTab.Songs) }
    var actionSong by remember { mutableStateOf<com.ella.music.data.model.Song?>(null) }
    var selectionMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }
    var playlistPickerSongs by remember { mutableStateOf<List<Song>?>(null) }
    var createPlaylistSongs by remember { mutableStateOf<List<Song>?>(null) }
    var pendingDeleteSongs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var pendingSystemDeleteSongs by remember { mutableStateOf<List<Song>>(emptyList()) }
    val sortedSongs = remember(songs, sortMode) { songs.sortedForMetadataDetail(sortMode) }
    val showAlbumTab = type == "genre" || type == "year" || type == "composer" || type == "lyricist"
    val detailAlbums = remember(songs, libraryAlbums) {
        LibraryAlbumAggregator.toAlbumsForSongs(
            songs = songs,
            libraryAlbums = libraryAlbums,
            unknownAlbumName = context.getString(R.string.player_unknown_album)
        )
    }
    val albumDurations = remember(songs) {
        LibraryAlbumAggregator.durationsByAlbumIdentity(songs)
    }
    val sortedAlbums = remember(detailAlbums, albumSortMode, albumDurations) {
        detailAlbums.sortedForMetadataAlbumDetail(albumSortMode, albumDurations)
    }
    val albumArtUrisByAlbumId = remember(sortedAlbums) {
        sortedAlbums.associate { album -> album.id to mainViewModel.getAlbumArtUri(album.artAlbumId) }
    }
    val albumArtUrisBySongId = remember(sortedSongs) {
        sortedSongs.associate { song -> song.id to mainViewModel.getAlbumArtUri(song.albumId) }
    }
    val hasSameNameArtist = remember(type, name, librarySongs) {
        (type == "composer" || type == "lyricist") && mainViewModel.getSongsForArtist(name).isNotEmpty()
    }
    val hasSameNameComposer = remember(type, name, librarySongs) {
        type == "lyricist" && mainViewModel.getSongsForMetadataCategory("composer", name).isNotEmpty()
    }
    val hasSameNameLyricist = remember(type, name, librarySongs) {
        type == "composer" && mainViewModel.getSongsForMetadataCategory("lyricist", name).isNotEmpty()
    }
    val pageBackground = ellaPageBackground()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val deleteRequestLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val songsToDelete = pendingSystemDeleteSongs
        pendingSystemDeleteSongs = emptyList()
        if (result.resultCode == Activity.RESULT_OK && songsToDelete.isNotEmpty()) {
            mainViewModel.removeSongsFromLibrary(songsToDelete)
            Toast.makeText(context, context.getString(R.string.library_deleted_songs, songsToDelete.size), Toast.LENGTH_SHORT).show()
            selectedIds = emptySet()
            selectionMode = false
        } else if (songsToDelete.isNotEmpty()) {
            Toast.makeText(context, context.getString(R.string.library_delete_cancelled), Toast.LENGTH_SHORT).show()
        }
    }
    fun deleteSelectedSongs(songsToDelete: List<Song>) {
        if (songsToDelete.isEmpty()) return
        scope.launch {
            val result = mainViewModel.deleteSongsResult(songsToDelete)
            if (result.isSuccess) {
                Toast.makeText(context, context.getString(R.string.library_deleted_songs, songsToDelete.size), Toast.LENGTH_SHORT).show()
                selectedIds = emptySet()
                selectionMode = false
                return@launch
            }
            val error = result.exceptionOrNull()
            if (error is WritePermissionRequiredException) {
                pendingSystemDeleteSongs = songsToDelete
                deleteRequestLauncher.launch(IntentSenderRequest.Builder(error.intentSender).build())
            } else {
                Toast.makeText(context, error?.localizedMessage ?: context.getString(R.string.song_more_metadata_save_failed), Toast.LENGTH_SHORT).show()
            }
        }
    }
    val currentSongItemIndex = remember(sortedSongs, currentSong?.id, selectedTab, selectionMode) {
        if (selectedTab != MetadataDetailTab.Songs || selectionMode) return@remember -1
        sortedSongs.indexOfFirst { it.id == currentSong?.id }
            .takeIf { it >= 0 }
            ?.plus(if (showAlbumTab) 2 else 1)
            ?: -1
    }
    BackHandler(enabled = selectionMode || sortExpanded) {
        when {
            selectionMode -> {
                selectedIds = emptySet()
                selectionMode = false
            }
            sortExpanded -> sortExpanded = false
        }
    }
    LaunchedEffect(selectedTab) {
        if (selectedTab != MetadataDetailTab.Songs && selectionMode) {
            selectedIds = emptySet()
            selectionMode = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(pageBackground)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Box {
            EllaSmallTopAppBar(
                title = name.ifBlank { type.categoryTitle() },
                color = pageBackground,
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
                            val selectedSongs = sortedSongs.filter { it.id in selectedIds }
                            if (selectedSongs.isEmpty()) {
                                Toast.makeText(context, context.getString(R.string.library_select_songs_first), Toast.LENGTH_SHORT).show()
                            } else {
                                playlistPickerSongs = selectedSongs
                            }
                        }) {
                            Icon(
                                imageVector = MiuixIcons.Regular.Add,
                                contentDescription = stringResource(R.string.song_more_add_to_playlist),
                                tint = MiuixTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        IconButton(onClick = {
                            val selectedSongs = sortedSongs.filter { it.id in selectedIds }
                            if (selectedSongs.isEmpty()) {
                                Toast.makeText(context, context.getString(R.string.library_select_songs_first), Toast.LENGTH_SHORT).show()
                            } else {
                                pendingDeleteSongs = selectedSongs
                            }
                        }) {
                            Icon(
                                imageVector = MiuixIcons.Regular.Delete,
                                contentDescription = stringResource(R.string.common_delete),
                                tint = Color(0xFFE5484D),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    } else {
                        if (selectedTab == MetadataDetailTab.Songs) {
                            IconButton(onClick = {
                                selectionMode = true
                                selectedIds = sortedSongs.map { it.id }.toSet()
                            }) {
                                Icon(
                                    imageVector = MiuixIcons.Regular.SelectAll,
                                    contentDescription = stringResource(R.string.common_multi_select),
                                    tint = MiuixTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        IconButton(onClick = { sortExpanded = !sortExpanded }) {
                            Icon(
                                imageVector = MiuixIcons.Regular.Sort,
                                contentDescription = stringResource(R.string.common_sort),
                                tint = MiuixTheme.colorScheme.onSurface,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            )
            DoubleTapScrollOverlay(
                onDoubleTap = { scope.launch { listState.animateScrollToItem(0) } },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                endPadding = 128.dp
            )
        }

        AnimatedVisibility(
            visible = sortExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                if (selectedTab == MetadataDetailTab.Albums) {
                    MetadataDetailAlbumSortMode.entries.forEach { mode ->
                        Text(
                            text = mode.label(),
                            fontSize = 14.sp,
                            fontWeight = if (albumSortMode == mode) FontWeight.Bold else FontWeight.Normal,
                            color = if (albumSortMode == mode) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    sortExpanded = false
                                    scope.launch { mainViewModel.settingsManager.setMetadataCategoryDetailAlbumSortIndex(type, mode.ordinal) }
                                    scope.launch { listState.animateScrollToItem(0) }
                                }
                                .padding(vertical = 10.dp)
                        )
                    }
                } else {
                    MetadataDetailSongSortMode.entries.forEach { mode ->
                    Text(
                        text = mode.label(),
                        fontSize = 14.sp,
                        fontWeight = if (sortMode == mode) FontWeight.Bold else FontWeight.Normal,
                        color = if (sortMode == mode) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                sortExpanded = false
                                scope.launch { mainViewModel.settingsManager.setMetadataCategoryDetailSongSortIndex(type, mode.ordinal) }
                                scope.launch { listState.animateScrollToItem(0) }
                            }
                            .padding(vertical = 10.dp)
                    )
                    }
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        val summaryText = if (selectionMode) {
                            stringResource(R.string.category_selected_count, selectedIds.size)
                        } else if (selectedTab == MetadataDetailTab.Albums) {
                            stringResource(R.string.category_album_summary, sortedAlbums.size, type.categoryTitle(), albumSortMode.label())
                        } else {
                            stringResource(R.string.category_song_summary, sortedSongs.size, type.categoryTitle(), sortMode.label())
                        }
                        if (type == "composer" || type == "lyricist") {
                            Row(
                                modifier = Modifier.padding(bottom = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (hasSameNameArtist) {
                                    MetadataDetailLinkChip(
                                        text = stringResource(R.string.category_artist_page),
                                        onClick = { onArtistClick(name) }
                                    )
                                }
                                if (hasSameNameComposer) {
                                    MetadataDetailLinkChip(
                                        text = stringResource(R.string.category_composer_page),
                                        onClick = { onMetadataCategoryClick("composer", name) }
                                    )
                                }
                                if (hasSameNameLyricist) {
                                    MetadataDetailLinkChip(
                                        text = stringResource(R.string.category_lyricist_page),
                                        onClick = { onMetadataCategoryClick("lyricist", name) }
                                    )
                                }
                            }
                        }
                        if (showAlbumTab) {
                            Row(
                                modifier = Modifier.padding(bottom = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                MetadataDetailTab.entries.forEach { tab ->
                                    Text(
                                        text = tab.label(),
                                        fontSize = 13.sp,
                                        fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selectedTab == tab) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(999.dp))
                                            .background(
                                                if (selectedTab == tab) MiuixTheme.colorScheme.primary.copy(alpha = 0.12f)
                                                else Color.Transparent
                                            )
                                            .clickable { selectedTab = tab }
                                            .padding(horizontal = 12.dp, vertical = 7.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = summaryText,
                            fontSize = 13.sp,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                        )
                    }
                }
                if (selectedTab == MetadataDetailTab.Albums) {
                    items(sortedAlbums, key = { it.id }) { album ->
                        MetadataAlbumRow(
                            album = album,
                            duration = albumDurations[album.id] ?: 0L,
                            albumArtUri = albumArtUrisByAlbumId[album.id],
                            onClick = { onAlbumClick(album.id) }
                        )
                    }
                } else {
                    itemsIndexed(sortedSongs, key = { _, song -> song.id }) { index, song ->
                        val selected = song.id in selectedIds
                        SongItem(
                            song = song,
                            isCurrent = currentSong?.id == song.id,
                            albumArtUri = albumArtUrisBySongId[song.id],
                            loadCoverArt = mainViewModel::getCoverArtBitmap,
                            loadAudioInfo = mainViewModel::getAudioInfo,
                            showPlayNextInLists = showPlayNextInLists,
                            isFavorite = song.playlistIdentityKey() in favoriteSongKeys,
                            loadSongRating = mainViewModel::getSongRating,
                            selectionMode = selectionMode,
                            selected = selected,
                            onLongClick = {
                                selectionMode = true
                                selectedIds = selectedIds + song.id
                            },
                            onClick = {
                                if (selectionMode) {
                                    selectedIds = if (selected) selectedIds - song.id else selectedIds + song.id
                                } else {
                                    playerViewModel.setPlaylist(sortedSongs, index)
                                    if (openPlayerOnPlay) onNavigateToPlayer()
                                }
                            },
                            onAddToQueue = { playerViewModel.addToPlaylist(song) },
                            onMore = { actionSong = song }
                        )
                    }
                }
                item { Spacer(modifier = Modifier.height(24.dp)) }
            }

            LocateCurrentSongFloatingButton(
                listState = listState,
                currentItemIndex = currentSongItemIndex,
                locateRequest = locateCurrentSongRequest,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 22.dp, bottom = 118.dp)
            )

            SongMoreActionHost(
                actionSong = actionSong,
                mainViewModel = mainViewModel,
                playerViewModel = playerViewModel,
                onDismissAction = { actionSong = null },
                onNavigateToAlbum = onAlbumClick,
                onNavigateToArtist = onArtistClick
            )

            playlistPickerSongs?.let { songsToAdd ->
                WindowBottomSheet(
                    show = true,
                    enableNestedScroll = false,
                    title = stringResource(R.string.song_more_add_to_playlist),
                    onDismissRequest = { playlistPickerSongs = null }
                ) {
                    AddToPlaylistSheet(
                        playlists = playlists
                            .sortedWith(compareByDescending<com.ella.music.data.model.UserPlaylist> { it.id == FAVORITES_PLAYLIST_ID }.thenByDescending { it.createdAt }),
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
                            Toast.makeText(context, context.getString(R.string.player_added_to_playlists, selectedPlaylists.size), Toast.LENGTH_SHORT).show()
                            playlistPickerSongs = null
                            selectedIds = emptySet()
                            selectionMode = false
                        }
                    )
                }
            }

            createPlaylistSongs?.let { songsToAdd ->
                CategoryCreatePlaylistAndAddSelectedSheet(
                    songCount = songsToAdd.size,
                    onDismiss = { createPlaylistSongs = null },
                    onCreate = { playlistName ->
                        mainViewModel.createPlaylist(playlistName) { playlist ->
                            if (playlist != null) {
                                mainViewModel.addSongsToPlaylist(playlist.id, songsToAdd)
                                Toast.makeText(context, context.getString(R.string.player_added_to_playlist_named, playlist.name), Toast.LENGTH_SHORT).show()
                                selectedIds = emptySet()
                                selectionMode = false
                            }
                        }
                        createPlaylistSongs = null
                    }
                )
            }

            ConfirmDangerDialog(
                show = pendingDeleteSongs.isNotEmpty(),
                title = stringResource(R.string.song_more_delete_song_title),
                message = stringResource(R.string.library_delete_selected_message, pendingDeleteSongs.size),
                confirmText = stringResource(R.string.song_more_delete_permanently),
                onDismiss = { pendingDeleteSongs = emptyList() },
                onConfirm = {
                    val songsToDelete = pendingDeleteSongs
                    pendingDeleteSongs = emptyList()
                    deleteSelectedSongs(songsToDelete)
                }
            )
        }
    }
}

@Composable
private fun CategoryAddSelectedSongsToPlaylistSheet(
    playlists: List<UserPlaylist>,
    songCount: Int,
    onDismiss: () -> Unit,
    onCreatePlaylist: () -> Unit,
    onPlaylistsConfirm: (List<UserPlaylist>) -> Unit
) {
    var selectedPlaylistIds by remember(playlists) { mutableStateOf(emptySet<String>()) }
    val selectedPlaylists = playlists.filter { it.id in selectedPlaylistIds }
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .padding(bottom = 18.dp)
            .heightIn(max = 400.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = stringResource(R.string.category_selected_songs, songCount),
            fontSize = 13.sp,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
        )
        CategorySheetItem(stringResource(R.string.song_more_create_playlist), onCreatePlaylist)
        if (playlists.isEmpty()) {
            Text(
                text = stringResource(R.string.song_more_no_custom_playlists),
                fontSize = 14.sp,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 18.dp)
            )
        } else {
            playlists.forEach { playlist ->
                val selected = playlist.id in selectedPlaylistIds
                CategorySheetItem("${if (selected) "✓ " else ""}${playlist.name} · ${playlist.songs.size} ${stringResource(R.string.library_search_song_unit)}") {
                    selectedPlaylistIds = if (selected) {
                        selectedPlaylistIds - playlist.id
                    } else {
                        selectedPlaylistIds + playlist.id
                    }
                }
            }
        }
        if (playlists.isNotEmpty()) {
            CategorySheetItem(stringResource(R.string.song_more_done_selected, selectedPlaylistIds.size)) {
                if (selectedPlaylists.isNotEmpty()) {
                    onPlaylistsConfirm(selectedPlaylists)
                }
            }
        }
        CategorySheetItem(stringResource(R.string.common_cancel), onDismiss)
    }
}

@Composable
private fun CategoryCreatePlaylistAndAddSelectedSheet(
    songCount: Int,
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    var playlistName by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) {
        delay(220L)
        focusRequester.requestFocus()
        keyboardController?.show()
    }
    EllaMiuixBottomSheet(
        show = true,
        title = stringResource(R.string.playlist_create_title),
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier.padding(bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = stringResource(R.string.library_create_playlist_add_count, songCount),
                fontSize = 13.sp,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
            )
            EllaMiuixTextField(
                value = playlistName,
                onValueChange = { playlistName = it },
                label = stringResource(R.string.playlist_name_label),
                focusRequester = focusRequester
            )
            EllaMiuixSheetActions(
                cancelText = stringResource(R.string.common_cancel),
                confirmText = stringResource(R.string.common_create),
                onCancel = onDismiss,
                onConfirm = { onCreate(playlistName) }
            )
        }
    }
}

@Composable
private fun MetadataDetailLinkChip(
    text: String,
    onClick: () -> Unit
) {
    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = MiuixTheme.colorScheme.primary,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(MiuixTheme.colorScheme.primary.copy(alpha = 0.12f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    )
}

@Composable
private fun CategorySheetItem(
    text: String,
    onClick: () -> Unit
) {
    Text(
        text = text,
        fontSize = 14.sp,
        color = MiuixTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 12.dp)
    )
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun MetadataCategoryCard(
    type: String,
    item: MetadataCategoryItem,
    sortMode: MetadataCategorySortMode,
    albumArtUri: android.net.Uri?,
    representativeSong: Song? = null,
    loadCoverArt: ((Song) -> Bitmap?)? = null,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val coverState = rememberSongArtworkState(
        song = representativeSong,
        albumArtUri = albumArtUri,
        loadCoverArt = loadCoverArt,
        usage = ArtworkUsage.ArtistImage,
        showDefaultWhenMissing = false
    )
    val coverModel: Any? = coverState.model

    when (type) {
        "folder" -> {
            FolderCategoryRow(
                item = item,
                sortMode = sortMode,
                coverModel = coverModel,
                onClick = onClick,
                onLongClick = onLongClick
            )
            return
        }
        "composer", "lyricist" -> {
            PersonCategoryRow(
                item = item,
                sortMode = sortMode,
                coverModel = coverModel,
                onClick = onClick,
                onLongClick = onLongClick
            )
            return
        }
    }

    val cardColor = remember(item.name) { item.name.categoryCardColor() }
    val hasCover = coverModel != null
    val isGenreCard = type == "genre"
    val useSmallCover = type == "genre" || type == "year"
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (isGenreCard) 112.dp else 116.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        cardColor,
                        cardColor.darkenCategoryColor(0.78f)
                    )
                )
            )
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(Color.Black.copy(alpha = 0.08f))
        )
        if (coverModel != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 2.dp)
                    .size(if (useSmallCover) 54.dp else 78.dp)
                    .graphicsLayer {
                        rotationZ = 13f
                        translationX = if (useSmallCover) 9.dp.toPx() else 16.dp.toPx()
                        translationY = if (useSmallCover) 3.dp.toPx() else 6.dp.toPx()
                    }
                    .clip(RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                SafeCoverImage(
                    model = coverModel,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    sizePx = if (useSmallCover) 128 else 220,
                    showDefaultPlaceholder = false
                )
            }
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color.Transparent, cardColor.copy(alpha = 0.16f), Color.Black.copy(alpha = 0.16f))
                        )
                    )
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = 14.dp,
                    top = if (isGenreCard) 12.dp else 13.dp,
                    end = if (hasCover) {
                        if (useSmallCover) 54.dp else 72.dp
                    } else {
                        14.dp
                    },
                    bottom = 12.dp
                ),
            verticalArrangement = if (isGenreCard) Arrangement.Center else Arrangement.SpaceBetween
        ) {
            Text(
                text = item.name,
                fontSize = if (isGenreCard) 13.sp else 16.sp,
                lineHeight = if (isGenreCard) 17.sp else 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = item.categorySortSummary(sortMode),
                fontSize = if (isGenreCard) 10.sp else 12.sp,
                lineHeight = if (isGenreCard) 13.sp else 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.78f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun FolderCategoryRow(
    item: MetadataCategoryItem,
    sortMode: MetadataCategorySortMode,
    coverModel: Any?,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MiuixTheme.colorScheme.surfaceContainer),
            contentAlignment = Alignment.Center
        ) {
            if (coverModel != null) {
                SafeCoverImage(
                    model = coverModel,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    sizePx = 160,
                    showDefaultPlaceholder = false
                )
            } else {
                FolderOutlineIcon(
                    tint = MiuixTheme.colorScheme.primary,
                    modifier = Modifier.size(34.dp)
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name.substringAfterLast('/').ifBlank { item.name.ifBlank { stringResource(R.string.folder_root) } },
                fontSize = 17.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = MiuixTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${item.folderSortSummary(sortMode)} · ${item.name}",
                fontSize = 13.sp,
                lineHeight = 17.sp,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun PersonCategoryRow(
    item: MetadataCategoryItem,
    sortMode: MetadataCategorySortMode,
    coverModel: Any?,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(76.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
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
                text = item.name.ifBlank { stringResource(R.string.common_unknown) },
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MiuixTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = item.personSortSummary(sortMode),
                fontSize = 12.sp,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun String.categoryCardColor(): Color {
    val palette = listOf(
        Color(0xFF141414),
        Color(0xFF825A58),
        Color(0xFFA92E4A),
        Color(0xFF626262),
        Color(0xFF352B28),
        Color(0xFF416B8D),
        Color(0xFF28295F),
        Color(0xFF9B463D),
        Color(0xFF6C4E86),
        Color(0xFF2A1024),
        Color(0xFFA88E24),
        Color(0xFF542231),
        Color(0xFF5EA91A)
    )
    val index = (lowercase(Locale.ROOT).hashCode() and Int.MAX_VALUE) % palette.size
    return palette[index]
}

private fun String.prefersEmbeddedCategoryCardCover(): Boolean =
    this == "folder" || this == "composer" || this == "lyricist"

private fun Color.darkenCategoryColor(factor: Float): Color {
    return Color(
        red = red * factor,
        green = green * factor,
        blue = blue * factor,
        alpha = alpha
    )
}

private enum class MetadataCategorySortMode {
    Name,
    NameDesc,
    SongCount,
    AlbumCount,
    Duration,
    DateModified,
    DateModifiedAsc
}

private fun MetadataCategorySortMode.availableFor(type: String): Boolean {
    return when (this) {
        MetadataCategorySortMode.NameDesc -> type == "year"
        MetadataCategorySortMode.DateModified,
        MetadataCategorySortMode.DateModifiedAsc -> type == "folder"
        else -> true
    }
}

@Composable
private fun MetadataCategorySortMode.displayLabel(type: String): String {
    val context = LocalContext.current
    return when {
        type == "year" && this == MetadataCategorySortMode.Name -> context.getString(R.string.category_sort_year_asc)
        (type == "composer" || type == "lyricist") && this == MetadataCategorySortMode.AlbumCount -> context.getString(R.string.category_sort_participating_albums)
        else -> context.getString(when (this) {
            MetadataCategorySortMode.Name -> R.string.category_sort_name
            MetadataCategorySortMode.NameDesc -> R.string.category_sort_year_desc
            MetadataCategorySortMode.SongCount -> R.string.playlist_sort_song_count
            MetadataCategorySortMode.AlbumCount -> R.string.category_sort_album_count
            MetadataCategorySortMode.Duration -> R.string.playlist_sort_duration
            MetadataCategorySortMode.DateModified -> R.string.playlist_song_sort_date_modified
            MetadataCategorySortMode.DateModifiedAsc -> R.string.category_sort_date_modified_asc
        })
    }
}

private fun List<MetadataCategoryItem>.sortedForCategory(mode: MetadataCategorySortMode): List<MetadataCategoryItem> {
    return when (mode) {
        MetadataCategorySortMode.Name -> sortedBy { it.name.lowercase(Locale.ROOT) }
        MetadataCategorySortMode.NameDesc -> sortedByDescending { it.name.toIntOrNull() ?: Int.MIN_VALUE }
        MetadataCategorySortMode.SongCount -> sortedByDescending { it.songCount }
        MetadataCategorySortMode.AlbumCount -> sortedByDescending { it.albumCount }
        MetadataCategorySortMode.Duration -> sortedByDescending { it.duration }
        MetadataCategorySortMode.DateModified -> sortedByDescending { it.dateModified }
        MetadataCategorySortMode.DateModifiedAsc -> sortedBy { it.dateModified }
    }
}

private fun MetadataCategoryItem.matchesCategorySearch(query: String, type: String): Boolean {
    return name.contains(query, ignoreCase = true) ||
        (type == "folder" && name.substringAfterLast('/').contains(query, ignoreCase = true))
}

@Composable
private fun MetadataCategoryItem.categorySortSummary(sortMode: MetadataCategorySortMode): String {
    val context = LocalContext.current
    return when (sortMode) {
        MetadataCategorySortMode.AlbumCount -> context.getString(R.string.category_album_count_detail, albumCount)
        MetadataCategorySortMode.Duration -> duration.formatDuration()
        else -> context.getString(R.string.category_song_count_card, songCount)
    }
}

@Composable
private fun MetadataCategoryItem.folderSortSummary(sortMode: MetadataCategorySortMode): String {
    val context = LocalContext.current
    return when (sortMode) {
        MetadataCategorySortMode.AlbumCount -> context.getString(R.string.category_album_count_detail, albumCount)
        MetadataCategorySortMode.Duration -> duration.formatDuration()
        MetadataCategorySortMode.DateModified,
        MetadataCategorySortMode.DateModifiedAsc -> dateModified.formatDateTimeText(context)
        else -> context.getString(R.string.analytics_song_count_value, songCount)
    }
}

@Composable
private fun MetadataCategoryItem.personSortSummary(sortMode: MetadataCategorySortMode): String {
    val context = LocalContext.current
    return when (sortMode) {
        MetadataCategorySortMode.Duration -> context.getString(R.string.category_person_sort_duration, duration.formatDuration(), albumCount)
        else -> context.getString(R.string.category_person_sort, songCount, albumCount)
    }
}

private enum class MetadataDetailSongSortMode {
    AlbumTrack,
    Title,
    FileName,
    Duration,
    YearAsc,
    YearDesc,
    DateAdded,
    DateAddedAsc,
    DateModified,
    DateModifiedAsc
}

@Composable
private fun MetadataDetailSongSortMode.label(): String {
    val context = LocalContext.current
    return context.getString(when (this) {
        MetadataDetailSongSortMode.AlbumTrack -> R.string.category_sort_album_track
        MetadataDetailSongSortMode.Title -> R.string.playlist_song_sort_title
        MetadataDetailSongSortMode.FileName -> R.string.playlist_song_sort_file_name
        MetadataDetailSongSortMode.Duration -> R.string.playlist_sort_duration
        MetadataDetailSongSortMode.YearAsc -> R.string.playlist_song_sort_year_asc
        MetadataDetailSongSortMode.YearDesc -> R.string.playlist_song_sort_year_desc
        MetadataDetailSongSortMode.DateAdded -> R.string.playlist_song_sort_date_added
        MetadataDetailSongSortMode.DateAddedAsc -> R.string.category_sort_date_added_asc
        MetadataDetailSongSortMode.DateModified -> R.string.playlist_song_sort_date_modified
        MetadataDetailSongSortMode.DateModifiedAsc -> R.string.category_sort_date_modified_asc
    })
}

private fun List<com.ella.music.data.model.Song>.sortedForMetadataDetail(
    mode: MetadataDetailSongSortMode
): List<com.ella.music.data.model.Song> {
    return when (mode) {
        MetadataDetailSongSortMode.AlbumTrack -> sortedWith(
            compareBy<com.ella.music.data.model.Song> { it.album.lowercase(Locale.ROOT) }
                .thenBy { if (it.discNumber > 0) it.discNumber else Int.MAX_VALUE }
                .thenBy { if (it.trackNumber > 0) it.trackNumber else Int.MAX_VALUE }
                .thenBy { it.title.lowercase(Locale.ROOT) }
        )
        MetadataDetailSongSortMode.Title -> sortedBy { it.title.lowercase(Locale.ROOT) }
        MetadataDetailSongSortMode.FileName -> sortedBy { song ->
            song.fileName.ifBlank { song.path.substringAfterLast('/') }.lowercase(Locale.ROOT)
        }
        MetadataDetailSongSortMode.Duration -> sortedByDescending { it.duration }
        MetadataDetailSongSortMode.YearAsc -> sortedByReleaseDate(ascending = true)
        MetadataDetailSongSortMode.YearDesc -> sortedByReleaseDate(ascending = false)
        MetadataDetailSongSortMode.DateAdded -> sortedByDescending { it.dateAdded }
        MetadataDetailSongSortMode.DateAddedAsc -> sortedBy { it.dateAdded }
        MetadataDetailSongSortMode.DateModified -> sortedByDescending { it.dateModified }
        MetadataDetailSongSortMode.DateModifiedAsc -> sortedBy { it.dateModified }
    }
}

private enum class MetadataDetailTab {
    Songs,
    Albums
}

@Composable
private fun MetadataDetailTab.label(): String {
    val context = LocalContext.current
    return context.getString(when (this) {
        MetadataDetailTab.Songs -> R.string.album_stat_songs
        MetadataDetailTab.Albums -> R.string.category_album
    })
}

private enum class MetadataDetailAlbumSortMode {
    YearAsc,
    YearDesc,
    SongCount,
    Duration,
    Name
}

@Composable
private fun MetadataDetailAlbumSortMode.label(): String {
    val context = LocalContext.current
    return context.getString(when (this) {
        MetadataDetailAlbumSortMode.YearAsc -> R.string.playlist_song_sort_year_asc
        MetadataDetailAlbumSortMode.YearDesc -> R.string.playlist_song_sort_year_desc
        MetadataDetailAlbumSortMode.SongCount -> R.string.playlist_sort_song_count
        MetadataDetailAlbumSortMode.Duration -> R.string.playlist_sort_duration
        MetadataDetailAlbumSortMode.Name -> R.string.category_sort_album_name
    })
}

private fun List<Album>.sortedForMetadataAlbumDetail(
    mode: MetadataDetailAlbumSortMode,
    durations: Map<Long, Long>
): List<Album> {
    return when (mode) {
        MetadataDetailAlbumSortMode.YearAsc -> sortedWith(compareBy<Album> { it.yearInt <= 0 }.thenBy { it.yearInt }.thenBy { it.name.lowercase(Locale.ROOT) })
        MetadataDetailAlbumSortMode.YearDesc -> sortedWith(compareBy<Album> { it.yearInt <= 0 }.thenByDescending { it.yearInt }.thenBy { it.name.lowercase(Locale.ROOT) })
        MetadataDetailAlbumSortMode.SongCount -> sortedByDescending { it.songCount }
        MetadataDetailAlbumSortMode.Duration -> sortedByDescending { durations[it.id] ?: 0L }
        MetadataDetailAlbumSortMode.Name -> sortedBy { it.name.lowercase(Locale.ROOT) }
    }
}

@Composable
private fun MetadataAlbumRow(
    album: Album,
    duration: Long,
    albumArtUri: android.net.Uri?,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val summary = buildList {
        add(context.getString(R.string.analytics_song_count_value, album.songCount))
        add(duration.formatDuration())
        if (album.year.isNotBlank()) add(album.year)
    }.joinToString(" · ")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MiuixTheme.colorScheme.surfaceContainer),
            contentAlignment = Alignment.Center
        ) {
            if (albumArtUri != null) {
                SafeCoverImage(
                    model = albumArtUri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    sizePx = 128,
                    showDefaultPlaceholder = false
                )
            } else {
                DefaultAlbumCover(modifier = Modifier.fillMaxSize())
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = album.name,
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
            .thenBy { it.album.lowercase(Locale.ROOT) }
            .thenBy { if (it.discNumber > 0) it.discNumber else Int.MAX_VALUE }
            .thenBy { if (it.trackNumber > 0) it.trackNumber else Int.MAX_VALUE }
            .thenBy { it.title.lowercase(Locale.ROOT) }
    )
}

private fun Song.releaseYearOrNull(): Int? =
    Regex("""\d{4}""").find(year)?.value?.toIntOrNull()

@Composable
private fun String.categoryTitle(): String {
    val context = LocalContext.current
    return when (this) {
        "genre" -> context.getString(R.string.category_genre)
        "year" -> context.getString(R.string.category_year)
        "composer" -> context.getString(R.string.category_composer)
        "lyricist" -> context.getString(R.string.category_lyricist)
        "folder" -> context.getString(R.string.category_folder)
        else -> context.getString(R.string.category_general)
    }
}

@Composable
private fun String.categoryCountSummary(count: Int): String {
    val context = LocalContext.current
    return when (this) {
        "genre" -> context.getString(R.string.category_count_genres, count)
        "composer" -> context.getString(R.string.category_count_composers, count)
        "lyricist" -> context.getString(R.string.category_count_lyricists, count)
        "folder" -> context.getString(R.string.category_count_folders, count)
        "year" -> context.getString(R.string.category_count_years, count)
        else -> context.getString(R.string.category_count_general, count)
    }
}

private fun String.usesSingleColumnCategory(): Boolean {
    return this == "composer" || this == "lyricist" || this == "folder"
}

private fun Long.formatDuration(): String {
    return formatPlaybackDuration()
}

private fun Long.formatDateText(context: android.content.Context): String {
    if (this <= 0L) return context.getString(R.string.folder_unknown_modified_time)
    val millis = if (this < 10_000_000_000L) this * 1000L else this
    return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(millis))
}

private fun Long.formatDateTimeText(context: android.content.Context): String {
    if (this <= 0L) return context.getString(R.string.folder_unknown_modified_time)
    val millis = if (this < 10_000_000_000L) this * 1000L else this
    return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(millis))
}
