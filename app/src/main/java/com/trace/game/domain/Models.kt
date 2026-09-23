package com.trace.game.domain

/** Grid is 5 columns × 8 rows. Tile values are conceptual powers of two: value = 2^exp. */
object Grid {
    const val COLS = 5
    const val ROWS = 8
    const val CELL_COUNT = COLS * ROWS
}

data class Cell(val col: Int, val row: Int) {
    init {
        require(col in 0 until Grid.COLS) { "col out of range: $col" }
        require(row in 0 until Grid.ROWS) { "row out of range: $row" }
    }

    fun isValid(): Boolean = col in 0 until Grid.COLS && row in 0 until Grid.ROWS

    companion object {
        fun orNull(col: Int, row: Int): Cell? =
            if (col in 0 until Grid.COLS && row in 0 until Grid.ROWS) Cell(col, row) else null
    }
}

/** A tile stores [exp] where the displayed value is conceptually 2^[exp] (exp 1 → "2"). */
data class Tile(val exp: Int) {
    init {
        require(exp >= 1) { "exp must be >= 1, was $exp" }
    }
}

/**
 * Mutable board indexed as [row][col]. Null means empty.
 * Gravity pulls tiles toward higher row indices (down).
 */
class Board(private val cells: Array<Array<Tile?>> = Array(Grid.ROWS) { arrayOfNulls(Grid.COLS) }) {

    operator fun get(cell: Cell): Tile? = cells[cell.row][cell.col]
    operator fun get(col: Int, row: Int): Tile? = cells[row][col]

    operator fun set(cell: Cell, tile: Tile?) {
        cells[cell.row][cell.col] = tile
    }

    operator fun set(col: Int, row: Int, tile: Tile?) {
        cells[row][col] = tile
    }

    fun copy(): Board {
        val next = Board()
        for (r in 0 until Grid.ROWS) {
            for (c in 0 until Grid.COLS) {
                next.cells[r][c] = cells[r][c]
            }
        }
        return next
    }

    fun forEachOccupied(action: (Cell, Tile) -> Unit) {
        for (r in 0 until Grid.ROWS) {
            for (c in 0 until Grid.COLS) {
                val tile = cells[r][c] ?: continue
                action(Cell(c, r), tile)
            }
        }
    }

    fun occupiedCells(): List<Cell> {
        val out = ArrayList<Cell>(Grid.CELL_COUNT)
        forEachOccupied { cell, _ -> out.add(cell) }
        return out
    }

    fun maxExpOrNull(): Int? {
        var max: Int? = null
        forEachOccupied { _, tile ->
            max = maxOf(max ?: tile.exp, tile.exp)
        }
        return max
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Board) return false
        for (r in 0 until Grid.ROWS) {
            for (c in 0 until Grid.COLS) {
                if (cells[r][c] != other.cells[r][c]) return false
            }
        }
        return true
    }

    override fun hashCode(): Int {
        var result = 1
        for (r in 0 until Grid.ROWS) {
            for (c in 0 until Grid.COLS) {
                result = 31 * result + (cells[r][c]?.exp ?: 0)
            }
        }
        return result
    }
}

data class RunState(
    val board: Board,
    val path: List<Cell> = emptyList(),
    val gems: Int = 0,
    val level: Int = 1,
    val goalExp: Int = LEVEL_1_GOAL_EXP,
    val minSpawnExp: Int = 1,
    val maxExpEver: Int = 1,
    val noMoves: Boolean = false,
) {
    companion object {
        /** Level 1 goal is exp 9 → 512. */
        const val LEVEL_1_GOAL_EXP = 9
    }
}

sealed class InputEvent {
    data class Down(val cell: Cell) : InputEvent()
    data class Move(val cell: Cell) : InputEvent()
    data object Up : InputEvent()
    data class UseShatter(val cell: Cell) : InputEvent()
    data class UseAlign(val targetExp: Int) : InputEvent()
    data object NewGame : InputEvent()
}

sealed class FeedbackEvent {
    data class PathUpdated(val path: List<Cell>) : FeedbackEvent()
    data class MergeCommitted(
        val resultExp: Int,
        val path: List<Cell>,
        val lastCell: Cell,
    ) : FeedbackEvent()
    data class Combo(val pathLen: Int, val gemsGained: Int) : FeedbackEvent()
    data class LevelUp(val newLevel: Int, val newGoalExp: Int, val gemsGained: Int) : FeedbackEvent()
    data class Spawned(val cells: List<Cell>) : FeedbackEvent()
    data object NoMoves : FeedbackEvent()
    data class ShatterUsed(val cell: Cell) : FeedbackEvent()
    data class AlignUsed(val targetExp: Int, val changed: List<Cell>) : FeedbackEvent()
    data object Rejected : FeedbackEvent()
}

data class EngineResult(
    val state: RunState,
    val feedback: List<FeedbackEvent> = emptyList(),
)

/**
 * Display helper for an exponent.
 * - If 2^exp fits in [Long] and the value is &lt; 10000, show the decimal number.
 * - Otherwise a compact form (K/M/B/T) when it still fits in [Long], else `"2^n"`.
 */
fun formatExp(exp: Int): String {
    require(exp >= 0) { "exp must be >= 0" }
    if (exp <= 62) {
        val value = 1L shl exp
        if (value < 10_000L) return value.toString()
        return compactLong(value)
    }
    return "2^$exp"
}

private fun compactLong(value: Long): String {
    // Powers-of-two tiles compact cleanly with binary scales (1024).
    val units = listOf(
        1L shl 40 to "T",
        1L shl 30 to "B",
        1L shl 20 to "M",
        1L shl 10 to "K",
    )
    for ((div, suffix) in units) {
        if (value >= div) {
            val whole = value / div
            val rem = value % div
            return if (rem == 0L) {
                "$whole$suffix"
            } else {
                val tenths = (rem * 10) / div
                if (tenths == 0L) "$whole$suffix" else "$whole.${tenths}$suffix"
            }
        }
    }
    return value.toString()
}

/** Eight neighbor offsets (including diagonals). */
val NEIGHBOR_OFFSETS: List<Pair<Int, Int>> = listOf(
    -1 to -1, 0 to -1, 1 to -1,
    -1 to 0, 1 to 0,
    -1 to 1, 0 to 1, 1 to 1,
)

fun Cell.neighbors8(): List<Cell> {
    val out = ArrayList<Cell>(8)
    for ((dc, dr) in NEIGHBOR_OFFSETS) {
        val n = Cell.orNull(col + dc, row + dr) ?: continue
        out.add(n)
    }
    return out
}

fun areAdjacent8(a: Cell, b: Cell): Boolean {
    val dc = kotlin.math.abs(a.col - b.col)
    val dr = kotlin.math.abs(a.row - b.row)
    return dc <= 1 && dr <= 1 && (dc != 0 || dr != 0)
}
