/*
 * Decides whether a book reads right to left, from the first strong characters of its text.
 */
package com.kelly.reader.ui.reader

import java.text.Bidi

// enough to pass a title line and reach the first paragraph
private const val SAMPLE_LENGTH = 500

object TextDirectionDetector {

    /**
     * True when the first strong character in [text] is right-to-left (Arabic, Hebrew and so
     * on). Text with no strong character, such as digits only or nothing, reads left to right.
     */
    fun isRtl(text: String): Boolean {
        if (text.isEmpty()) return false
        val sample = text.take(SAMPLE_LENGTH)
        return !Bidi(sample, Bidi.DIRECTION_DEFAULT_LEFT_TO_RIGHT).baseIsLeftToRight()
    }
}
