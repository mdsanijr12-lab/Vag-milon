package com.example.game

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.audio.GameAudio
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class GameEngine(
    private val context: Context,
    val audio: GameAudio
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("vag_milon_vag_prefs", Context.MODE_PRIVATE)

    // Game state
    var gameState by mutableStateOf(GameState.SPLASH)
        private set
    var gameOverReason by mutableStateOf(GameOverReason.CAUGHT_BY_STUDENTS)
        private set

    fun onSplashFinished() {
        if (gameState == GameState.SPLASH) {
            gameState = GameState.TITLE
            audio.stopSplashSound()
            audio.startBgm()
        }
    }

    // Dimensions
    var screenWidth by mutableFloatStateOf(1280f)
    var screenHeight by mutableFloatStateOf(720f)
    var groundY by mutableFloatStateOf(640f)

    // Score & stats
    var distanceTraveled by mutableFloatStateOf(0f)
        private set
    var coinCount by mutableIntStateOf(0)
        private set
    var bestDistance by mutableFloatStateOf(0f)
        private set
    var bestCoins by mutableIntStateOf(0)
        private set
    var isNewRecord by mutableStateOf(false)
        private set

    // Speeds & Physics
    var baseGameSpeed by mutableFloatStateOf(450f) // px/s
    var currentGameSpeed by mutableFloatStateOf(450f)
    var bgScrollOffset by mutableFloatStateOf(0f)
    val gravity = 2400f
    val jumpVelocity = -960f

    // Player State - Runs forward automatically at a fixed forward horizontal position
    var playerX by mutableFloatStateOf(480f)
    var playerY by mutableFloatStateOf(640f)
    var playerVy by mutableFloatStateOf(0f)
    var playerPose by mutableStateOf(PlayerPose.RUN)
    var isOnGround by mutableStateOf(true)
    var crouchTimer by mutableFloatStateOf(0f)
    var stumbleTimer by mutableFloatStateOf(0f)
    var isPropellerSpinning by mutableStateOf(false)

    // Proportional Player Dimensions
    var playerStandHeight by mutableFloatStateOf(140f)
    var playerStandWidth by mutableFloatStateOf(68f)
    var playerCrouchHeight by mutableFloatStateOf(80f)
    var playerCrouchWidth by mutableFloatStateOf(110f)

    // Enemies (School Students mob chasing strictly from behind with a clearly visible gap)
    var enemyX by mutableFloatStateOf(100f)
    var visibleGapBehindPlayer by mutableFloatStateOf(220f)
    var enemyHeight by mutableFloatStateOf(160f)
    var enemyWidth by mutableFloatStateOf(180f)

    // Obstacles and Coins
    val obstacles = mutableStateListOf<Obstacle>()
    val coins = mutableStateListOf<CoinItem>()
    val particles = mutableStateListOf<Particle>()

    private var obstacleSpawnTimer = 0f
    private var nextObstacleInterval = 2.4f
    private var coinSpawnTimer = 0f
    private var nextCoinInterval = 1.8f
    private var idCounter = 0L

    init {
        bestDistance = prefs.getFloat("best_distance", 0f)
        bestCoins = prefs.getInt("best_coins", 0)
    }

    fun updateScreenSize(width: Float, height: Float) {
        if (width > 0 && height > 0) {
            screenWidth = width
            screenHeight = height

            // The road in the background is at the bottom ~13% of the image.
            // Align ground level precisely with the road surface
            groundY = height * 0.885f

            // Proportional character sizes
            playerStandHeight = height * 0.28f
            playerStandWidth = playerStandHeight * (124f / 256f)
            playerCrouchHeight = height * 0.12f
            playerCrouchWidth = playerStandHeight * (140f / 232f)

            enemyHeight = height * 0.32f
            enemyWidth = enemyHeight * (597f / 526f)

            // Player is fixed ahead, running automatically
            playerX = width * 0.54f
            playerY = groundY

            // Responsive 180-250px visible gap between Milon and nearest enemy
            val defaultGap = (width * 0.25f).coerceIn(190f, 250f)
            visibleGapBehindPlayer = defaultGap
            enemyX = playerX - visibleGapBehindPlayer - enemyWidth
        }
    }

    fun startGame() {
        gameState = GameState.PLAYING
        distanceTraveled = 0f
        coinCount = 0
        isNewRecord = false
        baseGameSpeed = 450f
        currentGameSpeed = 450f
        bgScrollOffset = 0f

        playerX = screenWidth * 0.54f
        playerY = groundY
        playerVy = 0f
        playerPose = PlayerPose.RUN
        isOnGround = true
        crouchTimer = 0f
        stumbleTimer = 0f

        // Reset enemies behind player with proper visible gap
        val defaultGap = (screenWidth * 0.25f).coerceIn(190f, 250f)
        visibleGapBehindPlayer = defaultGap
        enemyX = playerX - visibleGapBehindPlayer - enemyWidth

        obstacles.clear()
        coins.clear()
        particles.clear()

        obstacleSpawnTimer = 0f
        nextObstacleInterval = 2.5f
        coinSpawnTimer = 0f
        nextCoinInterval = 1.6f

        audio.startBgm()
    }

    fun pauseGame() {
        if (gameState == GameState.PLAYING) {
            gameState = GameState.PAUSED
            audio.pauseBgm()
        }
    }

    fun resumeGame() {
        if (gameState == GameState.PAUSED) {
            gameState = GameState.PLAYING
            audio.resumeBgm()
        }
    }

    fun returnToTitle() {
        gameState = GameState.TITLE
        playerPose = PlayerPose.RUN
        playerY = groundY
        playerVy = 0f
        val defaultGap = (screenWidth * 0.25f).coerceIn(190f, 250f)
        visibleGapBehindPlayer = defaultGap
        enemyX = playerX - visibleGapBehindPlayer - enemyWidth
        audio.startBgm()
    }

    fun jump() {
        if (gameState != GameState.PLAYING) return

        if (isOnGround) {
            // First jump from ground - play jump sound once!
            playerVy = jumpVelocity
            isOnGround = false
            playerPose = PlayerPose.JUMP
            audio.playJumpSound()

            spawnDustParticles(playerX + 25f, groundY, 7)
        } else if (playerVy > -320f) {
            // Air propeller boost/glide
            playerVy = -480f
            isPropellerSpinning = true
            spawnPropellerParticles(playerX + 30f, playerY - playerStandHeight + 15f, 6)
        }
    }

    fun crouch() {
        if (gameState != GameState.PLAYING) return

        if (!isOnGround) {
            // Quick dive to ground
            playerVy = 900f
        }
        playerPose = PlayerPose.CROUCH
        crouchTimer = 0.65f // Slide duration

        spawnDustParticles(playerX + 20f, groundY, 5)
    }

    fun onCrouchReleased() {
        if (isOnGround && crouchTimer <= 0.15f) {
            playerPose = PlayerPose.RUN
            crouchTimer = 0f
        }
    }

    fun update(dt: Float) {
        if (gameState != GameState.PLAYING) return

        val delta = min(dt, 0.05f)

        // Speed progression over distance
        val speedProgression = 1.0f + (distanceTraveled / 1200f).coerceAtMost(0.85f)
        var targetSpeed = baseGameSpeed * speedProgression

        // Stumble speed reduction
        if (stumbleTimer > 0f) {
            stumbleTimer -= delta
            targetSpeed *= 0.45f
        }
        currentGameSpeed = targetSpeed

        // Update distance & background offset
        val distanceInc = (currentGameSpeed * delta) / 40f
        distanceTraveled += distanceInc
        bgScrollOffset += currentGameSpeed * delta

        // Player is stationary horizontally; world and obstacles move past
        playerX = screenWidth * 0.54f

        // Player Vertical Physics
        if (!isOnGround) {
            playerVy += gravity * delta
            playerY += playerVy * delta

            if (playerY >= groundY) {
                // Landed on the road
                playerY = groundY
                playerVy = 0f
                isOnGround = true
                isPropellerSpinning = false
                playerPose = if (crouchTimer > 0f) PlayerPose.CROUCH else PlayerPose.RUN
                spawnDustParticles(playerX + 25f, groundY, 4)
            } else {
                playerPose = if (crouchTimer > 0f) PlayerPose.CROUCH else PlayerPose.JUMP
            }
        } else {
            // On the road
            if (crouchTimer > 0f) {
                crouchTimer -= delta
                playerPose = PlayerPose.CROUCH
                if (Random.nextFloat() < 0.3f) {
                    spawnDustParticles(playerX + 15f, groundY, 1)
                }
            } else {
                playerPose = PlayerPose.RUN
            }
        }

        // Enemy chasing logic: STRICTLY BEHIND THE MAIN CHARACTER
        updateEnemyChase(delta)

        // Spawners & Updates
        updateObstacles(delta)
        updateCoins(delta)
        updateParticles(delta)

        // Check collisions
        checkCollisions()
    }

    private fun updateEnemyChase(delta: Float) {
        // Minimum distance of approximately 180–250 pixels between main character and nearest enemy
        val minSafeGap = (screenWidth * 0.22f).coerceIn(180f, 250f)
        val defaultGap = (screenWidth * 0.26f).coerceIn(210f, 260f)
        val minCatchDistance = 35f

        if (stumbleTimer > 0f) {
            // Milon stumbled! Enemies surge closer from behind
            visibleGapBehindPlayer = max(minCatchDistance, visibleGapBehindPlayer - 130f * delta)
        } else {
            // If an enemy gets closer than the minimum distance, automatically slow the enemy down
            // until the safe gap is restored, while naturally continuing to chase.
            if (visibleGapBehindPlayer < defaultGap) {
                val recoveryRate = if (visibleGapBehindPlayer < minSafeGap) 90f else 45f
                visibleGapBehindPlayer = min(defaultGap, visibleGapBehindPlayer + recoveryRate * delta)
            }
        }

        // Hard clamp to ensure enemies NEVER overlap or pass the main character
        visibleGapBehindPlayer = max(minCatchDistance, visibleGapBehindPlayer)

        // The nearest enemy is at (playerX - visibleGapBehindPlayer)
        val nearestEnemyX = playerX - visibleGapBehindPlayer
        enemyX = nearestEnemyX - enemyWidth

        // Trigger Game Over if enemy reaches catch distance from behind (before overlapping)
        if (visibleGapBehindPlayer <= minCatchDistance + 1f) {
            triggerGameOver(GameOverReason.CAUGHT_BY_STUDENTS)
        }
    }

    private fun updateObstacles(delta: Float) {
        obstacleSpawnTimer += delta
        if (obstacleSpawnTimer >= nextObstacleInterval) {
            obstacleSpawnTimer = 0f
            nextObstacleInterval = Random.nextFloat() * 1.5f + 2.0f
            spawnObstacle()
        }

        val iterator = obstacles.iterator()
        while (iterator.hasNext()) {
            val obs = iterator.next()
            obs.x -= currentGameSpeed * delta

            if (!obs.isPassed && obs.x + obs.width < playerX) {
                obs.isPassed = true
            }

            if (obs.x + obs.width < -120f) {
                iterator.remove()
            }
        }
    }

    private fun spawnObstacle() {
        val spawnX = screenWidth + 80f
        val roll = Random.nextInt(100)
        val type = when {
            roll < 35 -> ObstacleType.BARRICADE
            roll < 60 -> ObstacleType.CRATE
            roll < 80 -> ObstacleType.CONES
            else -> ObstacleType.OVERHEAD_BEAM
        }

        val obstacle = when (type) {
            ObstacleType.BARRICADE -> {
                val h = screenHeight * 0.13f
                val w = h * 1.25f
                Obstacle(
                    id = ++idCounter,
                    type = type,
                    x = spawnX,
                    y = groundY - h,
                    width = w,
                    height = h
                )
            }
            ObstacleType.CRATE -> {
                val h = screenHeight * 0.12f
                val w = h
                Obstacle(
                    id = ++idCounter,
                    type = type,
                    x = spawnX,
                    y = groundY - h,
                    width = w,
                    height = h
                )
            }
            ObstacleType.CONES -> {
                val h = screenHeight * 0.10f
                val w = h * 1.3f
                Obstacle(
                    id = ++idCounter,
                    type = type,
                    x = spawnX,
                    y = groundY - h,
                    width = w,
                    height = h
                )
            }
            ObstacleType.OVERHEAD_BEAM -> {
                // Overhead beam: hits a standing runner, cleared cleanly by crouching/sliding underneath
                val h = screenHeight * 0.085f
                val w = screenHeight * 0.22f
                // Positioned so the bottom of the beam is safely above the slide height
                val y = groundY - (screenHeight * 0.225f)
                Obstacle(
                    id = ++idCounter,
                    type = type,
                    x = spawnX,
                    y = y,
                    width = w,
                    height = h
                )
            }
        }
        obstacles.add(obstacle)
    }

    private fun updateCoins(delta: Float) {
        coinSpawnTimer += delta
        if (coinSpawnTimer >= nextCoinInterval) {
            coinSpawnTimer = 0f
            nextCoinInterval = Random.nextFloat() * 1.5f + 1.6f
            spawnCoinRow()
        }

        val iterator = coins.iterator()
        while (iterator.hasNext()) {
            val coin = iterator.next()
            coin.x -= currentGameSpeed * delta

            if (coin.x < -60f || coin.isCollected) {
                iterator.remove()
            }
        }
    }

    private fun spawnCoinRow() {
        val startX = screenWidth + 60f
        val count = Random.nextInt(3, 6)
        val isAirArc = Random.nextBoolean()

        for (i in 0 until count) {
            val cx = startX + i * 55f
            val cy = if (isAirArc) {
                val mid = count / 2f
                val arc = 1.0f - (kotlin.math.abs(i - mid) / mid)
                groundY - (playerStandHeight * 0.7f) - (arc * 70f)
            } else {
                groundY - 35f
            }
            coins.add(CoinItem(id = ++idCounter, x = cx, y = cy))
        }
    }

    private fun checkCollisions() {
        // Player hitbox matching actual visible character pose
        val (playerLeft, playerRight, playerTop, playerBottom) = when (playerPose) {
            PlayerPose.CROUCH -> {
                // Shorter and lower collision hitbox for slide: snug on road, low height
                val hitboxW = playerCrouchWidth * 0.72f
                val hitboxH = screenHeight * 0.082f
                val left = playerX + (playerCrouchWidth - hitboxW) / 2f
                val bottom = groundY - 2f
                val top = bottom - hitboxH
                val right = left + hitboxW
                PlayerHitbox(left, right, top, bottom)
            }
            PlayerPose.JUMP -> {
                val hitboxW = playerStandWidth * 0.68f
                val hitboxH = playerStandHeight * 0.78f
                val left = playerX + (playerStandWidth - hitboxW) / 2f
                val bottom = playerY - 2f
                val top = bottom - hitboxH
                val right = left + hitboxW
                PlayerHitbox(left, right, top, bottom)
            }
            PlayerPose.RUN -> {
                val hitboxW = playerStandWidth * 0.68f
                val hitboxH = playerStandHeight * 0.78f
                val left = playerX + (playerStandWidth - hitboxW) / 2f
                val bottom = groundY - 2f
                val top = bottom - hitboxH
                val right = left + hitboxW
                PlayerHitbox(left, right, top, bottom)
            }
        }

        // 1. Obstacle collisions
        for (obs in obstacles) {
            if (obs.isHit) continue

            // Forgiving margins on obstacles so transparent borders never trigger unfair collisions
            val obsMarginX = obs.width * 0.10f
            val obsMarginY = obs.height * 0.08f
            val obsLeft = obs.x + obsMarginX
            val obsRight = obs.x + obs.width - obsMarginX
            val obsTop = obs.y + obsMarginY
            val obsBottom = obs.y + obs.height - 2f

            val overlaps = playerRight > obsLeft &&
                    playerLeft < obsRight &&
                    playerBottom > obsTop &&
                    playerTop < obsBottom

            if (overlaps) {
                obs.isHit = true
                if (visibleGapBehindPlayer < 75f) {
                    // Chasing mob catches player during crash
                    triggerGameOver(GameOverReason.CRASH_OBSTACLE)
                    return
                } else {
                    // Stumble! Enemies surge closer
                    stumbleTimer = 1.2f
                    visibleGapBehindPlayer = max(45f, visibleGapBehindPlayer - 95f)
                    spawnImpactParticles(obs.x + obs.width / 2f, obs.y + obs.height / 2f, 10)
                }
            }
        }

        // 2. Coin collection
        val playerCenterX = (playerLeft + playerRight) / 2f
        val playerCenterY = (playerTop + playerBottom) / 2f
        val collectRadius = 45f

        for (coin in coins) {
            if (coin.isCollected) continue

            val dx = playerCenterX - coin.x
            val dy = playerCenterY - coin.y
            val distSq = dx * dx + dy * dy

            if (distSq < collectRadius * collectRadius) {
                coin.isCollected = true
                coinCount++
                // Collecting coins widens the escape gap from enemies
                val maxGap = (screenWidth * 0.28f).coerceIn(220f, 270f)
                visibleGapBehindPlayer = min(maxGap, visibleGapBehindPlayer + 16f)
                spawnCoinSparkles(coin.x, coin.y, 7)
            }
        }
    }

    private fun triggerGameOver(reason: GameOverReason) {
        gameState = GameState.GAME_OVER
        gameOverReason = reason
        audio.pauseBgm()

        // Enemies remain strictly behind, maintaining separation without overlapping
        val nearestEnemyX = playerX - max(35f, visibleGapBehindPlayer)
        enemyX = nearestEnemyX - enemyWidth

        isNewRecord = distanceTraveled > bestDistance
        if (isNewRecord) {
            bestDistance = distanceTraveled
            prefs.edit().putFloat("best_distance", bestDistance).apply()
        }
        if (coinCount > bestCoins) {
            bestCoins = coinCount
            prefs.edit().putInt("best_coins", bestCoins).apply()
        }
    }

    private fun updateParticles(delta: Float) {
        val iterator = particles.iterator()
        while (iterator.hasNext()) {
            val p = iterator.next()
            p.currentLife += delta
            p.x += p.vx * delta
            p.y += p.vy * delta

            if (p.currentLife >= p.maxLife) {
                iterator.remove()
            }
        }
    }

    private fun spawnDustParticles(x: Float, y: Float, count: Int) {
        for (i in 0 until count) {
            particles.add(
                Particle(
                    x = x + Random.nextFloat() * 16f - 8f,
                    y = y - Random.nextFloat() * 8f,
                    vx = -Random.nextFloat() * 120f - 40f,
                    vy = -Random.nextFloat() * 50f - 20f,
                    color = 0xFFCCCCCC,
                    maxLife = 0.35f + Random.nextFloat() * 0.2f,
                    radius = 3.5f + Random.nextFloat() * 4f
                )
            )
        }
    }

    private fun spawnPropellerParticles(x: Float, y: Float, count: Int) {
        for (i in 0 until count) {
            particles.add(
                Particle(
                    x = x + Random.nextFloat() * 24f - 12f,
                    y = y + Random.nextFloat() * 8f,
                    vx = -Random.nextFloat() * 140f - 30f,
                    vy = Random.nextFloat() * 60f - 30f,
                    color = 0xFF64B5F6,
                    maxLife = 0.4f,
                    radius = 3f + Random.nextFloat() * 3f
                )
            )
        }
    }

    private fun spawnCoinSparkles(x: Float, y: Float, count: Int) {
        for (i in 0 until count) {
            val angle = Random.nextFloat() * 2 * Math.PI.toFloat()
            val speed = Random.nextFloat() * 120f + 60f
            particles.add(
                Particle(
                    x = x,
                    y = y,
                    vx = kotlin.math.cos(angle) * speed,
                    vy = kotlin.math.sin(angle) * speed,
                    color = 0xFFFFD700,
                    maxLife = 0.45f,
                    radius = 3.5f + Random.nextFloat() * 3f
                )
            )
        }
    }

    private fun spawnImpactParticles(x: Float, y: Float, count: Int) {
        for (i in 0 until count) {
            val angle = Random.nextFloat() * 2 * Math.PI.toFloat()
            val speed = Random.nextFloat() * 160f + 50f
            particles.add(
                Particle(
                    x = x,
                    y = y,
                    vx = kotlin.math.cos(angle) * speed,
                    vy = kotlin.math.sin(angle) * speed,
                    color = 0xFFFF5722,
                    maxLife = 0.5f,
                    radius = 4.5f + Random.nextFloat() * 4f
                )
            )
        }
    }
}

private data class PlayerHitbox(
    val left: Float,
    val right: Float,
    val top: Float,
    val bottom: Float
)

