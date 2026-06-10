package com.ella.music.data.repository

import com.ella.music.data.model.Album
import com.ella.music.data.model.Song
import org.json.JSONArray
import org.json.JSONObject

internal fun songsToLibraryCacheJsonArray(songs: List<Song>): JSONArray {
    val array = JSONArray()
    songs.forEach { song ->
        array.put(
            JSONObject()
                .put("id", song.id)
                .put("title", song.title)
                .put("artist", song.artist)
                .put("album", song.album)
                .put("albumId", song.albumId)
                .put("duration", song.duration)
                .put("path", song.path)
                .put("fileName", song.fileName)
                .put("fileSize", song.fileSize)
                .put("mimeType", song.mimeType)
                .put("dateAdded", song.dateAdded)
                .put("dateModified", song.dateModified)
                .put("trackNumber", song.trackNumber)
                .put("discNumber", song.discNumber)
                .put("albumArtist", song.albumArtist)
                .put("genre", song.genre)
                .put("year", song.year)
                .put("composer", song.composer)
                .put("lyricist", song.lyricist)
                .put("coverUrl", song.coverUrl)
                .put("onlineSource", song.onlineSource)
                .put("onlineId", song.onlineId)
                .put("onlineLyrics", song.onlineLyrics)
                .put("onlineLyricTranslation", song.onlineLyricTranslation)
        )
    }
    return array
}

internal fun albumsToLibraryCacheJsonArray(albums: List<Album>): JSONArray {
    val array = JSONArray()
    albums.forEach { album ->
        array.put(
            JSONObject()
                .put("id", album.id)
                .put("name", album.name)
                .put("artist", album.artist)
                .put("songCount", album.songCount)
                .put("year", album.year)
                .put("artAlbumId", album.artAlbumId)
                .put("albumArtist", album.albumArtist)
        )
    }
    return array
}

internal fun JSONArray.toLibraryCacheSongList(): List<Song> =
    List(length()) { index ->
        val item = getJSONObject(index)
        Song(
            id = item.getLong("id"),
            title = item.optString("title"),
            artist = item.optString("artist"),
            album = item.optString("album"),
            albumId = item.optLong("albumId"),
            duration = item.optLong("duration"),
            path = item.optString("path"),
            fileName = item.optString("fileName"),
            fileSize = item.optLong("fileSize"),
            mimeType = item.optString("mimeType"),
            dateAdded = item.optLong("dateAdded"),
            dateModified = item.optLong("dateModified"),
            trackNumber = item.optInt("trackNumber"),
            discNumber = item.optInt("discNumber"),
            albumArtist = item.optString("albumArtist"),
            genre = item.optString("genre"),
            year = item.optString("year"),
            composer = item.optString("composer"),
            lyricist = item.optString("lyricist"),
            coverUrl = item.optString("coverUrl"),
            onlineSource = item.optString("onlineSource"),
            onlineId = item.optString("onlineId"),
            onlineLyrics = item.optString("onlineLyrics"),
            onlineLyricTranslation = item.optString("onlineLyricTranslation")
        )
    }

internal fun JSONArray.toLibraryCacheAlbumList(): List<Album> =
    List(length()) { index ->
        val item = getJSONObject(index)
        Album(
            id = item.getLong("id"),
            name = item.optString("name"),
            artist = item.optString("artist"),
            songCount = item.optInt("songCount"),
            year = item.optString("year", "").ifBlank { item.optInt("year").takeIf { it > 0 }?.toString() ?: "" },
            artAlbumId = item.optLong("artAlbumId", item.optLong("id")),
            albumArtist = item.optString("albumArtist")
        )
    }
