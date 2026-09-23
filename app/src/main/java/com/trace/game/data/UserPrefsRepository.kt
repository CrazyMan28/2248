package com.trace.game.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import com.trace.game.domain.Board
import com.trace.game.domain.Grid
import com.trace.game.domain.RunState
import com.trace.game.domain.Tile

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "trace_prefs")

data class UserPrefs(
    val gems: Int = 100,
    val highExp: Int = 1,
    val tutorialDone: Boolean = false,
    val hapticsEnabled: Boolean = true,
    val sfxVolume: Float = 0.85f,
    val sfxMuted: Boolean = false,
    val reduceMotion: Boolean = false,
    /** True when a mid-run board is saved and can be resumed. */
    val hasSavedRun: Boolean = false,
)

class UserPrefsRepository(private val context: Context) {
    private object Keys {
        val gems = intPreferencesKey("gems")
        val highExp = intPreferencesKey("high_exp")
        val tutorialDone = booleanPreferencesKey("tutorial_done")
        val haptics = booleanPreferencesKey("haptics")
        val sfxVolume = floatPreferencesKey("sfx_volume")
        val sfxMuted = booleanPreferencesKey("sfx_muted")
        val reduceMotion = booleanPreferencesKey("reduce_motion")
        val savedRun = stringPreferencesKey("saved_run_json")
    }

    val prefs: Flow<UserPrefs> = context.dataStore.data.map { p ->
        UserPrefs(
            gems = p[Keys.gems] ?: 100,
            highExp = p[Keys.highExp] ?: 1,
            tutorialDone = p[Keys.tutorialDone] ?: false,
            hapticsEnabled = p[Keys.haptics] ?: true,
            sfxVolume = p[Keys.sfxVolume] ?: 0.85f,
            sfxMuted = p[Keys.sfxMuted] ?: false,
            reduceMotion = p[Keys.reduceMotion] ?: false,
            hasSavedRun = !p[Keys.savedRun].isNullOrBlank(),
        )
    }

    suspend fun setGems(value: Int) {
        context.dataStore.edit { it[Keys.gems] = value.coerceAtLeast(0) }
    }

    suspend fun addGems(delta: Int) {
        context.dataStore.edit {
            val cur = it[Keys.gems] ?: 100
            it[Keys.gems] = (cur + delta).coerceAtLeast(0)
        }
    }

    suspend fun setHighExp(exp: Int) {
        context.dataStore.edit {
            val cur = it[Keys.highExp] ?: 1
            if (exp > cur) it[Keys.highExp] = exp
        }
    }

    suspend fun setTutorialDone(done: Boolean = true) {
        context.dataStore.edit { it[Keys.tutorialDone] = done }
    }

    suspend fun setHaptics(enabled: Boolean) {
        context.dataStore.edit { it[Keys.haptics] = enabled }
    }

    suspend fun setSfxVolume(volume: Float) {
        context.dataStore.edit { it[Keys.sfxVolume] = volume.coerceIn(0f, 1f) }
    }

    suspend fun setSfxMuted(muted: Boolean) {
        context.dataStore.edit { it[Keys.sfxMuted] = muted }
    }

    suspend fun setReduceMotion(enabled: Boolean) {
        context.dataStore.edit { it[Keys.reduceMotion] = enabled }
    }

    /** Persist the full in-progress run (board + progression). */
    suspend fun saveRun(state: RunState) {
        val json = encodeRun(state)
        context.dataStore.edit {
            it[Keys.savedRun] = json
            it[Keys.gems] = state.gems
            val cur = it[Keys.highExp] ?: 1
            if (state.maxExpEver > cur) it[Keys.highExp] = state.maxExpEver
        }
    }

    suspend fun loadRun(): RunState? {
        val raw = context.dataStore.data.first()[Keys.savedRun] ?: return null
        return decodeRun(raw)
    }

    suspend fun clearRun() {
        context.dataStore.edit { it.remove(Keys.savedRun) }
    }

    companion object {
        fun encodeRun(state: RunState): String {
            val cells = buildString {
                for (r in 0 until Grid.ROWS) {
                    for (c in 0 until Grid.COLS) {
                        if (isNotEmpty()) append(',')
                        append(state.board[c, r]?.exp ?: 0)
                    }
                }
            }
            return buildString {
                append("v1")
                append("|g=").append(state.gems)
                append("|l=").append(state.level)
                append("|goal=").append(state.goalExp)
                append("|min=").append(state.minSpawnExp)
                append("|max=").append(state.maxExpEver)
                append("|nm=").append(if (state.noMoves) 1 else 0)
                append("|c=").append(cells)
            }
        }

        fun decodeRun(raw: String): RunState? {
            return try {
                if (!raw.startsWith("v1|")) return null
                val map = raw.split('|').drop(1).associate { part ->
                    val eq = part.indexOf('=')
                    require(eq > 0)
                    part.substring(0, eq) to part.substring(eq + 1)
                }
                val cellParts = map["c"]?.split(',') ?: return null
                if (cellParts.size != Grid.CELL_COUNT) return null
                val board = Board()
                var i = 0
                for (r in 0 until Grid.ROWS) {
                    for (c in 0 until Grid.COLS) {
                        val exp = cellParts[i++].toInt()
                        board[c, r] = if (exp >= 1) Tile(exp) else null
                    }
                }
                RunState(
                    board = board,
                    path = emptyList(),
                    gems = map["g"]?.toIntOrNull() ?: 100,
                    level = (map["l"]?.toIntOrNull() ?: 1).coerceAtLeast(1),
                    goalExp = map["goal"]?.toIntOrNull() ?: RunState.LEVEL_1_GOAL_EXP,
                    minSpawnExp = (map["min"]?.toIntOrNull() ?: 1).coerceAtLeast(1),
                    maxExpEver = (map["max"]?.toIntOrNull() ?: 1).coerceAtLeast(1),
                    noMoves = map["nm"] == "1",
                )
            } catch (_: Throwable) {
                null
            }
        }
    }
}
