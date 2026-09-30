package com.example.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.GameEngine
import com.example.game.GameState

@Composable
fun HUDControls(
    engine: GameEngine,
    onPauseToggle: () -> Unit,
    onRestart: () -> Unit,
    onMainMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isPaused = engine.gameState == GameState.PAUSED

    Box(modifier = modifier.fillMaxSize()) {
        // TOP HUD BAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Distance counter
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.Black.copy(alpha = 0.5f),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF64B5F6))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "🏃 ", fontSize = 16.sp)
                    Text(
                        text = "${engine.distanceTraveled.toInt()} m",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            // Chasing Students proximity indicator
            ThreatIndicator(distanceBehind = engine.visibleGapBehindPlayer)

            // Right HUD: Coin counter + Audio toggle + Pause button
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Coins
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Black.copy(alpha = 0.5f),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFFFD54F))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "🪙 ", fontSize = 15.sp)
                        Text(
                            text = "${engine.coinCount}",
                            color = Color(0xFFFFD54F),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                // Audio Mute/Unmute
                IconButton(
                    onClick = { engine.audio.toggleMute() },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .border(1.dp, Color.White.copy(alpha = 0.6f), CircleShape)
                        .testTag("hud_sound_button")
                ) {
                    Icon(
                        imageVector = if (engine.audio.isAudioMuted()) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                        contentDescription = "Mute/Unmute",
                        tint = if (engine.audio.isAudioMuted()) Color(0xFFFF5252) else Color(0xFF69F0AE),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Pause button
                IconButton(
                    onClick = onPauseToggle,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .border(1.dp, Color.White.copy(alpha = 0.6f), CircleShape)
                        .testTag("hud_pause_button")
                ) {
                    Icon(
                        imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = "Pause",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // CLEAN, TRANSPARENT TOUCH CONTROLS (Only Jump and Slide)
        if (!isPaused) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                // LEFT THUMB: SLIDE / CROUCH (Transparent glassmorphic button)
                TransparentActionButton(
                    icon = Icons.Default.ArrowDownward,
                    label = "SLIDE",
                    accentColor = Color(0xFFFF9800),
                    testTag = "btn_crouch",
                    onPress = { engine.crouch() },
                    onRelease = { engine.onCrouchReleased() }
                )

                // RIGHT THUMB: JUMP (Transparent glassmorphic button)
                TransparentActionButton(
                    icon = Icons.Default.ArrowUpward,
                    label = "JUMP",
                    accentColor = Color(0xFF00E676),
                    testTag = "btn_jump",
                    onPress = { engine.jump() },
                    onRelease = {}
                )
            }
        }

        // PAUSE MODAL OVERLAY
        if (isPaused) {
            PauseOverlay(
                onResume = onPauseToggle,
                onRestart = onRestart,
                onMainMenu = onMainMenu
            )
        }
    }
}

@Composable
private fun ThreatIndicator(distanceBehind: Float) {
    val isCritical = distanceBehind < 90f
    val isWarning = distanceBehind < 180f

    val infiniteTransition = rememberInfiniteTransition(label = "threatPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isCritical) 1.12f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(350),
            repeatMode = RepeatMode.Reverse
        ),
        label = "threatPulseAnim"
    )

    val badgeColor = when {
        isCritical -> Color(0xFFFF1744)
        isWarning -> Color(0xFFFFAB00)
        else -> Color(0xFF00E676)
    }

    val text = when {
        isCritical -> "⚠️ DANGER! STUDENTS TOO CLOSE! ⚠️"
        isWarning -> "STUDENTS CHASING: ${(distanceBehind / 10f).toInt()}m BEHIND"
        else -> "STUDENTS BEHIND: ${(distanceBehind / 10f).toInt()}m"
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.Black.copy(alpha = 0.6f),
        border = androidx.compose.foundation.BorderStroke(2.dp, badgeColor),
        modifier = Modifier.scale(if (isCritical) pulseScale else 1f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(badgeColor)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun TransparentActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    accentColor: Color,
    testTag: String,
    onPress: () -> Unit,
    onRelease: () -> Unit
) {
    // Large, transparent, clean button that does not obstruct the gameplay view
    Box(
        modifier = Modifier
            .size(width = 96.dp, height = 80.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(Color.Black.copy(alpha = 0.28f))
            .border(2.dp, Color.White.copy(alpha = 0.70f), RoundedCornerShape(22.dp))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        onPress()
                        tryAwaitRelease()
                        onRelease()
                    }
                )
            }
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = accentColor,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
private fun PauseOverlay(
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onMainMenu: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF263238),
            border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF64B5F6)),
            modifier = Modifier.width(360.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "GAME PAUSED",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onResume,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                    modifier = Modifier.fillMaxWidth().testTag("resume_button")
                ) {
                    Text("RESUME", color = Color.Black, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = onRestart,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300)),
                    modifier = Modifier.fillMaxWidth().testTag("pause_restart_button")
                ) {
                    Text("RESTART", color = Color.Black, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onMainMenu,
                    modifier = Modifier.fillMaxWidth().testTag("pause_main_menu_button")
                ) {
                    Text("TITLE MENU", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
