package com.kelly.reader.ui.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Checks the derived page fields while pagination is still running.
 */
class ReaderUiStateTest {

    private val text = "abcdefghij"

    @Test
    fun pageText_beforeAnyPage_isEmpty() {
        val state = ReaderUiState(text = text)
        assertFalse(state.pageKnown)
        assertEquals("", state.pageText)
        assertEquals(0, state.percent)
    }

    @Test
    fun pageText_offsetBeyondKnownPages_isEmptyUntilNextStartArrives() {
        val waiting = ReaderUiState(text = text, offset = 4, pageStarts = intArrayOf(0, 4))
        assertFalse(waiting.pageKnown)
        assertEquals("", waiting.pageText)

        val known = waiting.copy(pageStarts = intArrayOf(0, 4, 8))
        assertTrue(known.pageKnown)
        assertEquals("efgh", known.pageText)
        assertEquals(1, known.pageIndex)
    }

    @Test
    fun pageText_lastPage_knownOnceDone() {
        val running = ReaderUiState(text = text, offset = 8, pageStarts = intArrayOf(0, 4, 8))
        assertEquals("", running.pageText)
        val done = running.copy(paginationDone = true)
        assertEquals("ij", done.pageText)
        assertEquals(100, done.percent)
    }

    @Test
    fun percent_beforeDone_usesChars() {
        val state = ReaderUiState(text = text, offset = 5, pageStarts = intArrayOf(0, 4, 8))
        assertEquals(50, state.percent)
    }
}
