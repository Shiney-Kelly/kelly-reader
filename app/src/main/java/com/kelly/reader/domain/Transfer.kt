/*
 * One file received by the upload server, as the Add books screen lists it.
 */
package com.kelly.reader.domain

/**
 * Why an upload was turned away. The screen maps each to a string resource.
 */
enum class TransferFailure {
    NOT_TXT,
    NOT_UTF8,
    INTERRUPTED,
}

sealed interface TransferStatus {

    /**
     * Bytes are still arriving; [percent] is 0..100.
     */
    data class Uploading(val percent: Int) : TransferStatus

    data object Added : TransferStatus

    data class Failed(val reason: TransferFailure) : TransferStatus
}

data class Transfer(
    val id: Int,
    val name: String,
    val sizeBytes: Long,
    val status: TransferStatus,
) {

    val isDone: Boolean
        get() = status is TransferStatus.Added

    val isFailed: Boolean
        get() = status is TransferStatus.Failed

    val isUploading: Boolean
        get() = status is TransferStatus.Uploading
}
