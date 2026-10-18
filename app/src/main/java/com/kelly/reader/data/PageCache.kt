package com.kelly.reader.data

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.IOException

// bump when the page format or the measuring changes, so old files are ignored
private const val FORMAT_VERSION = 2

/**
 * Page start offsets saved on disk, one file per book and settings key under [dir], so a book
 * does not have to be measured again on every open.
 */
class PageCache(
    private val dir: File,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {

    /**
     * The saved page starts for [bookId] under [key], or null when there are none.
     */
    suspend fun load(bookId: String, key: String): IntArray? = withContext(ioDispatcher) {
        val file = file(bookId, key)
        if (!file.isFile) return@withContext null
        try {
            DataInputStream(file.inputStream().buffered()).use { input ->
                if (input.readInt() != FORMAT_VERSION) return@withContext null
                IntArray(input.readInt()) { input.readInt() }
            }
        } catch (e: IOException) {
            null
        }
    }

    /**
     * Saves [starts] for [bookId] under [key], replacing any older file.
     */
    suspend fun save(bookId: String, key: String, starts: IntArray) = withContext(ioDispatcher) {
        val file = file(bookId, key)
        file.parentFile?.mkdirs()
        val temp = File(file.path + ".tmp")
        DataOutputStream(temp.outputStream().buffered()).use { output ->
            output.writeInt(FORMAT_VERSION)
            output.writeInt(starts.size)
            starts.forEach(output::writeInt)
        }
        if (!temp.renameTo(file)) {
            file.delete()
            temp.renameTo(file)
        }
    }

    /**
     * Drops every saved file for [bookId].
     */
    suspend fun clear(bookId: String) = withContext(ioDispatcher) {
        File(dir, bookId).deleteRecursively()
        Unit
    }

    // the key holds sizes and names, so anything outside a safe set becomes an underscore
    private fun file(bookId: String, key: String): File =
        File(File(dir, bookId), key.replace(Regex("[^A-Za-z0-9._-]"), "_") + ".pages")
}
