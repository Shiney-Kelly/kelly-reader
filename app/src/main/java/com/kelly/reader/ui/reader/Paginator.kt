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
     * Finds the pages of [text] one after another on [dispatcher], emitting the start offsets
     * found so far after every page. The last emission holds every page. Empty text is one
     * empty page.
     */
    fun paginate(
        text: String,
        measurer: PageMeasurer,
        dispatcher: CoroutineDispatcher = Dispatchers.Default,
    ): Flow<IntArray> = flow {
        val starts = ArrayList<Int>()
        var start = 0
        do {
            starts += start
            emit(starts.toIntArray())
            val fit = measurer.charsThatFit(text, start).coerceAtLeast(1)
            start += fit
        } while (start < text.length)
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
