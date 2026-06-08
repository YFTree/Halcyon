package com.ella.music.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryNormalizerTest {
    @Test
    fun unknownTextIsPreservedAsRealTagText() {
        assertEquals("unknown", LibraryNormalizer.cleanedTagText("unknown"))
        assertEquals("Unknown Album", LibraryNormalizer.cleanedTagText("Unknown Album"))
        assertEquals("Unknown Artist", LibraryNormalizer.cleanedTagText("Unknown Artist"))

        assertFalse(LibraryNormalizer.isMissingTag("unknown"))
        assertFalse(LibraryNormalizer.isMissingTag("Unknown Album"))
        assertFalse(LibraryNormalizer.isMissingTag("Unknown Artist"))
    }

    @Test
    fun systemUnknownPlaceholderIsMissing() {
        assertEquals("", LibraryNormalizer.cleanedTagText("<unknown>"))
        assertTrue(LibraryNormalizer.isMissingTag("<unknown>"))
        assertTrue(LibraryNormalizer.isMissingTag("   "))
    }

    @Test
    fun mediaStoreTitleMatchingFileNameIsMissing() {
        assertTrue(LibraryNormalizer.isMissingTag("Track 01", "Track 01.flac"))
        assertFalse(LibraryNormalizer.isMissingTag("Track 01", "Other.flac"))
    }
}
