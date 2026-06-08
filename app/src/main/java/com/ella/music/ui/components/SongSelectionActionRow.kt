package com.ella.music.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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
fun SongSelectionActionRow(
    selectedCount: Int,
    totalCount: Int,
    rangeEnabled: Boolean,
    allSelected: Boolean,
    onRangeSelect: () -> Unit,
    onSelectAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.library_selected_fraction, selectedCount, totalCount),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MiuixTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = stringResource(R.string.library_range_select),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = if (rangeEnabled) {
                MiuixTheme.colorScheme.onSurface
            } else {
                MiuixTheme.colorScheme.onSurfaceVariantSummary.copy(alpha = 0.38f)
            },
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .clickable(enabled = rangeEnabled, onClick = onRangeSelect)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        )
        Text(
            text = stringResource(if (allSelected) R.string.common_deselect_all else R.string.common_select_all),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = MiuixTheme.colorScheme.onSurface,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .clickable(onClick = onSelectAll)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}
