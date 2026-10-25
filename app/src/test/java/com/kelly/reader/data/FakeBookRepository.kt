package com.kelly.reader.data

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import java.io.File

import com.kelly.reader.domain.Book

/**
 * In-memory [BookRepository] for ViewModel tests. Text is kept per book id in [texts].
 */
class FakeBookRepository(initial: List<Book> = emptyList()) : BookRepository {

    val books = MutableStateFlow(initial)
    val texts = mutableMapOf<String, String>()
    val deleted = mutableListOf<String>()
    val files = mutableListOf<File>()

    override fun observeBooks(): Flow<List<Book>> = books

    override suspend fun getBook(id: String): Book? = books.value.firstOrNull { it.id == id }

    override suspend fun getTitles(): List<String> = books.value.map { it.title }

    // a free name only, like the real one: the upload makes the file
    override suspend fun newBookFile(): File =
        File.createTempFile("book", ".txt").also { it.delete(); files += it }

    override suspend fun addBook(file: File, title: String, charCount: Int): Book {
        val book = Book(
            id = "book-${books.value.size + 1}",
            title = title,
            fileName = file.name,
            sizeBytes = file.length(),
            charCount = charCount,
            addedAt = books.value.size.toLong(),
        )
        books.update { it + book }
        return book
    }

    override suspend fun deleteBooks(ids: List<String>) {
        deleted += ids
        books.update { list -> list.filterNot { it.id in ids } }
    }

    override suspend fun saveProgress(id: String, lastOffset: Int, progress: Float) {
        books.update { list ->
            list.map { book ->
                if (book.id == id) {
                    book.copy(lastOffset = lastOffset, progress = progress, openedAt = 1L)
                } else {
                    book
                }
            }
        }
    }

    // when set, reading a text waits for it, as a big book takes a while
    var readTextGate: CompletableDeferred<Unit>? = null

    override suspend fun readText(book: Book): String {
        readTextGate?.await()
        return texts.getValue(book.id)
    }

    val sweeps = mutableListOf<Long>()

    override suspend fun deleteOrphanFiles(olderThan: Long): Int {
        sweeps += olderThan
        return 0
    }
}
