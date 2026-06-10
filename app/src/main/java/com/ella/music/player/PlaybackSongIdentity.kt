package com.ella.music.player

import com.ella.music.data.model.Song

internal fun Song?.isSamePlaybackIdentity(other: Song?): Boolean {
    if (this == null || other == null) return this == other
    if (id > 0L && id == other.id) return true
    return path.isNotBlank() && path == other.path
}

internal fun Song.playbackStackKey(): String = when {
    id > 0L -> "id:$id"
    path.isNotBlank() -> "path:$path"
    else -> "title:$title|artist:$artist|album:$album"
}

internal fun Song.notificationArtworkKey(): String =
    "${id}:${path}:${dateModified}:${fileSize}:${coverUrl}"
