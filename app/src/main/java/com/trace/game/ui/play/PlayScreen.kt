package com.trace.game.ui.play

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.trace.game.domain.GameEngine
import com.trace.game.ui.components.GemBalance
import com.trace.game.ui.theme.BgDeep
import com.trace.game.ui.theme.BgLift
import com.trace.game.ui.theme.BgMid
import com.trace.game.ui.theme.CtaFrom
import com.trace.game.ui.theme.CtaTo
import com.trace.game.ui.theme.Ink
import com.trace.game.ui.theme.InkDim
import com.trace.game.ui.theme.Lock
import com.trace.game.ui.theme.TraceTypography

@Composable
fun PlayScreen(
    vm: GameViewModel,
    onExitHome: () -> Unit,
) {
    val state by vm.runState.collectAsState()
    val ui by vm.playUi.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BgDeep, BgMid, BgDeep))),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GemBalance(gems = state.gems)
                Text(
                    text = "Lv ${state.level}",
                    style = TraceTypography.titleMedium,
                    color = Ink,
                )
            }

            Spacer(Modifier.height(10.dp))

            GoalRail(
                minExp = state.minSpawnExp,
                currentExp = state.maxExpEver,
                goalExp = state.goalExp,
            )

            Spacer(Modifier.height(12.dp))

            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                BoardCanvas(
                    board = state.board,
                    path = state.path,
                    mergeFx = ui.mergeFx,
                    dimNonPath = state.path.isNotEmpty(),
                    onDown = vm::onDown,
                    onMove = vm::onMove,
                    onUp = vm::onUp,
                    modifier = Modifier.fillMaxWidth(),
                )

                if (ui.comboBanner != null) {
                    Text(
                        text = "Combo x${ui.comboBanner}",
                        style = TraceTypography.headlineMedium,
                        color = CtaFrom,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(12.dp),
                    )
                }

                if (state.noMoves) {
                    Text(
                        text = "No moves left",
                        style = TraceTypography.headlineMedium,
                        color = Ink,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .background(BgDeep.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                            .padding(16.dp),
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            PowerRow(
                level = state.level,
                gems = state.gems,
                powerMode = ui.powerMode,
                onShatter = vm::armShatter,
                onAlign = vm::armAlign,
                onPause = vm::togglePause,
            )
        }

        if (ui.showTutorial) {
            TutorialOverlay(step = ui.tutorialStep, onNext = vm::advanceTutorial)
        }

        if (ui.paused) {
            PauseOverlay(
                onResume = vm::resume,
                onRestart = vm::restart,
                onHome = {
                    vm.resume()
                    vm.saveAndExit { onExitHome() }
                },
            )
        }
    }
}

@Composable
private fun PowerRow(
    level: Int,
    gems: Int,
    powerMode: PowerMode,
    onShatter: () -> Unit,
    onAlign: () -> Unit,
    onPause: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PowerSlot(
            unlocked = level >= GameEngine.SHATTER_MIN_LEVEL,
            lockLabel = "Level ${GameEngine.SHATTER_MIN_LEVEL}",
            active = powerMode == PowerMode.ShatterTarget,
            enabled = gems >= GameEngine.SHATTER_COST && level >= GameEngine.SHATTER_MIN_LEVEL,
            caption = "${GameEngine.SHATTER_COST}",
            onClick = onShatter,
        ) {
            Icon(Icons.Default.Whatshot, contentDescription = "Shatter", tint = Ink)
        }
        PowerSlot(
            unlocked = level >= GameEngine.ALIGN_MIN_LEVEL,
            lockLabel = "Level ${GameEngine.ALIGN_MIN_LEVEL}",
            active = powerMode == PowerMode.AlignPick,
            enabled = gems >= GameEngine.ALIGN_COST && level >= GameEngine.ALIGN_MIN_LEVEL,
            caption = "${GameEngine.ALIGN_COST}",
            onClick = onAlign,
        ) {
            Icon(Icons.Default.Bolt, contentDescription = "Align", tint = Ink)
        }
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(BgLift)
                .clickable(onClick = onPause),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.Pause, contentDescription = "Pause", tint = Ink)
        }
    }
}

@Composable
private fun PowerSlot(
    unlocked: Boolean,
    lockLabel: String,
    active: Boolean,
    enabled: Boolean,
    caption: String,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
) {
    val bg: Color = when {
        !unlocked -> BgLift
        active -> CtaFrom
        else -> BgLift
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(bg)
                .then(
                    if (active) Modifier.border(2.dp, Ink.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    else Modifier,
                )
                .clickable(enabled = unlocked && enabled, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            if (unlocked) icon() else Icon(Icons.Default.Lock, contentDescription = null, tint = Lock)
        }
        Text(
            text = if (unlocked) caption else lockLabel,
            style = TraceTypography.labelMedium,
            color = InkDim,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun PauseOverlay(
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onHome: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDeep.copy(alpha = 0.82f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .clip(RoundedCornerShape(20.dp))
                .background(BgMid)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Paused", style = TraceTypography.headlineMedium)
            TraceActionButton("Resume", onResume)
            TraceActionButton("Restart", onRestart, secondary = true)
            TextButton(onClick = onHome) {
                Text("Home", color = InkDim)
            }
        }
    }
}

@Composable
private fun TutorialOverlay(step: Int, onNext: () -> Unit) {
    val text = when (step) {
        0 -> "Connect matching blocks"
        1 -> "Once you double, keep going"
        2 -> "Link any of 8 directions"
        else -> "Reach the GOAL — climb forever"
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDeep.copy(alpha = 0.55f))
            .clickable(onClick = onNext),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(24.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(BgMid)
                .padding(20.dp),
        ) {
            Text(
                text = text,
                style = TraceTypography.headlineMedium,
                textAlign = TextAlign.Center,
                color = Ink,
            )
            Spacer(Modifier.height(8.dp))
            Text("Tap to continue", style = TraceTypography.bodyMedium)
        }
    }
}

@Composable
fun TraceActionButton(
    label: String,
    onClick: () -> Unit,
    secondary: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val bgModifier = if (secondary) {
        Modifier.background(BgLift)
    } else {
        Modifier.background(Brush.horizontalGradient(listOf(CtaFrom, CtaTo)))
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .then(bgModifier)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = TraceTypography.labelLarge, color = Ink)
    }
}
