/*
 * The shelf bear: a ledge along the bottom of the shelf and the bear that rests, walks and reads
 * on it.
 */
package com.kelly.reader.ui.shelf

import android.animation.ValueAnimator
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.kelly.reader.R
import com.kelly.reader.ui.theme.AppTheme
import kotlin.random.Random
import kotlinx.coroutines.delay

// ----- ledge -----

// a 14dp strip under a 3dp lighter top edge, with a soft shadow above it
private val LEDGE_HEIGHT = 14.dp
private val LEDGE_TOP_HEIGHT = 3.dp
private val LEDGE_SHADE_HEIGHT = 6.dp

// ----- bear -----

// the bear's box stands 12dp up, which puts the paws on the ledge's top edge
private val PET_SIZE = 52.dp
private val PET_LIFT = 12.dp
private val PET_MARGIN = 8.dp

// the bubble sits above the bear's head, a little to the right
private val BUBBLE_LIFT = 68.dp
private val BUBBLE_SHIFT = 30.dp
private val BUBBLE_SHAPE = RoundedCornerShape(
    topStart = 10.dp,
    topEnd = 10.dp,
    bottomEnd = 10.dp,
    bottomStart = 2.dp,
)

private val PET_SAVER = listSaver<ShelfPetState, Any>(
    save = {
        listOf(
            it.mode.ordinal,
            it.x,
            it.facingRight,
            it.elapsedMs,
            it.step,
            it.readAfterSitting,
            it.idleForMs,
        )
    },
    restore = {
        ShelfPetState(
            mode = PetMode.entries[it[0] as Int],
            x = it[1] as Float,
            facingRight = it[2] as Boolean,
            elapsedMs = it[3] as Long,
            step = it[4] as Int,
            readAfterSitting = it[5] as Boolean,
            idleForMs = it[6] as Long,
        )
    },
)

/**
 * The ledge along the bottom of the shelf and the bear that lives on it. The bear rests, now
 * and then walks to the other end, and sits down to read when tapped. [visible] false hides
 * both and pauses the bear, as in edit mode; the bear also pauses while the screen is not
 * resumed, and stays seated when the system has animations turned off. It keeps its place
 * while a book is open.
 */
@Composable
fun ShelfPet(
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    var pet by rememberSaveable(stateSaver = PET_SAVER) {
        mutableStateOf(ShelfPetState(idleForMs = nextIdleMs(Random.Default)))
    }
    var animate by remember { mutableStateOf(true) }
    if (!visible) return

    val resources = LocalResources.current
    val frames = remember(resources) {
        PetFrame.entries.associateWith { ImageBitmap.imageResource(resources, frameRes(it)) }
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    val navigationBar = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val colors = AppTheme.colors

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val widthPx = constraints.maxWidth
        val maxX by rememberUpdatedState((maxWidth - PET_MARGIN - PET_SIZE).value)
        LaunchedEffect(lifecycleOwner) {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                animate = ValueAnimator.areAnimatorsEnabled()
                while (true) {
                    delay(PET_TICK_MS)
                    pet = pet.tick(PET_MARGIN.value, maxX, Random.Default, animate)
                }
            }
        }

        // the ledge runs on under the navigation bar, so the bear never stands on the bar
        val shade = Brush.verticalGradient(listOf(Color.Transparent, colors.ledgeShade))
        Column(modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(LEDGE_SHADE_HEIGHT)
                    .background(shade),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(LEDGE_TOP_HEIGHT)
                    .background(colors.ledgeTop),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(LEDGE_HEIGHT + navigationBar)
                    .background(colors.ledge),
            )
        }

        val reading = pet.mode == PetMode.READ
        val label = stringResource(
            if (reading) R.string.shelf_pet_reading_label else R.string.shelf_pet_label,
        )
        Image(
            bitmap = frames.getValue(pet.frame(animate)),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = pet.x.dp)
                .padding(bottom = PET_LIFT + navigationBar)
                .size(PET_SIZE)
                .graphicsLayer { scaleX = if (pet.facingRight) 1f else -1f }
                .semantics {
                    contentDescription = label
                    role = Role.Button
                }
                .clickable(interactionSource = null, indication = null) { pet = pet.onTap() },
        )
        if (reading) {
            Text(
                text = stringResource(R.string.shelf_pet_bubble),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(bottom = BUBBLE_LIFT + navigationBar)
                    .layout { measurable, bubbleConstraints ->
                        // beside the bear's head, but never past the end of the screen
                        val bubble = measurable.measure(bubbleConstraints)
                        val wanted = (pet.x.dp + BUBBLE_SHIFT).roundToPx()
                        val limit = widthPx - bubble.width - PET_MARGIN.roundToPx()
                        val x = wanted.coerceAtMost(limit).coerceAtLeast(0)
                        layout(bubble.width, bubble.height) { bubble.placeRelative(x, 0) }
                    }
                    .shadow(elevation = 1.dp, shape = BUBBLE_SHAPE)
                    .background(MaterialTheme.colorScheme.surface, BUBBLE_SHAPE)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
                    .clearAndSetSemantics {},
            )
        }
    }
}

@DrawableRes
private fun frameRes(frame: PetFrame): Int = when (frame) {
    PetFrame.IDLE_1 -> R.drawable.shelf_pet_idle_1
    PetFrame.IDLE_2 -> R.drawable.shelf_pet_idle_2
    PetFrame.IDLE_3 -> R.drawable.shelf_pet_idle_3
    PetFrame.SIT_1 -> R.drawable.shelf_pet_sit_1
    PetFrame.SIT_2 -> R.drawable.shelf_pet_sit_2
    PetFrame.SIT_3 -> R.drawable.shelf_pet_sit_3
    PetFrame.WALK_1 -> R.drawable.shelf_pet_walk_1
    PetFrame.WALK_2 -> R.drawable.shelf_pet_walk_2
    PetFrame.WALK_3 -> R.drawable.shelf_pet_walk_3
    PetFrame.WALK_4 -> R.drawable.shelf_pet_walk_4
    PetFrame.WALK_5 -> R.drawable.shelf_pet_walk_5
    PetFrame.WALK_6 -> R.drawable.shelf_pet_walk_6
    PetFrame.WALK_7 -> R.drawable.shelf_pet_walk_7
    PetFrame.WALK_8 -> R.drawable.shelf_pet_walk_8
    PetFrame.READ_1 -> R.drawable.shelf_pet_read_1
    PetFrame.READ_2 -> R.drawable.shelf_pet_read_2
    PetFrame.READ_3 -> R.drawable.shelf_pet_read_3
}
