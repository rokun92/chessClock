package com.example.chessclock.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.chessclock.data.ClockSettings
import com.example.chessclock.domain.model.MAX_DURATION_MINUTES
import com.example.chessclock.domain.model.MAX_INCREMENT_SECONDS
import com.example.chessclock.domain.model.MIN_DURATION_MINUTES
import com.example.chessclock.domain.model.MIN_INCREMENT_SECONDS
import com.example.chessclock.ui.theme.Charcoal
import com.example.chessclock.ui.theme.MutedText

private val DURATION_PRESETS = listOf(1, 3, 5, 10, 15, 25)
private val INCREMENT_PRESETS = listOf(3, 5, 10)

@Composable
fun SettingsDialog(
    settings: ClockSettings,
    onDismiss: () -> Unit,
    onSave: (ClockSettings) -> Unit,
) {
    var durationText by rememberSaveable(settings.durationMinutes) {
        mutableStateOf(settings.durationMinutes.toString())
    }
    var incrementEnabled by rememberSaveable(settings.incrementEnabled) {
        mutableStateOf(settings.incrementEnabled)
    }
    var incrementText by rememberSaveable(settings.incrementSeconds) {
        mutableStateOf(settings.incrementSeconds.toString())
    }

    val durationMinutes = durationText.toIntOrNull()
        ?.takeIf { it in MIN_DURATION_MINUTES..MAX_DURATION_MINUTES }
    val incrementSeconds = incrementText.toIntOrNull()
        ?.takeIf { it in MIN_INCREMENT_SECONDS..MAX_INCREMENT_SECONDS }
    val canApply = durationMinutes != null && (!incrementEnabled || incrementSeconds != null)

    Dialog(onDismissRequest = onDismiss) {
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
                    .padding(horizontal = 24.dp, vertical = 28.dp),
            ) {
                Text(
                    text = "Clock settings",
                    color = Charcoal,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(text = "Starting time", color = Charcoal, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Minutes for each player", color = MutedText, fontSize = 14.sp)

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    DURATION_PRESETS.forEach { minutes ->
                        OutlinedButton(
                            onClick = { durationText = minutes.toString() },
                            modifier = Modifier.padding(end = 8.dp),
                        ) {
                            Text(text = "$minutes min")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = durationText,
                    onValueChange = { value ->
                        if (value.length <= 3 && value.all { it.isDigit() }) {
                            durationText = value
                        }
                    },
                    label = { Text(text = "Custom minutes ($MIN_DURATION_MINUTES-$MAX_DURATION_MINUTES)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = durationText.isNotEmpty() && durationMinutes == null,
                    modifier = Modifier.fillMaxWidth(),
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Time increment",
                            color = Charcoal,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "Add time after each move",
                            color = MutedText,
                            fontSize = 13.sp,
                        )
                    }
                    Switch(
                        checked = incrementEnabled,
                        onCheckedChange = { incrementEnabled = it },
                    )
                }

                if (incrementEnabled) {
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        INCREMENT_PRESETS.forEach { seconds ->
                            OutlinedButton(
                                onClick = { incrementText = seconds.toString() },
                                modifier = Modifier.padding(end = 8.dp),
                            ) {
                                Text(text = "${seconds}s")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = incrementText,
                        onValueChange = { value ->
                            if (value.length <= 2 && value.all { it.isDigit() }) {
                                incrementText = value
                            }
                        },
                        label = { Text(text = "Seconds per move ($MIN_INCREMENT_SECONDS-$MAX_INCREMENT_SECONDS)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        isError = incrementText.isNotEmpty() && incrementSeconds == null,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(text = "Cancel", color = MutedText)
                    }
                    Button(
                        onClick = {
                            durationMinutes?.let { minutes ->
                                if (canApply) {
                                    onSave(
                                        ClockSettings(
                                            durationMinutes = minutes,
                                            incrementEnabled = incrementEnabled,
                                            incrementSeconds = incrementSeconds ?: 0,
                                        ),
                                    )
                                    onDismiss()
                                }
                            }
                        },
                        enabled = canApply,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Charcoal,
                            contentColor = Color.White,
                        ),
                    ) {
                        Text(text = "Apply")
                    }
                }
            }
        }
    }
}
