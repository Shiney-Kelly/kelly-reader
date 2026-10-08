package com.kelly.reader.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.kelly.reader.ui.theme.AppTheme
import com.kelly.reader.ui.theme.CHECK_OFF_BG
import com.kelly.reader.ui.theme.CHECK_OFF_INK
import com.kelly.reader.ui.theme.KellyReaderTheme
import com.kelly.reader.ui.theme.LITERATA

// the cover is a book seen from the front: tight corners on the spine side, round on the other
private val COVER_SHAPE = RoundedCornerShape(
    topStart = 4.dp,
    topEnd = 8.dp,
    bottomEnd = 8.dp,
    bottomStart = 4.dp,
)

/**
 * A generated cover: [color] background, a spine strip, the first character of the title as a
 * big glyph and [meta] ("TXT · 386 KB") at the bottom. In edit mode [selected] is non-null and a
 * check circle shows in the top right corner.
 */
@Composable
fun BookCover(
    glyph: String,
    meta: String,
    color: Color,
    modifier: Modifier = Modifier,
    selected: Boolean? = null,
) {
    val colors = AppTheme.colors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(2f / 3f)
            .shadow(elevation = 2.dp, shape = COVER_SHAPE, clip = false)
            .clip(COVER_SHAPE)
            .background(color)
            .border(width = 1.dp, color = colors.coverEdge, shape = COVER_SHAPE),
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(6.dp)
                .background(colors.coverSpine),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 16.dp, top = 12.dp, end = 10.dp, bottom = 10.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = glyph,
                color = colors.coverGlyph,
                fontFamily = LITERATA,
                fontSize = 40.sp,
                lineHeight = 40.sp,
                maxLines = 1,
            )
            Text(
                text = meta,
                color = colors.coverMeta,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.8.sp,
                maxLines = 1,
            )
        }
        if (selected != null) {
            CheckCircle(
                selected = selected,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp),
            )
        }
    }
}

/**
 * The 26dp selection mark: grey outline and check when off, accent fill with a white check
 * when on.
 */
@Composable
private fun CheckCircle(selected: Boolean, modifier: Modifier = Modifier) {
    val background = if (selected) MaterialTheme.colorScheme.primary else CHECK_OFF_BG
    val stroke = if (selected) Color.White else CHECK_OFF_INK
    Box(
        modifier = modifier
            .size(26.dp)
            .clip(CircleShape)
            .background(background)
            .border(width = 2.dp, color = stroke, shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = AppIcons.CHECK,
            contentDescription = null,
            tint = stroke,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Preview(showBackground = true, widthDp = 120)
@Composable
private fun BookCoverPreview() {
    KellyReaderTheme {
        BookCover(glyph = "山", meta = "TXT · 1.2 MB", color = AppTheme.colors.coverColors[1])
    }
}

@Preview(showBackground = true, widthDp = 120)
@Composable
private fun BookCoverSelectedPreview() {
    KellyReaderTheme {
        BookCover(
            glyph = "T",
            meta = "TXT · 386 KB",
            color = AppTheme.colors.coverColors[0],
            selected = true,
        )
    }
}
