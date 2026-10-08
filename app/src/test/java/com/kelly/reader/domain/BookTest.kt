package com.kelly.reader.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Checks the derived book fields and the size formatting.
 */
class BookTest {

    private fun book(title: String, fileName: String = "a.txt") =
        Book(id = "1", title = title, fileName = fileName, sizeBytes = 0, charCount = 0, addedAt = 0)

    @Test
    fun glyph_cjkAndEmoji_takesWholeFirstCharacter() {
        assertEquals("山", book("山间小记").glyph)
        assertEquals("📖", book("📖 Notes").glyph)
        assertEquals("", book("").glyph)
    }

    @Test
    fun coverColorIndex_stableAndInRange() {
        val first = book("x", "abc.txt").coverColorIndex
        assertEquals(first, book("y", "abc.txt").coverColorIndex)
        assertTrue(first in 0 until COVER_COLOR_COUNT)
    }

    @Test
    fun formatSize_matchesDesign() {
        assertEquals("386 KB", formatSize(386 * 1024L))
        assertEquals("1.2 MB", formatSize((1.2 * 1024 * 1024).toLong()))
        assertEquals("0 KB", formatSize(0))
    }
}
