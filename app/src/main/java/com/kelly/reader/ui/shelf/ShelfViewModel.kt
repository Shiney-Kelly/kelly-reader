package com.kelly.reader.ui.shelf

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlin.random.Random
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import com.kelly.reader.data.BookRepository
import com.kelly.reader.data.SettingsRepository

// the covers tumble for under a second, then stay mixed up for about a second
const val SHUFFLE_MS = 1_800L

/**
 * State for the bookshelf: the books from the repository plus the edit mode, the selection and
 * the shuffle after a shake. [random] picks the shuffled order.
 */
class ShelfViewModel(
    private val bookRepository: BookRepository,
    private val settingsRepository: SettingsRepository,
    private val random: Random = Random.Default,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShelfUiState())
    val uiState: StateFlow<ShelfUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            bookRepository.observeBooks().collect { books ->
                _uiState.update { state ->
                    // drop selections of books that are gone, and leave edit mode on an empty shelf
                    val ids = books.map { it.id }.toSet()
                    state.copy(
                        books = books,
                        selected = state.selected intersect ids,
                        editing = state.editing && books.isNotEmpty(),
                    )
                }
            }
        }
    }

    fun startEditing() {
        _uiState.update { it.copy(editing = true, selected = emptySet()) }
    }

    fun stopEditing() {
        _uiState.update { it.copy(editing = false, selected = emptySet()) }
    }

    /**
     * Toggles the selection of the book with [id] while editing.
     */
    fun toggleSelected(id: String) {
        _uiState.update { state ->
            if (!state.editing) return@update state
            val selected = if (id in state.selected) state.selected - id else state.selected + id
            state.copy(selected = selected)
        }
    }

    /**
     * Deletes the selected books right away. The recent book is forgotten when it was one of
     * them. Edit mode stays on while books remain.
     */
    fun deleteSelected() {
        val ids = _uiState.value.selected.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            if (settingsRepository.recentBookId.first() in ids) {
                settingsRepository.setRecentBookId(null)
            }
            bookRepository.deleteBooks(ids)
            _uiState.update { it.copy(selected = emptySet()) }
        }
    }

    /**
     * Mixes up the covers after a long shake and puts them back after [SHUFFLE_MS]. Does nothing
     * in edit mode, with fewer than two books, or while the covers are already mixed up.
     */
    fun onShake() {
        val state = _uiState.value
        val ids = state.books?.map { it.id } ?: return
        if (state.editing || state.shuffling || ids.size < 2) return
        var order = ids.shuffled(random)
        while (order == ids) order = ids.shuffled(random)
        _uiState.update { it.copy(shuffledIds = order) }
        viewModelScope.launch {
            delay(SHUFFLE_MS)
            _uiState.update { it.copy(shuffledIds = null) }
        }
    }
}
