package com.ella.music.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun FolderOutlineIcon(
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val strokeWidth = 2.8.dp.toPx().coerceAtMost(size.minDimension * 0.08f)
        val inset = strokeWidth / 2f
        val bodyTop = size.height * 0.34f
        val radius = size.minDimension * 0.12f
        val tabPath = Path().apply {
            moveTo(size.width * 0.13f, bodyTop + inset)
            lineTo(size.width * 0.13f, size.height * 0.25f)
            quadraticTo(size.width * 0.13f, size.height * 0.15f, size.width * 0.23f, size.height * 0.15f)
            lineTo(size.width * 0.42f, size.height * 0.15f)
            quadraticTo(size.width * 0.48f, size.height * 0.15f, size.width * 0.52f, size.height * 0.21f)
            lineTo(size.width * 0.60f, bodyTop)
            lineTo(size.width * 0.87f, bodyTop)
        }
        drawPath(
            path = tabPath,
            color = tint,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        drawRoundRect(
            color = tint,
            topLeft = Offset(size.width * 0.13f, bodyTop),
            size = Size(size.width * 0.74f, size.height * 0.50f),
            cornerRadius = CornerRadius(radius, radius),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}
