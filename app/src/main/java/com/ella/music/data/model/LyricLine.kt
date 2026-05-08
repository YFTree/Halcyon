package com.ella.music.data.model

data class LyricLine(
    val timeMs: Long,
    val text: String,
    val words: List<LyricWord> = emptyList(),
    val translation: String? = null,
    val agent: String? = null,
    val backgroundText: String? = null,
    val backgroundWords: List<LyricWord> = emptyList(),
    val backgroundTranslation: String? = null,
    val isTtml: Boolean = false,
    val endMs: Long? = null
)

data class LyricWord(
    val text: String,
    val startMs: Long,
    val endMs: Long
)
