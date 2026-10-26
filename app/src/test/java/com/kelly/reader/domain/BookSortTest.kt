package com.kelly.reader.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.text.Collator
import java.util.Locale

/**
 * Checks each shelf order, the place of books never opened, and the stored keys.
 */
class BookSortTest {

    private fun book(id: String, title: String, addedAt: Long, openedAt: Long? = null) =
        Book(id, title, "$id.txt", 10, 10, addedAt, openedAt = openedAt)

    private val books = listOf(
        book("a", "harbor", addedAt = 1, openedAt = 50),
        book("b", "Apple", addedAt = 2),
        book("c", "Ébène", addedAt = 3, openedAt = 90),
        book("d", "cuentos", addedAt = 4),
    )

    private fun ids(sort: BookSort): List<String> =
        sort.sort(books, Collator.getInstance(Locale.FRENCH)).map { it.id }

    @Test
    fun sort_lastRead_openedNewestFirstThenNeverOpenedNewestAdded() {
        assertEquals(listOf("c", "a", "d", "b"), ids(BookSort.LAST_READ))
    }

    @Test
    fun sort_added_bothWays() {
        assertEquals(listOf("d", "c", "b", "a"), ids(BookSort.ADDED_NEWEST))
        assertEquals(listOf("a", "b", "c", "d"), ids(BookSort.ADDED_OLDEST))
    }

    @Test
    fun sort_title_followsLanguageRulesNotCodePoints() {
        // by code point "É" would come after every plain letter and capitals before small ones
        assertEquals(listOf("b", "d", "c", "a"), ids(BookSort.TITLE))
    }

    @Test
    fun fromKey_knownAndUnknown() {
        assertEquals(BookSort.LAST_READ, BookSort.fromKey("last_read"))
        assertEquals(BookSort.TITLE, BookSort.fromKey("title"))
        assertEquals(BookSort.ADDED_OLDEST, BookSort.fromKey("by_color"))
        assertEquals(BookSort.ADDED_OLDEST, BookSort.fromKey(null))
    }
}
