/*
 * The reading settings a user can change in the drawer, with their stored keys and defaults.
 */
package com.kelly.reader.domain

/**
 * The font used for the reading text. [key] is what the settings store.
 */
enum class ReaderFont(val key: String) {
    SERIF("serif"),
    SANS("sans");

    companion object {

        /**
         * Returns the font stored under [key], or the default for an unknown key.
         */
        fun fromKey(key: String?): ReaderFont = entries.firstOrNull { it.key == key } ?: SERIF
    }
}

/**
 * Line height as a multiple of the font size. [key] is what the settings store.
 */
enum class LineSpacing(val key: String, val multiplier: Float) {
    COMPACT("compact", 1.45f),
    NORMAL("normal", 1.7f),
    LOOSE("loose", 2.0f);

    companion object {

        /**
         * Returns the spacing stored under [key], or the default for an unknown key.
         */
        fun fromKey(key: String?): LineSpacing = entries.firstOrNull { it.key == key } ?: NORMAL
    }
}

/**
 * How the text moves: one page at a time on a tap, or as one long column that scrolls.
 * [key] is what the settings store.
 */
enum class ReadingMode(val key: String) {
    PAGED("paged"),
    SCROLL("scroll");

    companion object {

        /**
         * Returns the mode stored under [key], or the default for an unknown key.
         */
        fun fromKey(key: String?): ReadingMode = entries.firstOrNull { it.key == key } ?: PAGED
    }
}

/**
 * How lines sit between the margins. [key] is what the settings store.
 */
enum class TextAlignment(val key: String) {
    LEFT("left"),
    JUSTIFY("justify"),
    RIGHT("right");

    companion object {

        /**
         * Returns the alignment stored under [key], or the default for an unknown key.
         */
        fun fromKey(key: String?): TextAlignment = entries.firstOrNull { it.key == key } ?: JUSTIFY
    }
}

/**
 * What the reader's lower right corner shows: how far into the book in percent, or the page
 * number. [key] is what the settings store.
 */
enum class ProgressLabel(val key: String) {
    PERCENT("percent"),
    PAGE("page");

    companion object {

        /**
         * Returns the label stored under [key], or the default for an unknown key.
         */
        fun fromKey(key: String?): ProgressLabel = entries.firstOrNull { it.key == key } ?: PERCENT
    }
}

/**
 * Everything the settings drawer controls. [themeKey] is null until the user picks a reader
 * theme, which means follow the system.
 */
data class ReaderSettings(
    val fontSize: Int = DEFAULT_FONT_SIZE,
    val lineSpacing: LineSpacing = LineSpacing.NORMAL,
    val font: ReaderFont = ReaderFont.SERIF,
    val themeKey: String? = null,
    val readingMode: ReadingMode = ReadingMode.PAGED,
    val textAlignment: TextAlignment = TextAlignment.JUSTIFY,
    val progressLabel: ProgressLabel = ProgressLabel.PERCENT,
) {

    companion object {
        const val MIN_FONT_SIZE = 14
        const val MAX_FONT_SIZE = 30
        const val DEFAULT_FONT_SIZE = 20
    }
}
