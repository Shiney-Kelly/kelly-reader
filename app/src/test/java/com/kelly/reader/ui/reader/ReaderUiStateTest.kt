package com.kelly.reader.ui.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

import com.kelly.reader.domain.Book

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
        assertEquals("0.00%", state.percentLabel)
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
        assertEquals("100.00%", done.percentLabel)
    }

    @Test
    fun pageTextForLayout_pageEndsInsideParagraph_addsFiller() {
        val state = ReaderUiState(
            text = "ab cd ef\ngh",
            offset = 0,
            pageStarts = intArrayOf(0, 3, 9),
            paginationDone = true,
        )
        assertEquals("ab ", state.pageText)
        assertEquals("ab " + NO_BREAK_RUN, state.pageTextForLayout)
        assertEquals("", ReaderUiState(text = text).pageTextForLayout)
    }

    @Test
    fun pageText_anchoredStarts_showsCurrentPageWithoutPageNumber() {
        val state = ReaderUiState(text = text, offset = 5, pageStarts = intArrayOf(5, 9))
        assertTrue(state.pageKnown)
        assertEquals("fghi", state.pageText)
        assertFalse(state.pageNumberKnown)
        assertFalse(state.hasPreviousPage)
        val filled = state.copy(pageStarts = intArrayOf(0, 4, 5, 9), paginationDone = true)
        assertTrue(filled.pageNumberKnown)
        assertEquals(2, filled.pageIndex)
        assertTrue(filled.hasPreviousPage)
    }

    @Test
    fun percent_beforeDone_usesCharsToTheEndOfThePage() {
        val state = ReaderUiState(text = text, offset = 5, pageStarts = intArrayOf(0, 4, 8))
        assertEquals("80.00%", state.percentLabel)
        // the first page counts as read, as it does by page, so a new count does not show 0%
        assertEquals("40.00%", state.copy(offset = 0).percentLabel)
    }

    @Test
    fun percent_beforeDone_pageEndNotKnown_usesTheOffset() {
        // the last page known so far: its end is still being measured
        val last = ReaderUiState(text = text, offset = 8, pageStarts = intArrayOf(0, 4, 8))
        assertEquals("80.00%", last.percentLabel)
        // jumped before the pages measured from a later spot
        val before = ReaderUiState(text = text, offset = 2, pageStarts = intArrayOf(5, 9))
        assertEquals("20.00%", before.percentLabel)
    }

    @Test
    fun labelPage_bookNotInYet_isSavedPage() {
        val loading = ReaderUiState(savedPage = SavedPage(26857, 67141))
        assertEquals(26857, loading.labelPage)
        assertEquals(67141, loading.labelPageCount)
        assertNull(ReaderUiState().labelPage)
        assertNull(ReaderUiState().labelPageCount)
    }

    @Test
    fun labelPage_bookIn_followsMeasuredPages() {
        val book = Book("b1", "Harbor", "b1.txt", 10, text.length, 0)
        // the text is in but no page yet: still the saved page
        val textOnly = ReaderUiState(book = book, savedPage = SavedPage(9, 9), text = text)
        assertEquals(9, textOnly.labelPage)
        assertEquals(9, textOnly.labelPageCount)
        val running = ReaderUiState(
            book = book,
            savedPage = SavedPage(9, 9),
            text = text,
            offset = 5,
            pageStarts = intArrayOf(0, 4, 8),
        )
        assertEquals(2, running.labelPage)
        assertNull(running.labelPageCount)
        val done = running.copy(paginationDone = true)
        assertEquals(2, done.labelPage)
        assertEquals(3, done.labelPageCount)
        // measured from the spot first, the pages before it are not known yet
        assertNull(running.copy(pageStarts = intArrayOf(5, 9)).labelPage)
    }

    @Test
    fun percent_textNotReadYet_isSavedProgress() {
        val loading = ReaderUiState(savedProgress = 0.4114f)
        assertEquals("41.14%", loading.percentLabel)
        val read = loading.copy(text = text, offset = 5)
        assertEquals("50.00%", read.percentLabel)
    }
}
