/*
 * The three reading themes from the design: Light, Paper and Dark. They color only the reader.
 */
package com.kelly.reader.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * A reading theme: page background, text color and the muted color for the title and footer.
 * [key] is what the settings store.
 */
enum class ReaderTheme(
    val key: String,
    val background: Color,
    val ink: Color,
    val muted: Color,
) {
    LIGHT("light", READER_LIGHT_BG, READER_LIGHT_INK, READER_LIGHT_MUTED),
    PAPER("paper", READER_PAPER_BG, READER_PAPER_INK, READER_PAPER_MUTED),
    DARK("dark", READER_DARK_BG, READER_DARK_INK, READER_DARK_MUTED);

    companion object {

        /**
         * Returns the theme stored under [key], or null for an unknown or missing key.
         */
        fun fromKey(key: String?): ReaderTheme? = entries.firstOrNull { it.key == key }

        /**
         * The theme to use when the user has not picked one: Dark when the system is dark.
         */
        fun forSystem(darkTheme: Boolean): ReaderTheme = if (darkTheme) DARK else LIGHT
    }
}
