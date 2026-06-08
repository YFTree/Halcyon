package com.ella.music

import android.app.Application
import com.ella.music.data.AppLogcatCollector
import com.ella.music.data.AppLogStore
import com.ella.music.data.SettingsManager
import com.ella.music.data.webdav.WebDavClient
import com.ella.music.mcp.McpServerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class EllaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        WebDavClient.initContext(this)
        AppLogStore.install(this)
        AppLogcatCollector.start(this)
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            AppLogStore.crash(this, thread.name, throwable)
            previousHandler?.uncaughtException(thread, throwable)
        }
        AppLogStore.info(this, "EllaApp", "Application started")

        // Auto-start MCP server if previously enabled
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            val settingsManager = SettingsManager.getInstance(this@EllaApp)
            if (settingsManager.mcpServerEnabled.first()) {
                McpServerService.start(this@EllaApp)
            }
        }
    }
}
