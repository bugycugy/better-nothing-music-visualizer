package com.better.nothing.music.vizualizer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AboutScreen() {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        ScreenTitle(text = "About")
        
        BodyText(
            text = "Better Nothing Music Visualizer unlocks the full potential of the Glyph Interface with high-fidelity, real-time frequency analysis."
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            BodyText(text = "Better than Stock:", size = 20.sp)
            BodyText(text = "• 12-bit brightness (4096 levels) vs ~2-bit stock")
            BodyText(text = "• 60 FPS fluid animations vs ~25 FPS stock")
            BodyText(text = "• Precise FFT sync vs semi-random response")
            BodyText(text = "• Independent sub-zone control")
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            BodyText(text = "The Team:", size = 20.sp)
            BodyText(text = "• Aleks-Levet: Founder & Coordinator")
            BodyText(text = "• oliver lebaigue: Android Developer")
            BodyText(text = "• rKyzen (Shivank Dan): Android Developer")
            BodyText(text = "• Nicouschulas: Documentation & Wiki")
            BodyText(text = "• SebiAi: Glyph Modder & Technical Help")
            BodyText(text = "• Earnedel-lab: UI/UX & Design")
            BodyText(text = "• あけ なるかみ (Luke20YT): Music App Integration")
            BodyText(text = "• Interlastic: Original Discord Bot")
        }

        Spacer(modifier = Modifier.height(28.dp))
    }
}
