package com.ella.music.ui.ai

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ella.music.R
import com.ella.music.data.model.Song
import com.ella.music.ui.components.EllaMiuixTextField
import com.ella.music.ui.components.ellaPageBackground
import com.ella.music.viewmodel.MainViewModel
import com.ella.music.viewmodel.PlayerViewModel
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun AiChatScreen(
    mainViewModel: MainViewModel,
    playerViewModel: PlayerViewModel,
    onBack: () -> Unit,
    onNavigateToPlayer: () -> Unit
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val inputFocusRequester = remember { FocusRequester() }
    var input by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var messages by remember { mutableStateOf(emptyList<AiChatMessage>()) }
    val background = ellaPageBackground()

    fun playSongs(songs: List<Song>) {
        if (songs.isEmpty()) return
        playerViewModel.setPlaylist(songs, 0)
        onNavigateToPlayer()
    }

    fun addSongsToQueue(songs: List<Song>) {
        if (songs.isEmpty()) return
        playerViewModel.addToPlaylist(songs)
        Toast.makeText(context, context.getString(R.string.ai_chat_added_to_queue, songs.size), Toast.LENGTH_SHORT).show()
    }

    fun createPlaylistFromSongs(songs: List<Song>) {
        if (songs.isEmpty()) return
        val playlistName = context.getString(R.string.ai_chat_playlist_name)
        mainViewModel.createPlaylist(playlistName) { playlist ->
            if (playlist == null) return@createPlaylist
            mainViewModel.addSongsToPlaylist(playlist.id, songs, appendToEnd = true)
            Toast.makeText(
                context,
                context.getString(R.string.ai_chat_playlist_created, playlist.name, songs.size),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun send() {
        val text = input.trim()
        if (text.isBlank() || sending) return
        input = ""
        val pending = AiChatMessage(role = AiChatRole.Assistant, text = context.getString(R.string.ai_chat_thinking), songs = emptyList(), loading = true)
        messages = messages + AiChatMessage(role = AiChatRole.User, text = text, songs = emptyList()) + pending
        sending = true
        scope.launch {
            listState.animateScrollToItem(messages.lastIndex.coerceAtLeast(0))
            runCatching { mainViewModel.chatWithOpenAiLibraryAssistant(text) }
                .onSuccess { result ->
                    messages = messages.dropLast(1) + AiChatMessage(
                        role = AiChatRole.Assistant,
                        text = result.answer,
                        songs = result.songs
                    )
                }
                .onFailure { error ->
                    messages = messages.dropLast(1) + AiChatMessage(
                        role = AiChatRole.Assistant,
                        text = context.getString(R.string.ai_chat_failed, error.message ?: context.getString(R.string.common_unknown)),
                        songs = emptyList()
                    )
                    Toast.makeText(context, error.message ?: context.getString(R.string.common_unknown), Toast.LENGTH_LONG).show()
                }
            sending = false
            listState.animateScrollToItem(messages.lastIndex.coerceAtLeast(0))
        }
    }

    LaunchedEffect(Unit) {
        inputFocusRequester.requestFocus()
        keyboardController?.show()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = MiuixIcons.Regular.Back,
                    contentDescription = stringResource(R.string.common_back),
                    tint = MiuixTheme.colorScheme.onBackground,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column(modifier = Modifier.padding(start = 8.dp)) {
                Text(
                    text = stringResource(R.string.ai_chat_title),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MiuixTheme.colorScheme.onBackground
                )
                Text(
                    text = stringResource(R.string.ai_chat_disclaimer),
                    fontSize = 11.sp,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                )
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 172.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages) { message ->
                AiChatBubble(
                    message = message,
                    onPlaySongs = { playSongs(message.songs) },
                    onAddSongsToQueue = { addSongsToQueue(message.songs) },
                    onCreatePlaylist = { createPlaylistFromSongs(message.songs) }
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 160.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            EllaMiuixTextField(
                value = input,
                onValueChange = { input = it },
                label = stringResource(R.string.ai_chat_input_hint),
                singleLine = false,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(inputFocusRequester)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = { if (input.isNotBlank() && !sending) send() }) {
                Text(stringResource(R.string.ai_chat_send))
            }
        }
    }
}

@Composable
private fun AiChatBubble(
    message: AiChatMessage,
    onPlaySongs: () -> Unit,
    onAddSongsToQueue: () -> Unit,
    onCreatePlaylist: () -> Unit
) {
    val isUser = message.role == AiChatRole.User
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(if (isUser) 0.84f else 0.96f)
                .clip(RoundedCornerShape(18.dp))
                .background(
                    if (isUser) MiuixTheme.colorScheme.primary.copy(alpha = 0.18f)
                    else MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.82f)
                )
                .padding(14.dp)
        ) {
            AiMarkdownText(
                text = message.text,
                color = MiuixTheme.colorScheme.onSurface
            )
            if (message.songs.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                message.songs.take(5).forEach { song ->
                    Text(
                        text = "${song.title} · ${song.artist}",
                        fontSize = 12.sp,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Button(onClick = onPlaySongs) {
                        Text(stringResource(R.string.ai_chat_play_songs, message.songs.size))
                    }
                    Button(onClick = onAddSongsToQueue) {
                        Text(stringResource(R.string.ai_chat_add_to_queue))
                    }
                    Button(onClick = onCreatePlaylist) {
                        Text(stringResource(R.string.ai_chat_create_playlist))
                    }
                }
            }
        }
    }
}

@Composable
private fun AiMarkdownText(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        val lines = text.trim().lines().ifEmpty { listOf(text) }
        lines.forEach { rawLine ->
            val line = rawLine.trimEnd()
            if (line.isBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                return@forEach
            }
            val trimmed = line.trimStart()
            val headingLevel = trimmed.takeWhile { it == '#' }.length.takeIf { it in 1..3 && trimmed.getOrNull(it) == ' ' }
            val bullet = trimmed.removePrefix("- ").takeIf { trimmed.startsWith("- ") }
                ?: trimmed.removePrefix("* ").takeIf { trimmed.startsWith("* ") }
            val numbered = Regex("""^(\d+)[.)]\s+(.+)$""").find(trimmed)

            when {
                headingLevel != null -> {
                    Text(
                        text = inlineAiMarkdown(trimmed.drop(headingLevel + 1), MiuixTheme.colorScheme.primary),
                        fontSize = when (headingLevel) {
                            1 -> 18.sp
                            2 -> 16.sp
                            else -> 15.sp
                        },
                        lineHeight = 23.sp,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                }
                bullet != null -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("•", fontSize = 14.sp, color = color.copy(alpha = 0.78f))
                        Text(
                            text = inlineAiMarkdown(bullet, MiuixTheme.colorScheme.primary),
                            fontSize = 14.sp,
                            lineHeight = 21.sp,
                            color = color,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                numbered != null -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("${numbered.groupValues[1]}.", fontSize = 14.sp, color = color.copy(alpha = 0.78f))
                        Text(
                            text = inlineAiMarkdown(numbered.groupValues[2], MiuixTheme.colorScheme.primary),
                            fontSize = 14.sp,
                            lineHeight = 21.sp,
                            color = color,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                else -> {
                    Text(
                        text = inlineAiMarkdown(trimmed, MiuixTheme.colorScheme.primary),
                        fontSize = 14.sp,
                        lineHeight = 21.sp,
                        color = color
                    )
                }
            }
        }
    }
}

private fun inlineAiMarkdown(text: String, accent: Color) = buildAnnotatedString {
    val pattern = Regex("""(\*\*[^*]+\*\*|`[^`]+`)""")
    var cursor = 0
    pattern.findAll(text).forEach { match ->
        append(text.substring(cursor, match.range.first))
        val token = match.value
        when {
            token.startsWith("**") -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                append(token.removeSurrounding("**"))
            }
            token.startsWith("`") -> withStyle(SpanStyle(color = accent, background = accent.copy(alpha = 0.10f))) {
                append(token.removeSurrounding("`"))
            }
            else -> append(token)
        }
        cursor = match.range.last + 1
    }
    append(text.substring(cursor))
}

private enum class AiChatRole { User, Assistant }

private data class AiChatMessage(
    val role: AiChatRole,
    val text: String,
    val songs: List<Song>,
    val loading: Boolean = false
)
