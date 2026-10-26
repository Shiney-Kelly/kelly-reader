package com.kelly.reader.ui.shelf

import com.kelly.reader.domain.Book
import com.kelly.reader.domain.BookSort

/**
 * What the shelf shows. [books] is null until the database has answered, so the empty state
 * does not flash before the first load, and comes in [sort] order. [shuffledIds] holds the
 * mixed up order of the covers for a moment after a shake, and is null the rest of the time.
 * [petFound] tells whether someone has found the shelf bear; until then only its ledge shows.
 */
data class ShelfUiState(
    val books: List<Book>? = null,
    val sort: BookSort = BookSort.ADDED_OLDEST,
    val editing: Boolean = false,
    val selected: Set<String> = emptySet(),
    val shuffledIds: List<String>? = null,
    val petFound: Boolean = false,
) {

    val isEmpty: Boolean
        get() = books?.isEmpty() == true

    val selectedCount: Int
        get() = selected.size

    val shuffling: Boolean
        get() = shuffledIds != null

    /**
     * The books in the order the grid shows them: mixed up while [shuffledIds] is set, otherwise
     * as stored. A book that arrives during a shuffle goes at the end.
     */
    val shownBooks: List<Book>?
        get() {
            val order = shuffledIds ?: return books
            val all = books ?: return null
            val byId = all.associateBy { it.id }
            val ids = order.toSet()
            return order.mapNotNull { byId[it] } + all.filter { it.id !in ids }
        }
}
