package com.trace.game.ui.play

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.PointerEventPass
import com.trace.game.domain.Board
import com.trace.game.domain.Cell
import com.trace.game.domain.Grid
import com.trace.game.domain.formatExp
import com.trace.game.ui.theme.Ink
import com.trace.game.ui.theme.LocalReduceMotion
import com.trace.game.ui.theme.PathCopper
import com.trace.game.ui.theme.tileColor
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.random.Random

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
    val burst = remember { Animatable(0f) }
    var burstCell by remember { mutableStateOf<Cell?>(null) }
    var burstColor by remember { mutableStateOf(PathCopper) }

    // Keep latest callbacks without restarting the pointer handler mid-drag.
    val onDownState = rememberUpdatedState(onDown)
    val onMoveState = rememberUpdatedState(onMove)
    val onUpState = rememberUpdatedState(onUp)

    LaunchedEffect(mergeFlash) {
        if (mergeFlash == null) {
            flash.snapTo(0f)
            return@LaunchedEffect
        }
        burstCell = mergeFlash
        burstColor = tileColor(board[mergeFlash]?.exp ?: 1)
        if (reduce) {
            flash.snapTo(1f)
            flash.snapTo(0f)
            burst.snapTo(1f)
            burst.snapTo(0f)
        } else {
            flash.snapTo(0f)
            flash.animateTo(1f, tween(120))
            flash.animateTo(0f, spring())
            burst.snapTo(0f)
            burst.animateTo(1f, tween(280, easing = LinearEasing))
            burst.snapTo(0f)
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
            // IMPORTANT: do NOT key on `path` — that cancels the gesture after the first tile.
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val w = size.width.toFloat()
                    val h = size.height.toFloat()
                    val start = nearestCell(down.position, w, h)
                    var last: Cell? = start
                    if (start != null) {
                        onDownState.value(start)
                    }
                    down.consume()

                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        val change = event.changes.firstOrNull { it.id == down.id }
                            ?: event.changes.firstOrNull()
                            ?: break

                        if (change.changedToUp()) {
                            change.consume()
                            onUpState.value()
                            break
                        }

                        val cell = nearestCell(change.position, w, h)
                        if (cell != null && cell != last) {
                            // Walk intermediate cells when the finger jumps (fast swipe).
                            val from = last
                            if (from != null) {
                                for (step in cellsBetween(from, cell)) {
                                    if (step != last) {
                                        onMoveState.value(step)
                                        last = step
                                    }
                                }
                            }
                            if (cell != last) {
                                onMoveState.value(cell)
                                last = cell
                            }
                        }
                        change.consume()
                    }
                }
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

        val bc = burstCell
        val t = burst.value
        if (bc != null && t > 0f && t < 1f && !reduce) {
            val center = cellCenter(bc)
            val shards = 14
            val rnd = Random(bc.col * 31 + bc.row)
            for (i in 0 until shards) {
                val angle = (i / shards.toFloat()) * Math.PI.toFloat() * 2f + rnd.nextFloat()
                val dist = cellW * (0.4f + t * 1.2f)
                val px = center.x + cos(angle) * dist
                val py = center.y + sin(angle) * dist
                val shard = cellW * 0.12f * (1f - t)
                drawRect(
                    color = burstColor.copy(alpha = 1f - t),
                    topLeft = Offset(px - shard / 2f, py - shard / 2f),
                    size = Size(shard, shard),
                )
            }
        }
    }
}

/**
 * Hit-test that includes gaps between tiles — maps finger to nearest cell center
 * so dragging feels continuous instead of dying in the gutters.
 */
private fun nearestCell(offset: Offset, width: Float, height: Float): Cell? {
    if (width <= 0f || height <= 0f) return null
    val gap = minOf(width, height) * 0.02f
    val cellW = (width - gap * (Grid.COLS + 1)) / Grid.COLS
    val cellH = (height - gap * (Grid.ROWS + 1)) / Grid.ROWS
    if (cellW <= 0f || cellH <= 0f) return null

    val colF = (offset.x - gap - cellW / 2f) / (cellW + gap)
    val rowF = (offset.y - gap - cellH / 2f) / (cellH + gap)
    val c = colF.roundToGrid(Grid.COLS)
    val r = rowF.roundToGrid(Grid.ROWS)
    return Cell.orNull(c, r)
}

private fun Float.roundToGrid(count: Int): Int =
    floor(this + 0.5f).toInt().coerceIn(0, count - 1)

/** Bresenham-style cells between [from] and [to], excluding [from], including [to]. */
private fun cellsBetween(from: Cell, to: Cell): List<Cell> {
    val dc = to.col - from.col
    val dr = to.row - from.row
    val steps = maxOf(kotlin.math.abs(dc), kotlin.math.abs(dr))
    if (steps <= 1) return listOf(to)
    val out = ArrayList<Cell>(steps)
    for (i in 1..steps) {
        val c = from.col + (dc * i) / steps
        val r = from.row + (dr * i) / steps
        Cell.orNull(c, r)?.let { out.add(it) }
    }
    return out
}
