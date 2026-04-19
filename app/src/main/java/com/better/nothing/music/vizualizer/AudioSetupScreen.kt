package com.better.nothing.music.vizualizer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AudioScreen(
    isRunning: Boolean,
    latencyMs: Int,
    onLatencyChanged: (Int) -> Unit,
    latencyPresets: List<Int>,
    onLatencyPresetsChanged: (List<Int>) -> Unit,
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 28.dp, vertical = 28.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        ScreenTitle(text = "Better Nothing\nMusic Visualizer")
        BodyText(
            text = "To synchronize the Glyph Interface with your music, this app needs to capture " +
                    "process the device's real-time audio output. We use the Media Projection API " +
                    "to ensure a high-fidelity audio capture for the best visualization.\n\n" +
                    "Privacy Note: Don't be scared, we only utilize the audio stream. This app does " +
                    "not record or even view your screen content. Because we bypass video processing " +
                    "entirely, the app remains lightweight and avoids the battery drain associated " +
                    "with traditional screen recording."
        )
        AnimatedVisibility(visible = isRunning) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                BodyText(
                    text = "This latency compensation slider is for when you're using Bluetooth " +
                            "audio devices for example."
                )
                LatencyCard(
                    latencyMs               = latencyMs,
                    onLatencyChanged        = onLatencyChanged,
                    latencyPresets          = latencyPresets,
                    onLatencyPresetsChanged = onLatencyPresetsChanged,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LatencyCard(
    latencyMs: Int,
    onLatencyChanged: (Int) -> Unit,
    latencyPresets: List<Int>,
    onLatencyPresetsChanged: (List<Int>) -> Unit,
) {
    // Identify which button is currently "active" by its value
    val selectedIndex = latencyPresets.indexOf(latencyMs)

    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF242222)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Latency Compensation",
                color = Color(0xFFE6E1E3),
                style = MaterialTheme.typography.titleMedium,
            )

            // Native M3 Connected Button Group
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth()
            ) {
                latencyPresets.forEachIndexed { index, preset ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = latencyPresets.size
                        ),
                        onClick = { onLatencyChanged(preset) },
                        selected = index == selectedIndex,
                        // Customizing colors to match your dark theme
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = Color(0xFF403F44),
                            activeContentColor = Color(0xFFB5F2B6),
                            inactiveContainerColor = Color(0xFF1C1B1B),
                            inactiveContentColor = Color(0xFFE6E1E3)
                        ),
                        label = {
                            Text(
                                text = "${preset}ms",
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        icon = {} // Removing the checkmark for a cleaner look
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ExpressiveSlider(
                    value = latencyMs.toFloat(),
                    onValueChange = { newValue ->
                        val intValue = newValue.toInt()

                        // 1. Update the engine immediately
                        onLatencyChanged(intValue)

                        // 2. Update the specific preset slot and auto-sort
                        val updatedList = latencyPresets.toMutableList()

                        // If current value isn't a preset, update the one that WAS selected
                        val indexToUpdate = if (selectedIndex != -1) selectedIndex else 0
                        updatedList[indexToUpdate] = intValue

                        // 3. Emit the sorted list back to the ViewModel/Persistence
                        onLatencyPresetsChanged(updatedList.sorted())
                    },
                    valueRange = 0f..500f,
                    modifier = Modifier.fillMaxWidth(),
                )

                Text(
                    text = "${latencyMs}ms",
                    color = Color(0xFFB5F2B6),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}