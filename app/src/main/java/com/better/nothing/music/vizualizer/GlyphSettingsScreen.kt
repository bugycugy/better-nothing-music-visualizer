package com.better.nothing.music.vizualizer

import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.math.pow

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun GlyphsScreen(
    gammaValue: Float,
    onGammaChanged: (Float) -> Unit,
    presets: List<AudioCaptureService.PresetInfo>,
    selectedPreset: String,
    onPresetSelected: (String) -> Unit,
    isRunning: Boolean,
    selectedDevice: Int,
    viewModel: MainViewModel,
) {
    val mainScrollState = rememberScrollState()
    val context = LocalContext.current

    val selectedInfo = remember(selectedPreset, presets) {
        presets.firstOrNull { it.key == selectedPreset } ?: presets.firstOrNull()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(mainScrollState)
            .padding(horizontal = 8.dp)
            .animateContentSize(spring(stiffness = Spring.StiffnessLow)),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        ScreenTitle(text = stringResource(R.string.glyph_controls))

        if (selectedDevice == DeviceProfile.DEVICE_NP1 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            if (!isGlyphDebugEnabled(context)) {
                GlyphDebugWarningCard(
                    developerModeEnabled = isDeveloperOptionsEnabled(context)
                )
            }
        }

        Text(
            text = stringResource(R.string.gamma_control),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GammaPreviewCard(gammaValue = gammaValue)
            BodyText(
                text = stringResource(R.string.gamma_description),
                modifier = Modifier.weight(1f),
                size = 14.sp,
                lineHeight = 22.sp,
            )
        }

        GammaCard(gammaValue = gammaValue, onGammaChanged = onGammaChanged)

        Text(
            text = stringResource(R.string.visualizer_presets),
            modifier = Modifier.padding(top = 20.dp),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            presets.forEach { preset ->
                key(preset.key) {
                    NativeFilterChip(
                        label = preset.key,
                        selected = preset.key == selectedPreset,
                        onClick = { onPresetSelected(preset.key) },
                    )
                }
            }
        }

        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                ),
        ) {
            Crossfade(
                targetState = selectedInfo?.description,
                label = "desc_fade",
                animationSpec = spring(stiffness = Spring.StiffnessMedium)
            ) { description ->
                Text(
                    text = description ?: stringResource(R.string.glyph_no_config),
                    style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 22.sp),
                    color = Color(0xFFFFFFFF),
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth(),
                )
            }
        }

        if (isRunning) {
            val vizState by viewModel.visualizerState.collectAsStateWithLifecycle()
            GlyphPreview(
                vizState = vizState,
                device = selectedDevice,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
            )
        }

        Spacer(modifier = Modifier.height(28.dp))
    }
}

@Composable
private fun GlyphDebugWarningCard(
    developerModeEnabled: Boolean,
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.glyph_debug_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            BodyText(
                text = stringResource(if (developerModeEnabled) R.string.glyph_debug_desc_adb_enabled else R.string.glyph_debug_desc_dev_options)
            )
            Text(
                text = stringResource(R.string.glyph_debug_command),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyLarge,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
            )
            if (developerModeEnabled) {
                BodyText(
                    text = stringResource(R.string.glyph_debug_instruction),
                    size = 14.sp,
                    lineHeight = 20.sp,
                )
            }
        }
        Spacer(modifier = Modifier.height(70.dp))
    }
}

private fun isDeveloperOptionsEnabled(context: Context): Boolean {
    return Settings.Global.getInt(
        context.contentResolver,
        Settings.Global.DEVELOPMENT_SETTINGS_ENABLED,
        0
    ) == 1
}

private fun isGlyphDebugEnabled(context: Context): Boolean {
    return Settings.Global.getInt(
        context.contentResolver,
        "nt_glyph_interface_debug_enable",
        0
    ) == 1
}

@Composable
fun GammaCard(
    gammaValue: Float,
    onGammaChanged: (Float) -> Unit,
) {
    val gammaLabel = stringResource(R.string.light_gamma).format(gammaValue)

    Card(
        shape    = RoundedCornerShape(28.dp),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(17.dp),
        ) {
            Text(
                text     = gammaLabel,
                color    = MaterialTheme.colorScheme.primary,
                style    = MaterialTheme.typography.titleMedium,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            ExpressiveSlider(
                value = gammaValue,
                onValueChange = onGammaChanged,
                valueRange = 0.4f..3.5f,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
fun GammaPreviewCard(gammaValue: Float) {
    val animatedGamma by animateFloatAsState(
        targetValue  = gammaValue,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessLow,
        ),
        label = "gamma_curve",
    )

    val curvePath = remember { Path() }

    val gridColor = MaterialTheme.colorScheme.outline
    val accent    = MaterialTheme.colorScheme.primary

    Card(
        shape    = RoundedCornerShape(28.dp),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.size(130.dp, 130.dp),
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(18.dp)) {
            val pad       = 8f
            val right  = size.width - pad
            val bottom = size.height - pad
            val w = right - pad
            val h = bottom - pad

            drawLine(gridColor, Offset(pad, bottom), Offset(right, bottom), strokeWidth = 4f, cap = StrokeCap.Round)
            drawLine(gridColor, Offset(pad, bottom), Offset(pad, pad),    strokeWidth = 4f, cap = StrokeCap.Round)

            val hStep = h / 4f
            val vStep = w / 4f
            repeat(3) { i ->
                drawLine(gridColor, Offset(pad,         bottom - hStep * (i + 1)), Offset(right, bottom - hStep * (i + 1)), strokeWidth = 1f)
                drawLine(gridColor, Offset(pad + vStep * (i + 1), bottom),         Offset(pad + vStep * (i + 1),
                    pad
                ),     strokeWidth = 1f)
            }

            curvePath.reset()
            curvePath.moveTo(pad, bottom)
            val steps = 50
            for (step in 1..steps) {
                val x = step / steps.toFloat()
                val y = x.pow(animatedGamma)
                curvePath.lineTo(pad + x * w, bottom - y * h)
            }
            drawPath(curvePath, accent, style = Stroke(width = 8f, cap = StrokeCap.Round))
        }
    }
}
