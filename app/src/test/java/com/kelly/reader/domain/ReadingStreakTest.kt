package com.kelly.reader.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Checks how the streak grows, resets, survives timezone changes, and when a milestone shows.
 */
class ReadingStreakTest {

    private val home = ZoneId.of("Europe/London")
    private val tokyo = ZoneId.of("Asia/Tokyo")
    private val honolulu = ZoneId.of("Pacific/Honolulu")
    private val day = LocalDate.of(2026, 10, 12)

    private fun at(date: LocalDate, hour: Int, zone: ZoneId = home): ZonedDateTime =
        ZonedDateTime.of(date, LocalTime.of(hour, 0), zone)

    private fun assertStreak(days: Int, lastDay: LocalDate, streak: ReadingStreak) {
        assertEquals(days, streak.days)
        assertEquals(lastDay, streak.lastDay)
    }

    // ----- plain days -----

    @Test
    fun recordedOn_firstEver_startsAtOne() {
        assertStreak(1, day, ReadingStreak().recordedOn(at(day, 9)))
    }

    @Test
    fun recordedOn_sameDay_keepsCount() {
        val streak = ReadingStreak().recordedOn(at(day, 9)).recordedOn(at(day, 22))
        assertStreak(1, day, streak)
    }

    @Test
    fun recordedOn_nextDay_growsByOne() {
        assertStreak(5, day, ReadingStreak(4, day.minusDays(1)).recordedOn(at(day, 9)))
    }

    @Test
    fun recordedOn_afterMissedDay_restartsAtOne() {
        assertStreak(1, day, ReadingStreak(20, day.minusDays(2)).recordedOn(at(day, 9)))
    }

    @Test
    fun recordedOn_acrossYearEnd_growsByOne() {
        val newYear = LocalDate.of(2027, 1, 1)
        val streak = ReadingStreak(6, LocalDate.of(2026, 12, 31)).recordedOn(at(newYear, 9))
        assertStreak(7, newYear, streak)
    }

    @Test
    fun recordedOn_keepsTimeAndZoneOfLatestRead() {
        val now = at(day, 21, tokyo)
        val streak = ReadingStreak(3, day).recordedOn(now)
        assertEquals(now.toInstant(), streak.lastReadAt)
        assertEquals(tokyo, streak.lastZone)
    }

    // ----- the date going back -----

    @Test
    fun recordedOn_earlierDate_keepsStreak() {
        assertStreak(40, day.plusDays(1), ReadingStreak(40, day.plusDays(1)).recordedOn(at(day, 9)))
    }

    @Test
    fun recordedOn_flyingWest_losesNothing() {
        // read in Tokyo on Tue 13 at 8 am, fly at 9 am for 7 hours, land in Honolulu on Mon 12
        // at 9 pm, then read there on Mon, Tue and Wed
        val tue = LocalDate.of(2026, 10, 13)
        var streak = ReadingStreak(39, tue.minusDays(1)).recordedOn(at(tue, 8, tokyo))
        assertStreak(40, tue, streak)
        streak = streak.recordedOn(at(tue.minusDays(1), 21, honolulu))
        assertStreak(40, tue, streak)
        streak = streak.recordedOn(at(tue, 20, honolulu))
        assertStreak(40, tue, streak)
        streak = streak.recordedOn(at(tue.plusDays(1), 20, honolulu))
        assertStreak(41, tue.plusDays(1), streak)
    }

    // ----- a skipped date -----

    @Test
    fun recordedOn_flyingEastSkipsDate_keepsStreak() {
        // read in Honolulu on Mon 12 at 9 pm, fly 8 hours, read in Tokyo on Wed 14 at 9 am:
        // Tue 13 passed during the flight and only 17 hours really went by
        val streak = ReadingStreak(39, day.minusDays(1))
            .recordedOn(at(day, 21, honolulu))
            .recordedOn(at(day.plusDays(2), 9, tokyo))
        assertStreak(41, day.plusDays(2), streak)
    }

    @Test
    fun recordedOn_skippedDayAtHome_restarts() {
        // Monday 11 pm to Wednesday 8 am is only 33 hours, but Tuesday was really skipped
        val streak = ReadingStreak(39, day.minusDays(1))
            .recordedOn(at(day, 23))
            .recordedOn(at(day.plusDays(2), 8))
        assertStreak(1, day.plusDays(2), streak)
    }

    @Test
    fun recordedOn_timezoneChangedButTooLong_restarts() {
        // Honolulu Mon 12 at 6 am to Tokyo Wed 14 at 8 pm is 43 hours
        val streak = ReadingStreak(39, day.minusDays(1))
            .recordedOn(at(day, 6, honolulu))
            .recordedOn(at(day.plusDays(2), 20, tokyo))
        assertStreak(1, day.plusDays(2), streak)
    }

    @Test
    fun recordedOn_threeDatesLater_restartsEvenAfterFlight() {
        // Honolulu Mon 12 at 9 pm to Tokyo Thu 15 at 1 am is 33 hours, but two dates went by
        val streak = ReadingStreak(39, day.minusDays(1))
            .recordedOn(at(day, 21, honolulu))
            .recordedOn(at(day.plusDays(3), 1, tokyo))
        assertStreak(1, day.plusDays(3), streak)
    }

    @Test
    fun recordedOn_oldRecordWithoutTime_noGrace() {
        assertStreak(1, day.plusDays(2), ReadingStreak(40, day).recordedOn(at(day.plusDays(2), 9, tokyo)))
    }

    // ----- milestones -----

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
        (0L until 7L).forEach { offset -> streak = streak.recordedOn(at(day.plusDays(offset), 20)) }
        assertEquals(7, streak.milestoneOn(day.plusDays(6)))
    }
}
