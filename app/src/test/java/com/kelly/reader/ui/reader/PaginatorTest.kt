package com.kelly.reader.ui.reader

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Checks paging with a fake measurer that fits a fixed number of chars per page.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PaginatorTest {

    private val dispatcher = UnconfinedTestDispatcher()

    private fun fixed(perPage: Int) = PageMeasurer { text, start -> minOf(perPage, text.length - start) }

    @Test
    fun paginate_textLongerThanPages_splitsEvenly() = runTest {
        val starts = Paginator.paginate("abcdefghij", fixed(4), dispatcher).last()
        assertArrayEquals(intArrayOf(0, 4, 8), starts)
    }

    @Test
    fun paginate_exactMultiple_hasNoEmptyLastPage() = runTest {
        val starts = Paginator.paginate("abcdefgh", fixed(4), dispatcher).last()
        assertArrayEquals(intArrayOf(0, 4), starts)
    }

    @Test
    fun paginate_emptyText_isOneEmptyPage() = runTest {
        val starts = Paginator.paginate("", fixed(4), dispatcher).last()
        assertArrayEquals(intArrayOf(0), starts)
        assertEquals("", Paginator.pageText("", starts, 0))
    }

    @Test
    fun paginate_emitsAfterEveryPage() = runTest {
        val emissions = Paginator.paginate("abcdef", fixed(2), dispatcher).toList()
        assertEquals(3, emissions.size)
        assertArrayEquals(intArrayOf(0), emissions[0])
        assertArrayEquals(intArrayOf(0, 2, 4), emissions[2])
    }

    @Test
    fun paginate_measurerSaysZero_stillAdvances() = runTest {
        val starts = Paginator.paginate("abc", { _, _ -> 0 }, dispatcher).last()
        assertArrayEquals(intArrayOf(0, 1, 2), starts)
    }

    @Test
    fun paginate_withAnchor_emitsPagesFromAnchorFirstThenNaturalPages() = runTest {
        val emissions = Paginator.paginate("abcdefghij", fixed(4), dispatcher, anchor = 5).toList()
        assertArrayEquals(intArrayOf(5), emissions[0])
        assertArrayEquals(intArrayOf(5, 9), emissions[1])
        // no page is cut at the anchor: the page from 4 runs to 8 and holds it
        assertArrayEquals(intArrayOf(0, 4, 8), emissions.last())
    }

    @Test
    fun paginate_withAnchor_anchorLandsInsideItsPage() = runTest {
        val text = "abcdefghij"
        val starts = Paginator.paginate(text, fixed(4), dispatcher, anchor = 5).last()
        val index = Paginator.pageIndexFor(starts, 5)
        assertEquals(1, index)
        assertEquals("efgh", Paginator.pageText(text, starts, index))
    }

    @Test
    fun paginate_anyAnchor_endsWithPlainPaging() = runTest {
        val text = "abcdefghijklmnopq"
        val plain = Paginator.paginate(text, fixed(4), dispatcher).last()
        for (anchor in 1 until text.length) {
            val starts = Paginator.paginate(text, fixed(4), dispatcher, anchor).last()
            assertArrayEquals("anchor $anchor", plain, starts)
        }
    }

    @Test
    fun paginate_anchorOnNaturalBoundary_matchesPlainPaging() = runTest {
        val starts = Paginator.paginate("abcdefghij", fixed(4), dispatcher, anchor = 4).last()
        assertArrayEquals(intArrayOf(0, 4, 8), starts)
    }

    @Test
    fun paginate_anchorOutsideText_startsFromZero() = runTest {
        assertArrayEquals(
            intArrayOf(0, 4, 8),
            Paginator.paginate("abcdefghij", fixed(4), dispatcher, anchor = 10).last(),
        )
        assertArrayEquals(intArrayOf(0), Paginator.paginate("", fixed(4), dispatcher, anchor = 3).last())
    }

    @Test
    fun pageIndexFor_findsPageHoldingOffset() {
        val starts = intArrayOf(0, 4, 8)
        assertEquals(0, Paginator.pageIndexFor(starts, 0))
        assertEquals(0, Paginator.pageIndexFor(starts, 3))
        assertEquals(1, Paginator.pageIndexFor(starts, 4))
        assertEquals(2, Paginator.pageIndexFor(starts, 100))
    }

    @Test
    fun pageText_returnsSliceOfPage() {
        val starts = intArrayOf(0, 4, 8)
        assertEquals("abcd", Paginator.pageText("abcdefghij", starts, 0))
        assertEquals("ij", Paginator.pageText("abcdefghij", starts, 2))
    }

    @Test
    fun pageTextForLayout_pageEndsOnSpaceInsideParagraph_addsRun() {
        val text = "ab cd ef\ngh"
        val page = Paginator.pageTextForLayout(text, intArrayOf(0, 3, 9), 0)
        assertEquals("ab " + NO_BREAK_RUN, page)
        assertEquals("cd ef\n", Paginator.pageTextForLayout(text, intArrayOf(0, 3, 9), 1))
    }

    @Test
    fun pageTextForLayout_pageEndsInsideChineseParagraph_addsBreakThenRun() {
        val page = Paginator.pageTextForLayout("你好世界", intArrayOf(0, 2), 0)
        assertEquals("你好" + RUN_BREAK + NO_BREAK_RUN, page)
    }

    @Test
    fun pageTextForLayout_pageEndsParagraphOrBook_isThePage() {
        assertEquals("ab\n", Paginator.pageTextForLayout("ab\ncd", intArrayOf(0, 3), 0))
        assertEquals("cd", Paginator.pageTextForLayout("ab\ncd", intArrayOf(0, 3), 1))
    }

    @Test
    fun copiedText_selectAllOnPage_dropsRun() {
        assertEquals("ab ", Paginator.copiedText("ab " + NO_BREAK_RUN))
        assertEquals("你好", Paginator.copiedText("你好" + RUN_BREAK + NO_BREAK_RUN))
    }

    @Test
    fun copiedText_acrossPages_dropsEveryRunAndItsJoin() {
        // scroll mode: a selection runs through each page's whole text, run included, and the
        // pages' texts come joined by line breaks
        val copied = "ab " + NO_BREAK_RUN + "\n你好" + RUN_BREAK + NO_BREAK_RUN + "\nef"
        assertEquals("ab 你好ef", Paginator.copiedText(copied))
    }

    @Test
    fun copiedText_joinAfterParagraph_keepsLineBreak() {
        // a page ending a paragraph is shown without its last break, and the join puts it back
        assertEquals("ab\n\ncd", Paginator.copiedText("ab\n" + "\n" + "cd"))
    }

    @Test
    fun copiedText_endsOnLastLine_dropsLoneBreakChar() {
        assertEquals("ab cd", Paginator.copiedText("ab cd​"))
        assertEquals("ab cd", Paginator.copiedText("ab cd"))
    }

    @Test
    fun scrollPageText_pageEndsOnLineBreak_dropsOneLineBreak() {
        assertEquals("ab", Paginator.scrollPageText("ab\ncd", intArrayOf(0, 3), 0))
        assertEquals("ab", Paginator.scrollPageText("ab\r\ncd", intArrayOf(0, 4), 0))
        // a blank line at the end of the page stays as one empty line
        assertEquals("ab\r\n", Paginator.scrollPageText("ab\r\n\r\ncd", intArrayOf(0, 6), 0))
    }

    @Test
    fun scrollPageText_pageEndsMidLine_isUnchanged() {
        assertEquals("ab ", Paginator.scrollPageText("ab cd", intArrayOf(0, 3), 0))
        assertEquals("cd", Paginator.scrollPageText("ab cd", intArrayOf(0, 3), 1))
    }
}
