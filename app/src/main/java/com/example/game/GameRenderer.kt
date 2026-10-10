package com.example.game

import android.graphics.Bitmap
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sin

class GameRenderer(
    bgMainBitmap: Bitmap?,
    bgParallaxBitmap: Bitmap?,
    playerRunBitmap: Bitmap?,
    playerJumpBitmap: Bitmap?,
    playerCrouchBitmap: Bitmap?,
    obstacleBitmap: Bitmap?,
    powerUpBitmap: Bitmap?,
    enemiesBitmap: Bitmap?
) {
    private val bgMainImage = bgMainBitmap?.asImageBitmap()
    private val bgParallaxImage = bgParallaxBitmap?.asImageBitmap()
    private val playerRunImage = playerRunBitmap?.asImageBitmap()
    private val playerJumpImage = playerJumpBitmap?.asImageBitmap()
    private val playerCrouchImage = playerCrouchBitmap?.asImageBitmap()
    private val obstacleImage = obstacleBitmap?.asImageBitmap()
    private val powerUpImage = powerUpBitmap?.asImageBitmap()
    private val enemiesImage = enemiesBitmap?.asImageBitmap()

    fun render(
        drawScope: DrawScope,
        engine: GameEngine,
        animTime: Float
    ) {
        with(drawScope) {
            val canvasW = size.width
            val canvasH = size.height

            // 1. Draw Parallax Background + Main Environment Background
            drawScrollingBackground(
                canvasW = canvasW,
                canvasH = canvasH,
                mainScrollOffset = engine.bgScrollOffset,
                parallaxScrollOffset = engine.parallaxScrollOffset,
                groundY = engine.groundY
            )

            // 2. Draw Obstacles on the road
            drawObstacles(engine)

            // 3. Draw Collectible Coins along the running path
            drawCoins(engine, animTime)

            // 4. Draw Collectible Power-Ups along the running path
            drawPowerUps(engine, animTime)

            // 5. Draw Chasing Enemies strictly behind the main character
            drawEnemies(engine, animTime)

            // 6. Draw New Playable Character (RUN / JUMP / SLIDE)
            drawPlayer(engine, animTime)

            // 7. Draw Particles
            drawParticles(engine)

            // 8. Danger warning vignette when enemies get close
            drawTensionWarning(canvasW, canvasH, engine, animTime)
        }
    }

    private fun DrawScope.drawScrollingBackground(
        canvasW: Float,
        canvasH: Float,
        mainScrollOffset: Float,
        parallaxScrollOffset: Float,
        groundY: Float
    ) {
        // Sky & horizon backdrop so there are never black gaps on any aspect ratio
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF90CAF9),
                    Color(0xFFE3F2FD),
                    Color(0xFF37474F)
                ),
                startY = 0f,
                endY = canvasH
            ),
            size = size
        )

        // Distant Parallax Layer (bg_parallax.png: 1324x236)
        if (bgParallaxImage != null && bgParallaxImage.height > 0) {
            val parallaxTop = 0f
            val parallaxHeight = canvasH * 0.54f
            val aspect = bgParallaxImage.width.toFloat() / bgParallaxImage.height.toFloat()
            val tileWidth = (parallaxHeight * aspect).coerceAtLeast(64f)

            val offset = ((parallaxScrollOffset % tileWidth) + tileWidth) % tileWidth
            var currentX = -offset
            while (currentX < canvasW) {
                drawImage(
                    image = bgParallaxImage,
                    dstOffset = IntOffset(currentX.toInt(), parallaxTop.toInt()),
                    dstSize = IntSize(tileWidth.toInt() + 3, parallaxHeight.toInt())
                )
                currentX += tileWidth
            }
        }

        // Main Foreground & Road Layer (bg_main.png: 768x204)
        if (bgMainImage != null && bgMainImage.height > 0) {
            val mainHeight = canvasH * 0.56f
            val mainTop = canvasH - mainHeight
            val aspect = bgMainImage.width.toFloat() / bgMainImage.height.toFloat()
            val tileWidth = (mainHeight * aspect).coerceAtLeast(64f)

            val offset = ((mainScrollOffset % tileWidth) + tileWidth) % tileWidth
            var currentX = -offset
            while (currentX < canvasW) {
                drawImage(
                    image = bgMainImage,
                    dstOffset = IntOffset(currentX.toInt(), mainTop.toInt()),
                    dstSize = IntSize(tileWidth.toInt() + 3, mainHeight.toInt())
                )
                currentX += tileWidth
            }
        }

        // Subtle road surface shadow line at groundY so feet and obstacles look firmly grounded
        drawLine(
            color = Color.Black.copy(alpha = 0.25f),
            start = Offset(0f, groundY + 2f),
            end = Offset(canvasW, groundY + 2f),
            strokeWidth = 4f
        )
    }

    private fun DrawScope.drawEnemies(engine: GameEngine, animTime: Float) {
        val bounce = abs(sin(animTime * 14f)) * 5f

        // Visible bottom of enemies_chase.png is at 500/526 of image height
        val visibleRatio = 500f / 526f
        val fullHeight = engine.enemyHeight / visibleRatio
        val fullWidth = fullHeight * (597f / 526f)

        // The nearest enemy is strictly behind playerX by visibleGapBehindPlayer
        val nearestEnemyX = engine.playerX - engine.visibleGapBehindPlayer
        val drawX = nearestEnemyX - fullWidth
        val drawY = engine.groundY - (fullHeight * visibleRatio) + bounce

        if (enemiesImage != null) {
            drawImage(
                image = enemiesImage,
                dstOffset = IntOffset(drawX.toInt(), drawY.toInt()),
                dstSize = IntSize(fullWidth.toInt(), fullHeight.toInt())
            )
        }
    }

    private fun DrawScope.drawPlayer(engine: GameEngine, animTime: Float) {
        val px = engine.playerX
        val py = engine.playerY
        val isStumbling = engine.stumbleTimer > 0f

        // Stumble blink effect
        if (isStumbling && sin(animTime * 28f) > 0.35f) {
            return
        }

        // All 3 character sprites share an 887px canvas height:
        // player_run: 557x887 (feet at 787/887)
        // player_jump: 561x887 (feet at 785/887 relative to jump elevation)
        // player_crouch: 726x887 (bottom at 784/887)
        val canvasScale = engine.playerCanvasHeight / 887f

        // Active Speed Boost Aura when power-up is active
        if (engine.isPowerUpActive) {
            val auraCenterX = px + engine.playerStandWidth * 0.5f
            val auraCenterY = py - engine.playerStandHeight * 0.45f
            val pulse = 1.0f + sin(animTime * 18f) * 0.08f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x8800E5FF),
                        Color(0x44FFD700),
                        Color.Transparent
                    ),
                    center = Offset(auraCenterX, auraCenterY),
                    radius = engine.playerStandHeight * 0.65f * pulse
                ),
                radius = engine.playerStandHeight * 0.65f * pulse,
                center = Offset(auraCenterX, auraCenterY)
            )
        }

        when (engine.playerPose) {
            PlayerPose.RUN -> {
                val drawW = 557f * canvasScale
                val drawH = 887f * canvasScale
                val feetOffset = 787f * canvasScale
                val bob = abs(sin(animTime * 16f)) * 3.5f
                val drawY = engine.groundY - feetOffset + bob

                if (playerRunImage != null) {
                    drawImage(
                        image = playerRunImage,
                        dstOffset = IntOffset(px.toInt(), drawY.toInt()),
                        dstSize = IntSize(drawW.toInt(), drawH.toInt())
                    )
                }
            }

            PlayerPose.JUMP -> {
                val drawW = 561f * canvasScale
                val drawH = 887f * canvasScale
                val feetOffset = 785f * canvasScale
                val drawY = py - feetOffset

                if (playerJumpImage != null) {
                    drawImage(
                        image = playerJumpImage,
                        dstOffset = IntOffset(px.toInt(), drawY.toInt()),
                        dstSize = IntSize(drawW.toInt(), drawH.toInt())
                    )
                }
            }

            PlayerPose.CROUCH -> {
                val drawW = 726f * canvasScale
                val drawH = 887f * canvasScale
                val bottomOffset = 784f * canvasScale
                val drawY = engine.groundY - bottomOffset

                if (playerCrouchImage != null) {
                    drawImage(
                        image = playerCrouchImage,
                        dstOffset = IntOffset(px.toInt(), drawY.toInt()),
                        dstSize = IntSize(drawW.toInt(), drawH.toInt())
                    )
                }

                // Speed lines behind sliding character
                drawLine(
                    color = Color.White.copy(alpha = 0.6f),
                    start = Offset(px - 12f, engine.groundY - 14f),
                    end = Offset(px - 52f, engine.groundY - 14f),
                    strokeWidth = 3f
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.45f),
                    start = Offset(px - 8f, engine.groundY - 28f),
                    end = Offset(px - 42f, engine.groundY - 28f),
                    strokeWidth = 2.5f
                )
            }
        }
    }

    private fun DrawScope.drawObstacles(engine: GameEngine) {
        val img = obstacleImage
        for (obs in engine.obstacles) {
            val ox = obs.x
            val oy = obs.y
            val ow = obs.width
            val oh = obs.height

            if (obs.type == ObstacleType.OVERHEAD_OBSTACLE) {
                // Support posts down to the road so player sees it's an overhead slide-under barrier
                val postColor = Color(0xFF37474F)
                drawRect(
                    color = postColor,
                    topLeft = Offset(ox + 4f, oy + oh),
                    size = Size(8f, (engine.groundY - (oy + oh)).coerceAtLeast(0f))
                )
                drawRect(
                    color = postColor,
                    topLeft = Offset(ox + ow - 12f, oy + oh),
                    size = Size(8f, (engine.groundY - (oy + oh)).coerceAtLeast(0f))
                )
            }

            if (img != null && img.width > 0 && img.height > 0) {
                // obstacle_sprite.png (1536x204): sample a proportional frame without stretching
                val frameCount = 4
                val frameW = img.width / frameCount
                val frameIdx = obs.variantIndex.coerceIn(0, frameCount - 1)
                val srcX = frameIdx * frameW

                drawImage(
                    image = img,
                    srcOffset = IntOffset(srcX, 0),
                    srcSize = IntSize(frameW, img.height),
                    dstOffset = IntOffset(ox.toInt(), oy.toInt()),
                    dstSize = IntSize(ow.toInt(), oh.toInt())
                )

                // Crisp hazard highlight border so obstacles stand out clearly on the road
                val borderColor = if (obs.type == ObstacleType.OVERHEAD_OBSTACLE) {
                    Color(0xFFFFD600)
                } else {
                    Color(0xFFFF6D00)
                }
                drawRoundRect(
                    color = borderColor.copy(alpha = 0.85f),
                    topLeft = Offset(ox, oy),
                    size = Size(ow, oh),
                    cornerRadius = CornerRadius(6f, 6f),
                    style = Stroke(width = 2.5f)
                )
            }
        }
    }

    private fun DrawScope.drawPowerUps(engine: GameEngine, animTime: Float) {
        val img = powerUpImage ?: return
        if (img.width <= 0 || img.height <= 0) return

        val frameCount = 4
        val frameW = img.width / frameCount

        for (item in engine.powerUps) {
            if (item.isCollected) continue

            val bob = sin(animTime * 7f + item.id) * 5f
            val drawX = item.x
            val drawY = item.y + bob
            val centerX = drawX + item.width * 0.5f
            val centerY = drawY + item.height * 0.5f

            // Glowing aura around power-up
            drawCircle(
                color = Color(0x5500E5FF),
                radius = maxOf(item.width, item.height) * 0.65f,
                center = Offset(centerX, centerY)
            )

            val frameIdx = item.variantIndex.coerceIn(0, frameCount - 1)
            val srcX = frameIdx * frameW

            drawImage(
                image = img,
                srcOffset = IntOffset(srcX, 0),
                srcSize = IntSize(frameW, img.height),
                dstOffset = IntOffset(drawX.toInt(), drawY.toInt()),
                dstSize = IntSize(item.width.toInt(), item.height.toInt())
            )

            drawRoundRect(
                color = Color(0xFF00E5FF),
                topLeft = Offset(drawX, drawY),
                size = Size(item.width, item.height),
                cornerRadius = CornerRadius(10f, 10f),
                style = Stroke(width = 2.5f)
            )
        }
    }

    private fun DrawScope.drawCoins(engine: GameEngine, animTime: Float) {
        for (coin in engine.coins) {
            if (coin.isCollected) continue

            val pulse = 1.0f + sin(animTime * 8f + coin.id) * 0.08f
            val r = coin.radius * pulse
            val cx = coin.x
            val cy = coin.y

            // Outer glow
            drawCircle(
                color = Color(0x55FFD700),
                radius = r * 1.35f,
                center = Offset(cx, cy)
            )

            // Main Gold Coin Body
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFFF59D), Color(0xFFFFC107), Color(0xFFFF8F00)),
                    center = Offset(cx - r * 0.25f, cy - r * 0.25f),
                    radius = r * 1.2f
                ),
                radius = r,
                center = Offset(cx, cy)
            )

            // Gold Rim
            drawCircle(
                color = Color(0xFFFFE082),
                radius = r * 0.78f,
                center = Offset(cx, cy),
                style = Stroke(width = 2.5f)
            )

            // Inner Shine
            drawRect(
                color = Color(0xFFFFFDE7),
                topLeft = Offset(cx - r * 0.14f, cy - r * 0.42f),
                size = Size(r * 0.28f, r * 0.84f)
            )
        }
    }

    private fun DrawScope.drawParticles(engine: GameEngine) {
        for (p in engine.particles) {
            val alpha = (1.0f - (p.currentLife / p.maxLife)).coerceIn(0f, 1f)
            drawCircle(
                color = Color(p.color).copy(alpha = alpha),
                radius = p.radius,
                center = Offset(p.x, p.y)
            )
        }
    }

    private fun DrawScope.drawTensionWarning(
        canvasW: Float,
        canvasH: Float,
        engine: GameEngine,
        animTime: Float
    ) {
        if (engine.visibleGapBehindPlayer < 120f) {
            val intensity = ((120f - engine.visibleGapBehindPlayer) / 80f).coerceIn(0f, 1f)
            val pulse = (sin(animTime * 15f) * 0.5f + 0.5f) * intensity * 0.35f

            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.Red.copy(alpha = pulse), Color.Transparent),
                    startX = 0f,
                    endX = min(300f, canvasW * 0.28f)
                ),
                size = Size(min(300f, canvasW * 0.28f), canvasH)
            )
        }
    }
}
