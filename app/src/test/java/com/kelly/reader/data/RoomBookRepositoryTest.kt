package com.kelly.reader.data

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File
import java.nio.file.Files

import com.kelly.reader.data.db.BookDao
import com.kelly.reader.data.db.BookEntity

/**
 * Checks the sweep of the books folder: leftovers go, books, other files and anything from
 * the current run stay.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RoomBookRepositoryTest {

    private val root = Files.createTempDirectory("books-test").toFile()
    private val booksDir = File(root, "books").apply { mkdirs() }
    private val start = 1_800_000_000_000L

    private fun row(id: String, title: String, fileName: String) =
        BookEntity(id, title, fileName, 10, 10, 0, 0, 0f, null)

    // a books table held in a list; [unreadable] makes every read fail
    private class FakeBookDao(
        private val rows: List<BookEntity>,
        private val unreadable: Boolean = false,
    ) : BookDao {
        override fun observeAll(): Flow<List<BookEntity>> = flowOf(rows)
        override suspend fun getById(id: String): BookEntity? = rows.firstOrNull { it.id == id }
        override suspend fun getTitles(): List<String> = rows.map { it.title }
        override suspend fun getFileNames(): List<String> {
            if (unreadable) throw IllegalStateException("database closed")
            return rows.map { it.fileName }
        }
        override suspend fun insert(book: BookEntity) = Unit
        override suspend fun deleteByIds(ids: List<String>) = Unit
        override suspend fun updateProgress(
            id: String,
            lastOffset: Int,
            progress: Float,
            openedAt: Long,
        ) = Unit
    }

    private fun file(name: String, changedAt: Long): File =
        File(booksDir, name).apply {
            writeText("x")
            setLastModified(changedAt)
        }

    @After
    fun cleanUp() {
        root.deleteRecursively()
    }

    @Test
    fun deleteOrphanFiles_leftoversFromBefore_goBooksAndOtherFilesStay() = runTest {
        // two books with the same title still have their own files
        val dao = FakeBookDao(listOf(row("b1", "notes", "a.txt"), row("b2", "notes", "b.txt")))
        val repository = RoomBookRepository(
            dao,
            booksDir,
            PageCache(File(root, "pages"), StandardTestDispatcher(testScheduler)),
            StandardTestDispatcher(testScheduler),
        )
        val before = start - 60_000
        val books = listOf(file("a.txt", before), file("b.txt", before))
        val orphan = file("c.txt", before)
        val part = file("d.txt.part", before)
        val other = file("notes.json", before)

        assertEquals(2, repository.deleteOrphanFiles(olderThan = start))
        books.forEach { assertTrue(it.name, it.exists()) }
        assertFalse(orphan.exists())
        assertFalse(part.exists())
        assertTrue(other.exists())
    }

    @Test
    fun deleteOrphanFiles_filesFromThisRun_stay() = runTest {
        val repository = RoomBookRepository(
            FakeBookDao(emptyList()),
            booksDir,
            PageCache(File(root, "pages"), StandardTestDispatcher(testScheduler)),
            StandardTestDispatcher(testScheduler),
        )
        val uploading = file("e.txt.part", start + 1_000)
        val justMoved = file("e.txt", start + 2_000)

        assertEquals(0, repository.deleteOrphanFiles(olderThan = start))
        assertTrue(uploading.exists())
        assertTrue(justMoved.exists())
    }

    @Test
    fun readText_utf8File_returnsWholeText() = runTest {
        val text = "雨停的时候，山谷里升起了一层薄雾。\nThe fog lifted. 😀\n".repeat(1_000)
        File(booksDir, "a.txt").writeBytes(text.toByteArray(Charsets.UTF_8))
        val repository = RoomBookRepository(
            FakeBookDao(emptyList()),
            booksDir,
            PageCache(File(root, "pages"), StandardTestDispatcher(testScheduler)),
            StandardTestDispatcher(testScheduler),
        )
        val book = row("b1", "rain", "a.txt").toBook()
        assertEquals(text, repository.readText(book))
    }

    @Test
    fun deleteOrphanFiles_booksUnreadable_deletesNothing() = runTest {
        val repository = RoomBookRepository(
            FakeBookDao(emptyList(), unreadable = true),
            booksDir,
            PageCache(File(root, "pages"), StandardTestDispatcher(testScheduler)),
            StandardTestDispatcher(testScheduler),
        )
        val orphan = file("c.txt", start - 60_000)
        try {
            repository.deleteOrphanFiles(olderThan = start)
            fail("expected the read to fail")
        } catch (e: IllegalStateException) {
            assertEquals("database closed", e.message)
        }
        assertTrue(orphan.exists())
    }
}
