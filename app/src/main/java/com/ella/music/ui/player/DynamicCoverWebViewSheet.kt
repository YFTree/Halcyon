package com.ella.music.ui.player

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.os.Environment
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.ella.music.R
import com.ella.music.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import top.yukonga.miuix.kmp.window.WindowBottomSheet
import java.io.File
import java.net.URLEncoder

@Composable
fun DynamicCoverWebViewSheet(
    show: Boolean,
    song: Song?,
    onDismissRequest: () -> Unit
) {
    if (!show || song == null) return

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    WindowBottomSheet(
        show = true,
        enableNestedScroll = false,
        title = context.getString(R.string.player_match_dynamic_cover),
        onDismissRequest = onDismissRequest
    ) {
        DynamicCoverWebViewContent(
            song = song,
            onDownloadComplete = { path ->
                scope.launch {
                    Toast.makeText(
                        context,
                        context.getString(R.string.player_dynamic_cover_downloaded),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            },
            onDownloadFailed = { error ->
                scope.launch {
                    Toast.makeText(
                        context,
                        context.getString(R.string.player_dynamic_cover_download_failed),
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.e("DynamicCoverWebView", "Download failed", error)
                }
            }
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun DynamicCoverWebViewContent(
    song: Song,
    onDownloadComplete: (String) -> Unit,
    onDownloadFailed: (Throwable) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(true) }

    val searchUrl = remember(song.artist, song.album) {
        val query = listOf(song.artist, song.album)
            .filter { it.isNotBlank() }
            .joinToString(" ")
        "https://covers.musichoarders.xyz/?q=${URLEncoder.encode(query, "UTF-8")}"
    }

    val downloadHelper = remember(song) {
        DynamicCoverDownloadHelper(context, song)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.6f)
            .navigationBarsPadding()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            mediaPlaybackRequiresUserGesture = false
                            cacheMode = WebSettings.LOAD_DEFAULT
                            allowContentAccess = true
                            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                            userAgentString = settings.userAgentString.replace(
                                "wv",
                                ""
                            ).trim()
                        }

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                isLoading = newProgress < 100
                            }
                        }

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                isLoading = true
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                isLoading = false
                                // Inject JS to intercept video clicks and extract URLs
                                view?.evaluateJavascript(VIDEO_INTERCEPT_JS, null)
                            }

                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): Boolean {
                                return false // Let WebView handle all URLs
                            }
                        }

                        // Bridge for JS to call Kotlin
                        addJavascriptInterface(
                            object {
                                @JavascriptInterface
                                fun onVideoUrlDetected(videoUrl: String) {
                                    if (videoUrl.isBlank()) return
                                    scope.launch(Dispatchers.IO) {
                                        try {
                                            downloadHelper.downloadVideo(videoUrl)
                                            withContext(Dispatchers.Main) {
                                                onDownloadComplete(videoUrl)
                                            }
                                        } catch (e: Exception) {
                                            withContext(Dispatchers.Main) {
                                                onDownloadFailed(e)
                                            }
                                        }
                                    }
                                }
                            },
                            "AndroidBridge"
                        )

                        // Set download listener for direct download buttons
                        setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
                            if (url != null && mimeType?.contains("video") == true) {
                                scope.launch(Dispatchers.IO) {
                                    try {
                                        downloadHelper.downloadVideo(url)
                                        withContext(Dispatchers.Main) {
                                            onDownloadComplete(url)
                                        }
                                    } catch (e: Exception) {
                                        withContext(Dispatchers.Main) {
                                            onDownloadFailed(e)
                                        }
                                    }
                                }
                            }
                        }

                        loadUrl(searchUrl)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            if (isLoading) {
                BasicText(
                    text = "…",
                    modifier = Modifier
                        .wrapContentSize()
                        .align(Alignment.Center)
                )
            }
        }
    }
}

/**
 * Downloads a dynamic cover video to the appropriate directory.
 *
 * Priority:
 * 1. Song's parent folder as {albumName}.mp4
 * 2. Movies/Halcyon/DynamicCovers/Album/{albumName}.mp4
 */
private class DynamicCoverDownloadHelper(
    private val context: Context,
    private val song: Song
) {
    private val okHttpClient = OkHttpClient.Builder()
        .build()

    suspend fun downloadVideo(videoUrl: String) {
        val fileName = determineFileName()
        val targetFile = determineTargetFile(fileName)

        targetFile.parentFile?.mkdirs()

        val request = Request.Builder()
            .url(videoUrl)
            .build()

        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            throw Exception("HTTP ${response.code}: ${response.message}")
        }

        response.body?.byteStream()?.use { input ->
            targetFile.outputStream().use { output ->
                input.copyTo(output)
            }
        } ?: throw Exception("Empty response body")

        // Scan the file into MediaStore so it's discoverable
        scanFileIntoMediaStore(targetFile)
    }

    private fun determineFileName(): String {
        val albumName = song.album.ifBlank { "Unknown" }
        return "${albumName.toSafeFileName()}.mp4"
    }

    private fun determineTargetFile(fileName: String): File {
        // Try song's parent folder first
        val songFile = song.path
            .takeUnless { it.startsWith("http://") || it.startsWith("https://") }
            ?.let { File(it) }

        val songFolder = songFile?.parentFile
        if (songFolder != null && songFolder.exists() && songFolder.isDirectory) {
            val candidate = File(songFolder, fileName)
            // Check if we can write here (not on read-only storage)
            runCatching {
                if (!candidate.exists()) {
                    candidate.createNewFile()
                    candidate.delete()
                }
                return File(songFolder, fileName)
            }
        }

        // Fallback: Movies/Halcyon/DynamicCovers/Album/
        val publicDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
            "Halcyon/DynamicCovers/Album"
        )
        return File(publicDir, fileName)
    }

    private fun scanFileIntoMediaStore(file: File) {
        try {
            android.media.MediaScannerConnection.scanFile(
                context,
                arrayOf(file.absolutePath),
                arrayOf("video/mp4"),
                null
            )
        } catch (e: Exception) {
            Log.d("DynamicCoverDownload", "MediaStore scan failed", e)
        }
    }

    private fun String.toSafeFileName(): String = trim()
        .replace(Regex("""[:*?"<>|]"""), "_")
        .replace(Regex("""\s+"""), " ")
        .ifBlank { "Unknown" }
}

/** JavaScript injected into the page to detect video elements and intercept clicks. */
private const val VIDEO_INTERCEPT_JS = """
(function() {
    // Watch for click events on or near video elements
    document.addEventListener('click', function(e) {
        var target = e.target;
        // Walk up to find video or its container
        for (var i = 0; i < 5; i++) {
            if (!target) break;
            if (target.tagName === 'VIDEO') {
                var src = target.src || target.currentSrc;
                if (src && src.length > 0) {
                    e.preventDefault();
                    e.stopPropagation();
                    AndroidBridge.onVideoUrlDetected(src);
                    return;
                }
                // Check child source elements
                var sources = target.querySelectorAll('source');
                for (var j = 0; j < sources.length; j++) {
                    var s = sources[j].src;
                    if (s && s.length > 0) {
                        e.preventDefault();
                        e.stopPropagation();
                        AndroidBridge.onVideoUrlDetected(s);
                        return;
                    }
                }
            }
            // Check for download links with video URLs
            if (target.tagName === 'A') {
                var href = target.href || '';
                if (href.match(/\.mp4/i)) {
                    e.preventDefault();
                    e.stopPropagation();
                    AndroidBridge.onVideoUrlDetected(href);
                    return;
                }
            }
            target = target.parentElement;
        }
    }, true);

    // Also intercept XMLHttpRequests and fetch for blob URLs
    var origCreateObjectURL = URL.createObjectURL;
    URL.createObjectURL = function(blob) {
        var url = origCreateObjectURL.call(URL, blob);
        if (blob && blob.type && blob.type.indexOf('video') >= 0) {
            var reader = new FileReader();
            reader.onloadend = function() {
                AndroidBridge.onVideoUrlDetected(reader.result);
            };
            reader.readAsDataURL(blob);
        }
        return url;
    };
})();
"""
