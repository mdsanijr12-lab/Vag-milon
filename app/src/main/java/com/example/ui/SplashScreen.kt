package com.example.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import com.example.R
import com.example.audio.GameAudio
import com.example.game.GameEngine
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    engine: GameEngine,
    audio: GameAudio,
    modifier: Modifier = Modifier
) {
    // Play splash sound once upon app launch
    DisposableEffect(Unit) {
        audio.playSplashSound()
        onDispose {
            audio.stopSplashSound()
        }
    }

    // Advance to game title screen after 2.5 seconds
    LaunchedEffect(Unit) {
        delay(2500L)
        engine.onSplashFinished()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                engine.onSplashFinished()
            }
            .testTag("splash_screen_container"),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.splash_bg),
            contentDescription = "Startup Splash Screen",
            modifier = Modifier
                .fillMaxSize()
                .testTag("splash_image"),
            contentScale = ContentScale.Fit
        )
    }
}
