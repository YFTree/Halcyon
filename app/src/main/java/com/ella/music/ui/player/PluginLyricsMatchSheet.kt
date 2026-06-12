package com.ella.music.ui.player

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ella.music.R
import com.ella.music.data.exception.WritePermissionRequiredException
import com.ella.music.data.metadata.AudioTagInfo
import com.ella.music.data.model.Song
import com.ella.music.plugin.source.LyricoPluginManager
import com.ella.music.plugin.source.PluginLyricsResult
import com.ella.music.plugin.source.PluginSearchHit
import com.ella.music.plugin.source.toEmbeddedLyricsText
import com.ella.music.ui.components.EllaMiuixTextField
import com.ella.music.viewmodel.MainViewModel
import com.ella.music.viewmodel.PlayerViewModel
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun PluginLyricsMatchSheet(
    song: Song,
    mainViewModel: MainViewModel,
    playerViewModel: PlayerViewModel,
    onDismiss: () -> Unit,
    onWritePermissionRequired: (WritePermissionRequiredException, suspend () -> Unit) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val manager = remember(context) { LyricoPluginManager(context) }
    val enabledSources by produceState(initialValue = 0, manager) {
        value = manager.enabledSources().size
    }
    var query by remember(song.id, song.path) {
        mutableStateOf(listOf(song.title, song.artist).filter { it.isNotBlank() }.joinToString(" "))
    }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var results by remember { mutableStateOf<List<PluginSearchHit>>(emptyList()) }
    var selectedHit by remember { mutableStateOf<PluginSearchHit?>(null) }
    var lyricsResult by remember { mutableStateOf<PluginLyricsResult?>(null) }
    var lyricsText by remember { mutableStateOf("") }
    var fetchingLyrics by remember { mutableStateOf(false) }

    fun writeLyrics(tags: AudioTagInfo) {
        if (song.path.startsWith("http://", true) || song.path.startsWith("https://", true)) {
            Toast.makeText(context, R.string.lyric_match_remote_not_supported, Toast.LENGTH_SHORT).show()
            return
        }
        suspend fun write() {
            val result = mainViewModel.writeSongMetadata(song, tags)
            val error = result.exceptionOrNull()
            if (error is WritePermissionRequiredException) {
                onWritePermissionRequired(error) { write() }
                return
            }
            if (result.isSuccess) {
                playerViewModel.refreshCurrentSongAfterExternalEdit(result.getOrNull())
                Toast.makeText(context, R.string.lyric_match_write_success, Toast.LENGTH_SHORT).show()
                onDismiss()
            } else {
                Toast.makeText(
                    context,
                    context.getString(R.string.lyric_match_write_failed, error?.message.orEmpty()),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        scope.launch { write() }
    }

    fun runSearch() {
        if (enabledSources <= 0) {
            message = context.getString(R.string.lyric_match_no_sources)
            return
        }
        scope.launch {
            loading = true
            message = null
            selectedHit = null
            lyricsResult = null
            lyricsText = ""
            results = manager.searchSongs(query.ifBlank { song.title })
            if (results.isEmpty()) message = context.getString(R.string.lyric_match_no_results)
            loading = false
        }
    }

    LaunchedEffect(song.id, enabledSources) {
        if (enabledSources > 0) runSearch()
        else message = context.getString(R.string.lyric_match_no_sources)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 620.dp)
            .padding(horizontal = 18.dp, vertical = 12.dp)
    ) {
        EllaMiuixTextField(
            value = query,
            onValueChange = { query = it },
            label = stringResource(R.string.lyric_match_query_label),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = { runSearch() },
            enabled = !loading && !fetchingLyrics,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(R.string.lyric_match_search))
        }
        message?.let {
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = it, color = MiuixTheme.colorScheme.onSurfaceVariantSummary, fontSize = 13.sp)
        }
        if (loading) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = stringResource(R.string.lyric_match_searching), color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
        }

        Spacer(modifier = Modifier.height(10.dp))
        LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
            items(results) { hit ->
                PluginSearchResultRow(
                    hit = hit,
                    selected = selectedHit == hit,
                    onClick = {
                        selectedHit = hit
                        fetchingLyrics = true
                        message = null
                        scope.launch {
                            lyricsResult = manager.getLyrics(hit)
                            lyricsText = lyricsResult?.toEmbeddedLyricsText().orEmpty()
                            if (lyricsText.isBlank()) message = context.getString(R.string.lyric_match_fetch_failed)
                            fetchingLyrics = false
                        }
                    }
                )
            }
        }

        if (fetchingLyrics) {
            Text(text = stringResource(R.string.lyric_match_fetching), color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
        }
        if (lyricsText.isNotBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.lyric_match_preview),
                fontWeight = FontWeight.Bold,
                color = MiuixTheme.colorScheme.onSurface
            )
            Text(
                text = lyricsText.lines().take(8).joinToString("\n"),
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                fontSize = 12.sp,
                maxLines = 8,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            if (lyricsResult?.rawTtml?.isNotBlank() == true) {
                Text(
                    text = stringResource(R.string.lyric_match_ttml_write_choice),
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Button(
                    onClick = {
                        writeLyrics(AudioTagInfo(customTags = mapOf("TTMLLYRIC" to listOf(lyricsText))))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(R.string.lyric_match_write_ttml_tag))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { writeLyrics(AudioTagInfo(lyrics = lyricsText)) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(R.string.lyric_match_write_lyrics_tag))
                }
            } else {
                Button(
                    onClick = { writeLyrics(AudioTagInfo(lyrics = lyricsText)) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(R.string.lyric_match_write_embedded))
                }
            }
        }
    }
}

@Composable
private fun PluginSearchResultRow(
    hit: PluginSearchHit,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = hit.song.title.ifBlank { hit.song.id },
                color = if (selected) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurface,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = listOf(hit.song.artist, hit.song.album).filter { it.isNotBlank() }.joinToString(" · "),
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(text = hit.sourceName, color = MiuixTheme.colorScheme.onSurfaceVariantSummary, fontSize = 12.sp)
    }
}
