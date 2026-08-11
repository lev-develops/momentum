package com.momentum.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.momentum.app.R

/** Inter (SIL OFL), bundled as static TTFs. Two weights only — Regular (400) and Medium (500). */
val InterFontFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
)

private val HeadingLetterSpacing = (-0.02).em

private fun heading(size: androidx.compose.ui.unit.TextUnit, lineHeight: androidx.compose.ui.unit.TextUnit) = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.Medium,
    fontSize = size,
    lineHeight = lineHeight,
    letterSpacing = HeadingLetterSpacing,
)

private fun body(size: androidx.compose.ui.unit.TextUnit, lineHeight: androidx.compose.ui.unit.TextUnit, weight: FontWeight = FontWeight.Normal) = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = weight,
    fontSize = size,
    lineHeight = lineHeight,
    letterSpacing = 0.em,
)

val MomentumTypography = Typography(
    displayLarge = heading(36.sp, 42.sp),
    displayMedium = heading(32.sp, 38.sp),
    displaySmall = heading(28.sp, 34.sp),
    headlineLarge = heading(26.sp, 32.sp),
    headlineMedium = heading(24.sp, 30.sp),
    headlineSmall = heading(22.sp, 28.sp),
    titleLarge = heading(20.sp, 26.sp),
    titleMedium = heading(17.sp, 24.sp),
    titleSmall = heading(15.sp, 20.sp),
    bodyLarge = body(16.sp, 24.sp),
    bodyMedium = body(14.sp, 20.sp),
    bodySmall = body(12.sp, 16.sp),
    labelLarge = body(14.sp, 20.sp, FontWeight.Medium),
    labelMedium = body(12.sp, 16.sp, FontWeight.Medium),
    labelSmall = body(11.sp, 14.sp, FontWeight.Medium),
)
