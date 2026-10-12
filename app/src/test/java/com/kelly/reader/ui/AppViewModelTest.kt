package com.kelly.reader.ui

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset

import com.kelly.reader.data.FakeBookRepository
import com.kelly.reader.data.FakeSettingsRepository
import com.kelly.reader.domain.Book
import com.kelly.reader.domain.ReadingStreak

/**
 * Checks the launch rule: reader for a recent book that still exists, shelf otherwise.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val today = LocalDate.of(2026, 10, 12)
    private val clock = Clock.fixed(today.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC)
    private val book = Book("b1", "Harbor", "b1.txt", 10, 10, 0)

    @Before
    fun setMainDispatcher() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    @Test
    fun startRoute_noRecentBook_isShelf() = runTest(dispatcher) {
        val viewModel = AppViewModel(FakeSettingsRepository(), FakeBookRepository())
        assertNull(viewModel.startRoute.value)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(Routes.SHELF, viewModel.startRoute.value)
    }

    @Test
    fun startRoute_recentBookExists_isReader() = runTest(dispatcher) {
        val book = Book("b1", "Harbor", "b1.txt", 10, 10, 0)
        val viewModel = AppViewModel(
            FakeSettingsRepository(recentBookId = "b1"),
            FakeBookRepository(listOf(book)),
        )
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals("reader/b1", viewModel.startRoute.value)
    }

    @Test
    fun startInReader_countsTodayAsReadingDay() = runTest(dispatcher) {
        val settings = FakeSettingsRepository(recentBookId = "b1")
        AppViewModel(settings, FakeBookRepository(listOf(book)), clock)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, settings.streak.value.days)
        assertEquals(today, settings.streak.value.lastDay)
    }

    @Test
    fun startOnShelf_doesNotCountTheDay() = runTest(dispatcher) {
        val settings = FakeSettingsRepository()
        AppViewModel(settings, FakeBookRepository(), clock)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(ReadingStreak(), settings.streak.value)
    }

    @Test
    fun streakMilestone_seventhDayInReader_isSeven() = runTest(dispatcher) {
        val settings = FakeSettingsRepository(
            recentBookId = "b1",
            streak = ReadingStreak(6, today.minusDays(1)),
        )
        val viewModel = AppViewModel(settings, FakeBookRepository(listOf(book)), clock)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(7, viewModel.streakMilestone.value)
    }

    @Test
    fun streakMilestone_laterLaunchSameDay_staysSeven() = runTest(dispatcher) {
        val settings = FakeSettingsRepository(streak = ReadingStreak(7, today))
        val viewModel = AppViewModel(settings, FakeBookRepository(), clock)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(7, viewModel.streakMilestone.value)
    }

    @Test
    fun streakMilestone_dayAfter_isNull() = runTest(dispatcher) {
        val settings = FakeSettingsRepository(
            recentBookId = "b1",
            streak = ReadingStreak(7, today.minusDays(1)),
        )
        val viewModel = AppViewModel(settings, FakeBookRepository(listOf(book)), clock)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(8, settings.streak.value.days)
        assertEquals(today, settings.streak.value.lastDay)
        assertNull(viewModel.streakMilestone.value)
    }

    @Test
    fun startRoute_recentBookDeleted_isShelf() = runTest(dispatcher) {
        val viewModel = AppViewModel(
            FakeSettingsRepository(recentBookId = "gone"),
            FakeBookRepository(),
        )
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(Routes.SHELF, viewModel.startRoute.value)
    }
}
