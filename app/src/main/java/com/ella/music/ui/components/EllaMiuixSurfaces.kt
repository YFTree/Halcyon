package com.ella.music.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowBottomSheet
import top.yukonga.miuix.kmp.window.WindowDialog

@Composable
fun EllaMiuixBottomSheet(
    show: Boolean,
    title: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    endAction: @Composable (() -> Unit)? = null,
    onDismissFinished: (() -> Unit)? = null,
    enableNestedScroll: Boolean = true,
    content: @Composable () -> Unit
) {
    WindowBottomSheet(
        show = show,
        title = title,
        endAction = endAction,
        onDismissRequest = onDismissRequest,
        onDismissFinished = onDismissFinished,
        enableNestedScroll = enableNestedScroll,
        cornerRadius = 28.dp,
        insideMargin = DpSize(20.dp, 18.dp),
        backgroundColor = MiuixTheme.colorScheme.background.copy(alpha = 0.98f),
        modifier = modifier,
        content = content
    )
}

@Composable
fun EllaMiuixDialog(
    show: Boolean,
    title: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    WindowDialog(
        show = show,
        title = title,
        onDismissRequest = onDismissRequest,
        backgroundColor = MiuixTheme.colorScheme.background.copy(alpha = 0.98f),
        insideMargin = DpSize(22.dp, 20.dp),
        modifier = modifier,
        content = content
    )
}

@Composable
fun EllaMiuixTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    focusRequester: FocusRequester? = null,
    textStyle: TextStyle = TextStyle(
        color = MiuixTheme.colorScheme.onSurface,
        fontSize = 15.sp
    )
) {
    val focusModifier = if (focusRequester != null) {
        Modifier.focusRequester(focusRequester)
    } else {
        Modifier
    }
    TextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        useLabelAsPlaceholder = true,
        singleLine = singleLine,
        insideMargin = DpSize(14.dp, 11.dp),
        backgroundColor = MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.84f),
        cornerRadius = 14.dp,
        textStyle = textStyle,
        modifier = modifier
            .fillMaxWidth()
            .then(focusModifier)
    )
}

@Composable
fun EllaMiuixSheetActions(
    cancelText: String,
    confirmText: String,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        Button(onClick = onCancel) {
            Text(cancelText)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Button(onClick = onConfirm) {
            Text(confirmText)
        }
    }
}
