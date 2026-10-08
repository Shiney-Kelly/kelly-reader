package com.kelly.reader.ui.shelf

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

import com.kelly.reader.data.FakeBookRepository
import com.kelly.reader.data.FakeSettingsRepository
import com.kelly.reader.domain.Book

/**
 * Checks edit mode, selection and deleting on the shelf.
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
}
