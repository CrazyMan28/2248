package com.trace.game.domain

import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class SpawnPoolTest {

    @Test
    fun retire_at512_advancesMinSpawn() {
        // 512 is exp 9. While maxExpEver >= minSpawnExp + 8 → retire.
        // From minSpawnExp=1, maxExpEver=9: 9 >= 1+8 → min becomes 2
        assertEquals(2, SpawnPool.retire(1, 9))
        assertTrue(SpawnPool.shouldRetire(1, 9))
    }

    @Test
    fun retire_keepsAdvancingWhileSpanExceeded() {
        // maxExpEver = 12, min starts at 1
        // 12 >= 1+8 → 2; 12 >= 2+8 → 3; 12 >= 3+8 → 4; 12 >= 4+8 → stop (12 < 12)
        // 12 >= 4+8 (=12) → yes, min=5; 12 >= 5+8=13? no → 5
        assertEquals(5, SpawnPool.retire(1, 12))
    }

    @Test
    fun retire_noopWhenWithinWindow() {
        assertEquals(1, SpawnPool.retire(1, 8)) // 8 >= 1+8? 8 >= 9 false
        assertEquals(3, SpawnPool.retire(3, 10)) // 10 >= 11 false
    }

    @Test
    fun poolExps_areFourConsecutive() {
        assertEquals(listOf(1, 2, 3, 4), SpawnPool.poolExps(1))
        assertEquals(listOf(2, 3, 4, 5), SpawnPool.poolExps(2))
    }

    @Test
    fun weights_are35_35_20_10() {
        assertEquals(listOf(35, 35, 20, 10), SpawnPool.weights())
    }

    @Test
    fun roll_staysInsidePool() {
        val random = Random(1234)
        repeat(200) {
            val exp = SpawnPool.roll(2, random)
            assertTrue(exp in 2..5)
        }
    }

    @Test
    fun roll_approximateWeights() {
        val random = Random(42)
        val counts = IntArray(4)
        val n = 10_000
        repeat(n) {
            val exp = SpawnPool.roll(1, random)
            counts[exp - 1]++
        }
        // Loose bounds around 35/35/20/10
        assertTrue(counts[0] in 3000..4000)
        assertTrue(counts[1] in 3000..4000)
        assertTrue(counts[2] in 1500..2500)
        assertTrue(counts[3] in 700..1300)
    }
}
