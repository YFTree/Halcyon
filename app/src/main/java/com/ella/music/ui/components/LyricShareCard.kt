package com.ella.music.ui.components

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.widget.Toast
import androidx.core.content.FileProvider
import com.ella.music.data.model.LyricLine
import com.ella.music.data.model.Song
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

fun shareLyricCard(
    context: Context,
    song: Song?,
    line: LyricLine,
    cover: Bitmap?,
    backgroundColors: List<Int>,
    customInfo: String = ""
) {
    shareLyricCard(
        context = context,
        song = song,
        lines = listOf(line),
        cover = cover,
        backgroundColors = backgroundColors,
        customInfo = customInfo
    )
}

fun shareLyricCard(
    context: Context,
    song: Song?,
    lines: List<LyricLine>,
    cover: Bitmap?,
    backgroundColors: List<Int>,
    customInfo: String = ""
) {
    runCatching {
        val shareLines = lines.filter { it.sharePrimaryText().isNotBlank() }.ifEmpty {
            lines.take(1)
        }
        val bitmap = createLyricShareCard(song, shareLines, cover, backgroundColors, customInfo)
        val uri = writeLyricShareCard(context, bitmap)
        bitmap.recycle()
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra("com.mocharealm.compound.EXTRA_SOURCE_NAME", "Ella Music")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            clipData = ClipData.newUri(context.contentResolver, "Ella Music Lyric Card", uri)
        }
        context.startActivity(Intent.createChooser(shareIntent, "分享歌词"))
    }.onFailure {
        Toast.makeText(context, "分享歌词失败", Toast.LENGTH_SHORT).show()
    }
}

private fun createLyricShareCard(
    song: Song?,
    lines: List<LyricLine>,
    cover: Bitmap?,
    backgroundColors: List<Int>,
    customInfo: String
): Bitmap {
    val width = 1080
    val height = 1350
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val palette = cover.extractSharePalette(backgroundColors)
    drawShareBackground(canvas, width, height, cover, palette)

    val edge = 86f
    val coverSize = 152f
    val coverRect = RectF(edge, edge, edge + coverSize, edge + coverSize)
    cover?.let { drawRoundedCover(canvas, it, coverRect, 28f) }

    val headerLeft = coverRect.right + 28f
    val headerWidth = width - headerLeft - edge
    val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 42f
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        setShadowLayer(14f, 0f, 5f, Color.argb(86, 0, 0, 0))
    }
    val artistPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(186, 255, 255, 255)
        textSize = 30f
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        setShadowLayer(12f, 0f, 4f, Color.argb(72, 0, 0, 0))
    }
    drawSingleLine(
        canvas = canvas,
        text = song?.title?.takeIf { it.isNotBlank() } ?: "未知歌曲",
        paint = titlePaint,
        x = headerLeft,
        y = coverRect.top + 60f,
        maxWidth = headerWidth
    )
    drawSingleLine(
        canvas = canvas,
        text = song?.artist?.takeIf { it.isNotBlank() } ?: "未知艺术家",
        paint = artistPaint,
        x = headerLeft,
        y = coverRect.top + 108f,
        maxWidth = headerWidth
    )

    val lyricBlocks = lines
        .mapNotNull { it.toShareLyricBlock() }
        .ifEmpty { listOf(ShareLyricBlock("♪", emptyList())) }
        .take(6)
    val primaryTexts = lyricBlocks.map { it.primary }
    val lyricPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = shareLyricTextSize(primaryTexts)
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        setShadowLayer(20f, 0f, 8f, Color.argb(96, 0, 0, 0))
    }
    val secondaryPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(188, 255, 255, 255)
        textSize = (lyricPaint.textSize * 0.48f).coerceIn(28f, 34f)
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        setShadowLayer(12f, 0f, 5f, Color.argb(72, 0, 0, 0))
    }
    val lyricTop = 344f
    val lyricWidth = (width - edge * 2).toInt()
    val footerTop = height - 170f
    var nextTop = lyricTop
    lyricBlocks.forEachIndexed { index, block ->
        if (nextTop >= footerTop) return@forEachIndexed
        val primaryLayout = buildLayout(block.primary, lyricPaint, lyricWidth, maxLines = 2)
        drawLayout(canvas, primaryLayout, edge, nextTop)
        nextTop += primaryLayout.height + 12f

        block.secondary.forEach { secondary ->
            if (nextTop >= footerTop) return@forEach
            val secondaryLayout = buildLayout(secondary, secondaryPaint, lyricWidth, maxLines = 2)
            drawLayout(canvas, secondaryLayout, edge, nextTop)
            nextTop += secondaryLayout.height + 8f
        }

        if (index != lyricBlocks.lastIndex) {
            nextTop += 22f
        }
    }

    val footerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(150, 255, 255, 255)
        textSize = 30f
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText(lyricShareFooter(customInfo), width / 2f, height - 86f, footerPaint)
    return bitmap
}

private fun drawShareBackground(
    canvas: Canvas,
    width: Int,
    height: Int,
    cover: Bitmap?,
    colors: List<Int>
) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true }
    if (cover != null) {
        val blurred = cover.centerCropScaled(54, 54).let {
            Bitmap.createScaledBitmap(it, width, height, true).also { _ -> it.recycle() }
        }
        canvas.drawBitmap(blurred, 0f, 0f, paint)
        blurred.recycle()
    }
    val fallbackColors = listOf(Color.rgb(82, 98, 82), Color.rgb(39, 64, 57), Color.rgb(18, 30, 34))
    val picked = colors.filter { Color.alpha(it) > 0 }.ifEmpty { fallbackColors }
    paint.shader = LinearGradient(
        0f,
        0f,
        width.toFloat(),
        height.toFloat(),
        intArrayOf(picked.first(), picked.getOrElse(1) { picked.first() }, picked.last()),
        floatArrayOf(0f, 0.58f, 1f),
        Shader.TileMode.CLAMP
    )
    paint.alpha = if (cover == null) 255 else 132
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
    paint.shader = null
    paint.color = Color.argb(if (cover == null) 54 else 24, 0, 0, 0)
    paint.alpha = 255
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
}

private fun drawRoundedCover(canvas: Canvas, cover: Bitmap, rect: RectF, radius: Float) {
    val path = Path().apply { addRoundRect(rect, radius, radius, Path.Direction.CW) }
    val save = canvas.save()
    canvas.clipPath(path)
    val cropped = cover.centerCropScaled(rect.width().toInt(), rect.height().toInt())
    canvas.drawBitmap(cropped, rect.left, rect.top, Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true })
    cropped.recycle()
    canvas.restoreToCount(save)
}

private fun Bitmap.centerCropScaled(width: Int, height: Int): Bitmap {
    val scale = max(width / this.width.toFloat(), height / this.height.toFloat())
    val scaledWidth = (this.width * scale).toInt().coerceAtLeast(width)
    val scaledHeight = (this.height * scale).toInt().coerceAtLeast(height)
    val scaled = Bitmap.createScaledBitmap(this, scaledWidth, scaledHeight, true)
    val left = ((scaledWidth - width) / 2).coerceAtLeast(0)
    val top = ((scaledHeight - height) / 2).coerceAtLeast(0)
    val result = Bitmap.createBitmap(scaled, left, top, width, height)
    if (scaled != this) scaled.recycle()
    return result
}

private fun buildLayout(text: String, paint: TextPaint, width: Int, maxLines: Int): StaticLayout {
    val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
        .setLineSpacing(8f, 1.04f)
        .setIncludePad(false)
        .setMaxLines(maxLines)
        .build()
    return layout
}

private fun drawLayout(canvas: Canvas, layout: StaticLayout, x: Float, y: Float) {
    val save = canvas.save()
    canvas.translate(x, y)
    layout.draw(canvas)
    canvas.restoreToCount(save)
}

private fun drawSingleLine(canvas: Canvas, text: String, paint: TextPaint, x: Float, y: Float, maxWidth: Float) {
    val ellipsized = android.text.TextUtils.ellipsize(text, paint, maxWidth, android.text.TextUtils.TruncateAt.END)
    canvas.drawText(ellipsized.toString(), x, y, paint)
}

private fun writeLyricShareCard(context: Context, bitmap: Bitmap): Uri {
    val dir = File(context.cacheDir, "lyric_share").apply {
        deleteRecursively()
        mkdirs()
    }
    val file = File(dir, "ella_lyric_${System.currentTimeMillis()}.png")
    FileOutputStream(file).use { output ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
    }
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

private fun LyricLine.sharePrimaryText(): String {
    return text.trim().ifBlank {
        backgroundText?.trim().orEmpty()
    }
}

private data class ShareLyricBlock(
    val primary: String,
    val secondary: List<String>
)

private fun LyricLine.toShareLyricBlock(): ShareLyricBlock? {
    val primary = sharePrimaryText().takeIf { it.isNotBlank() } ?: return null
    val secondary = listOfNotNull(
        translation?.trim()?.takeIf { it.isNotBlank() },
        backgroundText?.trim()?.takeIf { it.isNotBlank() && it != primary },
        backgroundTranslation?.trim()?.takeIf { it.isNotBlank() }
    ).distinct()
    return ShareLyricBlock(primary, secondary)
}

private fun shareLyricTextSize(lines: List<String>): Float {
    val longest = lines.maxOfOrNull { it.length } ?: 0
    return when {
        lines.size >= 5 -> 48f
        lines.size >= 4 -> 54f
        longest > 24 -> 54f
        longest > 18 -> 60f
        else -> 68f
    }
}

private fun Bitmap?.extractSharePalette(fallback: List<Int>): List<Int> {
    if (this == null || width <= 0 || height <= 0) return fallback
    val step = (minOf(width, height) / 42).coerceAtLeast(1)
    var red = 0.0
    var green = 0.0
    var blue = 0.0
    var weightSum = 0.0
    val hsv = FloatArray(3)
    var y = 0
    while (y < height) {
        var x = 0
        while (x < width) {
            val pixel = getPixel(x, y)
            if (Color.alpha(pixel) > 32) {
                Color.colorToHSV(pixel, hsv)
                val saturation = hsv[1].coerceIn(0f, 1f)
                val value = hsv[2].coerceIn(0f, 1f)
                val weight = (0.28 + saturation * 1.9 + value * 0.7).let {
                    if (value < 0.10f) it * 0.20 else it
                }
                red += Color.red(pixel) * weight
                green += Color.green(pixel) * weight
                blue += Color.blue(pixel) * weight
                weightSum += weight
            }
            x += step
        }
        y += step
    }
    if (weightSum <= 0.0) return fallback
    val r = (red / weightSum).toInt()
    val g = (green / weightSum).toInt()
    val b = (blue / weightSum).toInt()
    val base = Color.rgb(r, g, b).boostForShare()
    return listOf(
        base.lightenForShare(1.08f),
        base.darkenForShare(0.78f),
        base.darkenForShare(0.58f)
    )
}

private fun Int.boostForShare(): Int {
    val r = Color.red(this)
    val g = Color.green(this)
    val b = Color.blue(this)
    val maxChannel = maxOf(r, g, b).coerceAtLeast(1)
    val boost = (168f / maxChannel).coerceIn(1.08f, 2.35f)
    return Color.rgb(
        (r * boost).toInt().coerceIn(0, 255),
        (g * boost).toInt().coerceIn(0, 255),
        (b * boost).toInt().coerceIn(0, 255)
    )
}

private fun Int.lightenForShare(factor: Float): Int {
    return Color.rgb(
        (Color.red(this) * factor).toInt().coerceIn(0, 255),
        (Color.green(this) * factor).toInt().coerceIn(0, 255),
        (Color.blue(this) * factor).toInt().coerceIn(0, 255)
    )
}

private fun Int.darkenForShare(factor: Float): Int {
    return Color.rgb(
        (Color.red(this) * factor).toInt().coerceIn(0, 255),
        (Color.green(this) * factor).toInt().coerceIn(0, 255),
        (Color.blue(this) * factor).toInt().coerceIn(0, 255)
    )
}

private fun lyricShareFooter(customInfo: String): String {
    val normalized = customInfo.trim().removePrefix("@").trim()
    return if (normalized.isBlank()) {
        "Via Ella"
    } else {
        "Via @$normalized"
    }
}
