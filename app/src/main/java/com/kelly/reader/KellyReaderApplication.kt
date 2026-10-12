package com.kelly.reader

import android.app.Application

import com.kelly.reader.di.AppContainer

/**
 * Creates the [AppContainer] once for the process. Screens reach it through their context.
 */
class KellyReaderApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
