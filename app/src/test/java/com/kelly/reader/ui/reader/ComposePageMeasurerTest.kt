package com.kelly.reader.ui.reader

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Checks how the page measurer sizes the window of text it lays out.
 */
class ComposePageMeasurerTest {

    @Test
    fun nextWindow_noPageYet_startsWithTwoPagesOfText() {
        assertEquals(1_500, nextWindow(0))
    }

    @Test
    fun nextWindow_afterPage_isHalfAsManyAgain() {
        assertEquals(930, nextWindow(620))
    }

    @Test
    fun nextWindow_afterShortPage_isNeverTiny() {
        assertEquals(256, nextWindow(3))
    }
}
