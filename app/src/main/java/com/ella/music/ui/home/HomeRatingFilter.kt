package com.ella.music.ui.home

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ella.music.R
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun StarRatingFilterRow(
    selectedRatings: Set<Int>,
    onRatingsChange: (Set<Int>) -> Unit
) {
    val allSelected = selectedRatings.isEmpty()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StarRatingPill(
            text = stringResource(R.string.rating_filter_all),
            selected = allSelected,
            onClick = { onRatingsChange(emptySet()) }
        )
        (1..5).forEach { rating ->
            StarRatingPill(
                text = stringResource(R.string.rating_filter_star, rating),
                selected = rating in selectedRatings,
                onClick = {
                    val next = selectedRatings.toggleRating(rating)
                    onRatingsChange(next.normalizedRatingFilter())
                }
            )
        }
    }
}

private fun Set<Int>.toggleRating(rating: Int): Set<Int> {
    val safeRating = rating.coerceIn(1, 5)
    return if (isEmpty()) {
        setOf(safeRating)
    } else if (safeRating in this) {
        this - safeRating
    } else {
        this + safeRating
    }
}

internal fun Set<Int>.normalizedRatingFilter(): Set<Int> {
    val normalized = filter { it in 1..5 }.toSortedSet()
    return if (normalized.isEmpty() || normalized.size == 5) emptySet() else normalized
}

internal fun Set<Int>.summaryLabel(context: Context): String? {
    if (isEmpty()) return null
    return this
        .filter { it in 1..5 }
        .sorted()
        .joinToString(separator = " · ") { rating ->
            context.getString(R.string.rating_filter_star, rating)
        }
        .ifBlank { null }
}

@Composable
private fun StarRatingPill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        color = if (selected) MiuixTheme.colorScheme.onPrimary else MiuixTheme.colorScheme.onSurface,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.82f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp)
    )
}
