package com.kelly.reader.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Database access for the books table. The shelf keeps books in the order they were added.
 */
@Dao
interface BookDao {

    @Query("SELECT * FROM books ORDER BY addedAt ASC")
    fun observeAll(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE id = :id")
    suspend fun getById(id: String): BookEntity?

    @Query("SELECT title FROM books")
    suspend fun getTitles(): List<String>

    @Insert
    suspend fun insert(book: BookEntity)

    @Query("DELETE FROM books WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)

    @Query(
        "UPDATE books SET lastOffset = :lastOffset, progress = :progress, openedAt = :openedAt " +
            "WHERE id = :id",
    )
    suspend fun updateProgress(id: String, lastOffset: Int, progress: Float, openedAt: Long)
}
