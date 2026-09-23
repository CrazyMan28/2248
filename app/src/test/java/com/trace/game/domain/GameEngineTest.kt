package com.trace.game.domain

import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class GameEngineTest {

    private fun emptyBoard(): Board = Board()

    private fun fillBoard(exp: Int = 1): Board {
        val board = Board()
        for (c in 0 until Grid.COLS) {
            for (r in 0 until Grid.ROWS) {
                board[c, r] = Tile(exp)
            }
        }
        return board
    }

    private fun engineWith(board: Board, gems: Int = 0, level: Int = 1, seed: Long = 0L): GameEngine {
        val maxEver = board.maxExpOrNull() ?: 1
        val state = RunState(
            board = board,
            gems = gems,
            level = level,
            goalExp = RunState.LEVEL_1_GOAL_EXP + (level - 1),
            minSpawnExp = SpawnPool.retire(1, maxEver),
            maxExpEver = maxEver,
            noMoves = GameEngine.detectNoMoves(board),
        )
        return GameEngine(Random(seed), state)
    }

    @Test
    fun merge_twoTwosProducesFour() {
        val board = fillBoard(9) // high filler so merges stand out; path cells overwritten
        board[0, 7] = Tile(1)
        board[1, 7] = Tile(1)
        // Break adjacency among fillers in this row so only our pair matches if needed
        val engine = engineWith(board)
        engine.apply(InputEvent.Down(Cell(0, 7)))
        engine.apply(InputEvent.Move(Cell(1, 7)))
        val result = engine.apply(InputEvent.Up)

        val merge = result.feedback.filterIsInstance<FeedbackEvent.MergeCommitted>().single()
        assertEquals(2, merge.resultExp)
        assertEquals(Tile(2), result.state.board[merge.lastCell])
    }

    @Test
    fun merge_chain_twoTwoFourFourEightSixteen_toThirtyTwo() {
        val board = fillBoard(7)
        // 5 cols — use an L: (0..4,7) then (4,6) for 2-2-4-4-8-16
        board[0, 7] = Tile(1)
        board[1, 7] = Tile(1)
        board[2, 7] = Tile(2)
        board[3, 7] = Tile(2)
        board[4, 7] = Tile(3)
        board[4, 6] = Tile(4)

        val engine = engineWith(board)
        val cells = listOf(
            Cell(0, 7), Cell(1, 7), Cell(2, 7), Cell(3, 7), Cell(4, 7), Cell(4, 6),
        )
        engine.apply(InputEvent.Down(cells.first()))
        for (i in 1 until cells.size) {
            engine.apply(InputEvent.Move(cells[i]))
        }
        assertEquals(6, engine.state.path.size)

        val result = engine.apply(InputEvent.Up)
        val merge = result.feedback.filterIsInstance<FeedbackEvent.MergeCommitted>().single()
        assertEquals(5, merge.resultExp) // 32
        assertEquals(Tile(5), result.state.board[merge.lastCell])
    }

    @Test
    fun pathValidation_ascentThroughEngine() {
        val board = fillBoard(5)
        board[0, 0] = Tile(1)
        board[1, 0] = Tile(1)
        board[2, 0] = Tile(2)
        val engine = engineWith(board)
        engine.apply(InputEvent.Down(Cell(0, 0)))
        engine.apply(InputEvent.Move(Cell(1, 0)))
        val extended = engine.apply(InputEvent.Move(Cell(2, 0)))
        assertEquals(3, extended.state.path.size)

        // Cannot skip to exp 3 without exp 2
        board[3, 0] = Tile(3)
        // reset path
        engine.apply(InputEvent.Up) // commits 2-2-4
        // Fresh attempt for invalid skip on a new board setup
        val board2 = fillBoard(5)
        board2[0, 1] = Tile(1)
        board2[1, 1] = Tile(1)
        board2[2, 1] = Tile(3) // skip
        val engine2 = engineWith(board2)
        engine2.apply(InputEvent.Down(Cell(0, 1)))
        engine2.apply(InputEvent.Move(Cell(1, 1)))
        val rejected = engine2.apply(InputEvent.Move(Cell(2, 1)))
        assertEquals(2, rejected.state.path.size)
    }

    @Test
    fun retire_whenMaxExpEverReaches512() {
        assertEquals(2, SpawnPool.retire(1, 9))
        val board = fillBoard(1)
        // Simulate a merge that creates exp 9
        board[0, 7] = Tile(8)
        board[1, 7] = Tile(8)
        val engine = engineWith(board, seed = 1L)
        // maxExpEver starts at 8 from fill... fillBoard(1) → max 1
        // Place two 8s (exp 8 = 256); merge → exp 9 = 512
        engine.apply(InputEvent.Down(Cell(0, 7)))
        engine.apply(InputEvent.Move(Cell(1, 7)))
        val result = engine.apply(InputEvent.Up)
        assertEquals(9, result.feedback.filterIsInstance<FeedbackEvent.MergeCommitted>().single().resultExp)
        assertTrue(result.state.maxExpEver >= 9)
        assertTrue(
            "minSpawnExp should retire past 1 after reaching 512",
            result.state.minSpawnExp >= 2,
        )
    }

    @Test
    fun noMoves_whenNoAdjacentEqualExps() {
        val board = emptyBoard()
        // Checkerboard of alternating exps — no two equal 8-adjacent
        var toggle = true
        for (r in 0 until Grid.ROWS) {
            for (c in 0 until Grid.COLS) {
                // For 8-adjacency, simple checkerboard fails on diagonals.
                // Use unique-enough pattern: each cell gets exp based on ensuring
                // neighbors differ — assign exp = (r * COLS + c) % 3 + something
                // Safer: give every cell a unique exp.
                board[c, r] = Tile(1 + r * Grid.COLS + c)
            }
        }
        assertTrue(GameEngine.detectNoMoves(board))

        // Introduce an equal adjacent pair
        board[0, 0] = Tile(1)
        board[1, 0] = Tile(1)
        assertFalse(GameEngine.detectNoMoves(board))
    }

    @Test
    fun noMoves_diagonalEqualsCountAsMoves() {
        val board = emptyBoard()
        for (r in 0 until Grid.ROWS) {
            for (c in 0 until Grid.COLS) {
                board[c, r] = Tile(100 + r * Grid.COLS + c)
            }
        }
        board[0, 0] = Tile(2)
        board[1, 1] = Tile(2) // diagonal
        assertFalse(GameEngine.detectNoMoves(board))
    }

    @Test
    fun combo_awardsGemsForLongPath() {
        val board = fillBoard(1)
        // Path of 5 identical tiles in a row
        for (c in 0 until 5) board[c, 7] = Tile(1)
        val engine = engineWith(board, gems = 0)
        engine.apply(InputEvent.Down(Cell(0, 7)))
        for (c in 1 until 5) engine.apply(InputEvent.Move(Cell(c, 7)))
        val result = engine.apply(InputEvent.Up)
        val combo = result.feedback.filterIsInstance<FeedbackEvent.Combo>().single()
        assertEquals(5, combo.pathLen)
        assertEquals(1 + 5 / 5, combo.gemsGained)
        assertEquals(combo.gemsGained, result.state.gems) // no level-up from exp 2
    }

    @Test
    fun levelUp_whenResultMeetsGoal() {
        val board = fillBoard(1)
        // goal for level 1 is exp 9; merge two exp-8 → exp 9
        board[0, 7] = Tile(8)
        board[1, 7] = Tile(8)
        val engine = engineWith(board)
        engine.apply(InputEvent.Down(Cell(0, 7)))
        engine.apply(InputEvent.Move(Cell(1, 7)))
        val result = engine.apply(InputEvent.Up)
        val levelUp = result.feedback.filterIsInstance<FeedbackEvent.LevelUp>().single()
        assertEquals(2, levelUp.newLevel)
        assertEquals(10, levelUp.newGoalExp)
        assertEquals(10, levelUp.gemsGained)
        assertEquals(2, result.state.level)
        assertEquals(10, result.state.goalExp)
        // +1 merge + 5 milestone (first 512) + 10 level-clear
        assertEquals(16, result.state.gems)
    }

    @Test
    fun shatter_requiresLevelAndGems() {
        val board = fillBoard(1)
        val locked = engineWith(board, gems = 100, level = 4)
        val rejected = locked.apply(InputEvent.UseShatter(Cell(0, 0)))
        assertTrue(rejected.feedback.any { it is FeedbackEvent.Rejected })

        val ok = engineWith(board, gems = 25, level = 5, seed = 2L)
        val used = ok.apply(InputEvent.UseShatter(Cell(0, 0)))
        assertTrue(used.feedback.any { it is FeedbackEvent.ShatterUsed })
        assertEquals(0, used.state.gems)
    }

    @Test
    fun align_setsThreeOtherTiles() {
        val board = fillBoard(1)
        board[0, 0] = Tile(4) // target present
        val engine = engineWith(board, gems = 40, level = 10, seed = 99L)
        val result = engine.apply(InputEvent.UseAlign(4))
        val align = result.feedback.filterIsInstance<FeedbackEvent.AlignUsed>().single()
        assertEquals(4, align.targetExp)
        assertEquals(3, align.changed.size)
        for (cell in align.changed) {
            assertEquals(Tile(4), result.state.board[cell])
        }
        assertEquals(0, result.state.gems)
    }


    @Test
    fun milestone_awardsGemsWhenCrossing512() {
        val board = fillBoard(1)
        // Two exp-8 tiles merge to exp-9 (512) — first milestone
        board[0, 7] = Tile(8)
        board[1, 7] = Tile(8)
        val engine = engineWith(board, gems = 0, level = 1)
        // Pretend max was below 512
        engine.replaceState(engine.state.copy(maxExpEver = 8, gems = 0, goalExp = 20))
        engine.apply(InputEvent.Down(Cell(0, 7)))
        engine.apply(InputEvent.Move(Cell(1, 7)))
        val result = engine.apply(InputEvent.Up)
        val mile = result.feedback.filterIsInstance<FeedbackEvent.MilestoneGems>()
        assertTrue(mile.isNotEmpty())
        assertEquals(9, mile.first().exp)
        assertEquals(5, mile.first().gemsGained)
    }

    @Test
    fun formatExp_smallNumbersAndCompact() {
        assertEquals("2", formatExp(1))
        assertEquals("4", formatExp(2))
        assertEquals("512", formatExp(9))
        assertEquals("4096", formatExp(12))
        // 2^13 = 8192 >= 10000? 8192 < 10000 — still number
        assertEquals("8192", formatExp(13))
        // 2^14 = 16384 → compact
        assertEquals("16K", formatExp(14))
        assertEquals("2^63", formatExp(63))
    }

    @Test
    fun newGame_fillsBoard() {
        val engine = GameEngine(Random(0))
        for (c in 0 until Grid.COLS) {
            for (r in 0 until Grid.ROWS) {
                assertNotNull(engine.state.board[c, r])
            }
        }
        assertEquals(1, engine.state.level)
        assertEquals(9, engine.state.goalExp)
    }
}
