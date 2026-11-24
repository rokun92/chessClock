package com.example.chessclock.domain

import com.example.chessclock.domain.model.DEFAULT_DURATION_MS
import com.example.chessclock.domain.model.GameState
import com.example.chessclock.domain.model.GameStatus
import com.example.chessclock.domain.model.Player

/**
 * Pure-Kotlin chess clock logic. It has no Android or UI dependencies and never
 * schedules work itself: the caller supplies the current monotonic time and
 * asks for the next deadline. This keeps timing exact and makes the logic
 * deterministic in unit tests.
 *
 * @param timeSource monotonic clock (e.g. SystemClock.elapsedRealtime()).
 */
class ChessClockEngine(
    private val timeSource: () -> Long,
    initialDurationMs: Long = DEFAULT_DURATION_MS,
    initialIncrementMs: Long = 0L,
) {
    private var state = newReadyState(initialDurationMs, initialIncrementMs)

    /** Timestamp at which the current turn started, in the monotonic time base. */
    private var turnStartedAtMs: Long = 0L

    /** Remaining time of the active player at [turnStartedAtMs]. */
    private var turnStartRemainingMs: Long = 0L

    fun currentState(): GameState = state

    /** Starts a game from READY. White always moves first. */
    fun start(nowMs: Long = timeSource()): GameState {
        if (state.status != GameStatus.READY) return state

        turnStartedAtMs = nowMs
        turnStartRemainingMs = state.whiteRemainingMs
        state = state.copy(
            status = GameStatus.PLAYING,
            activePlayer = Player.WHITE,
            winner = null,
        )
        return state
    }

    /**
     * Recalculates remaining time from the monotonic clock. This is safe to call
     * at any frequency; the state only changes when a second boundary or the
     * exact timeout is crossed.
     */
    fun tick(nowMs: Long = timeSource()): GameState {
        if (state.status != GameStatus.PLAYING) return state
        val active = state.activePlayer ?: return state

        val remaining = remainingAt(nowMs)
        if (remaining == 0L) {
            state = state.copy(
                status = GameStatus.GAME_OVER,
                winner = active.opponent,
                activePlayer = null,
                whiteRemainingMs = if (active == Player.WHITE) 0L else state.whiteRemainingMs,
                blackRemainingMs = if (active == Player.BLACK) 0L else state.blackRemainingMs,
            )
        } else {
            state = state.copy(
                whiteRemainingMs = if (active == Player.WHITE) remaining else state.whiteRemainingMs,
                blackRemainingMs = if (active == Player.BLACK) remaining else state.blackRemainingMs,
            )
        }
        return state
    }

    /**
     * Hands over the turn when the **currently active** player's half is tapped.
     * Taps on the opponent's half are ignored. The outgoing clock is settled at
     * [nowMs]; if increment mode is on, the increment is added to that player's
     * time before the opponent's clock starts.
     */
    fun switchTurn(tappedPlayer: Player, nowMs: Long = timeSource()): GameState {
        if (state.status != GameStatus.PLAYING) return state
        val movingPlayer = state.activePlayer ?: return state
        if (tappedPlayer != movingPlayer) return state

        // Settle the outgoing clock first. If that tick ends the game, neither
        // the increment nor the handover may happen.
        tick(nowMs)
        if (state.status != GameStatus.PLAYING) return state

        val settledRemaining = state.remainingFor(movingPlayer)
        val remainingAfterIncrement = (settledRemaining + state.incrementMs)
            .coerceAtMost(Long.MAX_VALUE)

        val updated = state.copy(
            whiteRemainingMs = if (movingPlayer == Player.WHITE) {
                remainingAfterIncrement
            } else {
                state.whiteRemainingMs
            },
            blackRemainingMs = if (movingPlayer == Player.BLACK) {
                remainingAfterIncrement
            } else {
                state.blackRemainingMs
            },
            activePlayer = movingPlayer.opponent,
        )

        turnStartedAtMs = nowMs
        turnStartRemainingMs = updated.remainingFor(movingPlayer.opponent)
        state = updated
        return state
    }

    /**
     * Freezes both clocks at their exact remaining time. Intended for
     * ON_STOP; resuming is an explicit user action via [resume].
     */
    fun pause(nowMs: Long = timeSource()): GameState {
        if (state.status != GameStatus.PLAYING) return state
        tick(nowMs)
        if (state.status != GameStatus.GAME_OVER) {
            state = state.copy(status = GameStatus.PAUSED)
        }
        return state
    }

    /** Resumes a paused game without counting time spent in the background. */
    fun resume(nowMs: Long = timeSource()): GameState {
        if (state.status != GameStatus.PAUSED) return state
        val active = state.activePlayer ?: return state
        val remaining = state.remainingFor(active)

        if (remaining == 0L) {
            state = state.copy(
                status = GameStatus.GAME_OVER,
                winner = active.opponent,
                activePlayer = null,
            )
            return state
        }

        turnStartedAtMs = nowMs
        turnStartRemainingMs = remaining
        state = state.copy(status = GameStatus.PLAYING)
        return state
    }

    /** Returns to READY with fresh clocks, optionally with new settings. */
    fun reset(
        durationMs: Long = state.durationMs,
        incrementMs: Long = state.incrementMs,
    ): GameState {
        turnStartedAtMs = 0L
        turnStartRemainingMs = 0L
        state = newReadyState(durationMs, incrementMs)
        return state
    }

    /** Duration and increment can only be changed before the game starts. */
    fun updateSettings(durationMs: Long, incrementMs: Long): GameState {
        if (state.status != GameStatus.READY) return state
        state = newReadyState(durationMs, incrementMs)
        return state
    }

    /**
     * Milliseconds until the caller should tick again: either the next whole
     * second (for a smooth UI) or the exact timeout, whichever comes first.
     */
    fun millisUntilNextUpdate(nowMs: Long = timeSource()): Long {
        if (state.status != GameStatus.PLAYING) return Long.MAX_VALUE
        val elapsed = (nowMs - turnStartedAtMs).coerceAtLeast(0L)
        val remaining = remainingAt(nowMs)
        val untilSecondBoundary = 1000L - (elapsed % 1000L)
        return minOf(untilSecondBoundary, remaining).coerceAtLeast(1L)
    }

    private fun remainingAt(nowMs: Long): Long {
        val elapsed = (nowMs - turnStartedAtMs).coerceAtLeast(0L)
        return (turnStartRemainingMs - elapsed).coerceAtLeast(0L)
    }

    private fun newReadyState(durationMs: Long, incrementMs: Long): GameState = GameState(
        status = GameStatus.READY,
        durationMs = durationMs,
        incrementMs = incrementMs,
        whiteRemainingMs = durationMs,
        blackRemainingMs = durationMs,
        activePlayer = null,
        winner = null,
    )
}
