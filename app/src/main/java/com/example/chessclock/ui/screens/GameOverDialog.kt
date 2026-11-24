package com.example.chessclock.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chessclock.domain.model.Player
import com.example.chessclock.ui.theme.BoardBlack
import com.example.chessclock.ui.theme.BoardWhite
import com.example.chessclock.ui.theme.Charcoal

@Composable
fun GameOverDialog(
    winner: Player,
    onNewGameClick: () -> Unit,
) {
    Dialog(onDismissRequest = { /* Game over can only be dismissed with New Game. */ }) {
        Card(
            modifier = Modifier
                .wrapContentSize()
                .padding(20.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "GAME OVER",
                    color = Charcoal,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                )

                Spacer(modifier = Modifier.height(24.dp))

                val winnerBackground = if (winner == Player.WHITE) BoardWhite else BoardBlack
                val winnerForeground = if (winner == Player.WHITE) BoardBlack else BoardWhite
                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .background(winnerBackground, CircleShape)
                        .border(2.dp, winnerForeground.copy(alpha = 0.35f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = winner.name.first().toString(),
                        color = winnerForeground,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "${winner.name} WINS!",
                    color = Charcoal,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                )

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = onNewGameClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Charcoal,
                        contentColor = Color.White,
                    ),
                ) {
                    Text(text = "New Game", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
