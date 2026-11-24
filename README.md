# Chess Clock (Kotlin + Jetpack Compose)

A minimal, full-screen chess clock for Android. Two equal halves, one running
clock at a time, accurate monotonic timing, and no unnecessary UI.

## Architecture

- `domain/model/GameModels.kt` — `GameState`, `Player`, `GameStatus`, time formatting.
- `domain/ChessClockEngine.kt` — pure Kotlin clock/turn engine. It never schedules
  work and has no Android dependencies; the caller passes the current monotonic
  time and receives the next deadline.
- `data/SettingsRepository.kt` — SharedPreferences-backed `ClockSettings` (duration,
  increment on/off, increment seconds).
- `viewmodel/ChessClockViewModel.kt` — owns a single coroutine ticker, maps engine
  state to a `StateFlow`, and handles lifecycle pause/resume.
- `ui/` — stateless Compose screens and dialogs.

### Timing and lifecycle

`SystemClock.elapsedRealtime()` is the time source. Remaining time is always
computed as `turnStartRemaining - (now - turnStartedAt)`, so delayed coroutine
wake-ups never accumulate drift. The ticker wakes on second boundaries and also
at the exact timeout. Only one ticker job exists; it is cancelled before being
recreated and cancelled on `ON_STOP`.

When the app becomes invisible the game is frozen (`PAUSED`). Returning to the
app does **not** restart the clock: a modal **Resume** button is the explicit
user action. Rotation is handled by the ViewModel, so the clock keeps its state.

## Features

- Start screen with a centered Start button and a subtle settings icon.
- Settings dialog with duration presets (1/3/5/10/15/25 min) or any custom value 1–180 min.
- Two increment modes: off, or add a custom 0–60 seconds to the moving player after each move.
- Default 10:00 per player, MM:SS monospaced digits.
- Bottom (White) half is always white; top (Black) half is always black.
- Only a tap on the **currently active** half hands over; taps on the inactive half
  and after game over are ignored.
- Winner dialog with "GAME OVER", "WHITE WINS!" / "BLACK WINS!", and New Game.
- New Game returns to the start screen and clears the winner.
- System bars are hidden during gameplay and shown again on the start screen.

## Build and run

Requirements: JDK 17+, Android SDK platform 35 and build-tools 35.0.0.

### Android Studio

1. Open this directory as a project.
2. Let Gradle sync.
3. Select an emulator (API 24+) or a connected device and press **Run**.

### Command line

```bash
./gradlew assembleDebug          # build app/build/outputs/apk/debug/app-debug.apk
./gradlew installDebug           # install on a connected device/emulator
./gradlew test                   # run local unit tests
```

To install on a specific device:

```bash
adb devices
./gradlew installDebug -Pandroid.injected.select.device=<serial>
```

Or install the built APK directly:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Unit tests

`app/src/test/java/com/example/chessclock/domain/ChessClockEngineTest.kt` covers
initial values, starting, both switch directions, ignoring taps on the inactive
side, increment on/off behavior, exact remaining-time math, rapid switches, both
timeout winners, full state reset, post-game-over input guard, pause/resume, tick
deadlines, and time formatting.

Run them with `./gradlew testDebugUnitTest` and view the report at
`app/build/reports/tests/testDebugUnitTest/index.html`.
