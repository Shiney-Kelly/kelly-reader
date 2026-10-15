/*
 * The shelf bear: a ledge along the bottom of the shelf and the bear that rests, walks and reads
 * on it.
 */
package com.kelly.reader.ui.shelf

import android.animation.ValueAnimator
import android.graphics.drawable.BitmapDrawable
import android.os.SystemClock
import android.util.DisplayMetrics
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.mandatorySystemGestures
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
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

// sizes are measured on the 208 px square the art was cut on and scale with the bear; the
// frames themselves are 312 px, the bear's 78dp at 4x
private val PET_SIZE = 78.dp
private const val CANVAS_PX = 208
private val PET_MARGIN = 8.dp

/**
 * The on-screen size of [px] pixels of the frame canvas.
 */
private fun canvas(px: Int): Dp = PET_SIZE * px / CANVAS_PX

// standing paws end 13dp up, inside the ledge's top edge; every frame leaves 12 px of empty
// canvas under them
private val PAW_LINE = 13.dp
private val PET_FOOT_GAP = canvas(12)

// the paw line as a share of the frame's height, where the bear stands when it grows or sinks
private const val PAW_LINE_FRACTION = (CANVAS_PX - 12f) / CANVAS_PX
private val PET_LIFT = PAW_LINE - PET_FOOT_GAP

// seated poses touch the ground only with the near paw, their seat and far foot stop a little
// higher, so they sit lower than the standing ones
private val SEAT_SINK = canvas(8)
private val HALF_SEAT_SINK = canvas(4)

// a soft oval under the bear grounds it on the ledge
private val PET_SHADOW_WIDTH = canvas(160)
private val PET_SHADOW_HEIGHT = canvas(28)

// the bubble sits a little above the reading bear's head (11 px down the canvas, in the seated
// pose), a little right of its middle
private val BUBBLE_LIFT = PET_LIFT - SEAT_SINK + PET_SIZE - canvas(11) + 3.dp
private val BUBBLE_SHIFT = canvas(120)
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
            it.rising,
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
            rising = it.getOrNull(7) as? Boolean ?: false,
        )
    },
)

/**
 * The ledge along the bottom of the shelf and the bear that lives on it. The bear rests, now
 * and then walks to the other end, and sits down to read when tapped. [visible] false hides
 * both and pauses the bear, as in edit mode; the bear also pauses while the screen is not
 * resumed, and stays seated when the system has animations turned off. It keeps its place
 * while a book is open. Until [found] only the ledge shows: three quick knocks on it call the
 * bear, which peeks in from the nearer screen edge and walks on, and [onFound] keeps it.
 */
@Composable
fun ShelfPet(
    visible: Boolean,
    found: Boolean,
    onFound: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var pet by rememberSaveable(stateSaver = PET_SAVER) {
        mutableStateOf(ShelfPetState(idleForMs = nextIdleMs(Random.Default)))
    }
    var animate by remember { mutableStateOf(true) }
    var knocks by remember { mutableIntStateOf(0) }
    var lastKnockMs by remember { mutableLongStateOf(0L) }
    val onFoundNow by rememberUpdatedState(onFound)
    if (!visible) return

    val resources = LocalResources.current
    val frames = remember(resources) {
        // always the largest frames: at 312 px they hold the bear's 78dp at 4x, so they only ever
        // shrink, which stays sharp where stretching a small frame blurs
        PetFrame.entries.associateWith {
            val drawable = resources.getDrawableForDensity(
                frameRes(it),
                DisplayMetrics.DENSITY_XXXHIGH,
                null,
            )
            checkNotNull(drawable as? BitmapDrawable) { "Shelf bear frame $it is not a bitmap" }
                .bitmap
                .asImageBitmap()
        }
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    val navigationBar = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val gestureZone = WindowInsets.mandatorySystemGestures.asPaddingValues()
        .calculateBottomPadding()
    val colors = AppTheme.colors

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val widthPx = constraints.maxWidth
        val maxX by rememberUpdatedState((maxWidth - PET_MARGIN - PET_SIZE).value)
        LaunchedEffect(lifecycleOwner, found) {
            // a hidden bear stays where it is until a knock calls it
            if (!found) return@LaunchedEffect
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

        if (!found) {
            // while the bear is hidden, three quick knocks on the ledge call it in from the
            // nearer side; the knocks count from the ledge's shade down to where the system's
            // home gesture begins, since quick taps below that can send the app home
            val ledgeTop = LEDGE_SHADE_HEIGHT + LEDGE_TOP_HEIGHT + LEDGE_HEIGHT + navigationBar
            val knockHeight = ledgeTop - gestureZone
            if (knockHeight > 0.dp) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(bottom = gestureZone)
                        .fillMaxWidth()
                        .height(knockHeight)
                        .pointerInput(Unit) {
                            detectTapGestures { tap ->
                                val now = SystemClock.uptimeMillis()
                                knocks = knocksInRow(knocks, lastKnockMs, now)
                                lastKnockMs = now
                                if (knocks >= PET_KNOCKS) {
                                    knocks = 0
                                    pet = enteringPet(
                                        fromLeft = tap.x < size.width / 2f,
                                        ledgeWidth = size.width.toDp().value,
                                        petSize = PET_SIZE.value,
                                    )
                                    onFoundNow()
                                }
                            }
                        },
                )
            }
        }
        if (!found || !pet.shown) return@BoxWithConstraints

        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = pet.x.dp + (PET_SIZE - PET_SHADOW_WIDTH) / 2)
                .padding(bottom = PAW_LINE - PET_SHADOW_HEIGHT / 2 + navigationBar)
                .size(width = PET_SHADOW_WIDTH, height = PET_SHADOW_HEIGHT)
                .drawBehind {
                    // a round gradient squashed into an oval, dark in the middle and fading out
                    scale(scaleX = 1f, scaleY = size.height / size.width) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(colors.petShadow, Color.Transparent),
                                center = center,
                                radius = size.width / 2,
                            ),
                            radius = size.width / 2,
                        )
                    }
                },
        )

        val reading = pet.mode == PetMode.READ
        val label = stringResource(
            if (reading) R.string.shelf_pet_reading_label else R.string.shelf_pet_label,
        )
        val frame = pet.frame(animate)
        Image(
            bitmap = frames.getValue(frame),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = pet.x.dp)
                .padding(bottom = PET_LIFT - seatSink(frame) + navigationBar)
                .size(PET_SIZE)
                .graphicsLayer {
                    scaleX = if (pet.facingRight) 1f else -1f
                    scaleY = pet.height(animate) * frameFit(frame)
                    transformOrigin = TransformOrigin(0.5f, PAW_LINE_FRACTION)
                }
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

/**
 * How much lower [frame] sits than a standing pose: the full [SEAT_SINK] when seated or
 * reading, half of it halfway through sitting down, so the bear lowers smoothly into its seat.
 */
private fun seatSink(frame: PetFrame): Dp = when (frame) {
    PetFrame.SIT_2 -> HALF_SEAT_SINK
    PetFrame.SIT_3,
    PetFrame.IDLE_1, PetFrame.IDLE_2, PetFrame.IDLE_3,
    PetFrame.IDLE_4, PetFrame.IDLE_5, PetFrame.IDLE_6,
    PetFrame.READ_1, PetFrame.READ_2, PetFrame.READ_3,
    PetFrame.READ_4, PetFrame.READ_5, PetFrame.READ_6,
    PetFrame.READ_7, PetFrame.READ_8, PetFrame.READ_9, PetFrame.READ_10 -> SEAT_SINK
    else -> 0.dp
}

// read9 has the bear sitting up a little: its head is 6 px higher than in the reading frames
// around it (187 against 181 of 208 px above the paw line) and the book 4 px higher, with the body
// unchanged, so drawing it that much shorter keeps the head from bobbing
private const val READ_9_FIT = 181f / 187f

/**
 * The share of its full height to draw [frame] at, so that a frame drawn taller than its
 * neighbors lines up with them: below 1 only for [PetFrame.READ_9].
 */
private fun frameFit(frame: PetFrame): Float = if (frame == PetFrame.READ_9) READ_9_FIT else 1f

@DrawableRes
private fun frameRes(frame: PetFrame): Int = when (frame) {
    PetFrame.IDLE_1 -> R.drawable.shelf_pet_idle_1
    PetFrame.IDLE_2 -> R.drawable.shelf_pet_idle_2
    PetFrame.IDLE_3 -> R.drawable.shelf_pet_idle_3
    PetFrame.IDLE_4 -> R.drawable.shelf_pet_idle_4
    PetFrame.IDLE_5 -> R.drawable.shelf_pet_idle_5
    PetFrame.IDLE_6 -> R.drawable.shelf_pet_idle_6
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
    PetFrame.READ_4 -> R.drawable.shelf_pet_read_4
    PetFrame.READ_5 -> R.drawable.shelf_pet_read_5
    PetFrame.READ_6 -> R.drawable.shelf_pet_read_6
    PetFrame.READ_7 -> R.drawable.shelf_pet_read_7
    PetFrame.READ_8 -> R.drawable.shelf_pet_read_8
    PetFrame.READ_9 -> R.drawable.shelf_pet_read_9
    PetFrame.READ_10 -> R.drawable.shelf_pet_read_10
}
