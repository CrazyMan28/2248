package com.trace.game.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "trace_prefs")

data class UserPrefs(
    val gems: Int = 100,
    val highExp: Int = 1,
    val tutorialDone: Boolean = false,
    val hapticsEnabled: Boolean = true,
    val sfxVolume: Float = 0.85f,
    val sfxMuted: Boolean = false,
    val reduceMotion: Boolean = false,
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
}
