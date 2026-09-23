package com.trace.game.domain

import kotlin.random.Random

/**
 * Spawn pool and tile retirement.
 *
 * Retirement: while maxExpEver >= minSpawnExp + 8, minSpawnExp++.
 * Pool: four consecutive exps starting at minSpawnExp, weights 35 / 35 / 20 / 10.
 */
object SpawnPool {

    private val WEIGHTS = intArrayOf(35, 35, 20, 10)
    private const val TOTAL_WEIGHT = 100
    const val RETIRE_SPAN = 8

    /** Advance [minSpawnExp] until the spawn window sits below [maxExpEver]. */
    fun retire(minSpawnExp: Int, maxExpEver: Int): Int {
        var min = minSpawnExp
        while (maxExpEver >= min + RETIRE_SPAN) {
            min++
        }
        return min
    }

    /** The four consecutive exponents currently in the pool. */
    fun poolExps(minSpawnExp: Int): List<Int> =
        listOf(minSpawnExp, minSpawnExp + 1, minSpawnExp + 2, minSpawnExp + 3)

    fun weights(): List<Int> = WEIGHTS.toList()

    /** Roll one spawn exp from the weighted pool. */
    fun roll(minSpawnExp: Int, random: Random): Int {
        val pick = random.nextInt(TOTAL_WEIGHT)
        var cumulative = 0
        for (i in WEIGHTS.indices) {
            cumulative += WEIGHTS[i]
            if (pick < cumulative) return minSpawnExp + i
        }
        return minSpawnExp + WEIGHTS.lastIndex
    }

    /**
     * Whether [maxExpEver] causes at least one retirement step from [minSpawnExp].
     * Useful for tests (e.g. maxExpEver reaching 512 / exp 9 retires past 1).
     */
    fun shouldRetire(minSpawnExp: Int, maxExpEver: Int): Boolean =
        maxExpEver >= minSpawnExp + RETIRE_SPAN
}
