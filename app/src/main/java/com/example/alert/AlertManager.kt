package com.example.alert

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class AlertManager(private val context: Context) {
    private var ringtone: Ringtone? = null
    private var mediaPlayer: MediaPlayer? = null

    fun playLoudAlert(soundProfile: String? = "Loud Beep") {
        try {
            stopAll()
            
            if (soundProfile == "None" || soundProfile.isNullOrEmpty()) {
                return
            }
            
            val sampleUri: Uri = when (soundProfile) {
                "Victory Gong" -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                "Gentle Chime" -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                else -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            } ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
              ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, sampleUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK)
                prepare()
                android.util.Log.d("AlertManager", "MediaPlayer start() called at ${System.currentTimeMillis()}")
                start()
            }
            vibrateAlert()
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to older RingtoneManager if MediaPlayer fails on some specific resource Uris
            try {
                val sampleUri: Uri = when (soundProfile) {
                    "Victory Gong" -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                    "Gentle Chime" -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    else -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                } ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                
                ringtone = RingtoneManager.getRingtone(context, sampleUri)
                ringtone?.audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                ringtone?.play()
                android.util.Log.d("AlertManager", "Ringtone play() called at ${System.currentTimeMillis()}")
                vibrateAlert()
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    fun vibrateAlert() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                val vibrator = vibratorManager?.defaultVibrator
                vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 450, 200, 450, 200, 600), -1))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 450, 200, 450, 200, 600), -1))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopAll() {
        android.util.Log.d("AlertManager", "stopAll() called at ${System.currentTimeMillis()}")
        try {
            ringtone?.stop()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        try {
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                mediaPlayer?.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            mediaPlayer = null
        }
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.cancel()
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.cancel()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
