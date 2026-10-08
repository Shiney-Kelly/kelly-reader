package com.kelly.reader.ui.reader

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Constraints
import kotlin.math.min

// chars measured at a time, about three pages of Latin text; doubled when a whole window fits
private const val WINDOW = 4_000

/**
 * A [PageMeasurer] backed by Compose's [TextMeasurer]: lays out a window of text at the page
 * [widthPx] and keeps the lines whose bottom fits within [heightPx].
 */
class ComposePageMeasurer(
    private val textMeasurer: TextMeasurer,
    private val style: TextStyle,
    private val widthPx: Int,
    private val heightPx: Int,
) : PageMeasurer {

    override fun charsThatFit(text: String, start: Int): Int = charsThatFit(text, start, WINDOW)

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
