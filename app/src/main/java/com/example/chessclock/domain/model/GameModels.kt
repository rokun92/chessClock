package com.example.chessclock.domain.model

import java.util.Locale
import kotlin.math.ceil

/** Default starting time for each player: 10 minutes. */
const val DEFAULT_DURATION_MS: Long = 10L * 60L * 1000L

/** Bounds used by the settings screen. */
const val MIN_DURATION_MINUTES = 1
const val MAX_DURATION_MINUTES = 180
const val MIN_INCREMENT_SECONDS = 0
const val MAX_INCREMENT_SECONDS = 60

enum class Player {
    WHITE,
    BLACK,
    ;

    val opponent: Player
        get() = when (this) {
            WHITE -> BLACK
            BLACK -> WHITE
        }
}

enum class GameStatus {
    /** Before Start has been pressed (or after New Game). */
    READY,

    /** A clock is running. */
    PLAYING,

    /** The app became invisible; an explicit Resume is required. */
    PAUSED,

    /** Somebody ran out of time; only New Game is allowed. */
    GAME_OVER,
}

/**
 * Immutable snapshot consumed by Compose. All timing values are in milliseconds.
 * [incrementMs] is the effective increment (0 when increment mode is off).
 */
data class GameState(
    val status: GameStatus = GameStatus.READY,
    val durationMs: Long = DEFAULT_DURATION_MS,
    val incrementMs: Long = 0L,
    val whiteRemainingMs: Long = DEFAULT_DURATION_MS,
    val blackRemainingMs: Long = DEFAULT_DURATION_MS,
    val activePlayer: Player? = null,
    val winner: Player? = null,
) {
    fun remainingFor(player: Player): Long = when (player) {
        Player.WHITE -> whiteRemainingMs
        Player.BLACK -> blackRemainingMs
    }
}

/** Formats milliseconds as MM:SS, rounding partial seconds up for readability. */
fun formatTime(milliseconds: Long): String {
    val totalSeconds = ceil(milliseconds.coerceAtLeast(0L) / 1000.0).toLong()
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}
