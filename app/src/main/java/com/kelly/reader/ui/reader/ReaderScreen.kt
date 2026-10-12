package com.kelly.reader.ui.reader

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.distinctUntilChanged

import com.kelly.reader.R
import com.kelly.reader.domain.Book
import com.kelly.reader.domain.ReaderFont
import com.kelly.reader.domain.ReaderSettings
import com.kelly.reader.domain.ReadingMode
import com.kelly.reader.ui.components.AppIcons
import com.kelly.reader.ui.theme.FIGTREE
import com.kelly.reader.ui.theme.KellyReaderTheme
import com.kelly.reader.ui.theme.LITERATA
import com.kelly.reader.ui.theme.ReaderTheme

private val TOP_ROW_HEIGHT = 56.dp
private val FOOTER_HEIGHT = 44.dp
private val TEXT_SIDE_PADDING = 26.dp
private val TEXT_TOP_PADDING = 8.dp

/**
 * Everything that changes how much text fits on a page. A new key means paginating again.
 */
data class PageKey(
    val widthPx: Int,
    val heightPx: Int,
    val fontSize: Int,
    val lineSpacingKey: String,
    val fontKey: String,
) {

    /**
     * The key as one string, used for the in-memory and on-disk page caches.
     */
    val cacheKey: String
        get() = "${widthPx}x$heightPx-$fontSize-$lineSpacingKey-$fontKey"
}

/**
 * The text style for the page at the given [settings]: the chosen family, size and line height.
 * Lines are justified, which spreads the words of scripts that use spaces to both margins and
 * leaves scripts without spaces, such as Chinese, as they are.
 */
fun pageTextStyle(settings: ReaderSettings): TextStyle = TextStyle(
    fontFamily = if (settings.font == ReaderFont.SERIF) LITERATA else FIGTREE,
    fontSize = settings.fontSize.sp,
    lineHeight = (settings.fontSize * settings.lineSpacing.multiplier).sp,
    textDirection = TextDirection.Content,
    textAlign = TextAlign.Justify,
)

/**
 * The reader: a thin title row, the page text, a footer with the page number and the
 * percentage, and the settings drawer over it all when [drawerOpen]. [onPageMeasurer] gets a
 * measurer for the current page size and style.
 */
@Composable
fun ReaderScreen(
    uiState: ReaderUiState,
    theme: ReaderTheme,
    drawerOpen: Boolean,
    settingsActions: SettingsActions,
    onSettingsClick: () -> Unit,
    onCloseSettings: () -> Unit,
    onBackToShelf: () -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onScrolledToPage: (Int) -> Unit,
    onPageMeasurer: (String, PageMeasurer) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize().background(theme.background)) {
        ReaderPage(
            uiState = uiState,
            theme = theme,
            onSettingsClick = onSettingsClick,
            onPreviousPage = onPreviousPage,
            onNextPage = onNextPage,
            onScrolledToPage = onScrolledToPage,
            onPageMeasurer = onPageMeasurer,
        )
        SettingsDrawer(
            open = drawerOpen,
            uiState = uiState,
            actions = settingsActions,
            onClose = onCloseSettings,
            onBackToShelf = onBackToShelf,
        )
    }
}

/**
 * The page itself: title row, text and footer, inside the system bars.
 */
@Composable
private fun ReaderPage(
    uiState: ReaderUiState,
    theme: ReaderTheme,
    onSettingsClick: () -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onScrolledToPage: (Int) -> Unit,
    onPageMeasurer: (String, PageMeasurer) -> Unit,
) {
    val style = pageTextStyle(uiState.settings).copy(color = theme.ink)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        ReaderTopRow(title = uiState.book?.title ?: "", muted = theme.muted, onSettingsClick = onSettingsClick)
        // only the page follows the text direction, the title row and footer stay put
        val direction = if (uiState.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
        CompositionLocalProvider(LocalLayoutDirection provides direction) {
            PageArea(
                uiState = uiState,
                style = style,
                onPreviousPage = onPreviousPage,
                onNextPage = onNextPage,
                onScrolledToPage = onScrolledToPage,
                onPageMeasurer = onPageMeasurer,
                modifier = Modifier.weight(1f),
            )
        }
        ReaderFooter(uiState = uiState, muted = theme.muted)
    }
}

@Composable
private fun ReaderTopRow(
    title: String,
    muted: Color,
    onSettingsClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(TOP_ROW_HEIGHT)
            .padding(start = 24.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = muted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(textDirection = TextDirection.Content),
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onSettingsClick, modifier = Modifier.size(48.dp)) {
            Icon(
                imageVector = AppIcons.SLIDERS,
                contentDescription = stringResource(R.string.reader_settings),
                tint = muted,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

/**
 * The text area. Measures its own size, hands a matching measurer up, and draws the pages with
 * the same style and width so the measured lines fit: one page at a time in paged mode, or
 * all pages end to end in scroll mode.
 */
@Composable
private fun PageArea(
    uiState: ReaderUiState,
    style: TextStyle,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onScrolledToPage: (Int) -> Unit,
    onPageMeasurer: (String, PageMeasurer) -> Unit,
    modifier: Modifier = Modifier,
) {
    val fontFamilyResolver = LocalFontFamilyResolver.current
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    var size by remember { mutableStateOf(IntSize.Zero) }
    val key = PageKey(
        widthPx = size.width,
        heightPx = size.height,
        fontSize = style.fontSize.value.toInt(),
        lineSpacingKey = style.lineHeight.value.toString(),
        fontKey = if (style.fontFamily == LITERATA) ReaderFont.SERIF.key else ReaderFont.SANS.key,
    )
    LaunchedEffect(key) {
        if (size.width > 0 && size.height > 0) {
            // a measurer of its own per run, with no cache, so a run being cancelled and the
            // next one never share state across threads
            val textMeasurer = TextMeasurer(fontFamilyResolver, density, layoutDirection, cacheSize = 0)
            onPageMeasurer(key.cacheKey, ComposePageMeasurer(textMeasurer, style, size.width, size.height))
        }
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = TEXT_SIDE_PADDING, top = TEXT_TOP_PADDING, end = TEXT_SIDE_PADDING)
            .clipToBounds()
            .onSizeChanged { size = it },
    ) {
        if (uiState.settings.readingMode == ReadingMode.SCROLL) {
            ScrollingPages(
                uiState = uiState,
                style = style,
                onScrolledToPage = onScrolledToPage,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            SinglePage(
                text = uiState.pageText,
                style = style,
                onPreviousPage = onPreviousPage,
                onNextPage = onNextPage,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/**
 * Paged mode: the current page. A tap on the left half goes back a page and on the right half
 * forward, by screen position whatever the text direction. A long press starts a text
 * selection with the system copy toolbar; while one is shown, a tap clears it instead.
 */
@Composable
private fun SinglePage(
    text: String,
    style: TextStyle,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.pointerInput(Unit) {
            detectTapGestures { tap ->
                if (tap.x < this.size.width / 2) onPreviousPage() else onNextPage()
            }
        },
    ) {
        SelectionContainer {
            Text(text = text, style = style, overflow = TextOverflow.Clip)
        }
    }
}

/**
 * Scroll mode: the measured pages laid end to end in a lazy column. Only pages whose end is
 * known are listed. The page at the top is reported through [onScrolledToPage], and when the
 * pages change (new text size, or the pages before the resume point arriving) the same text
 * stays at the top.
 */
@Composable
private fun ScrollingPages(
    uiState: ReaderUiState,
    style: TextStyle,
    onScrolledToPage: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val starts = uiState.pageStarts ?: return
    val count = if (uiState.paginationDone) starts.size else starts.size - 1
    if (count <= 0) return
    val topIndex = uiState.pageIndex.coerceIn(0, count - 1)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = topIndex)

    LaunchedEffect(starts) {
        if (listState.firstVisibleItemIndex != topIndex) listState.scrollToItem(topIndex)
    }
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { index -> onScrolledToPage(index) }
    }
    // one selection container around the list, so a selection can run across page items
    SelectionContainer {
        LazyColumn(state = listState, modifier = modifier) {
            items(count = count, key = { starts[it] }) { index ->
                Text(
                    text = Paginator.pageText(uiState.text, starts, index),
                    style = style,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/**
 * "Page x of y" once everything is known, "Page x of …" while the count is still coming, and
 * "Page … of …" while the pages before the resume point are still coming.
 */
@Composable
fun pageLabel(uiState: ReaderUiState): String {
    val page = uiState.pageIndex + 1
    val count = uiState.pageCount
    return when {
        count != null -> stringResource(R.string.reader_page_of, page, count)
        uiState.pageNumberKnown -> stringResource(R.string.reader_page_of_unknown, page)
        else -> stringResource(R.string.reader_page_unknown)
    }
}

@Composable
private fun ReaderFooter(uiState: ReaderUiState, muted: Color) {
    val pageLabel = pageLabel(uiState)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(FOOTER_HEIGHT)
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = pageLabel, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = muted)
        Text(
            text = uiState.percentLabel,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = muted,
        )
    }
}

// ----- previews -----

private const val PREVIEW_TEXT = "Chapter 3 · Low Tide\n\nThe harbor woke slowly that morning. Fog sat on " +
    "the water like a folded blanket, and the fishing boats knocked against the pier in a sleepy " +
    "rhythm.\n\nMara pulled her coat tighter and walked to the end of the dock."

private val PREVIEW_STATE = ReaderUiState(
    book = Book("b1", "The Quiet Harbor", "b1.txt", 386 * 1024L, PREVIEW_TEXT.length, 0),
    text = PREVIEW_TEXT,
    offset = 0,
    pageStarts = intArrayOf(0, PREVIEW_TEXT.length / 2),
    paginationDone = true,
)

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ReaderScreenPreview() {
    KellyReaderTheme {
        ReaderScreen(PREVIEW_STATE, ReaderTheme.LIGHT, false, SettingsActions.NONE, {}, {}, {}, {}, {}, {}, { _, _ -> })
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ReaderScreenDarkPreview() {
    KellyReaderTheme {
        ReaderScreen(PREVIEW_STATE, ReaderTheme.DARK, false, SettingsActions.NONE, {}, {}, {}, {}, {}, {}, { _, _ -> })
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ReaderScreenScrollPreview() {
    KellyReaderTheme {
        ReaderScreen(
            PREVIEW_STATE.copy(settings = ReaderSettings(readingMode = ReadingMode.SCROLL)),
            ReaderTheme.LIGHT, false, SettingsActions.NONE, {}, {}, {}, {}, {}, {}, { _, _ -> },
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ReaderScreenPaperPreview() {
    KellyReaderTheme {
        ReaderScreen(PREVIEW_STATE, ReaderTheme.PAPER, true, SettingsActions.NONE, {}, {}, {}, {}, {}, {}, { _, _ -> })
    }
}
