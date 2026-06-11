package com.ella.music.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ella.music.R
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Sort
import top.yukonga.miuix.kmp.menu.WindowIconDropdownMenu
import top.yukonga.miuix.kmp.theme.MiuixTheme

data class SortDropdownItem(
    val text: String,
    val selected: Boolean,
    val summary: String? = null,
    val onClick: () -> Unit
)

@Composable
fun SortDropdownMenu(
    items: List<SortDropdownItem>,
    modifier: Modifier = Modifier,
    tint: Color = MiuixTheme.colorScheme.onSurface,
    contentDescription: String = stringResource(R.string.common_sort)
) {
    SortDropdownMenuContent(items = items) {
        Icon(
            imageVector = MiuixIcons.Regular.Sort,
            contentDescription = contentDescription,
            tint = tint,
            modifier = modifier.size(24.dp)
        )
    }
}

@Composable
fun SortDropdownMenuContent(
    items: List<SortDropdownItem>,
    content: @Composable () -> Unit
) {
    WindowIconDropdownMenu(
        entries = listOf(
            DropdownEntry(
                items = items.map { item ->
                    DropdownItem(
                        text = item.text,
                        selected = item.selected,
                        summary = item.summary,
                        onClick = item.onClick
                    )
                }
            )
        )
    ) {
        content()
    }
}
