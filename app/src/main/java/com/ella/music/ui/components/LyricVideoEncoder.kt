package com.ella.music.ui.components

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.view.Surface
import android.widget.Toast
import androidx.core.content.FileProvider
import com.ella.music.R
import com.ella.music.data.model.LyricLine
import com.ella.music.data.model.Song
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext
import java.io.File
import java.nio.ByteBuffer

internal data class LyricVideoProgress(
    val current: Int,
    val total: Int
) {
    val fraction: Float get() = if (total > 0) current.toFloat() / total else 0f
}

internal suspend fun generateLyricVideo(
    context: Context,
    song: Song?,
    lines: List<LyricLine>,
    cover: Bitmap?,
    includeTranslation: Boolean,
    typeface: android.graphics.Typeface? = null,
    onProgress: (LyricVideoProgress) -> Unit
): Uri? = withContext(Dispatchers.Default) {
    val renderer = LyricVideoRenderer(
        cover = cover,
        lines = lines,
        includeTranslation = includeTranslation,
        typeface = typeface
    )
    val totalFrames = renderer.totalFrames()
    if (totalFrames <= 0) {
        renderer.recycle()
        return@withContext null
    }

    val dir = File(context.cacheDir, "lyric_video_share").apply {
        deleteRecursively()
        mkdirs()
    }
    val videoOnlyFile = File(dir, "video_only_${System.currentTimeMillis()}.mp4")
    val outputFile = File(dir, "halcyon_lyric_${System.currentTimeMillis()}.mp4")

    try {
        encodeVideoTrack(renderer, totalFrames, videoOnlyFile, onProgress)
        renderer.recycle()

        val audioMuxed = muxVideoAndAudio(
            context = context,
            song = song,
            lines = lines,
            videoFile = videoOnlyFile,
            outputFile = outputFile
        )

        val shareFile = if (audioMuxed) {
            videoOnlyFile.delete()
            outputFile
        } else {
            outputFile.delete()
            videoOnlyFile
        }
        if (!shareFile.exists()) return@withContext null

        onProgress(LyricVideoProgress(totalFrames, totalFrames))
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", shareFile)
    } catch (e: CancellationException) {
        renderer.recycle()
        videoOnlyFile.delete()
        outputFile.delete()
        throw e
    } catch (e: Exception) {
        renderer.recycle()
        videoOnlyFile.delete()
        outputFile.delete()
        null
    }
}

private suspend fun encodeVideoTrack(
    renderer: LyricVideoRenderer,
    totalFrames: Int,
    outputFile: File,
    onProgress: (LyricVideoProgress) -> Unit
) {
    val videoFormat = MediaFormat.createVideoFormat(
        MediaFormat.MIMETYPE_VIDEO_AVC,
        LyricVideoRenderer.VIDEO_SIZE,
        LyricVideoRenderer.VIDEO_SIZE
    ).apply {
        setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
        setInteger(MediaFormat.KEY_BIT_RATE, 8_000_000)
        setInteger(MediaFormat.KEY_FRAME_RATE, LyricVideoRenderer.FPS)
        setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
    }

    val encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
    encoder.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
    val inputSurface = encoder.createInputSurface()
    encoder.start()

    val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
    var videoTrackIndex = -1
    var muxerStarted = false
    val bufferInfo = MediaCodec.BufferInfo()

    try {
        for (frameIndex in 0 until totalFrames) {
            coroutineContext.ensureActive()
            onProgress(LyricVideoProgress(frameIndex, totalFrames))

            val canvas = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                inputSurface.lockHardwareCanvas()
            } else {
                inputSurface.lockCanvas(null)
            }
            try {
                renderer.drawFrame(canvas, frameIndex)
            } finally {
                inputSurface.unlockCanvasAndPost(canvas)
            }

            drainEncoder(encoder, muxer, bufferInfo, false) { format ->
                videoTrackIndex = muxer.addTrack(format)
                muxer.start()
                muxerStarted = true
                videoTrackIndex
            }.let { track -> if (track >= 0) videoTrackIndex = track }
        }

        encoder.signalEndOfInputStream()

        var eos = false
        while (!eos) {
            drainEncoder(encoder, muxer, bufferInfo, true) { format ->
                if (videoTrackIndex < 0) {
                    videoTrackIndex = muxer.addTrack(format)
                    muxer.start()
                    muxerStarted = true
                }
                videoTrackIndex
            }.let { track -> if (track >= 0) videoTrackIndex = track }
            eos = true
        }
    } finally {
        try { encoder.stop() } catch (_: Exception) {}
        try { encoder.release() } catch (_: Exception) {}
        try { inputSurface.release() } catch (_: Exception) {}
        if (muxerStarted) {
            try { muxer.stop() } catch (_: Exception) {}
        }
        try { muxer.release() } catch (_: Exception) {}
    }
}

private fun drainEncoder(
    encoder: MediaCodec,
    muxer: MediaMuxer,
    bufferInfo: MediaCodec.BufferInfo,
    endOfStream: Boolean,
    onFormatAvailable: (MediaFormat) -> Int
): Int {
    val timeoutUs = if (endOfStream) 10_000L else 0L
    var trackIndex = -1

    while (true) {
        val index = encoder.dequeueOutputBuffer(bufferInfo, timeoutUs)
        when {
            index == MediaCodec.INFO_TRY_AGAIN_LATER -> break
            index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                trackIndex = onFormatAvailable(encoder.outputFormat)
            }
            index >= 0 -> {
                val buf = encoder.getOutputBuffer(index)
                if (buf != null &&
                    bufferInfo.size > 0 &&
                    bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0 &&
                    trackIndex >= 0
                ) {
                    muxer.writeSampleData(trackIndex, buf, bufferInfo)
                }
                encoder.releaseOutputBuffer(index, false)
                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) break
            }
        }
    }
    return trackIndex
}

private fun muxVideoAndAudio(
    context: Context,
    song: Song?,
    lines: List<LyricLine>,
    videoFile: File,
    outputFile: File
): Boolean {
    if (song == null || lines.isEmpty()) return false
    val path = song.path
    if (path.isBlank()) return false

    val globalStartMs = lines.first().timeMs
    val globalEndMs = lines.last().let { line ->
        val wordEnd = line.words.maxOfOrNull { it.endMs }
        wordEnd ?: line.endMs ?: (line.timeMs + 4000L)
    } + LyricVideoRenderer.DISSOLVE_MS + 300L

    val videoExtractor = MediaExtractor()
    val audioExtractor = MediaExtractor()
    var muxer: MediaMuxer? = null

    return try {
        videoExtractor.setDataSource(videoFile.absolutePath)
        var videoTrackSrc = -1
        var videoFormat: MediaFormat? = null
        for (i in 0 until videoExtractor.trackCount) {
            val fmt = videoExtractor.getTrackFormat(i)
            if (fmt.getString(MediaFormat.KEY_MIME).orEmpty().startsWith("video/")) {
                videoTrackSrc = i
                videoFormat = fmt
                break
            }
        }
        if (videoTrackSrc < 0 || videoFormat == null) return false

        val audioUri = when {
            path.startsWith("content://", ignoreCase = true) -> Uri.parse(path)
            path.startsWith("/") -> Uri.parse("file://$path")
            else -> Uri.parse(path)
        }
        audioExtractor.setDataSource(context, audioUri, null)

        var audioTrackSrc = -1
        var audioFormat: MediaFormat? = null
        for (i in 0 until audioExtractor.trackCount) {
            val fmt = audioExtractor.getTrackFormat(i)
            if (fmt.getString(MediaFormat.KEY_MIME).orEmpty().startsWith("audio/")) {
                audioTrackSrc = i
                audioFormat = fmt
                break
            }
        }
        if (audioTrackSrc < 0 || audioFormat == null) return false

        muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        val muxerVideoTrack = muxer.addTrack(videoFormat)
        val muxerAudioTrack = muxer.addTrack(audioFormat)
        muxer.start()

        videoExtractor.selectTrack(videoTrackSrc)
        val videoBuf = ByteBuffer.allocate(2 * 1024 * 1024)
        val videoInfo = MediaCodec.BufferInfo()
        while (true) {
            val size = videoExtractor.readSampleData(videoBuf, 0)
            if (size < 0) break
            videoInfo.set(0, size, videoExtractor.sampleTime, videoExtractor.sampleFlags)
            muxer.writeSampleData(muxerVideoTrack, videoBuf, videoInfo)
            videoExtractor.advance()
        }

        audioExtractor.selectTrack(audioTrackSrc)
        audioExtractor.seekTo(globalStartMs * 1000L, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
        val audioBuf = ByteBuffer.allocate(1024 * 1024)
        val audioInfo = MediaCodec.BufferInfo()
        val endUs = globalEndMs * 1000L
        while (true) {
            val size = audioExtractor.readSampleData(audioBuf, 0)
            if (size < 0) break
            val sampleTimeUs = audioExtractor.sampleTime
            if (sampleTimeUs > endUs) break
            val adjustedTimeUs = sampleTimeUs - (globalStartMs * 1000L)
            if (adjustedTimeUs >= 0) {
                audioInfo.set(0, size, adjustedTimeUs, audioExtractor.sampleFlags)
                muxer.writeSampleData(muxerAudioTrack, audioBuf, audioInfo)
            }
            audioExtractor.advance()
        }

        muxer.stop()
        muxer.release()
        muxer = null
        true
    } catch (_: Exception) {
        outputFile.delete()
        false
    } finally {
        try { videoExtractor.release() } catch (_: Exception) {}
        try { audioExtractor.release() } catch (_: Exception) {}
        try { muxer?.stop() } catch (_: Exception) {}
        try { muxer?.release() } catch (_: Exception) {}
    }
}

internal fun shareLyricVideoFile(context: Context, uri: Uri) {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "video/mp4"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        clipData = ClipData.newUri(
            context.contentResolver,
            "${context.getString(R.string.app_name)} Lyric Video",
            uri
        )
    }
    context.startActivity(
        Intent.createChooser(shareIntent, context.getString(R.string.lyric_video_share_chooser_title))
    )
}
