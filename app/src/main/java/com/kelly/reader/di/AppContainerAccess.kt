/*
 * How composables reach the app container without a DI framework.
 */
package com.kelly.reader.di

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalContext

import com.kelly.reader.KellyReaderApplication

/**
 * The process-wide [AppContainer], read from the application context.
 */
@Composable
@ReadOnlyComposable
fun appContainer(): AppContainer =
    (LocalContext.current.applicationContext as KellyReaderApplication).container
