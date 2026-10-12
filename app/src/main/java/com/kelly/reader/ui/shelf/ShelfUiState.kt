package com.kelly.reader.ui.shelf

import com.kelly.reader.domain.Book

/**
 * What the shelf shows. [books] is null until the database has answered, so the empty state
 * does not flash before the first load.
 */
data class ShelfUiState(
    val books: List<Book>? = null,
    val editing: Boolean = false,
    val selected: Set<String> = emptySet(),
) {

    val isEmpty: Boolean
        get() = books?.isEmpty() == true

    val selectedCount: Int
        get() = selected.size
}
