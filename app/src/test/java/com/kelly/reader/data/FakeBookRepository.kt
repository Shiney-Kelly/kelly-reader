package com.kelly.reader.data

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

    override suspend fun newBookFile(): File = File.createTempFile("book", ".txt").also { files += it }

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

    override suspend fun readText(book: Book): String = texts.getValue(book.id)
}
