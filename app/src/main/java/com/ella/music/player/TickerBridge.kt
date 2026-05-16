package com.ella.music.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.ella.music.R

class TickerBridge(private val context: Context) {

    companion object {
        private const val TAG = "TickerBridge"

        // 换一个新的 Channel ID，避免旧通知渠道配置缓存影响测试
        private const val CHANNEL_ID = "ella_flyme_ticker_lyrics_v2"
        private const val NOTIFICATION_ID = 0x454c4c41
        private const val LEGACY_HIDDEN_NOTIFICATION_ID = 1001
        private const val FLAG_ALWAYS_SHOW_TICKER_FALLBACK = 0x1000000
        private const val FLAG_ONLY_UPDATE_TICKER_FALLBACK = 0x2000000
        private const val ACTION_SEND_LYRIC = "com.meizu.flyme.ticker.ACTION_SEND"
        private const val ACTION_CLEAR_LYRIC = "com.meizu.flyme.ticker.ACTION_CLEAR"
        private const val SYSTEM_UI_PACKAGE = "com.android.systemui"
    }

    private var enabled = false
    private var hideNotification = false
    private var lastPayload: Pair<String?, String?>? = null
    private var hardCancelStandalonePending = true

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private val flagAlwaysShowTicker: Int by lazy {
        getNotificationFlag("FLAG_ALWAYS_SHOW_TICKER")
    }

    private val flagOnlyUpdateTicker: Int by lazy {
        getNotificationFlag("FLAG_ONLY_UPDATE_TICKER")
    }

    fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
        if (!enabled) {
            clearLyric()
        } else if (hideNotification) {
            hardCancelStandalonePending = true
            cancelStandaloneTickerNotifications()
        }
    }

    fun isEnabled() = enabled

    fun setHideNotification(enabled: Boolean) {
        hideNotification = enabled
        if (enabled) {
            hardCancelStandalonePending = true
            cancelStandaloneTickerNotifications()
        } else {
            PlaybackTickerState.clear()
        }
        lastPayload = null
    }

    fun sendLyric(text: String?, translation: String? = null) {
        if (!enabled) return
        val cleanTranslation = translation?.takeIf { it.isNotBlank() }
        val payload = text to cleanTranslation
        if (payload == lastPayload) return
        lastPayload = payload

        try {
            if (text.isNullOrEmpty()) {
                clearLyric()
                return
            }

            val intent = Intent(ACTION_SEND_LYRIC).apply {
                putExtra("ticker_text", text)
                putExtra("lyric", text)
                putExtra("text", text)
                putExtra("content", text)
                putExtra("ticker_package", context.packageName)
                putExtra("package", context.packageName)
                putExtra("ticker_app_name", "Ella Music")
                putExtra("app_name", "Ella Music")
            }

            sendFlymeBroadcast(intent)
            if (hideNotification) {
                PlaybackTickerState.update(text, cleanTranslation)
                cancelStandaloneTickerNotifications()
            } else {
                PlaybackTickerState.clear()
                postTickerNotification(text, cleanTranslation)
            }

            Log.d(TAG, "Ticker lyric sent: $text")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send ticker lyric", e)
        }
    }

    fun clearLyric() {
        lastPayload = null

        try {
            val intent = Intent(ACTION_CLEAR_LYRIC).apply {
                putExtra("ticker_package", context.packageName)
                putExtra("package", context.packageName)
            }

            sendFlymeBroadcast(intent)
            PlaybackTickerState.clear()
            cancelStandaloneTickerNotifications()
        } catch (_: Exception) {
        }
    }

    private fun cancelStandaloneTickerNotifications() {
        notificationManager.cancel(NOTIFICATION_ID)
        notificationManager.cancel(null, NOTIFICATION_ID)
        notificationManager.cancel(LEGACY_HIDDEN_NOTIFICATION_ID)
        notificationManager.cancel("ranker_group", Int.MAX_VALUE)
        if (hardCancelStandalonePending) {
            hardCancelStandalonePending = false
            runCatching {
                ensureNotificationChannel(CHANNEL_ID)
                val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    Notification.Builder(context, CHANNEL_ID)
                } else {
                    Notification.Builder(context)
                }
                val disposable = builder
                    .setSmallIcon(R.drawable.ic_flyme_ticker)
                    .setContentTitle("")
                    .setContentText("")
                    .setShowWhen(false)
                    .setOnlyAlertOnce(true)
                    .setLocalOnly(true)
                    .setOngoing(false)
                    .setAutoCancel(true)
                    .setPriority(Notification.PRIORITY_MIN)
                    .build()
                notificationManager.notify(NOTIFICATION_ID, disposable)
                notificationManager.cancel(NOTIFICATION_ID)
                notificationManager.cancel(null, NOTIFICATION_ID)
                notificationManager.cancel("ranker_group", Int.MAX_VALUE)
            }
        }
    }

    private fun sendFlymeBroadcast(intent: Intent) {
        context.sendBroadcast(intent)
        context.sendBroadcast(Intent(intent).setPackage(SYSTEM_UI_PACKAGE))
    }

    @Suppress("DEPRECATION")
    private fun postTickerNotification(text: String, translation: String?) {
        val channelId = CHANNEL_ID
        val notificationId = NOTIFICATION_ID
        ensureNotificationChannel(channelId)

        val flymeTickerSupported = isFlymeTickerSupported()
        val tickerText: CharSequence? = if (flymeTickerSupported) text else null

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(context, channelId)
        } else {
            Notification.Builder(context)
        }

        val notification = builder
            .setSmallIcon(R.drawable.ic_flyme_ticker)
            .setContentTitle(text)
            .setContentText(translation.orEmpty())
            .setTicker(tickerText)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setLocalOnly(true)
            .setDefaults(0)
            .setPriority(Notification.PRIORITY_MAX)
            .setCategory(Notification.CATEGORY_STATUS)
            .build()

        notification.flags = notification.flags or Notification.FLAG_NO_CLEAR

        if (flymeTickerSupported) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                notification.extras.putBoolean("ticker_icon_switch", false)
                notification.extras.putInt("ticker_icon", R.drawable.ic_flyme_ticker)
            }

            notification.flags = notification.flags or flagAlwaysShowTicker
            notification.flags = notification.flags or flagOnlyUpdateTicker

            Log.d(
                TAG,
                "Flyme ticker notification posted, text=$text, hidden=$hideNotification, showFlag=$flagAlwaysShowTicker, updateFlag=$flagOnlyUpdateTicker"
            )
        } else {
            Log.w(TAG, "Flyme ticker flags are using fallback values")
        }

        notificationManager.notify(notificationId, notification)
    }

    private fun ensureNotificationChannel(channelId: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        if (notificationManager.getNotificationChannel(channelId) != null) return

        val channel = NotificationChannel(
            channelId,
            "Flyme 状态栏歌词",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "用于向 Flyme 状态栏推送歌词"
            setSound(null, null)
            enableVibration(false)
            setShowBadge(false)
        }

        notificationManager.createNotificationChannel(channel)
    }

    private fun isFlymeTickerSupported(): Boolean {
        return flagAlwaysShowTicker > 0 && flagOnlyUpdateTicker > 0
    }

    private fun getNotificationFlag(name: String): Int {
        return try {
            val field = Notification::class.java.getDeclaredField(name)
            field.isAccessible = true
            field.getInt(null)
        } catch (e: Throwable) {
            when (name) {
                "FLAG_ALWAYS_SHOW_TICKER" -> FLAG_ALWAYS_SHOW_TICKER_FALLBACK
                "FLAG_ONLY_UPDATE_TICKER" -> FLAG_ONLY_UPDATE_TICKER_FALLBACK
                else -> 0
            }.also { fallback ->
                Log.w(TAG, "Flyme ticker flag not found: $name, fallback=$fallback")
            }
        }
    }
}

internal object PlaybackTickerState {
    data class Payload(
        val text: String,
        val translation: String?
    )

    @Volatile
    private var payload: Payload? = null
    private var refreshNotification: (() -> Unit)? = null

    @Synchronized
    fun setRefreshCallback(callback: (() -> Unit)?) {
        refreshNotification = callback
    }

    fun current(): Payload? = payload

    fun update(text: String?, translation: String?) {
        payload = text
            ?.takeIf { it.isNotBlank() }
            ?.let { Payload(it, translation?.takeIf { value -> value.isNotBlank() }) }
        refreshNotification?.invoke()
    }

    fun clear() {
        if (payload == null) return
        payload = null
        refreshNotification?.invoke()
    }
}
