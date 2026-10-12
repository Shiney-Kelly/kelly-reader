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
 * Owns the upload server for the Add books screen. The screen calls [startServer] when it
 * comes to the front and [stopServer] when it goes behind another app or screen, so transfer
 * only runs while the screen is visible, as its footer says. The received list survives a
 * stop. When no port is free the state says so instead of crashing.
 */
class AddBooksViewModel(
    private val server: UploadServer,
    wifiAddress: Flow<String?>,
    private val preferredPort: Int = UploadServer.DEFAULT_PORT,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddBooksUiState())
    val uiState: StateFlow<AddBooksUiState> = _uiState.asStateFlow()

    private var ip: String? = null
    private var port: Int? = null

    init {
        viewModelScope.launch {
            wifiAddress.collect { address ->
                ip = address
                refreshAddress()
            }
        }
        viewModelScope.launch {
            server.transfers.collect { transfers ->
                _uiState.update { it.copy(transfers = transfers) }
            }
        }
    }

    /**
     * Starts listening, unless already running or no port could be found before.
     */
    fun startServer() {
        if (port != null || _uiState.value.serverFailed) return
        port = runCatching { server.start(preferredPort) }.getOrNull()
        if (port == null) _uiState.update { it.copy(serverFailed = true) }
        refreshAddress()
    }

    fun stopServer() {
        if (port == null) return
        server.stop()
        port = null
        refreshAddress()
    }

    /**
     * Marks the address as copied; the screen does the clipboard write itself.
     */
    fun onAddressCopied() {
        _uiState.update { it.copy(copied = true) }
    }

    private fun refreshAddress() {
        val address = port?.let { p -> ip?.let { "http://$it:$p" } }
        _uiState.update { state ->
            state.copy(
                address = address,
                onWifi = ip != null,
                copied = state.copied && address == state.address,
            )
        }
    }

    override fun onCleared() {
        stopServer()
    }
}
