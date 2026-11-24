package com.example.chessclock.data

import android.content.Context
import com.example.chessclock.domain.model.MAX_DURATION_MINUTES
import com.example.chessclock.domain.model.MAX_INCREMENT_SECONDS
import com.example.chessclock.domain.model.MIN_DURATION_MINUTES
import com.example.chessclock.domain.model.MIN_INCREMENT_SECONDS

/** User-configurable clock options. Increment is ignored when disabled. */
data class ClockSettings(
    val durationMinutes: Int = DEFAULT_DURATION_MINUTES,
    val incrementEnabled: Boolean = false,
    val incrementSeconds: Int = DEFAULT_INCREMENT_SECONDS,
) {
    val durationMs: Long get() = durationMinutes * MILLIS_PER_MINUTE
    val incrementMs: Long get() = if (incrementEnabled) incrementSeconds * MILLIS_PER_SECOND else 0L

    private companion object {
        const val DEFAULT_DURATION_MINUTES = 10
        const val DEFAULT_INCREMENT_SECONDS = 5
        const val MILLIS_PER_MINUTE = 60_000L
        const val MILLIS_PER_SECOND = 1_000L
    }
}

interface SettingsRepository {
    fun load(): ClockSettings
    fun save(settings: ClockSettings)
}

/** Tiny SharedPreferences-backed store: no extra dependencies needed. */
class SharedPreferencesSettingsRepository(context: Context) : SettingsRepository {
    private val preferences = context.applicationContext
        .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override fun load(): ClockSettings = ClockSettings(
        durationMinutes = preferences.getInt(KEY_DURATION_MINUTES, 10)
            .coerceIn(MIN_DURATION_MINUTES, MAX_DURATION_MINUTES),
        incrementEnabled = preferences.getBoolean(KEY_INCREMENT_ENABLED, false),
        incrementSeconds = preferences.getInt(KEY_INCREMENT_SECONDS, 5)
            .coerceIn(MIN_INCREMENT_SECONDS, MAX_INCREMENT_SECONDS),
    )

    override fun save(settings: ClockSettings) {
        preferences.edit()
            .putInt(KEY_DURATION_MINUTES, settings.durationMinutes.coerceIn(MIN_DURATION_MINUTES, MAX_DURATION_MINUTES))
            .putBoolean(KEY_INCREMENT_ENABLED, settings.incrementEnabled)
            .putInt(KEY_INCREMENT_SECONDS, settings.incrementSeconds.coerceIn(MIN_INCREMENT_SECONDS, MAX_INCREMENT_SECONDS))
            .apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "chess_clock_settings"
        const val KEY_DURATION_MINUTES = "duration_minutes"
        const val KEY_INCREMENT_ENABLED = "increment_enabled"
        const val KEY_INCREMENT_SECONDS = "increment_seconds"
    }
}
