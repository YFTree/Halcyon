package com.ella.music.ui.ai

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
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
import com.ella.music.ui.components.ConfirmDangerDialog
import com.ella.music.ui.components.EllaMiuixTextField
import com.ella.music.ui.components.ellaPageBackground
import com.ella.music.viewmodel.MainViewModel
import com.ella.music.viewmodel.PlayerViewModel
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Play
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.io.File
import java.util.UUID

private const val MAX_SESSIONS = 20
private const val MAX_MESSAGES_PER_SESSION = 100

// ── Session persistence ──

private data class AiChatSession(
    val id: String,
    val title: String,
    val createdAt: Long,
    val messages: List<AiChatMessage>
)

private fun sessionsDir(context: Context): File =
    File(context.filesDir, "ai_chat_sessions").also { it.mkdirs() }

private fun defaultSessionTitle(context: Context): String =
    context.getString(R.string.ai_chat_new_session)

private fun String.isDefaultAiChatSessionTitle(): Boolean =
    trim() in setOf("New chat", "New Chat", "新对话", "新對話", "新しい会話")

private fun loadSessionIndex(context: Context): List<AiChatSessionMeta> {
    return runCatching {
        val file = File(sessionsDir(context), "index.json")
        if (!file.exists()) return@runCatching emptyList()
        val array = JSONArray(file.readText())
        (0 until array.length()).map { i ->
            val obj = array.getJSONObject(i)
            val rawTitle = obj.getString("title")
            AiChatSessionMeta(
                id = obj.getString("id"),
                title = if (rawTitle.isDefaultAiChatSessionTitle()) defaultSessionTitle(context) else rawTitle,
                createdAt = obj.getLong("createdAt")
            )
        }
    }.getOrDefault(emptyList())
}

private fun saveSessionIndex(context: Context, index: List<AiChatSessionMeta>) {
    runCatching {
        val array = JSONArray()
        index.forEach { meta ->
            array.put(JSONObject().put("id", meta.id).put("title", meta.title).put("createdAt", meta.createdAt))
        }
        File(sessionsDir(context), "index.json").writeText(array.toString())
    }
}

private fun loadSessionMessages(context: Context, sessionId: String): List<AiChatMessage> {
    return runCatching {
        val file = File(sessionsDir(context), "$sessionId.json")
        if (!file.exists()) return@runCatching emptyList()
        val array = JSONArray(file.readText())
        (0 until array.length()).map { i ->
            val obj = array.getJSONObject(i)
            AiChatMessage(
                role = if (obj.getString("role") == "user") AiChatRole.User else AiChatRole.Assistant,
                text = obj.getString("text"),
                songs = emptyList(),
                loading = false
            )
        }
    }.getOrDefault(emptyList())
}

private fun saveSessionMessages(context: Context, sessionId: String, messages: List<AiChatMessage>) {
    runCatching {
        val array = JSONArray()
        messages.filter { !it.loading }.takeLast(MAX_MESSAGES_PER_SESSION).forEach { msg ->
            array.put(
                JSONObject()
                    .put("role", if (msg.role == AiChatRole.User) "user" else "assistant")
                    .put("text", msg.text)
            )
        }
        File(sessionsDir(context), "$sessionId.json").writeText(array.toString())
    }
}

private fun deleteSession(context: Context, sessionId: String) {
    runCatching {
        File(sessionsDir(context), "$sessionId.json").delete()
    }
}

private data class AiChatSessionMeta(val id: String, val title: String, val createdAt: Long)

// ── Composable ──

@OptIn(ExperimentalLayoutApi::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
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
    val background = ellaPageBackground()

    // ── Multi-session state ──
    var sessionIndex by remember { mutableStateOf(loadSessionIndex(context)) }
    var currentSessionId by remember { mutableStateOf<String?>(null) }
    var messages by remember { mutableStateOf(emptyList<AiChatMessage>()) }
    var showDeleteDialog by remember { mutableStateOf<AiChatSessionMeta?>(null) }

    // Initialize: load or create first session
    LaunchedEffect(Unit) {
        if (sessionIndex.isEmpty()) {
            val id = UUID.randomUUID().toString()
            val meta = AiChatSessionMeta(id, defaultSessionTitle(context), System.currentTimeMillis())
            sessionIndex = listOf(meta)
            currentSessionId = id
            messages = emptyList()
            saveSessionIndex(context, sessionIndex)
        } else {
            val first = sessionIndex.first()
            currentSessionId = first.id
            messages = loadSessionMessages(context, first.id)
        }
    }

    // Save messages when they change
    LaunchedEffect(messages, currentSessionId) {
        currentSessionId?.let { id ->
            saveSessionMessages(context, id, messages)
            // Update session title from first user message
            val firstUserMsg = messages.firstOrNull { it.role == AiChatRole.User && !it.loading }
            if (firstUserMsg != null) {
                val newTitle = firstUserMsg.text.take(20).replace("\n", " ")
                val updatedIndex = sessionIndex.map {
                    if (it.id == id && it.title.isDefaultAiChatSessionTitle()) it.copy(title = newTitle) else it
                }
                if (updatedIndex != sessionIndex) {
                    sessionIndex = updatedIndex
                    saveSessionIndex(context, sessionIndex)
                }
            }
        }
    }

    fun switchSession(sessionId: String) {
        if (sessionId == currentSessionId) return
        currentSessionId = sessionId
        messages = loadSessionMessages(context, sessionId)
    }

    fun createNewSession() {
        val id = UUID.randomUUID().toString()
        val meta = AiChatSessionMeta(id, defaultSessionTitle(context), System.currentTimeMillis())
        sessionIndex = listOf(meta) + sessionIndex
        currentSessionId = id
        messages = emptyList()
        saveSessionIndex(context, sessionIndex)
    }

    fun deleteSession(meta: AiChatSessionMeta) {
        deleteSession(context, meta.id)
        sessionIndex = sessionIndex.filter { it.id != meta.id }
        saveSessionIndex(context, sessionIndex)
        if (currentSessionId == meta.id) {
            val next = sessionIndex.firstOrNull()
            currentSessionId = next?.id
            messages = next?.let { loadSessionMessages(context, it.id) } ?: emptyList()
        }
    }

    // ── Actions ──

    fun playSongs(songs: List<Song>) {
        if (songs.isEmpty()) return
        playerViewModel.setPlaylist(songs, 0)
        onNavigateToPlayer()
    }

    fun playSingleSong(song: Song) {
        playerViewModel.setPlaylist(listOf(song), 0)
        onNavigateToPlayer()
    }

    fun addSongsToQueue(songs: List<Song>) {
        if (songs.isEmpty()) return
        playerViewModel.addToPlaylist(songs)
        Toast.makeText(context, context.getString(R.string.ai_chat_added_to_queue, songs.size), Toast.LENGTH_SHORT).show()
    }

    fun createPlaylistFromSongs(songs: List<Song>, playlistName: String = "") {
        if (songs.isEmpty()) return
        val name = playlistName.ifBlank { context.getString(R.string.ai_chat_playlist_name) }
        mainViewModel.createPlaylist(name) { playlist ->
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
            val history = messages.dropLast(1).filter { !it.loading }.flatMap { msg ->
                val role = if (msg.role == AiChatRole.User) "user" else "assistant"
                listOf(role to msg.text)
            }
            runCatching { mainViewModel.chatWithOpenAiLibraryAssistant(text, conversationHistory = history) }
                .onSuccess { result ->
                    messages = messages.dropLast(1) + AiChatMessage(
                        role = AiChatRole.Assistant,
                        text = result.answer,
                        songs = result.songs,
                        playlistName = result.playlistName
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

    // ── Delete confirmation dialog ──
    showDeleteDialog?.let { meta ->
        ConfirmDangerDialog(
            show = true,
            title = stringResource(R.string.ai_chat_delete_session),
            message = stringResource(R.string.ai_chat_delete_session_confirm),
            onDismiss = { showDeleteDialog = null },
            onConfirm = {
                deleteSession(meta)
                showDeleteDialog = null
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        // ── Top bar ──
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
            Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
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

        // ── Session selector ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            sessionIndex.forEach { meta ->
                val selected = meta.id == currentSessionId
                val chipBackground = if (selected) MiuixTheme.colorScheme.primary.copy(alpha = 0.18f)
                    else MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.6f)
                val chipTextColor = if (selected) MiuixTheme.colorScheme.primary
                    else MiuixTheme.colorScheme.onSurfaceVariantSummary
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(chipBackground)
                        .combinedClickable(
                            onClick = { switchSession(meta.id) },
                            onLongClick = { showDeleteDialog = meta }
                        )
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = meta.title,
                        fontSize = 13.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        color = chipTextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (sessionIndex.size < MAX_SESSIONS) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.6f))
                        .clickable { createNewSession() }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = MiuixIcons.Regular.Add,
                        contentDescription = stringResource(R.string.ai_chat_new_session),
                        tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // ── Messages + Input (Box layout to handle mini player overlay) ──
        Box(modifier = Modifier.weight(1f)) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 160.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(messages) { message ->
                    AiChatBubble(
                    message = message,
                    onPlaySongs = { playSongs(message.songs) },
                    onPlaySingleSong = { playSingleSong(it) },
                    onAddSongsToQueue = { addSongsToQueue(message.songs) },
                    onCreatePlaylist = { createPlaylistFromSongs(message.songs, message.playlistName) }
                )
            }
        }

        // ── Input bar (positioned above mini player overlay) ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .imePadding()
                .navigationBarsPadding()
                .padding(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 72.dp),
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
        } // end Box
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AiChatBubble(
    message: AiChatMessage,
    onPlaySongs: () -> Unit,
    onPlaySingleSong: (Song) -> Unit,
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
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${song.title} · ${song.artist}",
                            fontSize = 12.sp,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { onPlaySingleSong(song) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = MiuixIcons.Regular.Play,
                                contentDescription = "Play",
                                tint = MiuixTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Button(onClick = onPlaySongs) {
                        Text(
                            stringResource(R.string.ai_chat_play_songs, message.songs.size),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Button(onClick = onAddSongsToQueue) {
                        Text(
                            stringResource(R.string.ai_chat_add_to_queue),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Button(onClick = onCreatePlaylist) {
                        Text(
                            stringResource(R.string.ai_chat_create_playlist),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
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
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(5.dp)) {
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
                        color = color,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                bullet != null -> {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
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
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
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
                        color = color,
                        modifier = Modifier.fillMaxWidth()
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
    val loading: Boolean = false,
    val playlistName: String = ""
)
