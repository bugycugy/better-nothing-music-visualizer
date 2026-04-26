package com.better.nothing.music.vizualizer

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.pow
import androidx.compose.ui.graphics.drawscope.withTransform
import kotlin.math.min
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.DrawScope

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GlyphsScreen(
    gammaValue: Float,
    onGammaChanged: (Float) -> Unit,
    presets: List<AudioCaptureService.PresetInfo>,
    selectedPreset: String,
    onPresetSelected: (String) -> Unit,
    isRunning: Boolean,
    vizState: FloatArray,
    selectedDevice: Int,
) {
    val mainScrollState = rememberScrollState()

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

        if (isRunning && vizState.isNotEmpty()) {
            GlyphPreview(
                vizState = vizState,
                device = selectedDevice,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp)
            )
        }

        ScreenTitle(text = stringResource(R.string.glyph_controls))

        Text(
            text = stringResource(R.string.gamma_control),
            style = MaterialTheme.typography.headlineMedium,
            color = Color(0xFFD2D2D2),
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
            color = Color(0xFFD2D2D2),
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
                    text = description ?: "Text describing the preset in a nice way.",
                    style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 22.sp),
                    color = Color(0xFFFFFFFF),
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth(),
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))
    }
}

@Composable
fun GlyphPreview(
    vizState: FloatArray,
    device: Int,
    modifier: Modifier = Modifier
) {
    val color = MaterialTheme.colorScheme.primary
    val baseOpacity = 0.15f

    // --- SMOOTHING LOGIC (SLOWER) ---
    val smoothedState = remember { mutableStateOf(FloatArray(0)) }
    LaunchedEffect(vizState) {
        if (smoothedState.value.size != vizState.size) {
            smoothedState.value = vizState.copyOf()
        } else {
            val next = FloatArray(vizState.size)
            for (i in vizState.indices) {
                val target = vizState[i]
                val current = smoothedState.value[i]
                val factor = if (target > current) 0.15f else 0.05f // Even slower smoothing
                next[i] = current + (target - current) * factor
            }
            smoothedState.value = next
        }
    }

    fun getAlpha(index: Int): Float {
        val value = smoothedState.value.getOrElse(index) { 0f }
        return baseOpacity + (value * (1f - baseOpacity))
    }

    val paths = remember {
        mutableMapOf<String, Path>().apply {
            // P1
            put("p1_cam", PathParser().parsePathString("m9.704 68.077v-35.9c-0-11.993 9.537-21.814 21.529-22.167 11.993-0.353 22.081 8.899 22.788 20.875 0.017 0.336-0.104 0.663-0.328 0.913-0.232 0.241-0.551 0.379-0.887 0.379h-3.67c-0.638 0-1.172-0.5-1.215-1.137-0.586-8.349-7.538-14.931-16.033-14.931-8.874 0-16.068 7.194-16.068 16.068v35.9c0 8.874 7.194 16.068 16.068 16.068s16.067-7.194 16.067-16.068v-10.381c0-0.672 0.543-1.224 1.224-1.224h3.661c0.672 0 1.224 0.543 1.224 1.224v10.381c0 7.926-4.23 15.241-11.088 19.204s-15.318 3.963-22.176 0c-6.866-3.963-11.096-11.287-11.096-19.204z").toPath())
            put("p1_slash", PathParser().parsePathString("m120.51 63.373c-0.698 0.835-0.905 1.981-0.534 3.006 0.37 1.026 1.266 1.775 2.334 1.965 1.069 0.189 2.171-0.207 2.869-1.043l33.712-40.173c1.085-1.292 0.913-3.214-0.379-4.299-1.292-1.086-3.214-0.913-4.299 0.379l-33.703 40.165z").toPath())
            put("p1_ring", PathParser().parsePathString("m173.49 194.58c0-1.69-1.37-3.05-3.05-3.05-1.69 0-3.05 1.37-3.05 3.05v51.82c0 1.81-0.62 3.57-1.76 4.99-18.23 22.52-45.65 35.62-74.63 35.62s-56.4-13.1-74.63-35.62c-1.14-1.42-1.76-3.18-1.76-4.99v-110.73c-0-1.81 0.62-3.57 1.76-4.98 18.09-22.37 45.25-35.44 74.01-35.63 28.76-0.18 56.09 12.54 74.46 34.67 0.7 0.83 1.79 1.24 2.86 1.06 1.08-0.18 1.98-0.93 2.35-1.95 0.38-1.02 0.19-2.17-0.51-3.01-19.54-23.53-48.61-37.07-79.19-36.87-30.6 0.2-59.48 14.1-78.72 37.89-2.03 2.5-3.13 5.62-3.13 8.83v110.72c-0 3.21 1.1 6.33 3.13 8.83 19.38 23.97 48.54 37.89 79.37 37.89s59.99-13.92 79.37-37.89c2.03-2.5 3.13-5.62 3.13-8.83v-51.82h-0.01z").toPath())
            put("p1_battery", PathParser().parsePathString("m90.991 356.73c1.689 0 3.05-1.37 3.05-3.05v-41.879c0-1.689-1.37-3.05-3.05-3.05s-3.05 1.37-3.05 3.05v41.879c0 1.688 1.37 3.05 3.05 3.05z").toPath())
            put("p1_dot", PathParser().parsePathString("m90.991 371c1.689 0 3.05-1.37 3.05-3.05v-1.835c0-1.688-1.37-3.05-3.05-3.05s-3.05 1.37-3.05 3.05v1.835c0 1.68 1.37 3.05 3.05 3.05z").toPath())

            // P2
            put("p2_cam_t", PathParser().parsePathString("M17.883,51.449l-0,-25.117c-0,-9.107 7.233,-16.58 16.353,-16.892c9.119,-0.311 16.836,6.662 17.46,15.751c0.042,0.64 0.578,1.141 1.219,1.141l3.686,0c0.337,0 0.657,-0.139 0.891,-0.381c0.234,-0.241 0.354,-0.569 0.337,-0.906c-0.71,-12.451 -11.195,-22.077 -23.671,-21.73c-12.477,0.354 -22.41,10.549 -22.41,23.017l0,25.117c0,0.674 0.546,1.226 1.229,1.226l3.677,0c0.675,0 1.229,-0.544 1.229,-1.226Z").toPath())
            put("p2_cam_b", PathParser().parsePathString("M51.975,48.161c-0,-0.674 0.544,-1.226 1.228,-1.226l3.677,-0c0.675,-0 1.229,0.544 1.229,1.226l0,17.817c0,8.657 -4.863,16.589 -12.589,20.511c-7.726,3.931 -17.01,3.197 -24.018,-1.901c-0.277,-0.198 -0.449,-0.501 -0.493,-0.829c-0.043,-0.329 0.052,-0.674 0.268,-0.933l2.336,-2.851c0.407,-0.502 1.134,-0.597 1.661,-0.225c5.166,3.663 11.94,4.139 17.564,1.235c5.624,-2.903 9.154,-8.692 9.154,-15.016l-0,-17.816l-0.017,0.008Z").toPath())
            put("p2_slash", PathParser().parsePathString("M154.368,14.853c1.09,-1.297 3.02,-1.461 4.318,-0.381c1.297,1.08 1.462,3.015 0.38,4.312l-33.362,39.701c-0.519,0.623 -1.271,1.011 -2.085,1.08c-0.814,0.069 -1.618,-0.181 -2.241,-0.708l-1.41,-1.184c-0.251,-0.207 -0.407,-0.51 -0.433,-0.83c-0.026,-0.319 0.069,-0.647 0.286,-0.889l34.539,-41.11l0.008,0.009Z").toPath())
            put("p2_ring", PathParser().parsePathString("M74.634,89.533c35.857,-5.279 71.801,8.96 94.341,37.376c1.054,1.322 0.829,3.249 -0.491,4.303c-1.32,1.055 -3.245,0.83 -4.298,-0.492c-21.177,-26.707 -54.964,-40.09 -88.654,-35.13c-1.079,0.155 -2.166,-0.268 -2.84,-1.132c-0.673,-0.864 -0.845,-2.013 -0.448,-3.032c0.406,-1.02 1.321,-1.746 2.398,-1.901l-0.008,0.008Z").toPath())
            put("p2_s1", PathParser().parsePathString("M49.732,97.623c0.995,-0.458 2.163,-0.345 3.054,0.293c0.891,0.631 1.375,1.695 1.272,2.783c-0.104,1.089 -0.78,2.039 -1.783,2.497c-13.756,6.264 -25.835,15.699 -35.231,27.527c-0.683,0.855 -1.764,1.288 -2.855,1.124c-1.081,-0.164 -1.998,-0.89 -2.405,-1.901c-0.407,-1.019 -0.234,-2.169 0.45,-3.024c10.001,-12.589 22.85,-22.62 37.489,-29.29l0.009,-0.009Z").toPath())
            put("p2_s2", PathParser().parsePathString("M14.625,188.142c-0,1.694 -1.376,3.059 -3.063,3.059c-1.687,-0 -3.063,-1.375 -3.063,-3.059l0,-41.542c0,-1.097 0.588,-2.108 1.532,-2.652c0.951,-0.544 2.119,-0.544 3.062,0c0.953,0.544 1.532,1.555 1.532,2.652l-0,41.542Z").toPath())
            put("p2_s3", PathParser().parsePathString("M17.044,250.861c21.232,26.707 55.105,40.09 88.883,35.13c1.081,-0.155 2.172,0.269 2.846,1.132c0.684,0.855 0.848,2.013 0.45,3.033c-0.407,1.019 -1.324,1.745 -2.406,1.901c-35.949,5.278 -71.984,-8.96 -94.583,-37.377c-0.684,-0.856 -0.857,-2.013 -0.45,-3.024c0.407,-1.02 1.315,-1.746 2.405,-1.901c1.082,-0.164 2.172,0.267 2.855,1.123l-0,-0.017Z").toPath())
            put("p2_s4", PathParser().parsePathString("M170.123,251.638c0.407,1.02 0.233,2.169 -0.451,3.025c-10.001,12.588 -22.849,22.619 -37.488,29.289c-0.995,0.459 -2.163,0.346 -3.055,-0.293c-0.891,-0.64 -1.375,-1.694 -1.271,-2.783c0.103,-1.088 0.778,-2.038 1.782,-2.497c13.757,-6.264 25.834,-15.699 35.231,-27.527c0.683,-0.855 1.765,-1.288 2.855,-1.124c1.082,0.165 1.999,0.891 2.405,1.901l-0.008,0.009Z").toPath())
            put("p2_s5", PathParser().parsePathString("M169.303,190.397c-1.695,-0 -3.063,1.373 -3.063,3.058l0,31.545c0,1.694 1.376,3.059 3.063,3.059c1.687,-0 3.063,-1.374 3.063,-3.059l-0,-31.545c-0,-1.693 -1.376,-3.058 -3.063,-3.058Z").toPath())
            put("p2_dot", PathParser().parsePathString("M90.191,364.357c-1.691,-0 -3.055,1.373 -3.055,3.058l-0,3.231c-0,1.694 1.372,3.059 3.055,3.059c1.682,-0 3.055,-1.374 3.055,-3.059l-0,-3.231c-0,-1.693 -1.373,-3.058 -3.055,-3.058Z").toPath())
            put("p2_battery", PathParser().parsePathString("M87.136,315.644l-0,39.873c-0,1.693 1.372,3.059 3.055,3.059c1.682,-0 3.055,-1.375 3.055,-3.059l-0,-39.873c-0,-1.097 -0.587,-2.108 -1.527,-2.653c-0.95,-0.544 -2.115,-0.544 -3.055,0c-0.949,0.545 -1.528,1.556 -1.528,2.653Z").toPath())
        }
    }

    Box(
        modifier = modifier
            .padding(horizontal = 4.dp)
            .clip(RoundedCornerShape(40.dp))
            .background(Color(0xFF0F0F0F))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val viewBoxW = 182f
            val viewBoxH = 382f
            val scale = min(size.width / viewBoxW, size.height / viewBoxH)
            val dx = (size.width - viewBoxW * scale) / 2
            val dy = (size.height - viewBoxH * scale) / 2

            withTransform({
                translate(dx, dy)
                scale(scale, scale, pivot = Offset.Zero)
            }) {
                when (device) {
                    DeviceProfile.DEVICE_NP1 -> {
                        // 0: Cam
                        drawPath(paths["p1_cam"]!!, color.copy(alpha = getAlpha(0)))
                        // 1: Slash
                        drawPath(paths["p1_slash"]!!, color.copy(alpha = getAlpha(1)))
                        
                        // Ring zones: 2, 3, 4, 5
                        val ring = paths["p1_ring"]!!
                        val rB = ring.getBounds()
                        
                        // Zone 2: BL
                        clipRect(0f, rB.top + rB.height/2, rB.left + rB.width/2, 382f) { drawPath(ring, color.copy(alpha = getAlpha(2))) }
                        // Zone 3: BR
                        clipRect(rB.left + rB.width/2, rB.top + rB.height/2, 182f, 382f) { drawPath(ring, color.copy(alpha = getAlpha(3))) }
                        // Zone 4: TR
                        clipRect(rB.left + rB.width/2, 0f, 182f, rB.top + rB.height/2) { drawPath(ring, color.copy(alpha = getAlpha(4))) }
                        // Zone 5: TL
                        clipRect(0f, 0f, rB.left + rB.width/2, rB.top + rB.height/2) { drawPath(ring, color.copy(alpha = getAlpha(5))) }

                        // 6: Dot
                        drawPath(paths["p1_dot"]!!, color.copy(alpha = getAlpha(6)))
                        // 7-14: Battery
                        drawPathVerticalSegments(paths["p1_battery"]!!, color, 7..14, smoothedState.value, baseOpacity)
                    }
                    DeviceProfile.DEVICE_NP2 -> {
                        // 0,1: Cam
                        val camT = paths["p2_cam_t"]!!
                        val camB = paths["p2_cam_b"]!!
                        drawPath(camT, color.copy(alpha = getAlpha(0)))
                        drawPath(camB, color.copy(alpha = getAlpha(1)))
                        
                        // 2: Slash
                        drawPath(paths["p2_slash"]!!, color.copy(alpha = getAlpha(2)))
                        
                        // 3-18: Ring (16 zones)
                        drawPathRingSegments(paths["p2_ring"]!!, color, (3..18).toList(), smoothedState.value, baseOpacity)
                        
                        // 19-23: Arcs
                        drawPath(paths["p2_s1"]!!, color.copy(alpha = getAlpha(19)))
                        drawPath(paths["p2_s2"]!!, color.copy(alpha = getAlpha(20)))
                        drawPath(paths["p2_s3"]!!, color.copy(alpha = getAlpha(21)))
                        drawPath(paths["p2_s4"]!!, color.copy(alpha = getAlpha(22)))
                        drawPath(paths["p2_s5"]!!, color.copy(alpha = getAlpha(23)))

                        // 24: Dot
                        drawPath(paths["p2_dot"]!!, color.copy(alpha = getAlpha(24)))
                        // 25-32: Battery
                        drawPathVerticalSegments(paths["p2_battery"]!!, color, 25..32, smoothedState.value, baseOpacity)
                    }
                    else -> {
                        // Standard grid for unknown
                        val cols = 8
                        val itemSize = 15f
                        for (i in smoothedState.value.indices) {
                            val r = i / cols
                            val c = i % cols
                            drawRect(
                                color = color.copy(alpha = getAlpha(i)),
                                topLeft = Offset(c * itemSize + 30f, r * itemSize + 100f),
                                size = Size(itemSize * 0.8f, itemSize * 0.8f)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawPathRingSegments(
    path: Path, 
    color: Color, 
    indices: List<Int>, 
    state: FloatArray, 
    baseOpacity: Float
) {
    val bounds = path.getBounds()
    val count = indices.size
    val centerX = bounds.left + bounds.width/2
    val centerY = bounds.top + bounds.height/2
    
    // We split the ring into horizontal slices for simple visualization of the 16 segments
    val sliceHeight = bounds.height / (count / 2)
    indices.forEachIndexed { i, idx ->
        val alpha = baseOpacity + (state.getOrElse(idx) { 0f } * (1f - baseOpacity))
        val isRight = i >= count / 2
        val row = if (isRight) i - (count/2) else i
        
        clipRect(
            left = if (isRight) centerX else 0f,
            top = bounds.top + row * sliceHeight,
            right = if (isRight) 182f else centerX,
            bottom = bounds.top + (row + 1) * sliceHeight
        ) {
            drawPath(path, color.copy(alpha = alpha))
        }
    }
}

private fun DrawScope.drawPathVerticalSegments(
    path: Path, 
    color: Color, 
    range: IntRange, 
    state: FloatArray, 
    baseOpacity: Float
) {
    val bounds = path.getBounds()
    val count = range.last - range.first + 1
    val segmentHeight = bounds.height / count
    for (i in 0 until count) {
        val idx = range.first + i
        val alpha = baseOpacity + (state.getOrElse(idx) { 0f } * (1f - baseOpacity))
        clipRect(
            left = 0f,
            top = bounds.bottom - (i + 1) * segmentHeight,
            right = 182f,
            bottom = bounds.bottom - i * segmentHeight
        ) {
            drawPath(path, color.copy(alpha = alpha))
        }
    }
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

    Card(
        shape    = RoundedCornerShape(28.dp),
        colors   = CardDefaults.cardColors(containerColor = Color(0xFF242222)),
        modifier = Modifier.size(130.dp, 130.dp),
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(18.dp)) {
            val gridColor = Color(0xFF4C494C)
            val accent    = Color(0xFFE6E0EB)
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
