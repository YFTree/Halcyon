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
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import android.widget.Toast
import androidx.core.content.FileProvider
import com.ella.music.R
import com.ella.music.data.model.LyricLine
import com.ella.music.data.model.Song
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

private const val SHARE_CARD_WIDTH = 1080
private const val SHARE_CARD_MIN_HEIGHT = 720
private const val SHARE_CARD_MAX_HEIGHT = 1920
private const val SHARE_CARD_HORIZONTAL_PADDING = 92f
private const val SHARE_CARD_TOP_PADDING = 88f
private const val SHARE_CARD_BOTTOM_PADDING = 72f
private const val SHARE_CARD_COVER_SIZE = 120f
private const val SHARE_CARD_HEADER_GAP = 60f
private const val SHARE_CARD_FOOTER_GAP = 60f
private const val SHARE_CARD_SECONDARY_GAP = 10f
private const val SHARE_CARD_MIN_BLOCK_GAP = 20f
private const val SHARE_CARD_MAX_BLOCK_GAP = 34f
private const val SHARE_CARD_MAX_BLOCKS = 10

internal data class ShareLyricBlock(
    val primary: String,
    val secondary: List<String>
)

internal data class LyricShareCardContent(
    val title: String,
    val artist: String,
    val annotation: String,
    val footerText: String,
    val blocks: List<ShareLyricBlock>,
    val backgroundColors: List<Int>
)

internal data class MeasuredTextBlock(
    val layout: StaticLayout,
    val gapAfter: Float
)

internal data class MeasuredShareLyricBlock(
    val primary: MeasuredTextBlock,
    val secondary: List<MeasuredTextBlock>,
    val gapAfter: Float
)

internal data class LyricShareCardLayout(
    val canvasWidth: Int,
    val adaptiveCanvasHeight: Int,
    val safePadding: Float,
    val headerTop: Float,
    val headerHeight: Float,
    val lyricsTop: Float,
    val lyricsHeight: Float,
    val footerTop: Float,
    val footerHeight: Float,
    val viaTextBaseline: Float,
    val songInfoTop: Float,
    val songInfoHeight: Float,
    val coverRect: RectF,
    val titleLayout: StaticLayout,
    val annotationLayout: StaticLayout?,
    val artistLayout: StaticLayout,
    val lyricBlocks: List<MeasuredShareLyricBlock>,
    val footerPaint: TextPaint,
    val footerText: String
)

private data class LyricSizingCandidate(
    val primarySize: Float,
    val secondarySize: Float,
    val primaryMaxLines: Int,
    val secondaryMaxLines: Int,
    val primaryLineSpacingAdd: Float,
    val secondaryLineSpacingAdd: Float,
    val blockGap: Float
)

private data class SharePaletteRegion(
    val leftFraction: Float,
    val topFraction: Float,
    val rightFraction: Float,
    val bottomFraction: Float
)

fun shareLyricCard(
    context: Context,
    song: Song?,
    line: LyricLine,
    cover: Bitmap?,
    backgroundColors: List<Int>,
    annotation: String = "",
    customInfo: String = "",
    shareTypeface: android.graphics.Typeface? = null,
    includeTranslation: Boolean = true
) {
    shareLyricCard(
        context = context,
        song = song,
        lines = listOf(line),
        cover = cover,
        backgroundColors = backgroundColors,
        annotation = annotation,
        customInfo = customInfo,
        shareTypeface = shareTypeface,
        includeTranslation = includeTranslation
    )
}

fun shareLyricCard(
    context: Context,
    song: Song?,
    lines: List<LyricLine>,
    cover: Bitmap?,
    backgroundColors: List<Int>,
    annotation: String = "",
    customInfo: String = "",
    shareTypeface: android.graphics.Typeface? = null,
    includeTranslation: Boolean = true
) {
    runCatching {
        val bitmap = createLyricShareCard(
            context = context,
            song = song,
            lines = lines,
            cover = cover,
            backgroundColors = backgroundColors,
            annotation = annotation,
            customInfo = customInfo,
            shareTypeface = shareTypeface,
            includeTranslation = includeTranslation
        )
        val uri = writeLyricShareCard(context, bitmap)
        bitmap.recycle()
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra("com.mocharealm.compound.EXTRA_SOURCE_NAME", context.getString(R.string.app_name))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            clipData = ClipData.newUri(context.contentResolver, "${context.getString(R.string.app_name)} Lyric Card", uri)
        }
        context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.lyric_share_chooser_title)))
    }.onFailure {
        Toast.makeText(context, context.getString(R.string.lyric_share_failed), Toast.LENGTH_SHORT).show()
    }
}

internal fun buildLyricShareCardContent(
    context: Context,
    song: Song?,
    lines: List<LyricLine>,
    backgroundColors: List<Int>,
    annotation: String,
    customInfo: String,
    includeTranslation: Boolean = true
): LyricShareCardContent {
    val blocks = lines
        .filter { it.sharePrimaryText().isNotBlank() }
        .mapNotNull { it.toShareLyricBlock(includeTranslation = includeTranslation) }
        .ifEmpty { listOf(ShareLyricBlock("\u266a", emptyList())) }
        .take(SHARE_CARD_MAX_BLOCKS)

    return LyricShareCardContent(
        title = song?.title?.takeIf { it.isNotBlank() } ?: context.getString(R.string.lyric_share_unknown_song),
        artist = song?.artist?.takeIf { it.isNotBlank() } ?: context.getString(R.string.lyric_share_unknown_artist),
        annotation = annotation.trim(),
        footerText = lyricShareFooter(context, customInfo),
        blocks = blocks,
        backgroundColors = backgroundColors
    )
}

internal fun calculateLyricShareLayout(
    content: LyricShareCardContent,
    canvasWidth: Int = SHARE_CARD_WIDTH,
    minHeight: Int = SHARE_CARD_MIN_HEIGHT,
    maxHeight: Int = SHARE_CARD_MAX_HEIGHT,
    shareTypeface: android.graphics.Typeface? = null
): LyricShareCardLayout {
    val safePadding = SHARE_CARD_HORIZONTAL_PADDING
    val coverRect = RectF(
        safePadding,
        SHARE_CARD_TOP_PADDING,
        safePadding + SHARE_CARD_COVER_SIZE,
        SHARE_CARD_TOP_PADDING + SHARE_CARD_COVER_SIZE
    )
    val textLeft = coverRect.right + 28f
    val textWidth = (canvasWidth - textLeft - safePadding).roundToInt()

    val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 38f
        typeface = shareTypeface.shareCardTypeface(android.graphics.Typeface.BOLD)
        setShadowLayer(18f, 0f, 8f, Color.argb(76, 0, 0, 0))
    }
    val annotationPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(214, 255, 255, 255)
        textSize = 27f
        typeface = shareTypeface.shareCardTypeface(android.graphics.Typeface.BOLD)
    }
    val artistPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(168, 255, 255, 255)
        textSize = 26f
        typeface = shareTypeface.shareCardTypeface(android.graphics.Typeface.NORMAL)
    }
    val titleLayout = buildLayout(
        text = content.title,
        paint = titlePaint,
        width = textWidth,
        maxLines = 2,
        lineSpacingAdd = 4f,
        lineSpacingMult = 1.02f
    )
    val annotationLayout = content.annotation.takeIf { it.isNotBlank() }?.let {
        buildLayout(
            text = it,
            paint = annotationPaint,
            width = textWidth,
            maxLines = 1,
            lineSpacingAdd = 2f,
            lineSpacingMult = 1f
        )
    }
    val artistLayout = buildLayout(
        text = content.artist,
        paint = artistPaint,
        width = textWidth,
        maxLines = 2,
        lineSpacingAdd = 2f,
        lineSpacingMult = 1f
    )
    val songInfoHeight = max(
        SHARE_CARD_COVER_SIZE,
        (
            titleLayout.height +
                (annotationLayout?.height?.plus(10) ?: 0) +
                12 +
                artistLayout.height
            ).toFloat()
    )
    val headerTop = SHARE_CARD_TOP_PADDING
    val headerHeight = songInfoHeight
    val songInfoTop = headerTop

    val footerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(112, 255, 255, 255)
        textSize = 24f
        typeface = shareTypeface.shareCardTypeface(android.graphics.Typeface.BOLD)
    }
    val footerHeight = footerPaint.fontMetrics.run { bottom - top } + 8f
    val lyricsTop = headerTop + headerHeight + SHARE_CARD_HEADER_GAP
    val maxLyricsHeight = (
        maxHeight -
            SHARE_CARD_BOTTOM_PADDING -
            footerHeight -
            SHARE_CARD_FOOTER_GAP -
            lyricsTop
        ).coerceAtLeast(120f).roundToInt()

    val candidates = buildLyricSizingCandidates(content.blocks)
    val measuredLyrics = candidates
        .firstNotNullOfOrNull { candidate ->
            measureLyricBlocks(
                blocks = content.blocks,
                width = (canvasWidth - safePadding * 2).roundToInt(),
                candidate = candidate,
                availableHeight = maxLyricsHeight.toFloat(),
                shareTypeface = shareTypeface
            )?.takeIf { it.first.isNotEmpty() }
        }
        ?: measureLyricBlocks(
            blocks = content.blocks,
            width = (canvasWidth - safePadding * 2).roundToInt(),
            candidate = candidates.last(),
            availableHeight = maxLyricsHeight.toFloat(),
            shareTypeface = shareTypeface,
            forceTruncate = true
        )
        ?: (emptyList<MeasuredShareLyricBlock>() to 0f)

    val lyricsHeight = measuredLyrics.second
    val footerTop = (lyricsTop + lyricsHeight + SHARE_CARD_FOOTER_GAP)
        .coerceAtLeast(lyricsTop + lyricsHeight + 48f)
    val adaptiveCanvasHeight = (
        footerTop +
            footerHeight +
            SHARE_CARD_BOTTOM_PADDING
        ).roundToInt().coerceIn(minHeight, maxHeight)
    val viaTextBaseline = adaptiveCanvasHeight - SHARE_CARD_BOTTOM_PADDING - footerPaint.fontMetrics.descent

    return LyricShareCardLayout(
        canvasWidth = canvasWidth,
        adaptiveCanvasHeight = adaptiveCanvasHeight,
        safePadding = safePadding,
        headerTop = headerTop,
        headerHeight = headerHeight,
        lyricsTop = lyricsTop,
        lyricsHeight = lyricsHeight,
        footerTop = footerTop,
        footerHeight = footerHeight,
        viaTextBaseline = viaTextBaseline,
        songInfoTop = songInfoTop,
        songInfoHeight = songInfoHeight,
        coverRect = coverRect,
        titleLayout = titleLayout,
        annotationLayout = annotationLayout,
        artistLayout = artistLayout,
        lyricBlocks = measuredLyrics.first,
        footerPaint = footerPaint,
        footerText = content.footerText
    )
}

internal fun renderLyricShareCardBitmap(
    content: LyricShareCardContent,
    layout: LyricShareCardLayout,
    cover: Bitmap?
): Bitmap {
    val bitmap = Bitmap.createBitmap(layout.canvasWidth, layout.adaptiveCanvasHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawShareBackground(
        canvas = canvas,
        width = layout.canvasWidth,
        height = layout.adaptiveCanvasHeight,
        colors = content.backgroundColors
    )
    drawShareHeader(canvas, layout, cover)
    drawShareLyrics(canvas, layout)
    drawShareFooter(canvas, layout)
    return bitmap
}

private fun createLyricShareCard(
    context: Context,
    song: Song?,
    lines: List<LyricLine>,
    cover: Bitmap?,
    backgroundColors: List<Int>,
    annotation: String,
    customInfo: String,
    shareTypeface: android.graphics.Typeface?,
    includeTranslation: Boolean
): Bitmap {
    val resolvedBackgroundColors = resolveLyricShareBackgroundColors(cover, backgroundColors)
    val content = buildLyricShareCardContent(
        context = context,
        song = song,
        lines = lines,
        backgroundColors = resolvedBackgroundColors,
        annotation = annotation,
        customInfo = customInfo,
        includeTranslation = includeTranslation
    )
    val layout = calculateLyricShareLayout(content, shareTypeface = shareTypeface)
    return renderLyricShareCardBitmap(content, layout, cover)
}

private fun android.graphics.Typeface?.shareCardTypeface(style: Int): android.graphics.Typeface =
    if (this != null) android.graphics.Typeface.create(this, style)
    else android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, style)

private fun buildLyricSizingCandidates(blocks: List<ShareLyricBlock>): List<LyricSizingCandidate> {
    val longestLine = blocks.maxOfOrNull { it.primary.length } ?: 0
    val preferredSizes = buildList {
        add(baseShareLyricTextSize(blocks.size, longestLine))
        add(78f)
        add(72f)
        add(66f)
        add(60f)
        add(54f)
        add(48f)
        add(44f)
    }.distinct()

    return preferredSizes.map { primarySize ->
        LyricSizingCandidate(
            primarySize = primarySize,
            secondarySize = (primarySize * 0.42f).coerceIn(22f, 34f),
            primaryMaxLines = if (blocks.size <= 2) 4 else 3,
            secondaryMaxLines = 2,
            primaryLineSpacingAdd = (primarySize * 0.07f).coerceIn(4f, 10f),
            secondaryLineSpacingAdd = 4f,
            blockGap = (primarySize * 0.24f).coerceIn(SHARE_CARD_MIN_BLOCK_GAP, SHARE_CARD_MAX_BLOCK_GAP)
        )
    }
}

private fun measureLyricBlocks(
    blocks: List<ShareLyricBlock>,
    width: Int,
    candidate: LyricSizingCandidate,
    availableHeight: Float,
    shareTypeface: android.graphics.Typeface? = null,
    forceTruncate: Boolean = false
): Pair<List<MeasuredShareLyricBlock>, Float>? {
    val primaryPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = candidate.primarySize
        typeface = shareTypeface.shareCardTypeface(android.graphics.Typeface.BOLD)
        setShadowLayer(20f, 0f, 8f, Color.argb(92, 0, 0, 0))
    }
    val secondaryPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(186, 255, 255, 255)
        textSize = candidate.secondarySize
        typeface = shareTypeface.shareCardTypeface(android.graphics.Typeface.NORMAL)
        setShadowLayer(10f, 0f, 4f, Color.argb(70, 0, 0, 0))
    }

    fun measureBlock(block: ShareLyricBlock, isLast: Boolean): MeasuredShareLyricBlock {
        val primaryLayout = buildLayout(
            text = block.primary,
            paint = primaryPaint,
            width = width,
            maxLines = candidate.primaryMaxLines,
            lineSpacingAdd = candidate.primaryLineSpacingAdd,
            lineSpacingMult = 1f
        )
        val secondaryLayouts = block.secondary.take(1).map {
            MeasuredTextBlock(
                layout = buildLayout(
                    text = it,
                    paint = secondaryPaint,
                    width = width,
                    maxLines = candidate.secondaryMaxLines,
                    lineSpacingAdd = candidate.secondaryLineSpacingAdd,
                    lineSpacingMult = 1f
                ),
                gapAfter = SHARE_CARD_SECONDARY_GAP
            )
        }
        return MeasuredShareLyricBlock(
            primary = MeasuredTextBlock(primaryLayout, 12f),
            secondary = secondaryLayouts,
            gapAfter = if (isLast) 0f else candidate.blockGap
        )
    }

    fun blockHeight(block: MeasuredShareLyricBlock): Float {
        return block.primary.layout.height +
            block.primary.gapAfter +
            block.secondary.fold(0f) { total, secondary ->
                total + secondary.layout.height + secondary.gapAfter
            } +
            block.gapAfter
    }

    val measured = mutableListOf<MeasuredShareLyricBlock>()
    var totalHeight = 0f
    blocks.forEachIndexed { index, block ->
        val candidateBlock = measureBlock(block, isLast = index == blocks.lastIndex)
        val nextHeight = totalHeight + blockHeight(candidateBlock)
        if (nextHeight <= availableHeight) {
            measured += candidateBlock
            totalHeight = nextHeight
        } else {
            if (!forceTruncate) return null
            if (measured.isEmpty()) {
                measured += candidateBlock
                totalHeight = blockHeight(candidateBlock)
            } else {
                val ellipsisBlock = measureBlock(
                    block = ShareLyricBlock("...", emptyList()),
                    isLast = true
                )
                val ellipsisHeight = blockHeight(ellipsisBlock)
                while (measured.isNotEmpty() && totalHeight + ellipsisHeight > availableHeight) {
                    val removed = measured.removeAt(measured.lastIndex)
                    totalHeight -= blockHeight(removed)
                }
                if (measured.isNotEmpty()) {
                    val last = measured.removeAt(measured.lastIndex)
                    totalHeight -= blockHeight(last)
                    measured += last.copy(gapAfter = candidate.blockGap)
                    totalHeight += blockHeight(measured.last())
                }
                if (totalHeight + ellipsisHeight <= availableHeight || measured.isEmpty()) {
                    measured += ellipsisBlock
                    totalHeight += ellipsisHeight
                }
            }
            return measured to totalHeight.coerceAtMost(availableHeight)
        }
    }
    return measured to totalHeight
}

private fun drawShareHeader(
    canvas: Canvas,
    layout: LyricShareCardLayout,
    cover: Bitmap?
) {
    drawShareHeaderCover(canvas, cover, layout.coverRect)

    var textTop = layout.songInfoTop + 6f
    val textLeft = layout.coverRect.right + 28f
    drawLayout(canvas, layout.titleLayout, textLeft, textTop)
    textTop += layout.titleLayout.height + 10f
    layout.annotationLayout?.let {
        drawLayout(canvas, it, textLeft, textTop)
        textTop += it.height + 12f
    }
    drawLayout(canvas, layout.artistLayout, textLeft, textTop)
}

private fun drawShareHeaderCover(canvas: Canvas, cover: Bitmap?, rect: RectF) {
    val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(44, 0, 0, 0)
        setShadowLayer(24f, 0f, 10f, Color.argb(72, 0, 0, 0))
    }
    canvas.drawRoundRect(rect, 30f, 30f, shadowPaint)
    if (cover != null) {
        drawRoundedCover(canvas, cover, rect, 30f)
    } else {
        val placeholderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                rect.left,
                rect.top,
                rect.right,
                rect.bottom,
                intArrayOf(
                    Color.argb(120, 255, 255, 255),
                    Color.argb(34, 255, 255, 255)
                ),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(rect, 30f, 30f, placeholderPaint)
        val notePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(208, 255, 255, 255)
            textSize = 42f
            textAlign = Paint.Align.CENTER
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        }
        val baseline = rect.centerY() - (notePaint.descent() + notePaint.ascent()) / 2f
        canvas.drawText("\u266a", rect.centerX(), baseline, notePaint)
    }
}

private fun drawShareLyrics(
    canvas: Canvas,
    layout: LyricShareCardLayout
) {
    var y = layout.lyricsTop
    layout.lyricBlocks.forEach { block ->
        drawLayout(canvas, block.primary.layout, layout.safePadding, y)
        y += block.primary.layout.height + block.primary.gapAfter
        block.secondary.forEach { secondary ->
            drawLayout(canvas, secondary.layout, layout.safePadding, y)
            y += secondary.layout.height + secondary.gapAfter
        }
        y += block.gapAfter
    }
}

private fun drawShareFooter(
    canvas: Canvas,
    layout: LyricShareCardLayout
) {
    canvas.drawText(layout.footerText, layout.safePadding, layout.viaTextBaseline, layout.footerPaint)
}

private fun drawShareBackground(
    canvas: Canvas,
    width: Int,
    height: Int,
    colors: List<Int>
) {
    val fallbackColors = listOf(
        Color.rgb(69, 78, 110),
        Color.rgb(36, 61, 92),
        Color.rgb(19, 25, 34)
    )
    val picked = colors.filter { Color.alpha(it) > 0 }.ifEmpty { fallbackColors }
    val c1 = picked.first().boostForShare()
    val c2 = picked.getOrElse(1) { c1 }.boostForShare()
    val c3 = picked.last().boostForShare()

    Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
            0f,
            0f,
            width.toFloat(),
            height * 0.92f,
            intArrayOf(
                c1.lightenForShare(1.12f),
                c2.lightenForShare(1.04f),
                c3.darkenForShare(0.74f)
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), this)
    }

    drawShareColorBlob(canvas, width * 0.14f, height * 0.16f, width * 0.76f, c1.lightenForShare(1.12f), 112)
    drawShareColorBlob(canvas, width * 0.84f, height * 0.24f, width * 0.62f, c2.lightenForShare(1.08f), 96)
    drawShareColorBlob(canvas, width * 0.46f, height * 0.86f, width * 0.82f, c3.lightenForShare(1.10f), 108)

    Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = RadialGradient(
            width * 0.38f,
            height * 0.42f,
            width * 0.82f,
            intArrayOf(
                Color.argb(42, 255, 255, 255),
                Color.argb(0, 255, 255, 255)
            ),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), this)
    }

    Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
            0f,
            0f,
            0f,
            height.toFloat(),
            intArrayOf(
                Color.argb(46, 4, 7, 12),
                Color.argb(12, 4, 7, 12),
                Color.argb(68, 4, 7, 12)
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), this)
    }

    Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(10, 0, 0, 0)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), this)
    }
}

private fun drawShareColorBlob(
    canvas: Canvas,
    cx: Float,
    cy: Float,
    radius: Float,
    color: Int,
    alpha: Int
) {
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = RadialGradient(
            cx,
            cy,
            radius,
            intArrayOf(
                Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color)),
                Color.argb(0, Color.red(color), Color.green(color), Color.blue(color))
            ),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, radius, this)
    }
}

private fun drawRoundedCover(canvas: Canvas, cover: Bitmap, rect: RectF, radius: Float) {
    val path = Path().apply {
        addRoundRect(rect, radius, radius, Path.Direction.CW)
    }
    val save = canvas.save()
    canvas.clipPath(path)
    val cropped = cover.centerCropScaled(rect.width().roundToInt(), rect.height().roundToInt())
    canvas.drawBitmap(
        cropped,
        rect.left,
        rect.top,
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            isFilterBitmap = true
            isDither = true
        }
    )
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
    if (scaled !== this && scaled !== result) {
        scaled.recycle()
    }
    return result
}

private fun buildLayout(
    text: String,
    paint: TextPaint,
    width: Int,
    maxLines: Int,
    lineSpacingAdd: Float,
    lineSpacingMult: Float
): StaticLayout {
    return StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
        .setIncludePad(false)
        .setLineSpacing(lineSpacingAdd, lineSpacingMult)
        .setMaxLines(maxLines)
        .setEllipsize(TextUtils.TruncateAt.END)
        .build()
}

private fun drawLayout(canvas: Canvas, layout: StaticLayout, x: Float, y: Float) {
    val save = canvas.save()
    canvas.translate(x, y)
    layout.draw(canvas)
    canvas.restoreToCount(save)
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

internal fun LyricLine.sharePrimaryText(): String {
    return text.trim().ifBlank {
        backgroundText?.trim().orEmpty()
    }
}

internal fun LyricLine.toShareLyricBlock(includeTranslation: Boolean = true): ShareLyricBlock? {
    val primary = sharePrimaryText().takeIf { it.isNotBlank() } ?: return null
    val secondary = listOfNotNull(
        translation?.trim()?.takeIf { includeTranslation && it.isNotBlank() },
        backgroundText?.trim()?.takeIf { it.isNotBlank() && it != primary },
        backgroundTranslation?.trim()?.takeIf { includeTranslation && it.isNotBlank() }
    ).distinct()
    return ShareLyricBlock(primary = primary, secondary = secondary)
}

internal fun resolveLyricShareBackgroundColors(
    cover: Bitmap?,
    fallbackColors: List<Int>
): List<Int> = cover.extractSharePalette(fallbackColors)

private fun baseShareLyricTextSize(blockCount: Int, longestLine: Int): Float {
    return when {
        blockCount <= 1 && longestLine <= 18 -> 90f
        blockCount <= 2 && longestLine <= 22 -> 82f
        blockCount <= 3 && longestLine <= 24 -> 76f
        blockCount <= 4 -> 70f
        blockCount <= 6 -> 62f
        else -> 56f
    }
}

private fun Bitmap?.extractSharePalette(fallback: List<Int>): List<Int> {
    if (this == null || width <= 0 || height <= 0) return fallback
    val regions = listOf(
        SharePaletteRegion(0f, 0f, 0.58f, 0.54f),
        SharePaletteRegion(0.42f, 0.06f, 1f, 0.64f),
        SharePaletteRegion(0.10f, 0.44f, 0.92f, 1f),
        SharePaletteRegion(0.18f, 0.18f, 0.82f, 0.82f)
    )
    val sampledColors = regions.mapNotNull { region ->
        sampleShareRegionColor(region)
    }
    val fallbackColors = fallback.filter { Color.alpha(it) > 0 }
    val palette = mutableListOf<Int>()
    sampledColors
        .sortedByDescending { it.shareVibrancyScore() }
        .forEach { color ->
            if (palette.none { it.shareColorDistanceTo(color) < 68f }) {
                palette += color.boostForShare()
            }
        }
    fallbackColors.forEach { color ->
        if (palette.size >= 3) return@forEach
        if (palette.none { it.shareColorDistanceTo(color) < 56f }) {
            palette += color.boostForShare()
        }
    }
    if (palette.isEmpty()) return fallback
    if (palette.size == 1) {
        val base = palette.first()
        palette += base.lightenForShare(1.10f)
        palette += base.darkenForShare(0.78f)
    } else if (palette.size == 2) {
        val bridge = blendShareColors(palette.first(), palette.last(), 0.45f).lightenForShare(1.04f)
        palette.add(if (palette.none { it.shareColorDistanceTo(bridge) < 36f }) bridge else palette.last().darkenForShare(0.78f))
    }
    return palette.take(3)
}

private fun Bitmap.sampleShareRegionColor(region: SharePaletteRegion): Int? {
    val left = (width * region.leftFraction).roundToInt().coerceIn(0, width - 1)
    val top = (height * region.topFraction).roundToInt().coerceIn(0, height - 1)
    val right = (width * region.rightFraction).roundToInt().coerceIn(left + 1, width)
    val bottom = (height * region.bottomFraction).roundToInt().coerceIn(top + 1, height)
    val step = (minOf(right - left, bottom - top) / 18).coerceAtLeast(1)
    var red = 0.0
    var green = 0.0
    var blue = 0.0
    var weightSum = 0.0
    val hsv = FloatArray(3)
    var y = top
    while (y < bottom) {
        var x = left
        while (x < right) {
            val pixel = getPixel(x, y)
            if (Color.alpha(pixel) > 32) {
                Color.colorToHSV(pixel, hsv)
                val saturation = hsv[1].coerceIn(0f, 1f)
                val value = hsv[2].coerceIn(0f, 1f)
                val chromaWeight = if (saturation > 0.16f) 1.0 else 0.55
                val weight = (0.30 + saturation * 2.35 + value * 0.82).let {
                    if (value < 0.10f) it * 0.20 else it
                } * chromaWeight
                red += Color.red(pixel) * weight
                green += Color.green(pixel) * weight
                blue += Color.blue(pixel) * weight
                weightSum += weight
            }
            x += step
        }
        y += step
    }
    if (weightSum <= 0.0) return null
    return Color.rgb(
        (red / weightSum).toInt().coerceIn(0, 255),
        (green / weightSum).toInt().coerceIn(0, 255),
        (blue / weightSum).toInt().coerceIn(0, 255)
    )
}

private fun Int.boostForShare(): Int {
    val hsv = FloatArray(3)
    Color.colorToHSV(this, hsv)
    hsv[1] = (hsv[1] * 1.18f + 0.06f).coerceIn(0.20f, 0.98f)
    hsv[2] = (hsv[2] * 1.08f + 0.05f).coerceIn(0.28f, 0.98f)
    return Color.HSVToColor(hsv)
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

private fun Int.shareColorDistanceTo(other: Int): Float {
    val dr = (Color.red(this) - Color.red(other)).toFloat()
    val dg = (Color.green(this) - Color.green(other)).toFloat()
    val db = (Color.blue(this) - Color.blue(other)).toFloat()
    return kotlin.math.sqrt(dr * dr + dg * dg + db * db)
}

private fun Int.shareVibrancyScore(): Float {
    val hsv = FloatArray(3)
    Color.colorToHSV(this, hsv)
    return hsv[1] * 1.6f + hsv[2]
}

private fun blendShareColors(start: Int, end: Int, fraction: Float): Int {
    val t = fraction.coerceIn(0f, 1f)
    val inverse = 1f - t
    return Color.rgb(
        (Color.red(start) * inverse + Color.red(end) * t).roundToInt().coerceIn(0, 255),
        (Color.green(start) * inverse + Color.green(end) * t).roundToInt().coerceIn(0, 255),
        (Color.blue(start) * inverse + Color.blue(end) * t).roundToInt().coerceIn(0, 255)
    )
}

private fun lyricShareFooter(context: Context, customInfo: String): String {
    val normalized = customInfo.trim().removePrefix("@").trim()
    return if (normalized.isBlank()) {
        context.getString(R.string.lyric_share_footer_default)
    } else {
        context.getString(R.string.lyric_share_footer_custom, normalized)
    }
}
