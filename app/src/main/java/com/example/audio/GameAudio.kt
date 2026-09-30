package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.util.Log
import com.example.R

class GameAudio(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    private var soundPool: SoundPool? = null
    private var jumpSoundId: Int = 0
    private var coinSoundId: Int = 0
    private var isMuted: Boolean = false
    private var isBgmPlaying: Boolean = false

    init {
        try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            soundPool = SoundPool.Builder()
                .setMaxStreams(5)
                .setAudioAttributes(audioAttributes)
                .build()

            jumpSoundId = soundPool?.load(context, R.raw.jump, 1) ?: 0
        } catch (e: Exception) {
            Log.e("GameAudio", "Error initializing SoundPool", e)
        }
    }

    fun startBgm() {
        if (isMuted) return
        try {
            if (mediaPlayer == null) {
                mediaPlayer = MediaPlayer.create(context, R.raw.bgm)?.apply {
                    isLooping = true
                    setVolume(0.85f, 0.85f)
                }
            }
            mediaPlayer?.let {
                if (!it.isPlaying) {
                    it.start()
                    isBgmPlaying = true
                }
            }
        } catch (e: Exception) {
            Log.e("GameAudio", "Error starting BGM", e)
        }
    }

    fun pauseBgm() {
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
            }
        } catch (e: Exception) {
            Log.e("GameAudio", "Error pausing BGM", e)
        }
    }

    fun resumeBgm() {
        if (isMuted) return
        try {
            if (isBgmPlaying && mediaPlayer?.isPlaying == false) {
                mediaPlayer?.start()
            }
        } catch (e: Exception) {
            Log.e("GameAudio", "Error resuming BGM", e)
        }
    }

    fun stopBgm() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
            isBgmPlaying = false
        } catch (e: Exception) {
            Log.e("GameAudio", "Error stopping BGM", e)
        }
    }

    fun playJumpSound() {
        if (isMuted) return
        try {
            if (jumpSoundId != 0) {
                soundPool?.play(jumpSoundId, 1.0f, 1.0f, 1, 0, 1.0f)
            }
        } catch (e: Exception) {
            Log.e("GameAudio", "Error playing jump sound", e)
        }
    }

    fun toggleMute(): Boolean {
        isMuted = !isMuted
        if (isMuted) {
            pauseBgm()
        } else {
            resumeBgm()
            if (mediaPlayer == null || mediaPlayer?.isPlaying == false) {
                startBgm()
            }
        }
        return isMuted
    }

    fun isAudioMuted(): Boolean = isMuted

    fun release() {
        stopBgm()
        soundPool?.release()
        soundPool = null
    }
}
