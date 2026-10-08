package com.kelly.reader.ui.shelf

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.kelly.reader.R
import com.kelly.reader.domain.Book
import com.kelly.reader.domain.formatPercent
import com.kelly.reader.domain.formatSize
import com.kelly.reader.ui.components.AccentTextButton
import com.kelly.reader.ui.components.AppIcons
import com.kelly.reader.ui.components.BookCover
import com.kelly.reader.ui.components.PillButton
import com.kelly.reader.ui.components.ProgressBar
import com.kelly.reader.ui.theme.AppTheme
import com.kelly.reader.ui.theme.KellyReaderTheme

/**
 * The bookshelf: a title row with Edit and Add, a three column grid of covers, and the delete
 * bar at the bottom while books are selected.
 */
@Composable
fun ShelfScreen(
    uiState: ShelfUiState,
    onBookClick: (String) -> Unit,
    onEditClick: () -> Unit,
    onDoneClick: () -> Unit,
    onAddClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            ShelfTopBar(
                uiState = uiState,
                onEditClick = onEditClick,
                onDoneClick = onDoneClick,
                onAddClick = onAddClick,
            )
            val books = uiState.books
            when {
                books == null -> Unit
                books.isEmpty() -> EmptyShelf(onAddClick = onAddClick, modifier = Modifier.weight(1f))
                else -> BookGrid(
                    books = books,
                    editing = uiState.editing,
                    selected = uiState.selected,
                    onBookClick = onBookClick,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        if (uiState.editing && uiState.selectedCount > 0) {
            DeleteBar(
                selectedCount = uiState.selectedCount,
                onDeleteClick = onDeleteClick,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

/**
 * The bottom bar in edit mode: how many books are selected and the red Delete button.
 */
@Composable
private fun DeleteBar(
    selectedCount: Int,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val lineColor = MaterialTheme.colorScheme.outlineVariant
    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 6.dp)
            .background(MaterialTheme.colorScheme.surface)
            .drawBehind {
                drawLine(
                    color = lineColor,
                    start = Offset.Zero,
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx(),
                )
            }
            .navigationBarsPadding()
            .padding(start = 16.dp, top = 14.dp, end = 16.dp, bottom = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = pluralStringResource(R.plurals.shelf_books_selected, selectedCount, selectedCount),
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        PillButton(
            label = stringResource(R.string.shelf_delete),
            onClick = onDeleteClick,
            icon = AppIcons.TRASH,
            height = 48.dp,
            containerColor = MaterialTheme.colorScheme.tertiary,
        )
    }
}

/**
 * What the shelf shows with no books: an icon, a title, a help line and an Add books button.
 */
@Composable
private fun EmptyShelf(onAddClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 40.dp, end = 40.dp, bottom = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(AppTheme.colors.tint),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = AppIcons.BOOK,
                contentDescription = null,
                tint = AppTheme.colors.accentText,
                modifier = Modifier.size(40.dp),
            )
        }
        Text(
            text = stringResource(R.string.shelf_empty_title),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            text = stringResource(R.string.shelf_empty_help),
            fontSize = 15.sp,
            lineHeight = 22.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        PillButton(
            label = stringResource(R.string.shelf_empty_add),
            onClick = onAddClick,
            icon = AppIcons.PLUS,
            height = 48.dp,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}

@Composable
private fun ShelfTopBar(
    uiState: ShelfUiState,
    onEditClick: () -> Unit,
    onDoneClick: () -> Unit,
    onAddClick: () -> Unit,
) {
    val title = when {
        !uiState.editing -> stringResource(R.string.shelf_title)
        uiState.selectedCount == 0 -> stringResource(R.string.shelf_select_books)
        else -> stringResource(R.string.shelf_selected_count, uiState.selectedCount)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .padding(start = 20.dp, top = 20.dp, end = 12.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.28).sp,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
        )
        if (uiState.editing) {
            AccentTextButton(label = stringResource(R.string.shelf_done), onClick = onDoneClick)
        } else {
            AccentTextButton(label = stringResource(R.string.shelf_edit), onClick = onEditClick)
            Spacer(modifier = Modifier.width(4.dp))
            PillButton(
                label = stringResource(R.string.shelf_add),
                onClick = onAddClick,
                icon = AppIcons.PLUS,
            )
        }
    }
}

@Composable
private fun BookGrid(
    books: List<Book>,
    editing: Boolean,
    selected: Set<String>,
    onBookClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 128.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        items(books, key = { it.id }) { book ->
            BookItem(
                book = book,
                selected = if (editing) book.id in selected else null,
                onClick = { onBookClick(book.id) },
            )
        }
    }
}

/**
 * One grid cell: the cover, the title on two lines, and the progress row.
 */
@Composable
private fun BookItem(
    book: Book,
    selected: Boolean?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = if (selected == null) {
        stringResource(R.string.shelf_open_book, book.title)
    } else {
        stringResource(R.string.shelf_select_book, book.title)
    }
    Column(
        modifier = modifier
            .semantics { contentDescription = label }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BookCover(
            glyph = book.glyph,
            meta = stringResource(R.string.shelf_cover_meta, formatSize(book.sizeBytes)),
            color = AppTheme.colors.coverColors[book.coverColorIndex],
            selected = selected,
        )
        Text(
            text = book.title,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 17.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.bodySmall.copy(textDirection = TextDirection.Content),
        )
        ProgressRow(book = book, modifier = Modifier.padding(top = (-2).dp))
    }
}

@Composable
private fun ProgressRow(book: Book, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProgressBar(progress = book.progress, modifier = Modifier.weight(1f))
        Text(
            text = if (book.isNew) {
                stringResource(R.string.shelf_new)
            } else {
                formatPercent(book.progress)
            },
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ----- previews -----

internal val PREVIEW_BOOKS = listOf(
    Book("b1", "The Quiet Harbor", "harbor.txt", 386 * 1024L, 1000, 1, 300, 0.33f, 1L),
    Book("b2", "山间小记", "shanjian.txt", 1258291L, 1000, 2, 0, 0.33f, 1L),
    Book("b3", "雨の日の手紙", "ame.txt", 642 * 1024L, 1000, 3, 999, 1f, 1L),
    Book("b4", "Зимняя дорога", "zima.txt", 904 * 1024L, 1000, 4),
    Book("b5", "바다의 노래", "bada.txt", 412 * 1024L, 1000, 5),
    Book("b6", "حكايات المساء", "hikayat.txt", 233 * 1024L, 1000, 6, 0, 0.5f, 1L),
    Book("b7", "Notes de voyage", "notes.txt", 158 * 1024L, 1000, 7, 0, 1f, 1L),
    Book("b8", "Cuentos del sur", "cuentos.txt", 268 * 1024L, 1000, 8),
)

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ShelfScreenPreview() {
    KellyReaderTheme {
        Box(modifier = Modifier.height(844.dp)) {
            ShelfScreen(ShelfUiState(books = PREVIEW_BOOKS), {}, {}, {}, {}, {})
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ShelfScreenDarkPreview() {
    KellyReaderTheme {
        ShelfScreen(ShelfUiState(books = PREVIEW_BOOKS), {}, {}, {}, {}, {})
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ShelfScreenEmptyPreview() {
    KellyReaderTheme {
        ShelfScreen(ShelfUiState(books = emptyList()), {}, {}, {}, {}, {})
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ShelfScreenEditPreview() {
    KellyReaderTheme {
        ShelfScreen(
            ShelfUiState(books = PREVIEW_BOOKS, editing = true, selected = setOf("b3", "b6")),
            {}, {}, {}, {}, {},
        )
    }
}
