package com.kelly.reader.ui.shelf

import kotlin.math.ceil
import kotlin.random.Random

// ----- timing -----

const val PET_TICK_MS = 125L

// while walking the ticks come faster, so the bear covers ground half again as fast with the
// same step and the same frames, and its feet still match the ground; every other time in the
// cycle is counted in milliseconds and keeps its length
const val PET_WALK_TICK_MS = 80L
const val PET_SIT_FRAME_MS = 160L
const val PET_IDLE_FRAME_MS = 500L
const val PET_READ_FRAME_MS = 360L

// reading plays its ten frames twice
const val PET_READ_MS = 2 * 10 * PET_READ_FRAME_MS

// the last sit-down frame is 95% as tall as the seated ones (its ears top out 176 against 185 of
// 208 px above the paw line), so a seated pose that follows it grows from that height over a few
// ticks, and before standing up the seated pose sinks to it
const val PET_SIT_DOWN_HEIGHT = 0.95f
const val PET_RISE_MS = 3 * PET_TICK_MS
const val PET_CROUCH_MS = 2 * PET_TICK_MS
const val PET_MIN_IDLE_MS = 3_000L
const val PET_MAX_IDLE_MS = 9_000L

// ----- walking -----

// planted paws in the walk frames slide back about 0.75 dp per frame with the bear drawn at
// 78dp, so the bear moves as much and its feet do not skate
const val PET_STEP_DP = 0.75f
const val PET_START_X_DP = 24f

// ----- finding the bear -----

// three knocks on the ledge, each within half a second of the last, call the hidden bear
const val PET_KNOCKS = 3
const val PET_KNOCK_GAP_MS = 500L

// after the knocks it waits a moment, then peeks in from the screen edge, showing a little under
// half of itself, holds still, and walks onto the ledge
const val PET_ANSWER_MS = 600L
const val PET_PEEK_MS = 1_200L
const val PET_PEEK_SHARE = 0.45f

// ----- sending the bear away -----

// a long press sends the bear off by the nearer screen edge at twice its walking pace, two walk
// frames a tick so its feet keep up; it gets up first if it was sitting
const val PET_LEAVE_STEPS = 2

// ----- paw print hint -----

// while the bear is hidden, now and then a trail of faint paw prints crosses the ledge: a print
// every 16dp, each one 350 ms after the last, fading in quickly and out over 1.8 s; the first
// trail comes 6 to 12 s after the shelf shows, the next ones 30 to 60 s apart
const val PAW_GAP_DP = 16f
const val PAW_STEP_MS = 350L
const val PAW_RISE_MS = 120L
const val PAW_FADE_MS = 1_800L
const val PAW_FIRST_MIN_MS = 6_000L
const val PAW_FIRST_MAX_MS = 12_000L
const val PAW_NEXT_MIN_MS = 30_000L
const val PAW_NEXT_MAX_MS = 60_000L

private const val WALK_FRAMES = 8
private const val SIT_FRAMES = 3

// sinking, then the sit frames played backward: how long getting up takes
private const val STAND_UP_MS = PET_CROUCH_MS + SIT_FRAMES * PET_SIT_FRAME_MS

/**
 * What the shelf bear is doing. [LEAVE] walks it off the screen after a long press, and once it
 * is out of sight it is [GONE].
 */
enum class PetMode { IDLE, STAND_UP, WALK, SIT_DOWN, READ, ENTER, LEAVE, GONE }

/**
 * One drawing of the bear, facing right. The screen maps each one to its bitmap and mirrors
 * it when the bear faces left.
 */
enum class PetFrame {
    IDLE_1, IDLE_2, IDLE_3, IDLE_4, IDLE_5, IDLE_6,
    SIT_1, SIT_2, SIT_3,
    WALK_1, WALK_2, WALK_3, WALK_4, WALK_5, WALK_6, WALK_7, WALK_8,
    READ_1, READ_2, READ_3, READ_4, READ_5, READ_6, READ_7, READ_8, READ_9, READ_10,
}

// sitting looks around, blinks and twitches an ear; reading blinks, reaches for the page, grasps,
// lifts and turns it, lets it fall over and settles the new page; both loops run one way, the
// last frame leading back into the first
private val IDLE = listOf(
    PetFrame.IDLE_1, PetFrame.IDLE_2, PetFrame.IDLE_3,
    PetFrame.IDLE_4, PetFrame.IDLE_5, PetFrame.IDLE_6,
)
private val SIT = listOf(PetFrame.SIT_1, PetFrame.SIT_2, PetFrame.SIT_3)
private val WALK = listOf(
    PetFrame.WALK_1, PetFrame.WALK_2, PetFrame.WALK_3, PetFrame.WALK_4,
    PetFrame.WALK_5, PetFrame.WALK_6, PetFrame.WALK_7, PetFrame.WALK_8,
)
private val READ = listOf(
    PetFrame.READ_1, PetFrame.READ_2, PetFrame.READ_3, PetFrame.READ_4, PetFrame.READ_5,
    PetFrame.READ_6, PetFrame.READ_7, PetFrame.READ_8, PetFrame.READ_9, PetFrame.READ_10,
)

/**
 * A random rest between walks, three to nine seconds.
 */
fun nextIdleMs(random: Random): Long = random.nextLong(PET_MIN_IDLE_MS, PET_MAX_IDLE_MS + 1)

/**
 * How many knocks on the ledge in a row there are after one at [nowMs], given [count] knocks so
 * far with the last at [lastMs]: one more when it follows within [PET_KNOCK_GAP_MS], otherwise
 * a new run of one.
 */
fun knocksInRow(count: Int, lastMs: Long, nowMs: Long): Int =
    if (count > 0 && nowMs - lastMs in 0..PET_KNOCK_GAP_MS) count + 1 else 1

/**
 * How many paw prints it takes to cross a ledge [ledgeWidth] dp wide.
 */
fun pawCount(ledgeWidth: Float): Int = ceil(ledgeWidth / PAW_GAP_DP).toInt().coerceAtLeast(1)

/**
 * How long a trail of [count] paw prints lasts, until its last print has faded out.
 */
fun pawTrailMs(count: Int): Long = (count - 1) * PAW_STEP_MS + PAW_RISE_MS + PAW_FADE_MS

/**
 * How strongly paw print [index] of a trail shows [elapsedMs] into it, from 0 to 1: nothing
 * before its turn, then a quick fade in and a slow fade out.
 */
fun pawAlpha(elapsedMs: Float, index: Int): Float {
    val since = elapsedMs - index * PAW_STEP_MS
    return when {
        since <= 0f -> 0f
        since < PAW_RISE_MS -> since / PAW_RISE_MS
        else -> (1f - (since - PAW_RISE_MS) / PAW_FADE_MS).coerceAtLeast(0f)
    }
}

/**
 * The bear answering a knock: about to peek in from the left edge when [fromLeft], else from the
 * right edge of a ledge [ledgeWidth] dp wide, with the bear [petSize] dp wide, facing in.
 */
fun enteringPet(
    fromLeft: Boolean,
    ledgeWidth: Float,
    petSize: Float,
): ShelfPetState = ShelfPetState(
    mode = PetMode.ENTER,
    x = if (fromLeft) -petSize * (1f - PET_PEEK_SHARE) else ledgeWidth - petSize * PET_PEEK_SHARE,
    facingRight = fromLeft,
)

/**
 * The shelf bear at one moment. It sits ([PetMode.IDLE]) for [idleForMs], stands up, walks to
 * the far end of the ledge and sits down again. A tap makes it sit and read for a few seconds.
 * Once found it first enters ([PetMode.ENTER]): it peeks in from the screen edge and walks onto
 * the ledge. A long press sends it off again ([PetMode.LEAVE]) until it is [PetMode.GONE].
 * [x] is the bear's left edge in dp from the start of the ledge, [elapsedMs] the time spent in
 * the current mode, [step] the walk frame, [readAfterSitting] remembers a tap that came while
 * it was on its feet, [rising] marks a seated pose that has just followed sitting down, and
 * [leaveToX] is where the left edge has to reach for a leaving bear to be out of sight.
 */
data class ShelfPetState(
    val mode: PetMode = PetMode.IDLE,
    val x: Float = PET_START_X_DP,
    val facingRight: Boolean = true,
    val elapsedMs: Long = 0L,
    val step: Int = 0,
    val readAfterSitting: Boolean = false,
    val idleForMs: Long = PET_MIN_IDLE_MS,
    val rising: Boolean = false,
    val leaveToX: Float = 0f,
) {

    /**
     * The state one tick ([tickMs]) later, on a ledge where the bear's left edge may go from
     * [minX] to [maxX]. [random] picks the next rest. With [animate] off, as when the system
     * removes animations, the bear never gets up and one that is on its feet sits at once; a
     * tap still makes it read.
     */
    fun tick(minX: Float, maxX: Float, random: Random, animate: Boolean = true): ShelfPetState {
        val high = maxOf(minX, maxX)
        val here = x.coerceIn(minX, high)
        val time = elapsedMs + tickMs
        if (mode == PetMode.GONE) return this
        // with animations off a bear sent away is gone at once
        if (mode == PetMode.LEAVE && !animate) return copy(mode = PetMode.GONE)
        if (!animate && mode != PetMode.IDLE && mode != PetMode.READ) {
            return if (readAfterSitting) {
                copy(
                    mode = PetMode.READ,
                    x = here,
                    elapsedMs = 0L,
                    readAfterSitting = false,
                    rising = false,
                )
            } else {
                rest(here, random, rising = false)
            }
        }
        return when (mode) {
            PetMode.IDLE -> if (animate && time >= idleForMs) {
                // head for the end that is farther away
                copy(
                    mode = PetMode.STAND_UP,
                    x = here,
                    facingRight = here <= (minX + high) / 2,
                    elapsedMs = 0L,
                    rising = false,
                )
            } else {
                copy(x = here, elapsedMs = time)
            }
            PetMode.STAND_UP -> if (time >= PET_CROUCH_MS + SIT_FRAMES * PET_SIT_FRAME_MS) {
                copy(mode = PetMode.WALK, x = here, elapsedMs = 0L, step = 0)
            } else {
                copy(x = here, elapsedMs = time)
            }
            PetMode.WALK -> {
                val next = here + if (facingRight) PET_STEP_DP else -PET_STEP_DP
                if (next <= minX || next >= high) {
                    copy(
                        mode = PetMode.SIT_DOWN,
                        x = next.coerceIn(minX, high),
                        elapsedMs = 0L,
                        step = 0,
                    )
                } else {
                    copy(x = next, elapsedMs = time, step = (step + 1) % WALK_FRAMES)
                }
            }
            PetMode.SIT_DOWN -> when {
                time < SIT_FRAMES * PET_SIT_FRAME_MS -> copy(x = here, elapsedMs = time)
                readAfterSitting -> copy(
                    mode = PetMode.READ,
                    x = here,
                    elapsedMs = 0L,
                    readAfterSitting = false,
                    rising = true,
                )
                else -> rest(here, random, rising = true)
            }
            PetMode.ENTER -> if (time < PET_ANSWER_MS + PET_PEEK_MS) {
                copy(elapsedMs = time)
            } else {
                // starts off the ledge, so x is not held to it until the bear arrives
                val next = x + if (facingRight) PET_STEP_DP else -PET_STEP_DP
                val arrived = if (facingRight) next >= minX else next <= high
                if (arrived) {
                    copy(
                        mode = PetMode.SIT_DOWN,
                        x = next.coerceIn(minX, high),
                        elapsedMs = 0L,
                        step = 0,
                    )
                } else {
                    copy(x = next, elapsedMs = time, step = (step + 1) % WALK_FRAMES)
                }
            }
            PetMode.READ -> if (time >= PET_READ_MS) {
                rest(here, random, rising = false)
            } else {
                copy(x = here, elapsedMs = time)
            }
            // off the ledge on purpose, so x is not held to it
            PetMode.LEAVE -> if (elapsedMs < STAND_UP_MS) {
                copy(elapsedMs = time)
            } else {
                val next = x + PET_LEAVE_STEPS * if (facingRight) PET_STEP_DP else -PET_STEP_DP
                val out = if (facingRight) next >= leaveToX else next <= leaveToX
                if (out) {
                    copy(mode = PetMode.GONE, x = next, elapsedMs = 0L)
                } else {
                    val nextStep = (step + PET_LEAVE_STEPS) % WALK_FRAMES
                    copy(x = next, elapsedMs = time, step = nextStep)
                }
            }
            PetMode.GONE -> this
        }
    }

    /**
     * The state after a long press on the bear: it turns to the nearer screen edge of a ledge
     * [ledgeWidth] dp wide and leaves, getting up first unless it is on its feet already; the
     * bear is [petSize] dp wide. A bear already leaving keeps going.
     */
    fun onLongPress(ledgeWidth: Float, petSize: Float): ShelfPetState {
        if (mode == PetMode.LEAVE || mode == PetMode.GONE) return this
        val toRight = x + petSize / 2 > ledgeWidth / 2
        val onFeet = mode == PetMode.WALK || mode == PetMode.ENTER
        return copy(
            mode = PetMode.LEAVE,
            facingRight = toRight,
            elapsedMs = if (onFeet) STAND_UP_MS else 0L,
            readAfterSitting = false,
            rising = false,
            leaveToX = if (toRight) ledgeWidth else -petSize,
        )
    }

    /**
     * The state after a tap on the bear: on its feet it sits down first and then reads, sitting
     * (or only just sinking to stand up) it reads at once, and a tap while it reads or leaves
     * changes nothing. Tapped while it enters, it reads once it has walked in and sat down.
     */
    fun onTap(): ShelfPetState = when {
        mode == PetMode.LEAVE || mode == PetMode.GONE -> this
        mode == PetMode.STAND_UP && elapsedMs < PET_CROUCH_MS -> copy(
            mode = PetMode.READ,
            elapsedMs = 0L,
            rising = false,
        )
        mode == PetMode.WALK || mode == PetMode.STAND_UP -> copy(
            mode = PetMode.SIT_DOWN,
            elapsedMs = 0L,
            step = 0,
            readAfterSitting = true,
        )
        mode == PetMode.SIT_DOWN || mode == PetMode.ENTER -> copy(readAfterSitting = true)
        mode == PetMode.IDLE -> copy(mode = PetMode.READ, elapsedMs = 0L, rising = false)
        else -> this
    }

    /**
     * How tall to draw the bear now, as a share of its full height, measured from the ground
     * line: under 1 while a seated pose grows after sitting down or sinks before standing up,
     * otherwise 1. With [animate] off it is always 1.
     */
    fun height(animate: Boolean = true): Float {
        if (!animate) return 1f
        val low = PET_SIT_DOWN_HEIGHT
        return when {
            rising && (mode == PetMode.IDLE || mode == PetMode.READ) && elapsedMs < PET_RISE_MS ->
                low + (1f - low) * elapsedMs / PET_RISE_MS
            (mode == PetMode.STAND_UP || mode == PetMode.LEAVE) && elapsedMs < PET_CROUCH_MS ->
                1f - (1f - low) * elapsedMs / PET_CROUCH_MS
            else -> 1f
        }
    }

    /**
     * Whether the bear is taking steps now: walking, walking in after its peek, or walking off
     * once it has got up.
     */
    val onFeet: Boolean
        get() = when (mode) {
            PetMode.WALK -> true
            PetMode.ENTER -> elapsedMs >= PET_ANSWER_MS + PET_PEEK_MS
            PetMode.LEAVE -> elapsedMs >= STAND_UP_MS
            else -> false
        }

    /**
     * How long the next tick lasts: [PET_WALK_TICK_MS] while the bear is [onFeet], else
     * [PET_TICK_MS]. The screen waits this long between ticks, and [elapsedMs] grows by it.
     */
    val tickMs: Long
        get() = if (onFeet) PET_WALK_TICK_MS else PET_TICK_MS

    /**
     * Whether the bear is drawn: not while it waits to answer a knock, nor once it is gone.
     */
    val shown: Boolean
        get() = mode != PetMode.GONE && !(mode == PetMode.ENTER && elapsedMs < PET_ANSWER_MS)

    /**
     * The drawing to show now. With [animate] off the bear holds the first idle or reading
     * frame.
     */
    fun frame(animate: Boolean = true): PetFrame {
        val sitIndex = (elapsedMs / PET_SIT_FRAME_MS).toInt().coerceAtMost(SIT_FRAMES - 1)
        val standIndex = ((elapsedMs - PET_CROUCH_MS) / PET_SIT_FRAME_MS).toInt()
            .coerceIn(0, SIT_FRAMES - 1)
        return when (mode) {
            PetMode.IDLE -> if (animate) IDLE[loop(IDLE.size, PET_IDLE_FRAME_MS)] else IDLE.first()
            PetMode.STAND_UP -> if (elapsedMs < PET_CROUCH_MS) {
                IDLE.first()
            } else {
                SIT[SIT_FRAMES - 1 - standIndex]
            }
            // leaving gets up the same way, then walks
            PetMode.LEAVE -> when {
                elapsedMs >= STAND_UP_MS -> WALK[step % WALK_FRAMES]
                elapsedMs < PET_CROUCH_MS -> IDLE.first()
                else -> SIT[SIT_FRAMES - 1 - standIndex]
            }
            // entering holds the first walk frame while it peeks; a gone bear is not drawn
            PetMode.WALK, PetMode.ENTER, PetMode.GONE -> WALK[step % WALK_FRAMES]
            PetMode.SIT_DOWN -> SIT[sitIndex]
            PetMode.READ -> if (animate) READ[loop(READ.size, PET_READ_FRAME_MS)] else READ.first()
        }
    }

    private fun loop(frames: Int, frameMs: Long): Int = ((elapsedMs / frameMs) % frames).toInt()

    private fun rest(here: Float, random: Random, rising: Boolean): ShelfPetState = copy(
        mode = PetMode.IDLE,
        x = here,
        elapsedMs = 0L,
        step = 0,
        idleForMs = nextIdleMs(random),
        rising = rising,
    )
}
