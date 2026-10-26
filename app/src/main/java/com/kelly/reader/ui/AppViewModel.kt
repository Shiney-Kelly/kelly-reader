package com.kelly.reader.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.ZonedDateTime

import com.kelly.reader.data.BookRepository
import com.kelly.reader.data.SettingsRepository

/**
 * Decides where the app starts: the reader when a recent book still exists, else the shelf.
 * Starting in the reader opens a book, so it counts as a reading day right away, in time for
 * the splash to show a streak milestone. It also clears files an earlier run left behind.
 */
class AppViewModel(
    private val settingsRepository: SettingsRepository,
    private val bookRepository: BookRepository,
    private val clock: Clock = Clock.systemDefaultZone(),
) : ViewModel() {

    private val today: LocalDate = LocalDate.now(clock)

    private val _startRoute = MutableStateFlow<String?>(null)

    /**
     * Null until the recent book has been looked up, then the route to start on.
     */
    val startRoute: StateFlow<String?> = _startRoute.asStateFlow()

    /**
     * The streak milestone reached today (7, 30, 100 or 365 days), or null on any other day.
     */
    val streakMilestone: StateFlow<Int?> = settingsRepository.readingStreak
        .map { streak -> streak.milestoneOn(today) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        viewModelScope.launch {
            val recentId = settingsRepository.recentBookId.first()
            val book = recentId?.let { bookRepository.getBook(it) }
            _startRoute.value = if (book != null) Routes.reader(book.id) else Routes.SHELF
            if (book != null) settingsRepository.recordReadingDay(ZonedDateTime.now(clock))
        }
        // an earlier run can leave an upload cut off when the app was killed; files changed
        // from now on belong to this run and stay
        val startedAt = clock.millis()
        viewModelScope.launch {
            try {
                bookRepository.deleteOrphanFiles(olderThan = startedAt)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // the books could not be read, so nothing was deleted; the next start tries again
            }
        }
    }
}
