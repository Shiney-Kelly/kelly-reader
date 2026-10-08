package com.kelly.reader.ui.add

import com.kelly.reader.domain.Transfer
import com.kelly.reader.domain.TransferStatus

/**
 * What the Add books screen shows. [address] is the full URL to open on another device, or
 * null when the phone is not on Wi-Fi or the server is not running. [serverFailed] is true
 * when no port was free.
 */
data class AddBooksUiState(
    val address: String? = null,
    val onWifi: Boolean = false,
    val serverFailed: Boolean = false,
    val transfers: List<Transfer> = emptyList(),
    val copied: Boolean = false,
) {

    val doneCount: Int
        get() = transfers.count { it.isDone }

    val uploadingCount: Int
        get() = transfers.count { it.isUploading }

    val failedCount: Int
        get() = transfers.count { it.isFailed }

    /**
     * Overall progress over all files in percent: finished files count as whole, the file in
     * flight by its own percentage, as the design shows it.
     */
    val totalPercent: Int
        get() {
            if (transfers.isEmpty()) return 0
            val partial = transfers.sumOf { transfer ->
                when (val status = transfer.status) {
                    is TransferStatus.Uploading -> status.percent / 100.0
                    is TransferStatus.Added -> 1.0
                    is TransferStatus.Failed -> 0.0
                }
            }
            return (partial / transfers.size * 100).toInt()
        }
}
