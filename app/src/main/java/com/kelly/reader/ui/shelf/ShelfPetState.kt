package com.kelly.reader.ui.shelf

import kotlin.random.Random

// ----- timing -----

const val PET_TICK_MS = 125L
const val PET_SIT_FRAME_MS = 160L
const val PET_IDLE_FRAME_MS = 450L
const val PET_READ_FRAME_MS = 600L
const val PET_READ_MS = 7_000L
const val PET_MIN_IDLE_MS = 3 * 60_000L
const val PET_MAX_IDLE_MS = 5 * 60_000L

// ----- walking -----

// planted paws in the walk frames slide back about half a dp per frame, so the bear moves as
// much and its feet do not skate
const val PET_STEP_DP = 0.5f
const val PET_START_X_DP = 24f

private const val WALK_FRAMES = 8
private const val SIT_FRAMES = 3

// the idle and reading loops play their three frames 1 2 3 2, so the end flows into the start
private val PING_PONG = intArrayOf(0, 1, 2, 1)

/**
 * What the shelf bear is doing.
 */
enum class PetMode { IDLE, STAND_UP, WALK, SIT_DOWN, READ }

/**
 * One drawing of the bear, facing right. The screen maps each one to its bitmap and mirrors
 * it when the bear faces left.
 */
enum class PetFrame {
    IDLE_1, IDLE_2, IDLE_3,
    SIT_1, SIT_2, SIT_3,
    WALK_1, WALK_2, WALK_3, WALK_4, WALK_5, WALK_6, WALK_7, WALK_8,
    READ_1, READ_2, READ_3,
}

private val IDLE = listOf(PetFrame.IDLE_1, PetFrame.IDLE_2, PetFrame.IDLE_3)
private val SIT = listOf(PetFrame.SIT_1, PetFrame.SIT_2, PetFrame.SIT_3)
private val WALK = listOf(
    PetFrame.WALK_1, PetFrame.WALK_2, PetFrame.WALK_3, PetFrame.WALK_4,
    PetFrame.WALK_5, PetFrame.WALK_6, PetFrame.WALK_7, PetFrame.WALK_8,
)
private val READ = listOf(PetFrame.READ_1, PetFrame.READ_2, PetFrame.READ_3)

/**
 * A random rest between walks, three to five minutes.
 */
fun nextIdleMs(random: Random): Long = random.nextLong(PET_MIN_IDLE_MS, PET_MAX_IDLE_MS + 1)

/**
 * The shelf bear at one moment. It sits ([PetMode.IDLE]) for [idleForMs], stands up, walks to
 * the far end of the ledge and sits down again. A tap makes it sit and read for a few seconds.
 * [x] is the bear's left edge in dp from the start of the ledge, [elapsedMs] the time spent in
 * the current mode, [step] the walk frame, and [readAfterSitting] remembers a tap that came
 * while it was on its feet.
 */
data class ShelfPetState(
    val mode: PetMode = PetMode.IDLE,
    val x: Float = PET_START_X_DP,
    val facingRight: Boolean = true,
    val elapsedMs: Long = 0L,
    val step: Int = 0,
    val readAfterSitting: Boolean = false,
    val idleForMs: Long = PET_MIN_IDLE_MS,
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
                copy(mode = PetMode.READ, x = here, elapsedMs = 0L, readAfterSitting = false)
            } else {
                rest(here, random)
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
                )
            } else {
                copy(x = here, elapsedMs = time)
            }
            PetMode.STAND_UP -> if (time >= SIT_FRAMES * PET_SIT_FRAME_MS) {
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
                )
                else -> rest(here, random)
            }
            PetMode.READ -> if (time >= PET_READ_MS) {
                rest(here, random)
            } else {
                copy(x = here, elapsedMs = time)
            }
        }
    }

    /**
     * The state after a tap on the bear: on its feet it sits down first and then reads, sitting
     * it reads at once, and a tap while it reads changes nothing.
     */
    fun onTap(): ShelfPetState = when (mode) {
        PetMode.WALK, PetMode.STAND_UP -> copy(
            mode = PetMode.SIT_DOWN,
            elapsedMs = 0L,
            step = 0,
            readAfterSitting = true,
        )
        PetMode.SIT_DOWN -> copy(readAfterSitting = true)
        PetMode.IDLE -> copy(mode = PetMode.READ, elapsedMs = 0L)
        PetMode.READ -> this
    }

    /**
     * The drawing to show now. With [animate] off the bear holds the first idle or reading
     * frame.
     */
    fun frame(animate: Boolean = true): PetFrame {
        val sitIndex = (elapsedMs / PET_SIT_FRAME_MS).toInt().coerceAtMost(SIT_FRAMES - 1)
        return when (mode) {
            PetMode.IDLE -> if (animate) IDLE[pingPong(PET_IDLE_FRAME_MS)] else IDLE.first()
            PetMode.STAND_UP -> SIT[SIT_FRAMES - 1 - sitIndex]
            PetMode.WALK -> WALK[step % WALK_FRAMES]
            PetMode.SIT_DOWN -> SIT[sitIndex]
            PetMode.READ -> if (animate) READ[pingPong(PET_READ_FRAME_MS)] else READ.first()
        }
    }

    private fun pingPong(frameMs: Long): Int =
        PING_PONG[((elapsedMs / frameMs) % PING_PONG.size).toInt()]

    private fun rest(here: Float, random: Random): ShelfPetState = copy(
        mode = PetMode.IDLE,
        x = here,
        elapsedMs = 0L,
        step = 0,
        idleForMs = nextIdleMs(random),
    )
}
