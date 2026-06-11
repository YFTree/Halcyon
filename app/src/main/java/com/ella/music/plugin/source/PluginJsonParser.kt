package com.ella.music.plugin.source

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.longOrNull

class PluginJsonParser(
    private val json: Json
) {
    fun parseSongResults(rawJson: String, pluginId: String, pluginName: String): List<PluginSongSearchResult> {
        val root = json.parseToJsonElement(rawJson)
        val items = when (root) {
            is JsonArray -> root
            is JsonObject -> root.array("items", "results", "songs", "data") ?: JsonArray(emptyList())
            else -> JsonArray(emptyList())
        }
        return items.mapNotNull { element ->
            val obj = element as? JsonObject ?: return@mapNotNull null
            val id = obj.string("id", "songId", "trackId") ?: return@mapNotNull null
            PluginSongSearchResult(
                id = id,
                pluginId = pluginId,
                pluginName = pluginName,
                title = obj.string("title", "name", "songName").orEmpty(),
                artist = obj.string("artist", "artists", "singer").orEmpty(),
                album = obj.string("album", "albumName").orEmpty(),
                duration = obj.long("duration", "durationMs", "duration_ms") ?: 0L,
                date = obj.string("date", "releaseDate", "release_date").orEmpty(),
                trackNumber = obj.string("trackNumber", "trackerNumber", "track_number").orEmpty(),
                picUrl = obj.string("picUrl", "coverUrl", "cover_url", "artworkUrl").orEmpty(),
                fields = obj.stringMap("fields", "metadata").orEmpty(),
                internal = obj.stringMap("internal").orEmpty()
                    .filter { (key, value) -> key.isNotBlank() && key.length <= 64 && value.length <= 4096 }
                    .entries.take(64).associate { it.key to it.value }
            )
        }
    }

    fun parseLyrics(rawJson: String): PluginLyricsResult? {
        val root = json.parseToJsonElement(rawJson)
        if (root is JsonNull) return null
        if (root is JsonPrimitive) {
            val lrc = root.contentOrNull.orEmpty()
            return lrc.takeIf { it.isNotBlank() }?.let {
                PluginLyricsResult(
                    tags = emptyMap(),
                    original = emptyList(),
                    translated = null,
                    romanization = null,
                    payloadType = PluginLyricsPayloadType.RAW_PLAIN_LRC,
                    rawPlainLrc = it
                )
            }
        }
        val obj = root as? JsonObject ?: return null
        if (obj.boolean("notFound") == true) return null
        val tags = obj.stringMap("tags").orEmpty()
        val payloadType = obj.primitiveString("type")?.toPayloadType() ?: PluginLyricsPayloadType.STRUCTURED
        val rawPlain = obj.primitiveString("rawPlainLrc", "raw_plain_lrc", "plainLrc", "plain_lrc", "lrc", "originalLrc", "original_lrc").orEmpty()
        val rawOriginal = obj.primitiveString("original").orEmpty()
        val rawVerbatim = obj.primitiveString("rawVerbatimLrc", "raw_verbatim_lrc").orEmpty()
        val rawEnhanced = obj.primitiveString("rawEnhancedLrc", "raw_enhanced_lrc").orEmpty()
        val rawTtml = obj.primitiveString("rawTtml", "raw_ttml").orEmpty()
        val rawMulti = obj.primitiveString("rawMultiPersonEnhancedLrc", "raw_multi_person_enhanced_lrc").orEmpty()
        if (payloadType != PluginLyricsPayloadType.STRUCTURED) {
            val plain = rawPlain.ifBlank { rawOriginal }
            val hasRaw = when (payloadType) {
                PluginLyricsPayloadType.RAW_PLAIN_LRC -> plain.isNotBlank()
                PluginLyricsPayloadType.RAW_VERBATIM_LRC -> rawVerbatim.isNotBlank()
                PluginLyricsPayloadType.RAW_ENHANCED_LRC -> rawEnhanced.isNotBlank()
                PluginLyricsPayloadType.RAW_TTML -> rawTtml.isNotBlank()
                PluginLyricsPayloadType.RAW_MULTI_PERSON_ENHANCED_LRC -> rawMulti.isNotBlank()
                PluginLyricsPayloadType.STRUCTURED -> false
            }
            if (!hasRaw) return null
            return PluginLyricsResult(tags, emptyList(), null, null, payloadType, plain, rawVerbatim, rawEnhanced, rawTtml, rawMulti)
        }
        val original = obj.array("original", "lines").parseWordLines()
        if (original.isEmpty()) return null
        return PluginLyricsResult(
            tags = tags,
            original = original,
            translated = obj.array("translated", "translation", "translations").parseTextLines().takeIf { it.isNotEmpty() },
            romanization = obj.array("romanization", "romanized", "roma").parseTextLines().takeIf { it.isNotEmpty() }
        )
    }
}

fun PluginLyricsResult.toEmbeddedLyricsText(): String = when (payloadType) {
    PluginLyricsPayloadType.RAW_TTML -> rawTtml
    PluginLyricsPayloadType.RAW_ENHANCED_LRC -> rawEnhancedLrc
    PluginLyricsPayloadType.RAW_VERBATIM_LRC -> rawVerbatimLrc
    PluginLyricsPayloadType.RAW_MULTI_PERSON_ENHANCED_LRC -> rawMultiPersonEnhancedLrc
    PluginLyricsPayloadType.RAW_PLAIN_LRC -> rawPlainLrc
    PluginLyricsPayloadType.STRUCTURED -> buildStructuredLrc()
}.trim()

private fun PluginLyricsResult.buildStructuredLrc(): String = buildString {
    tags["title"]?.takeIf { it.isNotBlank() }?.let { appendLine("[ti:$it]") }
    tags["artist"]?.takeIf { it.isNotBlank() }?.let { appendLine("[ar:$it]") }
    original.sortedBy { it.start }.forEach { line ->
        val text = line.text.trim()
        if (text.isBlank()) return@forEach
        appendLine("${line.start.lrcTimestamp()}$text")
        translated?.nearestText(line.start)?.let { appendLine(it) }
        romanization?.nearestText(line.start)?.let { appendLine(it) }
    }
}

fun PluginSongSearchResult.toPluginSongRequest(): PluginSongRequest =
    PluginSongRequest(id, title, artist, album, duration, pluginId, pluginId, fields, internal)

private fun List<PluginLyricsLine>.nearestText(start: Long): String? =
    minByOrNull { kotlin.math.abs(it.start - start) }
        ?.takeIf { kotlin.math.abs(it.start - start) <= 1500L }
        ?.text
        ?.trim()
        ?.takeIf { it.isNotBlank() }

private fun Long.lrcTimestamp(): String {
    val minutes = this / 60000
    val seconds = (this % 60000) / 1000
    val centis = (this % 1000) / 10
    return "[%02d:%02d.%02d]".format(minutes, seconds, centis)
}

private fun String.toPayloadType(): PluginLyricsPayloadType? = when (trim()) {
    "structured", "STRUCTURED" -> PluginLyricsPayloadType.STRUCTURED
    "rawPlainLrc", "raw_plain_lrc", "RAW_PLAIN_LRC", "plainLrc", "plain_lrc", "lrc" -> PluginLyricsPayloadType.RAW_PLAIN_LRC
    "rawVerbatimLrc", "raw_verbatim_lrc", "RAW_VERBATIM_LRC" -> PluginLyricsPayloadType.RAW_VERBATIM_LRC
    "rawEnhancedLrc", "raw_enhanced_lrc", "RAW_ENHANCED_LRC" -> PluginLyricsPayloadType.RAW_ENHANCED_LRC
    "rawTtml", "raw_ttml", "RAW_TTML", "ttml" -> PluginLyricsPayloadType.RAW_TTML
    "rawMultiPersonEnhancedLrc", "raw_multi_person_enhanced_lrc", "RAW_MULTI_PERSON_ENHANCED_LRC" -> PluginLyricsPayloadType.RAW_MULTI_PERSON_ENHANCED_LRC
    else -> null
}

private fun JsonArray?.parseWordLines(): List<PluginLyricsLine> = this?.mapNotNull { element ->
    val line = element as? JsonArray ?: return@mapNotNull null
    val start = line.longAt(0) ?: return@mapNotNull null
    val end = line.longAt(1) ?: start
    val wordsArray = line.arrayAt(2)
    val text = line.stringAt(2)
    val words = when {
        wordsArray != null -> wordsArray.mapNotNull { wordElement ->
            val word = wordElement as? JsonArray ?: return@mapNotNull null
            PluginLyricsWord(
                start = word.longAt(0) ?: start,
                end = word.longAt(1) ?: end,
                text = word.stringAt(2).orEmpty()
            ).takeIf { it.text.isNotEmpty() }
        }
        !text.isNullOrEmpty() -> listOf(PluginLyricsWord(start, end, text))
        else -> emptyList()
    }
    PluginLyricsLine(start, end, words).takeIf { words.isNotEmpty() }
}.orEmpty()

private fun JsonArray?.parseTextLines(): List<PluginLyricsLine> = this?.mapNotNull { element ->
    val line = element as? JsonArray ?: return@mapNotNull null
    val start = line.longAt(0) ?: return@mapNotNull null
    val end = line.longAt(1) ?: start
    val text = line.stringAt(2).orEmpty()
    PluginLyricsLine(start, end, listOf(PluginLyricsWord(start, end, text))).takeIf { text.isNotBlank() }
}.orEmpty()

private fun JsonArray.longAt(index: Int): Long? =
    (getOrNull(index) as? JsonPrimitive)?.let { it.longOrNull ?: it.contentOrNull?.toLongOrNull() }

private fun JsonArray.stringAt(index: Int): String? = (getOrNull(index) as? JsonPrimitive)?.contentOrNull
private fun JsonArray.arrayAt(index: Int): JsonArray? = getOrNull(index) as? JsonArray
private fun JsonObject.boolean(key: String): Boolean? = (this[key] as? JsonPrimitive)?.booleanOrNull
private fun JsonObject.array(vararg keys: String): JsonArray? = keys.firstNotNullOfOrNull { this[it] as? JsonArray }
private fun JsonObject.primitiveString(vararg keys: String): String? = keys.firstNotNullOfOrNull { (this[it] as? JsonPrimitive)?.contentOrNull }
private fun JsonObject.long(vararg keys: String): Long? =
    keys.firstNotNullOfOrNull { key -> (this[key] as? JsonPrimitive)?.let { it.longOrNull ?: it.contentOrNull?.toLongOrNull() } }

private fun JsonObject.string(vararg keys: String): String? = keys.firstNotNullOfOrNull { key ->
    when (val value = this[key]) {
        is JsonPrimitive -> value.contentOrNull
        is JsonArray -> value.joinToString("/") { item ->
            when (item) {
                is JsonPrimitive -> item.contentOrNull.orEmpty()
                is JsonObject -> item.string("name", "title", "value").orEmpty()
                else -> ""
            }
        }.takeIf { it.isNotBlank() }
        else -> null
    }
}

private fun JsonObject.stringMap(vararg keys: String): Map<String, String>? {
    val obj = keys.firstNotNullOfOrNull { this[it] as? JsonObject } ?: return null
    return obj.mapNotNull { (key, value) ->
        val text = when (value) {
            is JsonPrimitive -> value.contentOrNull
            else -> value.toString()
        }
        text?.takeIf { it.isNotBlank() }?.let { key to it }
    }.toMap()
}
