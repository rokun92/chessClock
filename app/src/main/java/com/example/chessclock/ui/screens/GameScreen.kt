package com.example.chessclock.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chessclock.domain.model.GameState
import com.example.chessclock.domain.model.GameStatus
import com.example.chessclock.domain.model.Player
import com.example.chessclock.domain.model.formatTime
import com.example.chessclock.ui.theme.BoardBlack
import com.example.chessclock.ui.theme.BoardWhite
import kotlin.math.min

@Composable
fun GameScreen(
    state: GameState,
    onPlayerTap: (Player) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().background(BoardBlack)) {
        PlayerSection(
            player = Player.BLACK,
            state = state,
            onTap = onPlayerTap,
            modifier = Modifier.weight(1f),
        )
        PlayerSection(
            player = Player.WHITE,
            state = state,
            onTap = onPlayerTap,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun PlayerSection(
    player: Player,
    state: GameState,
    onTap: (Player) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isBlack = player == Player.BLACK
    val backgroundColor = if (isBlack) BoardBlack else BoardWhite
    val foregroundColor = if (isBlack) BoardWhite else BoardBlack
    val isActive = state.status == GameStatus.PLAYING && state.activePlayer == player
    val time = formatTime(state.remainingFor(player))
    val description = "${player.name.lowercase()} clock, $time" +
        if (isActive) ", your turn, tap to move" else ", waiting"

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .clickable(enabled = state.status == GameStatus.PLAYING) { onTap(player) }
            .semantics(mergeDescendants = true) { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        val timerFontSize = min(maxWidth.value / 5.2f, maxHeight.value / 2.1f)
            .coerceIn(40f, 132f)
            .sp

        Text(
            text = time,
            color = foregroundColor,
            fontSize = timerFontSize,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
        )

        if (isActive) {
            Text(
                text = "YOUR TURN",
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 28.dp)
                    .background(
                        color = foregroundColor.copy(alpha = if (isBlack) 0.16f else 0.08f),
                        shape = RoundedCornerShape(50),
                    )
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                color = foregroundColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
            )

            Box(
                modifier = Modifier
                    .align(if (isBlack) Alignment.BottomCenter else Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(foregroundColor.copy(alpha = 0.35f)),
            )
        }
    }
}
