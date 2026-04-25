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
import androidx.compose.ui.res.stringResource

@Composable
fun AboutScreen() {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        Spacer(modifier = Modifier.height(20.dp))
        ScreenTitle(text = stringResource(R.string.about_title))
        
        BodyText(
            text = stringResource(R.string.about_intro)
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            BodyText(text = stringResource(R.string.about_section_why), size = 20.sp)
            BodyText(text = stringResource(R.string.about_why_1))
            BodyText(text = stringResource(R.string.about_why_2))
            BodyText(text = stringResource(R.string.about_why_3))
            BodyText(text = stringResource(R.string.about_why_4))
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            BodyText(text = stringResource(R.string.about_section_team), size = 20.sp)
            BodyText(text = stringResource(R.string.about_team_1))
            BodyText(text = stringResource(R.string.about_team_2))
            BodyText(text = stringResource(R.string.about_team_3))
            BodyText(text = stringResource(R.string.about_team_4))
            BodyText(text = stringResource(R.string.about_team_5))
            BodyText(text = stringResource(R.string.about_team_6))
            BodyText(text = stringResource(R.string.about_team_7))
        }
        Spacer(modifier = Modifier.height(28.dp))
    }
}
