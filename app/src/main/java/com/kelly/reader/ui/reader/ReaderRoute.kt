package com.kelly.reader.ui.reader

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory

import com.kelly.reader.di.appContainer
import com.kelly.reader.ui.theme.ReaderTheme

/**
 * The reader screen entry: builds the ViewModel for [bookId] and picks the reader theme, the
 * stored one or the one matching the system. The system back button leaves for the shelf, the
 * same as the drawer's button.
 */
@Composable
fun ReaderRoute(
    bookId: String,
    onBackToShelf: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = appContainer()
    val viewModel: ReaderViewModel = viewModel(
        key = bookId,
        factory = viewModelFactory {
            initializer {
                ReaderViewModel(bookId, container.bookRepository, container.settingsRepository)
            }
        },
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val theme = ReaderTheme.fromKey(uiState.settings.themeKey)
        ?: ReaderTheme.forSystem(isSystemInDarkTheme())

    BackHandler(onBack = onBackToShelf)
    ReaderScreen(
        uiState = uiState,
        theme = theme,
        onSettingsClick = {},
        onPageMeasurer = viewModel::setPageMeasurer,
        modifier = modifier,
    )
}
