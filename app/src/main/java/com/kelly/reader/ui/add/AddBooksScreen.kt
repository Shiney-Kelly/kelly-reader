package com.kelly.reader.ui.add

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.kelly.reader.R
import com.kelly.reader.domain.MAX_UPLOAD_MB
import com.kelly.reader.domain.Transfer
import com.kelly.reader.domain.TransferFailure
import com.kelly.reader.domain.TransferStatus
import com.kelly.reader.domain.formatSize
import com.kelly.reader.ui.components.AppIcons
import com.kelly.reader.ui.components.ProgressBar
import com.kelly.reader.ui.theme.AppTheme
import com.kelly.reader.ui.theme.KellyReaderTheme
import com.kelly.reader.ui.theme.LIVE_DOT

private val CARD_SHAPE = RoundedCornerShape(18.dp)

/**
 * The Add books screen: the Wi-Fi card with the address to open, the list of received files,
 * and a footer reminding to keep the screen open.
 */
@Composable
fun AddBooksScreen(
    uiState: AddBooksUiState,
    onBack: () -> Unit,
    onCopy: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            AddBooksTopBar(onBack = onBack)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                WifiCard(uiState = uiState, onCopy = onCopy)
                ReceivedSection(uiState = uiState)
            }
        }
        Footer(modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun AddBooksTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, top = 16.dp, end = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
            Icon(
                imageVector = AppIcons.BACK,
                contentDescription = stringResource(R.string.add_back),
                tint = MaterialTheme.colorScheme.onBackground,
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = stringResource(R.string.add_title),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

/**
 * The card with the Wi-Fi state, the address box with its Copy button and the format note.
 */
@Composable
private fun WifiCard(uiState: AddBooksUiState, onCopy: (String) -> Unit) {
    Card {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            WifiStatusRow(onWifi = uiState.onWifi, failed = uiState.serverFailed)
            Text(
                text = stringResource(R.string.add_help),
                fontSize = 14.sp,
                lineHeight = 21.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            uiState.address?.let { address ->
                AddressBox(address = address, copied = uiState.copied, onCopy = { onCopy(address) })
            }
            Text(
                text = stringResource(R.string.add_note),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun WifiStatusRow(onWifi: Boolean, failed: Boolean) {
    val title = when {
        failed -> R.string.add_server_failed
        onWifi -> R.string.add_wifi_on
        else -> R.string.add_wifi_off
    }
    val detail = when {
        failed -> R.string.add_server_failed_help
        onWifi -> R.string.add_wifi_connected
        else -> R.string.add_wifi_join
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(AppTheme.colors.tint),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = AppIcons.WIFI,
                contentDescription = null,
                tint = AppTheme.colors.accentText,
                modifier = Modifier.size(22.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(title),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(detail),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        if (onWifi && !failed) LiveTag()
    }
}

@Composable
private fun LiveTag() {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(AppTheme.colors.okBg)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(LIVE_DOT))
        Text(
            text = stringResource(R.string.add_live),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = AppTheme.colors.okInk,
        )
    }
}

@Composable
private fun AddressBox(address: String, copied: Boolean, onCopy: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AppTheme.colors.tint)
            .padding(start = 16.dp, top = 6.dp, end = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = address,
            fontFamily = FontFamily.Monospace,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = AppTheme.colors.tintInk,
            modifier = Modifier.weight(1f),
        )
        TextButton(
            onClick = onCopy,
            modifier = Modifier.height(44.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.textButtonColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = AppTheme.colors.accentText,
            ),
            contentPadding = PaddingValues(horizontal = 12.dp),
        ) {
            Icon(
                imageVector = AppIcons.COPY,
                contentDescription = stringResource(R.string.add_copy_address),
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(if (copied) R.string.add_copied else R.string.add_copy),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

/**
 * The received files: header with a count on the right, a summary card for many files, then
 * the list card with one row per file.
 */
@Composable
private fun ReceivedSection(uiState: AddBooksUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = stringResource(R.string.add_received),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = receivedLabel(uiState),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (uiState.transfers.size > FEW_FILES) SummaryCard(uiState = uiState)
        Card {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                if (uiState.transfers.isEmpty()) {
                    Text(
                        text = stringResource(R.string.add_waiting),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    )
                }
                uiState.transfers.forEachIndexed { index, transfer ->
                    if (index > 0) {
                        HorizontalDivider(color = AppTheme.colors.divider, thickness = 1.dp)
                    }
                    TransferRow(transfer = transfer)
                }
            }
        }
    }
}

/**
 * For many files: how many are in, an overall bar, and tags per outcome.
 */
@Composable
private fun SummaryCard(uiState: AddBooksUiState) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = stringResource(R.string.add_summary_count, uiState.doneCount),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.add_summary_of, uiState.transfers.size),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 2.dp),
                    )
                }
                Text(
                    text = stringResource(R.string.add_percent, uiState.totalPercent),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppTheme.colors.accentText,
                )
            }
            ProgressBar(progress = uiState.totalPercent / 100f, height = 6.dp)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Tag(
                    text = stringResource(R.string.add_tag_added, uiState.doneCount),
                    background = AppTheme.colors.okBg,
                    ink = AppTheme.colors.okInk,
                )
                if (uiState.uploadingCount > 0) {
                    Tag(
                        text = stringResource(R.string.add_tag_uploading, uiState.uploadingCount),
                        background = AppTheme.colors.tint,
                        ink = AppTheme.colors.tintInk,
                    )
                }
                if (uiState.failedCount > 0) {
                    Tag(
                        text = stringResource(R.string.add_tag_failed, uiState.failedCount),
                        background = AppTheme.colors.errorBg,
                        ink = AppTheme.colors.errorInk,
                    )
                }
            }
        }
    }
}

@Composable
private fun Tag(text: String, background: Color, ink: Color) {
    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = ink,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

/**
 * One received file: icon, name, "size · status" line, a bar while uploading, and a check or
 * an alert mark on the right.
 */
@Composable
private fun TransferRow(transfer: Transfer) {
    val status = transfer.status
    val statusText = when (status) {
        is TransferStatus.Uploading -> stringResource(R.string.add_status_uploading, status.percent)
        is TransferStatus.Added -> stringResource(R.string.add_status_added)
        is TransferStatus.Failed -> when (status.reason) {
            TransferFailure.NOT_TXT -> stringResource(R.string.add_status_not_txt)
            TransferFailure.NOT_UTF8 -> stringResource(R.string.add_status_not_utf8)
            TransferFailure.INTERRUPTED -> stringResource(R.string.add_status_interrupted)
            TransferFailure.TOO_BIG -> stringResource(R.string.add_status_too_big, MAX_UPLOAD_MB)
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(width = 36.dp, height = 44.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(AppTheme.colors.tint2),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = AppIcons.FILE,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = transfer.name,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium.copy(textDirection = TextDirection.Content),
            )
            Text(
                text = stringResource(R.string.add_row_sub, formatSize(transfer.sizeBytes), statusText),
                fontSize = 12.sp,
                color = if (transfer.isFailed) {
                    AppTheme.colors.errorInk
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            if (status is TransferStatus.Uploading) {
                ProgressBar(progress = status.percent / 100f, height = 4.dp)
            }
        }
        when (status) {
            is TransferStatus.Added -> StatusMark(
                icon = AppIcons.CHECK,
                description = stringResource(R.string.add_added),
                background = AppTheme.colors.okBg,
                ink = AppTheme.colors.okInk,
            )
            is TransferStatus.Failed -> StatusMark(
                icon = AppIcons.ALERT,
                description = stringResource(R.string.add_failed),
                background = AppTheme.colors.errorBg,
                ink = AppTheme.colors.errorInk,
            )
            is TransferStatus.Uploading -> Unit
        }
    }
}

@Composable
private fun StatusMark(icon: ImageVector, description: String, background: Color, ink: Color) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = ink,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun receivedLabel(uiState: AddBooksUiState): String {
    val total = uiState.transfers.size
    return if (total > FEW_FILES) {
        stringResource(R.string.add_received_files, total)
    } else {
        stringResource(R.string.add_received_of, uiState.doneCount, total)
    }
}

// up to this many files the list stands alone, above it a summary card goes on top
internal const val FEW_FILES = 3

/**
 * A surface card with the design's 18dp corners and faint shadow.
 */
@Composable
private fun Card(content: @Composable () -> Unit) {
    Surface(
        shape = CARD_SHAPE,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        content()
    }
}

@Composable
private fun Footer(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.add_footer),
        fontSize = 13.sp,
        lineHeight = 19.sp,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
            .padding(start = 32.dp, top = 14.dp, end = 32.dp, bottom = 14.dp),
    )
}

// ----- previews -----

private val PREVIEW_FEW = listOf(
    Transfer(0, "바다의 노래.txt", 412 * 1024L, TransferStatus.Added),
    Transfer(1, "Cuentos del sur.txt", 268 * 1024L, TransferStatus.Added),
    Transfer(2, "Die stille Stadt.txt", 1153433L, TransferStatus.Uploading(38)),
)

private val PREVIEW_MANY = PREVIEW_FEW + listOf(
    Transfer(3, "cover-scan.pdf", 3565158L, TransferStatus.Failed(TransferFailure.NOT_TXT)),
    Transfer(4, "छोटी कहानियाँ.txt", 289 * 1024L, TransferStatus.Added),
    Transfer(5, "حكايات المساء.txt", 233 * 1024L, TransferStatus.Added),
)

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun AddBooksScreenPreview() {
    KellyReaderTheme {
        AddBooksScreen(
            AddBooksUiState(address = "http://192.168.1.24:12121", onWifi = true, transfers = PREVIEW_FEW),
            {}, {},
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun AddBooksScreenManyPreview() {
    KellyReaderTheme {
        AddBooksScreen(
            AddBooksUiState(address = "http://192.168.1.24:12121", onWifi = true, transfers = PREVIEW_MANY),
            {}, {},
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AddBooksScreenDarkPreview() {
    KellyReaderTheme {
        AddBooksScreen(
            AddBooksUiState(address = "http://192.168.1.24:12121", onWifi = true, copied = true, transfers = PREVIEW_MANY),
            {}, {},
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun AddBooksScreenServerFailedPreview() {
    KellyReaderTheme {
        AddBooksScreen(AddBooksUiState(onWifi = true, serverFailed = true), {}, {})
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun AddBooksScreenOffWifiPreview() {
    KellyReaderTheme {
        AddBooksScreen(AddBooksUiState(address = null), {}, {})
    }
}
