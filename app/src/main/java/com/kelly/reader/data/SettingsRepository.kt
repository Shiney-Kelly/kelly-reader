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

/**
 * The recent book and the reading settings, kept in Preferences DataStore.
 */
class SettingsRepository(private val dataStore: DataStore<Preferences>) {

    /**
     * Id of the book the app opens on launch, or null when there is none.
     */
    val recentBookId: Flow<String?> = dataStore.data.map { prefs -> prefs[RECENT_BOOK_ID] }

    /**
     * The reading settings, with defaults for anything not set yet.
     */
    val readerSettings: Flow<ReaderSettings> = dataStore.data.map { prefs ->
        ReaderSettings(
            fontSize = prefs[FONT_SIZE] ?: ReaderSettings.DEFAULT_FONT_SIZE,
            lineSpacing = LineSpacing.fromKey(prefs[LINE_SPACING]),
            font = ReaderFont.fromKey(prefs[FONT]),
            themeKey = prefs[THEME],
        )
    }

    /**
     * Remembers [id] as the recent book, or forgets it when null.
     */
    suspend fun setRecentBookId(id: String?) {
        dataStore.edit { prefs ->
            if (id == null) prefs.remove(RECENT_BOOK_ID) else prefs[RECENT_BOOK_ID] = id
        }
    }

    /**
     * Stores the font size, clamped to the allowed range.
     */
    suspend fun setFontSize(size: Int) {
        val clamped = size.coerceIn(ReaderSettings.MIN_FONT_SIZE, ReaderSettings.MAX_FONT_SIZE)
        dataStore.edit { prefs -> prefs[FONT_SIZE] = clamped }
    }

    suspend fun setLineSpacing(spacing: LineSpacing) {
        dataStore.edit { prefs -> prefs[LINE_SPACING] = spacing.key }
    }

    suspend fun setFont(font: ReaderFont) {
        dataStore.edit { prefs -> prefs[FONT] = font.key }
    }

    /**
     * Stores the reader theme by its key. The user cannot unset it once picked.
     */
    suspend fun setThemeKey(key: String) {
        dataStore.edit { prefs -> prefs[THEME] = key }
    }

    private companion object {
        val RECENT_BOOK_ID = stringPreferencesKey("recent_book_id")
        val FONT_SIZE = intPreferencesKey("font_size")
        val LINE_SPACING = stringPreferencesKey("line_spacing")
        val FONT = stringPreferencesKey("font")
        val THEME = stringPreferencesKey("reader_theme")
    }
}
