package com.kelly.reader.ui.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.kelly.reader.R
import com.kelly.reader.ui.components.AppIcons
import com.kelly.reader.ui.components.ProgressBar
import com.kelly.reader.ui.theme.AppTheme
import com.kelly.reader.ui.theme.SCRIM

private val DRAWER_WIDTH = 310.dp
private val DRAWER_SHAPE = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp)
private val CONTROL_SHAPE = RoundedCornerShape(10.dp)

/**
 * The reading settings drawer: slides in from the right over a scrim. The sections sit in
 * [content], the book card and the back button are always there.
 */
@Composable
fun SettingsDrawer(
    open: Boolean,
    uiState: ReaderUiState,
    onClose: () -> Unit,
    onBackToShelf: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier.fillMaxSize()) {
        AnimatedVisibility(visible = open, enter = fadeIn(), exit = fadeOut()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(SCRIM)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClickLabel = stringResource(R.string.reader_close_settings),
                        onClick = onClose,
                    ),
            )
        }
        AnimatedVisibility(
            visible = open,
            enter = slideInHorizontally { it },
            exit = slideOutHorizontally { it },
            modifier = Modifier.align(Alignment.CenterEnd),
        ) {
            DrawerPanel(
                uiState = uiState,
                onClose = onClose,
                onBackToShelf = onBackToShelf,
                content = content,
            )
        }
    }
}

@Composable
private fun DrawerPanel(
    uiState: ReaderUiState,
    onClose: () -> Unit,
    onBackToShelf: () -> Unit,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(DRAWER_WIDTH)
            .shadow(elevation = 12.dp, shape = DRAWER_SHAPE)
            .clip(DRAWER_SHAPE)
            .background(MaterialTheme.colorScheme.surface)
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, top = 16.dp, end = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        DrawerHeader(onClose = onClose)
        BookCard(uiState = uiState)
        content()
        Spacer(modifier = Modifier.weight(1f))
        BackToShelfButton(onClick = onBackToShelf)
    }
}

@Composable
private fun DrawerHeader(onClose: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.reader_settings),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        IconButton(
            onClick = onClose,
            modifier = Modifier.size(44.dp).offset(x = 10.dp),
        ) {
            Icon(
                imageVector = AppIcons.CLOSE,
                contentDescription = stringResource(R.string.reader_close_settings),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

/**
 * The current book: title, a progress bar and "Page x of y · NN% read".
 */
@Composable
private fun BookCard(uiState: ReaderUiState) {
    val page = uiState.pageIndex + 1
    val pageLabel = uiState.pageCount?.let { stringResource(R.string.reader_page_of, page, it) }
        ?: stringResource(R.string.reader_page_of_unknown, page)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(AppTheme.colors.tint2)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = uiState.book?.title ?: "",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            style = TextStyle(textDirection = TextDirection.Content),
        )
        ProgressBar(progress = uiState.progress, height = 4.dp)
        Text(
            text = stringResource(R.string.reader_page_read, pageLabel, uiState.percent),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun BackToShelfButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp))
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = AppIcons.GRID,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = stringResource(R.string.reader_back_to_shelf),
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/**
 * A section of the drawer: a muted 13sp label above its controls.
 */
@Composable
fun DrawerSection(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        content()
    }
}

/**
 * One choice in a row of choices: outlined when off, tinted with an accent border when on.
 */
@Composable
fun SegmentButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
) {
    val border = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    val background = if (selected) AppTheme.colors.tint else MaterialTheme.colorScheme.surface
    val ink = if (selected) AppTheme.colors.tintInk else MaterialTheme.colorScheme.onSurface
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(CONTROL_SHAPE)
            .background(background)
            .border(1.5.dp, border, CONTROL_SHAPE)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = ink,
            style = style,
        )
    }
}
