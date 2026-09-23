package com.trace.game.data

import com.trace.game.domain.Board
import com.trace.game.domain.Grid
import com.trace.game.domain.RunState
import com.trace.game.domain.Tile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class SavedRunCodecTest {

    @Test
    fun roundTrip_preservesBoardAndProgress() {
        val board = Board()
        board[0, 7] = Tile(8) // 256
        board[2, 3] = Tile(5)
        val state = RunState(
            board = board,
            gems = 142,
            level = 3,
            goalExp = 11,
            minSpawnExp = 2,
            maxExpEver = 8,
            noMoves = false,
        )
        val encoded = UserPrefsRepository.encodeRun(state)
        val decoded = UserPrefsRepository.decodeRun(encoded)
        assertNotNull(decoded)
        assertEquals(142, decoded!!.gems)
        assertEquals(3, decoded.level)
        assertEquals(11, decoded.goalExp)
        assertEquals(2, decoded.minSpawnExp)
        assertEquals(8, decoded.maxExpEver)
        assertEquals(8, decoded.board[0, 7]?.exp)
        assertEquals(5, decoded.board[2, 3]?.exp)
        assertNull(decoded.board[1, 1])
        assertEquals(Grid.CELL_COUNT, 40)
    }
}
