package com.kelly.reader.ui.reader

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Checks the direction guess for the scripts the app must handle.
 */
class TextDirectionDetectorTest {

    @Test
    fun isRtl_arabic_isTrue() {
        assertTrue(TextDirectionDetector.isRtl("الحكاية الأولى · الفانوس\n\nفي قرية صغيرة"))
        assertTrue(TextDirectionDetector.isRtl("1. حكايات المساء"))
    }

    @Test
    fun isRtl_latinCjkCyrillic_isFalse() {
        assertFalse(TextDirectionDetector.isRtl("Chapter 3 · Low Tide"))
        assertFalse(TextDirectionDetector.isRtl("第一章 · 雨后"))
        assertFalse(TextDirectionDetector.isRtl("Глава 1 · Станция"))
        assertFalse(TextDirectionDetector.isRtl("1장 · 등대"))
    }

    @Test
    fun isRtl_emptyOrDigits_isFalse() {
        assertFalse(TextDirectionDetector.isRtl(""))
        assertFalse(TextDirectionDetector.isRtl("12345\n\n"))
    }
}
