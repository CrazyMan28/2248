package com.trace.game.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

private val TraceDarkColors = darkColorScheme(
    primary = CtaFrom,
    onPrimary = Ink,
    secondary = Goal,
    onSecondary = BgDeep,
    tertiary = Gem,
    background = BgDeep,
    onBackground = Ink,
    surface = BgMid,
    onSurface = Ink,
    surfaceVariant = BgLift,
    onSurfaceVariant = InkDim,
    error = Danger,
)

data class TraceMotionPrefs(val reduceMotion: Boolean = false)

val LocalReduceMotion = staticCompositionLocalOf { TraceMotionPrefs() }

@Composable
fun TraceTheme(
    reduceMotion: Boolean = false,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalReduceMotion provides TraceMotionPrefs(reduceMotion)) {
        MaterialTheme(
            colorScheme = TraceDarkColors,
            typography = TraceTypography,
            content = content,
        )
    }
}

