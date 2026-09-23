package com.trace.game.domain

import org.junit.Assert.*
import org.junit.Test

class PathRulesTest {

    private fun boardWith(vararg placed: Triple<Int, Int, Int>): Board {
        val board = Board()
        for ((col, row, exp) in placed) {
            board[col, row] = Tile(exp)
        }
        return board
    }

    @Test
    fun validSequence_allSameExps() {
        assertTrue(PathRules.isValidExpSequence(listOf(1)))
        assertTrue(PathRules.isValidExpSequence(listOf(1, 1)))
        assertTrue(PathRules.isValidExpSequence(listOf(1, 1, 1, 1)))
    }

    @Test
    fun validSequence_ascentAfterPair() {
        // 2-2-4
        assertTrue(PathRules.isValidExpSequence(listOf(1, 1, 2)))
        // 2-2-4-4-8-16
        assertTrue(PathRules.isValidExpSequence(listOf(1, 1, 2, 2, 3, 4)))
        // single tile at ascended tiers is allowed
        assertTrue(PathRules.isValidExpSequence(listOf(1, 1, 2, 3, 4)))
    }

    @Test
    fun invalidSequence_ascentWithoutPair() {
        assertFalse(PathRules.isValidExpSequence(listOf(1, 2)))
        assertFalse(PathRules.isValidExpSequence(listOf(1, 1, 3))) // skipped tier
        assertFalse(PathRules.isValidExpSequence(listOf(1, 1, 2, 4))) // skipped
    }

    @Test
    fun canExtend_requiresAdjacencyAndNoRevisit() {
        // Layout (col,row):
        // (0,0)=1  (1,0)=1  (2,0)=2
        val board = boardWith(
            Triple(0, 0, 1),
            Triple(1, 0, 1),
            Triple(2, 0, 2),
            Triple(0, 1, 9),
        )
        val path = listOf(Cell(0, 0), Cell(1, 0))
        assertTrue(PathRules.canExtend(board, path, Cell(2, 0)))
        assertFalse(PathRules.canExtend(board, path, Cell(0, 0))) // revisit
        assertFalse(PathRules.canExtend(board, path, Cell(0, 1))) // wrong exp
    }

    @Test
    fun backtrack_popsWhenTouchingSecondToLast() {
        val path = listOf(Cell(0, 0), Cell(1, 0), Cell(2, 0))
        assertTrue(PathRules.isBacktrack(path, Cell(1, 0)))
        assertFalse(PathRules.isBacktrack(path, Cell(0, 0)))

        val board = boardWith(
            Triple(0, 0, 1),
            Triple(1, 0, 1),
            Triple(2, 0, 1),
        )
        val after = PathRules.applyMove(board, path, Cell(1, 0))
        assertEquals(listOf(Cell(0, 0), Cell(1, 0)), after)
    }

    @Test
    fun startPath_emptyOnVacantCell() {
        val board = Board()
        assertTrue(PathRules.startPath(board, Cell(0, 0)).isEmpty())
        board[0, 0] = Tile(1)
        assertEquals(listOf(Cell(0, 0)), PathRules.startPath(board, Cell(0, 0)))
    }
}
