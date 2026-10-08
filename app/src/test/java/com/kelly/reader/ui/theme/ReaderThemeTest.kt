package com.kelly.reader.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Checks the key lookup and the system fallback of [ReaderTheme].
 */
class ReaderThemeTest {

    @Test
    fun fromKey_knownKey_returnsTheme() {
        assertEquals(ReaderTheme.PAPER, ReaderTheme.fromKey("paper"))
    }

    @Test
    fun fromKey_unknownOrNull_returnsNull() {
        assertNull(ReaderTheme.fromKey("sepia"))
        assertNull(ReaderTheme.fromKey(null))
    }

    @Test
    fun forSystem_dark_returnsDark() {
        assertEquals(ReaderTheme.DARK, ReaderTheme.forSystem(darkTheme = true))
        assertEquals(ReaderTheme.LIGHT, ReaderTheme.forSystem(darkTheme = false))
    }
}
