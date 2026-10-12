package com.kelly.reader.data.upload

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.flow.first
import org.junit.After
import org.junit.Assert.assertEquals
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

    private fun post(name: String, body: ByteArray): Pair<Int, String> {
        val connection = URL("http://127.0.0.1:$port/upload").openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.setRequestProperty("X-File-Name", URLEncoder.encode(name, "UTF-8"))
        connection.setFixedLengthStreamingMode(body.size)
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
    fun post_missingName_isRejectedWith400() {
        val connection = URL("http://127.0.0.1:$port/upload").openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.outputStream.use { it.write("x".toByteArray()) }
        assertEquals(400, connection.responseCode)
        connection.disconnect()
    }
}
