package com.example.game

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class GameRenderer(
    private val bgBitmap: Bitmap?,
    private val playerRunBitmap: Bitmap?,
    private val playerJumpBitmap: Bitmap?,
    private val playerCrouchBitmap: Bitmap?,
    private val enemiesBitmap: Bitmap?,
    private val char2RunSprite: PoseSprite? = null,
    private val char2JumpSprite: PoseSprite? = null,
    private val char2SlideSprite: PoseSprite? = null
) {
    private val bgImage = bgBitmap?.asImageBitmap()
    private val playerRunImage = playerRunBitmap?.asImageBitmap()
    private val playerJumpImage = playerJumpBitmap?.asImageBitmap()
    private val playerCrouchImage = playerCrouchBitmap?.asImageBitmap()
    private val enemiesImage = enemiesBitmap?.asImageBitmap()

    private val char2RunImage = char2RunSprite?.bitmap?.asImageBitmap()
    private val char2JumpImage = char2JumpSprite?.bitmap?.asImageBitmap()
    private val char2SlideImage = char2SlideSprite?.bitmap?.asImageBitmap()

    fun render(
        drawScope: DrawScope,
        engine: GameEngine,
        animTime: Float
    ) {
        with(drawScope) {
            val canvasW = size.width
            val canvasH = size.height

            // 1. Draw 2D Side-scrolling City Road Background
            drawScrollingBackground(canvasW, canvasH, engine.bgScrollOffset)

            // 2. Draw Obstacles (Barricades, Crates, Cones, Overhead Beams)
            drawObstacles(engine)

            // 3. Draw Collectible 2D Coins
            drawCoins(engine, animTime)

            // 4. Draw Chasing Students Mob (STRICTLY BEHIND THE MAIN CHARACTER)
            drawEnemies(engine, animTime)

            // 5. Draw Main Character (Milon) with exact state poses on the road
            drawPlayer(engine, animTime)

            // 6. Draw Dust & Sparkle Particles
            drawParticles(engine)

            // 7. Danger warning vignette when enemies get close
            drawTensionWarning(canvasW, canvasH, engine, animTime)
        }
    }

    private fun DrawScope.drawScrollingBackground(canvasW: Float, canvasH: Float, scrollOffset: Float) {
        if (bgImage != null) {
            // Keep original 1742x980 aspect ratio scaled to fill canvas height
            val bgScale = canvasH / 980f
            val tileWidth = 1742f * bgScale

            val offset = (scrollOffset % tileWidth)
            var currentX = -offset

            // Tile across screen horizontally for seamless scrolling
            while (currentX < canvasW) {
                drawImage(
                    image = bgImage,
                    dstOffset = IntOffset(currentX.toInt(), 0),
                    dstSize = IntSize(tileWidth.toInt() + 2, canvasH.toInt())
                )
                currentX += tileWidth
            }
        } else {
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(Color(0xFF87CEEB), Color(0xFFE0F7FA), Color(0xFF333A4C)),
                    startY = 0f,
                    endY = canvasH
                ),
                size = size
            )
        }
    }

    private fun DrawScope.drawEnemies(engine: GameEngine, animTime: Float) {
        val bounce = abs(sin(animTime * 14f)) * 5f

        // Visible bottom of enemies_chase.png is at 500/526 of image height
        val visibleRatio = 500f / 526f
        val fullHeight = engine.enemyHeight / visibleRatio
        val fullWidth = fullHeight * (597f / 526f)

        // The nearest enemy is at the front (right edge) of the enemy sprite.
        // Maintain a clearly visible gap strictly behind playerX.
        val nearestEnemyX = engine.playerX - engine.visibleGapBehindPlayer
        val drawX = nearestEnemyX - fullWidth

        // Feet rest directly on engine.groundY (the road)
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
        val isChar2 = engine.selectedCharacter == SelectedCharacter.CHARACTER_2

        // Stumble blink effect
        if (isStumbling && sin(animTime * 28f) > 0.35f) {
            return
        }

        when (engine.playerPose) {
            PlayerPose.RUN -> {
                val visibleRatio = if (isChar2 && char2RunSprite != null) {
                    char2RunSprite.visibleBottomRatio
                } else {
                    203f / 256f
                }
                val aspect = if (isChar2 && char2RunSprite != null) {
                    char2RunSprite.aspectRatio
                } else {
                    124f / 256f
                }
                val fullHeight = engine.playerStandHeight / visibleRatio
                val fullWidth = fullHeight * aspect

                // Gentle footstep bobbing on the road
                val bob = abs(sin(animTime * 16f)) * 4f
                val drawY = engine.groundY - (fullHeight * visibleRatio) + bob

                val img = if (isChar2 && char2RunImage != null) char2RunImage else playerRunImage
                if (img != null) {
                    drawImage(
                        image = img,
                        dstOffset = IntOffset(px.toInt(), drawY.toInt()),
                        dstSize = IntSize(fullWidth.toInt(), fullHeight.toInt())
                    )
                }
            }

            PlayerPose.JUMP -> {
                val visibleRatio = if (isChar2 && char2JumpSprite != null) {
                    char2JumpSprite.visibleBottomRatio
                } else {
                    194f / 256f
                }
                val aspect = if (isChar2 && char2JumpSprite != null) {
                    char2JumpSprite.aspectRatio
                } else {
                    120f / 256f
                }
                val fullHeight = engine.playerStandHeight / visibleRatio
                val fullWidth = fullHeight * aspect

                // Elevated above the road based on jump physics
                val drawY = py - (fullHeight * visibleRatio)

                val img = if (isChar2 && char2JumpImage != null) char2JumpImage else playerJumpImage
                if (img != null) {
                    drawImage(
                        image = img,
                        dstOffset = IntOffset(px.toInt(), drawY.toInt()),
                        dstSize = IntSize(fullWidth.toInt(), fullHeight.toInt())
                    )
                }
            }

            PlayerPose.CROUCH -> {
                val visibleRatio = if (isChar2 && char2SlideSprite != null) {
                    char2SlideSprite.visibleBottomRatio
                } else {
                    176f / 232f
                }
                val aspect = if (isChar2 && char2SlideSprite != null) {
                    char2SlideSprite.aspectRatio
                } else {
                    140f / 232f
                }
                val fullHeight = engine.playerCrouchHeight / visibleRatio
                val fullWidth = fullHeight * aspect

                // Resting flat on the road
                val drawY = engine.groundY - (fullHeight * visibleRatio)

                val img = if (isChar2 && char2SlideImage != null) char2SlideImage else playerCrouchImage
                if (img != null) {
                    drawImage(
                        image = img,
                        dstOffset = IntOffset(px.toInt(), drawY.toInt()),
                        dstSize = IntSize(fullWidth.toInt(), fullHeight.toInt())
                    )
                }

                // Speed lines behind sliding character
                drawLine(
                    color = Color.White.copy(alpha = 0.55f),
                    start = Offset(px - 15f, engine.groundY - 14f),
                    end = Offset(px - 50f, engine.groundY - 14f),
                    strokeWidth = 3f
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.45f),
                    start = Offset(px - 10f, engine.groundY - 28f),
                    end = Offset(px - 40f, engine.groundY - 28f),
                    strokeWidth = 2.5f
                )
            }
        }
    }

    private fun DrawScope.drawObstacles(engine: GameEngine) {
        for (obs in engine.obstacles) {
            val ox = obs.x
            val oy = obs.y
            val ow = obs.width
            val oh = obs.height

            when (obs.type) {
                ObstacleType.BARRICADE -> drawBarricade(ox, oy, ow, oh)
                ObstacleType.CRATE -> drawCrate(ox, oy, ow, oh)
                ObstacleType.CONES -> drawCones(ox, oy, ow, oh)
                ObstacleType.OVERHEAD_BEAM -> drawOverheadBeam(ox, oy, ow, oh)
            }
        }
    }

    private fun DrawScope.drawBarricade(x: Float, y: Float, w: Float, h: Float) {
        // Wooden legs resting on road
        val legW = w * 0.12f
        drawRect(Color(0xFF5D4037), Offset(x + w * 0.12f, y + h * 0.25f), Size(legW, h * 0.75f))
        drawRect(Color(0xFF5D4037), Offset(x + w * 0.76f, y + h * 0.25f), Size(legW, h * 0.75f))

        // Horizontal hazard plank
        val plankH = h * 0.42f
        val plankY = y + h * 0.10f
        drawRoundRect(
            color = Color(0xFFFF9800),
            topLeft = Offset(x, plankY),
            size = Size(w, plankH),
            style = Fill
        )

        // Reflective hazard stripes
        val stripeCount = 4
        val step = w / stripeCount
        for (i in 0 until stripeCount) {
            val sx = x + i * step + 4f
            val path = Path().apply {
                moveTo(sx, plankY)
                lineTo(sx + step * 0.5f, plankY)
                lineTo(sx + step * 0.25f, plankY + plankH)
                lineTo(sx - step * 0.25f, plankY + plankH)
                close()
            }
            drawPath(path, color = Color.White)
        }

        // Outline
        drawRoundRect(
            color = Color(0xFF3E2723),
            topLeft = Offset(x, plankY),
            size = Size(w, plankH),
            style = Stroke(width = 2.5f)
        )
    }

    private fun DrawScope.drawCrate(x: Float, y: Float, w: Float, h: Float) {
        // Wooden shipping box on the road
        drawRoundRect(
            color = Color(0xFF8D6E63),
            topLeft = Offset(x, y),
            size = Size(w, h),
            style = Fill
        )

        // Planks
        drawLine(
            color = Color(0xFF4E342E),
            start = Offset(x, y + h * 0.33f),
            end = Offset(x + w, y + h * 0.33f),
            strokeWidth = 2.5f
        )
        drawLine(
            color = Color(0xFF4E342E),
            start = Offset(x, y + h * 0.66f),
            end = Offset(x + w, y + h * 0.66f),
            strokeWidth = 2.5f
        )

        // Cross braces
        drawLine(
            color = Color(0xFF4E342E),
            start = Offset(x + 4f, y + 4f),
            end = Offset(x + w - 4f, y + h - 4f),
            strokeWidth = 3f
        )
        drawLine(
            color = Color(0xFF4E342E),
            start = Offset(x + w - 4f, y + 4f),
            end = Offset(x + 4f, y + h - 4f),
            strokeWidth = 3f
        )

        drawRoundRect(
            color = Color(0xFF3E2723),
            topLeft = Offset(x, y),
            size = Size(w, h),
            style = Stroke(width = 3f)
        )
    }

    private fun DrawScope.drawCones(x: Float, y: Float, w: Float, h: Float) {
        val coneW = w * 0.48f
        for (i in 0..1) {
            val cx = x + i * (coneW + w * 0.04f)
            // Base plate on the road
            drawRoundRect(
                color = Color(0xFF212121),
                topLeft = Offset(cx, y + h - 8f),
                size = Size(coneW, 8f)
            )

            // Orange cone
            val path = Path().apply {
                moveTo(cx + coneW * 0.5f, y)
                lineTo(cx + coneW - 2f, y + h - 8f)
                lineTo(cx + 2f, y + h - 8f)
                close()
            }
            drawPath(path, color = Color(0xFFFF5722))

            // White reflective collar
            val stripePath = Path().apply {
                moveTo(cx + coneW * 0.34f, y + h * 0.44f)
                lineTo(cx + coneW * 0.66f, y + h * 0.44f)
                lineTo(cx + coneW * 0.74f, y + h * 0.66f)
                lineTo(cx + coneW * 0.26f, y + h * 0.66f)
                close()
            }
            drawPath(stripePath, color = Color.White)
        }
    }

    private fun DrawScope.drawOverheadBeam(x: Float, y: Float, w: Float, h: Float) {
        // Vertical hanging bars from top of screen
        drawRect(Color(0xFF37474F), Offset(x + w * 0.12f, 0f), Size(6f, y + 8f))
        drawRect(Color(0xFF37474F), Offset(x + w * 0.82f, 0f), Size(6f, y + 8f))

        // Caution girder
        drawRoundRect(
            color = Color(0xFFFFD600),
            topLeft = Offset(x, y),
            size = Size(w, h),
            style = Fill
        )

        // Diagonal caution stripes
        val count = 5
        val step = w / count
        for (i in 0 until count) {
            val sx = x + i * step + 4f
            val path = Path().apply {
                moveTo(sx, y)
                lineTo(sx + step * 0.5f, y)
                lineTo(sx + step * 0.2f, y + h)
                lineTo(sx - step * 0.3f, y + h)
                close()
            }
            drawPath(path, color = Color(0xFF212121))
        }

        drawRoundRect(
            color = Color(0xFF212121),
            topLeft = Offset(x, y),
            size = Size(w, h),
            style = Stroke(width = 3f)
        )

        // Hanging red "SLIDE!" banner
        drawRoundRect(
            color = Color(0xFFD32F2F),
            topLeft = Offset(x + w * 0.18f, y + h - 2f),
            size = Size(w * 0.64f, 15f)
        )
    }

    private fun DrawScope.drawCoins(engine: GameEngine, animTime: Float) {
        for (coin in engine.coins) {
            val cx = coin.x
            val cy = coin.y
            val r = coin.radius

            val spinScale = abs(cos(animTime * 6f + coin.id * 0.5f)).coerceAtLeast(0.18f)
            val coinW = r * 2f * spinScale
            val coinH = r * 2f

            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFFEE58), Color(0xFFFFB300), Color(0xFFFF8F00)),
                    center = Offset(cx, cy),
                    radius = r
                ),
                topLeft = Offset(cx - coinW / 2f, cy - coinH / 2f),
                size = Size(coinW, coinH)
            )

            drawOval(
                color = Color(0xFFFFF9C4),
                topLeft = Offset(cx - (coinW * 0.7f) / 2f, cy - (coinH * 0.7f) / 2f),
                size = Size(coinW * 0.7f, coinH * 0.7f),
                style = Stroke(width = 2f)
            )

            val sparkleX = cx - coinW * 0.25f
            val sparkleY = cy - coinH * 0.25f
            drawCircle(Color.White.copy(alpha = 0.8f), radius = 2.5f, center = Offset(sparkleX, sparkleY))
        }
    }

    private fun DrawScope.drawParticles(engine: GameEngine) {
        for (p in engine.particles) {
            val alpha = (1f - (p.currentLife / p.maxLife)).coerceIn(0f, 1f)
            val color = Color(p.color).copy(alpha = alpha)
            drawCircle(
                color = color,
                radius = p.radius * (1f - p.currentLife / (p.maxLife * 1.5f)),
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
        val dist = engine.visibleGapBehindPlayer
        if (dist < 100f) {
            val intensity = ((100f - dist) / 80f).coerceIn(0f, 1f)
            val pulse = (sin(animTime * 10f) * 0.5f + 0.5f) * intensity
            val alpha = (pulse * 0.42f).coerceIn(0f, 0.42f)

            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.Red.copy(alpha = alpha), Color.Transparent),
                    startX = 0f,
                    endX = canvasW * 0.35f
                ),
                size = size
            )
        }
    }
}
