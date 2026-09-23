package com.trace.game.ui.shop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.trace.game.ui.components.GemBalance
import com.trace.game.ui.theme.BgDeep
import com.trace.game.ui.theme.BgLift
import com.trace.game.ui.theme.BgMid
import com.trace.game.ui.theme.CtaFrom
import com.trace.game.ui.theme.CtaTo
import com.trace.game.ui.theme.Ink
import com.trace.game.ui.theme.InkDim
import com.trace.game.ui.theme.TraceTypography

private data class GemPack(val gems: Int, val label: String)

@Composable
fun ShopScreen(
    gems: Int,
    onBuy: (Int) -> Unit,
    onBack: () -> Unit,
    onUiTap: () -> Unit,
) {
    val packs = listOf(
        GemPack(50, "Spark pack"),
        GemPack(150, "Thread pack"),
        GemPack(400, "Cascade pack"),
    )

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
            Text("Shop", style = TraceTypography.headlineMedium, modifier = Modifier.weight(1f))
            GemBalance(gems = gems)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Local gems for now — no real purchases in v1.",
            style = TraceTypography.bodyMedium,
            color = InkDim,
        )
        Spacer(Modifier.height(20.dp))
        packs.forEach { pack ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(BgLift)
                    .clickable {
                        onUiTap()
                        onBuy(pack.gems)
                    }
                    .padding(horizontal = 16.dp, vertical = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(pack.label, style = TraceTypography.titleMedium, color = Ink)
                    Text("+${pack.gems} gems", style = TraceTypography.bodyMedium)
                }
                Text(
                    "GET",
                    style = TraceTypography.labelLarge,
                    color = CtaFrom,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Brush.horizontalGradient(listOf(CtaFrom.copy(alpha = 0.2f), CtaTo.copy(alpha = 0.2f))))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
        }
    }
}
