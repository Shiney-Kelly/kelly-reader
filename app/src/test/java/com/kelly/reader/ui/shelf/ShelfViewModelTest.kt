package com.kelly.reader.ui.shelf

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

import com.kelly.reader.data.FakeBookRepository
import com.kelly.reader.data.FakeSettingsRepository
import com.kelly.reader.domain.Book

/**
 * Checks edit mode, selection, deleting and the shake shuffle on the shelf.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ShelfViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val books = listOf(
        Book("b1", "One", "1.txt", 1, 1, 1),
        Book("b2", "Two", "2.txt", 1, 1, 2),
        Book("b3", "Three", "3.txt", 1, 1, 3),
    )

    @Before
    fun setMainDispatcher() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    private fun viewModel(
        bookRepository: FakeBookRepository = FakeBookRepository(books),
        settingsRepository: FakeSettingsRepository = FakeSettingsRepository(),
    ): ShelfViewModel = ShelfViewModel(bookRepository, settingsRepository).also {
        dispatcher.scheduler.advanceUntilIdle()
    }

    @Test
    fun uiState_afterLoad_hasBooks() = runTest(dispatcher) {
        val viewModel = viewModel()
        assertEquals(books, viewModel.uiState.value.books)
        assertFalse(viewModel.uiState.value.editing)
    }

    @Test
    fun toggleSelected_outsideEditMode_doesNothing() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.toggleSelected("b1")
        assertTrue(viewModel.uiState.value.selected.isEmpty())
    }

    @Test
    fun toggleSelected_inEditMode_addsThenRemoves() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.startEditing()
        viewModel.toggleSelected("b1")
        viewModel.toggleSelected("b2")
        assertEquals(setOf("b1", "b2"), viewModel.uiState.value.selected)
        viewModel.toggleSelected("b1")
        assertEquals(setOf("b2"), viewModel.uiState.value.selected)
    }

    @Test
    fun stopEditing_clearsSelection() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.startEditing()
        viewModel.toggleSelected("b1")
        viewModel.stopEditing()
        assertFalse(viewModel.uiState.value.editing)
        assertTrue(viewModel.uiState.value.selected.isEmpty())
    }

    @Test
    fun deleteSelected_removesBooksAndStaysInEditMode() = runTest(dispatcher) {
        val repository = FakeBookRepository(books)
        val viewModel = viewModel(repository)
        viewModel.startEditing()
        viewModel.toggleSelected("b2")
        viewModel.deleteSelected()
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(listOf("b2"), repository.deleted)
        assertEquals(listOf("b1", "b3"), viewModel.uiState.value.books?.map { it.id })
        assertTrue(viewModel.uiState.value.editing)
        assertTrue(viewModel.uiState.value.selected.isEmpty())
    }

    @Test
    fun deleteSelected_lastBooks_leavesEditMode() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.startEditing()
        books.forEach { viewModel.toggleSelected(it.id) }
        viewModel.deleteSelected()
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(emptyList<Book>(), viewModel.uiState.value.books)
        assertFalse(viewModel.uiState.value.editing)
    }

    @Test
    fun deleteSelected_recentBook_forgetsIt() = runTest(dispatcher) {
        val settings = FakeSettingsRepository(recentBookId = "b3")
        val viewModel = viewModel(settingsRepository = settings)
        viewModel.startEditing()
        viewModel.toggleSelected("b3")
        viewModel.deleteSelected()
        dispatcher.scheduler.advanceUntilIdle()
        assertNull(settings.recent.value)
    }

    @Test
    fun deleteSelected_otherBook_keepsRecent() = runTest(dispatcher) {
        val settings = FakeSettingsRepository(recentBookId = "b3")
        val viewModel = viewModel(settingsRepository = settings)
        viewModel.startEditing()
        viewModel.toggleSelected("b1")
        viewModel.deleteSelected()
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals("b3", settings.recent.value)
    }

    @Test
    fun onShake_threeBooks_mixesUpTheOrderThenPutsItBack() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.onShake()
        val shuffled = viewModel.uiState.value.shuffledIds
        assertEquals(setOf("b1", "b2", "b3"), shuffled?.toSet())
        assertNotEquals(listOf("b1", "b2", "b3"), shuffled)
        assertEquals(shuffled, viewModel.uiState.value.shownBooks?.map { it.id })
        dispatcher.scheduler.advanceTimeBy(SHUFFLE_MS - 1)
        dispatcher.scheduler.runCurrent()
        assertTrue(viewModel.uiState.value.shuffling)
        dispatcher.scheduler.advanceTimeBy(1)
        dispatcher.scheduler.runCurrent()
        assertFalse(viewModel.uiState.value.shuffling)
        assertEquals(books, viewModel.uiState.value.shownBooks)
    }

    @Test
    fun onShake_twoBooks_swapsThem() = runTest(dispatcher) {
        val viewModel = viewModel(FakeBookRepository(books.take(2)))
        viewModel.onShake()
        assertEquals(listOf("b2", "b1"), viewModel.uiState.value.shuffledIds)
    }

    @Test
    fun onShake_oneBook_doesNothing() = runTest(dispatcher) {
        val viewModel = viewModel(FakeBookRepository(books.take(1)))
        viewModel.onShake()
        assertFalse(viewModel.uiState.value.shuffling)
    }

    @Test
    fun onShake_editing_doesNothing() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.startEditing()
        viewModel.onShake()
        assertFalse(viewModel.uiState.value.shuffling)
    }

    @Test
    fun onShake_whileMixedUp_keepsTheFirstOrderAndTimer() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.onShake()
        val first = viewModel.uiState.value.shuffledIds
        dispatcher.scheduler.advanceTimeBy(1_000)
        viewModel.onShake()
        assertEquals(first, viewModel.uiState.value.shuffledIds)
        dispatcher.scheduler.advanceTimeBy(SHUFFLE_MS - 1_000)
        dispatcher.scheduler.runCurrent()
        assertFalse(viewModel.uiState.value.shuffling)
    }

    @Test
    fun shownBooks_bookAddedWhileMixedUp_goesLast() = runTest(dispatcher) {
        val repository = FakeBookRepository(books)
        val viewModel = viewModel(repository)
        viewModel.onShake()
        val shuffled = viewModel.uiState.value.shuffledIds.orEmpty()
        repository.books.update { it + Book("b4", "Four", "4.txt", 1, 1, 4) }
        dispatcher.scheduler.runCurrent()
        assertEquals(shuffled + "b4", viewModel.uiState.value.shownBooks?.map { it.id })
    }
}
