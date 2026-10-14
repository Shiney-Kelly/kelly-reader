package com.kelly.reader.ui.shelf

import kotlin.random.Random

// ----- timing -----

const val PET_TICK_MS = 125L
const val PET_SIT_FRAME_MS = 160L
const val PET_IDLE_FRAME_MS = 500L
const val PET_READ_FRAME_MS = 450L

// reading plays its eight frames twice
const val PET_READ_MS = 2 * 8 * PET_READ_FRAME_MS

// the last sit-down frame is 93% as tall as the seated ones (its head 168 against 181 of 208 px
// above the paw line), so a seated pose that follows it grows from that height over a few
// ticks, and before standing up the seated pose sinks to it
const val PET_SIT_DOWN_HEIGHT = 0.93f
const val PET_RISE_MS = 3 * PET_TICK_MS
const val PET_CROUCH_MS = 2 * PET_TICK_MS
const val PET_MIN_IDLE_MS = 3 * 60_000L
const val PET_MAX_IDLE_MS = 5 * 60_000L

// ----- walking -----

// planted paws in the walk frames slide back about half a dp per frame, so the bear moves as
// much and its feet do not skate
const val PET_STEP_DP = 0.5f
const val PET_START_X_DP = 24f

private const val WALK_FRAMES = 8
private const val SIT_FRAMES = 3

/**
 * What the shelf bear is doing.
 */
enum class PetMode { IDLE, STAND_UP, WALK, SIT_DOWN, READ }

/**
 * One drawing of the bear, facing right. The screen maps each one to its bitmap and mirrors
 * it when the bear faces left.
 */
enum class PetFrame {
    IDLE_1, IDLE_2, IDLE_3, IDLE_4, IDLE_5, IDLE_6,
    SIT_1, SIT_2, SIT_3,
    WALK_1, WALK_2, WALK_3, WALK_4, WALK_5, WALK_6, WALK_7, WALK_8,
    READ_1, READ_2, READ_3, READ_4, READ_5, READ_6, READ_7, READ_8,
}

// sitting looks around, blinks and twitches an ear; reading blinks, reaches for the page, grasps
// and turns it and settles the new page; both loops run one way, the last frame leading back into
// the first
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
    PetFrame.READ_1, PetFrame.READ_2, PetFrame.READ_3, PetFrame.READ_4,
    PetFrame.READ_5, PetFrame.READ_6, PetFrame.READ_7, PetFrame.READ_8,
)

/**
 * A random rest between walks, three to five minutes.
 */
fun nextIdleMs(random: Random): Long = random.nextLong(PET_MIN_IDLE_MS, PET_MAX_IDLE_MS + 1)

/**
 * The shelf bear at one moment. It sits ([PetMode.IDLE]) for [idleForMs], stands up, walks to
 * the far end of the ledge and sits down again. A tap makes it sit and read for a few seconds.
 * [x] is the bear's left edge in dp from the start of the ledge, [elapsedMs] the time spent in
 * the current mode, [step] the walk frame, [readAfterSitting] remembers a tap that came while
 * it was on its feet, and [rising] marks a seated pose that has just followed sitting down.
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
) {

    /**
     * The state one [PET_TICK_MS] later, on a ledge where the bear's left edge may go from
     * [minX] to [maxX]. [random] picks the next rest. With [animate] off, as when the system
     * removes animations, the bear never gets up and one that is on its feet sits at once; a
     * tap still makes it read.
     */
    fun tick(minX: Float, maxX: Float, random: Random, animate: Boolean = true): ShelfPetState {
        val high = maxOf(minX, maxX)
        val here = x.coerceIn(minX, high)
        val time = elapsedMs + PET_TICK_MS
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
            PetMode.READ -> if (time >= PET_READ_MS) {
                rest(here, random, rising = false)
            } else {
                copy(x = here, elapsedMs = time)
            }
        }
    }

    /**
     * The state after a tap on the bear: on its feet it sits down first and then reads, sitting
     * (or only just sinking to stand up) it reads at once, and a tap while it reads changes
     * nothing.
     */
    fun onTap(): ShelfPetState = when {
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
        mode == PetMode.SIT_DOWN -> copy(readAfterSitting = true)
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
            mode == PetMode.STAND_UP && elapsedMs < PET_CROUCH_MS ->
                1f - (1f - low) * elapsedMs / PET_CROUCH_MS
            else -> 1f
        }
    }

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
            PetMode.WALK -> WALK[step % WALK_FRAMES]
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
