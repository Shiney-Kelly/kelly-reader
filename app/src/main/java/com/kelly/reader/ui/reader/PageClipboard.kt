package com.kelly.reader.ui.reader

import android.content.ClipData
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.NativeClipboard
import androidx.compose.ui.platform.toClipEntry

/**
 * The [clipboard] for text copied from the pages: a copied text goes on without the hidden
 * filler that pages ending inside a paragraph are laid out with, as [Paginator.copiedText]
 * cleans it. Anything else passes through as it is.
 */
class PageClipboard(private val clipboard: Clipboard) : Clipboard {

    override suspend fun getClipEntry(): ClipEntry? = clipboard.getClipEntry()

    override suspend fun setClipEntry(clipEntry: ClipEntry?) {
        val clipData = clipEntry?.clipData
        val copied = clipData?.takeIf { it.itemCount == 1 }?.getItemAt(0)?.text?.toString()
        if (clipData == null || copied == null) {
            clipboard.setClipEntry(clipEntry)
            return
        }
        val cleaned = Paginator.copiedText(copied)
        if (cleaned == copied) {
            clipboard.setClipEntry(clipEntry)
        } else {
            val plain = ClipData.newPlainText(clipData.description.label, cleaned)
            clipboard.setClipEntry(plain.toClipEntry())
        }
    }

    override val nativeClipboard: NativeClipboard
        get() = clipboard.nativeClipboard
}
