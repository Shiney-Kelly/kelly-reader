package com.kelly.reader.data

import java.io.OutputStream
import java.io.OutputStreamWriter
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.CodingErrorAction

private const val CHAR_BUFFER_SIZE = 16 * 1024
private const val BOM = '﻿'

/**
 * Cleans text piece by piece on its way to [out], the same way [TextImport.decodeUtf8] cleans
 * a whole file: it must be strict UTF-8, a BOM at the very start is dropped, and Windows and
 * old Mac line endings become "\n". Only small buffers are held, whatever the size of the
 * text. Hand it the bytes with [write] as they come, then call [finish]; both answer false once
 * the bytes are not valid UTF-8. [charCount] is the length of the cleaned text as a Kotlin
 * String counts it. Closing [out] is left to the caller.
 */
class Utf8TextWriter(out: OutputStream) {

    private val decoder = Charsets.UTF_8.newDecoder()
        .onMalformedInput(CodingErrorAction.REPORT)
        .onUnmappableCharacter(CodingErrorAction.REPORT)
    private val writer = OutputStreamWriter(out, Charsets.UTF_8)

    // bytes not decoded yet, kept ready to read: a character cut off at the end of a piece
    private var input: ByteBuffer = ByteBuffer.allocate(0)
    private val decoded = CharBuffer.allocate(CHAR_BUFFER_SIZE)
    private val cleaned = CharArray(CHAR_BUFFER_SIZE)
    private var atStart = true
    private var afterCarriageReturn = false

    /**
     * Chars of cleaned text written so far.
     */
    var charCount: Int = 0
        private set

    /**
     * False once a byte sequence was not valid UTF-8; nothing more is written after that.
     */
    var valid: Boolean = true
        private set

    /**
     * Cleans and writes the first [length] bytes of [bytes]. A character cut off at the end
     * waits for the next call. Returns false when the bytes are not valid UTF-8.
     */
    fun write(bytes: ByteArray, length: Int = bytes.size): Boolean {
        if (!valid) return false
        if (input.capacity() - input.remaining() < length) {
            val bigger = ByteBuffer.allocate(input.remaining() + length)
            bigger.put(input)
            input = bigger
        } else {
            input.compact()
        }
        input.put(bytes, 0, length)
        input.flip()
        return decode(endOfInput = false)
    }

    /**
     * Ends the text: bytes still waiting are an unfinished character and make it invalid.
     * Flushes everything written to the output stream. Returns false when it is not valid.
     */
    fun finish(): Boolean {
        if (!valid || !decode(endOfInput = true)) return false
        while (true) {
            val result = decoder.flush(decoded)
            drain()
            if (result.isUnderflow) break
        }
        writer.flush()
        return true
    }

    private fun decode(endOfInput: Boolean): Boolean {
        while (true) {
            val result = decoder.decode(input, decoded, endOfInput)
            drain()
            if (result.isError) {
                valid = false
                return false
            }
            if (result.isUnderflow) return true
        }
    }

    // moves the decoded chars to the writer, dropping the BOM and turning line endings into \n
    private fun drain() {
        decoded.flip()
        var count = 0
        while (decoded.hasRemaining()) {
            val char = decoded.get()
            when {
                atStart && char == BOM -> Unit
                char == '\r' -> {
                    cleaned[count++] = '\n'
                    afterCarriageReturn = true
                }
                char == '\n' && afterCarriageReturn -> afterCarriageReturn = false
                else -> {
                    cleaned[count++] = char
                    afterCarriageReturn = false
                }
            }
            atStart = false
        }
        decoded.clear()
        writer.write(cleaned, 0, count)
        charCount += count
    }
}
