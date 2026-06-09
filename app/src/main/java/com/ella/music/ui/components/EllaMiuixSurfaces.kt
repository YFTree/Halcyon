package com.ella.music.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TextFieldDefaults
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowBottomSheet
import top.yukonga.miuix.kmp.window.WindowDialog

data class EllaMiuixAction(
    val text: String,
    val onClick: () -> Unit,
    val primary: Boolean = false,
    val weight: Float = 1f
)

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
    summary: String? = null,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    WindowDialog(
        show = show,
        title = title,
        summary = summary,
        onDismissRequest = onDismissRequest,
        backgroundColor = MiuixTheme.colorScheme.background.copy(alpha = 0.98f),
        insideMargin = DpSize(22.dp, 20.dp),
        modifier = modifier,
        content = content
    )
}

@Composable
fun EllaMiuixDialogActions(
    cancelText: String,
    confirmText: String,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    EllaMiuixActionRow(
        actions = listOf(
            EllaMiuixAction(text = cancelText, onClick = onCancel),
            EllaMiuixAction(text = confirmText, onClick = onConfirm, primary = true)
        ),
        modifier = modifier
    )
}

@Composable
fun EllaMiuixTripleDialogActions(
    firstText: String,
    secondText: String,
    confirmText: String,
    onFirst: () -> Unit,
    onSecond: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    EllaMiuixActionRow(
        actions = listOf(
            EllaMiuixAction(text = firstText, onClick = onFirst),
            EllaMiuixAction(text = secondText, onClick = onSecond),
            EllaMiuixAction(text = confirmText, onClick = onConfirm, primary = true)
        ),
        modifier = modifier,
        spacing = 8.dp
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
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
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
        colors = TextFieldDefaults.textFieldColors(
            backgroundColor = MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.84f),
        ),
        cornerRadius = 14.dp,
        textStyle = textStyle,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        modifier = modifier
            .fillMaxWidth()
            .then(focusModifier)
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EllaMiuixChip(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    horizontalPadding: Dp = 14.dp,
    verticalPadding: Dp = 8.dp,
    content: @Composable RowScope.(contentColor: Color) -> Unit
) {
    val chipBackground = if (selected) {
        MiuixTheme.colorScheme.primary.copy(alpha = 0.18f)
    } else {
        MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.6f)
    }
    val chipContentColor = if (selected) {
        MiuixTheme.colorScheme.primary
    } else {
        MiuixTheme.colorScheme.onSurfaceVariantSummary
    }
    val clickModifier = if (onLongClick != null) {
        Modifier.combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick
        )
    } else {
        Modifier.clickable(onClick = onClick)
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(chipBackground)
            .then(clickModifier)
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        content = { content(chipContentColor) }
    )
}

@Composable
fun EllaMiuixChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    horizontalPadding: Dp = 14.dp,
    verticalPadding: Dp = 8.dp
) {
    EllaMiuixChip(
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        onLongClick = onLongClick,
        horizontalPadding = horizontalPadding,
        verticalPadding = verticalPadding
    ) { contentColor ->
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun EllaMiuixSurfaceCard(
    modifier: Modifier = Modifier,
    color: Color = MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.82f),
    shape: Shape = RoundedCornerShape(18.dp),
    contentPadding: PaddingValues = PaddingValues(14.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .clip(shape)
            .background(color)
            .padding(contentPadding),
        content = content
    )
}

@Composable
fun EllaMiuixListItem(
    title: String,
    summary: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(14.dp)
) {
    BasicComponent(
        title = title,
        summary = summary,
        modifier = modifier
            .clip(shape)
            .clickable(enabled = enabled, onClick = onClick)
    )
}

@Composable
fun EllaMiuixBadge(
    text: String,
    color: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(6.dp),
    horizontalPadding: Dp = 7.dp,
    verticalPadding: Dp = 2.dp,
    fontSize: androidx.compose.ui.unit.TextUnit = 12.sp,
    fontWeight: FontWeight = FontWeight.Bold
) {
    Text(
        text = text,
        modifier = modifier
            .clip(shape)
            .background(color)
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        color = contentColor,
        fontWeight = fontWeight,
        maxLines = 1,
        fontSize = fontSize
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
    EllaMiuixActionRow(
        actions = listOf(
            EllaMiuixAction(text = cancelText, onClick = onCancel),
            EllaMiuixAction(text = confirmText, onClick = onConfirm, primary = true)
        ),
        modifier = modifier
    )
}

@Composable
fun EllaMiuixActionRow(
    actions: List<EllaMiuixAction>,
    modifier: Modifier = Modifier,
    spacing: Dp = 12.dp
) {
    if (actions.isEmpty()) return

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing)
    ) {
        actions.forEach { action ->
            val buttonModifier = Modifier.weight(action.weight)
            if (action.primary) {
                TextButton(
                    text = action.text,
                    onClick = action.onClick,
                    modifier = buttonModifier,
                    colors = ButtonDefaults.textButtonColorsPrimary()
                )
            } else {
                TextButton(
                    text = action.text,
                    onClick = action.onClick,
                    modifier = buttonModifier
                )
            }
        }
    }
}
