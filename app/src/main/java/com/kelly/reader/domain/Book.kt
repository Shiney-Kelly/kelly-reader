package com.kelly.reader.domain

import kotlin.math.abs

// how many cover colors the theme offers, see LIGHT_COVER_COLORS
const val COVER_COLOR_COUNT = 9

/**
 * A book on the shelf. The text itself lives in the file named [fileName] under the books folder.
 * [lastOffset] is the char index where the reader resumes, [progress] is 0..1 for the shelf bar,
 * and [openedAt] is null until the book is opened for the first time.
 */
data class Book(
    val id: String,
    val title: String,
    val fileName: String,
    val sizeBytes: Long,
    val charCount: Int,
    val addedAt: Long,
    val lastOffset: Int = 0,
    val progress: Float = 0f,
    val openedAt: Long? = null,
) {

    /**
     * True until the book has been opened once; the shelf shows "New" instead of a percentage.
     */
    val isNew: Boolean
        get() = openedAt == null

    /**
     * Index into the cover palette, stable for the file name.
     */
    val coverColorIndex: Int
        get() = abs(fileName.hashCode()) % COVER_COLOR_COUNT
}
