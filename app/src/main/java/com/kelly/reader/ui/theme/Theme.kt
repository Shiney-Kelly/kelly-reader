/*
 * App theme: the handoff tokens mapped onto a Material 3 color scheme, plus the extra colors.
 */
package com.kelly.reader.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

// token to slot: bg -> background, surface -> surface, ink -> on*, muted -> onSurfaceVariant,
// line -> outline, accent -> primary, tint -> primaryContainer, tint2 -> surfaceVariant,
// error ink/bg -> error/errorContainer, delete -> tertiary
private val LIGHT_COLOR_SCHEME = lightColorScheme(
    primary = ACCENT,
    onPrimary = ON_ACCENT,
    primaryContainer = LIGHT_TINT,
    onPrimaryContainer = LIGHT_TINT_INK,
    secondary = LIGHT_ACCENT_TEXT,
    onSecondary = ON_ACCENT,
    tertiary = DELETE,
    onTertiary = ON_ACCENT,
    background = LIGHT_BG,
    onBackground = LIGHT_INK,
    surface = LIGHT_SURFACE,
    onSurface = LIGHT_INK,
    surfaceVariant = LIGHT_TINT_2,
    onSurfaceVariant = LIGHT_MUTED,
    outline = LIGHT_LINE,
    outlineVariant = LIGHT_LINE,
    error = LIGHT_ERROR_INK,
    errorContainer = LIGHT_ERROR_BG,
    onErrorContainer = LIGHT_ERROR_INK,
    scrim = SCRIM,
)

private val DARK_COLOR_SCHEME = darkColorScheme(
    primary = ACCENT,
    onPrimary = ON_ACCENT,
    primaryContainer = DARK_TINT,
    onPrimaryContainer = DARK_TINT_INK,
    secondary = DARK_ACCENT_TEXT,
    onSecondary = DARK_BG,
    tertiary = DELETE,
    onTertiary = ON_ACCENT,
    background = DARK_BG,
    onBackground = DARK_INK,
    surface = DARK_SURFACE,
    onSurface = DARK_INK,
    surfaceVariant = DARK_TINT_2,
    onSurfaceVariant = DARK_MUTED,
    outline = DARK_LINE,
    outlineVariant = DARK_LINE,
    error = DARK_ERROR_INK,
    errorContainer = DARK_ERROR_BG,
    onErrorContainer = DARK_ERROR_INK,
    scrim = SCRIM,
)

/**
 * Wraps [content] in the app theme. [darkTheme] follows the system by default. Dynamic color is
 * off on purpose: the design has a fixed palette.
 */
@Composable
fun KellyReaderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DARK_COLOR_SCHEME else LIGHT_COLOR_SCHEME
    val appColors = if (darkTheme) DARK_APP_COLORS else LIGHT_APP_COLORS

    CompositionLocalProvider(LocalAppColors provides appColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = TYPOGRAPHY,
            content = content,
        )
    }
}
