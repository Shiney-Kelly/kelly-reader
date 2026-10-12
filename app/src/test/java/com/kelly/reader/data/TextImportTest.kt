package com.kelly.reader.data

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Checks UTF-8 decoding across scripts, the BOM, newlines, bad bytes and titles.
 */
class TextImportTest {

    private val samples = listOf(
        "The harbor woke slowly that morning.",
        "雨停的时候，山谷里升起了一层薄雾。",
        "朝から雨が降っていた。",
        "섬의 끝에는 오래된 등대가 있었다.",
        "Поезд пришёл поздно ночью.",
        "في قرية صغيرة على حافة الصحراء",
        "छोटी कहानियाँ",
    )

    @Test
    fun decodeUtf8_everyScript_roundTrips() {
        samples.forEach { sample ->
            assertEquals(sample, TextImport.decodeUtf8(sample.toByteArray(Charsets.UTF_8)))
        }
    }

    @Test
    fun decodeUtf8_bom_isStripped() {
        val bytes = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) + "Hi".toByteArray()
        assertEquals("Hi", TextImport.decodeUtf8(bytes))
    }

    @Test
    fun decodeUtf8_windowsAndMacNewlines_becomeUnix() {
        assertEquals("a\nb\nc\n", TextImport.decodeUtf8("a\r\nb\rc\n".toByteArray()))
    }

    @Test
    fun decodeUtf8_invalidBytes_returnsNull() {
        assertNull(TextImport.decodeUtf8(byteArrayOf(0xFF.toByte(), 0xFE.toByte(), 0x41)))
        // a GBK encoded "中" is not valid UTF-8: the second byte is not a continuation byte
        assertNull(TextImport.decodeUtf8(byteArrayOf(0xD6.toByte(), 0xD0.toByte())))
    }

    @Test
    fun decodeUtf8_empty_returnsEmpty() {
        assertEquals("", TextImport.decodeUtf8(ByteArray(0)))
    }

    @Test
    fun prepareFile_bomFile_isRewrittenClean() {
        val file = File.createTempFile("import", ".txt")
        file.writeBytes(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) + "x\r\ny".toByteArray())
        assertEquals("x\ny", TextImport.prepareFile(file))
        assertArrayEquals("x\ny".toByteArray(), file.readBytes())
        file.delete()
    }

    @Test
    fun prepareFile_cleanFile_isLeftAlone() {
        val file = File.createTempFile("import", ".txt")
        file.writeText("山间小记\n")
        val before = file.lastModified()
        assertEquals("山间小记\n", TextImport.prepareFile(file))
        assertEquals(before, file.lastModified())
        file.delete()
    }

    @Test
    fun prepareFile_notUtf8_returnsNull() {
        val file = File.createTempFile("import", ".txt")
        file.writeBytes(byteArrayOf(0xD6.toByte(), 0xD0.toByte()))
        assertNull(TextImport.prepareFile(file))
        file.delete()
    }

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
