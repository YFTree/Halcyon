package com.ella.music.ui.player

import android.graphics.Bitmap
import android.graphics.Typeface
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.res.stringResource
import com.ella.music.R
import com.ella.music.data.exception.WritePermissionRequiredException
import com.ella.music.data.model.FAVORITES_PLAYLIST_ID
import com.ella.music.data.model.LyricLine
import com.ella.music.data.model.Song
import com.ella.music.data.model.UserPlaylist
import com.ella.music.ui.components.AddToPlaylistSheet
import com.ella.music.ui.components.ArtistPickerSheet
import com.ella.music.ui.components.ConfirmDangerDialog
import com.ella.music.ui.components.CreatePlaylistAndAddSheet
import com.ella.music.ui.components.LyricSharePicker
import com.ella.music.ui.components.RatingSheet
import com.ella.music.ui.components.SongAiInterpretationSheet
import com.ella.music.ui.components.SongInfoSheet
import com.ella.music.viewmodel.MainViewModel
import com.ella.music.viewmodel.PlayerViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.window.WindowBottomSheet

@Composable
internal fun PlayerScreenSheetHost(
    context: android.content.Context,
    scope: CoroutineScope,
    mainViewModel: MainViewModel,
    playerViewModel: PlayerViewModel,
    song: Song?,
    playlists: List<UserPlaylist>,
    artistChoices: List<String>,
    onArtistChoicesChange: (List<String>) -> Unit,
    onNavigateToArtist: (String) -> Unit,
    songInfoExpanded: Boolean,
    onSongInfoExpandedChange: (Boolean) -> Unit,
    dynamicCoverSheetSong: Song?,
    onDynamicCoverSheetSongChange: (Song?) -> Unit,
    ratingSheetSong: Song?,
    onRatingSheetSongChange: (Song?) -> Unit,
    aiSheetSong: Song?,
    onAiSheetSongChange: (Song?) -> Unit,
    deleteConfirmSong: Song?,
    onDeleteConfirmSongChange: (Song?) -> Unit,
    onWritePermissionRequired: (WritePermissionRequiredException, suspend () -> Unit) -> Unit,
    playlistPickerSong: Song?,
    onPlaylistPickerSongChange: (Song?) -> Unit,
    playlistPickerSongs: List<Song>?,
    onPlaylistPickerSongsChange: (List<Song>?) -> Unit,
    createPlaylistSong: Song?,
    onCreatePlaylistSongChange: (Song?) -> Unit,
    createPlaylistSongs: List<Song>?,
    onCreatePlaylistSongsChange: (List<Song>?) -> Unit
) {
    if (artistChoices.isNotEmpty()) {
        Popup(
            alignment = Alignment.BottomCenter,
            onDismissRequest = { onArtistChoicesChange(emptyList()) },
            properties = PopupProperties(focusable = true, dismissOnBackPress = true, dismissOnClickOutside = true)
        ) {
            ArtistPickerSheet(
                artists = artistChoices,
                onArtistSelected = { artist ->
                    onArtistChoicesChange(emptyList())
                    onNavigateToArtist(artist)
                },
                onDismiss = { onArtistChoicesChange(emptyList()) }
            )
        }
    }

    if (songInfoExpanded && song != null) {
        WindowBottomSheet(
            show = true,
            enableNestedScroll = false,
            title = stringResource(R.string.player_song_info),
            onDismissRequest = { onSongInfoExpandedChange(false) }
        ) {
            SongInfoSheet(
                song = song,
                audioInfoLoader = playerViewModel::getAudioInfo,
                tagInfoLoader = playerViewModel::getSongTagInfo,
                onDismiss = { onSongInfoExpandedChange(false) }
            )
        }
    }

    if (dynamicCoverSheetSong != null) {
        DynamicCoverWebViewSheet(
            show = true,
            song = dynamicCoverSheetSong,
            onDismissRequest = { onDynamicCoverSheetSongChange(null) }
        )
    }

    ratingSheetSong?.let { currentSong ->
        WindowBottomSheet(
            show = true,
            enableNestedScroll = false,
            title = stringResource(R.string.song_more_rating_title),
            onDismissRequest = { onRatingSheetSongChange(null) }
        ) {
            RatingSheet(
                currentRating = mainViewModel.getSongRating(currentSong),
                onDismiss = { onRatingSheetSongChange(null) },
                onRatingSelected = { rating ->
                    scope.launch {
                        val result = mainViewModel.writeSongRating(currentSong, rating)
                        if (result.isSuccess) {
                            Toast.makeText(context, context.getString(R.string.song_more_rating_saved), Toast.LENGTH_SHORT).show()
                            onRatingSheetSongChange(null)
                        } else {
                            val error = result.exceptionOrNull()
                            if (error is WritePermissionRequiredException) {
                                onWritePermissionRequired(error) {
                                    val retryResult = mainViewModel.writeSongRating(currentSong, rating)
                                    if (retryResult.isSuccess) {
                                        Toast.makeText(context, context.getString(R.string.song_more_rating_saved), Toast.LENGTH_SHORT).show()
                                        onRatingSheetSongChange(null)
                                    } else {
                                        Toast.makeText(context, retryResult.exceptionOrNull()?.localizedMessage ?: context.getString(R.string.song_more_rating_failed), Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } else {
                                Toast.makeText(context, error?.localizedMessage ?: context.getString(R.string.song_more_rating_failed), Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            )
        }
    }

    aiSheetSong?.let { currentSong ->
        WindowBottomSheet(
            show = true,
            enableNestedScroll = false,
            title = stringResource(R.string.song_more_ai_title),
            onDismissRequest = { onAiSheetSongChange(null) }
        ) {
            SongAiInterpretationSheet(
                song = currentSong,
                mainViewModel = mainViewModel,
                onDismiss = { onAiSheetSongChange(null) }
            )
        }
    }

    ConfirmDangerDialog(
        show = deleteConfirmSong != null,
        title = stringResource(R.string.song_more_delete_song_title),
        message = deleteConfirmSong?.let {
            context.getString(
                R.string.song_more_delete_song_message,
                it.title.ifBlank { it.fileName.ifBlank { context.getString(R.string.common_this_song) } }
            )
        }.orEmpty(),
        confirmText = stringResource(R.string.song_more_delete_permanently),
        onDismiss = { onDeleteConfirmSongChange(null) },
        onConfirm = {
            val currentSong = deleteConfirmSong ?: return@ConfirmDangerDialog
            onDeleteConfirmSongChange(null)
            scope.launch {
                val result = mainViewModel.deleteSongsResult(listOf(currentSong))
                if (result.isSuccess) {
                    Toast.makeText(context, context.getString(R.string.library_deleted_songs, 1), Toast.LENGTH_SHORT).show()
                } else {
                    val error = result.exceptionOrNull()
                    if (error is WritePermissionRequiredException) {
                        onWritePermissionRequired(error) {
                            mainViewModel.removeSongsFromLibrary(listOf(currentSong))
                            Toast.makeText(context, context.getString(R.string.library_deleted_songs, 1), Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, error?.localizedMessage ?: context.getString(R.string.song_more_metadata_save_failed), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    )

    playlistPickerSong?.let { currentSong ->
        WindowBottomSheet(
            show = true,
            enableNestedScroll = false,
            title = stringResource(R.string.player_add_to_playlist),
            onDismissRequest = { onPlaylistPickerSongChange(null) }
        ) {
            AddToPlaylistSheet(
                playlists = playlists.sortedForPlayerSheet(),
                onDismiss = { onPlaylistPickerSongChange(null) },
                onCreatePlaylist = {
                    onCreatePlaylistSongChange(currentSong)
                    onPlaylistPickerSongChange(null)
                },
                onPlaylistsConfirm = { selectedPlaylists, appendToEnd ->
                    selectedPlaylists.forEach { playlist ->
                        mainViewModel.addSongsToPlaylist(playlist.id, listOf(currentSong), appendToEnd)
                    }
                    Toast.makeText(
                        context,
                        context.getString(R.string.player_added_to_playlists, selectedPlaylists.size),
                        Toast.LENGTH_SHORT
                    ).show()
                    onPlaylistPickerSongChange(null)
                }
            )
        }
    }

    playlistPickerSongs?.let { songsToAdd ->
        WindowBottomSheet(
            show = true,
            enableNestedScroll = false,
            title = stringResource(R.string.player_add_to_playlist),
            onDismissRequest = { onPlaylistPickerSongsChange(null) }
        ) {
            AddToPlaylistSheet(
                playlists = playlists.sortedForPlayerSheet(),
                onDismiss = { onPlaylistPickerSongsChange(null) },
                onCreatePlaylist = {
                    onCreatePlaylistSongsChange(songsToAdd)
                    onPlaylistPickerSongsChange(null)
                },
                onPlaylistsConfirm = { selectedPlaylists, appendToEnd ->
                    selectedPlaylists.forEach { playlist ->
                        mainViewModel.addSongsToPlaylist(playlist.id, songsToAdd, appendToEnd)
                    }
                    Toast.makeText(
                        context,
                        context.getString(R.string.player_added_to_playlists, selectedPlaylists.size),
                        Toast.LENGTH_SHORT
                    ).show()
                    onPlaylistPickerSongsChange(null)
                }
            )
        }
    }

    createPlaylistSong?.let { currentSong ->
        CreatePlaylistAndAddSheet(
            onDismiss = { onCreatePlaylistSongChange(null) },
            onCreate = { name ->
                mainViewModel.createPlaylist(name) { playlist ->
                    if (playlist != null) {
                        mainViewModel.addSongsToPlaylist(playlist.id, listOf(currentSong))
                        Toast.makeText(
                            context,
                            context.getString(R.string.player_added_to_playlist_named, playlist.name),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                onCreatePlaylistSongChange(null)
            }
        )
    }

    createPlaylistSongs?.let { songsToAdd ->
        CreatePlaylistAndAddSheet(
            onDismiss = { onCreatePlaylistSongsChange(null) },
            onCreate = { name ->
                mainViewModel.createPlaylist(name) { playlist ->
                    if (playlist != null) {
                        mainViewModel.addSongsToPlaylist(playlist.id, songsToAdd)
                        Toast.makeText(
                            context,
                            context.getString(R.string.player_added_to_playlist_named, playlist.name),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                onCreatePlaylistSongsChange(null)
            }
        )
    }
}

@Composable
internal fun PlayerLyricShareHost(
    song: Song?,
    lyrics: List<LyricLine>,
    initialLine: LyricLine?,
    embeddedCover: Bitmap?,
    paletteBitmap: Bitmap?,
    palette: PlayerPalette,
    annotation: String,
    customInfo: String,
    shareTypeface: Typeface?,
    onDismiss: () -> Unit,
    onShare: (List<LyricLine>, Boolean) -> Unit
) {
    initialLine?.let { line ->
        LyricSharePicker(
            song = song,
            lyrics = lyrics,
            initialLine = line,
            cover = embeddedCover ?: paletteBitmap,
            backgroundColors = listOf(palette.top, palette.middle, palette.bottom),
            annotation = annotation,
            customInfo = customInfo,
            shareTypeface = shareTypeface,
            onDismiss = onDismiss,
            onShare = onShare
        )
    }
}

private fun List<UserPlaylist>.sortedForPlayerSheet(): List<UserPlaylist> =
    sortedWith(
        compareByDescending<UserPlaylist> { it.id == FAVORITES_PLAYLIST_ID }
            .thenByDescending { it.createdAt }
    )
