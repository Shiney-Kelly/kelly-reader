package com.kelly.reader.di

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import java.io.File

import com.kelly.reader.data.BookRepository
import com.kelly.reader.data.SettingsRepository
import com.kelly.reader.data.db.AppDatabase

// one preferences file for the whole app, created on first access
private val Context.settingsStore by preferencesDataStore(name = "settings")

/**
 * Builds and holds the app-wide objects by hand: the database and the repositories. Lives in
 * the Application, so there is one of each for the whole process.
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val database: AppDatabase by lazy { AppDatabase.build(appContext) }

    val bookRepository: BookRepository by lazy {
        BookRepository(database.bookDao(), File(appContext.filesDir, "books"))
    }

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(appContext.settingsStore)
    }
}
