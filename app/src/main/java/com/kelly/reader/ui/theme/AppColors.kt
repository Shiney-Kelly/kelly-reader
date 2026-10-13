/*
 * Colors the design needs beyond the Material color scheme, provided through a composition local.
 */
package com.kelly.reader.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * The handoff tokens that have no Material slot: tints, status colors, track, cover colors and
 * the ledge the shelf bear walks on.
 */
@Immutable
data class AppColors(
    val tint: Color,
    val tint2: Color,
    val tintInk: Color,
    val accentText: Color,
    val track: Color,
    val divider: Color,
    val okBg: Color,
    val okInk: Color,
    val errorBg: Color,
    val errorInk: Color,
    val coverEdge: Color,
    val coverSpine: Color,
    val coverGlyph: Color,
    val coverMeta: Color,
    val coverColors: List<Color>,
    val ledge: Color,
    val ledgeTop: Color,
    val ledgeShade: Color,
)

val LIGHT_APP_COLORS = AppColors(
    tint = LIGHT_TINT,
    tint2 = LIGHT_TINT_2,
    tintInk = LIGHT_TINT_INK,
    accentText = LIGHT_ACCENT_TEXT,
    track = LIGHT_TRACK,
    divider = LIGHT_DIVIDER,
    okBg = LIGHT_OK_BG,
    okInk = LIGHT_OK_INK,
    errorBg = LIGHT_ERROR_BG,
    errorInk = LIGHT_ERROR_INK,
    coverEdge = LIGHT_COVER_EDGE,
    coverSpine = LIGHT_COVER_SPINE,
    coverGlyph = LIGHT_COVER_GLYPH,
    coverMeta = LIGHT_COVER_META,
    coverColors = LIGHT_COVER_COLORS,
    ledge = LIGHT_LEDGE,
    ledgeTop = LIGHT_LEDGE_TOP,
    ledgeShade = LIGHT_LEDGE_SHADE,
)

val DARK_APP_COLORS = AppColors(
    tint = DARK_TINT,
    tint2 = DARK_TINT_2,
    tintInk = DARK_TINT_INK,
    accentText = DARK_ACCENT_TEXT,
    track = DARK_TRACK,
    divider = DARK_DIVIDER,
    okBg = DARK_OK_BG,
    okInk = DARK_OK_INK,
    errorBg = DARK_ERROR_BG,
    errorInk = DARK_ERROR_INK,
    coverEdge = DARK_COVER_EDGE,
    coverSpine = DARK_COVER_SPINE,
    coverGlyph = DARK_COVER_GLYPH,
    coverMeta = DARK_COVER_META,
    coverColors = DARK_COVER_COLORS,
    ledge = DARK_LEDGE,
    ledgeTop = DARK_LEDGE_TOP,
    ledgeShade = DARK_LEDGE_SHADE,
)

val LocalAppColors = staticCompositionLocalOf { LIGHT_APP_COLORS }

/**
 * Entry point for the extra colors, used as `AppTheme.colors.tint` next to `MaterialTheme`.
 */
object AppTheme {
    val colors: AppColors
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColors.current
}
