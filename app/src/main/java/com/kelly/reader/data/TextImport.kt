/*
 * Uploaded file names: the .txt check and the book title. The text itself is checked and
 * cleaned while it arrives, by Utf8TextWriter.
 */
package com.kelly.reader.data

object TextImport {

    private const val TXT_EXTENSION = ".txt"

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
}
