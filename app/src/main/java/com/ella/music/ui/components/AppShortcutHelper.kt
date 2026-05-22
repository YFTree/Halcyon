package com.ella.music.ui.components

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Build
import com.ella.music.MainActivity
import com.ella.music.R
import com.ella.music.ui.navigation.EXTRA_SHORTCUT_ROUTE

fun requestPinnedEllaShortcut(
    context: Context,
    id: String,
    label: String,
    route: String
): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
    val shortcutManager = context.getSystemService(ShortcutManager::class.java) ?: return false
    if (!shortcutManager.isRequestPinShortcutSupported) return false
    val intent = Intent(context, MainActivity::class.java).apply {
        action = Intent.ACTION_VIEW
        putExtra(EXTRA_SHORTCUT_ROUTE, route)
    }
    val shortcut = ShortcutInfo.Builder(context, id.toShortcutId())
        .setShortLabel(label.take(10).ifBlank { "Ella Music" })
        .setLongLabel(label.ifBlank { "Ella Music" })
        .setIcon(Icon.createWithResource(context, R.mipmap.ic_launcher))
        .setIntent(intent)
        .build()
    shortcutManager.requestPinShortcut(shortcut, null)
    return true
}

private fun String.toShortcutId(): String =
    "ella_${replace(Regex("[^A-Za-z0-9_.-]"), "_").take(80)}"

