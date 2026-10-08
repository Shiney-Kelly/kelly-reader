package com.kelly.reader.ui.shelf

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory

import com.kelly.reader.di.appContainer

/**
 * The bookshelf screen entry: builds the ViewModel and wires its state to [ShelfScreen].
 * A tap on a book opens it, or toggles its selection while editing.
 */
@Composable
fun ShelfRoute(
    onOpenBook: (String) -> Unit,
    onAddBooks: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = appContainer()
    val viewModel: ShelfViewModel = viewModel(
        factory = viewModelFactory { initializer { ShelfViewModel(container.bookRepository) } },
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ShelfScreen(
        uiState = uiState,
        onBookClick = { id -> if (uiState.editing) viewModel.toggleSelected(id) else onOpenBook(id) },
        onEditClick = viewModel::startEditing,
        onDoneClick = viewModel::stopEditing,
        onAddClick = onAddBooks,
        modifier = modifier,
    )
}
