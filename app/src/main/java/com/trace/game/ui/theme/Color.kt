package com.trace.game.ui.theme

import androidx.compose.ui.graphics.Color

val BgDeep = Color(0xFF061820)
val BgMid = Color(0xFF0C2A36)
val BgLift = Color(0xFF143844)
val Ink = Color(0xFFF2F0E8)
val InkDim = Color(0xFF9BB0B8)
val CtaFrom = Color(0xFFE07A3D)
val CtaTo = Color(0xFFB84E1F)
val Gem = Color(0xFF3DDC97)
val Goal = Color(0xFF5EC8D8)
val Lock = Color(0xFF6A7A82)
val Danger = Color(0xFFE85D4C)
val PathCopper = Color(0xFFE07A3D)

/** Infinite tile palette driven by exponent. */
fun tileColor(exp: Int): Color {
    val k = exp.toFloat()
    val h = (210f + k * 37f) % 360f
    val s = (0.55f + 0.25f * kotlin.math.sin(k * 0.7).toFloat()).coerceIn(0.45f, 0.85f)
    val l = (0.42f + 0.08f * kotlin.math.cos(k * 0.5).toFloat()).coerceIn(0.32f, 0.58f)
    return Color.hsl(h, s, l)
}
