package com.example.chessclock

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.chessclock.domain.model.GameStatus
import com.example.chessclock.ui.components.SystemBars
import com.example.chessclock.ui.screens.GameOverDialog
import com.example.chessclock.ui.screens.GameScreen
import com.example.chessclock.ui.screens.PausedDialog
import com.example.chessclock.ui.screens.SettingsDialog
import com.example.chessclock.ui.screens.StartScreen
import com.example.chessclock.ui.theme.ChessClockTheme
import com.example.chessclock.viewmodel.ChessClockViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val viewModel = ViewModelProvider(this)[ChessClockViewModel::class.java]
        lifecycle.addObserver(
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_STOP) {
                    viewModel.onAppBackground()
                }
            },
        )

        setContent {
            ChessClockTheme {
                ChessClockApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
private fun ChessClockApp(viewModel: ChessClockViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var showSettings by rememberSaveable { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize()) {
        when (state.status) {
            GameStatus.READY -> {
                SystemBars(visible = true)
                StartScreen(
                    onStartClick = viewModel::startGame,
                    onSettingsClick = { showSettings = true },
                )
            }

            GameStatus.PLAYING, GameStatus.PAUSED -> {
                SystemBars(visible = false)
                GameScreen(
                    state = state,
                    onPlayerTap = viewModel::onPlayerTap,
                )
            }

            GameStatus.GAME_OVER -> {
                SystemBars(visible = false)
                GameScreen(state = state, onPlayerTap = {})
                state.winner?.let { winner ->
                    GameOverDialog(
                        winner = winner,
                        onNewGameClick = viewModel::newGame,
                    )
                }
            }
        }

        if (state.status == GameStatus.PAUSED) {
            PausedDialog(onResumeClick = viewModel::resumeGame)
        }

        if (showSettings && state.status == GameStatus.READY) {
            SettingsDialog(
                settings = settings,
                onDismiss = { showSettings = false },
                onSave = viewModel::saveSettings,
            )
        }
    }
}
