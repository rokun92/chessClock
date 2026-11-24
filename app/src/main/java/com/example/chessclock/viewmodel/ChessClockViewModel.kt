package com.example.chessclock.viewmodel

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.chessclock.data.ClockSettings
import com.example.chessclock.data.SettingsRepository
import com.example.chessclock.data.SharedPreferencesSettingsRepository
import com.example.chessclock.domain.ChessClockEngine
import com.example.chessclock.domain.model.GameState
import com.example.chessclock.domain.model.GameStatus
import com.example.chessclock.domain.model.Player
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Owns a single ticker coroutine and exposes immutable [GameState] snapshots.
 * The ticker is cancelled and recreated only here, so rotation and rapid taps
 * can never produce duplicate timers.
 */
class ChessClockViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsRepository: SettingsRepository =
        SharedPreferencesSettingsRepository(application.applicationContext)

    private val timeSource: () -> Long = { SystemClock.elapsedRealtime() }

    private val initialSettings = settingsRepository.load()

    private val engine = ChessClockEngine(
        timeSource = timeSource,
        initialDurationMs = initialSettings.durationMs,
        initialIncrementMs = initialSettings.incrementMs,
    )

    private val _uiState = MutableStateFlow(engine.currentState())
    val uiState: StateFlow<GameState> = _uiState.asStateFlow()

    private val _settings = MutableStateFlow(initialSettings)
    val settings: StateFlow<ClockSettings> = _settings.asStateFlow()

    private var tickerJob: Job? = null

    fun startGame() {
        if (engine.currentState().status != GameStatus.READY) return
        publish(engine.start())
        startTicker()
    }

    /** Only a tap on the active player's half hands over the turn. */
    fun onPlayerTap(player: Player) {
        val before = engine.currentState()
        if (before.status != GameStatus.PLAYING) return

        val after = engine.switchTurn(player)
        if (after == before) return

        publish(after)
        startTicker()
    }

    /** Called from ON_STOP: freeze the clocks; no time passes invisibly. */
    fun onAppBackground() {
        if (engine.currentState().status != GameStatus.PLAYING) return
        publish(engine.pause())
        stopTicker()
    }

    /** Explicit user action required after a background pause. */
    fun resumeGame() {
        if (engine.currentState().status != GameStatus.PAUSED) return
        publish(engine.resume())
        if (engine.currentState().status == GameStatus.PLAYING) {
            startTicker()
        }
    }

    fun newGame() {
        stopTicker()
        publish(engine.reset())
    }

    /** Clock settings can only be changed before the game starts. */
    fun saveSettings(newSettings: ClockSettings) {
        if (engine.currentState().status != GameStatus.READY) return
        settingsRepository.save(newSettings)
        _settings.value = newSettings
        publish(engine.updateSettings(newSettings.durationMs, newSettings.incrementMs))
    }

    private fun publish(state: GameState) {
        _uiState.value = state
    }

    private fun startTicker() {
        stopTicker()
        tickerJob = viewModelScope.launch {
            while (isActive && engine.currentState().status == GameStatus.PLAYING) {
                val now = timeSource()
                publish(engine.tick(now))
                if (engine.currentState().status != GameStatus.PLAYING) break
                delay(engine.millisUntilNextUpdate(now))
            }
        }
    }

    private fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    override fun onCleared() {
        stopTicker()
        super.onCleared()
    }
}
