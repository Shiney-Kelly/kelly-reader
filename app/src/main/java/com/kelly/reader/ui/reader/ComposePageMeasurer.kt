package com.kelly.reader.ui.reader

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Constraints
import kotlin.math.min

// chars laid out for the first page, about two pages of Latin text at the default size
private const val FIRST_WINDOW = 1_500

// the fewest chars laid out for a page
private const val MIN_WINDOW = 256

/**
 * How many chars to lay out for the next page: half as many again as the last page took, so
 * the window holds the page and a line more without laying out pages that are not needed. A
 * window that turns out too small fits whole and is doubled, so a poor guess costs time only.
 */
internal fun nextWindow(lastPageChars: Int): Int =
    if (lastPageChars <= 0) FIRST_WINDOW else maxOf(MIN_WINDOW, lastPageChars + lastPageChars / 2)

/**
 * A [PageMeasurer] backed by Compose's [TextMeasurer]: lays out a window of text at the page
 * [widthPx] and keeps the lines whose bottom fits within [heightPx]. The window is sized from
 * the page before; lines break greedily, so how far it reaches past the page does not move
 * the page's own line breaks.
 */
class ComposePageMeasurer(
    private val textMeasurer: TextMeasurer,
    private val style: TextStyle,
    private val widthPx: Int,
    private val heightPx: Int,
) : PageMeasurer {

    // chars the last page took, only to size the next window
    private var lastPageChars = 0

    override fun charsThatFit(text: String, start: Int): Int =
        charsThatFit(text, start, nextWindow(lastPageChars)).also { lastPageChars = it }

    private fun charsThatFit(text: String, start: Int, window: Int): Int {
        val end = min(text.length, start + window)
        val chunk = text.substring(start, end)
        if (chunk.isEmpty()) return 0
        val result = textMeasurer.measure(
            text = AnnotatedString(chunk),
            style = style,
            constraints = Constraints(maxWidth = widthPx),
        )
        var fitLines = 0
        while (fitLines < result.lineCount && result.getLineBottom(fitLines) <= heightPx) fitLines++

        // not even one line fits: take one char so paging still moves on
        if (fitLines == 0) return 1
        // the whole window fit and more text remains: measure a bigger window
        if (fitLines == result.lineCount && end < text.length) {
            return charsThatFit(text, start, window * 2)
        }
        // getLineEnd with visibleEnd false includes the newline or space the line broke at
        return result.getLineEnd(fitLines - 1, visibleEnd = false).coerceAtLeast(1)
    }
}
