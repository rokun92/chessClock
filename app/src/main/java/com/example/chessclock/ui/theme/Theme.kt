package com.example.chessclock.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Charcoal,
    onPrimary = Color.White,
    secondary = MutedText,
    onSecondary = Color.White,
    background = NeutralBackground,
    onBackground = Charcoal,
    surface = Color.White,
    onSurface = Charcoal,
)

/** Fixed light palette: the two player halves must always be black and white. */
@Composable
fun ChessClockTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography(),
        content = content,
    )
}
