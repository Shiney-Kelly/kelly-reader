package com.kelly.reader.ui.shelf

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.delay

import com.kelly.reader.R
import com.kelly.reader.di.appContainer

// how long a first back press waits for the second one that leaves the app
private const val EXIT_WINDOW_MS = 2_000L

/**
 * The bookshelf screen entry: builds the ViewModel and wires its state to [ShelfScreen].
 * A tap on a book opens it, or toggles its selection while editing. A long shake mixes up the
 * covers for a moment. Three quick knocks on the ledge bring out the hidden shelf bear. Back
 * leaves edit mode before anything else; otherwise it leaves the app only when pressed twice
 * within [EXIT_WINDOW_MS], and the first press says so.
 */
@Composable
fun ShelfRoute(
    onOpenBook: (String) -> Unit,
    onAddBooks: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = appContainer()
    val viewModel: ShelfViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                ShelfViewModel(container.bookRepository, container.settingsRepository)
            }
        },
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    BackHandler(enabled = uiState.editing) { viewModel.stopEditing() }
    // the first press arms the exit and says so; while armed this handler is off, so the next
    // press reaches the system and leaves the app (the shelf is always the first screen here)
    val context = LocalContext.current
    val exitHint = stringResource(R.string.shelf_press_back_again)
    var exitArmed by remember { mutableStateOf(false) }
    BackHandler(enabled = !uiState.editing && !exitArmed) {
        exitArmed = true
        Toast.makeText(context, exitHint, Toast.LENGTH_SHORT).show()
    }
    LaunchedEffect(exitArmed) {
        if (exitArmed) {
            delay(EXIT_WINDOW_MS)
            exitArmed = false
        }
    }
    ShakeEffect(onShake = viewModel::onShake)
    ShelfScreen(
        uiState = uiState,
        onBookClick = { id -> if (uiState.editing) viewModel.toggleSelected(id) else onOpenBook(id) },
        onEditClick = viewModel::startEditing,
        onDoneClick = viewModel::stopEditing,
        onAddClick = onAddBooks,
        onDeleteClick = viewModel::deleteSelected,
        onPetFound = viewModel::onPetFound,
        modifier = modifier,
    )
}
