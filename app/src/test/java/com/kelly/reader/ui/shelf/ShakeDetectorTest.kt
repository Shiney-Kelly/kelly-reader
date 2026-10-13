package com.kelly.reader.ui.shelf

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

private const val GRAVITY = 9.81f
private const val STEP_MS = 20L

/**
 * Checks that only a steady shake of three seconds counts.
 */
class ShakeDetectorTest {

    private val detector = ShakeDetector()

    /**
     * Feeds readings every [STEP_MS] from [fromMs] until [toMs], with a strong jolt every
     * [peakEveryMs] and the phone at rest in between, and returns the times it fired.
     */
    private fun shake(fromMs: Long, toMs: Long, peakEveryMs: Long = 100L): List<Long> {
        val fired = mutableListOf<Long>()
        for (time in fromMs until toMs step STEP_MS) {
            val x = if ((time - fromMs) % peakEveryMs == 0L) 20f else 0f
            if (detector.onReading(x, 0f, GRAVITY, time)) fired += time
        }
        return fired
    }

    private fun rest(fromMs: Long, toMs: Long): List<Long> {
        val fired = mutableListOf<Long>()
        for (time in fromMs until toMs step STEP_MS) {
            if (detector.onReading(0f, 0f, GRAVITY, time)) fired += time
        }
        return fired
    }

    @Test
    fun onReading_phoneAtRest_neverFires() {
        assertEquals(emptyList<Long>(), rest(0L, 10_000L))
    }

    @Test
    fun onReading_steadyShakeForThreeSeconds_firesOnceAtThreeSeconds() {
        assertEquals(listOf(SHAKE_DURATION_MS), shake(0L, 3_050L))
    }

    @Test
    fun onReading_shakeShorterThanThreeSeconds_neverFires() {
        assertEquals(emptyList<Long>(), shake(0L, 2_900L))
    }

    @Test
    fun onReading_pauseLongerThanGap_startsTheCountOver() {
        assertEquals(emptyList<Long>(), shake(0L, 2_000L))
        assertEquals(emptyList<Long>(), rest(2_000L, 2_600L))
        assertEquals(emptyList<Long>(), shake(2_600L, 4_000L))
        assertEquals(listOf(5_600L), shake(4_000L, 5_700L))
    }

    @Test
    fun onReading_shortPauses_keepTheShakeGoing() {
        assertEquals(listOf(3_360L), shake(0L, 3_400L, peakEveryMs = 480L))
    }

    @Test
    fun onReading_shakeKeepsGoing_firesAgainAfterAnotherThreeSeconds() {
        assertEquals(listOf(3_000L, 6_100L), shake(0L, 6_200L))
    }

    @Test
    fun onReading_jolt_isNotAShakeOnItsOwn() {
        assertFalse(detector.onReading(30f, 0f, GRAVITY, 0L))
        assertEquals(emptyList<Long>(), rest(20L, 5_000L))
    }
}
