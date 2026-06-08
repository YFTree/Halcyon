package com.ella.music.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue

object LibrarySortUiState {
    var librarySongSortIndex by mutableIntStateOf(0)
    var albumListSortIndex by mutableIntStateOf(0)
    var albumListFirstVisibleItemIndex by mutableIntStateOf(0)
    var albumListFirstVisibleItemScrollOffset by mutableIntStateOf(0)
    var artistListSortIndex by mutableIntStateOf(0)
    var artistListFirstVisibleItemIndex by mutableIntStateOf(0)
    var artistListFirstVisibleItemScrollOffset by mutableIntStateOf(0)
    var albumDetailSongSortIndex by mutableIntStateOf(0)
    var artistDetailSongSortIndex by mutableIntStateOf(0)
    var folderListSortIndex by mutableIntStateOf(0)
    var folderListFirstVisibleItemIndex by mutableIntStateOf(0)
    var folderListFirstVisibleItemScrollOffset by mutableIntStateOf(0)
    var folderDetailSongSortIndex by mutableIntStateOf(0)
    var playlistListSortIndex by mutableIntStateOf(2)

    val metadataCategoryScrollPositions = mutableMapOf<String, Pair<Int, Int>>()
}
