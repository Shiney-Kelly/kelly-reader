package com.kelly.reader.ui.shelf

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private const val MIN_X = 8f
private const val MAX_X = 330f

/**
 * Checks the shelf bear's rest, walk, sit and read cycle, its taps, its frames, and how it
 * answers knocks on the ledge.
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
    fun tick_standingUp_sinksThenPlaysSitFramesBackwardThenWalks() {
        val start = ShelfPetState(mode = PetMode.STAND_UP)
        assertEquals(PetFrame.IDLE_1, start.frame())
        assertEquals(1f, start.height(), 0.001f)
        val sinking = start.copy(elapsedMs = PET_TICK_MS)
        assertEquals(PetFrame.IDLE_1, sinking.frame())
        assertEquals(1f - (1f - PET_SIT_DOWN_HEIGHT) / 2, sinking.height(), 0.001f)
        assertEquals(PetFrame.SIT_3, start.copy(elapsedMs = PET_CROUCH_MS).frame())
        assertEquals(1f, start.copy(elapsedMs = PET_CROUCH_MS).height(), 0.001f)
        val sitFrames = (1..2).map {
            start.copy(elapsedMs = PET_CROUCH_MS + it * PET_SIT_FRAME_MS).frame()
        }
        assertEquals(listOf(PetFrame.SIT_2, PetFrame.SIT_1), sitFrames)
        assertEquals(PetMode.STAND_UP, start.ticks(5).mode)
        assertEquals(PetMode.WALK, start.ticks(6).mode)
    }

    @Test
    fun tick_sittingDownEnds_seatedPoseGrowsFromSitDownHeight() {
        val seated = ShelfPetState(mode = PetMode.SIT_DOWN, x = 100f).ticks(4)
        assertEquals(PetMode.IDLE, seated.mode)
        assertTrue(seated.rising)
        assertEquals(PET_SIT_DOWN_HEIGHT, seated.height(), 0.001f)
        val heights = (1..3).map { seated.ticks(it).height() }
        assertTrue(heights[0] > PET_SIT_DOWN_HEIGHT && heights[1] > heights[0])
        assertEquals(1f, heights[2], 0.001f)
        assertEquals(1f, seated.ticks(20).height(), 0.001f)
    }

    @Test
    fun tick_tappedWhileWalking_readingPoseGrowsToo() {
        val reading = ShelfPetState(mode = PetMode.WALK, x = 100f).onTap().ticks(4)
        assertEquals(PetMode.READ, reading.mode)
        assertEquals(PET_SIT_DOWN_HEIGHT, reading.height(), 0.001f)
        assertEquals(1f, reading.ticks(3).height(), 0.001f)
    }

    @Test
    fun height_seatedFromTheStartOrAfterReading_isFull() {
        assertEquals(1f, ShelfPetState().height(), 0.001f)
        val afterReading = ShelfPetState(mode = PetMode.READ).ticks(ticksFor(PET_READ_MS))
        assertEquals(PetMode.IDLE, afterReading.mode)
        assertEquals(1f, afterReading.height(), 0.001f)
    }

    @Test
    fun onTap_whileSinkingToStandUp_readsAtOnce() {
        val tapped = ShelfPetState(mode = PetMode.STAND_UP, elapsedMs = PET_TICK_MS).onTap()
        assertEquals(PetMode.READ, tapped.mode)
        assertEquals(1f, tapped.height(), 0.001f)
    }

    @Test
    fun height_animationsOff_isAlwaysFull() {
        val seated = ShelfPetState(mode = PetMode.SIT_DOWN, x = 100f).ticks(4)
        assertEquals(1f, seated.height(animate = false), 0.001f)
    }

    @Test
    fun tick_walking_movesOneStepAFrameAndLoopsEightFrames() {
        var state = ShelfPetState(mode = PetMode.WALK, x = 100f)
        val frames = mutableListOf(state.frame())
        repeat(8) {
            state = state.ticks(1)
            frames += state.frame()
        }
        assertEquals(100f + 8 * PET_STEP_DP, state.x, 0.001f)
        val walk = listOf(
            PetFrame.WALK_1, PetFrame.WALK_2, PetFrame.WALK_3, PetFrame.WALK_4,
            PetFrame.WALK_5, PetFrame.WALK_6, PetFrame.WALK_7, PetFrame.WALK_8, PetFrame.WALK_1,
        )
        assertEquals(walk, frames)
    }

    @Test
    fun tick_walkingLeft_movesTowardTheStart() {
        val state = ShelfPetState(mode = PetMode.WALK, x = 100f, facingRight = false).ticks(4)
        assertEquals(100f - 4 * PET_STEP_DP, state.x, 0.001f)
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
    fun onFeet_onlyWhileTakingSteps() {
        assertTrue(ShelfPetState(mode = PetMode.WALK).onFeet)
        assertFalse(ShelfPetState(mode = PetMode.IDLE).onFeet)
        assertFalse(ShelfPetState(mode = PetMode.READ).onFeet)
        assertFalse(ShelfPetState(mode = PetMode.STAND_UP).onFeet)
        // entering: not while it waits and peeks, then yes
        val peeking = ShelfPetState(mode = PetMode.ENTER, elapsedMs = PET_ANSWER_MS)
        assertFalse(peeking.onFeet)
        assertTrue(peeking.copy(elapsedMs = PET_ANSWER_MS + PET_PEEK_MS).onFeet)
        // leaving: not while it gets up, then yes
        val leaving = ShelfPetState(x = 24f).onLongPress(ledgeWidth = 400f, petSize = 78f)
        assertFalse(leaving.onFeet)
        assertTrue(leaving.ticks(ticksFor(PET_CROUCH_MS + 3 * PET_SIT_FRAME_MS)).onFeet)
    }

    @Test
    fun onLongPress_sittingOnTheLeft_getsUpWalksOffLeftAndIsGone() {
        val leaving = ShelfPetState(x = 24f).onLongPress(ledgeWidth = 400f, petSize = 78f)
        assertEquals(PetMode.LEAVE, leaving.mode)
        assertFalse(leaving.facingRight)
        assertEquals(-78f, leaving.leaveToX)
        // gets up first: sinks, then the sit frames backward, the bear staying where it was
        val standing = leaving.ticks(2)
        assertEquals(24f, standing.x)
        assertEquals(PetFrame.SIT_3, standing.frame())
        // then two steps a tick, off the ledge on the left: 24 to -78 dp is 68 ticks
        val standTicks = ticksFor(PET_CROUCH_MS + 3 * PET_SIT_FRAME_MS)
        val walking = leaving.ticks(standTicks + 10)
        assertEquals(PetMode.LEAVE, walking.mode)
        assertTrue(walking.x < 24f)
        assertTrue(walking.shown)
        val gone = leaving.ticks(standTicks + 68)
        assertEquals(PetMode.GONE, gone.mode)
        assertFalse(gone.shown)
        assertEquals(gone, gone.ticks(5))
    }

    @Test
    fun onLongPress_walkingOnTheRight_leavesRightWithoutStopping() {
        val leaving = ShelfPetState(mode = PetMode.WALK, x = 300f, facingRight = false, step = 3)
            .onLongPress(ledgeWidth = 400f, petSize = 78f)
        assertTrue(leaving.facingRight)
        assertEquals(400f, leaving.leaveToX)
        val next = leaving.ticks(1)
        assertEquals(300f + 2 * PET_STEP_DP, next.x)
        assertEquals(5, next.step)
    }

    @Test
    fun onLongPress_animationsOff_goneAtOnce() {
        val leaving = ShelfPetState(x = 24f).onLongPress(ledgeWidth = 400f, petSize = 78f)
        assertEquals(PetMode.GONE, leaving.ticks(1, animate = false).mode)
    }

    @Test
    fun onTapAndLongPress_whileLeaving_changeNothing() {
        val leaving = ShelfPetState(x = 24f).onLongPress(ledgeWidth = 400f, petSize = 78f)
        assertEquals(leaving, leaving.onTap())
        assertEquals(leaving, leaving.onLongPress(ledgeWidth = 400f, petSize = 78f))
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
    fun frame_sitting_loopsOneToSixThenBackToOne() {
        val frames = (0..6).map { ShelfPetState(elapsedMs = it * PET_IDLE_FRAME_MS).frame() }
        val loop = listOf(
            PetFrame.IDLE_1, PetFrame.IDLE_2, PetFrame.IDLE_3, PetFrame.IDLE_4,
            PetFrame.IDLE_5, PetFrame.IDLE_6, PetFrame.IDLE_1,
        )
        assertEquals(loop, frames)
    }

    @Test
    fun frame_reading_loopsOneToTenThenBackToOne() {
        val reading = ShelfPetState(mode = PetMode.READ)
        val frames = (0..10).map { reading.copy(elapsedMs = it * PET_READ_FRAME_MS).frame() }
        val loop = listOf(
            PetFrame.READ_1, PetFrame.READ_2, PetFrame.READ_3, PetFrame.READ_4, PetFrame.READ_5,
            PetFrame.READ_6, PetFrame.READ_7, PetFrame.READ_8, PetFrame.READ_9, PetFrame.READ_10,
            PetFrame.READ_1,
        )
        assertEquals(loop, frames)
    }

    @Test
    fun readMs_isTwoFullLoops() {
        assertEquals(2 * 10 * PET_READ_FRAME_MS, PET_READ_MS)
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
    fun tick_animationsTurnedOffMidWalk_sitsAtOnceWhereItIs() {
        val walking = ShelfPetState(mode = PetMode.WALK, x = 100f, step = 3)
        val seated = walking.tick(MIN_X, MAX_X, random, animate = false)
        assertEquals(PetMode.IDLE, seated.mode)
        assertEquals(100f, seated.x)
        assertEquals(PetFrame.IDLE_1, seated.frame(animate = false))
        val tapped = walking.onTap().tick(MIN_X, MAX_X, random, animate = false)
        assertEquals(PetMode.READ, tapped.mode)
    }

    @Test
    fun tick_ledgeNarrowerThanTheBear_keepsItInside() {
        val outside = ShelfPetState(x = 500f).tick(MIN_X, MAX_X, random)
        assertEquals(MAX_X, outside.x)
        val tiny = ShelfPetState(x = 20f).tick(MIN_X, MIN_X - 10f, random)
        assertEquals(MIN_X, tiny.x)
    }

    @Test
    fun nextIdleMs_anySeed_isFiveToFifteenSeconds() {
        repeat(50) { seed ->
            assertTrue(nextIdleMs(Random(seed)) in PET_MIN_IDLE_MS..PET_MAX_IDLE_MS)
        }
    }

    @Test
    fun knocksInRow_quickKnocks_countUp() {
        assertEquals(1, knocksInRow(count = 0, lastMs = 0L, nowMs = 1_000L))
        assertEquals(2, knocksInRow(count = 1, lastMs = 1_000L, nowMs = 1_300L))
        val third = knocksInRow(count = 2, lastMs = 1_300L, nowMs = 1_300L + PET_KNOCK_GAP_MS)
        assertEquals(PET_KNOCKS, third)
    }

    @Test
    fun knocksInRow_slowKnock_startsOver() {
        assertEquals(1, knocksInRow(count = 2, lastMs = 1_000L, nowMs = 1_001L + PET_KNOCK_GAP_MS))
    }

    @Test
    fun enteringPet_fromEitherSide_startsMostlyOffScreenFacingIn() {
        val left = enteringPet(fromLeft = true, ledgeWidth = 392f, petSize = 78f)
        assertEquals(PetMode.ENTER, left.mode)
        assertTrue(left.facingRight)
        assertEquals(-78f * (1f - PET_PEEK_SHARE), left.x, 0.001f)
        val right = enteringPet(fromLeft = false, ledgeWidth = 392f, petSize = 78f)
        assertFalse(right.facingRight)
        assertEquals(392f - 78f * PET_PEEK_SHARE, right.x, 0.001f)
    }

    @Test
    fun tick_entering_waitsThenPeeksStillThenWalksIn() {
        val start = enteringPet(fromLeft = true, ledgeWidth = 392f, petSize = 78f)
        assertFalse(start.shown)
        val peeking = start.ticks(ticksFor(PET_ANSWER_MS))
        assertTrue(peeking.shown)
        assertEquals(start.x, peeking.x, 0.001f)
        assertEquals(PetFrame.WALK_1, peeking.frame())
        val held = start.ticks(ticksFor(PET_ANSWER_MS + PET_PEEK_MS) - 1)
        assertEquals(start.x, held.x, 0.001f)
        val walking = start.ticks(ticksFor(PET_ANSWER_MS + PET_PEEK_MS))
        assertEquals(PetMode.ENTER, walking.mode)
        assertTrue(walking.x > start.x)
        assertEquals(PetFrame.WALK_2, walking.frame())
    }

    @Test
    fun tick_enteringFromLeft_sitsDownOnceOnTheLedge() {
        var state = enteringPet(fromLeft = true, ledgeWidth = 392f, petSize = 78f)
        while (state.mode == PetMode.ENTER) state = state.tick(MIN_X, MAX_X, random)
        assertEquals(PetMode.SIT_DOWN, state.mode)
        assertEquals(MIN_X, state.x, PET_STEP_DP)
        assertTrue(state.x >= MIN_X)
    }

    @Test
    fun tick_enteringFromRight_walksLeftAndSits() {
        var state = enteringPet(fromLeft = false, ledgeWidth = MAX_X + 86f, petSize = 78f)
        var lastX = state.x
        while (state.mode == PetMode.ENTER) {
            state = state.tick(MIN_X, MAX_X, random)
            assertTrue(state.x <= lastX)
            lastX = state.x
        }
        assertEquals(PetMode.SIT_DOWN, state.mode)
        assertTrue(state.x <= MAX_X)
        assertEquals(MAX_X, state.x, PET_STEP_DP)
    }

    @Test
    fun onTap_entering_readsAfterWalkingInAndSitting() {
        var state = enteringPet(fromLeft = true, ledgeWidth = 392f, petSize = 78f)
            .ticks(ticksFor(PET_ANSWER_MS + PET_PEEK_MS) + 2)
            .onTap()
        assertEquals(PetMode.ENTER, state.mode)
        while (state.mode != PetMode.READ) state = state.tick(MIN_X, MAX_X, random)
        assertEquals(MIN_X, state.x, PET_STEP_DP)
    }

    @Test
    fun tick_enteringWithAnimationsOff_sitsAtTheNearEnd() {
        val state = enteringPet(fromLeft = false, ledgeWidth = 392f, petSize = 78f)
            .ticks(1, animate = false)
        assertEquals(PetMode.IDLE, state.mode)
        assertEquals(MAX_X, state.x, 0.001f)
        assertTrue(state.shown)
    }

    @Test
    fun pawCount_phoneLedge_coversTheWidth() {
        assertEquals(25, pawCount(392f))
        assertTrue(pawCount(392f) * PAW_GAP_DP >= 392f)
        assertEquals(1, pawCount(0f))
    }

    @Test
    fun pawAlpha_overOnePrint_waitsRisesThenFades() {
        assertEquals(0f, pawAlpha(elapsedMs = 2f * PAW_STEP_MS, index = 2), 0.001f)
        val risen = 2f * PAW_STEP_MS + PAW_RISE_MS
        assertEquals(1f, pawAlpha(elapsedMs = risen, index = 2), 0.001f)
        assertEquals(0.5f, pawAlpha(elapsedMs = risen + PAW_FADE_MS / 2f, index = 2), 0.001f)
        assertEquals(0f, pawAlpha(elapsedMs = risen + PAW_FADE_MS, index = 2), 0.001f)
    }

    @Test
    fun pawTrailMs_atTheEnd_lastPrintHasFaded() {
        val count = pawCount(392f)
        val end = pawTrailMs(count).toFloat()
        assertEquals(0f, pawAlpha(elapsedMs = end, index = count - 1), 0.001f)
        assertTrue(pawAlpha(elapsedMs = end - 100f, index = count - 1) > 0f)
    }
}
