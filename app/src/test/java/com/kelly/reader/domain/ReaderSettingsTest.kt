package com.kelly.reader.domain

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Checks the key lookups fall back to the defaults.
 */
class ReaderSettingsTest {

    @Test
    fun readerFont_fromKey_knownAndUnknown() {
        assertEquals(ReaderFont.SANS, ReaderFont.fromKey("sans"))
        assertEquals(ReaderFont.SERIF, ReaderFont.fromKey("mono"))
        assertEquals(ReaderFont.SERIF, ReaderFont.fromKey(null))
    }

    @Test
    fun lineSpacing_fromKey_knownAndUnknown() {
        assertEquals(LineSpacing.LOOSE, LineSpacing.fromKey("loose"))
        assertEquals(LineSpacing.NORMAL, LineSpacing.fromKey("huge"))
        assertEquals(1.45f, LineSpacing.fromKey("compact").multiplier)
    }

    @Test
    fun readingMode_fromKey_knownAndUnknown() {
        assertEquals(ReadingMode.SCROLL, ReadingMode.fromKey("scroll"))
        assertEquals(ReadingMode.PAGED, ReadingMode.fromKey("sideways"))
        assertEquals(ReadingMode.PAGED, ReadingMode.fromKey(null))
    }

    @Test
    fun readerSettings_defaults_matchDesign() {
        val settings = ReaderSettings()
        assertEquals(19, settings.fontSize)
        assertEquals(LineSpacing.NORMAL, settings.lineSpacing)
        assertEquals(ReaderFont.SERIF, settings.font)
        assertEquals(null, settings.themeKey)
        assertEquals(ReadingMode.PAGED, settings.readingMode)
    }
}
