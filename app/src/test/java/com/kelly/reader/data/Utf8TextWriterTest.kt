package com.kelly.reader.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream

/**
 * Checks that cleaning piece by piece gives the same text whatever the piece sizes, and that
 * bad or cut off bytes are caught.
 */
class Utf8TextWriterTest {

    private val samples = listOf(
        "The harbor woke slowly that morning.",
        "雨停的时候，山谷里升起了一层薄雾。",
        "섬의 끝에는 오래된 등대가 있었다.",
        "في قرية صغيرة على حافة الصحراء",
        "﻿a BOM first, a﻿kept one later",
        "windows\r\nlines\r\nand old mac\rlines\r\r\nand\n\rmixed\r",
        "emoji 😀 and 雨\r\n",
        "",
    )

    // the cleanup a whole file gets on import
    private fun cleaned(text: String): String =
        text.removePrefix("﻿").replace("\r\n", "\n").replace('\r', '\n')

    private fun writeInPieces(bytes: ByteArray, piece: Int): Pair<Boolean, Utf8TextWriter> {
        val out = ByteArrayOutputStream()
        val writer = Utf8TextWriter(out)
        var ok = true
        var start = 0
        while (start < bytes.size && ok) {
            val end = minOf(start + piece, bytes.size)
            ok = writer.write(bytes.copyOfRange(start, end))
            start = end
        }
        return (ok && writer.finish()) to writer
    }

    @Test
    fun write_everyPieceSize_matchesWholeFileCleanup() {
        samples.forEach { sample ->
            val bytes = sample.toByteArray(Charsets.UTF_8)
            (1..7).plus(bytes.size.coerceAtLeast(1)).forEach { piece ->
                val out = ByteArrayOutputStream()
                val writer = Utf8TextWriter(out)
                bytes.toList().chunked(piece).forEach { writer.write(it.toByteArray()) }
                assertTrue(writer.finish())
                val expected = cleaned(sample)
                assertEquals("piece $piece of '$sample'", expected, out.toString("UTF-8"))
                assertEquals(expected.length, writer.charCount)
            }
        }
    }

    @Test
    fun write_carriageReturnEndsOnePieceLineFeedStartsNext_oneLineBreak() {
        val out = ByteArrayOutputStream()
        val writer = Utf8TextWriter(out)
        writer.write("a\r".toByteArray())
        writer.write("\nb".toByteArray())
        assertTrue(writer.finish())
        assertEquals("a\nb", out.toString("UTF-8"))
    }

    @Test
    fun write_lengthShorterThanBuffer_usesOnlyThoseBytes() {
        val out = ByteArrayOutputStream()
        val writer = Utf8TextWriter(out)
        writer.write("abcXYZ".toByteArray(), length = 3)
        assertTrue(writer.finish())
        assertEquals("abc", out.toString("UTF-8"))
    }

    @Test
    fun write_invalidBytes_returnsFalse() {
        assertFalse(writeInPieces(byteArrayOf(0xFF.toByte(), 0xFE.toByte(), 0x41), 2).first)
        // a GBK encoded "中" is not valid UTF-8: the second byte is not a continuation byte
        val gbk = "ok ".toByteArray() + byteArrayOf(0xD6.toByte(), 0xD0.toByte())
        val (ok, writer) = writeInPieces(gbk, piece = 64)
        assertFalse(ok)
        assertFalse(writer.valid)
        assertFalse(writer.write("more".toByteArray()))
    }

    @Test
    fun finish_cutInsideCharacter_returnsFalse() {
        val bytes = "雨".toByteArray(Charsets.UTF_8)
        assertFalse(writeInPieces(bytes.copyOf(bytes.size - 1), 1).first)
    }
}
