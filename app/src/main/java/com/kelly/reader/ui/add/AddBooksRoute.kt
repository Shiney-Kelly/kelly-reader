package com.kelly.reader.ui.add

import android.content.ClipData
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.toClipEntry
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.launch

import com.kelly.reader.di.appContainer

/**
 * The add books screen entry: builds the ViewModel, which runs the upload server while this
 * screen is on the back stack, and wires the clipboard for the Copy button.
 */
@Composable
fun AddBooksRoute(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val container = appContainer()
    val viewModel: AddBooksViewModel = viewModel(
        factory = viewModelFactory {
            initializer { AddBooksViewModel(container.newUploadServer(), container.wifiAddress()) }
        },
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    AddBooksScreen(
        uiState = uiState,
        onBack = onBack,
        onCopy = { address ->
            scope.launch {
                clipboard.setClipEntry(ClipData.newPlainText("address", address).toClipEntry())
                viewModel.onAddressCopied()
            }
        },
        modifier = modifier,
    )
}
