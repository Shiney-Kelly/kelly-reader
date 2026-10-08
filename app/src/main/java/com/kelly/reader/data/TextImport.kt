/*
 * Turning an uploaded file into book text: UTF-8 check, BOM strip, newline cleanup and title.
 */
package com.kelly.reader.data

import java.io.File
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction

object TextImport {

    private const val TXT_EXTENSION = ".txt"
    private const val BOM = '﻿'

    /**
     * True when [fileName] ends in ".txt", in any case.
     */
    fun isTextFile(fileName: String): Boolean = fileName.lowercase().endsWith(TXT_EXTENSION)

    /**
     * The title for [fileName]: the name without its extension, trimmed. A name that is only
     * an extension keeps the whole name.
     */
    fun titleFrom(fileName: String): String {
        val base = fileName.substringBeforeLast('.').trim()
        return base.ifEmpty { fileName.trim() }
    }

    /**
     * [title] unchanged when no book has it yet, else "title (2)", "title (3)" and so on.
     */
    fun uniqueTitle(title: String, existing: Collection<String>): String {
        if (title !in existing) return title
        var n = 2
        while ("$title ($n)" in existing) n++
        return "$title ($n)"
    }

    /**
     * Decodes [bytes] as strict UTF-8, drops a leading BOM and turns Windows and old Mac line
     * endings into "\n". Returns null when the bytes are not valid UTF-8.
     */
    fun decodeUtf8(bytes: ByteArray): String? {
        val decoder = Charsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
        val text = try {
            decoder.decode(ByteBuffer.wrap(bytes)).toString()
        } catch (e: CharacterCodingException) {
            return null
        }
        return text.removePrefix(BOM.toString()).replace("\r\n", "\n").replace('\r', '\n')
    }

    /**
     * Reads [file], decodes it with [decodeUtf8] and writes the cleaned text back when it
     * changed, so later reads are plain. Returns the text, or null for a file that is not UTF-8.
     */
    fun prepareFile(file: File): String? {
        val bytes = file.readBytes()
        val text = decodeUtf8(bytes) ?: return null
        val cleaned = text.toByteArray(Charsets.UTF_8)
        if (!cleaned.contentEquals(bytes)) file.writeBytes(cleaned)
        return text
    }
}
