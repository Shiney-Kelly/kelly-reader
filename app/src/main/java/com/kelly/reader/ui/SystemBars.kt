/*
 * Status and navigation bar icon colors for screens whose background does not follow the
 * system theme.
 */
package com.kelly.reader.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Draws the system bar icons dark while [darkIcons] is true, light otherwise, and puts them
 * back to the system default when this leaves the composition. Does nothing in previews.
 */
@Composable
fun SystemBarIcons(darkIcons: Boolean) {
    val view = LocalView.current
    val systemDark = isSystemInDarkTheme()
    DisposableEffect(darkIcons, systemDark) {
        val window = view.context.findActivity()?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        controller?.isAppearanceLightStatusBars = darkIcons
        controller?.isAppearanceLightNavigationBars = darkIcons
        onDispose {
            controller?.isAppearanceLightStatusBars = !systemDark
            controller?.isAppearanceLightNavigationBars = !systemDark
        }
    }
}

/**
 * The activity behind a context, or null outside one, such as in a preview.
 */
private fun Context.findActivity(): Activity? {
    var context: Context? = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}
