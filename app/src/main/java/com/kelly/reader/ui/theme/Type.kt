/*
 * Font families and the Material 3 typography. Figtree for the UI, Literata for reading.
 */
package com.kelly.reader.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

import com.kelly.reader.R

// both files are variable fonts, so each weight is the same file with a weight axis setting
@OptIn(ExperimentalTextApi::class)
private fun variableFont(resId: Int, weight: FontWeight): Font = Font(
    resId = resId,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

val FIGTREE = FontFamily(
    variableFont(R.font.figtree, FontWeight.Normal),
    variableFont(R.font.figtree, FontWeight.Medium),
    variableFont(R.font.figtree, FontWeight.SemiBold),
    variableFont(R.font.figtree, FontWeight.Bold),
)

val LITERATA = FontFamily(
    variableFont(R.font.literata, FontWeight.Normal),
    variableFont(R.font.literata, FontWeight.SemiBold),
)

/**
 * Returns a copy of this style in the UI font.
 */
private fun TextStyle.ui(): TextStyle = copy(fontFamily = FIGTREE)

// material defaults with the UI font applied to every style
val TYPOGRAPHY = Typography().let { base ->
    Typography(
        displayLarge = base.displayLarge.ui(),
        displayMedium = base.displayMedium.ui(),
        displaySmall = base.displaySmall.ui(),
        headlineLarge = base.headlineLarge.ui(),
        headlineMedium = base.headlineMedium.ui(),
        headlineSmall = base.headlineSmall.ui(),
        titleLarge = base.titleLarge.ui(),
        titleMedium = base.titleMedium.ui(),
        titleSmall = base.titleSmall.ui(),
        bodyLarge = base.bodyLarge.ui().copy(fontSize = 16.sp, lineHeight = 24.sp),
        bodyMedium = base.bodyMedium.ui(),
        bodySmall = base.bodySmall.ui(),
        labelLarge = base.labelLarge.ui(),
        labelMedium = base.labelMedium.ui(),
        labelSmall = base.labelSmall.ui(),
    )
}
