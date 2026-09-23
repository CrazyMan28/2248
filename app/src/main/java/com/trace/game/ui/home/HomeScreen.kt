package com.trace.game.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.trace.game.domain.formatExp
import com.trace.game.ui.components.GemBalance
import com.trace.game.ui.play.TraceActionButton
import com.trace.game.ui.theme.BgDeep
import com.trace.game.ui.theme.BgLift
import com.trace.game.ui.theme.BgMid
import com.trace.game.ui.theme.CtaFrom
import com.trace.game.ui.theme.Goal
import com.trace.game.ui.theme.Ink
import com.trace.game.ui.theme.InkDim
import com.trace.game.ui.theme.TraceTypography

@Composable
fun HomeScreen(
    gems: Int,
    highExp: Int,
    onPlay: () -> Unit,
    onJourney: () -> Unit,
    onShop: () -> Unit,
    onSettings: () -> Unit,
    onUiTap: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BgDeep, BgMid, Color(0xFF082028)))),
    ) {
        ThreadMesh(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GemBalance(gems = gems, onAdd = {
                    onUiTap()
                    onShop()
                })
                Text(
                    text = "Best ${formatExp(highExp)}",
                    style = TraceTypography.labelMedium,
                    color = InkDim,
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "TRACE",
                        style = TraceTypography.displayLarge,
                        color = Ink,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Draw a path. Rise.",
                        style = TraceTypography.headlineMedium,
                        color = Ink,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Draw equals. Climb forever.",
                        style = TraceTypography.bodyMedium,
                        color = InkDim,
                    )
                    Spacer(Modifier.height(28.dp))
                    TraceActionButton(
                        label = "PLAY",
                        onClick = {
                            onUiTap()
                            onPlay()
                        },
                        modifier = Modifier.fillMaxWidth(0.85f),
                    )
                    TextButton(onClick = {
                        onUiTap()
                        onJourney()
                    }) {
                        Text("Journey", color = Goal, style = TraceTypography.titleMedium)
                    }
                }
            }

            BottomRail(
                onShop = { onUiTap(); onShop() },
                onRank = onUiTap,
                onHome = onUiTap,
                onProfile = onUiTap,
                onSettings = { onUiTap(); onSettings() },
            )
        }
    }
}

@Composable
private fun ThreadMesh(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val step = size.minDimension / 12f
        var y = 0f
        while (y < size.height) {
            val path = Path()
            path.moveTo(0f, y)
            var x = 0f
            var up = true
            while (x < size.width) {
                val ny = y + if (up) step * 0.35f else -step * 0.35f
                path.lineTo(x + step, ny)
                x += step
                up = !up
            }
            drawPath(
                path,
                color = Goal.copy(alpha = 0.08f),
                style = Stroke(width = 2f, cap = StrokeCap.Round),
            )
            y += step
        }
        // Dominant path accent
        val accent = Path()
        accent.moveTo(size.width * 0.1f, size.height * 0.72f)
        accent.quadraticBezierTo(
            size.width * 0.45f,
            size.height * 0.55f,
            size.width * 0.85f,
            size.height * 0.35f,
        )
        drawPath(
            accent,
            color = CtaFrom.copy(alpha = 0.45f),
            style = Stroke(width = 8f, cap = StrokeCap.Round),
        )
        drawCircle(CtaFrom.copy(alpha = 0.7f), radius = 10f, center = Offset(size.width * 0.85f, size.height * 0.35f))
    }
}

@Composable
private fun BottomRail(
    onShop: () -> Unit,
    onRank: () -> Unit,
    onHome: () -> Unit,
    onProfile: () -> Unit,
    onSettings: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BgLift.copy(alpha = 0.9f))
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RailItem(Icons.Default.ShoppingCart, "Shop", onShop)
        RailItem(Icons.Default.EmojiEvents, "Rank", onRank, dimmed = true)
        RailItem(Icons.Default.Home, "Home", onHome, active = true)
        RailItem(Icons.Default.Person, "Profile", onProfile, dimmed = true)
        RailItem(Icons.Default.Settings, "Settings", onSettings)
    }
}

@Composable
private fun RailItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    active: Boolean = false,
    dimmed: Boolean = false,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = when {
                active -> CtaFrom
                dimmed -> InkDim.copy(alpha = 0.5f)
                else -> InkDim
            },
            modifier = Modifier.size(22.dp),
        )
        Text(
            label,
            style = TraceTypography.labelMedium,
            color = if (active) CtaFrom else InkDim,
        )
    }
}
