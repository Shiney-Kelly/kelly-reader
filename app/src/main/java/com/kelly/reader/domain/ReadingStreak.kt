package com.kelly.reader.domain

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * How many days in a row a book has been opened, ending on [lastDay]. [lastReadAt] and
 * [lastZone] are the moment and timezone of the latest open, which let a long eastward flight
 * that skips a calendar date keep the streak. A fresh install has no streak.
 */
data class ReadingStreak(
    val days: Int = 0,
    val lastDay: LocalDate? = null,
    val lastReadAt: Instant? = null,
    val lastZone: ZoneId? = null,
) {

    /**
     * The streak after a book is opened at [now], in the phone's timezone at that moment:
     * - the same date, or an earlier one after flying west or setting an earlier timezone:
     *   unchanged
     * - the next date: one longer
     * - two dates later, but less than [GRACE] really passed and the timezone changed, as when
     *   a flight east skips a date: one longer
     * - anything else: back to one
     */
    fun recordedOn(now: ZonedDateTime): ReadingStreak {
        val today = now.toLocalDate()
        val last = lastDay
        val count = when {
            last == null -> 1
            !today.isAfter(last) -> days
            today == last.plusDays(1) -> days + 1
            today == last.plusDays(2) && dateSkippedByTimezone(now) -> days + 1
            else -> 1
        }
        val day = if (last != null && !today.isAfter(last)) last else today
        return ReadingStreak(count, day, now.toInstant(), now.zone)
    }

    // at home, a two-date gap of under 36 hours (Monday 11 pm to Wednesday 8 am) is a skipped
    // day, so the grace needs the timezone to have changed as well
    private fun dateSkippedByTimezone(now: ZonedDateTime): Boolean {
        val readAt = lastReadAt ?: return false
        val zone = lastZone ?: return false
        return zone != now.zone && Duration.between(readAt, now.toInstant()) < GRACE
    }

    /**
     * The milestone reached on [today], or null. Only the day the streak hits a milestone counts,
     * so the splash line shows for that one day and not the day after.
     */
    fun milestoneOn(today: LocalDate): Int? =
        days.takeIf { lastDay == today && it in MILESTONES }

    companion object {
        val MILESTONES = setOf(7, 30, 100, 365)
        val GRACE: Duration = Duration.ofHours(36)
    }
}
