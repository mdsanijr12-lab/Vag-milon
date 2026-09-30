package com.example.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import com.example.R
import com.example.audio.GameAudio
import com.example.game.GameEngine
import com.example.game.GameRenderer
import com.example.game.GameState

@Composable
fun GameScreen(
    audio: GameAudio,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    val engine = remember { GameEngine(context, audio) }

    // Load Bitmaps once
    val renderer = remember {
        val bgBmp = BitmapFactory.decodeResource(context.resources, R.drawable.bg_city)
        val runBmp = BitmapFactory.decodeResource(context.resources, R.drawable.player_run)
        val jumpBmp = BitmapFactory.decodeResource(context.resources, R.drawable.player_jump)
        val crouchBmp = BitmapFactory.decodeResource(context.resources, R.drawable.player_crouch)
        val enemiesBmp = BitmapFactory.decodeResource(context.resources, R.drawable.enemies_chase)
        GameRenderer(bgBmp, runBmp, jumpBmp, crouchBmp, enemiesBmp)
    }

    var animTime by remember { mutableFloatStateOf(0f) }

    // Clean up audio resources on dispose
    DisposableEffect(Unit) {
        onDispose {
            audio.stopBgm()
            audio.stopSplashSound()
        }
    }

    // High performance frame loop
    LaunchedEffect(engine.gameState) {
        var lastTime = 0L
        while (true) {
            withFrameNanos { now ->
                if (lastTime != 0L) {
                    val dt = (now - lastTime) / 1_000_000_000f
                    animTime += dt
                    engine.update(dt)
                }
                lastTime = now
            }
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()

        LaunchedEffect(widthPx, heightPx) {
            engine.updateScreenSize(widthPx, heightPx)
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                // Direct gesture support: tap to jump, swipe down to crouch/slide
                .pointerInput(engine.gameState) {
                    if (engine.gameState == GameState.PLAYING) {
                        detectTapGestures(
                            onTap = { offset ->
                                if (offset.x > widthPx * 0.5f || offset.y < heightPx * 0.4f) {
                                    engine.jump()
                                } else {
                                    engine.crouch()
                                }
                            }
                        )
                    }
                }
                .pointerInput(engine.gameState) {
                    if (engine.gameState == GameState.PLAYING) {
                        detectDragGestures(
                            onDrag = { change, dragAmount ->
                                change.consume()
                                if (dragAmount.y < -15f) {
                                    engine.jump()
                                } else if (dragAmount.y > 15f) {
                                    engine.crouch()
                                }
                            }
                        )
                    }
                }
        ) {
            // Main 60fps Game Canvas (rendered during gameplay, pause, title, and game over)
            if (engine.gameState != GameState.SPLASH) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    renderer.render(this, engine, animTime)
                }
            }

            // UI Layer based on Game State
            when (engine.gameState) {
                GameState.SPLASH -> {
                    SplashScreen(
                        engine = engine,
                        audio = audio
                    )
                }
                GameState.TITLE -> {
                    TitleScreen(
                        engine = engine,
                        onStartGame = { engine.startGame() }
                    )
                }
                GameState.PLAYING, GameState.PAUSED -> {
                    HUDControls(
                        engine = engine,
                        onPauseToggle = {
                            if (engine.gameState == GameState.PLAYING) engine.pauseGame()
                            else engine.resumeGame()
                        },
                        onRestart = { engine.startGame() },
                        onMainMenu = { engine.returnToTitle() }
                    )
                }
                GameState.GAME_OVER -> {
                    GameOverScreen(
                        engine = engine,
                        onRestart = { engine.startGame() },
                        onMainMenu = { engine.returnToTitle() }
                    )
                }
            }
        }
    }
}
