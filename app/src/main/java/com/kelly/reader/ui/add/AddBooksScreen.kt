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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.kelly.reader.R
import com.kelly.reader.ui.components.AppIcons
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
            WifiStatusRow(onWifi = uiState.onWifi)
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
private fun WifiStatusRow(onWifi: Boolean) {
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
                text = stringResource(if (onWifi) R.string.add_wifi_on else R.string.add_wifi_off),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(
                    if (onWifi) R.string.add_wifi_connected else R.string.add_wifi_join,
                ),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        if (onWifi) LiveTag()
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
 * The received files: header with a count on the right, then the list card. Filled in by
 * the next step.
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

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun AddBooksScreenPreview() {
    KellyReaderTheme {
        AddBooksScreen(AddBooksUiState(address = "http://192.168.1.24:8080"), {}, {})
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AddBooksScreenDarkPreview() {
    KellyReaderTheme {
        AddBooksScreen(AddBooksUiState(address = "http://192.168.1.24:8080", copied = true), {}, {})
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun AddBooksScreenOffWifiPreview() {
    KellyReaderTheme {
        AddBooksScreen(AddBooksUiState(address = null), {}, {})
    }
}
