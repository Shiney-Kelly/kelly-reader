package com.kelly.reader.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

/**
 * Checks how the streak grows, resets and when a milestone shows.
 */
class ReadingStreakTest {

    private val day = LocalDate.of(2026, 10, 12)

    @Test
    fun recordedOn_firstEver_startsAtOne() {
        assertEquals(ReadingStreak(1, day), ReadingStreak().recordedOn(day))
    }

    @Test
    fun recordedOn_sameDay_isUnchanged() {
        val streak = ReadingStreak(4, day)
        assertEquals(streak, streak.recordedOn(day))
    }

    @Test
    fun recordedOn_nextDay_growsByOne() {
        assertEquals(ReadingStreak(5, day), ReadingStreak(4, day.minusDays(1)).recordedOn(day))
    }

    @Test
    fun recordedOn_afterMissedDay_restartsAtOne() {
        assertEquals(ReadingStreak(1, day), ReadingStreak(20, day.minusDays(2)).recordedOn(day))
    }

    @Test
    fun recordedOn_earlierDate_keepsStreak() {
        val streak = ReadingStreak(40, day.plusDays(1))
        assertEquals(streak, streak.recordedOn(day))
    }

    @Test
    fun recordedOn_flyingWest_losesNothing() {
        // read in Tokyo on Tue 13, land in Honolulu on Mon 12, then read Mon, Tue, Wed there
        val tue = LocalDate.of(2026, 10, 13)
        var streak = ReadingStreak(40, tue)
        streak = streak.recordedOn(tue.minusDays(1))
        assertEquals(ReadingStreak(40, tue), streak)
        streak = streak.recordedOn(tue)
        assertEquals(ReadingStreak(40, tue), streak)
        streak = streak.recordedOn(tue.plusDays(1))
        assertEquals(ReadingStreak(41, tue.plusDays(1)), streak)
    }

    @Test
    fun recordedOn_acrossYearEnd_growsByOne() {
        val newYear = LocalDate.of(2027, 1, 1)
        val streak = ReadingStreak(6, LocalDate.of(2026, 12, 31)).recordedOn(newYear)
        assertEquals(ReadingStreak(7, newYear), streak)
    }

    @Test
    fun milestoneOn_milestoneDay_returnsIt() {
        listOf(7, 30, 100, 365).forEach { days ->
            assertEquals(days, ReadingStreak(days, day).milestoneOn(day))
        }
    }

    @Test
    fun milestoneOn_otherDays_isNull() {
        assertNull(ReadingStreak(6, day).milestoneOn(day))
        assertNull(ReadingStreak(8, day).milestoneOn(day))
        assertNull(ReadingStreak(366, day).milestoneOn(day))
    }

    @Test
    fun milestoneOn_dayAfterMilestone_isNull() {
        // reached 7 yesterday and not opened a book yet today
        assertNull(ReadingStreak(7, day.minusDays(1)).milestoneOn(day))
    }

    @Test
    fun sevenDaysInARow_reachesFirstMilestoneOnDaySeven() {
        var streak = ReadingStreak()
        (0L until 7L).forEach { offset -> streak = streak.recordedOn(day.plusDays(offset)) }
        assertEquals(7, streak.milestoneOn(day.plusDays(6)))
    }
}
