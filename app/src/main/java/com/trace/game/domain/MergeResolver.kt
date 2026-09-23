package com.trace.game.domain

/**
 * Resolves merge commits: compute result exp, clear path cells, place result,
 * apply gravity, and refill empty cells from the spawn pool.
 */
object MergeResolver {

    /**
     * resultExp = floor(log2(sum of 2^exp for each tile)) —
     * i.e. the highest power of two less than or equal to the sum.
     *
     * Implemented via binary carry so large exponents never overflow [Long].
     */
    fun resultExp(exps: List<Int>): Int {
        require(exps.isNotEmpty()) { "exps must not be empty" }
        val counts = sortedMapOf<Int, Int>()
        for (e in exps) {
            counts[e] = (counts[e] ?: 0) + 1
        }
        var e = counts.firstKey()
        while (true) {
            val c = counts[e] ?: 0
            if (c >= 2) {
                counts[e] = c % 2
                counts[e + 1] = (counts[e + 1] ?: 0) + c / 2
            }
            val nextKeys = counts.tailMap(e + 1)
            val hasHigher = nextKeys.any { it.value > 0 }
            if (!hasHigher) break
            e++
        }
        return counts.filterValues { it > 0 }.keys.maxOrNull()
            ?: error("merge produced empty counts")
    }

    fun resultExpForPath(board: Board, path: List<Cell>): Int {
        val exps = path.map { cell ->
            board[cell]?.exp ?: error("path cell empty: $cell")
        }
        return resultExp(exps)
    }

    fun applyMerge(
        board: Board,
        path: List<Cell>,
        result: Int,
        minSpawnExp: Int,
        maxExpEver: Int,
        random: kotlin.random.Random,
    ): MergeOutcome {
        require(path.size >= 2) { "merge path must have >= 2 cells" }
        val next = board.copy()
        for (cell in path) {
            next[cell] = null
        }
        val last = path.last()
        next[last] = Tile(result)

        val gravity = applyGravity(next, track = last)
        val resultCell = gravity.tracked ?: last

        var minSpawn = minSpawnExp
        var maxEver = maxOf(maxExpEver, result)
        minSpawn = SpawnPool.retire(minSpawn, maxEver)

        val spawned = spawnFill(next, minSpawn, random)
        val boardMax = next.maxExpOrNull() ?: maxEver
        maxEver = maxOf(maxEver, boardMax)
        minSpawn = SpawnPool.retire(minSpawn, maxEver)

        return MergeOutcome(
            board = next,
            resultExp = result,
            lastCell = last,
            resultCell = resultCell,
            gravityMoves = gravity.moves,
            spawnedCells = spawned,
            minSpawnExp = minSpawn,
            maxExpEver = maxEver,
        )
    }

    /**
     * Tiles fall toward higher row indices within each column.
     * Returns where [track] landed (if set) and every from→to slide.
     */
    fun applyGravity(board: Board, track: Cell? = null): GravityResult {
        var tracked: Cell? = null
        val moves = ArrayList<Pair<Cell, Cell>>()
        for (c in 0 until Grid.COLS) {
            var writeRow = Grid.ROWS - 1
            for (r in Grid.ROWS - 1 downTo 0) {
                val tile = board[c, r] ?: continue
                board[c, r] = null
                board[c, writeRow] = tile
                val from = Cell(c, r)
                val to = Cell(c, writeRow)
                if (r != writeRow) {
                    moves += from to to
                }
                if (track != null && track.col == c && track.row == r) {
                    tracked = to
                }
                writeRow--
            }
        }
        return GravityResult(tracked = tracked, moves = moves)
    }

    /** Convenience for callers that only need the tracked landing cell. */
    fun applyGravityTracking(board: Board, track: Cell? = null): Cell? =
        applyGravity(board, track).tracked

    fun spawnFill(
        board: Board,
        minSpawnExp: Int,
        random: kotlin.random.Random,
    ): List<Cell> {
        val spawned = ArrayList<Cell>()
        for (c in 0 until Grid.COLS) {
            for (r in 0 until Grid.ROWS) {
                if (board[c, r] == null) {
                    val exp = SpawnPool.roll(minSpawnExp, random)
                    board[c, r] = Tile(exp)
                    spawned.add(Cell(c, r))
                }
            }
        }
        return spawned
    }

    data class GravityResult(
        val tracked: Cell?,
        val moves: List<Pair<Cell, Cell>>,
    )

    data class MergeOutcome(
        val board: Board,
        val resultExp: Int,
        val lastCell: Cell,
        val resultCell: Cell,
        val gravityMoves: List<Pair<Cell, Cell>>,
        val spawnedCells: List<Cell>,
        val minSpawnExp: Int,
        val maxExpEver: Int,
    )
}
