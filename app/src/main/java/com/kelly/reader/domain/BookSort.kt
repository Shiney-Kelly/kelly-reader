package com.kelly.reader.domain

import java.text.Collator

/**
 * The orders the shelf can show its books in. [key] is what the settings store; an unknown or
 * missing key means [ADDED_OLDEST], the order the shelf always had. [TITLE_A_TO_Z] keeps the
 * key it had when it was the only title order.
 */
enum class BookSort(val key: String) {
    LAST_READ("last_read"),
    ADDED_NEWEST("added_newest"),
    ADDED_OLDEST("added_oldest"),
    TITLE_A_TO_Z("title"),
    TITLE_Z_TO_A("title_z_to_a");

    /**
     * [books] in this order. Last read puts books never opened after the rest, newest added
     * first; titles follow [collator], the phone's language rules by default, either way round.
     * Ties keep the order the books were added in.
     */
    fun sort(books: List<Book>, collator: Collator = Collator.getInstance()): List<Book> =
        when (this) {
            LAST_READ -> books.sortedWith(
                compareByDescending<Book> { it.openedAt ?: Long.MIN_VALUE }
                    .thenByDescending { it.addedAt },
            )
            ADDED_NEWEST -> books.sortedByDescending { it.addedAt }
            ADDED_OLDEST -> books.sortedBy { it.addedAt }
            TITLE_A_TO_Z -> books.sortedWith(
                compareBy<Book, String>(collator) { it.title }.thenBy { it.addedAt },
            )
            TITLE_Z_TO_A -> books.sortedWith(
                compareBy<Book, String>(collator.reversed()) { it.title }.thenBy { it.addedAt },
            )
        }

    companion object {

        /**
         * Returns the order stored under [key], or the default for an unknown key.
         */
        fun fromKey(key: String?): BookSort = entries.firstOrNull { it.key == key } ?: ADDED_OLDEST
    }
}
