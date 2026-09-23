package com.trace.game.domain

/**
 * Path validation for TRACE.
 *
 * - 8-way adjacency
 * - Path cannot revisit a cell
 * - Touching [path]\[len-2] backtracks (pops the last cell)
 * - Must start with a run of matching exps; after that run reaches length >= 2,
 *   the path may ascend to exp+1, then continue same or keep ascending by +1
 *   at each subsequent tier (ascended tiers may be length 1).
 */
object PathRules {

    /**
     * Whether [candidate] can be appended to [path] given [board] tile exps.
     * Does not handle backtracking — caller should check [isBacktrack] first.
     */
    fun canExtend(board: Board, path: List<Cell>, candidate: Cell): Boolean {
        if (board[candidate] == null) return false
        if (path.isEmpty()) return true
        if (path.contains(candidate)) return false
        val last = path.last()
        if (!areAdjacent8(last, candidate)) return false
        val exps = ArrayList<Int>(path.size + 1)
        for (cell in path) {
            val tile = board[cell] ?: return false
            exps.add(tile.exp)
        }
        exps.add(board[candidate]!!.exp)
        return isValidExpSequence(exps)
    }

    /** True when [candidate] is the cell before the tip (undo one step). */
    fun isBacktrack(path: List<Cell>, candidate: Cell): Boolean =
        path.size >= 2 && path[path.size - 2] == candidate

    /**
     * Apply a move onto [path]: backtrack, extend, or ignore.
     * Returns the new path (possibly unchanged).
     */
    fun applyMove(board: Board, path: List<Cell>, candidate: Cell): List<Cell> {
        if (path.isEmpty()) {
            return if (board[candidate] != null) listOf(candidate) else path
        }
        if (isBacktrack(path, candidate)) {
            return path.dropLast(1)
        }
        if (canExtend(board, path, candidate)) {
            return path + candidate
        }
        return path
    }

    fun startPath(board: Board, cell: Cell): List<Cell> =
        if (board[cell] != null) listOf(cell) else emptyList()

    /**
     * Validates the exponent sequence along a path.
     *
     * Rules:
     * 1. Leading run of identical exps (building a path of length 1 is allowed).
     * 2. To leave the leading run, it must have length >= 2, and the next exp
     *    must be exactly leadingExp + 1.
     * 3. After ascending, further tiles must equal the current tier or step
     *    exactly +1 to the next tier (no skipping). Ascended tiers may have
     *    length 1 before ascending again.
     */
    fun isValidExpSequence(exps: List<Int>): Boolean {
        if (exps.isEmpty()) return true
        var i = 0
        val startExp = exps[0]
        while (i < exps.size && exps[i] == startExp) i++
        if (i == exps.size) return true
        if (i < 2) return false

        var currentExp = startExp
        // i points at first tile after the opening run — must be startExp + 1
        while (i < exps.size) {
            val next = exps[i]
            when {
                next == currentExp -> {
                    // continue current tier (only reachable after an ascent set currentExp)
                    i++
                }
                next == currentExp + 1 -> {
                    // For the first ascent, opening run already ensured >= 2.
                    // For later ascents, a single tile at currentExp is enough.
                    currentExp = next
                    i++
                }
                else -> return false
            }
        }
        return true
    }

    /** True when the path is long enough to commit a merge. */
    fun canCommit(path: List<Cell>): Boolean = path.size >= 2
}
