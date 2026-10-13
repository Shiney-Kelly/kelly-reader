package com.kelly.reader.ui.shelf

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private const val MIN_X = 8f
private const val MAX_X = 330f

/**
 * Checks the shelf bear's rest, walk, sit and read cycle, its taps and its frames.
 */
class ShelfPetStateTest {

    private val random = Random(7)

    private fun ShelfPetState.ticks(count: Int, animate: Boolean = true): ShelfPetState {
        var state = this
        repeat(count) { state = state.tick(MIN_X, MAX_X, random, animate) }
        return state
    }

    private fun ticksFor(ms: Long): Int = ((ms + PET_TICK_MS - 1) / PET_TICK_MS).toInt()

    @Test
    fun tick_restNotOver_staysSitting() {
        val state = ShelfPetState(idleForMs = PET_MIN_IDLE_MS).ticks(ticksFor(PET_MIN_IDLE_MS) - 1)
        assertEquals(PetMode.IDLE, state.mode)
        assertEquals(PET_START_X_DP, state.x)
    }

    @Test
    fun tick_restOver_standsUpFacingTheFartherEnd() {
        val rest = ticksFor(PET_MIN_IDLE_MS)
        val left = ShelfPetState(x = 24f, idleForMs = PET_MIN_IDLE_MS).ticks(rest)
        assertEquals(PetMode.STAND_UP, left.mode)
        assertTrue(left.facingRight)
        val right = ShelfPetState(x = 300f, idleForMs = PET_MIN_IDLE_MS).ticks(rest)
        assertFalse(right.facingRight)
    }

    @Test
    fun tick_standingUp_playsSitFramesBackwardThenWalks() {
        val start = ShelfPetState(mode = PetMode.STAND_UP)
        assertEquals(PetFrame.SIT_3, start.frame())
        assertEquals(PetFrame.SIT_2, start.copy(elapsedMs = PET_SIT_FRAME_MS).frame())
        assertEquals(PetFrame.SIT_1, start.copy(elapsedMs = 2 * PET_SIT_FRAME_MS).frame())
        assertEquals(PetMode.STAND_UP, start.ticks(3).mode)
        assertEquals(PetMode.WALK, start.ticks(4).mode)
    }

    @Test
    fun tick_walking_movesHalfDpAndLoopsEightFrames() {
        var state = ShelfPetState(mode = PetMode.WALK, x = 100f)
        val frames = mutableListOf(state.frame())
        repeat(8) {
            state = state.ticks(1)
            frames += state.frame()
        }
        assertEquals(100f + 8 * PET_STEP_DP, state.x)
        val walk = listOf(
            PetFrame.WALK_1, PetFrame.WALK_2, PetFrame.WALK_3, PetFrame.WALK_4,
            PetFrame.WALK_5, PetFrame.WALK_6, PetFrame.WALK_7, PetFrame.WALK_8, PetFrame.WALK_1,
        )
        assertEquals(walk, frames)
    }

    @Test
    fun tick_walkingLeft_movesTowardTheStart() {
        val state = ShelfPetState(mode = PetMode.WALK, x = 100f, facingRight = false).ticks(4)
        assertEquals(100f - 4 * PET_STEP_DP, state.x)
        assertFalse(state.facingRight)
    }

    @Test
    fun tick_walkReachesTheEnd_sitsDownThenRests() {
        val atEnd = ShelfPetState(mode = PetMode.WALK, x = MAX_X - 0.25f).ticks(1)
        assertEquals(PetMode.SIT_DOWN, atEnd.mode)
        assertEquals(MAX_X, atEnd.x)
        assertEquals(PetFrame.SIT_1, atEnd.frame())
        val seated = atEnd.ticks(4)
        assertEquals(PetMode.IDLE, seated.mode)
        assertTrue(seated.idleForMs in PET_MIN_IDLE_MS..PET_MAX_IDLE_MS)
        assertTrue(seated.facingRight)
    }

    @Test
    fun onTap_whileWalking_sitsDownThenReadsThenRests() {
        val tapped = ShelfPetState(mode = PetMode.WALK, x = 100f, step = 5).onTap()
        assertEquals(PetMode.SIT_DOWN, tapped.mode)
        assertEquals(PetMode.READ, tapped.ticks(4).mode)
        assertEquals(PetMode.READ, tapped.ticks(4 + ticksFor(PET_READ_MS) - 1).mode)
        assertEquals(PetMode.IDLE, tapped.ticks(4 + ticksFor(PET_READ_MS)).mode)
    }

    @Test
    fun onTap_whileStandingUp_sitsDownThenReads() {
        val tapped = ShelfPetState(mode = PetMode.STAND_UP, elapsedMs = 250L).onTap()
        assertEquals(PetMode.SIT_DOWN, tapped.mode)
        assertEquals(PetMode.READ, tapped.ticks(4).mode)
    }

    @Test
    fun onTap_whileSittingDown_readsOnceSeated() {
        val tapped = ShelfPetState(mode = PetMode.SIT_DOWN, elapsedMs = 250L).onTap()
        assertEquals(PetMode.SIT_DOWN, tapped.mode)
        assertEquals(PetMode.READ, tapped.ticks(2).mode)
    }

    @Test
    fun onTap_sitting_readsAtOnce() {
        val tapped = ShelfPetState(elapsedMs = 60_000L).onTap()
        assertEquals(PetMode.READ, tapped.mode)
        assertEquals(0L, tapped.elapsedMs)
    }

    @Test
    fun onTap_whileReading_changesNothing() {
        val reading = ShelfPetState(mode = PetMode.READ, elapsedMs = 1_000L)
        assertEquals(reading, reading.onTap())
    }

    @Test
    fun frame_sitting_loopsOneTwoThreeTwo() {
        val frames = (0..4).map { ShelfPetState(elapsedMs = it * PET_IDLE_FRAME_MS).frame() }
        val loop = listOf(
            PetFrame.IDLE_1, PetFrame.IDLE_2, PetFrame.IDLE_3, PetFrame.IDLE_2, PetFrame.IDLE_1,
        )
        assertEquals(loop, frames)
    }

    @Test
    fun frame_reading_loopsOneTwoThreeTwo() {
        val reading = ShelfPetState(mode = PetMode.READ)
        val frames = (0..4).map { reading.copy(elapsedMs = it * PET_READ_FRAME_MS).frame() }
        val loop = listOf(
            PetFrame.READ_1, PetFrame.READ_2, PetFrame.READ_3, PetFrame.READ_2, PetFrame.READ_1,
        )
        assertEquals(loop, frames)
    }

    @Test
    fun tick_animationsOff_neverStandsUpAndHoldsOneFrame() {
        val sitting = ShelfPetState(idleForMs = PET_MIN_IDLE_MS)
            .ticks(ticksFor(PET_MAX_IDLE_MS) * 2, animate = false)
        assertEquals(PetMode.IDLE, sitting.mode)
        assertEquals(PetFrame.IDLE_1, sitting.frame(animate = false))
        val reading = sitting.onTap().ticks(10, animate = false)
        assertEquals(PetFrame.READ_1, reading.frame(animate = false))
        assertEquals(PetMode.IDLE, reading.ticks(ticksFor(PET_READ_MS), animate = false).mode)
    }

    @Test
    fun tick_ledgeNarrowerThanTheBear_keepsItInside() {
        val outside = ShelfPetState(x = 500f).tick(MIN_X, MAX_X, random)
        assertEquals(MAX_X, outside.x)
        val tiny = ShelfPetState(x = 20f).tick(MIN_X, MIN_X - 10f, random)
        assertEquals(MIN_X, tiny.x)
    }

    @Test
    fun nextIdleMs_anySeed_isThreeToFiveMinutes() {
        repeat(50) { seed ->
            assertTrue(nextIdleMs(Random(seed)) in PET_MIN_IDLE_MS..PET_MAX_IDLE_MS)
        }
    }
}
