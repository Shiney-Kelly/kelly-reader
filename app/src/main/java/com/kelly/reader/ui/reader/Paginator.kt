/*
 * Splitting a book into pages. The measuring is behind an interface so the logic runs on the JVM.
 */
package com.kelly.reader.ui.reader

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

// enough of the paragraph after a page to finish laying out its last line
const val PAGE_TAIL_MAX = 1_000

/**
 * Knows the page size and text style, and answers how much text fits on one page.
 */
fun interface PageMeasurer {

    /**
     * How many chars of [text], counting from [start], fit on one page. Must be at least 1
     * while text remains, so paging always moves forward.
     */
    fun charsThatFit(text: String, start: Int): Int
}

object Paginator {

    /**
     * Finds the pages of [text] on [dispatcher], emitting the start offsets found so far after
     * every page. The last emission holds every page. Empty text is one empty page.
     *
     * With an [anchor] inside the text, the pages from the anchor onward come first, so the
     * page the reader is on shows at once. Then the whole text is paged again from the start
     * with natural breaks only, and emitted once: the anchor is not kept as a page start, so
     * no page before it ends early, and it lands inside the page that now holds it.
     */
    fun paginate(
        text: String,
        measurer: PageMeasurer,
        dispatcher: CoroutineDispatcher = Dispatchers.Default,
        anchor: Int = 0,
    ): Flow<IntArray> = flow {
        val from = if (anchor in 1 until text.length) anchor else 0
        val after = ArrayList<Int>()
        var start = from
        do {
            after += start
            emit(after.toIntArray())
            start += measurer.charsThatFit(text, start).coerceAtLeast(1)
        } while (start < text.length)

        if (from == 0) return@flow
        // all pages again: with a fixed line height every page holds the same number of lines,
        // so the pages counted from the anchor stay shifted from the natural ones to the end
        val all = ArrayList<Int>()
        start = 0
        do {
            all += start
            start += measurer.charsThatFit(text, start).coerceAtLeast(1)
        } while (start < text.length)
        emit(all.toIntArray())
    }.flowOn(dispatcher)

    /**
     * The page that holds char [offset]: the last page whose start is at or before it.
     */
    fun pageIndexFor(pageStarts: IntArray, offset: Int): Int {
        var index = 0
        while (index + 1 < pageStarts.size && pageStarts[index + 1] <= offset) index++
        return index
    }

    /**
     * The text of page [index], up to the next page start or the end.
     */
    fun pageText(text: String, pageStarts: IntArray, index: Int): String {
        val start = pageStarts[index]
        val end = if (index + 1 < pageStarts.size) pageStarts[index + 1] else text.length
        return text.substring(start, end)
    }

    /**
     * The text to lay out for page [index]: when the page ends inside a paragraph, the page and
     * then the rest of that paragraph, at most [maxTail] chars of it, so the page's last line is
     * laid out as a line inside the paragraph, justified like the others, not as its end. Only
     * the page's own lines are to be shown. A page that ends a paragraph or the book is as is.
     */
    fun pageTextWithTail(
        text: String,
        pageStarts: IntArray,
        index: Int,
        maxTail: Int = PAGE_TAIL_MAX,
    ): String {
        val start = pageStarts[index]
        val end = if (index + 1 < pageStarts.size) pageStarts[index + 1] else text.length
        if (end >= text.length || text[end - 1] == '\n') return text.substring(start, end)
        val paragraphEnd = text.indexOf('\n', end).let { if (it < 0) text.length else it }
        return text.substring(start, minOf(paragraphEnd, end + maxTail))
    }

    /**
     * The text of page [index] for scroll mode, where the pages sit one under another: without
     * the one line break it may end on, since a text ending in a line break gets an empty line
     * after it, which would add a blank line where this page meets the next.
     */
    fun scrollPageText(text: String, pageStarts: IntArray, index: Int): String {
        val page = pageText(text, pageStarts, index)
        return when {
            page.endsWith("\r\n") -> page.dropLast(2)
            page.endsWith("\n") -> page.dropLast(1)
            else -> page
        }
    }
}
