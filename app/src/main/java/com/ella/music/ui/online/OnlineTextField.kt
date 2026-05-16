package com.ella.music.ui.online

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.basic.InputField
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun OnlineTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    InputField(
        query = value,
        onQueryChange = onValueChange,
        onSearch = { onSearch() },
        expanded = true,
        onExpandedChange = {},
        label = placeholder,
        textStyle = TextStyle(
            color = MiuixTheme.colorScheme.onSurface,
            fontSize = 16.sp
        ),
        modifier = modifier.fillMaxWidth()
    )
}
