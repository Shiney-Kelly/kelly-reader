package com.kelly.reader.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import java.time.ZonedDateTime

import com.kelly.reader.domain.BookSort
import com.kelly.reader.domain.LineSpacing
import com.kelly.reader.domain.ProgressLabel
import com.kelly.reader.domain.ReaderFont
import com.kelly.reader.domain.ReaderSettings
import com.kelly.reader.domain.ReadingMode
import com.kelly.reader.domain.ReadingStreak
import com.kelly.reader.domain.TextAlignment

/**
 * In-memory [SettingsRepository] for ViewModel tests.
 */
class FakeSettingsRepository(
    recentBookId: String? = null,
    settings: ReaderSettings = ReaderSettings(),
    streak: ReadingStreak = ReadingStreak(),
    shelfPetFound: Boolean = false,
) : SettingsRepository {

    val recent = MutableStateFlow(recentBookId)
    val settings = MutableStateFlow(settings)
    val streak = MutableStateFlow(streak)
    val petFound = MutableStateFlow(shelfPetFound)

    override val recentBookId: Flow<String?> = recent

    override val readerSettings: Flow<ReaderSettings> = this.settings

    override suspend fun setRecentBookId(id: String?) {
        recent.value = id
    }

    override suspend fun setFontSize(size: Int) {
        val clamped = size.coerceIn(ReaderSettings.MIN_FONT_SIZE, ReaderSettings.MAX_FONT_SIZE)
        settings.update { it.copy(fontSize = clamped) }
    }

    override suspend fun setLineSpacing(spacing: LineSpacing) {
        settings.update { it.copy(lineSpacing = spacing) }
    }

    override suspend fun setFont(font: ReaderFont) {
        settings.update { it.copy(font = font) }
    }

    override suspend fun setThemeKey(key: String?) {
        settings.update { it.copy(themeKey = key) }
    }

    override suspend fun setReadingMode(mode: ReadingMode) {
        settings.update { it.copy(readingMode = mode) }
    }

    override suspend fun setTextAlignment(alignment: TextAlignment) {
        settings.update { it.copy(textAlignment = alignment) }
    }

    override suspend fun setProgressLabel(label: ProgressLabel) {
        settings.update { it.copy(progressLabel = label) }
    }

    override val readingStreak: Flow<ReadingStreak> = this.streak

    override suspend fun recordReadingDay(now: ZonedDateTime) {
        streak.update { it.recordedOn(now) }
    }

    override val shelfPetFound: Flow<Boolean> = petFound

    override suspend fun setShelfPetFound(found: Boolean) {
        petFound.value = found
    }

    val sort = MutableStateFlow(BookSort.ADDED_OLDEST)

    override val shelfSort: Flow<BookSort> = sort

    override suspend fun setShelfSort(sort: BookSort) {
        this.sort.value = sort
    }
}
