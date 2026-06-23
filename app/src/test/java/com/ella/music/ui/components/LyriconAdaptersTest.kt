package com.ella.music.ui.components

import com.ella.music.data.model.LyricLine
import com.ella.music.data.model.LyricWord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class LyriconAdaptersTest {
    @Test
    fun backgroundTranslationDisablesSecondaryKaraokeWords() {
        val song = listOf(
            LyricLine(
                timeMs = 1_000L,
                text = "To get respect from",
                backgroundText = "(Baby",
                backgroundWords = listOf(
                    LyricWord("(Ba", 2_000L, 2_400L),
                    LyricWord("by", 2_400L, 2_800L)
                ),
                backgroundTranslation = "宝贝",
                endMs = 3_000L
            )
        ).toLyriconSong(songId = 1L, songTitle = "Let Me Hear", songArtist = "Fear, and Loathing in Las Vegas")

        val line = song.lyrics!!.single()
        assertEquals("Baby\u000B宝贝", line.secondary)
        assertNull(line.secondaryWords)
    }

    @Test
    fun backgroundWithoutTranslationKeepsSecondaryKaraokeWords() {
        val song = listOf(
            LyricLine(
                timeMs = 3_000L,
                text = "Others To get closer",
                backgroundText = "(Yeah",
                backgroundWords = listOf(
                    LyricWord("(Yeah", 3_100L, 3_600L)
                ),
                endMs = 4_000L
            )
        ).toLyriconSong(songId = 2L, songTitle = "Let Me Hear", songArtist = "Fear, and Loathing in Las Vegas")

        val line = song.lyrics!!.single()
        assertEquals("Yeah", line.secondary)
        assertNotNull(line.secondaryWords)
        assertEquals(listOf("Yeah"), line.secondaryWords?.map { it.text })
    }

    @Test
    fun overlappingDuetLinesKeepIndependentHighlightWindows() {
        val song = listOf(
            LyricLine(
                timeMs = 158_424L,
                text = "パッと花火が",
                words = listOf(
                    LyricWord("パッと", 158_424L, 158_857L),
                    LyricWord("花火", 158_857L, 159_306L),
                    LyricWord("が", 159_306L, 160_548L)
                ),
                agent = "v1"
            ),
            LyricLine(
                timeMs = 159_490L,
                text = "パッと花火が",
                words = listOf(
                    LyricWord("パッと", 159_490L, 160_051L),
                    LyricWord("花火", 160_051L, 160_582L),
                    LyricWord("が", 160_582L, 161_521L)
                ),
                agent = "v2"
            )
        ).toLyriconSong(songId = 3L, songTitle = "打上花火", songArtist = "DAOKO×米津玄師")

        val lines = requireNotNull(song.lyrics)
        assertEquals(2, lines.size)
        assertEquals(false, lines[0].isAlignedRight)
        assertEquals(true, lines[1].isAlignedRight)
        assertEquals(160_548L, lines[0].end)
        assertEquals(159_490L, lines[1].begin)
        assert(lines[0].end > lines[1].begin)
    }
}
