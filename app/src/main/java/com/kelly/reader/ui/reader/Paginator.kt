/*
 * Splitting a book into pages. The measuring is behind an interface so the logic runs on the JVM.
 */
package com.kelly.reader.ui.reader

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

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
     * page the reader is on shows at once; then the pages before it are filled in from the
     * start and emitted once, with the page that would cross the anchor cut at the anchor.
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
        val before = ArrayList<Int>()
        start = 0
        while (start < from) {
            before += start
            start += measurer.charsThatFit(text, start).coerceAtLeast(1)
        }
        emit((before + after).toIntArray())
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
}
