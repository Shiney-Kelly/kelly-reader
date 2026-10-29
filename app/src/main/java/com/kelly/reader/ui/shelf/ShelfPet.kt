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
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
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
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.delay

// ----- ledge -----

// a 14dp strip under a 3dp lighter top edge, with a soft shadow above it
private val LEDGE_HEIGHT = 14.dp
private val LEDGE_TOP_HEIGHT = 3.dp
private val LEDGE_SHADE_HEIGHT = 6.dp

/**
 * How far the ledge reaches up from the navigation bar, shade included.
 */
internal val LEDGE_TOTAL_HEIGHT = LEDGE_SHADE_HEIGHT + LEDGE_TOP_HEIGHT + LEDGE_HEIGHT

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

// paw prints sit on the ledge where the standing paws would, above the system's home gesture,
// the near and far feet 1.5dp apart; each is a pad with four toes ahead of it, seen from above
// at a slant, so squashed to 60% of its depth
private val PAW_SWAY = 1.5.dp
private val PAW_PAD_LENGTH = 3.2.dp
private val PAW_PAD_WIDTH = 4.6.dp
private val PAW_TOE_RADIUS = 0.9.dp
private val PAW_TOE_REACH = 3.4.dp
private const val PAW_SLANT = 0.6f
private val PAW_TOE_ANGLES = listOf(-60.0, -20.0, 20.0, 60.0).map(Math::toRadians)

// a soft oval under the bear grounds it on the ledge
private val PET_SHADOW_WIDTH = canvas(160)
private val PET_SHADOW_HEIGHT = canvas(28)

// the bubble sits a little above the reading bear's head (11 px down the canvas, in the seated
// pose), its pointed corner over the head: a little right of the middle of the art, so a little
// left of it when the bear faces left and the art is mirrored
private val BUBBLE_LIFT = PET_LIFT - SEAT_SINK + PET_SIZE - canvas(11) + 3.dp
private val HEAD_X = canvas(120)
private val HEAD_X_MIRRORED = canvas(CANVAS_PX - 120)

// on its feet the bear's head is lower and ahead of its middle: the ears top out 46 px down the
// canvas and the face is about 157 px across, so hello and bye sit there instead
private val WALK_BUBBLE_LIFT = PET_LIFT + PET_SIZE - canvas(46) + 3.dp
private val WALK_HEAD_X = canvas(157)
private val WALK_HEAD_X_MIRRORED = canvas(CANVAS_PX - 157)

// the bubble opens away from the nearer screen edge: to the right of the head, pointing down
// to its left, or to the left of it, pointing down to its right
private val BUBBLE_POINTS_LEFT = RoundedCornerShape(
    topStart = 10.dp,
    topEnd = 10.dp,
    bottomEnd = 10.dp,
    bottomStart = 2.dp,
)
private val BUBBLE_POINTS_RIGHT = RoundedCornerShape(
    topStart = 10.dp,
    topEnd = 10.dp,
    bottomEnd = 2.dp,
    bottomStart = 10.dp,
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
            it.leaveToX,
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
            leaveToX = it.getOrNull(8) as? Float ?: 0f,
        )
    },
)

/**
 * The ledge along the bottom of the shelf and the bear that lives on it. The bear rests, now
 * and then walks to the other end, and sits down to read when tapped. [visible] false hides
 * both and pauses the bear, as in edit mode; the bear also pauses while the screen is not
 * resumed, and stays seated when the system has animations turned off. It keeps its place
 * while a book is open. Until [found] only the ledge shows: three quick knocks on it call the
 * bear, which peeks in from the nearer screen edge with a hello and walks on, and [onFound] keeps
 * it. A long press sends it off by the nearer edge with a bye; once it is out of sight [onLeft]
 * hides it again. [called] says it was found elsewhere: it comes in the same way from a random
 * side, and [onCalledIn] says so.
 */
@Composable
fun ShelfPet(
    visible: Boolean,
    found: Boolean,
    called: Boolean,
    onFound: () -> Unit,
    onLeft: () -> Unit,
    onCalledIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var pet by rememberSaveable(stateSaver = PET_SAVER) {
        mutableStateOf(ShelfPetState(idleForMs = nextIdleMs(Random.Default)))
    }
    var animate by remember { mutableStateOf(true) }
    var knocks by remember { mutableIntStateOf(0) }
    var lastKnockMs by remember { mutableLongStateOf(0L) }
    val onFoundNow by rememberUpdatedState(onFound)
    val onLeftNow by rememberUpdatedState(onLeft)
    val onCalledInNow by rememberUpdatedState(onCalledIn)
    // time into the current paw print trail, below zero while there is none
    val trail = remember { Animatable(-1f) }
    var trailFromLeft by remember { mutableStateOf(true) }
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
        val ledgeWidth by rememberUpdatedState(maxWidth.value)
        LaunchedEffect(lifecycleOwner, found) {
            // a hidden bear stays where it is until a knock calls it
            if (!found) return@LaunchedEffect
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                animate = ValueAnimator.areAnimatorsEnabled()
                while (true) {
                    delay(pet.tickMs)
                    pet = pet.tick(PET_MARGIN.value, maxX, Random.Default, animate)
                }
            }
        }

        LaunchedEffect(pet.mode) {
            // walked out of sight after a long press: hidden until knocked for again
            if (pet.mode == PetMode.GONE) onLeftNow()
        }

        LaunchedEffect(called) {
            // found elsewhere, such as on the upload page: it peeks in and walks on as after
            // the knocks, from whichever side
            if (!called) return@LaunchedEffect
            pet = enteringPet(
                fromLeft = Random.nextBoolean(),
                ledgeWidth = ledgeWidth,
                petSize = PET_SIZE.value,
            )
            onCalledInNow()
        }

        LaunchedEffect(lifecycleOwner, found) {
            // only a hidden bear leaves paw prints, and none with animations off; a trail cut off
            // by the knocks keeps its last look, so it is cleared before the bear comes in
            trail.snapTo(-1f)
            if (found) return@LaunchedEffect
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                trail.snapTo(-1f)
                if (!ValueAnimator.areAnimatorsEnabled()) return@repeatOnLifecycle
                var wait = Random.nextLong(PAW_FIRST_MIN_MS, PAW_FIRST_MAX_MS + 1)
                while (true) {
                    delay(wait)
                    val trailMs = pawTrailMs(pawCount(ledgeWidth)).toFloat()
                    trailFromLeft = Random.nextBoolean()
                    trail.snapTo(0f)
                    trail.animateTo(
                        targetValue = trailMs,
                        animationSpec = tween(
                            durationMillis = trailMs.toInt(),
                            easing = LinearEasing,
                        ),
                    )
                    trail.snapTo(-1f)
                    wait = Random.nextLong(PAW_NEXT_MIN_MS, PAW_NEXT_MAX_MS + 1)
                }
            }
        }

        // the ledge runs on under the navigation bar, so the bear never stands on the bar
        val shade = Brush.verticalGradient(listOf(Color.Transparent, colors.ledgeShade))
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .drawWithContent {
                    drawContent()
                    val elapsed = trail.value
                    if (elapsed >= 0f) {
                        val y = size.height - (navigationBar + PAW_LINE).toPx()
                        drawPawTrail(elapsed, y, trailFromLeft, colors.petShadow)
                    }
                },
        ) {
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
        val leaveLabel = stringResource(R.string.shelf_pet_leave_label)
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
                .combinedClickable(
                    interactionSource = null,
                    indication = null,
                    onLongClickLabel = leaveLabel,
                    onLongClick = { pet = pet.onLongPress(ledgeWidth, PET_SIZE.value) },
                    onClick = { pet = pet.onTap() },
                ),
        )
        val bubble = pet.bubble
        if (bubble != null) {
            // the bear reads seated, and says hello and bye on its feet
            val seated = bubble == PetBubble.READING
            val headX = pet.x.dp + when {
                seated && pet.facingRight -> HEAD_X
                seated -> HEAD_X_MIRRORED
                pet.facingRight -> WALK_HEAD_X
                else -> WALK_HEAD_X_MIRRORED
            }
            val lift = if (seated) BUBBLE_LIFT else WALK_BUBBLE_LIFT
            val opensRight = headX < maxWidth / 2
            val shape = if (opensRight) BUBBLE_POINTS_LEFT else BUBBLE_POINTS_RIGHT
            val text = when (bubble) {
                PetBubble.READING -> R.string.shelf_pet_bubble
                PetBubble.HELLO -> R.string.shelf_pet_hello
                PetBubble.BYE -> R.string.shelf_pet_bye
            }
            Text(
                text = stringResource(text),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(bottom = lift + navigationBar)
                    .layout { measurable, bubbleConstraints ->
                        // the pointed corner over the head, the bubble on the side with room;
                        // kept on screen all the same
                        val bubble = measurable.measure(bubbleConstraints)
                        val head = headX.roundToPx()
                        val wanted = if (opensRight) head else head - bubble.width
                        val margin = PET_MARGIN.roundToPx()
                        val limit = (widthPx - bubble.width - margin).coerceAtLeast(margin)
                        val x = wanted.coerceIn(margin, limit)
                        layout(bubble.width, bubble.height) { bubble.placeRelative(x, 0) }
                    }
                    .shadow(elevation = 1.dp, shape = shape)
                    .background(MaterialTheme.colorScheme.surface, shape)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
                    .clearAndSetSemantics {},
            )
        }
    }
}

/**
 * Draws the paw prints of a trail [elapsedMs] in, walking along the line [y] from the left edge
 * when [fromLeft], else from the right, each one as strong as [pawAlpha] says.
 */
private fun DrawScope.drawPawTrail(
    elapsedMs: Float,
    y: Float,
    fromLeft: Boolean,
    color: Color,
) {
    val gap = PAW_GAP_DP.dp.toPx()
    val sway = PAW_SWAY.toPx()
    for (index in 0 until pawCount(size.width.toDp().value)) {
        val alpha = pawAlpha(elapsedMs, index)
        if (alpha <= 0f) continue
        val along = (index + 0.5f) * gap
        val x = if (fromLeft) along else size.width - along
        // near and far feet take turns
        val center = Offset(x, y + if (index % 2 == 0) sway else -sway)
        val faded = color.copy(alpha = color.alpha * alpha)
        drawPawPrint(center, facingRight = fromLeft, color = faded)
    }
}

/**
 * Draws one paw print around [center]: a pad with four toes ahead of it, pointing right when
 * [facingRight].
 */
private fun DrawScope.drawPawPrint(center: Offset, facingRight: Boolean, color: Color) {
    val padLength = PAW_PAD_LENGTH.toPx()
    val padDepth = PAW_PAD_WIDTH.toPx() * PAW_SLANT
    drawOval(
        color = color,
        topLeft = Offset(center.x - padLength / 2, center.y - padDepth / 2),
        size = Size(padLength, padDepth),
    )
    val forward = if (facingRight) 1f else -1f
    val reach = PAW_TOE_REACH.toPx()
    for (angle in PAW_TOE_ANGLES) {
        val toe = Offset(
            center.x + forward * reach * cos(angle).toFloat(),
            center.y + reach * PAW_SLANT * sin(angle).toFloat(),
        )
        drawCircle(color = color, radius = PAW_TOE_RADIUS.toPx(), center = toe)
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
