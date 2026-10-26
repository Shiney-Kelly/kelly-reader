package com.kelly.reader.ui.reader

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.nio.file.Files
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset

import com.kelly.reader.data.FakeBookRepository
import com.kelly.reader.data.PageCache
import com.kelly.reader.data.FakeSettingsRepository
import com.kelly.reader.domain.Book
import com.kelly.reader.domain.LineSpacing
import com.kelly.reader.domain.ProgressLabel
import com.kelly.reader.domain.ReadingMode
import com.kelly.reader.domain.ReadingStreak
import com.kelly.reader.domain.TextAlignment

/**
 * Checks loading, paging, resuming and saving with a fixed-size fake measurer.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ReaderViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val text = "abcdefghij" // ten chars, four per page: pages at 0, 4, 8
    private val book = Book("b1", "Harbor", "b1.txt", 10, text.length, 0, lastOffset = 0)
    private val repository = FakeBookRepository(listOf(book)).apply { texts["b1"] = text }
    private val settings = FakeSettingsRepository()
    private val cacheDir = Files.createTempDirectory("pages").toFile()
    private val pageCache = PageCache(cacheDir, dispatcher)
    private val today = LocalDate.of(2026, 10, 12)
    private val clock = Clock.fixed(today.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC)

    private fun fixed(perPage: Int) = PageMeasurer { t, start -> minOf(perPage, t.length - start) }

    @Before
    fun setMainDispatcher() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
        cacheDir.deleteRecursively()
    }

    private fun viewModel(bookId: String = "b1"): ReaderViewModel =
        ReaderViewModel(bookId, repository, settings, pageCache, dispatcher, clock).also {
            dispatcher.scheduler.advanceUntilIdle()
        }

    @Test
    fun init_loadsBookAndMarksItRecent() = runTest(dispatcher) {
        val viewModel = viewModel()
        assertEquals(book, viewModel.uiState.value.book)
        assertEquals(text, viewModel.uiState.value.text)
        assertEquals("b1", settings.recent.value)
    }

    @Test
    fun init_textStillReading_showsSavedPercentAndPagesNothing() = runTest(dispatcher) {
        repository.books.value = listOf(book.copy(lastOffset = 4, progress = 0.4114f))
        val gate = CompletableDeferred<Unit>()
        repository.readTextGate = gate
        val viewModel = viewModel()
        viewModel.setPageMeasurer("k", fixed(4))
        dispatcher.scheduler.advanceUntilIdle()
        val loading = viewModel.uiState.value
        assertEquals("41.14%", loading.percentLabel)
        assertEquals(null, loading.book)
        assertEquals(null, loading.pageStarts)

        gate.complete(Unit)
        dispatcher.scheduler.advanceUntilIdle()
        val read = viewModel.uiState.value
        assertEquals(text, read.text)
        assertEquals(4, read.offset)
        assertEquals("efgh", read.pageText)
    }

    @Test
    fun init_textStillReading_showsPageFromSavedList() = runTest(dispatcher) {
        repository.books.value = listOf(book.copy(lastOffset = 5, progress = 0.5f))
        pageCache.save("b1", "k", intArrayOf(0, 4, 8))
        val gate = CompletableDeferred<Unit>()
        repository.readTextGate = gate
        val viewModel = viewModel()
        viewModel.setPageMeasurer("k", fixed(4))
        dispatcher.scheduler.advanceUntilIdle()
        val loading = viewModel.uiState.value
        assertEquals(null, loading.book)
        assertEquals(2, loading.labelPage)
        assertEquals(3, loading.labelPageCount)

        gate.complete(Unit)
        dispatcher.scheduler.advanceUntilIdle()
        val read = viewModel.uiState.value
        assertEquals(2, read.labelPage)
        assertEquals(3, read.labelPageCount)
    }

    @Test
    fun init_textStillReading_passingLayoutWithoutList_keepsSavedPage() = runTest(dispatcher) {
        repository.books.value = listOf(book.copy(lastOffset = 5, progress = 0.5f))
        pageCache.save("b1", "k", intArrayOf(0, 4, 8))
        repository.readTextGate = CompletableDeferred()
        val viewModel = viewModel()
        viewModel.setPageMeasurer("k", fixed(4))
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.setPageMeasurer("settling", fixed(4))
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.labelPage)
        assertEquals(3, viewModel.uiState.value.labelPageCount)
    }

    @Test
    fun init_textStillReading_layoutNeverCounted_pageUnknown() = runTest(dispatcher) {
        repository.readTextGate = CompletableDeferred()
        val viewModel = viewModel()
        viewModel.setPageMeasurer("never-counted", fixed(4))
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(null, viewModel.uiState.value.labelPage)
        assertEquals(null, viewModel.uiState.value.labelPageCount)
    }

    @Test
    fun init_countsTodayAsReadingDay() = runTest(dispatcher) {
        viewModel()
        assertEquals(1, settings.streak.value.days)
        assertEquals(today, settings.streak.value.lastDay)
    }

    @Test
    fun setPageMeasurer_paginatesAndShowsFirstPage() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.setPageMeasurer("k", fixed(4))
        dispatcher.scheduler.advanceUntilIdle()
        val state = viewModel.uiState.value
        assertArrayEquals(intArrayOf(0, 4, 8), state.pageStarts)
        assertTrue(state.paginationDone)
        assertEquals(3, state.pageCount)
        assertEquals("abcd", state.pageText)
        assertEquals("33.33%", state.percentLabel)
    }

    @Test
    fun nextAndPrevious_moveAndSaveProgress() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.setPageMeasurer("k", fixed(4))
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.nextPage()
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.pageIndex)
        assertEquals("efgh", viewModel.uiState.value.pageText)
        val saved = repository.books.value.single()
        assertEquals(4, saved.lastOffset)
        assertEquals(2f / 3f, saved.progress, 0.001f)

        viewModel.nextPage()
        viewModel.nextPage() // past the end, stays on the last page
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.pageIndex)
        assertFalse(viewModel.uiState.value.hasNextPage)
        assertEquals("100.00%", viewModel.uiState.value.percentLabel)

        viewModel.previousPage()
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.pageIndex)
    }

    @Test
    fun init_resumesAtSavedOffset() = runTest(dispatcher) {
        repository.books.value = listOf(book.copy(lastOffset = 8))
        val viewModel = viewModel()
        viewModel.setPageMeasurer("k", fixed(4))
        dispatcher.scheduler.advanceUntilIdle()
        assertArrayEquals(intArrayOf(0, 4, 8), viewModel.uiState.value.pageStarts)
        assertEquals(2, viewModel.uiState.value.pageIndex)
        assertEquals("ij", viewModel.uiState.value.pageText)
    }

    @Test
    fun init_resumeOffBoundary_keepsPositionInsideFullPage() = runTest(dispatcher) {
        repository.books.value = listOf(book.copy(lastOffset = 9))
        val viewModel = viewModel()
        viewModel.setPageMeasurer("k", fixed(4))
        dispatcher.scheduler.advanceUntilIdle()
        val state = viewModel.uiState.value
        assertArrayEquals(intArrayOf(0, 4, 8), state.pageStarts)
        assertEquals(9, state.offset)
        assertEquals(2, state.pageIndex)
        assertEquals("ij", state.pageText)
        assertTrue(state.pageNumberKnown)
    }

    private fun onSecondPageWithThreePerPage(): ReaderViewModel {
        val viewModel = viewModel()
        viewModel.setPageMeasurer("four", fixed(4))
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.nextPage()
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.setPageMeasurer("three", fixed(3))
        dispatcher.scheduler.advanceUntilIdle()
        return viewModel
    }

    @Test
    fun setPageMeasurer_newKeyMidBook_holdsNewPagesUntilNextTurn() = runTest(dispatcher) {
        val viewModel = onSecondPageWithThreePerPage()
        // the pages from the spot show, the ones from the start wait
        var state = viewModel.uiState.value
        assertArrayEquals(intArrayOf(4, 7), state.pageStarts)
        assertEquals(4, state.offset)
        assertEquals("efg", state.pageText)
        assertFalse(state.paginationDone)
        assertArrayEquals(intArrayOf(0, 3, 6, 9), pageCache.load("b1", "three"))

        // the next page brings them in, at the first char not shown yet
        viewModel.nextPage()
        dispatcher.scheduler.advanceUntilIdle()
        state = viewModel.uiState.value
        assertArrayEquals(intArrayOf(0, 3, 6, 9), state.pageStarts)
        assertTrue(state.paginationDone)
        assertEquals(7, state.offset)
        assertEquals("ghi", state.pageText)
        assertEquals(7, repository.books.value.single().lastOffset)
    }

    @Test
    fun previousPage_newPagesWaiting_showsPageBeforeSpot() = runTest(dispatcher) {
        val viewModel = onSecondPageWithThreePerPage()
        viewModel.previousPage()
        dispatcher.scheduler.advanceUntilIdle()
        val state = viewModel.uiState.value
        assertArrayEquals(intArrayOf(0, 3, 6, 9), state.pageStarts)
        assertEquals(3, state.offset)
        assertEquals("def", state.pageText)
    }

    @Test
    fun setPageMeasurer_newKeyOnLastPage_showsNewPagesAtOnce() = runTest(dispatcher) {
        repository.books.value = listOf(book.copy(lastOffset = 8))
        val viewModel = viewModel()
        viewModel.setPageMeasurer("three", fixed(3))
        dispatcher.scheduler.advanceUntilIdle()
        val state = viewModel.uiState.value
        assertArrayEquals(intArrayOf(0, 3, 6, 9), state.pageStarts)
        assertTrue(state.paginationDone)
        assertEquals(8, state.offset)
        assertEquals("ghi", state.pageText)
    }

    @Test
    fun moveToPage_newPagesWaiting_showsThemAtNewTopPage() = runTest(dispatcher) {
        repository.books.value = listOf(book.copy(lastOffset = 5))
        val viewModel = viewModel()
        viewModel.setPageMeasurer("k", fixed(4))
        dispatcher.scheduler.advanceUntilIdle()
        assertArrayEquals(intArrayOf(5, 9), viewModel.uiState.value.pageStarts)

        viewModel.moveToPage(1)
        dispatcher.scheduler.advanceUntilIdle()
        val state = viewModel.uiState.value
        assertArrayEquals(intArrayOf(0, 4, 8), state.pageStarts)
        assertEquals(9, state.offset)
        assertEquals(2, state.pageIndex)
    }

    @Test
    fun setPageMeasurer_cachedOnDisk_skipsMeasuring() = runTest(dispatcher) {
        pageCache.save("b1", "k", intArrayOf(0, 5))
        val viewModel = viewModel()
        viewModel.setPageMeasurer("k") { _, _ -> error("should not measure") }
        dispatcher.scheduler.advanceUntilIdle()
        val state = viewModel.uiState.value
        assertArrayEquals(intArrayOf(0, 5), state.pageStarts)
        assertTrue(state.paginationDone)
    }

    @Test
    fun setPageMeasurer_savesPagesToDisk() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.setPageMeasurer("k", fixed(4))
        dispatcher.scheduler.advanceUntilIdle()
        assertArrayEquals(intArrayOf(0, 4, 8), pageCache.load("b1", "k"))
    }

    @Test
    fun setPageMeasurer_sameKey_doesNothing() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.setPageMeasurer("k", fixed(4))
        dispatcher.scheduler.advanceUntilIdle()
        val before = viewModel.uiState.value
        viewModel.setPageMeasurer("k", fixed(2))
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(before, viewModel.uiState.value)
    }

    @Test
    fun settings_changesGoToRepository() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.increaseFontSize()
        viewModel.setLineSpacing(LineSpacing.LOOSE)
        viewModel.setThemeKey("paper")
        dispatcher.scheduler.advanceUntilIdle()
        val state = viewModel.uiState.value.settings
        assertEquals(21, state.fontSize)
        assertEquals(LineSpacing.LOOSE, state.lineSpacing)
        assertEquals("paper", state.themeKey)

        viewModel.setThemeKey(null)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(null, viewModel.uiState.value.settings.themeKey)
    }

    @Test
    fun moveToPage_setsOffsetAndSaves() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.setPageMeasurer("k", fixed(4))
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.moveToPage(2)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(8, viewModel.uiState.value.offset)
        assertEquals(8, repository.books.value.single().lastOffset)
        viewModel.moveToPage(7) // out of range, ignored
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(8, viewModel.uiState.value.offset)
    }

    @Test
    fun moveToPage_pageHoldingOffset_keepsExactOffset() = runTest(dispatcher) {
        repository.books.value = listOf(book.copy(lastOffset = 5))
        pageCache.save("b1", "k", intArrayOf(0, 4, 8)) // known pages show at once
        val viewModel = viewModel()
        viewModel.setPageMeasurer("k", fixed(4))
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.moveToPage(1)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(5, viewModel.uiState.value.offset)
        viewModel.moveToPage(2)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(8, viewModel.uiState.value.offset)
    }

    @Test
    fun setPageMeasurer_keyChangedAndBack_offsetDoesNotDrift() = runTest(dispatcher) {
        repository.books.value = listOf(book.copy(lastOffset = 5))
        val viewModel = viewModel()
        viewModel.setPageMeasurer("four", fixed(4))
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.setPageMeasurer("three", fixed(3))
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(5, viewModel.uiState.value.offset)
        assertEquals("fgh", viewModel.uiState.value.pageText) // from the spot, the rest waits

        // four chars a page is known by now, so those pages show at once
        viewModel.setPageMeasurer("four", fixed(4))
        dispatcher.scheduler.advanceUntilIdle()
        val state = viewModel.uiState.value
        assertEquals(5, state.offset)
        assertEquals("efgh", state.pageText)
        assertEquals(5, repository.books.value.single().lastOffset)
    }

    @Test
    fun setPageMeasurer_newKeyMidPage_savesNaturalPagesToDisk() = runTest(dispatcher) {
        repository.books.value = listOf(book.copy(lastOffset = 5))
        val viewModel = viewModel()
        viewModel.setPageMeasurer("three", fixed(3))
        dispatcher.scheduler.advanceUntilIdle()
        assertArrayEquals(intArrayOf(0, 3, 6, 9), pageCache.load("b1", "three"))
        assertArrayEquals(intArrayOf(5, 8), viewModel.uiState.value.pageStarts)
    }

    @Test
    fun setReadingMode_goesToRepository() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.setReadingMode(ReadingMode.SCROLL)
        viewModel.setTextAlignment(TextAlignment.LEFT)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(ReadingMode.SCROLL, viewModel.uiState.value.settings.readingMode)
        assertEquals(TextAlignment.LEFT, viewModel.uiState.value.settings.textAlignment)
    }

    @Test
    fun setProgressLabel_page_storedAndPagesKept() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.setPageMeasurer("k", fixed(4))
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.nextPage()
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.setProgressLabel(ProgressLabel.PAGE)
        dispatcher.scheduler.advanceUntilIdle()
        val state = viewModel.uiState.value
        assertEquals(ProgressLabel.PAGE, settings.settings.value.progressLabel)
        assertEquals(ProgressLabel.PAGE, state.settings.progressLabel)
        assertArrayEquals(intArrayOf(0, 4, 8), state.pageStarts)
        assertEquals(1, state.pageIndex)
    }

    @Test
    fun init_missingBook_staysEmpty() = runTest(dispatcher) {
        val viewModel = viewModel(bookId = "gone")
        assertEquals(null, viewModel.uiState.value.book)
        assertEquals("", viewModel.uiState.value.pageText)
    }
}
