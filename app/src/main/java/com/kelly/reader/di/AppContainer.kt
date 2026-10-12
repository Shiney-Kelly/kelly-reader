package com.kelly.reader.di

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import java.io.File

import com.kelly.reader.data.BookRepository
import com.kelly.reader.data.DataStoreSettingsRepository
import com.kelly.reader.data.PageCache
import com.kelly.reader.data.RoomBookRepository
import com.kelly.reader.data.SettingsRepository
import com.kelly.reader.data.WifiAddress
import com.kelly.reader.data.db.AppDatabase
import com.kelly.reader.data.upload.UploadServer

// one preferences file for the whole app, created on first access
private val Context.settingsStore by preferencesDataStore(name = "settings")

private const val UPLOAD_PAGE_ASSET = "upload_page.html"

/**
 * Builds and holds the app-wide objects by hand: the database and the repositories. Lives in
 * the Application, so there is one of each for the whole process.
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val database: AppDatabase by lazy { AppDatabase.build(appContext) }

    val pageCache: PageCache by lazy { PageCache(File(appContext.filesDir, "pages")) }

    val bookRepository: BookRepository by lazy {
        RoomBookRepository(database.bookDao(), File(appContext.filesDir, "books"), pageCache)
    }

    val settingsRepository: SettingsRepository by lazy {
        DataStoreSettingsRepository(appContext.settingsStore)
    }

    private val uploadPage: String by lazy {
        appContext.assets.open(UPLOAD_PAGE_ASSET).bufferedReader().use { it.readText() }
    }

    /**
     * A fresh upload server, not started. The Add books screen owns its lifetime.
     */
    fun newUploadServer(): UploadServer = UploadServer(bookRepository, uploadPage)

    /**
     * The Wi-Fi IPv4 address, or null off Wi-Fi, updated as the network changes.
     */
    fun wifiAddress(): Flow<String?> = WifiAddress.observe(appContext)
}
