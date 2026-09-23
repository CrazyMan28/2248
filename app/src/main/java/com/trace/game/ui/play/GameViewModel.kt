package com.trace.game.ui.play

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.trace.game.data.UserPrefs
import com.trace.game.data.UserPrefsRepository
import com.trace.game.domain.Cell
import com.trace.game.domain.FeedbackEvent
import com.trace.game.domain.GameEngine
import com.trace.game.domain.InputEvent
import com.trace.game.domain.RunState
import com.trace.game.feedback.HapticsEngine
import com.trace.game.feedback.SfxEngine
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class PowerMode { None, ShatterTarget, AlignPick }

data class PlayUiState(
    val paused: Boolean = false,
    val comboBanner: Int? = null,
    val mergeFlashCell: Cell? = null,
    val powerMode: PowerMode = PowerMode.None,
    val tutorialStep: Int = 0,
    val showTutorial: Boolean = false,
)

class GameViewModel(
    private val prefsRepo: UserPrefsRepository,
    private val haptics: HapticsEngine,
    private val sfx: SfxEngine,
) : ViewModel() {

    private val engine = GameEngine()

    private val _runState = MutableStateFlow(engine.state)
    val runState: StateFlow<RunState> = _runState.asStateFlow()

    private val _playUi = MutableStateFlow(PlayUiState())
    val playUi: StateFlow<PlayUiState> = _playUi.asStateFlow()

    val userPrefs: StateFlow<UserPrefs> = prefsRepo.prefs.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        UserPrefs(),
    )

    init {
        viewModelScope.launch {
            prefsRepo.prefs.collect { p ->
                haptics.enabled = p.hapticsEnabled
                sfx.volume = p.sfxVolume
                sfx.muted = p.sfxMuted
            }
        }
    }

    fun startNewRun() {
        apply(InputEvent.NewGame)
        val gems = userPrefs.value.gems
        engine.setGems(gems)
        _runState.value = engine.state
        val tutorialDone = userPrefs.value.tutorialDone
        _playUi.value = PlayUiState(
            showTutorial = !tutorialDone,
            tutorialStep = if (tutorialDone) -1 else 0,
        )
    }

    fun onDown(cell: Cell) {
        if (_playUi.value.powerMode != PowerMode.None) {
            onTileTapForPower(cell)
            return
        }
        apply(InputEvent.Down(cell))
    }

    fun onMove(cell: Cell) = apply(InputEvent.Move(cell))
    fun onUp() = apply(InputEvent.Up)

    fun togglePause() {
        _playUi.value = _playUi.value.copy(paused = !_playUi.value.paused)
        sfx.playUi()
    }

    fun resume() {
        _playUi.value = _playUi.value.copy(paused = false)
        sfx.playUi()
    }

    fun restart() {
        startNewRun()
        sfx.playUi()
    }

    fun uiTap() = sfx.playUi()

    fun armShatter() {
        val s = _runState.value
        if (s.level < GameEngine.SHATTER_MIN_LEVEL || s.gems < GameEngine.SHATTER_COST) return
        _playUi.value = _playUi.value.copy(
            powerMode = if (_playUi.value.powerMode == PowerMode.ShatterTarget) {
                PowerMode.None
            } else {
                PowerMode.ShatterTarget
            },
        )
        sfx.playUi()
    }

    fun armAlign() {
        val s = _runState.value
        if (s.level < GameEngine.ALIGN_MIN_LEVEL || s.gems < GameEngine.ALIGN_COST) return
        _playUi.value = _playUi.value.copy(
            powerMode = if (_playUi.value.powerMode == PowerMode.AlignPick) {
                PowerMode.None
            } else {
                PowerMode.AlignPick
            },
        )
        sfx.playUi()
    }

    fun onTileTapForPower(cell: Cell) {
        when (_playUi.value.powerMode) {
            PowerMode.ShatterTarget -> {
                apply(InputEvent.UseShatter(cell))
                _playUi.value = _playUi.value.copy(powerMode = PowerMode.None)
            }
            PowerMode.AlignPick -> {
                val exp = _runState.value.board[cell]?.exp ?: return
                apply(InputEvent.UseAlign(exp))
                _playUi.value = _playUi.value.copy(powerMode = PowerMode.None)
            }
            PowerMode.None -> Unit
        }
    }

    fun advanceTutorial() {
        val step = _playUi.value.tutorialStep + 1
        if (step >= 4) {
            _playUi.value = _playUi.value.copy(showTutorial = false, tutorialStep = -1)
            viewModelScope.launch { prefsRepo.setTutorialDone(true) }
        } else {
            _playUi.value = _playUi.value.copy(tutorialStep = step)
        }
        sfx.playUi()
    }

    fun buyGemsLocal(amount: Int) {
        viewModelScope.launch {
            prefsRepo.addGems(amount)
            engine.setGems(_runState.value.gems + amount)
            _runState.value = engine.state
            sfx.playUi()
        }
    }

    fun setHaptics(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setHaptics(enabled) }
    }

    fun setSfxVolume(v: Float) {
        viewModelScope.launch { prefsRepo.setSfxVolume(v) }
    }

    fun setSfxMuted(muted: Boolean) {
        viewModelScope.launch { prefsRepo.setSfxMuted(muted) }
    }

    fun setReduceMotion(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setReduceMotion(enabled) }
    }

    private fun apply(event: InputEvent) {
        if (_playUi.value.paused && event !is InputEvent.NewGame) return
        val result = engine.apply(event)
        _runState.value = result.state
        haptics.onFeedback(result.feedback)
        sfx.onFeedback(result.feedback)

        var ui = _playUi.value
        for (f in result.feedback) {
            when (f) {
                is FeedbackEvent.Combo -> ui = ui.copy(comboBanner = f.pathLen)
                is FeedbackEvent.MergeCommitted -> ui = ui.copy(mergeFlashCell = f.lastCell)
                else -> Unit
            }
        }
        _playUi.value = ui

        if (ui.comboBanner != null || ui.mergeFlashCell != null) {
            viewModelScope.launch {
                delay(500)
                _playUi.value = _playUi.value.copy(comboBanner = null, mergeFlashCell = null)
            }
        }

        viewModelScope.launch {
            prefsRepo.setGems(result.state.gems)
            prefsRepo.setHighExp(result.state.maxExpEver)
        }
    }
}

class GameViewModelFactory(
    private val prefs: UserPrefsRepository,
    private val haptics: HapticsEngine,
    private val sfx: SfxEngine,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return GameViewModel(prefs, haptics, sfx) as T
    }
}
