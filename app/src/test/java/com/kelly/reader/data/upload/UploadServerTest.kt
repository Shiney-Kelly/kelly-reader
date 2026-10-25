package com.kelly.reader.data.upload

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.flow.first
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

import com.kelly.reader.data.FakeBookRepository
import com.kelly.reader.domain.TransferFailure
import com.kelly.reader.domain.TransferStatus

/**
 * Runs the real Ktor server on localhost and uploads files to it with HttpURLConnection.
 */
class UploadServerTest {

    private val repository = FakeBookRepository()
    private val server = UploadServer(repository, page = "<html>upload</html>")
    private var port = 0

    @Before
    fun startServer() {
        port = server.start(preferredPort = 0)
    }

    @After
    fun stopServer() {
        server.stop()
    }

    // chunked sends the body without a Content-Length, as a client that does not know the size
    private fun post(
        name: String,
        body: ByteArray,
        toPort: Int = port,
        chunked: Boolean = false,
    ): Pair<Int, String> {
        val connection = URL("http://127.0.0.1:$toPort/upload").openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.setRequestProperty("X-File-Name", URLEncoder.encode(name, "UTF-8"))
        if (chunked) {
            connection.setChunkedStreamingMode(1_024)
        } else {
            connection.setFixedLengthStreamingMode(body.size)
        }
        connection.outputStream.use { it.write(body) }
        val code = connection.responseCode
        val stream = if (code < 400) connection.inputStream else connection.errorStream
        val text = stream.bufferedReader().use { it.readText() }
        connection.disconnect()
        return code to text
    }

    private fun get(path: String): Pair<Int, String> {
        val connection = URL("http://127.0.0.1:$port$path").openConnection() as HttpURLConnection
        val code = connection.responseCode
        val text = connection.inputStream.bufferedReader().use { it.readText() }
        connection.disconnect()
        return code to text
    }

    @Test
    fun get_root_servesPage() {
        val (code, body) = get("/")
        assertEquals(200, code)
        assertEquals("<html>upload</html>", body)
    }

    @Test
    fun post_utf8TextFile_isAddedToShelf() = runBlocking {
        val text = "山间小记\n\n雨停的时候。"
        val (code, body) = post("山间小记.txt", text.toByteArray(Charsets.UTF_8))
        assertEquals(200, code)
        assertEquals("ok", body)

        val transfer = withTimeout(2_000) {
            server.transfers.first { list -> list.any { it.isDone } }
        }.single()
        assertEquals("山间小记.txt", transfer.name)
        assertEquals(TransferStatus.Added, transfer.status)

        val book = repository.books.value.single()
        assertEquals("山间小记", book.title)
        assertEquals(text.length, book.charCount)
    }

    @Test
    fun post_sameNameTwice_getsNumberedTitle() = runBlocking {
        post("notes.txt", "a".toByteArray())
        post("notes.txt", "b".toByteArray())
        assertEquals(listOf("notes", "notes (2)"), repository.books.value.map { it.title })
    }

    @Test
    fun post_nonTxt_isRejectedWith415() {
        val (code, body) = post("cover-scan.pdf", ByteArray(10))
        assertEquals(415, code)
        assertTrue(body.contains("Not a .txt file"))
        val transfer = server.transfers.value.single()
        assertEquals(TransferStatus.Failed(TransferFailure.NOT_TXT), transfer.status)
        assertTrue(repository.books.value.isEmpty())
    }

    @Test
    fun post_notUtf8_isRejectedWith422() {
        val (code, body) = post("gbk.txt", byteArrayOf(0xD6.toByte(), 0xD0.toByte()))
        assertEquals(422, code)
        assertTrue(body.contains("Not UTF-8"))
        val transfer = server.transfers.value.single()
        assertEquals(TransferStatus.Failed(TransferFailure.NOT_UTF8), transfer.status)
        assertTrue(repository.books.value.isEmpty())
    }

    @Test
    fun post_bomAndWindowsLines_storedClean() {
        val bom = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
        val (code, _) = post("clean.txt", bom + "x\r\ny\rz".toByteArray())
        assertEquals(200, code)
        assertEquals(5, repository.books.value.single().charCount)
        assertEquals("x\ny\nz", repository.files.single().readText())
    }

    @Test
    fun post_bodyOfManyPieces_keepsEveryChar() {
        val line = "雨停的时候，山谷里升起了一层薄雾。The fog lifted.\r\n"
        val text = line.repeat(20_000) // about 1.2 MB, many 64 KB pieces
        val (code, _) = post("long.txt", text.toByteArray(Charsets.UTF_8))
        assertEquals(200, code)
        val cleaned = text.replace("\r\n", "\n")
        assertEquals(cleaned.length, repository.books.value.single().charCount)
        assertEquals(cleaned, repository.files.single().readText())
    }

    @Test
    fun post_badByteLate_isRejectedWith422AndFileDeleted() {
        val good = "a".repeat(200_000).toByteArray()
        val (code, message) = post("late.txt", good + byteArrayOf(0xFF.toByte()) + good)
        assertEquals(422, code)
        assertTrue(message.contains("Not UTF-8"))
        assertTrue(repository.books.value.isEmpty())
        assertFalse(repository.files.single().exists())
    }

    @Test
    fun post_overLimit_isRejectedWith413BeforeReceiving() {
        val small = UploadServer(repository, page = "", maxBytes = 1_000)
        val smallPort = small.start(preferredPort = 0)
        try {
            val body = ByteArray(2_000) { 'a'.code.toByte() }
            val (code, message) = post("big.txt", body, toPort = smallPort)
            assertEquals(413, code)
            assertTrue(message.contains("MB, skipped"))
            val transfer = small.transfers.value.single()
            assertEquals(TransferStatus.Failed(TransferFailure.TOO_BIG), transfer.status)
            assertTrue(repository.books.value.isEmpty())
            assertTrue(repository.files.isEmpty())
        } finally {
            small.stop()
        }
    }

    @Test
    fun post_overLimitWithoutSize_isRejectedWith413AndFileDeleted() {
        val small = UploadServer(repository, page = "", maxBytes = 1_000)
        val smallPort = small.start(preferredPort = 0)
        try {
            val body = ByteArray(5_000) { 'a'.code.toByte() }
            val (code, _) = post("big.txt", body, toPort = smallPort, chunked = true)
            assertEquals(413, code)
            val transfer = small.transfers.value.single()
            assertEquals(TransferStatus.Failed(TransferFailure.TOO_BIG), transfer.status)
            assertTrue(repository.books.value.isEmpty())
            assertFalse(repository.files.single().exists())
        } finally {
            small.stop()
        }
    }

    @Test
    fun post_missingName_isRejectedWith400() {
        val connection = URL("http://127.0.0.1:$port/upload").openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.outputStream.use { it.write("x".toByteArray()) }
        assertEquals(400, connection.responseCode)
        connection.disconnect()
    }
}
