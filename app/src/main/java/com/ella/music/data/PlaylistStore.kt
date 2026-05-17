package com.ella.music.data

import android.content.Context
import android.util.Log
import com.ella.music.data.model.FAVORITES_PLAYLIST_ID
import com.ella.music.data.model.Song
import com.ella.music.data.model.UserPlaylist
import com.ella.music.data.model.playlistIdentityKey
import com.ella.music.data.model.toJson
import com.ella.music.data.model.toPlaylistSong
import com.ella.music.data.model.toUserPlaylist
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class PlaylistStore private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val file = File(appContext.filesDir, "ella_playlists.json")
    private val lock = Any()
    private val _playlists = MutableStateFlow(loadPlaylists())

    val playlists: StateFlow<List<UserPlaylist>> = _playlists.asStateFlow()

    fun favoriteSongKeys(): Set<String> =
        playlists.value
            .firstOrNull { it.id == FAVORITES_PLAYLIST_ID }
            ?.songs
            ?.mapTo(mutableSetOf()) { it.key }
            ?: emptySet()

    fun isFavorite(song: Song): Boolean =
        song.playlistIdentityKey() in favoriteSongKeys()

    suspend fun toggleFavorite(song: Song): Boolean = withContext(Dispatchers.IO) {
        val key = song.playlistIdentityKey()
        var added = false
        synchronized(lock) {
            val now = System.currentTimeMillis()
            val next = playlists.value.withPlaylist(FAVORITES_PLAYLIST_ID) { playlist ->
                val exists = playlist.songs.any { it.key == key }
                added = !exists
                val nextSongs = if (exists) {
                    playlist.songs.filterNot { it.key == key }
                } else {
                    listOf(song.toPlaylistSong(now)) + playlist.songs
                }
                playlist.copy(songs = nextSongs, updatedAt = now)
            }
            _playlists.value = next
            saveLocked(next)
        }
        added
    }

    suspend fun createPlaylist(name: String): UserPlaylist? = withContext(Dispatchers.IO) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return@withContext null
        synchronized(lock) {
            val now = System.currentTimeMillis()
            val playlist = UserPlaylist(
                id = "playlist-${UUID.randomUUID()}",
                name = trimmed,
                createdAt = now,
                updatedAt = now
            )
            val next = playlists.value + playlist
            _playlists.value = next
            saveLocked(next)
            playlist
        }
    }

    suspend fun deletePlaylist(id: String) = withContext(Dispatchers.IO) {
        if (id == FAVORITES_PLAYLIST_ID) return@withContext
        synchronized(lock) {
            val next = playlists.value.filterNot { it.id == id }.ensureFavorites()
            _playlists.value = next
            saveLocked(next)
        }
    }

    suspend fun addSongToPlaylist(playlistId: String, song: Song) = withContext(Dispatchers.IO) {
        val key = song.playlistIdentityKey()
        synchronized(lock) {
            val now = System.currentTimeMillis()
            val next = playlists.value.withPlaylist(playlistId) { playlist ->
                if (playlist.songs.any { it.key == key }) return@withPlaylist playlist
                playlist.copy(
                    songs = listOf(song.toPlaylistSong(now)) + playlist.songs,
                    updatedAt = now
                )
            }
            _playlists.value = next
            saveLocked(next)
        }
    }

    suspend fun addSongsToPlaylist(playlistId: String, songs: Collection<Song>) = withContext(Dispatchers.IO) {
        if (songs.isEmpty()) return@withContext
        synchronized(lock) {
            val now = System.currentTimeMillis()
            val next = playlists.value.withPlaylist(playlistId) { playlist ->
                val existingKeys = playlist.songs.mapTo(mutableSetOf()) { it.key }
                val newSongs = songs
                    .filter { existingKeys.add(it.playlistIdentityKey()) }
                    .map { it.toPlaylistSong(now) }
                if (newSongs.isEmpty()) return@withPlaylist playlist
                playlist.copy(
                    songs = newSongs + playlist.songs,
                    updatedAt = now
                )
            }
            _playlists.value = next
            saveLocked(next)
        }
    }

    suspend fun removeSongFromPlaylist(playlistId: String, songKey: String) = withContext(Dispatchers.IO) {
        synchronized(lock) {
            val now = System.currentTimeMillis()
            val next = playlists.value.withPlaylist(playlistId) { playlist ->
                playlist.copy(
                    songs = playlist.songs.filterNot { it.key == songKey },
                    updatedAt = now
                )
            }
            _playlists.value = next
            saveLocked(next)
        }
    }

    private fun loadPlaylists(): List<UserPlaylist> {
        if (!file.exists()) return emptyList<UserPlaylist>().ensureFavorites()
        return runCatching {
            val root = JSONObject(file.readText())
            val items = root.optJSONArray("playlists") ?: JSONArray()
            List(items.length()) { index -> items.optJSONObject(index)?.toUserPlaylist() }
                .filterNotNull()
                .filter { it.id.isNotBlank() && it.name.isNotBlank() }
                .ensureFavorites()
        }.getOrElse {
            Log.w(TAG, "Failed to load playlists", it)
            emptyList<UserPlaylist>().ensureFavorites()
        }
    }

    private fun List<UserPlaylist>.ensureFavorites(): List<UserPlaylist> {
        if (any { it.id == FAVORITES_PLAYLIST_ID }) return this
        val now = System.currentTimeMillis()
        return listOf(
            UserPlaylist(
                id = FAVORITES_PLAYLIST_ID,
                name = "我喜欢的音乐",
                createdAt = now,
                updatedAt = now
            )
        ) + this
    }

    private fun List<UserPlaylist>.withPlaylist(
        playlistId: String,
        transform: (UserPlaylist) -> UserPlaylist
    ): List<UserPlaylist> {
        val ensured = ensureFavorites()
        return ensured.map { playlist ->
            if (playlist.id == playlistId) transform(playlist) else playlist
        }
    }

    private fun saveLocked(playlists: List<UserPlaylist>) {
        runCatching {
            val root = JSONObject()
                .put("version", 1)
                .put("playlists", JSONArray().also { array ->
                    playlists.ensureFavorites().forEach { array.put(it.toJson()) }
                })
            file.writeText(root.toString())
        }.onFailure {
            Log.w(TAG, "Failed to save playlists", it)
        }
    }

    companion object {
        private const val TAG = "PlaylistStore"

        @Volatile
        private var instance: PlaylistStore? = null

        fun getInstance(context: Context): PlaylistStore =
            instance ?: synchronized(this) {
                instance ?: PlaylistStore(context).also { instance = it }
            }
    }
}
