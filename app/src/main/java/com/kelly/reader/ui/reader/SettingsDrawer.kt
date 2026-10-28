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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

import com.kelly.reader.R
import com.kelly.reader.domain.LineSpacing
import com.kelly.reader.domain.ProgressLabel
import com.kelly.reader.domain.ReaderFont
import com.kelly.reader.domain.ReadingMode
import com.kelly.reader.domain.TextAlignment
import com.kelly.reader.domain.formatPercent
import com.kelly.reader.ui.components.AppIcons
import com.kelly.reader.ui.theme.AppTheme
import com.kelly.reader.ui.theme.FIGTREE
import com.kelly.reader.ui.theme.LITERATA
import com.kelly.reader.ui.theme.ReaderTheme
import com.kelly.reader.ui.theme.SCRIM

private val DRAWER_WIDTH = 310.dp
private val DRAWER_SHAPE = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp)
private val CONTROL_SHAPE = RoundedCornerShape(10.dp)
private val SWATCH_SHAPE = RoundedCornerShape(12.dp)
// button labels take 14 sp unless one in the row is too wide for its button, then the whole
// row steps down together, to 8 sp at least; both grow with the system font scale
private const val BUTTON_LABEL_MAX_SP = 14f
private const val BUTTON_LABEL_MIN_SP = 8f
private const val BUTTON_LABEL_STEP_SP = 0.5f
private val BUTTON_LABEL_PADDING = 6.dp
private val BUTTON_GAP = 8.dp

/**
 * What the drawer's controls do. Each call goes to the settings repository through the
 * ViewModel.
 */
data class SettingsActions(
    val onSmallerText: () -> Unit,
    val onLargerText: () -> Unit,
    val onLineSpacing: (LineSpacing) -> Unit,
    val onFont: (ReaderFont) -> Unit,
    val onTheme: (ReaderTheme?) -> Unit,
    val onReadingMode: (ReadingMode) -> Unit,
    val onTextAlignment: (TextAlignment) -> Unit,
    val onProgressLabel: (ProgressLabel) -> Unit,
    val onJump: (Float) -> Unit,
) {

    companion object {
        val NONE = SettingsActions({}, {}, {}, {}, {}, {}, {}, {}, {})
    }
}

/**
 * The reading settings drawer: slides in from the right over a scrim, with the controls and
 * the back to shelf button.
 */
@Composable
fun SettingsDrawer(
    open: Boolean,
    uiState: ReaderUiState,
    actions: SettingsActions,
    onClose: () -> Unit,
    onBackToShelf: () -> Unit,
    modifier: Modifier = Modifier,
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
                actions = actions,
                onClose = onClose,
                onBackToShelf = onBackToShelf,
            )
        }
    }
}

@Composable
private fun DrawerPanel(
    uiState: ReaderUiState,
    actions: SettingsActions,
    onClose: () -> Unit,
    onBackToShelf: () -> Unit,
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
        PositionSection(uiState = uiState, onJump = actions.onJump)
        ReadingModeSection(selected = uiState.settings.readingMode, onPick = actions.onReadingMode)
        ProgressLabelSection(
            selected = uiState.settings.progressLabel,
            onPick = actions.onProgressLabel,
        )
        TextSizeSection(fontSize = uiState.settings.fontSize, actions = actions)
        AlignmentSection(selected = uiState.settings.textAlignment, onPick = actions.onTextAlignment)
        LineSpacingSection(selected = uiState.settings.lineSpacing, onPick = actions.onLineSpacing)
        FontSection(selected = uiState.settings.font, onPick = actions.onFont)
        ThemeSection(selectedKey = uiState.settings.themeKey, onPick = actions.onTheme)
        Spacer(modifier = Modifier.weight(1f))
        BackToShelfButton(onClick = onBackToShelf)
    }
}

/**
 * Where the reader is, as a slider over the whole book. While it is dragged the label shows
 * where it would go, in percent, or as a page when the footer shows pages and every page is
 * known; letting go jumps there.
 */
@Composable
private fun PositionSection(uiState: ReaderUiState, onJump: (Float) -> Unit) {
    var dragged by remember { mutableStateOf<Float?>(null) }
    val shown = dragged ?: uiState.progress
    val count = uiState.pageCount
    val label = if (uiState.settings.progressLabel == ProgressLabel.PAGE && count != null) {
        val page = (shown * count).roundToInt().coerceIn(1, count)
        stringResource(R.string.reader_page_of, page, count)
    } else {
        formatPercent(shown)
    }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.reader_position),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Slider(
            value = shown,
            onValueChange = { dragged = it },
            onValueChangeFinished = {
                dragged?.let(onJump)
                dragged = null
            },
            enabled = uiState.book != null,
        )
    }
}

@Composable
private fun AlignmentSection(selected: TextAlignment, onPick: (TextAlignment) -> Unit) {
    DrawerSection(label = stringResource(R.string.reader_alignment)) {
        val alignments = TextAlignment.entries
        val labels = alignments.map { stringResource(it.labelRes) }
        EvenButtonRow(labels = labels) { index, label, size ->
            SegmentButton(
                label = label,
                selected = alignments[index] == selected,
                onClick = { onPick(alignments[index]) },
                modifier = Modifier.weight(1f),
                labelSize = size,
            )
        }
    }
}

@Composable
private fun ReadingModeSection(selected: ReadingMode, onPick: (ReadingMode) -> Unit) {
    DrawerSection(label = stringResource(R.string.reader_mode)) {
        val modes = ReadingMode.entries
        EvenButtonRow(labels = modes.map { stringResource(it.labelRes) }) { index, label, size ->
            SegmentButton(
                label = label,
                selected = modes[index] == selected,
                onClick = { onPick(modes[index]) },
                modifier = Modifier.weight(1f),
                labelSize = size,
            )
        }
    }
}

@Composable
private fun ProgressLabelSection(selected: ProgressLabel, onPick: (ProgressLabel) -> Unit) {
    DrawerSection(label = stringResource(R.string.reader_progress)) {
        val choices = ProgressLabel.entries
        EvenButtonRow(labels = choices.map { stringResource(it.labelRes) }) { index, label, size ->
            SegmentButton(
                label = label,
                selected = choices[index] == selected,
                onClick = { onPick(choices[index]) },
                modifier = Modifier.weight(1f),
                labelSize = size,
            )
        }
    }
}

/**
 * A row of equal buttons, one for each of [labels], with every label at one size: the largest
 * from 14 sp down to 8 sp at which each label fits its button on one line, so one long label
 * does not leave its row uneven. [styleFor] gives label number i the style its button draws it
 * with. [button] draws button number i with its label and the size they share.
 */
@Composable
private fun EvenButtonRow(
    labels: List<String>,
    styleFor: (Int) -> TextStyle = { TextStyle.Default },
    button: @Composable RowScope.(index: Int, label: String, labelSize: TextUnit) -> Unit,
) {
    BoxWithConstraints {
        val gaps = BUTTON_GAP * (labels.size - 1)
        val room = (maxWidth - gaps) / labels.size - BUTTON_LABEL_PADDING * 2
        val labelSize = rememberSharedLabelSize(labels, labels.indices.map(styleFor), room)
        Row(horizontalArrangement = Arrangement.spacedBy(BUTTON_GAP)) {
            labels.forEachIndexed { index, label -> button(index, label, labelSize) }
        }
    }
}

/**
 * The largest size from 14 sp down to 8 sp, in 0.5 sp steps, at which each of [labels], drawn
 * semibold in its own of [styles], fits [room] on one line; 8 sp when even that does not fit.
 */
@Composable
private fun rememberSharedLabelSize(
    labels: List<String>,
    styles: List<TextStyle>,
    room: Dp,
): TextUnit {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    return remember(labels, styles, room, density) {
        // a pixel short of the room, as the buttons share the row's width in whole pixels
        val roomPx = with(density) { room.toPx() }.toInt() - 1
        fun fits(size: Float): Boolean = labels.indices.all { index ->
            val style = styles[index].copy(fontSize = size.sp, fontWeight = FontWeight.SemiBold)
            val line = measurer.measure(labels[index], style, maxLines = 1, softWrap = false)
            line.size.width <= roomPx
        }
        var size = BUTTON_LABEL_MAX_SP
        while (size > BUTTON_LABEL_MIN_SP && !fits(size)) size -= BUTTON_LABEL_STEP_SP
        size.sp
    }
}

@Composable
private fun TextSizeSection(fontSize: Int, actions: SettingsActions) {
    DrawerSection(label = stringResource(R.string.reader_text_size)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StepButton(
                label = "A−",
                description = stringResource(R.string.reader_smaller_text),
                fontSize = 14.sp,
                onClick = actions.onSmallerText,
            )
            Text(
                text = fontSize.toString(),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            StepButton(
                label = "A+",
                description = stringResource(R.string.reader_larger_text),
                fontSize = 19.sp,
                onClick = actions.onLargerText,
            )
        }
    }
}

@Composable
private fun StepButton(
    label: String,
    description: String,
    fontSize: TextUnit,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(width = 56.dp, height = 44.dp)
            .clip(CONTROL_SHAPE)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.5.dp, MaterialTheme.colorScheme.outline, CONTROL_SHAPE)
            .clickable(onClickLabel = description, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = fontSize,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun LineSpacingSection(selected: LineSpacing, onPick: (LineSpacing) -> Unit) {
    DrawerSection(label = stringResource(R.string.reader_line_spacing)) {
        val spacings = LineSpacing.entries
        EvenButtonRow(labels = spacings.map { stringResource(it.labelRes) }) { index, label, size ->
            SegmentButton(
                label = label,
                selected = spacings[index] == selected,
                onClick = { onPick(spacings[index]) },
                modifier = Modifier.weight(1f),
                labelSize = size,
            )
        }
    }
}

@Composable
private fun FontSection(selected: ReaderFont, onPick: (ReaderFont) -> Unit) {
    DrawerSection(label = stringResource(R.string.reader_font)) {
        val fonts = ReaderFont.entries
        // each name in its own face
        val styles = fonts.map { font ->
            TextStyle(fontFamily = if (font == ReaderFont.SERIF) LITERATA else FIGTREE)
        }
        EvenButtonRow(
            labels = fonts.map { stringResource(it.labelRes) },
            styleFor = { styles[it] },
        ) { index, label, size ->
            SegmentButton(
                label = label,
                selected = fonts[index] == selected,
                onClick = { onPick(fonts[index]) },
                modifier = Modifier.weight(1f),
                labelSize = size,
                style = styles[index],
            )
        }
    }
}

/**
 * The three reading themes as swatches in their own colors plus Auto, which follows the
 * system. The chosen one has an accent border; [selectedKey] null means Auto.
 */
@Composable
private fun ThemeSection(selectedKey: String?, onPick: (ReaderTheme?) -> Unit) {
    DrawerSection(label = stringResource(R.string.reader_theme)) {
        val themes = ReaderTheme.entries
        // a swatch draws its label in the surrounding text style, so it is measured in it too
        val textStyle = LocalTextStyle.current
        val labels = themes.map { stringResource(it.labelRes) } +
            stringResource(R.string.reader_theme_auto)
        EvenButtonRow(labels = labels, styleFor = { textStyle }) { index, label, size ->
            // the last swatch, after the themes, is Auto
            val theme = themes.getOrNull(index)
            ThemeSwatch(
                label = label,
                background = theme?.background ?: MaterialTheme.colorScheme.surface,
                ink = theme?.ink ?: MaterialTheme.colorScheme.onSurface,
                selected = theme?.key == selectedKey,
                onClick = { onPick(theme) },
                labelSize = size,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ThemeSwatch(
    label: String,
    background: Color,
    ink: Color,
    selected: Boolean,
    onClick: () -> Unit,
    labelSize: TextUnit,
    modifier: Modifier = Modifier,
) {
    val border = if (selected) AppTheme.colors.accentText else MaterialTheme.colorScheme.outline
    Box(
        modifier = modifier
            .height(52.dp)
            .clip(SWATCH_SHAPE)
            .background(background)
            .border(2.dp, border, SWATCH_SHAPE)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = labelSize,
            fontWeight = FontWeight.SemiBold,
            color = ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = BUTTON_LABEL_PADDING),
        )
    }
}

private val LineSpacing.labelRes: Int
    get() = when (this) {
        LineSpacing.COMPACT -> R.string.reader_spacing_compact
        LineSpacing.NORMAL -> R.string.reader_spacing_normal
        LineSpacing.LOOSE -> R.string.reader_spacing_loose
    }

private val TextAlignment.labelRes: Int
    get() = when (this) {
        TextAlignment.LEFT -> R.string.reader_align_left
        TextAlignment.JUSTIFY -> R.string.reader_align_justify
        TextAlignment.RIGHT -> R.string.reader_align_right
    }

private val ReadingMode.labelRes: Int
    get() = when (this) {
        ReadingMode.PAGED -> R.string.reader_mode_paged
        ReadingMode.SCROLL -> R.string.reader_mode_scroll
    }

private val ProgressLabel.labelRes: Int
    get() = when (this) {
        ProgressLabel.PERCENT -> R.string.reader_progress_percent
        ProgressLabel.PAGE -> R.string.reader_progress_page
    }

private val ReaderFont.labelRes: Int
    get() = when (this) {
        ReaderFont.SERIF -> R.string.reader_font_serif
        ReaderFont.SANS -> R.string.reader_font_sans
    }

private val ReaderTheme.labelRes: Int
    get() = when (this) {
        ReaderTheme.LIGHT -> R.string.reader_theme_light
        ReaderTheme.PAPER -> R.string.reader_theme_paper
        ReaderTheme.DARK -> R.string.reader_theme_dark
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
 * One choice in a row of choices: outlined when off, tinted with an accent border when on. The
 * label shows at [labelSize] on one line, cut with an ellipsis if it is still too wide.
 */
@Composable
fun SegmentButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    labelSize: TextUnit = BUTTON_LABEL_MAX_SP.sp,
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
            fontSize = labelSize,
            fontWeight = FontWeight.SemiBold,
            color = ink,
            style = style,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = BUTTON_LABEL_PADDING),
        )
    }
}
