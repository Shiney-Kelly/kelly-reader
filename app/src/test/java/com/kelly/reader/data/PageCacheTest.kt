package com.kelly.reader.data

import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.io.DataOutputStream
import java.io.File
import java.nio.file.Files

/**
 * Checks the page cache files in a temporary folder.
 */
class PageCacheTest {

    private lateinit var dir: File
    private lateinit var cache: PageCache

    @Before
    fun setUp() {
        dir = Files.createTempDirectory("pages").toFile()
        cache = PageCache(dir)
    }

    @After
    fun tearDown() {
        dir.deleteRecursively()
    }

    @Test
    fun load_nothingSaved_returnsNull() = runTest {
        assertNull(cache.load("b1", "k"))
    }

    @Test
    fun saveThenLoad_roundTrips() = runTest {
        val starts = intArrayOf(0, 412, 833, 1290)
        cache.save("b1", "v1-1080x1920-19-32.3-serif", starts)
        assertArrayEquals(starts, cache.load("b1", "v1-1080x1920-19-32.3-serif"))
        assertNull(cache.load("b1", "v1-1080x1920-20-34.0-serif"))
        assertNull(cache.load("b2", "v1-1080x1920-19-32.3-serif"))
    }

    @Test
    fun save_again_replaces() = runTest {
        cache.save("b1", "k", intArrayOf(0, 10))
        cache.save("b1", "k", intArrayOf(0, 7, 14))
        assertArrayEquals(intArrayOf(0, 7, 14), cache.load("b1", "k"))
    }

    @Test
    fun clear_dropsEveryKeyOfTheBook() = runTest {
        cache.save("b1", "a", intArrayOf(0))
        cache.save("b1", "b", intArrayOf(0))
        cache.save("b2", "a", intArrayOf(0))
        cache.clear("b1")
        assertNull(cache.load("b1", "a"))
        assertNull(cache.load("b1", "b"))
        assertArrayEquals(intArrayOf(0), cache.load("b2", "a"))
    }

    @Test
    fun load_olderFormatVersion_returnsNull() = runTest {
        // a version 1 file, as saved while a page could still be cut short at the resume point
        val file = File(File(dir, "b1"), "k.pages")
        file.parentFile?.mkdirs()
        DataOutputStream(file.outputStream()).use { output ->
            output.writeInt(1)
            output.writeInt(3)
            intArrayOf(0, 4, 5).forEach(output::writeInt)
        }
        assertNull(cache.load("b1", "k"))
    }

    @Test
    fun load_corruptFile_returnsNull() = runTest {
        cache.save("b1", "k", intArrayOf(0, 5))
        File(File(dir, "b1"), "k.pages").writeBytes(byteArrayOf(1, 2, 3))
        assertNull(cache.load("b1", "k"))
    }
}
