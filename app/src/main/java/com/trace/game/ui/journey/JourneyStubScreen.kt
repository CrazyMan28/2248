package com.trace.game.ui.journey

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.trace.game.ui.theme.BgDeep
import com.trace.game.ui.theme.BgLift
import com.trace.game.ui.theme.BgMid
import com.trace.game.ui.theme.CtaFrom
import com.trace.game.ui.theme.Ink
import com.trace.game.ui.theme.InkDim
import com.trace.game.ui.theme.TraceTypography

@Composable
fun JourneyStubScreen(
    level: Int,
    onBack: () -> Unit,
    onUiTap: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BgDeep, BgMid)))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onUiTap(); onBack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Ink)
            }
            Text("Journey", style = TraceTypography.headlineMedium)
        }
        Text(
            "Campaign map stub — endless climb for now.",
            style = TraceTypography.bodyMedium,
            color = InkDim,
            modifier = Modifier.padding(vertical = 8.dp),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            for (n in 1..12) {
                val unlocked = n <= level.coerceAtLeast(1)
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (unlocked) CtaFrom else BgLift)
                        .then(
                            if (n == level) Modifier.border(3.dp, Ink, CircleShape) else Modifier,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("$n", style = TraceTypography.titleMedium, color = Ink)
                }
                if (n < 12) {
                    Box(
                        modifier = Modifier
                            .size(width = 4.dp, height = 28.dp)
                            .background(if (unlocked) CtaFrom.copy(alpha = 0.6f) else BgLift),
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
