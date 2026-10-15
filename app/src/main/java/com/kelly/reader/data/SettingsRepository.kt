package com.kelly.reader.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

import com.kelly.reader.domain.LineSpacing
import com.kelly.reader.domain.ReaderFont
import com.kelly.reader.domain.ReaderSettings
import com.kelly.reader.domain.ReadingMode
import com.kelly.reader.domain.ReadingStreak
import com.kelly.reader.domain.TextAlignment

/**
 * The recent book, the reading settings, the reading streak and whether the shelf bear has been
 * found. An interface so tests can use a fake.
 */
interface SettingsRepository {

    /**
     * Id of the book the app opens on launch, or null when there is none.
     */
    val recentBookId: Flow<String?>

    /**
     * The reading settings, with defaults for anything not set yet.
     */
    val readerSettings: Flow<ReaderSettings>

    /**
     * Remembers [id] as the recent book, or forgets it when null.
     */
    suspend fun setRecentBookId(id: String?)

    /**
     * Stores the font size, clamped to the allowed range.
     */
    suspend fun setFontSize(size: Int)

    suspend fun setLineSpacing(spacing: LineSpacing)

    suspend fun setFont(font: ReaderFont)

    /**
     * Stores the reader theme by its key, or goes back to following the system when null.
     */
    suspend fun setThemeKey(key: String?)

    suspend fun setReadingMode(mode: ReadingMode)

    suspend fun setTextAlignment(alignment: TextAlignment)

    /**
     * The days in a row a book has been opened.
     */
    val readingStreak: Flow<ReadingStreak>

    /**
     * Counts a book opened at [now] toward the streak. More opens on the same day change only
     * the stored time and timezone.
     */
    suspend fun recordReadingDay(now: ZonedDateTime)

    /**
     * Whether someone has found the shelf bear, which stays hidden until then.
     */
    val shelfPetFound: Flow<Boolean>

    suspend fun setShelfPetFound(found: Boolean)
}

/**
 * Settings kept in Preferences DataStore.
 */
class DataStoreSettingsRepository(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    override val recentBookId: Flow<String?> = dataStore.data.map { prefs -> prefs[RECENT_BOOK_ID] }

    override val readerSettings: Flow<ReaderSettings> = dataStore.data.map { prefs ->
        ReaderSettings(
            fontSize = prefs[FONT_SIZE] ?: ReaderSettings.DEFAULT_FONT_SIZE,
            lineSpacing = LineSpacing.fromKey(prefs[LINE_SPACING]),
            font = ReaderFont.fromKey(prefs[FONT]),
            themeKey = prefs[THEME],
            readingMode = ReadingMode.fromKey(prefs[READING_MODE]),
            textAlignment = TextAlignment.fromKey(prefs[TEXT_ALIGNMENT]),
        )
    }

    override suspend fun setRecentBookId(id: String?) {
        dataStore.edit { prefs ->
            if (id == null) prefs.remove(RECENT_BOOK_ID) else prefs[RECENT_BOOK_ID] = id
        }
    }

    override suspend fun setFontSize(size: Int) {
        val clamped = size.coerceIn(ReaderSettings.MIN_FONT_SIZE, ReaderSettings.MAX_FONT_SIZE)
        dataStore.edit { prefs -> prefs[FONT_SIZE] = clamped }
    }

    override suspend fun setLineSpacing(spacing: LineSpacing) {
        dataStore.edit { prefs -> prefs[LINE_SPACING] = spacing.key }
    }

    override suspend fun setFont(font: ReaderFont) {
        dataStore.edit { prefs -> prefs[FONT] = font.key }
    }

    override suspend fun setThemeKey(key: String?) {
        dataStore.edit { prefs -> if (key == null) prefs.remove(THEME) else prefs[THEME] = key }
    }

    override suspend fun setReadingMode(mode: ReadingMode) {
        dataStore.edit { prefs -> prefs[READING_MODE] = mode.key }
    }

    override suspend fun setTextAlignment(alignment: TextAlignment) {
        dataStore.edit { prefs -> prefs[TEXT_ALIGNMENT] = alignment.key }
    }

    override val readingStreak: Flow<ReadingStreak> = dataStore.data.map { prefs -> prefs.streak() }

    // read and write in one edit, so two book opens at once cannot both count from the old value
    override suspend fun recordReadingDay(now: ZonedDateTime) {
        dataStore.edit { prefs ->
            val next = prefs.streak().recordedOn(now)
            prefs[STREAK_DAYS] = next.days
            next.lastDay?.let { prefs[STREAK_LAST_DAY] = it.toEpochDay() }
            next.lastReadAt?.let { prefs[STREAK_LAST_READ_AT] = it.toEpochMilli() }
            next.lastZone?.let { prefs[STREAK_LAST_ZONE] = it.id }
        }
    }

    override val shelfPetFound: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[SHELF_PET_FOUND] ?: false
    }

    override suspend fun setShelfPetFound(found: Boolean) {
        dataStore.edit { prefs -> prefs[SHELF_PET_FOUND] = found }
    }

    private fun Preferences.streak(): ReadingStreak = ReadingStreak(
        days = this[STREAK_DAYS] ?: 0,
        lastDay = this[STREAK_LAST_DAY]?.let(LocalDate::ofEpochDay),
        lastReadAt = this[STREAK_LAST_READ_AT]?.let(Instant::ofEpochMilli),
        // a zone id this phone's timezone database does not know just means no grace
        lastZone = this[STREAK_LAST_ZONE]?.let { id -> runCatching { ZoneId.of(id) }.getOrNull() },
    )

    private companion object {
        val RECENT_BOOK_ID = stringPreferencesKey("recent_book_id")
        val FONT_SIZE = intPreferencesKey("font_size")
        val LINE_SPACING = stringPreferencesKey("line_spacing")
        val FONT = stringPreferencesKey("font")
        val THEME = stringPreferencesKey("reader_theme")
        val READING_MODE = stringPreferencesKey("reading_mode")
        val TEXT_ALIGNMENT = stringPreferencesKey("text_alignment")
        val STREAK_DAYS = intPreferencesKey("streak_days")
        // the last reading day as days since 1970-01-01, which is what LocalDate keeps inside
        val STREAK_LAST_DAY = longPreferencesKey("streak_last_day")
        val STREAK_LAST_READ_AT = longPreferencesKey("streak_last_read_at")
        val STREAK_LAST_ZONE = stringPreferencesKey("streak_last_zone")
        val SHELF_PET_FOUND = booleanPreferencesKey("shelf_pet_found")
    }
}
