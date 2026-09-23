package com.trace.game.domain

import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class MergeResolverTest {

    @Test
    fun twoTwosMergeToFour() {
        // exp 1 + exp 1 → sum 4 → floor(log2(4)) = 2 → tile "4"
        assertEquals(2, MergeResolver.resultExp(listOf(1, 1)))
    }

    @Test
    fun chain_twoTwoFourFourEightSixteen_mergesToThirtyTwo() {
        // 2+2+4+4+8+16 = 36 → floor(log2(36)) = 5 → 32
        val exps = listOf(1, 1, 2, 2, 3, 4)
        assertEquals(5, MergeResolver.resultExp(exps))
    }

    @Test
    fun resultExp_threeTwosIsFourWithRemainderSemantics() {
        // 2+2+2 = 6 → floor(log2(6)) = 2
        assertEquals(2, MergeResolver.resultExp(listOf(1, 1, 1)))
    }

    @Test
    fun resultExp_fourTwosIsEight() {
        // 2+2+2+2 = 8 → 3
        assertEquals(3, MergeResolver.resultExp(listOf(1, 1, 1, 1)))
    }

    @Test
    fun applyMerge_placesResultOnLastClearsOthersAppliesGravity() {
        val board = Board()
        // Column 0: rows 5,6,7 have tiles; merge path uses (0,6) and (0,7)
        board[0, 5] = Tile(3)
        board[0, 6] = Tile(1)
        board[0, 7] = Tile(1)
        // Fill the rest so spawnFill only touches cleared vacancies after gravity
        for (c in 0 until Grid.COLS) {
            for (r in 0 until Grid.ROWS) {
                if (board[c, r] == null) board[c, r] = Tile(1)
            }
        }

        val path = listOf(Cell(0, 6), Cell(0, 7))
        val outcome = MergeResolver.applyMerge(
            board = board,
            path = path,
            result = 2,
            minSpawnExp = 1,
            maxExpEver = 3,
            random = Random(0),
        )

        assertEquals(2, outcome.resultExp)
        assertEquals(Cell(0, 7), outcome.lastCell)
        // After clear + place on last + gravity: result sits at bottom of col 0
        assertEquals(Tile(2), outcome.board[0, 7])
        // The exp-3 tile that was above falls onto the result
        assertEquals(Tile(3), outcome.board[0, 6])
        // Board remains full
        outcome.board.forEachOccupied { _, _ -> }
        for (c in 0 until Grid.COLS) {
            for (r in 0 until Grid.ROWS) {
                assertNotNull(outcome.board[c, r])
            }
        }
    }

    @Test
    fun applyGravity_dropsTilesDown() {
        val board = Board()
        board[1, 0] = Tile(2)
        board[1, 2] = Tile(3)
        MergeResolver.applyGravity(board)
        assertNull(board[1, 0])
        assertNull(board[1, 2])
        assertEquals(Tile(2), board[1, 6])
        assertEquals(Tile(3), board[1, 7])
    }
}
