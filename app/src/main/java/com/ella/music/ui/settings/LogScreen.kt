package com.ella.music.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ella.music.data.AppLogEntry
import com.ella.music.data.AppLogStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun LogScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isDark = MiuixTheme.colorScheme.background.luminance() < 0.5f
    val pageBackground = if (isDark) Color(0xFF101014) else Color(0xFFF4F4F7)
    var refreshKey by remember { mutableIntStateOf(0) }
    val entries by produceState(initialValue = emptyList<AppLogEntry>(), refreshKey) {
        value = withContext(Dispatchers.IO) { AppLogStore.read(context) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(pageBackground)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        SmallTopAppBar(
            title = "日志",
            color = pageBackground,
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = MiuixIcons.Regular.Back,
                        contentDescription = "返回",
                        tint = MiuixTheme.colorScheme.onSurface
                    )
                }
            }
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "只保存 info 和闪退日志",
                    fontSize = 14.sp,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                )
                Text(
                    text = "最多保留最近 500 条",
                    fontSize = 12.sp,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary.copy(alpha = 0.72f)
                )
            }
            Button(
                onClick = {
                    scope.launch {
                        withContext(Dispatchers.IO) { AppLogStore.clear(context) }
                        refreshKey++
                    }
                }
            ) {
                Text("清空")
            }
        }

        if (entries.isEmpty()) {
            Spacer(modifier = Modifier.height(90.dp))
            Text(
                text = "暂无日志",
                fontSize = 15.sp,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                items(entries) { entry ->
                    LogEntryCard(entry = entry, isDark = isDark)
                }
                item { Spacer(modifier = Modifier.height(120.dp)) }
            }
        }
    }
}

@Composable
private fun LogEntryCard(
    entry: AppLogEntry,
    isDark: Boolean
) {
    val cardColor = if (isDark) Color(0xFF1D1D21) else Color.White
    val levelColor = if (entry.level == "CRASH") Color(0xFFFF6B6B) else MiuixTheme.colorScheme.primary
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp),
        cornerRadius = 14.dp,
        insideMargin = PaddingValues(14.dp),
        colors = CardDefaults.defaultColors(color = cardColor)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = entry.level,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = levelColor
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = entry.tag,
                    fontSize = 13.sp,
                    color = MiuixTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = AppLogStore.formatTime(entry.time),
                    fontSize = 11.sp,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = entry.message,
                fontSize = 13.sp,
                color = MiuixTheme.colorScheme.onSurface
            )
            entry.throwable?.let { stack ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stack,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                )
            }
        }
    }
}
