package com.kelly.reader.ui.reader

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

    @Test
    fun setPageMeasurer_newKey_repaginatesKeepingOffset() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.setPageMeasurer("four", fixed(4))
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.nextPage()
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(4, viewModel.uiState.value.offset)

        // the new pages break naturally, so the offset now sits inside the second page
        viewModel.setPageMeasurer("three", fixed(3))
        dispatcher.scheduler.advanceUntilIdle()
        val state = viewModel.uiState.value
        assertArrayEquals(intArrayOf(0, 3, 6, 9), state.pageStarts)
        assertEquals(4, state.offset)
        assertEquals(1, state.pageIndex)
        assertEquals("def", state.pageText)
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
        assertEquals("def", viewModel.uiState.value.pageText)

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
        assertEquals(1, viewModel.uiState.value.pageIndex)
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
    fun init_missingBook_staysEmpty() = runTest(dispatcher) {
        val viewModel = viewModel(bookId = "gone")
        assertEquals(null, viewModel.uiState.value.book)
        assertEquals("", viewModel.uiState.value.pageText)
    }
}
