package com.kelly.reader.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Checks the .txt test and the titles made from file names.
 */
class TextImportTest {

    @Test
    fun isTextFile_checksExtensionIgnoringCase() {
        assertTrue(TextImport.isTextFile("book.txt"))
        assertTrue(TextImport.isTextFile("BOOK.TXT"))
        assertFalse(TextImport.isTextFile("cover-scan.pdf"))
        assertFalse(TextImport.isTextFile("txt"))
    }

    @Test
    fun titleFrom_dropsExtensionAndTrims() {
        assertEquals("The Quiet Harbor", TextImport.titleFrom(" The Quiet Harbor .txt"))
        assertEquals("雨の日の手紙", TextImport.titleFrom("雨の日の手紙.txt"))
        assertEquals("notes.final", TextImport.titleFrom("notes.final.txt"))
        assertEquals(".txt", TextImport.titleFrom(".txt"))
    }

    @Test
    fun uniqueTitle_addsCounter() {
        assertEquals("A", TextImport.uniqueTitle("A", emptyList()))
        assertEquals("A (2)", TextImport.uniqueTitle("A", listOf("A")))
        assertEquals("A (3)", TextImport.uniqueTitle("A", listOf("A", "A (2)")))
    }
}
