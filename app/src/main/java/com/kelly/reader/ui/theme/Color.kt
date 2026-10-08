/*
 * Every color in the app, taken from the design handoff tokens. See docs/design.md.
 */
package com.kelly.reader.ui.theme

import androidx.compose.ui.graphics.Color

// template colors, replaced by the theme rewrite
val PURPLE_80 = Color(0xFFD0BCFF)
val PURPLE_GREY_80 = Color(0xFFCCC2DC)
val PINK_80 = Color(0xFFEFB8C8)
val PURPLE_40 = Color(0xFF6650A4)
val PURPLE_GREY_40 = Color(0xFF625B71)
val PINK_40 = Color(0xFF7D5260)

// ----- shared -----

val ACCENT = Color(0xFF56699A)
val ON_ACCENT = Color(0xFFFFFFFF)
val DELETE = Color(0xFFA8524A)
val LIVE_DOT = Color(0xFF1E8A4A)
val SCRIM = Color(0x38282622)
val CHECK_OFF_BG = Color(0xE0FFFFFF)
val CHECK_OFF_INK = Color(0xFF9AA0A6)

// ----- light -----

val LIGHT_BG = Color(0xFFF4F3F0)
val LIGHT_SURFACE = Color(0xFFFFFFFF)
val LIGHT_INK = Color(0xFF2E3035)
val LIGHT_MUTED = Color(0xFF5F6368)
val LIGHT_LINE = Color(0xFFE1E2E5)
val LIGHT_TRACK = Color(0xFFE1E3E7)
val LIGHT_TINT = Color(0xFFECEEF4)
val LIGHT_TINT_2 = Color(0xFFF1F2F4)
val LIGHT_TINT_INK = Color(0xFF3E4A6E)
val LIGHT_ACCENT_TEXT = Color(0xFF56699A)
val LIGHT_OK_BG = Color(0xFFE4F2E8)
val LIGHT_OK_INK = Color(0xFF1E6B3A)
val LIGHT_ERROR_BG = Color(0xFFF6E9E7)
val LIGHT_ERROR_INK = Color(0xFF8C3F38)
val LIGHT_DIVIDER = Color(0xFFEEEFF1)
val LIGHT_COVER_EDGE = Color(0x00000000)
val LIGHT_COVER_SPINE = Color(0x1A000000)
val LIGHT_COVER_GLYPH = Color(0xF0FFFFFF)
val LIGHT_COVER_META = Color(0xDBFFFFFF)

// ----- dark -----

val DARK_BG = Color(0xFF1C1D20)
val DARK_SURFACE = Color(0xFF26282C)
val DARK_INK = Color(0xFFE3E1DC)
val DARK_MUTED = Color(0xFFA3A6AB)
val DARK_LINE = Color(0xFF3A3D42)
val DARK_TRACK = Color(0xFF3A3D42)
val DARK_TINT = Color(0xFF2E3340)
val DARK_TINT_2 = Color(0xFF2F3135)
val DARK_TINT_INK = Color(0xFFC3CCE6)
val DARK_ACCENT_TEXT = Color(0xFF9DB0DE)
val DARK_OK_BG = Color(0xFF22362A)
val DARK_OK_INK = Color(0xFF8FCCA2)
val DARK_ERROR_BG = Color(0xFF3D2826)
val DARK_ERROR_INK = Color(0xFFE3A39B)
val DARK_DIVIDER = Color(0xFF303236)
val DARK_COVER_EDGE = Color(0x12FFFFFF)
val DARK_COVER_SPINE = Color(0x47000000)
val DARK_COVER_GLYPH = Color(0xD1ECE8E0)
val DARK_COVER_META = Color(0x9EECE8E0)

// ----- covers -----

// one color per book, picked by hashing the file name, same index in both lists
val LIGHT_COVER_COLORS = listOf(
    Color(0xFF56699A),
    Color(0xFF5F8276),
    Color(0xFF7C6C96),
    Color(0xFF5A7E8F),
    Color(0xFFA06B7E),
    Color(0xFFA07D55),
    Color(0xFF7C8087),
    Color(0xFFA26B5C),
    Color(0xFF6F8463),
)

val DARK_COVER_COLORS = listOf(
    Color(0xFF34405C),
    Color(0xFF304740),
    Color(0xFF433A55),
    Color(0xFF30444D),
    Color(0xFF553A46),
    Color(0xFF554431),
    Color(0xFF43464C),
    Color(0xFF563B34),
    Color(0xFF3B4834),
)

// ----- reader themes -----

val READER_LIGHT_BG = Color(0xFFFAF9F7)
val READER_LIGHT_INK = Color(0xFF36383C)
val READER_LIGHT_MUTED = Color(0xFF6A6D72)
val READER_PAPER_BG = Color(0xFFF3EDE3)
val READER_PAPER_INK = Color(0xFF4A4034)
val READER_PAPER_MUTED = Color(0xFF76695A)
val READER_DARK_BG = Color(0xFF25272B)
val READER_DARK_INK = Color(0xFFC9C6BF)
val READER_DARK_MUTED = Color(0xFF8F8D87)
