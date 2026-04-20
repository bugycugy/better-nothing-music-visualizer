package com.better.nothing.music.vizualizer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlin.math.roundToInt

@Composable
fun AudioScreen(
    isRunning: Boolean,
    latencyMs: Int,
    onLatencyChanged: (Int) -> Unit,
    latencyPresets: List<Int>,
    onLatencyPresetsChanged: (List<Int>) -> Unit,
    autoDeviceEnabled: Boolean,
    onAutoDeviceToggle: (Boolean) -> Unit,
    connectedDeviceName: String? = null
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Launcher to handle the Bluetooth permission request
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onAutoDeviceToggle(true)
        }
    }

    // Logic to handle the toggle with permission check
    val handleAutoToggle: (Boolean) -> Unit = { setEnabled ->
        if (setEnabled) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val status = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_CONNECT
                )
                if (status == PackageManager.PERMISSION_GRANTED) {
                    onAutoDeviceToggle(true)
                } else {
                    permissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                }
            } else {
                onAutoDeviceToggle(true)
            }
        } else {
            onAutoDeviceToggle(false)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        ScreenTitle(text = "Better Nothing\nMusic Visualizer")

        val descriptionText = if (isRunning) {
            "Real time audio visualizer is active. Your phone is now dancing to the beat! " +
                    "No content is saved, and privacy is respected."
        } else {
            "To synchronize the Glyph Interface with your music, this app captures " +
                    "device audio. We use Media Projection for high-fidelity visualization.\n\n" +
                    "Privacy Note: We only utilize the audio stream. No screen content is recorded."
        }
        BodyText(text = descriptionText)

        AnimatedVisibility(visible = isRunning) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

                AutoDeviceCard(
                    enabled = autoDeviceEnabled,
                    onToggle = handleAutoToggle,
                    deviceName = connectedDeviceName
                )

                BodyText(
                    text = "Latency compensation ensures Glyphs hit exactly on the beat, " +
                            "especially useful for Bluetooth devices."
                )

                LatencyCard(
                    latencyMs = latencyMs,
                    onLatencyChanged = onLatencyChanged,
                    latencyPresets = latencyPresets,
                    onLatencyPresetsChanged = onLatencyPresetsChanged,
                )
            }
        }
    }
}

@Composable
fun AutoDeviceCard(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    deviceName: String?
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1B1B)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Auto-Memorize Device",
                    color = Color.White,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = if (enabled)
                        "Saving latency for: ${deviceName ?: "Internal Speaker"}"
                    else "Manual mode (Global latency)",
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFFB5F2B6),
                    checkedTrackColor = Color(0xFF403F44)
                )
            )
        }
    }
}

@Composable
fun LatencyCard(
    latencyMs: Int,
    onLatencyChanged: (Int) -> Unit,
    latencyPresets: List<Int>,
    onLatencyPresetsChanged: (List<Int>) -> Unit,
) {
    val haptics = LocalHapticFeedback.current

    // KEY FIX: Track the ID (index) of the preset being edited.
    // We initialize it by finding which index currently matches latencyMs.
    var draggingIndex by remember { mutableIntStateOf(-1) }

    // Sync the local selection with the external state only when NOT dragging
    val activeIndex = if (draggingIndex != -1) draggingIndex else latencyPresets.indexOf(latencyMs)

    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF242222)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(text = "Latency Compensation", color = Color(0xFFE6E1E3), style = MaterialTheme.typography.titleMedium)

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(Color(0xFF1C1B1B), RoundedCornerShape(24.dp))
                    .padding(4.dp)
            ) {
                val itemWidth = (maxWidth - (4.dp * (latencyPresets.size - 1))) / latencyPresets.size

                // Sort by value but keep track of the original index to maintain "Box Identity"
                val sortedWithIndices = latencyPresets.mapIndexed { i, v -> i to v }.sortedBy { it.second }

                latencyPresets.forEachIndexed { index, preset ->
                    val isSelected = index == activeIndex
                    val visualIndex = sortedWithIndices.indexOfFirst { it.first == index }

                    LaunchedEffect(visualIndex) {
                        haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                    }

                    val targetOffset = (itemWidth + 4.dp) * visualIndex
                    val animatedX by animateDpAsState(
                        targetValue = targetOffset,
                        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow),
                        label = "bouncy_swap"
                    )

                    Box(
                        modifier = Modifier
                            .width(itemWidth)
                            .fillMaxHeight()
                            .offset(x = animatedX)
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) Color(0xFF403F44) else Color(0xFF2B2929))
                            .clickable {
                                draggingIndex = index
                                onLatencyChanged(preset)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${preset}ms",
                            color = if (isSelected) Color(0xFFB5F2B6) else Color(0xFFE6E1E3),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }

            ExpressiveSlider(
                value = latencyMs.toFloat(),
                onValueChange = { newValue ->
                    val intValue = newValue.toInt()

                    // 1. If we don't know who is being dragged yet, find the closest match
                    if (draggingIndex == -1) {
                        draggingIndex = latencyPresets.indexOf(latencyMs)
                    }

                    // 2. Update engine
                    onLatencyChanged(intValue)

                    // 3. Update list memory using the locked index
                    if (draggingIndex != -1) {
                        val currentList = latencyPresets.toMutableList()

                        // Collision check: Is some OTHER slot already using this value?
                        val isColliding = currentList.mapIndexed { i, v -> i to v }
                            .any { (i, v) -> i != draggingIndex && v == intValue }

                        if (!isColliding) {
                            currentList[draggingIndex] = intValue
                            onLatencyPresetsChanged(currentList)
                        }
                    }
                },
                valueRange = 0f..500f,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}