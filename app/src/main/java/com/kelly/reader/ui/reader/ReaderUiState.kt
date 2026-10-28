package com.kelly.reader.ui.reader

import com.kelly.reader.domain.Book
import com.kelly.reader.domain.ReaderSettings
import com.kelly.reader.domain.formatPercent

/**
 * What the reader shows. [offset] is the char index of the reading spot and the one source of
 * truth for the position: a page start, or after the pages change, a char inside the page that
 * holds it. The page index follows from it and [pageStarts].
 * [pageStarts] is null until the first page has been measured, and [paginationDone] turns
 * true once every page is known. [isRtl] says the text reads right to left. [savedProgress] is
 * the progress saved for the book, and [savedPage] its page from the saved page list of this
 * layout when there is one; both show while the text is still being read. [waitingPage] is
 * the page of [offset] in the full page list held back until the next page turn after a
 * change, so the progress can go by page before that list shows.
 */
data class ReaderUiState(
    val book: Book? = null,
    val savedProgress: Float = 0f,
    val savedPage: SavedPage? = null,
    val text: String = "",
    val isRtl: Boolean = false,
    val offset: Int = 0,
    val pageStarts: IntArray? = null,
    val paginationDone: Boolean = false,
    val waitingPage: SavedPage? = null,
    val settings: ReaderSettings = ReaderSettings(),
) {

    val pageIndex: Int
        get() = pageStarts?.let { Paginator.pageIndexFor(it, offset) } ?: 0

    /**
     * Null until every page is known.
     */
    val pageCount: Int?
        get() = if (paginationDone) pageStarts?.size else null

    /**
     * False while pagination ran from the resume point first and the pages before it are not
     * in yet; the page number means nothing then.
     */
    val pageNumberKnown: Boolean
        get() = pageStarts?.let { it.isNotEmpty() && it[0] == 0 } ?: false

    /**
     * True once the page holding [offset] has both ends known: a later page start exists, or
     * every page is known. Before that the page would run to the end of the book.
     */
    val pageKnown: Boolean
        get() = pageStarts?.let { paginationDone || it.last() > offset } ?: false

    val pageText: String
        get() = if (pageKnown) Paginator.pageText(text, requireNotNull(pageStarts), pageIndex) else ""

    /**
     * The text to lay out for the current page: [pageText] and, when the page ends inside a
     * paragraph, the hidden no-break run, so its last line lays out like the others. Only the
     * first [pageText] chars are to be shown.
     */
    val pageTextForLayout: String
        get() = if (pageKnown) {
            Paginator.pageTextForLayout(text, requireNotNull(pageStarts), pageIndex)
        } else {
            ""
        }

    /**
     * Progress in 0..1: by page once the count is known, by chars before that, and the saved
     * progress while there is no text yet, so a big book does not show 0% while it loads.
     * By page the first page already counts as read, so by chars it runs to the end of the
     * page when that is known: both measures then agree closely, and a new count after a
     * settings change does not drop the first page to 0%. A full list held back after a
     * change already counts by page.
     */
    val progress: Float
        get() {
            val count = pageCount
            val waiting = waitingPage
            return when {
                count != null && count > 0 -> (pageIndex + 1).toFloat() / count
                waiting != null && waiting.count > 0 -> waiting.number.toFloat() / waiting.count
                text.isEmpty() -> savedProgress
                else -> (pageEnd ?: offset).toFloat() / text.length
            }
        }

    // where the page holding the offset ends, while the pages known say so: not when the
    // offset lies before the first page known, nor past the last
    private val pageEnd: Int?
        get() {
            val starts = pageStarts ?: return null
            val index = pageIndex
            val start = starts.getOrNull(index) ?: return null
            return if (start <= offset) starts.getOrNull(index + 1) else null
        }

    val percentLabel: String
        get() = formatPercent(progress)

    // just opened: the text, or its first pages, are not in yet
    private val beforeFirstPages: Boolean
        get() = book == null || pageStarts == null

    /**
     * The page number for the label, null while not known: the saved page until the first
     * pages are in, then the page once the pages before it are measured, or its page in a full
     * list held back after a change.
     */
    val labelPage: Int?
        get() = when {
            beforeFirstPages -> savedPage?.number
            pageNumberKnown -> pageIndex + 1
            else -> waitingPage?.number
        }

    /**
     * The page count for the label, null while not known: the saved count until the first
     * pages are in, then the count once every page is measured, or that of a full list held
     * back after a change.
     */
    val labelPageCount: Int?
        get() = if (beforeFirstPages) savedPage?.count else pageCount ?: waitingPage?.count

    val hasPreviousPage: Boolean
        get() = pageIndex > 0

    val hasNextPage: Boolean
        get() = pageStarts?.let { pageIndex + 1 < it.size } ?: false

    // IntArray needs explicit equality for the data class to compare by content
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ReaderUiState) return false
        return book == other.book &&
            savedProgress == other.savedProgress &&
            savedPage == other.savedPage &&
            text == other.text &&
            isRtl == other.isRtl &&
            offset == other.offset &&
            pageStarts.contentEquals(other.pageStarts) &&
            paginationDone == other.paginationDone &&
            waitingPage == other.waitingPage &&
            settings == other.settings
    }

    override fun hashCode(): Int {
        var result = book.hashCode()
        result = 31 * result + savedProgress.hashCode()
        result = 31 * result + (savedPage?.hashCode() ?: 0)
        result = 31 * result + text.hashCode()
        result = 31 * result + isRtl.hashCode()
        result = 31 * result + offset
        result = 31 * result + (pageStarts?.contentHashCode() ?: 0)
        result = 31 * result + paginationDone.hashCode()
        result = 31 * result + (waitingPage?.hashCode() ?: 0)
        result = 31 * result + settings.hashCode()
        return result
    }
}

/**
 * Page [number] of [count], from a full page list of a layout: a saved one, or one held back.
 */
data class SavedPage(val number: Int, val count: Int)
