package com.example.chessclock.domain

import com.example.chessclock.domain.model.DEFAULT_DURATION_MS
import com.example.chessclock.domain.model.GameStatus
import com.example.chessclock.domain.model.Player
import com.example.chessclock.domain.model.formatTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class ChessClockEngineTest {

    private lateinit var clock: FakeClock
    private lateinit var engine: ChessClockEngine

    @Before
    fun setUp() {
        clock = FakeClock()
        engine = ChessClockEngine(clock.source)
    }

    @Test
    fun `initial state has ten minutes for both players and no increment`() {
        val state = engine.currentState()

        assertEquals(GameStatus.READY, state.status)
        assertEquals(DEFAULT_DURATION_MS, state.durationMs)
        assertEquals(0L, state.incrementMs)
        assertEquals(DEFAULT_DURATION_MS, state.whiteRemainingMs)
        assertEquals(DEFAULT_DURATION_MS, state.blackRemainingMs)
        assertNull(state.activePlayer)
        assertNull(state.winner)
    }

    @Test
    fun `starting the game makes white active and starts only white clock`() {
        val state = engine.start()

        assertEquals(GameStatus.PLAYING, state.status)
        assertEquals(Player.WHITE, state.activePlayer)
        assertEquals(DEFAULT_DURATION_MS, state.whiteRemainingMs)
        assertEquals(DEFAULT_DURATION_MS, state.blackRemainingMs)
        assertNull(state.winner)

        clock.advance(2_000L)
        val afterTick = engine.tick()
        assertEquals(DEFAULT_DURATION_MS - 2_000L, afterTick.whiteRemainingMs)
        assertEquals(DEFAULT_DURATION_MS, afterTick.blackRemainingMs)
    }

    @Test
    fun `switching from white to black settles white time exactly`() {
        engine.start()
        clock.advance(3_000L)

        val state = engine.switchTurn(Player.WHITE)

        assertEquals(Player.BLACK, state.activePlayer)
        assertEquals(DEFAULT_DURATION_MS - 3_000L, state.whiteRemainingMs)
        assertEquals(DEFAULT_DURATION_MS, state.blackRemainingMs)
        assertEquals(GameStatus.PLAYING, state.status)

        clock.advance(2_000L)
        val afterTick = engine.tick()
        assertEquals(DEFAULT_DURATION_MS - 3_000L, afterTick.whiteRemainingMs)
        assertEquals(DEFAULT_DURATION_MS - 2_000L, afterTick.blackRemainingMs)
    }

    @Test
    fun `switching from black to white settles black time exactly`() {
        engine.start()
        clock.advance(2_000L)
        engine.switchTurn(Player.WHITE)
        clock.advance(4_000L)

        val state = engine.switchTurn(Player.BLACK)

        assertEquals(Player.WHITE, state.activePlayer)
        assertEquals(DEFAULT_DURATION_MS - 2_000L, state.whiteRemainingMs)
        assertEquals(DEFAULT_DURATION_MS - 4_000L, state.blackRemainingMs)
    }

    @Test
    fun `tapping the non-active player is ignored`() {
        val started = engine.start()
        clock.advance(3_000L)

        val ignored = engine.switchTurn(Player.BLACK)

        assertEquals(started, ignored)
        assertEquals(Player.WHITE, ignored.activePlayer)

        // The active clock keeps running normally after the ignored tap.
        clock.advance(1_000L)
        assertEquals(DEFAULT_DURATION_MS - 4_000L, engine.tick().whiteRemainingMs)
    }

    @Test
    fun `time increment is added to the moving player after each switch`() {
        val incrementClock = FakeClock()
        val incrementEngine = ChessClockEngine(
            timeSource = incrementClock.source,
            initialIncrementMs = 5_000L,
        )
        incrementEngine.start()

        incrementClock.advance(3_000L)
        val afterWhiteMove = incrementEngine.switchTurn(Player.WHITE)

        assertEquals(Player.BLACK, afterWhiteMove.activePlayer)
        assertEquals(DEFAULT_DURATION_MS - 3_000L + 5_000L, afterWhiteMove.whiteRemainingMs)
        assertEquals(DEFAULT_DURATION_MS, afterWhiteMove.blackRemainingMs)

        incrementClock.advance(2_000L)
        val afterBlackMove = incrementEngine.switchTurn(Player.BLACK)

        assertEquals(Player.WHITE, afterBlackMove.activePlayer)
        assertEquals(DEFAULT_DURATION_MS - 2_000L + 5_000L, afterBlackMove.blackRemainingMs)
        assertEquals(DEFAULT_DURATION_MS - 3_000L + 5_000L, afterBlackMove.whiteRemainingMs)
    }

    @Test
    fun `increment mode can be turned off and settings are locked while playing`() {
        val configured = engine.updateSettings(DEFAULT_DURATION_MS, 5_000L)
        assertEquals(5_000L, configured.incrementMs)

        engine.start()
        val ignored = engine.updateSettings(60_000L, 0L)
        assertEquals(configured.durationMs, ignored.durationMs)
        assertEquals(configured.incrementMs, ignored.incrementMs)

        clock.advance(1_000L)
        val afterSwitch = engine.switchTurn(Player.WHITE)
        assertEquals(DEFAULT_DURATION_MS - 1_000L + 5_000L, afterSwitch.whiteRemainingMs)

        val reset = engine.reset()
        assertEquals(5_000L, reset.incrementMs)
        assertEquals(DEFAULT_DURATION_MS, reset.whiteRemainingMs)
    }

    @Test
    fun `remaining time is calculated from real elapsed duration without drift`() {
        engine.start()

        // Simulate a coroutine that wakes up late: the calculation must not
        // depend on how many ticks happened or their individual delays.
        clock.advance(5_000L)
        engine.tick()
        clock.advance(5_500L)
        val state = engine.tick()

        assertEquals(DEFAULT_DURATION_MS - 10_500L, state.whiteRemainingMs)
        assertEquals("09:50", formatTime(state.whiteRemainingMs))
    }

    @Test
    fun `repeated rapid switches do not lose or gain time`() {
        engine.start()
        repeat(10) {
            clock.advance(137L)
            engine.switchTurn(engine.currentState().activePlayer!!)
        }

        val state = engine.tick()

        assertEquals(Player.WHITE, state.activePlayer)
        assertEquals(DEFAULT_DURATION_MS - 685L, state.whiteRemainingMs)
        assertEquals(DEFAULT_DURATION_MS - 685L, state.blackRemainingMs)
    }

    @Test
    fun `white losing on timeout ends the game and black wins`() {
        engine.start()
        clock.advance(DEFAULT_DURATION_MS)

        val state = engine.tick()

        assertEquals(GameStatus.GAME_OVER, state.status)
        assertEquals(Player.BLACK, state.winner)
        assertNull(state.activePlayer)
        assertEquals(0L, state.whiteRemainingMs)
        assertEquals(DEFAULT_DURATION_MS, state.blackRemainingMs)
    }

    @Test
    fun `black losing on timeout ends the game and white wins`() {
        engine.start()
        clock.advance(1_000L)
        engine.switchTurn(Player.WHITE)
        clock.advance(DEFAULT_DURATION_MS)

        val state = engine.tick()

        assertEquals(GameStatus.GAME_OVER, state.status)
        assertEquals(Player.WHITE, state.winner)
        assertNull(state.activePlayer)
        assertEquals(0L, state.blackRemainingMs)
        assertEquals(DEFAULT_DURATION_MS - 1_000L, state.whiteRemainingMs)
    }

    @Test
    fun `new game resets the complete state`() {
        engine.start()
        clock.advance(DEFAULT_DURATION_MS)
        val gameOver = engine.tick()
        assertEquals(GameStatus.GAME_OVER, gameOver.status)

        val state = engine.reset()

        assertEquals(GameStatus.READY, state.status)
        assertEquals(DEFAULT_DURATION_MS, state.whiteRemainingMs)
        assertEquals(DEFAULT_DURATION_MS, state.blackRemainingMs)
        assertEquals(0L, state.incrementMs)
        assertNull(state.activePlayer)
        assertNull(state.winner)
    }

    @Test
    fun `all interactions are ignored after game over`() {
        engine.start()
        clock.advance(DEFAULT_DURATION_MS)
        val gameOver = engine.tick()

        clock.advance(10_000L)
        assertEquals(gameOver, engine.tick())
        assertEquals(gameOver, engine.switchTurn(Player.WHITE))
        assertEquals(gameOver, engine.start())
        assertEquals(gameOver, engine.pause())
        assertEquals(gameOver, engine.resume())
        assertEquals(gameOver, engine.updateSettings(60_000L, 3_000L))
    }

    @Test
    fun `background pause freezes clocks and resume needs an explicit action`() {
        engine.start()
        clock.advance(5_000L)

        val paused = engine.pause()
        assertEquals(GameStatus.PAUSED, paused.status)
        assertEquals(DEFAULT_DURATION_MS - 5_000L, paused.whiteRemainingMs)

        clock.advance(60_000L)
        assertEquals(paused, engine.tick())
        assertEquals(paused, engine.switchTurn(Player.WHITE))

        engine.resume()
        clock.advance(2_000L)
        val afterResume = engine.tick()

        assertEquals(GameStatus.PLAYING, afterResume.status)
        assertEquals(DEFAULT_DURATION_MS - 7_000L, afterResume.whiteRemainingMs)
    }

    @Test
    fun `next tick deadline is the next second or the exact timeout`() {
        engine.start()
        assertEquals(1_000L, engine.millisUntilNextUpdate())

        clock.advance(400L)
        assertEquals(600L, engine.millisUntilNextUpdate())

        val shortEngine = ChessClockEngine(clock.source, initialDurationMs = 700L)
        clock.advance(5_000L)
        shortEngine.start()
        assertEquals(700L, shortEngine.millisUntilNextUpdate())
        clock.advance(400L)
        assertEquals(300L, shortEngine.millisUntilNextUpdate())
    }

    @Test
    fun `time formatting uses MM SS with whole seconds displayed`() {
        assertEquals("10:00", formatTime(DEFAULT_DURATION_MS))
        assertEquals("00:00", formatTime(0L))
        assertEquals("00:01", formatTime(1L))
        assertEquals("01:00", formatTime(59_999L))
        assertEquals("00:09", formatTime(8_001L))
    }

    private class FakeClock(initialTimeMs: Long = 0L) {
        private var nowMs = initialTimeMs
        val source: () -> Long = { nowMs }

        fun advance(milliseconds: Long) {
            nowMs += milliseconds
        }
    }
}
