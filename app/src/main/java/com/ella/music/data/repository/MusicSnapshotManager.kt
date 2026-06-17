package com.ella.music.data.repository

import android.util.Log
import com.ella.music.data.model.Song
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import org.json.JSONObject

internal data class RatingSnapshotEntry(
    val rating: Int,
    val dateModified: Long,
    val fileSize: Long
)

internal class MusicSnapshotManager(
    private val librarySearchSnapshotFile: File,
    private val libraryRatingSnapshotFile: File,
    private val searchTextBuilder: (Song) -> String
) {
    private val searchTextCache = ConcurrentHashMap<String, String>()
    private val ratingSnapshotCache = ConcurrentHashMap<String, RatingSnapshotEntry>()
    @Volatile private var searchSnapshotLoaded = false
    @Volatile private var searchSnapshotDirty = false
    @Volatile private var ratingSnapshotLoaded = false
    @Volatile private var ratingSnapshotDirty = false

    fun getSongSearchText(song: Song): String {
        ensureSearchSnapshotLoaded()
        val key = song.searchSnapshotKey()
        searchTextCache[key]?.let { return it }
        val text = buildSongSearchText(song)
        searchTextCache[key] = text
        searchSnapshotDirty = true
        return text
    }

    fun getSongRating(song: Song): Int {
        ensureRatingSnapshotLoaded()
        val key = song.searchSnapshotKey()
        val entry = ratingSnapshotCache[key] ?: return 0
        return if (entry.isFreshFor(song)) entry.rating else 0
    }

    fun updateRatingSnapshot(song: Song, rating: Int) {
        ensureRatingSnapshotLoaded()
        ratingSnapshotCache[song.searchSnapshotKey()] = RatingSnapshotEntry(
            rating = rating.coerceIn(0, 5),
            dateModified = song.dateModified,
            fileSize = song.fileSize
        )
        ratingSnapshotDirty = true
    }

    fun preloadSearchSnapshot(songs: List<Song>) {
        ensureSearchSnapshotLoaded()
        songs.forEach { song ->
            val key = song.searchSnapshotKey()
            if (!searchTextCache.containsKey(key)) {
                searchTextCache[key] = buildSongSearchText(song)
            }
        }
        searchSnapshotDirty = true
        saveSearchSnapshot()
    }

    fun preloadSongRatings(songs: List<Song>) {
        ensureRatingSnapshotLoaded()
        songs.forEach { song ->
            val key = song.searchSnapshotKey()
            if (!ratingSnapshotCache.containsKey(key)) {
                ratingSnapshotCache[key] = RatingSnapshotEntry(
                    rating = 0, dateModified = song.dateModified, fileSize = song.fileSize
                )
            }
        }
        ratingSnapshotDirty = true
    }

    suspend fun songMatchesSearchSnapshot(song: Song, query: String): Boolean {
        val text = getSongSearchText(song)
        return text.contains(query, ignoreCase = true)
    }

    suspend fun filterSongsBySearchSnapshot(songs: List<Song>, query: String): List<Song> {
        if (query.isBlank()) return songs
        return songs.filter { songMatchesSearchSnapshot(it, query) }
    }

    fun clearCache() {
        ratingSnapshotCache.clear()
        ratingSnapshotLoaded = false
        ratingSnapshotDirty = false
    }

    fun clearLibraryCache() {
        searchTextCache.clear()
        searchSnapshotLoaded = true
        searchSnapshotDirty = false
        if (librarySearchSnapshotFile.exists()) librarySearchSnapshotFile.delete()
        ratingSnapshotCache.clear()
        ratingSnapshotLoaded = true
        ratingSnapshotDirty = false
        if (libraryRatingSnapshotFile.exists()) libraryRatingSnapshotFile.delete()
    }

    fun clearMetadataCache(song: Song) {
        ensureRatingSnapshotLoaded()
        ratingSnapshotCache.keys.removeAll { it == song.searchSnapshotKey() || it.startsWith("${song.id}|") }
        ratingSnapshotDirty = true
        saveRatingSnapshot()
        ensureSearchSnapshotLoaded()
        searchTextCache.keys.removeAll { it == song.searchSnapshotKey() || it.startsWith("${song.id}|") }
        saveSearchSnapshot()
    }

    fun saveAll() {
        saveSearchSnapshot()
        saveRatingSnapshot()
    }

    private fun buildSongSearchText(song: Song): String = searchTextBuilder(song)

    private fun ensureSearchSnapshotLoaded() {
        if (searchSnapshotLoaded) return
        synchronized(searchTextCache) {
            if (searchSnapshotLoaded) return
            if (librarySearchSnapshotFile.exists()) {
                runCatching {
                    val root = JSONObject(librarySearchSnapshotFile.readText())
                    root.keys().forEach { key ->
                        val value = root.optString(key)
                        val parts = key.split('|')
                        val stableKey = if (parts.size >= 2) "${parts[0]}|${parts[1]}" else key
                        searchTextCache[stableKey] = value
                    }
                }.onFailure {
                    Log.w("MusicRepo", "Failed to load library search snapshot", it)
                    searchTextCache.clear()
                }
            }
            searchSnapshotLoaded = true
        }
    }

    private fun saveSearchSnapshot() {
        if (!searchSnapshotDirty) return
        runCatching {
            val root = JSONObject()
            searchTextCache.forEach { (key, value) -> root.put(key, value) }
            librarySearchSnapshotFile.writeText(root.toString())
            searchSnapshotDirty = false
        }.onFailure {
            Log.w("MusicRepo", "Failed to save library search snapshot", it)
        }
    }

    private fun ensureRatingSnapshotLoaded() {
        if (ratingSnapshotLoaded) return
        synchronized(ratingSnapshotCache) {
            if (ratingSnapshotLoaded) return
            if (libraryRatingSnapshotFile.exists()) {
                runCatching {
                    val root = JSONObject(libraryRatingSnapshotFile.readText())
                    root.keys().forEach { key ->
                        val value = root.optJSONObject(key) ?: return@forEach
                        ratingSnapshotCache[key] = RatingSnapshotEntry(
                            rating = value.optInt("rating", 0).coerceIn(0, 5),
                            dateModified = value.optLong("dateModified", 0L),
                            fileSize = value.optLong("fileSize", 0L)
                        )
                    }
                }.onFailure {
                    Log.w("MusicRepo", "Failed to load library rating snapshot", it)
                    ratingSnapshotCache.clear()
                }
            }
            ratingSnapshotLoaded = true
        }
    }

    private fun saveRatingSnapshot() {
        if (!ratingSnapshotDirty) return
        runCatching {
            val root = JSONObject()
            ratingSnapshotCache.forEach { (key, value) ->
                root.put(key, JSONObject()
                    .put("rating", value.rating)
                    .put("dateModified", value.dateModified)
                    .put("fileSize", value.fileSize))
            }
            libraryRatingSnapshotFile.writeText(root.toString())
            ratingSnapshotDirty = false
        }.onFailure {
            Log.w("MusicRepo", "Failed to save library rating snapshot", it)
        }
    }

    private fun RatingSnapshotEntry.isFreshFor(song: Song): Boolean =
        dateModified == song.dateModified && fileSize == song.fileSize
}
