package com.kelly.reader.ui.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import com.kelly.reader.data.upload.UploadServer

/**
 * Owns the upload server for as long as the Add books screen is on the back stack: starts it
 * when created, stops it in [onCleared], and mirrors its transfers into the state.
 */
class AddBooksViewModel(
    private val server: UploadServer,
    wifiAddress: Flow<String?>,
    preferredPort: Int = UploadServer.DEFAULT_PORT,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddBooksUiState())
    val uiState: StateFlow<AddBooksUiState> = _uiState.asStateFlow()

    init {
        val port = server.start(preferredPort)
        viewModelScope.launch {
            wifiAddress.collect { ip ->
                _uiState.update { state ->
                    state.copy(address = ip?.let { "http://$it:$port" }, copied = false)
                }
            }
        }
        viewModelScope.launch {
            server.transfers.collect { transfers ->
                _uiState.update { it.copy(transfers = transfers) }
            }
        }
    }

    /**
     * Marks the address as copied; the screen does the clipboard write itself.
     */
    fun onAddressCopied() {
        _uiState.update { it.copy(copied = true) }
    }

    override fun onCleared() {
        server.stop()
    }
}
