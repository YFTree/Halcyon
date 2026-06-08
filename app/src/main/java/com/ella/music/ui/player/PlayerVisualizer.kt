package com.ella.music.ui.player

import android.media.audiofx.Visualizer
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

@Composable
internal fun AudioVisualizer(
    enabled: Boolean,
    audioSessionId: Int,
    isPlaying: Boolean,
    positionMs: Long,
    accent: Color,
    modifier: Modifier = Modifier
) {
    if (!enabled) return
    var fftData by remember { mutableStateOf<ByteArray?>(null) }
    var levels by remember { mutableStateOf<List<Float>>(emptyList()) }
    var visualizerFailed by remember { mutableStateOf(false) }
    val playingState by rememberUpdatedState(isPlaying)

    LaunchedEffect(enabled, audioSessionId) {
        fftData = null
        levels = emptyList()
        visualizerFailed = false
        if (!enabled || audioSessionId <= 0) return@LaunchedEffect
        val visualizer = runCatching {
            Visualizer(audioSessionId).apply {
                captureSize = Visualizer.getCaptureSizeRange()[1].coerceAtMost(512)
                scalingMode = Visualizer.SCALING_MODE_NORMALIZED
                this.enabled = true
            }
        }.onFailure { visualizerFailed = true }.getOrNull() ?: return@LaunchedEffect

        Log.d("PlayerScreenPerf", "visualizer start")
        val buffer = ByteArray(visualizer.captureSize)
        try {
            while (isActive) {
                if (playingState) {
                    if (visualizer.getFft(buffer) == Visualizer.SUCCESS) {
                        fftData = buffer.copyOf()
                    }
                } else {
                    fftData = null
                    delay(120L)
                    continue
                }
                delay(66L)
            }
        } finally {
            Log.d("PlayerScreenPerf", "visualizer stop")
            runCatching { visualizer.enabled = false }
            visualizer.release()
        }
    }

    LaunchedEffect(fftData, enabled, visualizerFailed) {
        val fft = fftData
        levels = if (enabled && !visualizerFailed && fft != null && fft.size > 8) {
            mapFftToLogBars(fft, levels, barCount = 84)
        } else {
            emptyList()
        }
    }

    Canvas(modifier = modifier.graphicsLayer { alpha = if (isPlaying) 1f else 0.42f }) {
        val barCount = 84
        val horizontalPadding = size.width * 0.065f
        val usableWidth = (size.width - horizontalPadding * 2f).coerceAtLeast(1f)
        val gap = usableWidth / barCount
        val barWidth = (gap * 0.34f).coerceIn(2.dp.toPx(), 3.8.dp.toPx())
        val minHeight = 2.5.dp.toPx()
        val visualHeight = min(size.height * 0.34f, 18.dp.toPx())
        val centerY = size.height - 11.dp.toPx()
        val glowWidth = (barWidth * 2.7f).coerceAtLeast(5.dp.toPx())
        val halfCount = (barCount - 1) / 2f
        for (index in 0 until barCount) {
            val x = horizontalPadding + gap * index + gap / 2f
            val normalized = (levels.getOrNull(index) ?: 0.06f).coerceIn(0.04f, 1f)
            val distance = abs(index - halfCount) / halfCount
            val edgeFade = (1f - distance * distance * 0.48f).coerceIn(0.48f, 1f)
            val height = (minHeight + visualHeight * normalized).coerceAtMost(visualHeight + minHeight)
            val top = centerY - height / 2f
            val alpha = (0.18f + normalized * 0.54f) * edgeFade

            drawRoundRect(
                color = accent.copy(alpha = alpha * 0.18f),
                topLeft = Offset(x - glowWidth / 2f, top - 1.5.dp.toPx()),
                size = Size(glowWidth, height + 3.dp.toPx()),
                cornerRadius = CornerRadius(glowWidth, glowWidth)
            )
            drawRoundRect(
                color = accent.copy(alpha = alpha),
                topLeft = Offset(x - barWidth / 2f, top),
                size = Size(barWidth, height),
                cornerRadius = CornerRadius(barWidth, barWidth)
            )
        }
    }
}

private fun mapFftToLogBars(
    fft: ByteArray,
    previous: List<Float>,
    barCount: Int
): List<Float> {
    val binCount = fft.size / 2
    if (binCount <= 2) return List(barCount) { 0.06f }

    return List(barCount) { index ->
        val startRatio = index.toFloat() / barCount
        val endRatio = (index + 1f) / barCount
        val startBin = (1f + (binCount - 2) * startRatio * startRatio)
            .toInt()
            .coerceIn(1, binCount - 1)
        val endBin = (1f + (binCount - 2) * endRatio * endRatio)
            .toInt()
            .coerceIn(startBin, binCount - 1)

        var peak = 0f
        for (bin in startBin..endBin) {
            val real = fft[bin * 2].toFloat()
            val imag = fft[bin * 2 + 1].toFloat()
            peak = max(peak, sqrt(real * real + imag * imag))
        }

        val db = 20f * (ln(peak.coerceAtLeast(1f)) / ln(10f))
        val normalized = ((db - 16f) / 36f).coerceIn(0f, 1f)
        val shaped = 0.06f + sqrt(normalized) * 0.94f
        val old = previous.getOrNull(index) ?: 0.06f
        if (shaped > old) {
            old * 0.42f + shaped * 0.58f
        } else {
            old * 0.84f + shaped * 0.16f
        }
    }
}
