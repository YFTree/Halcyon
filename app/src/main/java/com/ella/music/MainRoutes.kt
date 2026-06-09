package com.ella.music

import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import com.ella.music.ui.navigation.EXTRA_SHORTCUT_ROUTE
import com.ella.music.ui.navigation.EXTRA_SHORTCUT_ROUTE_NEW
import com.ella.music.ui.navigation.Screen

internal fun Intent.resolveShortcutRoute(): String {
    val uri = data
    if (uri != null && uri.scheme in setOf("ella", "halcyon")) {
        val host = uri.host.orEmpty()
        if (host == "search") {
            val keyword = uri.getQueryParameter("keyword")
            return Screen.LibrarySearch.createRoute(
                type = uri.getQueryParameter("type"),
                keyword = keyword
            )
        }
        if (host == "shortcut") {
            uri.getQueryParameter("route")
                ?.takeIf { it.isNotBlank() }
                ?.let { return it }
        }
        uri.getQueryParameter("route")
            ?.takeIf { it.isNotBlank() }
            ?.let { return it }
    }
    return getStringExtra(EXTRA_SHORTCUT_ROUTE)
        ?: getStringExtra(EXTRA_SHORTCUT_ROUTE_NEW)
        ?: ""
}

internal fun String?.toCurrentTabRoute(): String? {
    return when {
        this == null -> null
        this == Screen.Home.route -> Screen.Home.route
        this == Screen.Library.route -> Screen.Library.route
        this.isSearchRoute() -> Screen.LibrarySearch.createRoute()
        this == Screen.Playlists.route -> Screen.Playlists.route
        this == Screen.Folder.route -> Screen.Folder.route
        this == Screen.Artist.route -> Screen.Artist.route
        this == Screen.Album.route -> Screen.Album.route
        else -> null
    }
}

internal fun String?.isSearchRoute(): Boolean {
    return this?.startsWith(Screen.LibrarySearch.baseRoute) == true ||
        this == Screen.LibrarySearch.route
}

internal fun String?.isBottomDockRoute(): Boolean {
    return when {
        this == null -> false
        this.isSearchRoute() -> true
        this == Screen.Home.route -> true
        this == Screen.Library.route -> true
        this == Screen.Playlists.route -> true
        this == Screen.Folder.route -> true
        this == Screen.Artist.route -> true
        this == Screen.Album.route -> true
        else -> false
    }
}

internal fun String?.matchesRoute(route: String): Boolean {
    return when {
        route.startsWith(Screen.LibrarySearch.baseRoute) -> this.isSearchRoute()
        else -> this == route
    }
}

internal fun String.isMusicSymbolOnly(): Boolean {
    val content = trim()
    if (content.isBlank()) return true

    return content.all { char ->
        char.isWhitespace() ||
            char in setOf('♪', '♫', '♬', '♩', '♭', '♯', '♮') ||
            Character.UnicodeBlock.of(char) == Character.UnicodeBlock.MUSICAL_SYMBOLS
    }
}

internal fun Uri.toPrimaryStoragePath(): String? {
    val documentId = runCatching { DocumentsContract.getTreeDocumentId(this) }.getOrNull() ?: return null
    val parts = documentId.split(':', limit = 2)
    val volume = parts.firstOrNull().orEmpty()
    val path = parts.getOrNull(1).orEmpty().trim('/')
    return when {
        volume.equals("primary", ignoreCase = true) && path.isBlank() -> "/storage/emulated/0"
        volume.equals("primary", ignoreCase = true) -> "/storage/emulated/0/$path"
        else -> null
    }
}
