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
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

class GameEngine(
    context: Context,
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
    var groundY by mutableFloatStateOf(637f)

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
    var baseGameSpeed by mutableFloatStateOf(460f) // px/s
    var currentGameSpeed by mutableFloatStateOf(460f)
    var bgScrollOffset by mutableFloatStateOf(0f)
    var parallaxScrollOffset by mutableFloatStateOf(0f)
    val gravity = 2350f
    val jumpVelocity = -1020f

    // Player State - Runs forward automatically at a fixed horizontal road position
    var playerX by mutableFloatStateOf(480f)
    var playerY by mutableFloatStateOf(637f)
    var playerVy by mutableFloatStateOf(0f)
    var playerPose by mutableStateOf(PlayerPose.RUN)
    var isOnGround by mutableStateOf(true)
    var crouchTimer by mutableFloatStateOf(0f)
    var stumbleTimer by mutableFloatStateOf(0f)

    // Power-Up State (Temporary Speed Boost with visible duration indicator)
    val powerUpMaxDuration = 5.0f
    var powerUpTimer by mutableFloatStateOf(0f)
        private set
    val isPowerUpActive: Boolean
        get() = powerUpTimer > 0f

    // Proportional Player Dimensions (calibrated to 887px sprite canvas height)
    var playerCanvasHeight by mutableFloatStateOf(245f)
    var playerStandHeight by mutableFloatStateOf(188f)
    var playerStandWidth by mutableFloatStateOf(142f)
    var playerCrouchHeight by mutableFloatStateOf(140f)
    var playerCrouchWidth by mutableFloatStateOf(192f)

    // Enemies (Chasing strictly from behind with a clearly visible gap)
    var enemyX by mutableFloatStateOf(100f)
    var visibleGapBehindPlayer by mutableFloatStateOf(220f)
    var enemyHeight by mutableFloatStateOf(160f)
    var enemyWidth by mutableFloatStateOf(180f)

    // World Objects
    val obstacles = mutableStateListOf<Obstacle>()
    val coins = mutableStateListOf<CoinItem>()
    val powerUps = mutableStateListOf<PowerUpItem>()
    val particles = mutableStateListOf<Particle>()

    private var obstacleSpawnTimer = 0f
    private var nextObstacleInterval = 2.4f
    private var coinSpawnTimer = 0f
    private var nextCoinInterval = 1.6f
    private var powerUpSpawnTimer = 0f
    private var nextPowerUpInterval = 7.5f
    private var idCounter = 0L

    init {
        bestDistance = prefs.getFloat("best_distance", 0f)
        bestCoins = prefs.getInt("best_coins", 0)
    }

    fun updateScreenSize(width: Float, height: Float) {
        if (width > 0 && height > 0) {
            screenWidth = width
            screenHeight = height

            // Align ground level directly with the road surface
            groundY = height * 0.885f

            // All 3 character sprites share an 887px canvas height:
            // player_run: 557x887 (visible 514x684)
            // player_jump: 561x887 (visible 542x634)
            // player_crouch: 726x887 (visible 696x506)
            playerCanvasHeight = height * 0.34f
            playerStandHeight = playerCanvasHeight * (684f / 887f)
            playerStandWidth = playerCanvasHeight * (514f / 887f)
            playerCrouchHeight = playerCanvasHeight * (506f / 887f)
            playerCrouchWidth = playerCanvasHeight * (696f / 887f)

            enemyHeight = height * 0.31f
            enemyWidth = enemyHeight * (597f / 526f)

            playerX = width * 0.52f
            if (isOnGround) {
                playerY = groundY
            }

            val defaultGap = (width * 0.24f).coerceIn(185f, 250f)
            if (gameState != GameState.PLAYING) {
                visibleGapBehindPlayer = defaultGap
            }
            enemyX = playerX - visibleGapBehindPlayer - enemyWidth
        }
    }

    fun startGame() {
        gameState = GameState.PLAYING
        distanceTraveled = 0f
        coinCount = 0
        isNewRecord = false
        baseGameSpeed = 460f
        currentGameSpeed = 460f
        bgScrollOffset = 0f
        parallaxScrollOffset = 0f

        playerX = screenWidth * 0.52f
        playerY = groundY
        playerVy = 0f
        playerPose = PlayerPose.RUN
        isOnGround = true
        crouchTimer = 0f
        stumbleTimer = 0f
        powerUpTimer = 0f

        val defaultGap = (screenWidth * 0.24f).coerceIn(185f, 250f)
        visibleGapBehindPlayer = defaultGap
        enemyX = playerX - visibleGapBehindPlayer - enemyWidth

        obstacles.clear()
        coins.clear()
        powerUps.clear()
        particles.clear()

        obstacleSpawnTimer = 0f
        nextObstacleInterval = 2.2f
        coinSpawnTimer = 0f
        nextCoinInterval = 1.4f
        powerUpSpawnTimer = 0f
        nextPowerUpInterval = 6.5f

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
        isOnGround = true
        crouchTimer = 0f
        powerUpTimer = 0f
        val defaultGap = (screenWidth * 0.24f).coerceIn(185f, 250f)
        visibleGapBehindPlayer = defaultGap
        enemyX = playerX - visibleGapBehindPlayer - enemyWidth
        audio.startBgm()
    }

    /**
     * Triggers a single valid jump from the ground.
     * Prevents double jumping and prevents repeated sound playback while airborne.
     */
    fun jump() {
        if (gameState != GameState.PLAYING) return
        if (!isOnGround) return

        crouchTimer = 0f
        playerVy = jumpVelocity
        isOnGround = false
        playerPose = PlayerPose.JUMP
        audio.playJumpSound()

        spawnDustParticles(playerX + playerStandWidth * 0.4f, groundY, 7)
    }

    /**
     * Switches to the SLIDE state with a shorter, lower collision hitbox.
     */
    fun crouch() {
        if (gameState != GameState.PLAYING) return

        // Avoid re-triggering if already in the middle of a fresh slide
        if (playerPose == PlayerPose.CROUCH && crouchTimer > 0.55f) return

        if (!isOnGround) {
            // Fast descent to slide on the road
            playerVy = max(playerVy, 1100f)
        }
        playerPose = PlayerPose.CROUCH
        crouchTimer = 0.72f

        spawnDustParticles(playerX + playerStandWidth * 0.35f, groundY, 5)
    }

    fun onCrouchReleased() {
        if (playerPose == PlayerPose.CROUCH && crouchTimer > 0.25f) {
            crouchTimer = 0.22f
        }
    }

    fun update(deltaSec: Float) {
        val delta = deltaSec.coerceIn(0f, 0.05f)

        if (gameState == GameState.TITLE || gameState == GameState.SPLASH) {
            bgScrollOffset += 150f * delta
            parallaxScrollOffset += 55f * delta
            return
        }

        if (gameState != GameState.PLAYING) return

        // 1. Update Speed & Power-Up Boost
        baseGameSpeed = (460f + (distanceTraveled * 0.32f)).coerceAtMost(860f)

        if (powerUpTimer > 0f) {
            powerUpTimer = max(0f, powerUpTimer - delta)
        }

        val speedMultiplier = when {
            powerUpTimer > 0f -> 1.35f
            stumbleTimer > 0f -> 0.72f
            else -> 1.0f
        }
        currentGameSpeed = baseGameSpeed * speedMultiplier

        if (stumbleTimer > 0f) {
            stumbleTimer = max(0f, stumbleTimer - delta)
        }

        // Scroll background & parallax layers smoothly at different speeds
        bgScrollOffset += currentGameSpeed * delta
        parallaxScrollOffset += (currentGameSpeed * 0.36f) * delta
        distanceTraveled += (currentGameSpeed * delta) / 38f

        // 2. Update Player Physics & Animation States
        if (!isOnGround) {
            playerVy += gravity * delta
            playerY += playerVy * delta

            if (playerY >= groundY) {
                playerY = groundY
                playerVy = 0f
                isOnGround = true
                playerPose = if (crouchTimer > 0f) PlayerPose.CROUCH else PlayerPose.RUN
                spawnDustParticles(playerX + playerStandWidth * 0.4f, groundY, 5)
            }
        } else {
            playerY = groundY
            if (crouchTimer > 0f) {
                crouchTimer -= delta
                if (crouchTimer <= 0f) {
                    crouchTimer = 0f
                    playerPose = PlayerPose.RUN
                } else {
                    playerPose = PlayerPose.CROUCH
                }
            } else {
                playerPose = PlayerPose.RUN
            }
        }

        // 3. Update Enemy Chase Distance (Enemies always stay strictly behind player)
        val maxSafeGap = (screenWidth * 0.27f).coerceIn(210f, 265f)
        val minSafeGap = 52f
        val gapRecoveryRate = if (powerUpTimer > 0f) 42f else if (stumbleTimer <= 0f) 14f else -20f
        visibleGapBehindPlayer = (visibleGapBehindPlayer + gapRecoveryRate * delta).coerceIn(minSafeGap, maxSafeGap)
        enemyX = playerX - visibleGapBehindPlayer - enemyWidth

        // 4. Update Obstacles, Coins, Power-Ups, and Particles
        updateObstacles(delta)
        updateCoins(delta)
        updatePowerUps(delta)
        updateParticles(delta)

        // 5. Accurate Collision Detection
        checkCollisions()
    }

    private fun updateObstacles(delta: Float) {
        obstacleSpawnTimer += delta
        if (obstacleSpawnTimer >= nextObstacleInterval) {
            obstacleSpawnTimer = 0f
            val speedFactor = (460f / currentGameSpeed).coerceIn(0.58f, 1.0f)
            nextObstacleInterval = (Random.nextFloat() * 1.0f + 1.85f) * speedFactor
            spawnObstacle()
        }

        val iterator = obstacles.iterator()
        while (iterator.hasNext()) {
            val obs = iterator.next()
            obs.x -= currentGameSpeed * delta

            if (!obs.isPassed && obs.x + obs.width < playerX) {
                obs.isPassed = true
            }

            // Remove obstacles after they leave the playable area
            if (obs.x + obs.width < -120f) {
                iterator.remove()
            }
        }
    }

    private fun spawnObstacle() {
        val spawnX = screenWidth + 90f
        // Ensure fair spacing from any existing obstacle
        val lastObs = obstacles.lastOrNull()
        if (lastObs != null && spawnX - (lastObs.x + lastObs.width) < screenWidth * 0.38f) {
            return
        }

        // Mix of jumpable ground obstacles and slideable overhead obstacles
        val type = if (Random.nextFloat() < 0.38f) {
            ObstacleType.OVERHEAD_OBSTACLE
        } else {
            ObstacleType.GROUND_OBSTACLE
        }
        val variant = Random.nextInt(4)

        val obstacle = when (type) {
            ObstacleType.GROUND_OBSTACLE -> {
                val h = screenHeight * 0.115f
                val w = h * 1.42f
                Obstacle(
                    id = ++idCounter,
                    type = type,
                    x = spawnX,
                    y = groundY - h,
                    width = w,
                    height = h,
                    variantIndex = variant
                )
            }
            ObstacleType.OVERHEAD_OBSTACLE -> {
                // Overhead obstacle: positioned so standing runner collides,
                // while sliding runner passes cleanly underneath
                val h = screenHeight * 0.095f
                val w = screenHeight * 0.21f
                // Bottom of overhead obstacle is at groundY - 0.145 * screenHeight
                // Sliding hitbox top is at groundY - 0.080 * screenHeight -> 0.065 * H safe clearance!
                val bottomY = groundY - (screenHeight * 0.145f)
                Obstacle(
                    id = ++idCounter,
                    type = type,
                    x = spawnX,
                    y = bottomY - h,
                    width = w,
                    height = h,
                    variantIndex = variant
                )
            }
        }

        if (obstacles.size < 12) {
            obstacles.add(obstacle)
        }
    }

    private fun updateCoins(delta: Float) {
        coinSpawnTimer += delta
        if (coinSpawnTimer >= nextCoinInterval) {
            coinSpawnTimer = 0f
            nextCoinInterval = Random.nextFloat() * 1.2f + 1.4f
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
        if (coins.size >= 24) return
        val startX = screenWidth + 70f
        val count = Random.nextInt(3, 6)
        val isAirArc = Random.nextBoolean()
        val spacing = screenHeight * 0.08f
        val coinRadius = (screenHeight * 0.032f).coerceIn(18f, 28f)

        for (i in 0 until count) {
            val cx = startX + i * spacing
            val cy = if (isAirArc) {
                val mid = (count - 1).coerceAtLeast(1) / 2f
                val normalized = 1.0f - (abs(i - mid) / (mid + 0.5f))
                groundY - (screenHeight * 0.14f) - (normalized * screenHeight * 0.11f)
            } else {
                // Positioned directly along the running path on the road
                groundY - (screenHeight * 0.075f)
            }
            coins.add(
                CoinItem(
                    id = ++idCounter,
                    x = cx,
                    y = cy,
                    radius = coinRadius
                )
            )
        }
    }

    private fun updatePowerUps(delta: Float) {
        powerUpSpawnTimer += delta
        if (powerUpSpawnTimer >= nextPowerUpInterval) {
            powerUpSpawnTimer = 0f
            nextPowerUpInterval = Random.nextFloat() * 4.0f + 7.0f
            spawnPowerUp()
        }

        val iterator = powerUps.iterator()
        while (iterator.hasNext()) {
            val item = iterator.next()
            item.x -= currentGameSpeed * delta

            if (item.x + item.width < -80f || item.isCollected) {
                iterator.remove()
            }
        }
    }

    private fun spawnPowerUp() {
        if (powerUps.size >= 3) return
        val spawnX = screenWidth + 140f

        // Avoid spawning directly on top of an obstacle
        val overlapsObstacle = obstacles.any { obs ->
            abs((obs.x + obs.width / 2f) - spawnX) < screenWidth * 0.18f
        }
        val finalX = if (overlapsObstacle) spawnX + screenWidth * 0.22f else spawnX

        val h = screenHeight * 0.085f
        val w = h * 1.25f
        // Positioned at chest/reachable height along the running path
        val y = groundY - (screenHeight * 0.15f)

        powerUps.add(
            PowerUpItem(
                id = ++idCounter,
                x = finalX,
                y = y,
                width = w,
                height = h,
                variantIndex = Random.nextInt(4)
            )
        )
    }

    private fun checkCollisions() {
        // 1. State-dependent obstacle collision hitbox (forgiving around transparent edges)
        val obstacleHitbox = when (playerPose) {
            PlayerPose.CROUCH -> {
                // Substantially shorter and lower hitbox during SLIDE
                val w = playerStandWidth * 0.72f
                val h = screenHeight * 0.080f
                val left = playerX + (playerStandWidth - w) * 0.5f
                val bottom = groundY - 2f
                val top = bottom - h
                PlayerHitbox(left, left + w, top, bottom)
            }
            PlayerPose.JUMP -> {
                // Airborne hitbox matching jumping character
                val w = playerStandWidth * 0.60f
                val h = playerStandHeight * 0.74f
                val left = playerX + (playerStandWidth - w) * 0.5f
                val bottom = playerY - 6f
                val top = bottom - h
                PlayerHitbox(left, left + w, top, bottom)
            }
            PlayerPose.RUN -> {
                // Normal running hitbox
                val w = playerStandWidth * 0.62f
                val h = playerStandHeight * 0.78f
                val left = playerX + (playerStandWidth - w) * 0.5f
                val bottom = groundY - 2f
                val top = bottom - h
                PlayerHitbox(left, left + w, top, bottom)
            }
        }

        for (obs in obstacles) {
            if (obs.isHit) continue

            val obsMarginX = obs.width * 0.12f
            val obsMarginY = obs.height * 0.10f
            val obsLeft = obs.x + obsMarginX
            val obsRight = obs.x + obs.width - obsMarginX
            val obsTop = obs.y + obsMarginY
            val obsBottom = obs.y + obs.height - 2f

            val hitsObstacle = obstacleHitbox.right > obsLeft &&
                    obstacleHitbox.left < obsRight &&
                    obstacleHitbox.bottom > obsTop &&
                    obstacleHitbox.top < obsBottom

            if (hitsObstacle) {
                obs.isHit = true
                if (isPowerUpActive) {
                    // Speed boost shield absorbs one obstacle stumble
                    spawnImpactParticles(obs.x + obs.width / 2f, obs.y + obs.height / 2f, 12)
                } else if (visibleGapBehindPlayer < 75f) {
                    triggerGameOver(GameOverReason.CRASH_OBSTACLE)
                    return
                } else {
                    stumbleTimer = 1.15f
                    visibleGapBehindPlayer = max(48f, visibleGapBehindPlayer - 90f)
                    spawnImpactParticles(obs.x + obs.width / 2f, obs.y + obs.height / 2f, 10)
                }
            }
        }

        // 2. Visible Character Body Bounds for Collectible Pickup (Coins & Power-Ups)
        // Uses the full visible body span of the active pose so any touch collects reliably
        val pickupWidth = if (playerPose == PlayerPose.CROUCH) playerCrouchWidth * 0.88f else playerStandWidth * 0.88f
        val pickupHeight = if (playerPose == PlayerPose.CROUCH) playerCrouchHeight * 0.75f else playerStandHeight * 0.92f
        val pickupLeft = playerX + (playerStandWidth - pickupWidth) * 0.5f - 8f
        val pickupRight = pickupLeft + pickupWidth + 16f
        val pickupBottom = playerY + 6f
        val pickupTop = playerY - pickupHeight - 8f

        // Coin Collection (AABB overlap between visible character body and coin circle bounds)
        val coinIter = coins.iterator()
        while (coinIter.hasNext()) {
            val coin = coinIter.next()
            if (coin.isCollected) continue

            val coinLeft = coin.x - coin.radius
            val coinRight = coin.x + coin.radius
            val coinTop = coin.y - coin.radius
            val coinBottom = coin.y + coin.radius

            val touchesCoin = pickupRight >= coinLeft &&
                    pickupLeft <= coinRight &&
                    pickupBottom >= coinTop &&
                    pickupTop <= coinBottom

            if (touchesCoin) {
                coin.isCollected = true
                coinCount++
                val maxGap = (screenWidth * 0.27f).coerceIn(210f, 265f)
                visibleGapBehindPlayer = min(maxGap, visibleGapBehindPlayer + 14f)
                spawnCoinSparkles(coin.x, coin.y, 7)
                coinIter.remove()
            }
        }

        // Power-Up Collection (Activates once per pickup and removes power-up)
        val powerIter = powerUps.iterator()
        while (powerIter.hasNext()) {
            val item = powerIter.next()
            if (item.isCollected) continue

            val itemLeft = item.x
            val itemRight = item.x + item.width
            val itemTop = item.y
            val itemBottom = item.y + item.height

            val touchesPowerUp = pickupRight >= itemLeft &&
                    pickupLeft <= itemRight &&
                    pickupBottom >= itemTop &&
                    pickupTop <= itemBottom

            if (touchesPowerUp) {
                item.isCollected = true
                powerUpTimer = powerUpMaxDuration
                stumbleTimer = 0f
                val maxGap = (screenWidth * 0.27f).coerceIn(210f, 265f)
                visibleGapBehindPlayer = min(maxGap, visibleGapBehindPlayer + 45f)
                spawnPowerUpParticles(item.x + item.width / 2f, item.y + item.height / 2f, 14)
                powerIter.remove()
            }
        }
    }

    private fun triggerGameOver(reason: GameOverReason) {
        gameState = GameState.GAME_OVER
        gameOverReason = reason

        val distRecord = distanceTraveled > bestDistance
        val coinRecord = coinCount > bestCoins
        isNewRecord = distRecord || coinRecord

        val editor = prefs.edit()
        if (distRecord) {
            bestDistance = distanceTraveled
            editor.putFloat("best_distance", bestDistance)
        }
        if (coinRecord) {
            bestCoins = coinCount
            editor.putInt("best_coins", bestCoins)
        }
        editor.apply()
    }

    private fun updateParticles(delta: Float) {
        val iterator = particles.iterator()
        while (iterator.hasNext()) {
            val p = iterator.next()
            p.x += p.vx * delta
            p.y += p.vy * delta
            p.currentLife += delta
            if (p.currentLife >= p.maxLife) {
                iterator.remove()
            }
        }
    }

    private fun spawnDustParticles(x: Float, y: Float, count: Int) {
        if (particles.size > 60) return
        for (i in 0 until count) {
            particles.add(
                Particle(
                    x = x + Random.nextFloat() * 20f - 10f,
                    y = y - Random.nextFloat() * 8f,
                    vx = -Random.nextFloat() * 140f - 50f,
                    vy = -Random.nextFloat() * 70f - 10f,
                    color = 0xFFD7CCC8,
                    maxLife = 0.35f + Random.nextFloat() * 0.2f,
                    radius = 4f + Random.nextFloat() * 4f
                )
            )
        }
    }

    private fun spawnCoinSparkles(x: Float, y: Float, count: Int) {
        if (particles.size > 60) return
        for (i in 0 until count) {
            val angle = Random.nextFloat() * 2 * Math.PI.toFloat()
            val speed = Random.nextFloat() * 120f + 60f
            particles.add(
                Particle(
                    x = x,
                    y = y,
                    vx = cos(angle) * speed,
                    vy = sin(angle) * speed,
                    color = 0xFFFFD700,
                    maxLife = 0.42f,
                    radius = 3.5f + Random.nextFloat() * 3f
                )
            )
        }
    }

    private fun spawnPowerUpParticles(x: Float, y: Float, count: Int) {
        if (particles.size > 60) return
        for (i in 0 until count) {
            val angle = Random.nextFloat() * 2 * Math.PI.toFloat()
            val speed = Random.nextFloat() * 170f + 70f
            particles.add(
                Particle(
                    x = x,
                    y = y,
                    vx = cos(angle) * speed,
                    vy = sin(angle) * speed,
                    color = 0xFF00E5FF,
                    maxLife = 0.50f,
                    radius = 4.5f + Random.nextFloat() * 3.5f
                )
            )
        }
    }

    private fun spawnImpactParticles(x: Float, y: Float, count: Int) {
        if (particles.size > 60) return
        for (i in 0 until count) {
            val angle = Random.nextFloat() * 2 * Math.PI.toFloat()
            val speed = Random.nextFloat() * 160f + 50f
            particles.add(
                Particle(
                    x = x,
                    y = y,
                    vx = cos(angle) * speed,
                    vy = sin(angle) * speed,
                    color = 0xFFFF5722,
                    maxLife = 0.48f,
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
