package com.example.game

enum class GameState {
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
    BARRICADE,     // Low wooden saw-horse with hazard stripes - Jump required
    CRATE,         // Low wooden shipping crate - Jump required
    CONES,         // Low traffic cones - Jump required
    OVERHEAD_BEAM  // Overhead caution sign / beam - Crouch/Slide required
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
