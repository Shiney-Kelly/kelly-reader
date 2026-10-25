package com.kelly.reader.data.upload

import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.cio.CIO
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.request.header
import io.ktor.server.request.receiveChannel
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.io.EOFException
import java.io.File
import java.net.ServerSocket
import java.net.URLDecoder

import com.kelly.reader.data.BookRepository
import com.kelly.reader.data.TextImport
import com.kelly.reader.data.Utf8TextWriter
import com.kelly.reader.domain.MAX_UPLOAD_MB
import com.kelly.reader.domain.Transfer
import com.kelly.reader.domain.TransferFailure
import com.kelly.reader.domain.TransferStatus

// the page is served at "/", files are posted one at a time to "/upload"
private const val UPLOAD_PATH = "/upload"
private const val FILE_NAME_HEADER = "X-File-Name"
private const val STOP_GRACE_MS = 500L
private const val STOP_TIMEOUT_MS = 1_000L
private const val BUFFER_SIZE = 64 * 1024
private const val BYTES_PER_MB = 1024L * 1024L
private const val PART_SUFFIX = ".part"

/**
 * What came of reading an upload's body: [bytes] read, and [charCount] of the cleaned text,
 * null when it was not UTF-8 or was [tooBig].
 */
private class Received(val bytes: Long, val charCount: Int?, val tooBig: Boolean)

/**
 * The Wi-Fi upload server. Serves [page] at the root and takes one file per `POST /upload`,
 * sent as a raw body with the file name URL-encoded in the `X-File-Name` header. Files over
 * [maxBytes] are turned away. Progress and results of every file show up in [transfers].
 * Start it with [start], stop it with [stop].
 */
class UploadServer(
    private val bookRepository: BookRepository,
    private val page: String,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val maxBytes: Long = MAX_UPLOAD_MB * BYTES_PER_MB,
) {

    private val _transfers = MutableStateFlow<List<Transfer>>(emptyList())
    val transfers: StateFlow<List<Transfer>> = _transfers.asStateFlow()

    private var engine: EmbeddedServer<*, *>? = null
    private var nextId = 0

    /**
     * The port the server listens on, or null while stopped.
     */
    var port: Int? = null
        private set

    /**
     * Starts listening on the first free port from [preferredPort] to [preferredPort] + 10.
     * [preferredPort] 0 means any free port. Returns the port in use.
     */
    fun start(preferredPort: Int = DEFAULT_PORT): Int {
        check(engine == null) { "server already running" }
        val chosen = pickPort(preferredPort)
        engine = embeddedServer(CIO, port = chosen, host = "0.0.0.0") {
            routing {
                get("/") { call.respondText(page, ContentType.Text.Html) }
                post(UPLOAD_PATH) { handleUpload(call) }
            }
        }.start(wait = false)
        port = chosen
        return chosen
    }

    fun stop() {
        engine?.stop(STOP_GRACE_MS, STOP_TIMEOUT_MS)
        engine = null
        port = null
    }

    private suspend fun handleUpload(call: ApplicationCall) {
        val rawName = call.request.header(FILE_NAME_HEADER)
        if (rawName == null) {
            call.respondText("Missing file name", status = HttpStatusCode.BadRequest)
            return
        }
        val name = URLDecoder.decode(rawName, "UTF-8")
        val size = call.request.header(HttpHeaders.ContentLength)?.toLongOrNull() ?: 0L
        val id = nextId++

        if (!TextImport.isTextFile(name)) {
            setStatus(id, name, size, TransferStatus.Failed(TransferFailure.NOT_TXT))
            call.respondText("Not a .txt file, skipped", status = HttpStatusCode.UnsupportedMediaType)
            return
        }
        if (size > maxBytes) {
            turnAwayTooBig(call, id, name, size)
            return
        }
        setStatus(id, name, size, TransferStatus.Uploading(0))

        val file = bookRepository.newBookFile()
        // the upload grows under a .part name and takes the book's name only once it is whole
        // and checked, so a .part file left behind is always an unfinished upload
        val part = File(file.path + PART_SUFFIX)
        val body = try {
            val body = withContext(ioDispatcher) { receiveInto(part, call, id, name, size) }
            val charCount = body.charCount
            if (charCount != null) {
                check(part.renameTo(file)) { "could not move the upload into place" }
                val titles = bookRepository.getTitles()
                val title = TextImport.uniqueTitle(TextImport.titleFrom(name), titles)
                bookRepository.addBook(file, title, charCount)
            } else {
                part.delete()
            }
            body
        } catch (e: Throwable) {
            // a lost connection, a stopped server or anything else: nothing is left behind
            part.delete()
            file.delete()
            setStatus(id, name, size, TransferStatus.Failed(TransferFailure.INTERRUPTED))
            throw e
        }

        when {
            body.tooBig -> turnAwayTooBig(call, id, name, body.bytes)
            body.charCount == null -> {
                setStatus(id, name, body.bytes, TransferStatus.Failed(TransferFailure.NOT_UTF8))
                call.respondText(
                    text = "Not UTF-8 text, skipped",
                    status = HttpStatusCode.UnprocessableEntity,
                )
            }
            else -> {
                setStatus(id, name, file.length(), TransferStatus.Added)
                call.respondText("ok")
            }
        }
    }

    /**
     * Reads the upload's body into [part], checking and cleaning the text as it arrives, so a
     * big file never sits in memory. [size] is the stated length, 0 when unknown.
     */
    private suspend fun receiveInto(
        part: File,
        call: ApplicationCall,
        id: Int,
        name: String,
        size: Long,
    ): Received {
        val channel = call.receiveChannel()
        val buffer = ByteArray(BUFFER_SIZE)
        var received = 0L
        part.outputStream().use { out ->
            val text = Utf8TextWriter(out)
            while (true) {
                val n = channel.readAvailable(buffer, 0, buffer.size)
                if (n == -1) break
                received += n
                // a body sent without its size can still run past the limit
                if (received > maxBytes) return Received(received, charCount = null, tooBig = true)
                // after a bad byte the rest is only read off the connection, unused
                if (text.valid) text.write(buffer, n)
                if (size > 0) {
                    val percent = (received * 100 / size).toInt().coerceAtMost(99)
                    setStatus(id, name, size, TransferStatus.Uploading(percent))
                }
            }
            // a connection closed early can end the body short of its stated size
            if (size > 0 && received < size) {
                throw EOFException("body ended after $received of $size bytes")
            }
            val charCount = if (text.valid && text.finish()) text.charCount else null
            return Received(received, charCount, tooBig = false)
        }
    }

    private suspend fun turnAwayTooBig(call: ApplicationCall, id: Int, name: String, size: Long) {
        setStatus(id, name, size, TransferStatus.Failed(TransferFailure.TOO_BIG))
        call.respondText(
            text = "Over $MAX_UPLOAD_MB MB, skipped",
            status = HttpStatusCode.PayloadTooLarge,
        )
    }

    /**
     * Adds or updates the transfer with [id]. A transfer keeps its place in the list.
     */
    private fun setStatus(id: Int, name: String, size: Long, status: TransferStatus) {
        val transfer = Transfer(id, name, size, status)
        _transfers.update { list ->
            if (list.any { it.id == id }) list.map { if (it.id == id) transfer else it } else list + transfer
        }
    }

    /**
     * The first port from [preferred] that can be bound, or any free port when [preferred] is 0.
     */
    private fun pickPort(preferred: Int): Int {
        if (preferred == 0) return ServerSocket(0).use { it.localPort }
        for (candidate in preferred..preferred + PORT_TRIES) {
            val free = runCatching { ServerSocket(candidate).close() }.isSuccess
            if (free) return candidate
        }
        error("no free port between $preferred and ${preferred + PORT_TRIES}")
    }

    companion object {
        const val DEFAULT_PORT = 12121
        private const val PORT_TRIES = 10
    }
}
