package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.util.Log
import com.example.R

class GameAudio(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    private var splashPlayer: MediaPlayer? = null
    private var soundPool: SoundPool? = null
    private var jumpSoundId: Int = 0
    private var isJumpSoundLoaded: Boolean = false
    private var isMuted: Boolean = false
    private var isBgmPlaying: Boolean = false

    init {
        try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            soundPool = SoundPool.Builder()
                .setMaxStreams(4)
                .setAudioAttributes(audioAttributes)
                .build()?.apply {
                    setOnLoadCompleteListener { _, sampleId, status ->
                        if (status == 0 && sampleId == jumpSoundId) {
                            isJumpSoundLoaded = true
                        }
                    }
                }

            jumpSoundId = soundPool?.load(context, R.raw.jump, 1) ?: 0
        } catch (e: Exception) {
            Log.e("GameAudio", "Error initializing SoundPool", e)
        }
    }

    fun playSplashSound() {
        if (isMuted) return
        try {
            stopSplashSound()
            splashPlayer = MediaPlayer.create(context, R.raw.splash)?.apply {
                isLooping = false
                setVolume(1.0f, 1.0f)
                setOnCompletionListener {
                    it.release()
                    splashPlayer = null
                }
                start()
            }
        } catch (e: Exception) {
            Log.e("GameAudio", "Error playing splash sound", e)
        }
    }

    fun stopSplashSound() {
        try {
            splashPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
            splashPlayer = null
        } catch (e: Exception) {
            Log.e("GameAudio", "Error stopping splash sound", e)
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
            stopSplashSound()
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
        stopSplashSound()
        soundPool?.release()
        soundPool = null
    }
}
