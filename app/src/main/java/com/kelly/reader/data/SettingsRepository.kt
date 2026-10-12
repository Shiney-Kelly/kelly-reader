package com.kelly.reader.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

import com.kelly.reader.domain.LineSpacing
import com.kelly.reader.domain.ReaderFont
import com.kelly.reader.domain.ReaderSettings
import com.kelly.reader.domain.ReadingMode
import com.kelly.reader.domain.TextAlignment

/**
 * The recent book and the reading settings. An interface so tests can use a fake.
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

    private companion object {
        val RECENT_BOOK_ID = stringPreferencesKey("recent_book_id")
        val FONT_SIZE = intPreferencesKey("font_size")
        val LINE_SPACING = stringPreferencesKey("line_spacing")
        val FONT = stringPreferencesKey("font")
        val THEME = stringPreferencesKey("reader_theme")
        val READING_MODE = stringPreferencesKey("reading_mode")
        val TEXT_ALIGNMENT = stringPreferencesKey("text_alignment")
    }
}
