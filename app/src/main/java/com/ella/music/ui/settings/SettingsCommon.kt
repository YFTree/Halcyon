package com.ella.music.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ella.music.ui.components.EllaMiuixTextField
import kotlinx.coroutines.delay
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun SettingsCardGroup(content: @Composable () -> Unit) {
    val isDark = MiuixTheme.colorScheme.background.luminance() < 0.5f
    val cardColor = if (isDark) Color(0xFF1D1D21) else Color(0xFFFFFFFF)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 14.dp),
        cornerRadius = 16.dp,
        insideMargin = PaddingValues(0.dp),
        colors = CardDefaults.defaultColors(
            color = cardColor
        )
    ) {
        content()
    }
}

@Composable
internal fun SplitSettingTextField(
    label: String,
    value: String,
    summary: String,
    singleLine: Boolean = false,
    isPassword: Boolean = false,
    onValueChange: (String) -> Unit
) {
    var localValue by remember(label) { mutableStateOf(value) }
    var pendingValue by remember(label) { mutableStateOf<String?>(null) }

    LaunchedEffect(value) {
        if (pendingValue == value) {
            pendingValue = null
            if (localValue != value) localValue = value
        } else if (pendingValue == null && value != localValue) {
            localValue = value
        }
    }

    LaunchedEffect(localValue) {
        if (pendingValue == localValue || localValue == value) return@LaunchedEffect
        delay(360)
        if (localValue != value) {
            pendingValue = localValue
            onValueChange(localValue)
        }
    }

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(
            text = label,
            fontSize = 15.sp,
            color = MiuixTheme.colorScheme.onSurface
        )
        Text(
            text = summary,
            fontSize = 13.sp,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
        )
        if (isPassword) {
            androidx.compose.foundation.text.BasicTextField(
                value = localValue,
                onValueChange = {
                    localValue = it
                },
                singleLine = singleLine,
                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.84f), RoundedCornerShape(14.dp))
                    .padding(14.dp),
                textStyle = TextStyle(
                    color = MiuixTheme.colorScheme.onSurface,
                    fontSize = 15.sp
                ),
                decorationBox = { innerTextField ->
                    Box {
                        if (localValue.isEmpty()) {
                            Text(
                                text = label,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                fontSize = 15.sp
                            )
                        }
                        innerTextField()
                    }
                }
            )
        } else {
            EllaMiuixTextField(
                value = localValue,
                onValueChange = {
                    localValue = it
                },
                label = label,
                singleLine = singleLine,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
