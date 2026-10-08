/*
 * Small formatting helpers shared by the screens.
 */
package com.kelly.reader.domain

import java.util.Locale

private const val KB = 1024.0
private const val MB = KB * 1024

/**
 * Formats [bytes] the way the design shows sizes: "386 KB" below a megabyte, "1.2 MB" above.
 */
fun formatSize(bytes: Long): String = if (bytes < MB) {
    "${Math.round(bytes / KB)} KB"
} else {
    String.format(Locale.US, "%.1f MB", bytes / MB)
}
