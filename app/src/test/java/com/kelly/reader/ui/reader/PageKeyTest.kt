package com.kelly.reader.ui.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * Checks that the page key changes with everything that moves the page breaks.
 */
class PageKeyTest {

    private val key = PageKey(
        widthPx = 948,
        heightPx = 1880,
        fontSize = 18,
        fontScale = 1f,
        lineSpacingKey = "1.6",
        fontKey = "serif",
    )

    @Test
    fun cacheKey_sameLayout_isTheSame() {
        assertEquals(key.cacheKey, key.copy().cacheKey)
    }

    @Test
    fun cacheKey_phoneFontSizeChanged_differs() {
        assertNotEquals(key.cacheKey, key.copy(fontScale = 2f).cacheKey)
    }
}
