package com.ella.music.ui.player

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.util.Log
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.ella.music.data.SettingsManager
import com.ella.music.data.model.Song
import java.net.URL
import kotlin.math.abs
import kotlin.math.max

@Composable
internal fun ImmersiveCoverBackground(
    palette: PlayerPalette,
    flowEffectMode: Int,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            palette.top.copy(alpha = 0.64f),
                            palette.middle.copy(alpha = 0.58f),
                            palette.bottom.copy(alpha = 0.72f)
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            palette.accent.copy(alpha = 0.20f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.18f)
                        ),
                        start = Offset.Zero,
                        end = Offset.Infinite
                    )
                )
        )
    }
}

@Composable
internal fun PlayerFlowBackground(
    palette: PlayerPalette,
    flowEffectMode: Int,
    animate: Boolean = true,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(animate) {
        Log.d("PlayerScreenPerf", "flow background ${if (animate) "animated" else "static"}")
    }
    val sweepDrift = if (animate) {
        val transition = rememberInfiniteTransition(label = "player_flow_background")
        val value by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 46_000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "player_flow_background_drift"
        )
        value
    } else {
        0f
    }

    Canvas(modifier = modifier.background(palette.middle)) {
        val w = size.width
        val h = size.height
        val baseTop = palette.top.boosted().lighten(0.18f)
        val baseMid = palette.middle.boosted().lighten(0.12f)
        val baseBottom = palette.bottom.boosted().lighten(0.08f)

        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    baseTop.copy(alpha = 0.96f),
                    baseMid.copy(alpha = 0.98f),
                    baseBottom.copy(alpha = 1f)
                )
            )
        )

        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.10f),
                    palette.accent.lighten(0.20f).copy(alpha = 0.08f),
                    Color.Transparent
                ),
                start = Offset(0f, 0f),
                end = Offset(w, h * 0.72f)
            )
        )
        drawRect(
            brush = Brush.verticalGradient(
                colorStops = arrayOf(
                    0.0f to Color.White.copy(alpha = 0.06f),
                    0.48f to Color.Transparent,
                    1.0f to Color.Black.copy(alpha = 0.28f)
                )
            )
        )

        val sweepStart = Offset((-0.36f + sweepDrift * 1.72f) * w, -0.08f * h)
        val sweepEnd = Offset((0.12f + sweepDrift * 1.72f) * w, 1.08f * h)
        drawRect(
            brush = Brush.linearGradient(
                colorStops = arrayOf(
                    0.0f to Color.Transparent,
                    0.42f to Color.Transparent,
                    0.50f to Color.White.copy(alpha = 0.08f),
                    0.58f to Color.Transparent,
                    1.0f to Color.Transparent
                ),
                start = sweepStart,
                end = sweepEnd
            )
        )
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.08f),
                    Color.Transparent
                )
            )
        )
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.Black.copy(alpha = 0.24f)
                )
            )
        )
    }
}

internal fun playerContentSurfaceBrush(
    palette: PlayerPalette,
    flowEffectMode: Int
): Brush {
    return Brush.verticalGradient(
        colorStops = arrayOf(
            0.0f to palette.middle.copy(alpha = 0.70f),
            0.16f to palette.middle.copy(alpha = 0.82f),
            1.0f to palette.middle.copy(alpha = 0.90f)
        )
    )
}

@Composable
internal fun NonImmersiveAlbumFlowBackground(
    song: Song?,
    embeddedCover: Bitmap?,
    palette: PlayerPalette,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "non_immersive_album_flow")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 28_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "non_immersive_album_flow_drift"
    )
    val pulse = if (isPlaying) {
        0.5f + 0.5f * kotlin.math.sin(drift * kotlin.math.PI.toFloat() * 2f)
    } else {
        0.28f
    }

    Box(modifier = modifier.background(palette.middle)) {
        PlayerBlurBackground(
            song = song,
            embeddedCover = embeddedCover,
            palette = palette,
            motion = drift,
            isPlaying = isPlaying,
            modifier = Modifier.fillMaxSize()
        )
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val t = drift * kotlin.math.PI.toFloat() * 2f
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        palette.top.copy(alpha = 0.78f),
                        palette.middle.copy(alpha = 0.70f),
                        palette.bottom.copy(alpha = 0.88f)
                    )
                )
            )
            val glows = listOf(
                Triple(
                    Offset((0.12f + 0.10f * kotlin.math.sin(t * 0.72f)) * w, (0.18f + 0.08f * kotlin.math.cos(t)) * h),
                    palette.accent.lighten(0.18f).copy(alpha = 0.24f + pulse * 0.08f),
                    0.56f
                ),
                Triple(
                    Offset((0.88f + 0.08f * kotlin.math.cos(t * 0.62f)) * w, (0.28f + 0.10f * kotlin.math.sin(t * 0.9f)) * h),
                    palette.top.lighten(0.30f).copy(alpha = 0.22f),
                    0.48f
                ),
                Triple(
                    Offset((0.50f + 0.12f * kotlin.math.sin(t * 0.45f)) * w, (0.82f + 0.05f * kotlin.math.cos(t * 0.8f)) * h),
                    Color.White.copy(alpha = 0.08f + pulse * 0.04f),
                    0.42f
                )
            )
            glows.forEach { (center, color, radiusScale) ->
                val radius = max(w, h) * radiusScale
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(color, Color.Transparent),
                        center = center,
                        radius = radius
                    ),
                    radius = radius,
                    center = center
                )
            }
            val sweepStart = Offset((-0.42f + drift * 1.72f) * w, -0.12f * h)
            val sweepEnd = Offset((0.16f + drift * 1.72f) * w, 1.08f * h)
            drawRect(
                brush = Brush.linearGradient(
                    colorStops = arrayOf(
                        0.0f to Color.Transparent,
                        0.42f to Color.Transparent,
                        0.50f to Color.White.copy(alpha = 0.11f),
                        0.58f to Color.Transparent,
                        1.0f to Color.Transparent
                    ),
                    start = sweepStart,
                    end = sweepEnd
                )
            )
            drawRect(
                brush = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.0f to Color.Black.copy(alpha = 0.06f),
                        0.42f to Color.Black.copy(alpha = 0.10f),
                        0.74f to Color.Black.copy(alpha = 0.22f),
                        1.0f to Color.Black.copy(alpha = 0.48f)
                    )
                )
            )
        }
    }
}

@Composable
internal fun PlayerCustomBackground(
    uri: String,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.background(Color.Black)) {
        PlayerCoverImage(
            model = Uri.parse(uri),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            sizePx = 1400
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.26f))
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.08f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.48f)
                        )
                    )
                )
        )
    }
}

internal fun loadPaletteCoverBitmap(context: Context, song: Song): Bitmap? {
    return runCatching {
        when {
            song.coverUrl.isNotBlank() -> URL(song.coverUrl).openStream().use { input ->
                BitmapFactory.decodeStream(input)
            }
            song.albumId > 0L -> context.contentResolver
                .openInputStream(Uri.parse("content://media/external/audio/albumart/${song.albumId}"))
                ?.use { input -> BitmapFactory.decodeStream(input) }
            else -> null
        }?.scaledForPalette()
    }.getOrNull()
}

private fun Bitmap.scaledForPalette(): Bitmap {
    val longest = max(width, height)
    if (longest <= 480) return this
    val scale = 480f / longest.toFloat()
    return Bitmap.createScaledBitmap(
        this,
        (width * scale).toInt().coerceAtLeast(1),
        (height * scale).toInt().coerceAtLeast(1),
        true
    )
}

@Composable
internal fun FluidLyricBackground(
    palette: PlayerPalette,
    positionMs: Long,
    isPlaying: Boolean,
    flowEffectMode: Int = SettingsManager.PLAYER_FLOW_EFFECT_DARK,
    animate: Boolean = false,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(animate) {
        Log.d("PlayerScreenPerf", "flow background ${if (animate) "animated" else "static"}")
    }
    val drift = if (animate) {
        val transition = rememberInfiniteTransition(label = "fluid_lyric_background")
        val value by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 18_000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "fluid_lyric_background_drift"
        )
        value
    } else {
        0.36f
    }
    val pulse = if (animate && isPlaying) {
        0.5f + 0.5f * kotlin.math.sin(positionMs / 900.0).toFloat()
    } else {
        0.28f
    }

    Canvas(modifier = modifier.background(palette.middle)) {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    palette.top.copy(alpha = 0.98f),
                    palette.middle.copy(alpha = 0.98f),
                    palette.bottom.copy(alpha = 1f)
                )
            )
        )
        val w = size.width
        val h = size.height
        val t = drift * kotlin.math.PI.toFloat() * 2f
        val centers = listOf(
            Offset((0.18f + 0.04f * kotlin.math.sin(t)) * w, (0.24f + 0.08f * kotlin.math.cos(t * 0.7f)) * h),
            Offset((0.82f + 0.05f * kotlin.math.cos(t * 0.8f)) * w, (0.20f + 0.06f * kotlin.math.sin(t)) * h),
            Offset((0.48f + 0.08f * kotlin.math.sin(t * 0.55f)) * w, (0.62f + 0.05f * kotlin.math.cos(t * 0.9f)) * h),
            Offset((0.72f + 0.06f * kotlin.math.sin(t * 0.95f)) * w, (0.86f + 0.04f * kotlin.math.cos(t * 0.6f)) * h)
        )
        val colors = listOf(
            palette.accent.copy(alpha = 0.22f + pulse * 0.05f),
            Color.White.copy(alpha = 0.10f),
            palette.top.copy(alpha = 0.20f),
            Color.Black.copy(alpha = 0.20f)
        )
        centers.forEachIndexed { index, center ->
            val radius = minOf(w, h) * (0.34f + index * 0.055f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(colors[index], Color.Transparent),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )
        }
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Black.copy(alpha = 0.10f),
                    Color.Transparent,
                    Color.Black.copy(alpha = 0.42f)
                )
            )
        )
    }
}

@Composable
internal fun PlayerBlurBackground(
    song: Song?,
    embeddedCover: Bitmap?,
    palette: PlayerPalette,
    motion: Float,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val uri = if ((song?.albumId ?: 0L) > 0) {
        Uri.parse("content://media/external/audio/albumart/${song?.albumId}")
    } else null
    val coverModel = embeddedCover ?: song?.coverUrl?.takeIf { it.isNotBlank() } ?: uri
    val movingScale = 2.90f
    val movingOffset = 0f
    LaunchedEffect(coverModel, isPlaying) {
        Log.d("PlayerScreenPerf", "blur background static")
    }

    Box(modifier = modifier.background(palette.middle)) {
        if (coverModel != null) {
            PlayerCoverImage(
                model = coverModel,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = movingScale
                        scaleY = movingScale
                        translationX = movingOffset
                        translationY = -movingOffset * 0.65f
                        alpha = 0.78f
                    }
                    .blur(48.dp),
                contentScale = ContentScale.Crop,
                sizePx = 1200
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            palette.accent.copy(alpha = 0.28f),
                            palette.top.copy(alpha = 0.42f),
                            Color.Black.copy(alpha = 0.34f)
                        ),
                        start = Offset.Zero,
                        end = Offset.Infinite
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.06f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.32f)
                        )
                    )
                )
        )
    }
}

internal data class PlayerPalette(
    val top: Color,
    val middle: Color,
    val bottom: Color,
    val accent: Color
) {
    companion object {
        val Default = PlayerPalette(
            top = Color(0xFF171717),
            middle = Color(0xFF0B0B0D),
            bottom = Color.Black,
            accent = Color(0xFF2F7DFF)
        )

        fun from(bitmap: Bitmap?): PlayerPalette {
            if (bitmap == null || bitmap.width <= 0 || bitmap.height <= 0) return Default
            val sampleStep = (minOf(bitmap.width, bitmap.height) / 36).coerceAtLeast(1)
            var red = 0L
            var green = 0L
            var blue = 0L
            var count = 0L

            var y = 0
            while (y < bitmap.height) {
                var x = 0
                while (x < bitmap.width) {
                    val pixel = bitmap.getPixel(x, y)
                    val alpha = AndroidColor.alpha(pixel)
                    if (alpha > 24) {
                        red += AndroidColor.red(pixel)
                        green += AndroidColor.green(pixel)
                        blue += AndroidColor.blue(pixel)
                        count++
                    }
                    x += sampleStep
                }
                y += sampleStep
            }
            if (count == 0L) return Default

            val r = (red / count).toInt()
            val g = (green / count).toInt()
            val b = (blue / count).toInt()
            val accent = Color(r, g, b).boosted()
            return PlayerPalette(
                top = accent.darken(0.58f),
                middle = accent.darken(0.78f),
                bottom = Color.Black,
                accent = accent
            )
        }

        fun fromLyricBackground(bitmap: Bitmap?): PlayerPalette {
            if (bitmap == null || bitmap.width <= 0 || bitmap.height <= 0) return Default
            val sampleStep = (minOf(bitmap.width, bitmap.height) / 36).coerceAtLeast(1)
            val buckets = linkedMapOf<Int, LongArray>()
            val fallback = LongArray(4)
            val hsv = FloatArray(3)

            var y = 0
            while (y < bitmap.height) {
                var x = 0
                while (x < bitmap.width) {
                    val pixel = bitmap.getPixel(x, y)
                    val alpha = AndroidColor.alpha(pixel)
                    if (alpha > 24) {
                        val r = AndroidColor.red(pixel)
                        val g = AndroidColor.green(pixel)
                        val b = AndroidColor.blue(pixel)
                        AndroidColor.RGBToHSV(r, g, b, hsv)
                        val saturation = hsv[1]
                        val value = hsv[2]

                        fallback[0]++
                        fallback[1] += r.toLong()
                        fallback[2] += g.toLong()
                        fallback[3] += b.toLong()

                        if (value > 0.08f && !(value > 0.94f && saturation < 0.20f)) {
                            val key = ((r ushr 4) shl 8) or ((g ushr 4) shl 4) or (b ushr 4)
                            val bucket = buckets.getOrPut(key) { LongArray(4) }
                            bucket[0]++
                            bucket[1] += r.toLong()
                            bucket[2] += g.toLong()
                            bucket[3] += b.toLong()
                        }
                    }
                    x += sampleStep
                }
                y += sampleStep
            }
            if (fallback[0] == 0L) return Default

            val best = buckets.values.maxByOrNull { bucket ->
                val count = bucket[0].coerceAtLeast(1L)
                val r = (bucket[1] / count).toInt()
                val g = (bucket[2] / count).toInt()
                val b = (bucket[3] / count).toInt()
                AndroidColor.RGBToHSV(r, g, b, hsv)
                val luminance = (0.2126f * r + 0.7152f * g + 0.0722f * b) / 255f
                val balance = 1f - abs(luminance - 0.50f).coerceIn(0f, 0.50f) * 1.25f
                count.toFloat() * (0.55f + hsv[1] * 1.65f) * (0.75f + balance * 0.55f)
            } ?: fallback

            val count = best[0].coerceAtLeast(1L)
            val r = (best[1] / count).toInt()
            val g = (best[2] / count).toInt()
            val b = (best[3] / count).toInt()
            val accent = Color(r, g, b).toPlayerAccent()
            return PlayerPalette(
                top = accent.darken(0.42f),
                middle = accent.darken(0.68f),
                bottom = accent.darken(0.88f),
                accent = accent
            )
        }
    }
}

private fun Color.darken(amount: Float): Color = Color(
    red = red * (1f - amount),
    green = green * (1f - amount),
    blue = blue * (1f - amount),
    alpha = 1f
)

private fun Color.lighten(amount: Float): Color = Color(
    red = red + (1f - red) * amount,
    green = green + (1f - green) * amount,
    blue = blue + (1f - blue) * amount,
    alpha = alpha
)

private fun Color.boosted(): Color {
    val max = maxOf(red, green, blue).coerceAtLeast(0.01f)
    val scale = (0.86f / max).coerceIn(1f, 2.4f)
    return Color(
        red = (red * scale).coerceAtMost(1f),
        green = (green * scale).coerceAtMost(1f),
        blue = (blue * scale).coerceAtMost(1f),
        alpha = 1f
    )
}

private fun Color.toPlayerAccent(): Color {
    val r = (red * 255f).toInt().coerceIn(0, 255)
    val g = (green * 255f).toInt().coerceIn(0, 255)
    val b = (blue * 255f).toInt().coerceIn(0, 255)
    val hsv = FloatArray(3)
    AndroidColor.RGBToHSV(r, g, b, hsv)
    if (hsv[1] < 0.12f) return Color(0xFF4D72B8)
    hsv[1] = hsv[1].coerceAtLeast(0.34f)
    hsv[2] = hsv[2].coerceIn(0.46f, 0.88f)
    return Color(AndroidColor.HSVToColor(hsv))
}
