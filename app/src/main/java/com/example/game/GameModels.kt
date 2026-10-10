package com.example.game

enum class GameState {
    SPLASH,
    TITLE,
    PLAYING,
    PAUSED,
    GAME_OVER
}

enum class PlayerPose {
    RUN,
    JUMP,
    CROUCH
}

enum class ObstacleType {
    GROUND_OBSTACLE,   // On the road surface - Jump required
    OVERHEAD_OBSTACLE  // Elevated road barrier - Slide required
}

enum class GameOverReason {
    CAUGHT_BY_STUDENTS,
    CRASH_OBSTACLE
}

data class Obstacle(
    val id: Long,
    val type: ObstacleType,
    var x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val variantIndex: Int = 0,
    var isPassed: Boolean = false,
    var isHit: Boolean = false
)

data class CoinItem(
    val id: Long,
    var x: Float,
    var y: Float,
    val radius: Float = 22f,
    var isCollected: Boolean = false
)

data class PowerUpItem(
    val id: Long,
    var x: Float,
    var y: Float,
    val width: Float,
    val height: Float,
    val variantIndex: Int = 0,
    var isCollected: Boolean = false
)

data class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val color: Long,
    val maxLife: Float,
    var currentLife: Float = 0f,
    val radius: Float
)
