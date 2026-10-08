package com.kelly.reader.ui.shelf

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import com.kelly.reader.data.BookRepository

/**
 * State for the bookshelf: the books from the repository plus the edit mode and selection.
 */
class ShelfViewModel(private val bookRepository: BookRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(ShelfUiState())
    val uiState: StateFlow<ShelfUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            bookRepository.observeBooks().collect { books ->
                _uiState.update { state ->
                    // drop selections of books that are gone
                    val ids = books.map { it.id }.toSet()
                    state.copy(books = books, selected = state.selected intersect ids)
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
}
