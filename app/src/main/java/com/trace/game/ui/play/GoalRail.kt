package com.trace.game.ui.play

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.trace.game.domain.formatExp
import com.trace.game.ui.theme.BgLift
import com.trace.game.ui.theme.CtaFrom
import com.trace.game.ui.theme.Goal
import com.trace.game.ui.theme.Ink
import com.trace.game.ui.theme.LocalReduceMotion
import com.trace.game.ui.theme.TraceTypography
import com.trace.game.ui.theme.tileColor
import kotlin.math.roundToInt

@Composable
fun GoalRail(
    minExp: Int,
    currentExp: Int,
    goalExp: Int,
    modifier: Modifier = Modifier,
) {
    val reduce = LocalReduceMotion.current.reduceMotion
    val progress = ((currentExp - minExp).toFloat() / (goalExp - minExp).toFloat().coerceAtLeast(1f))
        .coerceIn(0f, 1f)
    val animated by animateFloatAsState(
        targetValue = progress,
        animationSpec = if (reduce) spring(stiffness = 10_000f) else spring(),
        label = "goal",
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(BgLift)
            .padding(horizontal = 4.dp, vertical = 4.dp),
    ) {
        val trackWidth = maxWidth - 72.dp
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 36.dp)
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(BgLift),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animated)
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Brush.horizontalGradient(listOf(CtaFrom, Goal))),
            )
        }

        TileChip(exp = minExp, modifier = Modifier.align(Alignment.CenterStart))

        val density = LocalDensity.current
        val x = with(density) { (trackWidth.toPx() * animated).roundToInt() }
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 36.dp)
                .offset { IntOffset(x, 0) },
        ) {
            TileChip(exp = currentExp, crowned = true)
        }

        Box(modifier = Modifier.align(Alignment.CenterEnd)) {
            ColumnGoal(exp = goalExp)
        }
    }
}

@Composable
private fun TileChip(exp: Int, crowned: Boolean = false, modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.TopCenter) {
        if (crowned) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = CtaFrom,
                modifier = Modifier
                    .size(12.dp)
                    .align(Alignment.TopCenter)
                    .offset(y = (-6).dp),
            )
        }
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(tileColor(exp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(formatExp(exp), style = TraceTypography.labelMedium, color = Ink)
        }
    }
}

@Composable
private fun ColumnGoal(exp: Int) {
    Box(contentAlignment = Alignment.TopCenter) {
        Text(
            text = "GOAL",
            style = TraceTypography.labelMedium,
            color = Ink,
            modifier = Modifier.offset(y = (-10).dp),
        )
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(tileColor(exp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(formatExp(exp), style = TraceTypography.labelMedium, color = Ink)
        }
    }
}
