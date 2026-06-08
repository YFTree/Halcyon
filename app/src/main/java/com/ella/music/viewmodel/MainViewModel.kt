package com.ella.music.viewmodel

import android.app.Application
import android.net.Uri
import com.ella.music.R
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ella.music.data.PlaylistBatchImportResult
import com.ella.music.data.PlaylistBatchExportResult
import com.ella.music.data.PlaylistExportResult
import com.ella.music.data.PlaylistExportFormat
import com.ella.music.data.PlaylistImportResult
import com.ella.music.data.PlaylistImportMode
import com.ella.music.data.PlaylistStore
import com.ella.music.data.SettingsManager
import com.ella.music.data.PlaybackHistoryEntry
import com.ella.music.data.PlaybackStatsStore
import com.ella.music.data.SongPlaybackStats
import com.ella.music.data.NameSplitConfigStore
import com.ella.music.data.decodeNeteaseKey
import com.ella.music.data.matchesArtistName
import com.ella.music.data.neteaseAlbumUrl
import com.ella.music.data.neteaseArtistUrl
import com.ella.music.data.ai.OpenAiSongInterpretationConfig
import com.ella.music.data.ai.OpenAiLibraryChatAssistant
import com.ella.music.data.ai.OpenAiLibraryChatInput
import com.ella.music.data.ai.OpenAiSongInterpretationInput
import com.ella.music.data.ai.OpenAiSongInterpreter
import com.ella.music.data.ai.OpenAiPlaylistRecommendationInput
import com.ella.music.data.ai.OpenAiPlaylistRecommender
import com.ella.music.data.detailedAudioInfo
import com.ella.music.data.model.Album
import com.ella.music.data.model.Artist
import com.ella.music.data.model.AudioInfo
import com.ella.music.data.model.Song
import com.ella.music.data.model.SongTagInfo
import com.ella.music.data.metadata.AudioTagInfo
import com.ella.music.data.model.UserPlaylist
import com.ella.music.data.model.playlistIdentityKey
import com.ella.music.data.model.toSong
import com.ella.music.data.model.albumIdentityId
import com.ella.music.data.parseNameSplitSetting
import com.ella.music.data.repository.CoverUsage
import com.ella.music.data.repository.MusicRepository
import com.ella.music.data.splitGenreNames
import com.ella.music.data.splitArtistNames
import com.ella.music.data.tagIdentityKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val repository = MusicRepository.getInstance(application)
    val settingsManager = SettingsManager.getInstance(application)
    private val playlistStore = PlaylistStore.getInstance(application)
    private val playbackStatsStore = PlaybackStatsStore.getInstance(application)
    private val openAiSongInterpreter = OpenAiSongInterpreter(getApplication())
    private val openAiPlaylistRecommender = OpenAiPlaylistRecommender(getApplication())
    private val openAiLibraryChatAssistant = OpenAiLibraryChatAssistant(getApplication())

    val songs: StateFlow<List<Song>> = repository.songs
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val albums: StateFlow<List<Album>> = repository.albums
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val isScanning: StateFlow<Boolean> = repository.isScanning
    val scanProgress: StateFlow<Int> = repository.scanProgress
    val playbackStats: StateFlow<List<SongPlaybackStats>> = playbackStatsStore.stats
    val playbackHistory: StateFlow<List<PlaybackHistoryEntry>> = playbackStatsStore.history
    val dailyListenMs: StateFlow<Map<String, Long>> = playbackStatsStore.dailyListenMs
    val playlists: StateFlow<List<UserPlaylist>> = playlistStore.playlists
    private val _libraryCacheLoaded = MutableStateFlow(false)
    val libraryCacheLoaded: StateFlow<Boolean> = _libraryCacheLoaded.asStateFlow()
    private val _ratingRevision = MutableStateFlow(0)
    val ratingRevision: StateFlow<Int> = _ratingRevision.asStateFlow()

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()
    private var scanJob: Job? = null
    private var autoScanRequested = false

    init {
        viewModelScope.launch {
            settingsManager.artistSeparators.collect {
                NameSplitConfigStore.artistCustomSeparators = parseNameSplitSetting(it)
            }
        }
        viewModelScope.launch {
            settingsManager.artistProtectedNames.collect {
                NameSplitConfigStore.artistProtectedNames = parseNameSplitSetting(it)
            }
        }
        viewModelScope.launch {
            settingsManager.genreSeparators.collect {
                NameSplitConfigStore.genreCustomSeparators = parseNameSplitSetting(it)
            }
        }
        viewModelScope.launch {
            settingsManager.genreProtectedNames.collect {
                NameSplitConfigStore.genreProtectedNames = parseNameSplitSetting(it)
            }
        }
        viewModelScope.launch {
            settingsManager.tagIgnoreCase.collect {
                NameSplitConfigStore.tagIgnoreCase = it
            }
        }
    }

    fun selectTab(index: Int) {
        _selectedTab.value = index
    }

    fun scanMusic(fullRescan: Boolean = false, deepRescan: Boolean = fullRescan) {
        if (scanJob?.isActive == true || isScanning.value) return
        scanJob = viewModelScope.launch {
            scanFromCurrentSettings(fullRescan = fullRescan, deepRescan = deepRescan)
        }
    }

    fun fullRescanMusic() {
        scanMusic(fullRescan = true, deepRescan = true)
    }

    fun scanMusicIfAutoEnabled() {
        if (autoScanRequested) return
        autoScanRequested = true
        if (scanJob?.isActive == true || isScanning.value) return
        scanJob = viewModelScope.launch {
            if (!settingsManager.autoScan.first()) return@launch
            scanFromCurrentSettings(fullRescan = false, deepRescan = false)
        }
    }

    private suspend fun scanFromCurrentSettings(fullRescan: Boolean = false, deepRescan: Boolean = fullRescan) {
        val minDuration = settingsManager.minDurationSec.first() * 1000L
        val includeFolders = settingsManager.scanIncludeFolders.first().toFolderFilterList()
        val excludeFolders = settingsManager.scanExcludeFolders.first().toFolderFilterList()
        val useAndroidMediaLibrary = settingsManager.useAndroidMediaLibrary.first()
        val count = repository.scanMusic(
            minDuration,
            if (useAndroidMediaLibrary) emptyList() else includeFolders.ifEmpty { listOf("__ella_no_custom_folder__") },
            excludeFolders,
            fullRescan = fullRescan,
            deepRescan = deepRescan
        )
        if (count == 0 && useAndroidMediaLibrary && includeFolders.isNotEmpty()) {
            repository.scanMusic(
                minDuration,
                includeFolders,
                excludeFolders,
                fullRescan = fullRescan,
                deepRescan = deepRescan
            )
        }
        // Scan USB folders via SAF
        val usbFolderUris = settingsManager.usbFolderUris.first()
            .split('\n')
            .map { it.trim() }
            .filter { it.isNotBlank() }
        if (usbFolderUris.isNotEmpty()) {
            val uris = usbFolderUris.mapNotNull { runCatching { Uri.parse(it) }.getOrNull() }
            repository.scanUsbFolders(
                usbUris = uris,
                minDurationMs = minDuration,
                deepMetadata = deepRescan
            )
        }
        preloadLibrarySearchSnapshot()
    }

    fun loadCachedLibrary() {
        viewModelScope.launch {
            repository.loadCachedLibrary()
            _libraryCacheLoaded.value = true
            preloadLibrarySearchSnapshot()
        }
    }

    fun preloadLibrarySearchSnapshot() {
        val currentSongs = songs.value
        if (currentSongs.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            repository.preloadLibrarySearchSnapshot(currentSongs)
        }
    }

    fun getSongsForAlbum(albumId: Long): List<Song> {
        return repository.getSongsForAlbum(albumId)
    }

    fun getArtists(includeAlbumArtists: Boolean = false): List<Artist> {
        val currentSongs = songs.value
        val currentAlbums = albums.value
        val counts = linkedMapOf<String, ArtistAccumulator>()
        val albumIdsByArtist = mutableMapOf<String, MutableSet<Long>>()

        currentSongs.forEach { song ->
            splitArtistNames(song.artist).forEach { rawName ->
                val key = rawName.tagIdentityKey()
                val accumulator = counts.getOrPut(key) { ArtistAccumulator(rawName) }
                accumulator.songCount += 1
                albumIdsByArtist.getOrPut(key) { mutableSetOf() } += song.albumIdentityId()
            }
            if (includeAlbumArtists) {
                splitArtistNames(song.albumArtist).forEach { rawName ->
                    val key = rawName.tagIdentityKey()
                    counts.getOrPut(key) { ArtistAccumulator(rawName) }
                    albumIdsByArtist.getOrPut(key) { mutableSetOf() } += song.albumIdentityId()
                }
            }
        }

        if (includeAlbumArtists) {
            currentAlbums.forEach { album ->
                splitArtistNames(album.albumArtist).forEach { rawName ->
                    val key = rawName.tagIdentityKey()
                    counts.getOrPut(key) { ArtistAccumulator(rawName) }
                    if (album.id > 0L) {
                        albumIdsByArtist.getOrPut(key) { mutableSetOf() } += album.id
                    }
                }
            }
        }

        return counts
            .map { (key, accumulator) ->
                Artist(
                    name = accumulator.name,
                    songCount = accumulator.songCount,
                    albumCount = albumIdsByArtist[key]?.size ?: 0
                )
            }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
    }

    fun getSongsForArtist(artistName: String): List<Song> {
        return songs.value.filter { it.artist.matchesArtistName(artistName) }
    }

    fun getAlbumsForArtist(artistName: String): List<Album> {
        return getParticipatedAlbumsForArtist(artistName)
    }

    fun getParticipatedAlbumsForArtist(artistName: String): List<Album> {
        val artistSongs = getSongsForArtist(artistName)
        val artistAlbumIds = artistSongs.map { it.albumIdentityId() }.toSet()
        return albums.value
            .filter { it.id in artistAlbumIds }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
    }

    fun getReleaseAlbumsForArtist(artistName: String): List<Album> {
        return albums.value
            .filter { it.albumArtist.isNotBlank() && it.albumArtist.matchesArtistName(artistName) }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
    }

    fun hasAlbumArtistTags(): Boolean {
        return songs.value.any { it.albumArtist.isNotBlank() } || albums.value.any { it.albumArtist.isNotBlank() }
    }

    fun getMetadataCategoryItems(type: String): List<MetadataCategoryItem> {
        val groups = linkedMapOf<String, MutableList<Song>>()
        val displayNames = linkedMapOf<String, String>()
        songs.value.forEach { song ->
            song.metadataCategoryNames(type).forEach { name ->
                val key = name.tagIdentityKey()
                displayNames.putIfAbsent(key, name)
                groups.getOrPut(key) { mutableListOf() } += song
            }
        }
        return groups
            .map { (key, items) ->
                MetadataCategoryItem(
                    name = displayNames[key] ?: key,
                    songCount = items.size,
                    albumCount = items.map { it.albumIdentityId() }.distinct().size,
                    duration = items.sumOf { it.duration },
                    dateModified = items.maxOfOrNull { it.dateModified } ?: 0L,
                    coverAlbumIds = items
                        .mapNotNull { it.albumId.takeIf { albumId -> albumId > 0L } }
                        .distinct()
                        .take(3)
                )
            }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
    }

    fun getSongsForMetadataCategory(type: String, name: String): List<Song> {
        val target = name.trim()
        if (target.isBlank()) return emptyList()
        return songs.value
            .filter { song -> song.metadataCategoryNames(type).any { it.equals(target, ignoreCase = NameSplitConfigStore.tagIgnoreCase) } }
            .sortedWith(
                compareBy<Song, String>(String.CASE_INSENSITIVE_ORDER) { it.album }
                    .thenBy { if (it.discNumber > 0) it.discNumber else Int.MAX_VALUE }
                    .thenBy { if (it.trackNumber > 0) it.trackNumber else Int.MAX_VALUE }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { song -> song.title }
            )
    }

    fun hasMetadataCategory(type: String, name: String): Boolean {
        val target = name.trim()
        if (target.isBlank()) return false
        return songs.value.any { song ->
            song.metadataCategoryNames(type).any { it.equals(target, ignoreCase = NameSplitConfigStore.tagIgnoreCase) }
        }
    }

    suspend fun getNeteaseArtistUrlForArtist(artistName: String): String? = withContext(Dispatchers.IO) {
        val targetNames = splitArtistNames(artistName)
            .ifEmpty { listOf(artistName.trim()) }
            .filter { it.isNotBlank() }
        val targetKeys = targetNames.map { it.tagIdentityKey() }.toSet()
        val matchedArtist = getSongsForArtist(artistName).asSequence()
            .take(80)
            .mapNotNull { song -> decodeNeteaseKey(repository.getSongTagInfo(song).neteaseKey) }
            .flatMap { it.artists.asSequence() }
            .firstOrNull { artist ->
                artist.id.isNotBlank() && artist.name.tagIdentityKey() in targetKeys
            }
            ?: getSongsForArtist(artistName).asSequence()
                .take(80)
                .mapNotNull { song -> decodeNeteaseKey(repository.getSongTagInfo(song).neteaseKey) }
                .flatMap { it.artists.asSequence() }
                .firstOrNull { artist ->
                    artist.id.isNotBlank() && targetNames.any { target ->
                        artist.name.equals(target, ignoreCase = true) ||
                            (artist.name.length >= 3 && target.contains(artist.name, ignoreCase = true)) ||
                            (target.length >= 3 && artist.name.contains(target, ignoreCase = true))
                    }
            }
        matchedArtist?.id?.let(::neteaseArtistUrl)
    }

    suspend fun getNeteaseAlbumUrlForAlbum(albumId: Long): String? = withContext(Dispatchers.IO) {
        getSongsForAlbum(albumId).asSequence()
            .take(40)
            .mapNotNull { song -> decodeNeteaseKey(repository.getSongTagInfo(song).neteaseKey) }
            .firstOrNull { it.albumId.isNotBlank() }
            ?.albumId
            ?.let(::neteaseAlbumUrl)
    }

    fun getAlbumArtUri(albumId: Long) = repository.getAlbumArtUri(albumId)

    fun getCoverArtBitmap(song: Song) = repository.getCoverArtBitmap(song, 128, CoverUsage.ListThumbnail)

    fun getAlbumCoverArtBitmap(song: Song) = repository.getCoverArtBitmap(song, 512, CoverUsage.AlbumGrid)

    fun getLargeCoverArtBitmap(song: Song) = repository.getCoverArtBitmap(song, 1200, CoverUsage.Player)

    fun getReplayGain(song: Song): Float? {
        return repository.getReplayGain(song)
    }

    fun getAudioInfo(song: Song): AudioInfo {
        return repository.getAudioInfo(song)
    }

    fun getSongTagInfo(song: Song): SongTagInfo {
        return repository.getSongTagInfo(song)
    }

    suspend fun getFiveStarSongs(): List<Song> = withContext(Dispatchers.IO) {
        songs.value.filter { repository.getSongRating(it) >= 5 }
    }

    fun getSongRating(song: Song): Int = repository.getSongRating(song)

    suspend fun writeSongRating(song: Song, rating: Int): Result<Song?> {
        val result = repository.writeSongRating(song, rating)
        if (result.isSuccess) {
            _ratingRevision.value += 1
        }
        return result
    }

    suspend fun writeSongCustomTag(song: Song, key: String, value: String): Result<Song?> =
        repository.writeSongCustomTag(song, key, value)

    suspend fun writeSongMetadata(song: Song, tags: AudioTagInfo): Result<Song?> {
        val result = repository.writeSongMetadata(song, tags)
        if (result.isSuccess && tags.rating != null) {
            _ratingRevision.value += 1
        }
        return result
    }

    fun getFullAudioTagInfo(song: Song): AudioTagInfo? =
        repository.getFullAudioTagInfo(song)

    suspend fun interpretSongWithOpenAi(song: Song): String = withContext(Dispatchers.IO) {
        val lyricSourceMode = settingsManager.lyricSourceMode.first()
        val tagInfo = repository.getSongTagInfo(song)
        val audioInfo = runCatching { repository.getAudioInfo(song) }.getOrNull()
        val lyrics = repository.getLyrics(song, lyricSourceMode)
        openAiSongInterpreter.interpret(
            config = OpenAiSongInterpretationConfig(
                apiKey = settingsManager.openAiApiKey.first(),
                baseUrl = settingsManager.openAiBaseUrl.first(),
                model = settingsManager.openAiModel.first()
            ),
            input = OpenAiSongInterpretationInput(
                song = song,
                tagInfo = tagInfo,
                audioInfo = audioInfo,
                audioInfoText = audioInfo?.let { detailedAudioInfo(it) }.orEmpty(),
                lyrics = lyrics
            )
        )
    }

    suspend fun recommendPlaylistWithOpenAi(maxItems: Int = 30): AiPlaylistRecommendationResult = withContext(Dispatchers.IO) {
        val librarySongs = songs.value
        val currentStats = playbackStats.value
        val currentHistory = playbackHistory.value
        if (librarySongs.isEmpty()) error(getApplication<android.app.Application>().getString(R.string.error_library_empty))

        val candidates = buildOpenAiRecommendationCandidates(
            library = librarySongs,
            stats = currentStats,
            history = currentHistory
        )
        val recommendation = openAiPlaylistRecommender.recommend(
            config = OpenAiSongInterpretationConfig(
                apiKey = settingsManager.openAiApiKey.first(),
                baseUrl = settingsManager.openAiBaseUrl.first(),
                model = settingsManager.openAiModel.first()
            ),
            input = OpenAiPlaylistRecommendationInput(
                songs = candidates,
                playbackStats = currentStats,
                playbackHistory = currentHistory,
                maxItems = maxItems.coerceIn(5, 50)
            )
        )
        val libraryByKey = librarySongs.associateBy { it.playlistIdentityKey() }
        val recommendedSongs = recommendation.songKeys
            .mapNotNull { key -> libraryByKey[key] }
            .distinctBy { it.playlistIdentityKey() }
            .take(maxItems.coerceAtLeast(1))
        if (recommendedSongs.isEmpty()) error(getApplication<android.app.Application>().getString(R.string.error_ai_no_playable_songs))

        AiPlaylistRecommendationResult(
            title = recommendation.title.ifBlank { getApplication<android.app.Application>().getString(R.string.ai_default_playlist_title) },
            reason = recommendation.reason,
            songs = recommendedSongs
        )
    }

    suspend fun chatWithOpenAiLibraryAssistant(
        message: String,
        conversationHistory: List<Pair<String, String>> = emptyList(),
        maxPlayableItems: Int = 30
    ): AiLibraryChatResult = withContext(Dispatchers.IO) {
        val librarySongs = songs.value
        if (librarySongs.isEmpty()) error(getApplication<android.app.Application>().getString(R.string.error_library_empty))
        val candidates = buildOpenAiRecommendationCandidates(
            library = librarySongs,
            stats = playbackStats.value,
            history = playbackHistory.value
        )
        val response = openAiLibraryChatAssistant.chat(
            config = OpenAiSongInterpretationConfig(
                apiKey = settingsManager.openAiApiKey.first(),
                baseUrl = settingsManager.openAiBaseUrl.first(),
                model = settingsManager.openAiModel.first()
            ),
            input = OpenAiLibraryChatInput(
                songs = candidates,
                playbackStats = playbackStats.value,
                playbackHistory = playbackHistory.value,
                userMessage = message,
                maxPlayableItems = maxPlayableItems.coerceIn(1, 50),
                conversationHistory = conversationHistory
            )
        )
        val libraryByKey = librarySongs.associateBy { it.playlistIdentityKey() }
        AiLibraryChatResult(
            answer = response.answer,
            songs = response.songKeys
                .mapNotNull { key -> libraryByKey[key] }
                .distinctBy { it.playlistIdentityKey() }
                .take(maxPlayableItems.coerceAtLeast(1)),
            playlistName = response.playlistName.ifBlank { getApplication<android.app.Application>().getString(R.string.ai_chat_playlist_name) }
        )
    }

    fun clearOnlineMetadataCache() {
        repository.clearRemoteMetadataCache()
    }

    suspend fun prefetchWebDavMetadataHeaders(songs: List<Song>, maxItems: Int = 80) {
        repository.prefetchWebDavMetadataHeaders(songs, maxItems)
    }

    suspend fun resolveSongForPlayback(song: Song): Song =
        repository.resolveSongForPlayback(song)

    suspend fun songMatchesSearchSnapshot(song: Song, query: String): Boolean =
        repository.songMatchesSearchSnapshot(song, query)

    fun clearLibrarySnapshotCache() {
        viewModelScope.launch {
            repository.clearLibrarySnapshotCache()
        }
    }

    fun refreshSongAfterExternalEdit(song: Song, onUpdated: (Song?) -> Unit = {}) {
        viewModelScope.launch {
            onUpdated(repository.refreshSongAfterExternalEdit(song))
        }
    }

    fun playlistSongs(playlist: UserPlaylist): List<Song> {
        val libraryByKey = songs.value.associateBy { it.playlistIdentityKey() }
        return playlist.songs.map { item -> libraryByKey[item.key] ?: item.toSong() }
    }

    fun createPlaylist(name: String, onCreated: (UserPlaylist?) -> Unit = {}) {
        viewModelScope.launch {
            onCreated(playlistStore.createPlaylist(name))
        }
    }

    fun deletePlaylist(id: String) {
        viewModelScope.launch { playlistStore.deletePlaylist(id) }
    }

    fun deletePlaylists(ids: Set<String>) {
        if (ids.isEmpty()) return
        viewModelScope.launch { playlistStore.deletePlaylists(ids) }
    }

    fun removeSongFromPlaylist(playlistId: String, songKey: String) {
        viewModelScope.launch { playlistStore.removeSongFromPlaylist(playlistId, songKey) }
    }

    fun removeSongsFromPlaylist(playlistId: String, songKeys: Set<String>) {
        if (songKeys.isEmpty()) return
        viewModelScope.launch { playlistStore.removeSongsFromPlaylist(playlistId, songKeys) }
    }

    fun addSongsToPlaylist(playlistId: String, songs: Collection<Song>, appendToEnd: Boolean = false) {
        if (songs.isEmpty()) return
        viewModelScope.launch { playlistStore.addSongsToPlaylist(playlistId, songs, appendToEnd) }
    }

    fun reorderPlaylistSongs(playlistId: String, orderedKeys: List<String>) {
        if (orderedKeys.isEmpty()) return
        viewModelScope.launch { playlistStore.reorderPlaylistSongs(playlistId, orderedKeys) }
    }

    fun reorderPlaylists(orderedIds: List<String>) {
        if (orderedIds.isEmpty()) return
        viewModelScope.launch { playlistStore.reorderPlaylists(orderedIds) }
    }

    fun importLocalPlaylist(uri: Uri, onResult: (Result<PlaylistImportResult>) -> Unit) {
        viewModelScope.launch {
            val result = runCatching { playlistStore.importLocalPlaylist(uri, songs.value) }
            onResult(result)
        }
    }

    fun importLocalPlaylists(
        uris: List<Uri>,
        mode: PlaylistImportMode = PlaylistImportMode.MergeKeepExisting,
        onResult: (Result<PlaylistBatchImportResult>) -> Unit
    ) {
        viewModelScope.launch {
            val result = runCatching { playlistStore.importLocalPlaylists(uris, songs.value, mode) }
            onResult(result)
        }
    }

    fun scanLocalPlaylistFiles(
        onResult: (Result<PlaylistBatchImportResult>) -> Unit = {}
    ) {
        viewModelScope.launch {
            val result = runCatching { playlistStore.importLocalPlaylistFiles(songs.value) }
            onResult(result)
        }
    }

    fun exportLocalPlaylist(
        playlist: UserPlaylist,
        uri: Uri,
        format: PlaylistExportFormat = PlaylistExportFormat.PlainText,
        onResult: (Result<PlaylistExportResult>) -> Unit
    ) {
        viewModelScope.launch {
            val result = runCatching { playlistStore.exportLocalPlaylist(playlist, uri, format) }
            onResult(result)
        }
    }

    fun exportLocalPlaylists(
        playlists: List<UserPlaylist>,
        treeUri: Uri,
        format: PlaylistExportFormat = PlaylistExportFormat.PlainText,
        onResult: (Result<PlaylistBatchExportResult>) -> Unit
    ) {
        viewModelScope.launch {
            val result = runCatching { playlistStore.exportLocalPlaylists(playlists, treeUri, format) }
            onResult(result)
        }
    }

    fun deleteSongs(songs: Collection<Song>) {
        if (songs.isEmpty()) return
        viewModelScope.launch {
            runCatching { repository.deleteSongs(songs) }
        }
    }

    suspend fun deleteSongsResult(songs: Collection<Song>): Result<Int> {
        if (songs.isEmpty()) return Result.success(0)
        return runCatching { repository.deleteSongs(songs) }
    }

    fun removeSongsFromLibrary(songs: Collection<Song>) {
        if (songs.isEmpty()) return
        viewModelScope.launch {
            repository.removeSongsFromLibrary(songs)
        }
    }

    private fun String.toFolderFilterList(): List<String> {
        return split('\n', ';', '；')
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }

    private fun buildOpenAiRecommendationCandidates(
        library: List<Song>,
        stats: List<SongPlaybackStats>,
        history: List<PlaybackHistoryEntry>,
        maxCandidates: Int = 160
    ): List<Song> {
        if (library.size <= maxCandidates) return library.distinctBy { it.playlistIdentityKey() }

        val songsById = library.associateBy { it.id }
        val selected = linkedMapOf<String, Song>()

        fun add(song: Song) {
            if (selected.size >= maxCandidates) return
            selected.putIfAbsent(song.playlistIdentityKey(), song)
        }

        history
            .mapNotNull { entry -> songsById[entry.songId] }
            .take(60)
            .forEach(::add)

        stats
            .sortedWith(
                compareByDescending<SongPlaybackStats> { it.playCount }
                    .thenByDescending { it.listenedMs }
                    .thenByDescending { it.lastPlayedAt }
            )
            .mapNotNull { stat -> songsById[stat.songId] }
            .take(60)
            .forEach(::add)

        stats
            .sortedByDescending { it.lastPlayedAt }
            .mapNotNull { stat -> songsById[stat.songId] }
            .take(40)
            .forEach(::add)

        library
            .sortedByDescending { it.dateModified }
            .take(40)
            .forEach(::add)

        val remaining = maxCandidates - selected.size
        if (remaining > 0) {
            val sortedLibrary = library.sortedWith(
                compareBy<Song, String>(String.CASE_INSENSITIVE_ORDER) { it.artist.ifBlank { it.albumArtist } }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { it.album }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { it.title }
            )
            val step = (sortedLibrary.size / remaining.coerceAtLeast(1)).coerceAtLeast(1)
            sortedLibrary.forEachIndexed { index, song ->
                if (selected.size < maxCandidates && index % step == 0) add(song)
            }
        }

        return selected.values.toList().ifEmpty { library.take(maxCandidates) }
    }
}

data class AiPlaylistRecommendationResult(
    val title: String,
    val reason: String,
    val songs: List<Song>
)

data class AiLibraryChatResult(
    val answer: String,
    val songs: List<Song>,
    val playlistName: String
)

data class MetadataCategoryItem(
    val name: String,
    val songCount: Int,
    val albumCount: Int,
    val duration: Long,
    val dateModified: Long = 0L,
    val coverAlbumIds: List<Long> = emptyList()
)

private data class ArtistAccumulator(
    val name: String,
    var songCount: Int = 0
)

private fun Song.metadataCategoryNames(type: String): List<String> {
    return when (type) {
        "genre" -> splitGenreNames(genre)
        "year" -> listOfNotNull(year.extractYear())
        "composer" -> splitArtistNames(composer)
        "lyricist" -> splitArtistNames(lyricist)
        "folder" -> listOfNotNull(parentFolderPath())
        else -> emptyList()
    }
}

private fun String.extractYear(): String? {
    return Regex("""\d{4}""").find(this)?.value
}

private fun Song.parentFolderPath(): String? {
    val normalized = path.replace('\\', '/')
    return normalized.substringBeforeLast('/', missingDelimiterValue = "")
        .trim()
        .ifBlank { null }
}
