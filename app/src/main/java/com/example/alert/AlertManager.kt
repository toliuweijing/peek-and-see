package com.example.alert

import android.content.Context
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class AlertManager(private val context: Context) {
    private var ringtone: Ringtone? = null

    fun playLoudAlert(soundProfile: String? = "Loud Beep") {
        try {
            stopAll()
            
            val sampleUri: Uri = when (soundProfile) {
                "Victory Gong" -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                "Gentle Chime" -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                else -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            } ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
              ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            ringtone = RingtoneManager.getRingtone(context, sampleUri)
            ringtone?.audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            
            ringtone?.play()
            vibrateAlert()
        } catch (e: Exception) {
            e.printStackTrace()
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
        try {
            ringtone?.stop()
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
