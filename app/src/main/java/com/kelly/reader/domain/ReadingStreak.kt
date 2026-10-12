package com.kelly.reader.domain

import java.time.LocalDate

/**
 * How many days in a row a book has been opened, ending on [lastDay]. A fresh install has no
 * streak: zero days and no last day.
 */
data class ReadingStreak(
    val days: Int = 0,
    val lastDay: LocalDate? = null,
) {

    /**
     * The streak after a book is opened on [today]: unchanged on the same day, one longer the
     * day after, and back to one after a missed day or a clock that went backwards.
     */
    fun recordedOn(today: LocalDate): ReadingStreak = when (lastDay) {
        today -> this
        today.minusDays(1) -> ReadingStreak(days + 1, today)
        else -> ReadingStreak(1, today)
    }

    /**
     * The milestone reached on [today], or null. Only the day the streak hits a milestone counts,
     * so the splash line shows for that one day and not the day after.
     */
    fun milestoneOn(today: LocalDate): Int? =
        days.takeIf { lastDay == today && it in MILESTONES }

    companion object {
        val MILESTONES = setOf(7, 30, 100, 365)
    }
}
