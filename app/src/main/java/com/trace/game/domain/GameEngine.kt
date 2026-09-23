package com.trace.game.domain

import kotlin.random.Random

/**
 * Pure-Kotlin TRACE run loop. Holds [RunState], applies [InputEvent]s, and
 * emits [FeedbackEvent]s for haptics/sfx layers.
 */
class GameEngine(
    private val random: Random = Random.Default,
    initial: RunState? = null,
) {
    var state: RunState = initial ?: newGameState(random)
        private set

    fun apply(event: InputEvent): EngineResult {
        val feedback = mutableListOf<FeedbackEvent>()
        when (event) {
            is InputEvent.NewGame -> {
                state = newGameState(random)
                feedback += FeedbackEvent.Spawned(state.board.occupiedCells())
            }
            is InputEvent.Down -> onDown(event.cell, feedback)
            is InputEvent.Move -> onMove(event.cell, feedback)
            InputEvent.Up -> onUp(feedback)
            is InputEvent.UseShatter -> onShatter(event.cell, feedback)
            is InputEvent.UseAlign -> onAlign(event.targetExp, feedback)
        }
        return EngineResult(state, feedback.toList())
    }

    /** Replace gems without resetting the board (wallet sync from prefs). */
    fun setGems(gems: Int) {
        state = state.copy(gems = gems.coerceAtLeast(0))
    }

    fun replaceState(newState: RunState) {
        state = newState
    }

    private fun onDown(cell: Cell, feedback: MutableList<FeedbackEvent>) {
        if (state.noMoves) {
            feedback += FeedbackEvent.Rejected
            return
        }
        val path = PathRules.startPath(state.board, cell)
        if (path.isEmpty()) {
            feedback += FeedbackEvent.Rejected
            return
        }
        state = state.copy(path = path)
        feedback += FeedbackEvent.PathUpdated(path)
    }

    private fun onMove(cell: Cell, feedback: MutableList<FeedbackEvent>) {
        if (state.path.isEmpty()) return
        val nextPath = PathRules.applyMove(state.board, state.path, cell)
        if (nextPath != state.path) {
            state = state.copy(path = nextPath)
            feedback += FeedbackEvent.PathUpdated(nextPath)
        }
    }

    private fun onUp(feedback: MutableList<FeedbackEvent>) {
        val path = state.path
        if (path.isEmpty()) return
        if (!PathRules.canCommit(path)) {
            state = state.copy(path = emptyList())
            feedback += FeedbackEvent.PathUpdated(emptyList())
            return
        }

        val result = MergeResolver.resultExpForPath(state.board, path)
        val outcome = MergeResolver.applyMerge(
            board = state.board,
            path = path,
            result = result,
            minSpawnExp = state.minSpawnExp,
            maxExpEver = state.maxExpEver,
            random = random,
        )

        var gems = state.gems
        var level = state.level
        var goalExp = state.goalExp

        feedback += FeedbackEvent.MergeCommitted(result, path, outcome.resultCell)

        // +1 gem per merge, plus floor(pathLen/5) combo bonus
        val mergeGems = 1 + path.size / 5
        gems += mergeGems
        if (path.size >= 5) {
            feedback += FeedbackEvent.Combo(path.size, mergeGems)
        } else if (mergeGems > 0) {
            // still a gem gain without combo banner
        }

        if (result >= goalExp) {
            level += 1
            goalExp += 1
            gems += LEVEL_CLEAR_GEMS
            feedback += FeedbackEvent.LevelUp(level, goalExp, LEVEL_CLEAR_GEMS)
        }

        if (outcome.spawnedCells.isNotEmpty()) {
            feedback += FeedbackEvent.Spawned(outcome.spawnedCells)
        }

        val noMoves = detectNoMoves(outcome.board)
        state = RunState(
            board = outcome.board,
            path = emptyList(),
            gems = gems,
            level = level,
            goalExp = goalExp,
            minSpawnExp = outcome.minSpawnExp,
            maxExpEver = outcome.maxExpEver,
            noMoves = noMoves,
        )
        if (noMoves) feedback += FeedbackEvent.NoMoves
    }

    private fun onShatter(cell: Cell, feedback: MutableList<FeedbackEvent>) {
        if (state.level < SHATTER_MIN_LEVEL || state.gems < SHATTER_COST) {
            feedback += FeedbackEvent.Rejected
            return
        }
        if (state.board[cell] == null) {
            feedback += FeedbackEvent.Rejected
            return
        }

        val board = state.board.copy()
        board[cell] = null
        MergeResolver.applyGravity(board)

        var minSpawn = state.minSpawnExp
        var maxEver = state.maxExpEver
        minSpawn = SpawnPool.retire(minSpawn, maxEver)

        val spawned = MergeResolver.spawnFill(board, minSpawn, random)
        maxEver = maxOf(maxEver, board.maxExpOrNull() ?: maxEver)
        minSpawn = SpawnPool.retire(minSpawn, maxEver)

        val noMoves = detectNoMoves(board)
        state = state.copy(
            board = board,
            path = emptyList(),
            gems = state.gems - SHATTER_COST,
            minSpawnExp = minSpawn,
            maxExpEver = maxEver,
            noMoves = noMoves,
        )
        feedback += FeedbackEvent.ShatterUsed(cell)
        if (spawned.isNotEmpty()) feedback += FeedbackEvent.Spawned(spawned)
        if (noMoves) feedback += FeedbackEvent.NoMoves
    }

    private fun onAlign(targetExp: Int, feedback: MutableList<FeedbackEvent>) {
        if (state.level < ALIGN_MIN_LEVEL || state.gems < ALIGN_COST) {
            feedback += FeedbackEvent.Rejected
            return
        }

        val board = state.board.copy()
        val present = mutableListOf<Cell>()
        val others = mutableListOf<Cell>()
        board.forEachOccupied { cell, tile ->
            if (tile.exp == targetExp) present.add(cell) else others.add(cell)
        }
        if (present.isEmpty()) {
            feedback += FeedbackEvent.Rejected
            return
        }

        others.shuffle(random)
        val changed = others.take(ALIGN_COUNT)
        for (cell in changed) {
            board[cell] = Tile(targetExp)
        }

        var maxEver = maxOf(state.maxExpEver, targetExp)
        var minSpawn = SpawnPool.retire(state.minSpawnExp, maxEver)
        val noMoves = detectNoMoves(board)

        state = state.copy(
            board = board,
            path = emptyList(),
            gems = state.gems - ALIGN_COST,
            minSpawnExp = minSpawn,
            maxExpEver = maxEver,
            noMoves = noMoves,
        )
        feedback += FeedbackEvent.AlignUsed(targetExp, changed)
        if (noMoves) feedback += FeedbackEvent.NoMoves
    }

    companion object {
        const val SHATTER_MIN_LEVEL = 5
        const val SHATTER_COST = 25
        const val ALIGN_MIN_LEVEL = 10
        const val ALIGN_COST = 40
        const val ALIGN_COUNT = 3
        const val LEVEL_CLEAR_GEMS = 10

        fun newGameState(random: Random = Random.Default): RunState {
            val board = Board()
            val minSpawn = 1
            MergeResolver.spawnFill(board, minSpawn, random)
            val maxEver = board.maxExpOrNull() ?: 1
            return RunState(
                board = board,
                path = emptyList(),
                gems = STARTING_GEMS,
                level = 1,
                goalExp = RunState.LEVEL_1_GOAL_EXP,
                minSpawnExp = SpawnPool.retire(minSpawn, maxEver),
                maxExpEver = maxEver,
                noMoves = detectNoMoves(board),
            )
        }

        const val STARTING_GEMS = 100

        /** noMoves when no two 8-adjacent occupied cells share the same exp. */
        fun detectNoMoves(board: Board): Boolean {
            for (r in 0 until Grid.ROWS) {
                for (c in 0 until Grid.COLS) {
                    val tile = board[c, r] ?: continue
                    val cell = Cell(c, r)
                    for (n in cell.neighbors8()) {
                        // Compare each unordered pair once (neighbor with larger index).
                        if (n.row < r || (n.row == r && n.col < c)) continue
                        val other = board[n] ?: continue
                        if (other.exp == tile.exp) return false
                    }
                }
            }
            return true
        }
    }
}
