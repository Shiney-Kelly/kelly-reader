package com.kelly.reader.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

import com.kelly.reader.data.BookRepository
import com.kelly.reader.data.SettingsRepository

/**
 * Decides where the app starts: the reader when a recent book still exists, else the shelf.
 */
class AppViewModel(
    private val settingsRepository: SettingsRepository,
    private val bookRepository: BookRepository,
) : ViewModel() {

    private val _startRoute = MutableStateFlow<String?>(null)

    /**
     * Null until the recent book has been looked up, then the route to start on.
     */
    val startRoute: StateFlow<String?> = _startRoute.asStateFlow()

    init {
        viewModelScope.launch {
            val recentId = settingsRepository.recentBookId.first()
            val book = recentId?.let { bookRepository.getBook(it) }
            _startRoute.value = if (book != null) Routes.reader(book.id) else Routes.SHELF
        }
    }
}
