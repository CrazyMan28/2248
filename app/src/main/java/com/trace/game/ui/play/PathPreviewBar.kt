package com.trace.game.ui.play

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.trace.game.domain.formatExp
import com.trace.game.ui.theme.BgLift
import com.trace.game.ui.theme.Ink
import com.trace.game.ui.theme.InkDim
import com.trace.game.ui.theme.TraceTypography
import com.trace.game.ui.theme.tileColor

@Composable
fun PathPreviewBar(
    previewExp: Int?,
    pathLen: Int,
    modifier: Modifier = Modifier,
) {
    val visible = previewExp != null && pathLen >= 2
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            val exp = previewExp ?: return@AnimatedVisibility
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(BgLift)
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "Becomes",
                    style = TraceTypography.labelMedium,
                    color = InkDim,
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(tileColor(exp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = formatExp(exp),
                        style = TraceTypography.labelMedium,
                        color = Ink,
                    )
                }
                Text(
                    text = "· $pathLen",
                    style = TraceTypography.labelMedium,
                    color = InkDim,
                )
            }
        }
    }
}
