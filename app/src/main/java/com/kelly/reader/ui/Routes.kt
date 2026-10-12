/*
 * Navigation routes. Plain strings, so no serialization plugin is needed.
 */
package com.kelly.reader.ui

object Routes {
    const val SHELF = "shelf"
    const val ADD = "add"
    const val READER = "reader/{$BOOK_ID_ARG}"

    /**
     * The reader route for the book with [bookId].
     */
    fun reader(bookId: String): String = "reader/$bookId"
}

const val BOOK_ID_ARG = "bookId"
