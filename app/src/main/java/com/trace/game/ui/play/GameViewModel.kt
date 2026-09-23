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
import com.trace.game.domain.formatExp
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

/** One tile sliding from [fromRow] (may be < 0 for off-screen spawn) into [to]. */
data class FallSlide(
    val to: Cell,
    val fromRow: Float,
    val exp: Int,
)

data class MergeFx(
    val resultCell: Cell,
    val resultExp: Int,
    val resultLabel: String,
    val slides: List<FallSlide>,
    val epoch: Long,
)

data class PlayUiState(
    val paused: Boolean = false,
    val comboBanner: Int? = null,
    val mergeFlashCell: Cell? = null,
    val mergeFx: MergeFx? = null,
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

    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    val userPrefs: StateFlow<UserPrefs> = prefsRepo.prefs.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        UserPrefs(),
    )

    private var mergeEpoch = 0L
    private var runLoaded = false

    init {
        viewModelScope.launch {
            prefsRepo.prefs.collect { p ->
                haptics.enabled = p.hapticsEnabled
                sfx.volume = p.sfxVolume
                sfx.muted = p.sfxMuted
            }
        }
        viewModelScope.launch {
            val saved = prefsRepo.loadRun()
            if (saved != null) {
                engine.replaceState(saved.copy(path = emptyList()))
                _runState.value = engine.state
                runLoaded = true
            }
            _ready.value = true
        }
    }

    /** Resume saved board if any, otherwise start fresh. Does not wipe progress. */
    fun enterPlay() {
        viewModelScope.launch {
            if (!_ready.value) {
                // wait briefly for load
                delay(50)
            }
            val saved = prefsRepo.loadRun()
            if (saved != null) {
                engine.replaceState(saved.copy(path = emptyList(), gems = userPrefs.value.gems.coerceAtLeast(saved.gems)))
                _runState.value = engine.state
                runLoaded = true
                _playUi.value = PlayUiState(
                    showTutorial = false,
                    tutorialStep = -1,
                )
            } else if (!runLoaded || engine.state.board.occupiedCells().isEmpty()) {
                startNewRunInternal(keepGems = true)
            } else {
                // In-memory run still valid (navigated home without clearing)
                engine.replaceState(engine.state.copy(path = emptyList()))
                _runState.value = engine.state
            }
        }
    }

    fun startNewRun() {
        startNewRunInternal(keepGems = true)
        viewModelScope.launch { prefsRepo.clearRun() }
    }

    private fun startNewRunInternal(keepGems: Boolean) {
        apply(InputEvent.NewGame, persist = false)
        val gems = if (keepGems) userPrefs.value.gems else engine.state.gems
        engine.setGems(gems)
        _runState.value = engine.state
        runLoaded = true
        val tutorialDone = userPrefs.value.tutorialDone
        _playUi.value = PlayUiState(
            showTutorial = !tutorialDone,
            tutorialStep = if (tutorialDone) -1 else 0,
        )
        viewModelScope.launch { persistNow() }
    }

    fun saveAndExit(onDone: () -> Unit) {
        viewModelScope.launch {
            persistNow()
            onDone()
        }
    }

    fun onDown(cell: Cell) {
        if (_playUi.value.paused) return
        if (_playUi.value.powerMode != PowerMode.None) {
            onTileTapForPower(cell)
            return
        }
        apply(InputEvent.Down(cell))
    }

    fun onMove(cell: Cell) {
        if (_playUi.value.paused) return
        apply(InputEvent.Move(cell))
    }

    fun onUp() {
        if (_playUi.value.paused) return
        apply(InputEvent.Up)
    }

    fun togglePause() {
        _playUi.value = _playUi.value.copy(paused = !_playUi.value.paused)
        sfx.playUi()
        if (_playUi.value.paused) {
            viewModelScope.launch { persistNow() }
        }
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
            persistNow()
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

    private fun apply(event: InputEvent, persist: Boolean = true) {
        if (_playUi.value.paused && event !is InputEvent.NewGame) return
        val result = engine.apply(event)
        _runState.value = result.state
        haptics.onFeedback(result.feedback)
        sfx.onFeedback(result.feedback)

        var ui = _playUi.value
        for (f in result.feedback) {
            when (f) {
                is FeedbackEvent.Combo -> ui = ui.copy(comboBanner = f.pathLen)
                is FeedbackEvent.MergeCommitted -> {
                    mergeEpoch += 1
                    val board = result.state.board
                    val slides = buildList {
                        for ((from, to) in f.gravityMoves) {
                            val exp = board[to]?.exp ?: continue
                            add(FallSlide(to = to, fromRow = from.row.toFloat(), exp = exp))
                        }
                        for (cell in f.spawned) {
                            val exp = board[cell]?.exp ?: continue
                            // Drop in from above the grid (negative rows).
                            add(FallSlide(to = cell, fromRow = -1.5f - cell.row * 0.15f, exp = exp))
                        }
                    }
                    ui = ui.copy(
                        mergeFlashCell = f.lastCell,
                        mergeFx = MergeFx(
                            resultCell = f.lastCell,
                            resultExp = f.resultExp,
                            resultLabel = formatExp(f.resultExp),
                            slides = slides,
                            epoch = mergeEpoch,
                        ),
                    )
                }
                else -> Unit
            }
        }
        _playUi.value = ui

        if (ui.comboBanner != null || ui.mergeFlashCell != null || ui.mergeFx != null) {
            viewModelScope.launch {
                delay(420)
                _playUi.value = _playUi.value.copy(
                    comboBanner = null,
                    mergeFlashCell = null,
                    mergeFx = null,
                )
            }
        }

        if (persist && (event is InputEvent.Up || event is InputEvent.UseShatter || event is InputEvent.UseAlign)) {
            viewModelScope.launch { persistNow() }
        }
    }

    private suspend fun persistNow() {
        val s = engine.state.copy(path = emptyList())
        prefsRepo.saveRun(s)
        prefsRepo.setGems(s.gems)
        prefsRepo.setHighExp(s.maxExpEver)
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
