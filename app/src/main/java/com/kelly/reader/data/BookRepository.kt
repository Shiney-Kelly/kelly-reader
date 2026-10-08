package com.kelly.reader.data

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

import com.kelly.reader.data.db.BookDao
import com.kelly.reader.data.db.BookEntity
import com.kelly.reader.domain.Book

/**
 * Single source of truth for books. An interface so tests can use a fake.
 */
interface BookRepository {

    /**
     * All books in the order they were added, updated whenever a row changes.
     */
    fun observeBooks(): Flow<List<Book>>

    /**
     * Returns the book with [id], or null when it was deleted.
     */
    suspend fun getBook(id: String): Book?

    /**
     * Titles already on the shelf, so a new book can get a unique one.
     */
    suspend fun getTitles(): List<String>

    /**
     * A fresh file in the books folder for an upload to stream into. Nothing is recorded until
     * [addBook] is called with it.
     */
    suspend fun newBookFile(): File

    /**
     * Records a book whose text is already in [file] (a file from [newBookFile]) and returns it.
     */
    suspend fun addBook(file: File, title: String, charCount: Int): Book

    /**
     * Removes the books with [ids] and deletes their files.
     */
    suspend fun deleteBooks(ids: List<String>)

    /**
     * Saves where the reader is in the book with [id] and marks it opened.
     */
    suspend fun saveProgress(id: String, lastOffset: Int, progress: Float)

    /**
     * Reads the whole text of [book].
     */
    suspend fun readText(book: Book): String
}

/**
 * Books as rows in Room plus one text file per book in [booksDir]. Every call is main-safe;
 * file work runs on [ioDispatcher].
 */
class RoomBookRepository(
    private val bookDao: BookDao,
    private val booksDir: File,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : BookRepository {

    override fun observeBooks(): Flow<List<Book>> =
        bookDao.observeAll().map { rows -> rows.map(BookEntity::toBook) }

    override suspend fun getBook(id: String): Book? = bookDao.getById(id)?.toBook()

    override suspend fun getTitles(): List<String> = bookDao.getTitles()

    override suspend fun newBookFile(): File = withContext(ioDispatcher) {
        booksDir.mkdirs()
        File(booksDir, "${UUID.randomUUID()}.txt")
    }

    override suspend fun addBook(file: File, title: String, charCount: Int): Book {
        val book = Book(
            id = UUID.randomUUID().toString(),
            title = title,
            fileName = file.name,
            sizeBytes = file.length(),
            charCount = charCount,
            addedAt = System.currentTimeMillis(),
        )
        bookDao.insert(BookEntity.fromBook(book))
        return book
    }

    override suspend fun deleteBooks(ids: List<String>) {
        if (ids.isEmpty()) return
        val files = ids.mapNotNull { id -> bookDao.getById(id)?.fileName }
        bookDao.deleteByIds(ids)
        withContext(ioDispatcher) {
            files.forEach { name -> File(booksDir, name).delete() }
        }
    }

    override suspend fun saveProgress(id: String, lastOffset: Int, progress: Float) {
        bookDao.updateProgress(id, lastOffset, progress, System.currentTimeMillis())
    }

    // files are stored as UTF-8 with the BOM already stripped at import, so this is a plain read
    override suspend fun readText(book: Book): String = withContext(ioDispatcher) {
        File(booksDir, book.fileName).readText(Charsets.UTF_8)
    }
}
