package com.kelly.reader.ui.add

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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
import java.net.ServerSocket

import com.kelly.reader.data.FakeBookRepository
import com.kelly.reader.data.upload.UploadServer
import com.kelly.reader.domain.Transfer
import com.kelly.reader.domain.TransferFailure
import com.kelly.reader.domain.TransferStatus

/**
 * Checks the address, the copied flag and the counts. The server runs on a free port.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AddBooksViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val wifi = MutableStateFlow<String?>("192.168.1.24")
    private val server = UploadServer(FakeBookRepository(), page = "<html></html>")
    private lateinit var viewModel: AddBooksViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        viewModel = AddBooksViewModel(server, wifi, preferredPort = 0)
        dispatcher.scheduler.advanceUntilIdle()
    }

    @After
    fun tearDown() {
        server.stop()
        Dispatchers.resetMain()
    }

    @Test
    fun address_onWifi_isHttpIpAndPort() = runTest(dispatcher) {
        assertEquals("http://192.168.1.24:${server.port}", viewModel.uiState.value.address)
        assertTrue(viewModel.uiState.value.onWifi)
    }

    @Test
    fun address_offWifi_isNullAndCopiedResets() = runTest(dispatcher) {
        viewModel.onAddressCopied()
        assertTrue(viewModel.uiState.value.copied)
        wifi.value = null
        dispatcher.scheduler.advanceUntilIdle()
        assertNull(viewModel.uiState.value.address)
        assertFalse(viewModel.uiState.value.copied)
    }

    @Test
    fun serverFailed_whenNoPortIsFree_statesItInsteadOfCrashing() = runTest(dispatcher) {
        // take the preferred port and the ten after it, which is the whole range tried
        val base = 24121
        val taken = (base..base + 10).map { ServerSocket(it) }
        try {
            val failing = AddBooksViewModel(
                UploadServer(FakeBookRepository(), page = ""),
                wifi,
                preferredPort = base,
            )
            dispatcher.scheduler.advanceUntilIdle()
            val state = failing.uiState.value
            assertTrue(state.serverFailed)
            assertNull(state.address)
            assertTrue(state.onWifi)
        } finally {
            taken.forEach { it.close() }
        }
    }

    @Test
    fun counts_followTransfers() {
        val state = AddBooksUiState(
            transfers = listOf(
                Transfer(0, "a.txt", 10, TransferStatus.Added),
                Transfer(1, "b.txt", 10, TransferStatus.Uploading(50)),
                Transfer(2, "c.pdf", 10, TransferStatus.Failed(TransferFailure.NOT_TXT)),
                Transfer(3, "d.txt", 10, TransferStatus.Added),
            ),
        )
        assertEquals(2, state.doneCount)
        assertEquals(1, state.uploadingCount)
        assertEquals(1, state.failedCount)
        assertEquals(62, state.totalPercent)
    }

    @Test
    fun totalPercent_noTransfers_isZero() {
        assertEquals(0, AddBooksUiState().totalPercent)
    }
}
