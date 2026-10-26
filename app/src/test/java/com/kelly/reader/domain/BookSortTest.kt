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
    fun sort_titleAToZ_followsLanguageRulesNotCodePoints() {
        // by code point "É" would come after every plain letter and capitals before small ones
        assertEquals(listOf("b", "d", "c", "a"), ids(BookSort.TITLE_A_TO_Z))
    }

    @Test
    fun sort_titleZToA_reversesTheTitlesTiesStillOldestFirst() {
        assertEquals(listOf("a", "c", "d", "b"), ids(BookSort.TITLE_Z_TO_A))
        val twins = listOf(
            book("x", "Same", addedAt = 1),
            book("y", "same", addedAt = 2),
            book("z", "Other", addedAt = 3),
        )
        // a collator at primary strength sees "Same" and "same" as equal
        val collator = Collator.getInstance(Locale.FRENCH).apply { strength = Collator.PRIMARY }
        val sorted = BookSort.TITLE_Z_TO_A.sort(twins, collator).map { it.id }
        assertEquals(listOf("x", "y", "z"), sorted)
    }

    @Test
    fun fromKey_knownAndUnknown() {
        assertEquals(BookSort.LAST_READ, BookSort.fromKey("last_read"))
        assertEquals(BookSort.TITLE_A_TO_Z, BookSort.fromKey("title"))
        assertEquals(BookSort.TITLE_Z_TO_A, BookSort.fromKey("title_z_to_a"))
        assertEquals(BookSort.ADDED_OLDEST, BookSort.fromKey("by_color"))
        assertEquals(BookSort.ADDED_OLDEST, BookSort.fromKey(null))
    }
}
