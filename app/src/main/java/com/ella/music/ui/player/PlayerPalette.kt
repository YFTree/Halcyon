package com.ella.music.ui.player

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
import android.net.Uri
import androidx.compose.ui.graphics.Color
import com.ella.music.data.model.Song
import java.net.URL
import kotlin.math.abs
import kotlin.math.max

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

internal fun Color.darken(amount: Float): Color = Color(
    red = red * (1f - amount),
    green = green * (1f - amount),
    blue = blue * (1f - amount),
    alpha = 1f
)

internal fun Color.lighten(amount: Float): Color = Color(
    red = red + (1f - red) * amount,
    green = green + (1f - green) * amount,
    blue = blue + (1f - blue) * amount,
    alpha = alpha
)

internal fun Color.boosted(): Color {
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
