package com.kelly.reader.ui.reader

import com.kelly.reader.domain.Book
import com.kelly.reader.domain.ReaderSettings

/**
 * What the reader shows. [offset] is the char index of the current page start and the one
 * source of truth for the position; the page index follows from it and [pageStarts].
 * [pageStarts] is null until the first page has been measured, and [paginationDone] turns
 * true once every page is known.
 */
data class ReaderUiState(
    val book: Book? = null,
    val text: String = "",
    val offset: Int = 0,
    val pageStarts: IntArray? = null,
    val paginationDone: Boolean = false,
    val settings: ReaderSettings = ReaderSettings(),
) {

    val pageIndex: Int
        get() = pageStarts?.let { Paginator.pageIndexFor(it, offset) } ?: 0

    /**
     * Null until every page is known.
     */
    val pageCount: Int?
        get() = if (paginationDone) pageStarts?.size else null

    val pageText: String
        get() = pageStarts?.let { Paginator.pageText(text, it, pageIndex) } ?: ""

    /**
     * Progress in 0..1: by page once the count is known, by chars before that.
     */
    val progress: Float
        get() {
            val count = pageCount
            return when {
                count != null && count > 0 -> (pageIndex + 1).toFloat() / count
                text.isEmpty() -> 0f
                else -> offset.toFloat() / text.length
            }
        }

    val percent: Int
        get() = (progress * 100).toInt()

    val hasPreviousPage: Boolean
        get() = pageIndex > 0

    val hasNextPage: Boolean
        get() = pageStarts?.let { pageIndex + 1 < it.size } ?: false

    // IntArray needs explicit equality for the data class to compare by content
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ReaderUiState) return false
        return book == other.book &&
            text == other.text &&
            offset == other.offset &&
            pageStarts.contentEquals(other.pageStarts) &&
            paginationDone == other.paginationDone &&
            settings == other.settings
    }

    override fun hashCode(): Int {
        var result = book.hashCode()
        result = 31 * result + text.hashCode()
        result = 31 * result + offset
        result = 31 * result + (pageStarts?.contentHashCode() ?: 0)
        result = 31 * result + paginationDone.hashCode()
        result = 31 * result + settings.hashCode()
        return result
    }
}
