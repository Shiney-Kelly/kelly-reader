package com.kelly.reader.ui.shelf

import android.content.res.Configuration
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.sin
import kotlinx.coroutines.delay

import com.kelly.reader.R
import com.kelly.reader.domain.Book
import com.kelly.reader.domain.BookSort
import com.kelly.reader.domain.formatPercent
import com.kelly.reader.domain.formatSize
import com.kelly.reader.ui.components.AccentTextButton
import com.kelly.reader.ui.components.AppIcons
import com.kelly.reader.ui.components.BookCover
import com.kelly.reader.ui.components.PillButton
import com.kelly.reader.ui.components.ProgressBar
import com.kelly.reader.ui.theme.AppTheme
import com.kelly.reader.ui.theme.KellyReaderTheme

// ----- shake shuffle -----

// the covers spin once and grow a little on the way to their mixed up places
private const val TUMBLE_MS = 700
private const val FULL_TURN = 360f
private const val TUMBLE_LIFT = 0.08f

// long enough for the covers to snap back before moves stop gliding
private const val SETTLE_MS = 1_000L

private val TUMBLE_SPRING = spring(
    dampingRatio = 0.7f,
    stiffness = Spring.StiffnessLow,
    visibilityThreshold = IntOffset.VisibilityThreshold,
)
private val SNAP_BACK_SPRING = spring(
    stiffness = Spring.StiffnessMedium,
    visibilityThreshold = IntOffset.VisibilityThreshold,
)

// never null: a null spec removes the item animations, and new ones made in the middle of a
// move draw nothing until the move ends
private val NO_GLIDE = snap<IntOffset>()

// ----- bottom of the grid -----

// the delete bar above the navigation bar: 14dp, the 48dp button, 14dp
private val DELETE_BAR_HEIGHT = 76.dp

// the last row of covers ends this far above the ledge, or above the delete bar when editing
private val GRID_END_GAP = 16.dp

/**
 * The bookshelf: a title row with Sort, Edit and Add, a three column grid of covers, the ledge
 * with the shelf bear along the bottom, and the delete bar in its place while books are
 * selected. [onPetFound] is called when someone knocks the hidden bear out onto the ledge, and
 * [onPetLeft] when a long press has sent it off again.
 */
@Composable
fun ShelfScreen(
    uiState: ShelfUiState,
    onBookClick: (String) -> Unit,
    onEditClick: () -> Unit,
    onDoneClick: () -> Unit,
    onAddClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onPetFound: () -> Unit,
    onPetLeft: () -> Unit,
    onSortChange: (BookSort) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            ShelfTopBar(
                uiState = uiState,
                onEditClick = onEditClick,
                onDoneClick = onDoneClick,
                onAddClick = onAddClick,
                onSortChange = onSortChange,
            )
            val books = uiState.shownBooks
            // the grid scrolls on behind the ledge or the delete bar, and its last row stops
            // just above them
            val navigationBar = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            val bottomBar = if (uiState.editing) DELETE_BAR_HEIGHT else LEDGE_TOTAL_HEIGHT
            when {
                books == null -> Unit
                books.isEmpty() -> EmptyShelf(onAddClick = onAddClick, modifier = Modifier.weight(1f))
                else -> BookGrid(
                    books = books,
                    sort = uiState.sort,
                    editing = uiState.editing,
                    selected = uiState.selected,
                    shuffling = uiState.shuffling,
                    onBookClick = onBookClick,
                    bottomPadding = bottomBar + navigationBar + GRID_END_GAP,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        ShelfPet(
            visible = !uiState.editing,
            found = uiState.petFound,
            onFound = onPetFound,
            onLeft = onPetLeft,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
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
    onSortChange: (BookSort) -> Unit,
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
            if (uiState.books?.isNotEmpty() == true) {
                SortButton(sort = uiState.sort, onSortChange = onSortChange)
            }
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

/**
 * The sort icon, which opens a menu of the shelf's orders with a check by the current [sort].
 */
@Composable
private fun SortButton(sort: BookSort, onSortChange: (BookSort) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }, modifier = Modifier.size(44.dp)) {
            Icon(
                imageVector = AppIcons.SORT,
                contentDescription = stringResource(R.string.shelf_sort),
                tint = AppTheme.colors.accentText,
                modifier = Modifier.size(22.dp),
            )
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            BookSort.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(text = stringResource(option.labelRes)) },
                    onClick = {
                        open = false
                        onSortChange(option)
                    },
                    trailingIcon = {
                        if (option == sort) {
                            Icon(
                                imageVector = AppIcons.CHECK,
                                contentDescription = null,
                                tint = AppTheme.colors.accentText,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    },
                )
            }
        }
    }
}

private val BookSort.labelRes: Int
    get() = when (this) {
        BookSort.LAST_READ -> R.string.shelf_sort_last_read
        BookSort.ADDED_NEWEST -> R.string.shelf_sort_added_newest
        BookSort.ADDED_OLDEST -> R.string.shelf_sort_added_oldest
        BookSort.TITLE_A_TO_Z -> R.string.shelf_sort_title_a_to_z
    }

/**
 * The covers in a three column grid, ending [bottomPadding] above the bottom of the screen.
 * A new [sort] goes back to the top. While [shuffling] the covers tumble to their mixed up
 * places, and when it ends they snap back.
 */
@Composable
private fun BookGrid(
    books: List<Book>,
    sort: BookSort,
    editing: Boolean,
    selected: Set<String>,
    shuffling: Boolean,
    onBookClick: (String) -> Unit,
    bottomPadding: Dp,
    modifier: Modifier = Modifier,
) {
    val gridState = rememberLazyGridState()
    // the grid would keep the cover that was at the top in view, which in a new order is
    // somewhere in the middle; a picked order starts from its first cover instead
    var shownSort by remember { mutableStateOf(sort) }
    LaunchedEffect(sort) {
        if (sort != shownSort) {
            shownSort = sort
            gridState.scrollToItem(0)
        }
    }
    // moves glide only around a shuffle, so deleting books stays instant
    var settling by remember { mutableStateOf(false) }
    LaunchedEffect(shuffling) {
        if (shuffling) {
            settling = true
        } else {
            delay(SETTLE_MS)
            settling = false
        }
    }
    // on a new order the grid would follow its first visible cover; this keeps the rows in place
    DisposableEffect(shuffling) {
        gridState.requestScrollToItem(
            index = gridState.firstVisibleItemIndex,
            scrollOffset = gridState.firstVisibleItemScrollOffset,
        )
        onDispose {}
    }
    val placementSpec = when {
        shuffling -> TUMBLE_SPRING
        settling -> SNAP_BACK_SPRING
        else -> NO_GLIDE
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        state = gridState,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = bottomPadding),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        // with the progress line kept to its text, this leaves rows clearly farther apart than
        // the columns, so each row's progress line reads as its own
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        items(books, key = { it.id }) { book ->
            BookItem(
                book = book,
                selected = if (editing) book.id in selected else null,
                tumbling = shuffling,
                onClick = { onBookClick(book.id) },
                modifier = Modifier.animateItem(
                    fadeInSpec = null,
                    placementSpec = placementSpec,
                    fadeOutSpec = null,
                ),
            )
        }
    }
}

/**
 * One grid cell: the cover, the title on two lines, and the progress row. When [tumbling] turns
 * on, the cell spins once, clockwise or not depending on the book.
 */
@Composable
private fun BookItem(
    book: Book,
    selected: Boolean?,
    tumbling: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tumble = remember { Animatable(0f) }
    LaunchedEffect(tumbling) {
        if (tumbling) {
            tumble.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = TUMBLE_MS, easing = FastOutSlowInEasing),
            )
        }
        tumble.snapTo(0f)
    }
    val turn = if (book.id.hashCode() % 2 == 0) FULL_TURN else -FULL_TURN
    val label = if (selected == null) {
        stringResource(R.string.shelf_open_book, book.title)
    } else {
        stringResource(R.string.shelf_select_book, book.title)
    }
    Column(
        modifier = modifier
            .graphicsLayer {
                val progress = tumble.value
                val scale = 1f + TUMBLE_LIFT * sin(PI.toFloat() * progress)
                rotationZ = turn * progress
                scaleX = scale
                scaleY = scale
            }
            .semantics { contentDescription = label }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BookCover(
            title = book.title,
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
        // the design pulls the progress row 2dp up; padding cannot be negative, an offset can
        ProgressRow(book = book, modifier = Modifier.offset(y = (-2).dp))
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
            // its own line height: the theme's 24sp would make the row twice as tall as the text
            lineHeight = 14.sp,
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
            ShelfScreen(
                ShelfUiState(books = PREVIEW_BOOKS, petFound = true),
                {}, {}, {}, {}, {}, {}, {}, {},
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ShelfScreenDarkPreview() {
    KellyReaderTheme {
        ShelfScreen(
            ShelfUiState(books = PREVIEW_BOOKS, petFound = true),
            {}, {}, {}, {}, {}, {}, {}, {},
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ShelfScreenEmptyPreview() {
    KellyReaderTheme {
        ShelfScreen(ShelfUiState(books = emptyList()), {}, {}, {}, {}, {}, {}, {}, {})
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ShelfScreenEditPreview() {
    KellyReaderTheme {
        ShelfScreen(
            ShelfUiState(books = PREVIEW_BOOKS, editing = true, selected = setOf("b3", "b6")),
            {}, {}, {}, {}, {}, {}, {}, {},
        )
    }
}
