package com.trace.game.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.trace.game.R

val Bricolage = FontFamily(
    Font(R.font.bricolage_grotesque_semibold, FontWeight.SemiBold),
    Font(R.font.bricolage_grotesque_bold, FontWeight.Bold),
)

val Figtree = FontFamily(
    Font(R.font.figtree_regular, FontWeight.Normal),
    Font(R.font.figtree_medium, FontWeight.Medium),
)

val TraceTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = Bricolage,
        fontWeight = FontWeight.Bold,
        fontSize = 56.sp,
        lineHeight = 60.sp,
        letterSpacing = (-1).sp,
        color = Ink,
    ),
    headlineLarge = TextStyle(
        fontFamily = Bricolage,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        color = Ink,
    ),
    headlineMedium = TextStyle(
        fontFamily = Bricolage,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        color = Ink,
    ),
    titleLarge = TextStyle(
        fontFamily = Bricolage,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        color = Ink,
    ),
    titleMedium = TextStyle(
        fontFamily = Figtree,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        color = Ink,
    ),
    bodyLarge = TextStyle(
        fontFamily = Figtree,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        color = Ink,
    ),
    bodyMedium = TextStyle(
        fontFamily = Figtree,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        color = InkDim,
    ),
    labelLarge = TextStyle(
        fontFamily = Bricolage,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        color = Ink,
    ),
    labelMedium = TextStyle(
        fontFamily = Figtree,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        color = InkDim,
    ),
)
