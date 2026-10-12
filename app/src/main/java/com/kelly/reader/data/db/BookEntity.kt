package com.kelly.reader.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

import com.kelly.reader.domain.Book

/**
 * Room row for a book. Mirrors [Book] field for field.
 */
@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey val id: String,
    val title: String,
    val fileName: String,
    val sizeBytes: Long,
    val charCount: Int,
    val addedAt: Long,
    val lastOffset: Int,
    val progress: Float,
    val openedAt: Long?,
) {

    /**
     * Converts this row to the domain model.
     */
    fun toBook(): Book = Book(
        id = id,
        title = title,
        fileName = fileName,
        sizeBytes = sizeBytes,
        charCount = charCount,
        addedAt = addedAt,
        lastOffset = lastOffset,
        progress = progress,
        openedAt = openedAt,
    )

    companion object {

        /**
         * Converts a domain [book] to a row.
         */
        fun fromBook(book: Book): BookEntity = BookEntity(
            id = book.id,
            title = book.title,
            fileName = book.fileName,
            sizeBytes = book.sizeBytes,
            charCount = book.charCount,
            addedAt = book.addedAt,
            lastOffset = book.lastOffset,
            progress = book.progress,
            openedAt = book.openedAt,
        )
    }
}
