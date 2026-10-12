/*
 * Small formatting helpers shared by the screens.
 */
package com.kelly.reader.domain

import java.util.Locale

private const val KB = 1024.0
private const val MB = KB * 1024

/**
 * Formats reading [progress] (0..1) with two decimals: "98.88%".
 */
fun formatPercent(progress: Float): String =
    String.format(Locale.US, "%.2f%%", progress.coerceIn(0f, 1f) * 100)

/**
 * Formats [bytes] the way the design shows sizes: "386 KB" below a megabyte, "1.2 MB" above.
 * Anything under a kilobyte, except nothing at all, shows as "1 KB" rather than "0 KB".
 */
fun formatSize(bytes: Long): String = if (bytes < MB) {
    "${Math.round(bytes / KB).coerceAtLeast(if (bytes > 0) 1 else 0)} KB"
} else {
    String.format(Locale.US, "%.1f MB", bytes / MB)
}
