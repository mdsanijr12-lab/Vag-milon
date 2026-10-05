package com.example.game

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import com.example.R
import java.io.File
import java.io.FileOutputStream

data class PoseSprite(
    val bitmap: Bitmap,
    val aspectRatio: Float,
    val visibleBottomRatio: Float
)

object Character2AssetManager {
    private const val FILE_RUN = "char2_custom_run.png"
    private const val FILE_JUMP = "char2_custom_jump.png"
    private const val FILE_SLIDE = "char2_custom_slide.png"

    fun loadRunSprite(context: Context): PoseSprite {
        val customFile = File(context.filesDir, FILE_RUN)
        val bmp = if (customFile.exists()) {
            BitmapFactory.decodeFile(customFile.absolutePath)
        } else {
            null
        } ?: BitmapFactory.decodeResource(context.resources, R.drawable.char2_run)
        return buildPoseSprite(bmp)
    }

    fun loadJumpSprite(context: Context): PoseSprite {
        val customFile = File(context.filesDir, FILE_JUMP)
        val bmp = if (customFile.exists()) {
            BitmapFactory.decodeFile(customFile.absolutePath)
        } else {
            null
        } ?: BitmapFactory.decodeResource(context.resources, R.drawable.char2_jump)
        return buildPoseSprite(bmp)
    }

    fun loadSlideSprite(context: Context): PoseSprite {
        val customFile = File(context.filesDir, FILE_SLIDE)
        val bmp = if (customFile.exists()) {
            BitmapFactory.decodeFile(customFile.absolutePath)
        } else {
            null
        } ?: BitmapFactory.decodeResource(context.resources, R.drawable.char2_crouch)
        return buildPoseSprite(bmp)
    }

    fun saveCustomPoseUri(context: Context, pose: PlayerPose, uri: Uri): Boolean {
        return try {
            val fileName = when (pose) {
                PlayerPose.RUN -> FILE_RUN
                PlayerPose.JUMP -> FILE_JUMP
                PlayerPose.CROUCH -> FILE_SLIDE
            }
            val input = context.contentResolver.openInputStream(uri) ?: return false
            val decoded = BitmapFactory.decodeStream(input)
            input.close()
            if (decoded == null) return false

            val outFile = File(context.filesDir, fileName)
            FileOutputStream(outFile).use { out ->
                decoded.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun buildPoseSprite(bitmap: Bitmap): PoseSprite {
        val w = bitmap.width.coerceAtLeast(1)
        val h = bitmap.height.coerceAtLeast(1)
        val aspect = w.toFloat() / h.toFloat()
        val bottomRatio = computeVisibleBottomRatio(bitmap)
        return PoseSprite(
            bitmap = bitmap,
            aspectRatio = aspect,
            visibleBottomRatio = bottomRatio
        )
    }

    /**
     * Scans from the bottom of the bitmap upward to find the lowest visible pixel row,
     * ensuring the character's feet land directly on the road surface even if the PNG
     * has transparent padding at the bottom.
     */
    private fun computeVisibleBottomRatio(bitmap: Bitmap): Float {
        val w = bitmap.width
        val h = bitmap.height
        if (w <= 0 || h <= 0 || !bitmap.hasAlpha()) return 1.0f

        val stepX = (w / 24).coerceAtLeast(1)
        for (y in (h - 1) downTo (h / 3)) {
            var x = 0
            while (x < w) {
                val pixel = bitmap.getPixel(x, y)
                if (Color.alpha(pixel) > 25) {
                    return ((y + 1).toFloat() / h.toFloat()).coerceIn(0.5f, 1.0f)
                }
                x += stepX
            }
        }
        return 1.0f
    }
}
