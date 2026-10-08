/*
 * App theme: the light and dark Material 3 color schemes and the theme wrapper.
 */
package com.kelly.reader.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DARK_COLOR_SCHEME = darkColorScheme(
    primary = PURPLE_80,
    secondary = PURPLE_GREY_80,
    tertiary = PINK_80,
)

private val LIGHT_COLOR_SCHEME = lightColorScheme(
    primary = PURPLE_40,
    secondary = PURPLE_GREY_40,
    tertiary = PINK_40,
)

/**
 * Wraps [content] in the app's Material 3 theme. On Android 12 and newer, with [dynamicColor]
 * on, colors come from the wallpaper; otherwise [darkTheme] picks the fixed light or dark scheme.
 */
@Composable
fun KellyReaderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DARK_COLOR_SCHEME
        else -> LIGHT_COLOR_SCHEME
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = TYPOGRAPHY,
        content = content,
    )
}
