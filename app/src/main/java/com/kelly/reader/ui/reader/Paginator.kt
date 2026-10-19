/*
 * Splitting a book into pages. The measuring is behind an interface so the logic runs on the JVM.
 */
package com.kelly.reader.ui.reader

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

// laid out after a page that ends inside a paragraph, on lines that are not shown: no-break
// spaces wider than any line, so the page's last line is not the end of the text and justifies
// like the others; taken out again on copy
internal val NO_BREAK_RUN = " ".repeat(1_000)

// put before the run when the page does not end on a space, as after Chinese, since a line
// may not break between other text and a no-break space; after a space it may, and a zero
// width space there would make Justify stretch that space too
internal const val RUN_BREAK = "​"

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
     * then [NO_BREAK_RUN], after [RUN_BREAK] unless the page ends on a space, so the page's last
     * line is not the end of the text and justifies like the others. Only the page's own lines
     * are to be shown. A page that ends a paragraph or the book is as is.
     */
    fun pageTextForLayout(text: String, pageStarts: IntArray, index: Int): String {
        val page = pageText(text, pageStarts, index)
        val end = pageStarts[index] + page.length
        if (end >= text.length || page.endsWith("\n")) return page
        val breakBefore = if (page.last().isWhitespace()) "" else RUN_BREAK
        return page + breakBefore + NO_BREAK_RUN
    }

    /**
     * Text copied from the pages as the book has it: every [NO_BREAK_RUN] taken out with the
     * [RUN_BREAK] before it and the line break after it, and a [RUN_BREAK] left at the end when
     * a selection ends on a page's last line. A selection over several pages, in scroll mode,
     * comes with a line break between each page's text; after a run that break is not the
     * book's, since that page ends inside a paragraph.
     */
    fun copiedText(copied: String): String = copied
        .replace(RUN_BREAK + NO_BREAK_RUN + "\n", "")
        .replace(NO_BREAK_RUN + "\n", "")
        .replace(RUN_BREAK + NO_BREAK_RUN, "")
        .replace(NO_BREAK_RUN, "")
        .removeSuffix(RUN_BREAK)

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
