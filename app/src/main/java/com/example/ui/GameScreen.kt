package com.example.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val engine = remember { GameEngine(context, audio) }

    // Load and validate all required game assets once
    val (renderer, missingAssets) = remember {
        val missing = mutableListOf<String>()
        val bgMainBmp = BitmapFactory.decodeResource(context.resources, R.drawable.bg_main)
            ?: run { missing.add("MAIN BACKGROUND (bg_main)"); null }
        val bgParallaxBmp = BitmapFactory.decodeResource(context.resources, R.drawable.bg_parallax)
            ?: run { missing.add("PARALLAX BACKGROUND (bg_parallax)"); null }
        val runBmp = BitmapFactory.decodeResource(context.resources, R.drawable.player_run)
            ?: run { missing.add("CHARACTER RUN (player_run)"); null }
        val jumpBmp = BitmapFactory.decodeResource(context.resources, R.drawable.player_jump)
            ?: run { missing.add("CHARACTER JUMP (player_jump)"); null }
        val crouchBmp = BitmapFactory.decodeResource(context.resources, R.drawable.player_crouch)
            ?: run { missing.add("CHARACTER SLIDE (player_crouch)"); null }
        val obstacleBmp = BitmapFactory.decodeResource(context.resources, R.drawable.obstacle_sprite)
            ?: run { missing.add("OBSTACLE (obstacle_sprite)"); null }
        val powerUpBmp = BitmapFactory.decodeResource(context.resources, R.drawable.powerup_sprite)
            ?: run { missing.add("POWER-UP (powerup_sprite)"); null }
        val enemiesBmp = BitmapFactory.decodeResource(context.resources, R.drawable.enemies_chase)
            ?: run { missing.add("ENEMIES (enemies_chase)"); null }

        val gameRenderer = GameRenderer(
            bgMainBitmap = bgMainBmp,
            bgParallaxBitmap = bgParallaxBmp,
            playerRunBitmap = runBmp,
            playerJumpBitmap = jumpBmp,
            playerCrouchBitmap = crouchBmp,
            obstacleBitmap = obstacleBmp,
            powerUpBitmap = powerUpBmp,
            enemiesBitmap = enemiesBmp
        )
        Pair(gameRenderer, missing.toList())
    }

    var animTime by remember { mutableFloatStateOf(0f) }

    // Clean up audio resources on dispose
    DisposableEffect(Unit) {
        onDispose {
            audio.stopBgm()
            audio.stopSplashSound()
        }
    }

    // High performance 60fps frame loop
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
                // Direct gesture support: tap to jump, swipe down to slide
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
            // Main 60fps Game Canvas
            if (engine.gameState != GameState.SPLASH) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    renderer.render(this, engine, animTime)
                }
            }

            // Clear asset-loading error display if any asset ever failed to decode
            if (missingAssets.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFD50000),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Asset Loading Error: ${missingAssets.joinToString(", ")}",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
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
