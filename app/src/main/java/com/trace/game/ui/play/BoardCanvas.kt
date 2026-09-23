package com.trace.game.ui.play

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.trace.game.domain.Board
import com.trace.game.domain.Cell
import com.trace.game.domain.Grid
import com.trace.game.domain.formatExp
import com.trace.game.ui.theme.Ink
import com.trace.game.ui.theme.LocalReduceMotion
import com.trace.game.ui.theme.PathCopper
import com.trace.game.ui.theme.tileColor

@Composable
fun BoardCanvas(
    board: Board,
    path: List<Cell>,
    mergeFlash: Cell?,
    dimNonPath: Boolean,
    onDown: (Cell) -> Unit,
    onMove: (Cell) -> Unit,
    onUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reduce = LocalReduceMotion.current.reduceMotion
    val flash = remember { Animatable(0f) }
    LaunchedEffect(mergeFlash) {
        if (mergeFlash == null) {
            flash.snapTo(0f)
        } else if (reduce) {
            flash.snapTo(1f)
            flash.snapTo(0f)
        } else {
            flash.snapTo(0f)
            flash.animateTo(1f, tween(120))
            flash.animateTo(0f, spring())
        }
    }

    val density = LocalDensity.current
    val textSizePx = with(density) { 18.dp.toPx() }
    val paint = remember {
        android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textAlign = android.graphics.Paint.Align.CENTER
            isFakeBoldText = true
        }
    }
    paint.textSize = textSizePx

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(Grid.COLS.toFloat() / Grid.ROWS.toFloat())
            .pointerInput(board, path) {
                detectDragGestures(
                    onDragStart = { offset ->
                        cellAt(offset, size.width.toFloat(), size.height.toFloat())?.let(onDown)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        cellAt(change.position, size.width.toFloat(), size.height.toFloat())?.let(onMove)
                    },
                    onDragEnd = { onUp() },
                    onDragCancel = { onUp() },
                )
            },
    ) {
        val gap = size.minDimension * 0.02f
        val cellW = (size.width - gap * (Grid.COLS + 1)) / Grid.COLS
        val cellH = (size.height - gap * (Grid.ROWS + 1)) / Grid.ROWS
        val radius = CornerRadius(cellW * 0.22f, cellH * 0.22f)

        fun cellOrigin(c: Int, r: Int): Offset =
            Offset(gap + c * (cellW + gap), gap + r * (cellH + gap))

        fun cellCenter(cell: Cell): Offset {
            val o = cellOrigin(cell.col, cell.row)
            return Offset(o.x + cellW / 2f, o.y + cellH / 2f)
        }

        val pathSet = path.toSet()

        for (r in 0 until Grid.ROWS) {
            for (c in 0 until Grid.COLS) {
                val tile = board[c, r] ?: continue
                val cell = Cell(c, r)
                val origin = cellOrigin(c, r)
                val base = tileColor(tile.exp)
                val alpha = when {
                    path.isEmpty() -> 1f
                    pathSet.contains(cell) -> 1f
                    dimNonPath -> 0.35f
                    else -> 1f
                }
                val scaleBoost = if (mergeFlash == cell) flash.value * 0.12f else 0f
                val inset = cellW * scaleBoost * -0.5f
                drawRoundRect(
                    color = base.copy(alpha = alpha),
                    topLeft = Offset(origin.x + inset, origin.y + inset),
                    size = Size(cellW - inset * 2, cellH - inset * 2),
                    cornerRadius = radius,
                )
                if (pathSet.contains(cell)) {
                    drawRoundRect(
                        color = Ink.copy(alpha = 0.85f),
                        topLeft = origin,
                        size = Size(cellW, cellH),
                        cornerRadius = radius,
                        style = Stroke(width = 3f),
                    )
                }
                val label = formatExp(tile.exp)
                drawContext.canvas.nativeCanvas.drawText(
                    label,
                    origin.x + cellW / 2f,
                    origin.y + cellH / 2f - (paint.ascent() + paint.descent()) / 2f,
                    paint.apply { this.alpha = (255 * alpha).toInt() },
                )
            }
        }

        if (path.size >= 2) {
            val line = Path()
            val first = cellCenter(path.first())
            line.moveTo(first.x, first.y)
            for (i in 1 until path.size) {
                val p = cellCenter(path[i])
                line.lineTo(p.x, p.y)
            }
            drawPath(
                path = line,
                color = PathCopper.copy(alpha = 0.9f),
                style = Stroke(width = cellW * 0.18f, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
            drawPath(
                path = line,
                color = Color.White.copy(alpha = 0.35f),
                style = Stroke(width = cellW * 0.06f, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
    }
}

private fun cellAt(offset: Offset, width: Float, height: Float): Cell? {
    val gap = minOf(width, height) * 0.02f
    val cellW = (width - gap * (Grid.COLS + 1)) / Grid.COLS
    val cellH = (height - gap * (Grid.ROWS + 1)) / Grid.ROWS
    val c = ((offset.x - gap) / (cellW + gap)).toInt()
    val r = ((offset.y - gap) / (cellH + gap)).toInt()
    return Cell.orNull(c, r)
}
