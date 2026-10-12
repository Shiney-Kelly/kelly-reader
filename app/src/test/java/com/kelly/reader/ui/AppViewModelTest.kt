package com.kelly.reader.ui

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

import com.kelly.reader.data.FakeBookRepository
import com.kelly.reader.data.FakeSettingsRepository
import com.kelly.reader.domain.Book

/**
 * Checks the launch rule: reader for a recent book that still exists, shelf otherwise.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setMainDispatcher() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    @Test
    fun startRoute_noRecentBook_isShelf() = runTest(dispatcher) {
        val viewModel = AppViewModel(FakeSettingsRepository(), FakeBookRepository())
        assertNull(viewModel.startRoute.value)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(Routes.SHELF, viewModel.startRoute.value)
    }

    @Test
    fun startRoute_recentBookExists_isReader() = runTest(dispatcher) {
        val book = Book("b1", "Harbor", "b1.txt", 10, 10, 0)
        val viewModel = AppViewModel(
            FakeSettingsRepository(recentBookId = "b1"),
            FakeBookRepository(listOf(book)),
        )
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals("reader/b1", viewModel.startRoute.value)
    }

    @Test
    fun startRoute_recentBookDeleted_isShelf() = runTest(dispatcher) {
        val viewModel = AppViewModel(
            FakeSettingsRepository(recentBookId = "gone"),
            FakeBookRepository(),
        )
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(Routes.SHELF, viewModel.startRoute.value)
    }
}
