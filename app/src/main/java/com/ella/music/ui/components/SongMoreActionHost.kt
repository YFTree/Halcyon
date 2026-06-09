package com.ella.music.ui.components

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ella.music.R
import com.ella.music.data.decodeNeteaseKey
import com.ella.music.data.exception.WritePermissionRequiredException
import com.ella.music.data.metadata.AudioTagInfo
import com.lonx.audiotag.model.AudioTagKeys
import com.ella.music.data.detailedAudioInfo
import com.ella.music.data.model.AudioInfo
import com.ella.music.data.model.FAVORITES_PLAYLIST_ID
import com.ella.music.data.model.Song
import com.ella.music.data.model.SongTagInfo
import com.ella.music.data.model.UserPlaylist
import com.ella.music.data.model.albumIdentityId
import com.ella.music.data.neteaseAlbumUrl
import com.ella.music.data.neteaseArtistUrl
import com.ella.music.data.neteaseSongUrl
import com.ella.music.data.splitArtistNames
import com.ella.music.data.tagIdentityKey
import com.ella.music.viewmodel.MainViewModel
import com.ella.music.viewmodel.PlayerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowBottomSheet
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.ella.music.data.NeteaseArtist

@Composable
fun SongMoreActionHost(
    actionSong: Song?,
    mainViewModel: MainViewModel,
    playerViewModel: PlayerViewModel,
    onDismissAction: () -> Unit,
    onNavigateToAlbum: (Long) -> Unit = {},
    onNavigateToArtist: (String) -> Unit = {},
    onSongRemovedFromPlaylist: ((Song) -> Unit)? = null,
    deleteFromLibrary: Boolean = true,
    showDelete: Boolean = true,
    showLocalFileActions: Boolean = true,
    showAddToQueue: Boolean = true,
    resolveSongForAction: (suspend (Song) -> Song)? = null,
    onDeleteSong: ((Song) -> Unit)? = null,
    extraTopContent: (@Composable ColumnScope.() -> Unit)? = null
) {
    val context = LocalContext.current
    val defaultDangerText = stringResource(R.string.common_delete)
    val actionSheetTitle = stringResource(R.string.song_more_actions_title)
    val addToPlaylistFailed = stringResource(R.string.song_more_add_to_playlist_failed)
    val addToQueueFailed = stringResource(R.string.song_more_add_to_queue_failed)
    val playNextFailed = stringResource(R.string.song_more_play_next_failed)
    val shareFailed = stringResource(R.string.song_more_share_failed)
    val addedToPlayNext = stringResource(R.string.song_more_added_to_play_next)
    val addedToQueue = stringResource(R.string.song_more_added_to_queue)
    val noArtistJump = stringResource(R.string.song_more_no_artist_jump)
    val noAlbumJump = stringResource(R.string.song_more_no_album_jump)
    val selectArtistTitle = stringResource(R.string.song_more_select_artist)
    val addToPlaylistTitle = stringResource(R.string.song_more_add_to_playlist_title)
    val editTagTitle = stringResource(R.string.song_more_edit_tags_title)
    val lyricTimingTitle = stringResource(R.string.song_more_lyric_timing)
    val aiInterpretTitle = stringResource(R.string.song_more_ai_title)
    val playlists by mainViewModel.playlists.collectAsState(initial = emptyList())
    val metadataEditorId by mainViewModel.settingsManager.metadataEditorId.collectAsState(initial = TagEditorOptionIds.ASK_EACH_TIME)
    val lyricTimingEditorId by mainViewModel.settingsManager.lyricTimingEditorId.collectAsState(initial = TagEditorOptionIds.ASK_EACH_TIME)
    val scope = rememberCoroutineScope()
    var playlistSong by remember { mutableStateOf<Song?>(null) }
    var createPlaylistSong by remember { mutableStateOf<Song?>(null) }
    var tagEditorSong by remember { mutableStateOf<Song?>(null) }
    var tagEditorKind by remember { mutableStateOf(TagEditorOptionKind.Metadata) }
    var metadataEditorSong by remember { mutableStateOf<Song?>(null) }
    var ratingSong by remember { mutableStateOf<Song?>(null) }
    var infoSong by remember { mutableStateOf<Song?>(null) }
    var aiSong by remember { mutableStateOf<Song?>(null) }
    var artistChoices by remember { mutableStateOf<List<String>>(emptyList()) }
    var dangerConfirmTitle by remember { mutableStateOf("") }
    var dangerConfirmMessage by remember { mutableStateOf("") }
    var dangerConfirmText by remember { mutableStateOf("") }
    var dangerConfirmAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var pendingWriteRetry by remember { mutableStateOf<(suspend () -> Unit)?>(null) }
    val writePermissionNeeded = stringResource(R.string.song_more_metadata_write_permission_needed)

    val writePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            pendingWriteRetry?.let { retry ->
                scope.launch { retry() }
                pendingWriteRetry = null
            }
        } else {
            pendingWriteRetry = null
            Toast.makeText(context, writePermissionNeeded, Toast.LENGTH_SHORT).show()
        }
    }

    fun closeAction() = onDismissAction()

    fun requestDangerConfirm(
        title: String,
        message: String,
        confirmText: String,
        action: () -> Unit
    ) {
        dangerConfirmTitle = title
        dangerConfirmMessage = message
        dangerConfirmText = confirmText
        dangerConfirmAction = action
    }

    fun runResolvedSongAction(
        sourceSong: Song,
        failureMessage: String,
        action: (Song) -> Unit
    ) {
        scope.launch {
            runCatching {
                resolveSongForAction?.invoke(sourceSong) ?: sourceSong
            }.onSuccess { resolvedSong ->
                action(resolvedSong)
            }.onFailure { error ->
                Toast.makeText(context, error.localizedMessage ?: failureMessage, Toast.LENGTH_SHORT).show()
            }
        }
    }

    actionSong?.let { song ->
        WindowBottomSheet(
            show = true,
            enableNestedScroll = false,
            title = song.title.ifBlank { actionSheetTitle },
            onDismissRequest = ::closeAction
        ) {
            SongMoreActionSheet(
                song = song,
                extraTopContent = extraTopContent,
                onDismiss = ::closeAction,
                onAddToPlaylist = {
                    runResolvedSongAction(song, addToPlaylistFailed) { resolvedSong ->
                        playlistSong = resolvedSong
                        closeAction()
                    }
                },
                onAddToQueue = {
                    runResolvedSongAction(song, addToQueueFailed) { resolvedSong ->
                        playerViewModel.addToPlaylist(resolvedSong)
                        Toast.makeText(context, addedToQueue, Toast.LENGTH_SHORT).show()
                        closeAction()
                    }
                },
                onPlayNext = {
                    runResolvedSongAction(song, playNextFailed) { resolvedSong ->
                        playerViewModel.playNext(resolvedSong)
                        Toast.makeText(context, addedToPlayNext, Toast.LENGTH_SHORT).show()
                        closeAction()
                    }
                },
                onShare = {
                    runResolvedSongAction(song, shareFailed) { resolvedSong ->
                        shareLocalSong(context, resolvedSong)
                        closeAction()
                    }
                },
                onSpectrum = {
                    openSongSpectrumWithAspectPro(context, song)
                    closeAction()
                },
                onInfo = {
                    infoSong = song
                    closeAction()
                },
                onRating = {
                    ratingSong = song
                    closeAction()
                },
                onAiInterpret = {
                    aiSong = song
                    closeAction()
                },
                onArtist = {
                    val artists = splitArtistNames(song.artist)
                        .distinctBy { it.tagIdentityKey() }
                    when (artists.size) {
                        0 -> Toast.makeText(context, noArtistJump, Toast.LENGTH_SHORT).show()
                        1 -> onNavigateToArtist(artists.first())
                        else -> artistChoices = artists
                    }
                    closeAction()
                },
                onAlbum = {
                    val albumId = song.albumIdentityId()
                    if (albumId > 0L) {
                        onNavigateToAlbum(albumId)
                    } else {
                        Toast.makeText(context, noAlbumJump, Toast.LENGTH_SHORT).show()
                    }
                    closeAction()
                },
                onEditTag = if (showLocalFileActions) {
                    {
                        tagEditorKind = TagEditorOptionKind.Metadata
                        tagEditorSong = song
                        closeAction()
                    }
                } else null,
                onLyricTiming = if (showLocalFileActions) {
                    {
                        tagEditorKind = TagEditorOptionKind.LyricTiming
                        tagEditorSong = song
                        closeAction()
                    }
                } else null,
                onRemoveFromPlaylist = onSongRemovedFromPlaylist?.let {
                    {
                        closeAction()
                        requestDangerConfirm(
                            title = context.getString(R.string.playlist_remove_song_title),
                            message = context.getString(
                                R.string.song_more_remove_from_playlist_message,
                                song.title.ifBlank { song.fileName.ifBlank { context.getString(R.string.common_this_song) } }
                            ),
                            confirmText = context.getString(R.string.common_remove)
                        ) {
                            it(song)
                        }
                    }
                },
                onDelete = if (showDelete) {
                    {
                        closeAction()
                        requestDangerConfirm(
                            title = if (deleteFromLibrary) {
                                context.getString(R.string.song_more_delete_song_title)
                            } else {
                                context.getString(R.string.song_more_remove_from_library_title)
                            },
                            message = if (deleteFromLibrary) {
                                context.getString(
                                    R.string.song_more_delete_song_message,
                                    song.title.ifBlank { song.fileName.ifBlank { context.getString(R.string.common_this_song) } }
                                )
                            } else {
                                context.getString(
                                    R.string.song_more_remove_from_library_message,
                                    song.title.ifBlank { song.fileName.ifBlank { context.getString(R.string.common_this_song) } }
                                )
                            },
                            confirmText = if (deleteFromLibrary) {
                                context.getString(R.string.song_more_delete_permanently)
                            } else {
                                context.getString(R.string.common_remove)
                            }
                        ) {
                            if (onDeleteSong != null) {
                                onDeleteSong(song)
                            } else if (deleteFromLibrary) {
                                scope.launch {
                                    val result = mainViewModel.deleteSongsResult(listOf(song))
                                    if (result.isSuccess) {
                                        Toast.makeText(context, context.getString(R.string.library_deleted_songs, 1), Toast.LENGTH_SHORT).show()
                                    } else {
                                        val error = result.exceptionOrNull()
                                        if (error is WritePermissionRequiredException) {
                                            pendingWriteRetry = {
                                                mainViewModel.removeSongsFromLibrary(listOf(song))
                                                Toast.makeText(context, context.getString(R.string.library_deleted_songs, 1), Toast.LENGTH_SHORT).show()
                                            }
                                            writePermissionLauncher.launch(
                                                IntentSenderRequest.Builder(error.intentSender).build()
                                            )
                                        } else {
                                            Toast.makeText(context, error?.localizedMessage ?: context.getString(R.string.song_more_metadata_save_failed), Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            } else {
                                mainViewModel.removeSongsFromLibrary(listOf(song))
                            }
                        }
                    }
                } else null,
                showSpectrum = showLocalFileActions,
                showAddToQueue = showAddToQueue
            )
        }
    }

    ConfirmDangerDialog(
        show = dangerConfirmAction != null,
        title = dangerConfirmTitle,
        message = dangerConfirmMessage,
        confirmText = dangerConfirmText,
        onDismiss = { dangerConfirmAction = null },
        onConfirm = {
            val action = dangerConfirmAction
            dangerConfirmAction = null
            action?.invoke()
        }
    )

    if (artistChoices.isNotEmpty()) {
        WindowBottomSheet(
            show = true,
            enableNestedScroll = false,
            title = selectArtistTitle,
            onDismissRequest = { artistChoices = emptyList() }
        ) {
            ArtistPickerContent(
                artists = artistChoices,
                onArtistSelected = { artist ->
                    artistChoices = emptyList()
                    onNavigateToArtist(artist)
                },
                onDismiss = { artistChoices = emptyList() }
            )
        }
    }

    playlistSong?.let { song ->
        WindowBottomSheet(
            show = true,
            enableNestedScroll = false,
            title = addToPlaylistTitle,
            onDismissRequest = { playlistSong = null }
        ) {
            AddToPlaylistSheet(
                playlists = playlists
                    .sortedWith(compareByDescending<com.ella.music.data.model.UserPlaylist> { it.id == FAVORITES_PLAYLIST_ID }.thenByDescending { it.createdAt }),
                onDismiss = { playlistSong = null },
                onCreatePlaylist = {
                    createPlaylistSong = song
                    playlistSong = null
                },
                onPlaylistsConfirm = { selectedPlaylists, appendToEnd ->
                    selectedPlaylists.forEach { playlist ->
                        mainViewModel.addSongsToPlaylist(playlist.id, listOf(song), appendToEnd)
                    }
                    Toast.makeText(
                        context,
                        context.getString(R.string.player_added_to_playlists, selectedPlaylists.size),
                        Toast.LENGTH_SHORT
                    ).show()
                    playlistSong = null
                }
            )
        }
    }

    createPlaylistSong?.let { song ->
        CreatePlaylistAndAddSheet(
            onDismiss = { createPlaylistSong = null },
            onCreate = { name ->
                mainViewModel.createPlaylist(name) { playlist ->
                    if (playlist != null) {
                        mainViewModel.addSongsToPlaylist(playlist.id, listOf(song))
                        Toast.makeText(
                            context,
                            context.getString(R.string.player_added_to_playlist_named, playlist.name),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                createPlaylistSong = null
            }
        )
    }

    tagEditorSong?.let { song ->
        val builtinOption = remember(song.id, tagEditorKind) {
            TagEditorOption(
                id = TagEditorOptionIds.BUILTIN_CUSTOM_TAG,
                label = context.getString(R.string.settings_editor_builtin_custom_tag),
                summary = context.getString(R.string.tag_editor_builtin_custom_tag_summary),
                kind = TagEditorOptionKind.Metadata,
                intents = emptyList(),
                sourceSong = song
            )
        }
        val tagOptions = remember(song.id, song.path, song.mimeType, tagEditorKind, builtinOption) {
            val external = buildTagEditorOptions(context, song)
                .filter { it.kind == tagEditorKind }
            if (tagEditorKind == TagEditorOptionKind.Metadata) listOf(builtinOption) + external else external
        }
        val preferredEditorId = if (tagEditorKind == TagEditorOptionKind.LyricTiming) {
            lyricTimingEditorId
        } else {
            metadataEditorId
        }
        val preferredOption = remember(tagOptions, preferredEditorId) {
            tagOptions.firstOrNull { it.id == preferredEditorId }
        }
        LaunchedEffect(song.id, preferredEditorId, preferredOption, tagEditorKind) {
            if (preferredEditorId.isNotBlank() && preferredOption != null) {
                if (preferredOption.id == TagEditorOptionIds.BUILTIN_CUSTOM_TAG) {
                    metadataEditorSong = song
                } else {
                    launchTagEditorOption(context, preferredOption)
                }
                tagEditorSong = null
            }
        }
        WindowBottomSheet(
            show = true,
            enableNestedScroll = false,
            title = if (tagEditorKind == TagEditorOptionKind.LyricTiming) lyricTimingTitle else editTagTitle,
            onDismissRequest = { tagEditorSong = null }
        ) {
            SongTagEditorSheet(
                song = song,
                options = tagOptions,
                onDismiss = { tagEditorSong = null },
                onOptionClick = { option ->
                    if (option.id == TagEditorOptionIds.BUILTIN_CUSTOM_TAG) {
                        metadataEditorSong = song
                    } else {
                        launchTagEditorOption(context, option)
                    }
                    tagEditorSong = null
                }
            )
        }
    }

    metadataEditorSong?.let { song ->
        WindowBottomSheet(
            show = true,
            enableNestedScroll = false,
            title = stringResource(R.string.song_more_metadata_editor_title),
            onDismissRequest = { metadataEditorSong = null }
        ) {
            SongMetadataEditorSheet(
                song = song,
                mainViewModel = mainViewModel,
                onDismiss = { metadataEditorSong = null },
                onSave = { tags ->
                    scope.launch {
                        val result = mainViewModel.writeSongMetadata(song, tags)
                        if (result.isSuccess) {
                            Toast.makeText(context, context.getString(R.string.song_more_metadata_saved), Toast.LENGTH_SHORT).show()
                            metadataEditorSong = null
                        } else {
                            val error = result.exceptionOrNull()
                            if (error is WritePermissionRequiredException) {
                                pendingWriteRetry = {
                                    val retryResult = mainViewModel.writeSongMetadata(song, tags)
                                    if (retryResult.isSuccess) {
                                        Toast.makeText(context, context.getString(R.string.song_more_metadata_saved), Toast.LENGTH_SHORT).show()
                                        metadataEditorSong = null
                                    } else {
                                        Toast.makeText(context, retryResult.exceptionOrNull()?.localizedMessage ?: context.getString(R.string.song_more_metadata_save_failed), Toast.LENGTH_SHORT).show()
                                    }
                                }
                                writePermissionLauncher.launch(
                                    IntentSenderRequest.Builder(error.intentSender).build()
                                )
                            } else {
                                Toast.makeText(context, error?.localizedMessage ?: context.getString(R.string.song_more_metadata_save_failed), Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            )
        }
    }

    ratingSong?.let { song ->
        WindowBottomSheet(
            show = true,
            enableNestedScroll = false,
            title = stringResource(R.string.song_more_rating_title),
            onDismissRequest = { ratingSong = null }
        ) {
            RatingSheet(
                currentRating = mainViewModel.getSongRating(song),
                onDismiss = { ratingSong = null },
                onRatingSelected = { rating ->
                    scope.launch {
                        val result = mainViewModel.writeSongRating(song, rating)
                        if (result.isSuccess) {
                            Toast.makeText(context, context.getString(R.string.song_more_rating_saved), Toast.LENGTH_SHORT).show()
                            ratingSong = null
                        } else {
                            val error = result.exceptionOrNull()
                            if (error is WritePermissionRequiredException) {
                                pendingWriteRetry = {
                                    val retryResult = mainViewModel.writeSongRating(song, rating)
                                    if (retryResult.isSuccess) {
                                        Toast.makeText(context, context.getString(R.string.song_more_rating_saved), Toast.LENGTH_SHORT).show()
                                        ratingSong = null
                                    } else {
                                        Toast.makeText(context, retryResult.exceptionOrNull()?.localizedMessage ?: context.getString(R.string.song_more_rating_failed), Toast.LENGTH_SHORT).show()
                                    }
                                }
                                writePermissionLauncher.launch(
                                    IntentSenderRequest.Builder(error.intentSender).build()
                                )
                            } else {
                                Toast.makeText(context, error?.localizedMessage ?: context.getString(R.string.song_more_rating_failed), Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            )
        }
    }

    infoSong?.let { song ->
        WindowBottomSheet(
            show = true,
            enableNestedScroll = false,
            title = stringResource(R.string.player_song_details),
            onDismissRequest = { infoSong = null }
        ) {
            SongInfoSheet(
                song = song,
                audioInfoLoader = mainViewModel::getAudioInfo,
                tagInfoLoader = mainViewModel::getSongTagInfo,
                onDismiss = { infoSong = null }
            )
        }
    }

    aiSong?.let { song ->
        WindowBottomSheet(
            show = true,
            enableNestedScroll = false,
            title = aiInterpretTitle,
            onDismissRequest = { aiSong = null }
        ) {
            SongAiInterpretationSheet(
                song = song,
                mainViewModel = mainViewModel,
                onDismiss = { aiSong = null }
            )
        }
    }
}
@Composable
private fun SongMoreActionSheet(
    song: Song,
    extraTopContent: (@Composable ColumnScope.() -> Unit)?,
    onDismiss: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onAddToQueue: () -> Unit,
    onPlayNext: () -> Unit,
    onShare: () -> Unit,
    onSpectrum: () -> Unit,
    onInfo: () -> Unit,
    onRating: () -> Unit,
    onAiInterpret: () -> Unit,
    onArtist: () -> Unit,
    onAlbum: () -> Unit,
    onEditTag: (() -> Unit)?,
    onLyricTiming: (() -> Unit)?,
    onRemoveFromPlaylist: (() -> Unit)?,
    onDelete: (() -> Unit)?,
    showSpectrum: Boolean,
    showAddToQueue: Boolean
) {
    SongSheetColumn {
        extraTopContent?.invoke(this)
        SongMenuItem(stringResource(R.string.song_more_add_to_playlist), onAddToPlaylist)
        if (showAddToQueue) {
            SongMenuItem(stringResource(R.string.common_add_to_queue), onAddToQueue)
        }
        SongMenuItem(stringResource(R.string.song_more_play_next), onPlayNext)
        SongMenuItem(stringResource(R.string.common_share), onShare)
        if (showSpectrum) {
            SongMenuItem(stringResource(R.string.song_more_view_spectrum), onSpectrum)
        }
        SongMenuItem(stringResource(R.string.song_more_ai_title), onAiInterpret)
        SongMenuItem(stringResource(R.string.song_more_view_song_info), onInfo)
        SongMenuItem(stringResource(R.string.song_more_set_rating), onRating)
        SongMenuItem(
            stringResource(
                R.string.song_more_artist_entry,
                song.artist.ifBlank { stringResource(R.string.player_unknown_artist) }
            ),
            onArtist
        )
        SongMenuItem(
            stringResource(
                R.string.song_more_album_entry,
                song.album.ifBlank { stringResource(R.string.player_unknown_album) }
            ),
            onAlbum
        )
        if (onEditTag != null) {
            SongMenuItem(stringResource(R.string.song_more_edit_tags_title), onEditTag)
        }
        if (onLyricTiming != null) {
            SongMenuItem(stringResource(R.string.song_more_lyric_timing), onLyricTiming)
        }
        if (onRemoveFromPlaylist != null) {
            SongMenuItem(stringResource(R.string.playlist_remove_song_title), onRemoveFromPlaylist, danger = true)
        }
        if (onDelete != null) {
            SongMenuItem(stringResource(R.string.song_more_delete_permanently), onDelete, danger = true)
        }
        SongMenuItem(stringResource(R.string.common_cancel), onDismiss)
    }
}

@Composable
private fun SongTagEditorSheet(
    song: Song,
    options: List<TagEditorOption>,
    onDismiss: () -> Unit,
    onOptionClick: (TagEditorOption) -> Unit
) {
    SongSheetColumn {
        Text(
            text = song.title,
            fontSize = 13.sp,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        options.forEach { option -> SongMenuItem(option.label, onClick = { onOptionClick(option) }) }
        SongMenuItem(stringResource(R.string.common_cancel), onDismiss)
    }
}
