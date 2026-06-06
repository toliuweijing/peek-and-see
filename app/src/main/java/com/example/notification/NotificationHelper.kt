package com.example.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.viewmodel.TrainingState

class NotificationHelper(private val context: Context) {
    companion object {
        const val TIMER_CHANNEL_ID = "training_timer_channel"
        const val ALARM_CHANNEL_ID = "training_alarm_channel"
        const val NOTIFICATION_ID = 1001
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Timer Active Notification Channel (Low importance so it doesn't noisily beep every second)
            val timerChannel = NotificationChannel(
                TIMER_CHANNEL_ID,
                "Active Training Timer",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live remaining duration during eye exercises."
                setShowBadge(false)
            }
            manager.createNotificationChannel(timerChannel)

            // Stage Complete Alarm Channel (Loud! High importance)
            val alarmChannel = NotificationChannel(
                ALARM_CHANNEL_ID,
                "Stage Expiry Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Loud alerts and vibrations when a training stage finishes."
                enableVibration(true)
                enableLights(true)
            }
            manager.createNotificationChannel(alarmChannel)
        }
    }

    private fun getServicePendingIntent(action: String, requestCode: Int): PendingIntent {
        val intent = Intent().setClassName(context.packageName, "com.example.notification.TimerService").apply {
            this.action = action
        }
        return PendingIntent.getService(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun buildTimerNotification(
        routineName: String,
        stageName: String,
        timeLeftFormatted: String,
        state: TrainingState,
        nextStageExists: Boolean
    ): Notification {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 100, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = if (state == TrainingState.EXPIRED_WAITING) ALARM_CHANNEL_ID else TIMER_CHANNEL_ID
        val priority = if (state == TrainingState.EXPIRED_WAITING) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_LOW
        val category = if (state == TrainingState.EXPIRED_WAITING) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_PROGRESS

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(if (state == TrainingState.EXPIRED_WAITING) android.R.drawable.ic_dialog_alert else android.R.drawable.ic_media_play)
            .setContentTitle(routineName)
            .setContentText("$stageName — $timeLeftFormatted")
            .setOngoing(state != TrainingState.EXPIRED_WAITING)
            .setContentIntent(pendingIntent)
            .setPriority(priority)
            .setCategory(category)
            .setAutoCancel(false)

        if (state == TrainingState.EXPIRED_WAITING) {
            builder.setDefaults(NotificationCompat.DEFAULT_ALL)
            builder.setFullScreenIntent(pendingIntent, true)
        }

        // Add actions depending on state
        when (state) {
            TrainingState.RUNNING -> {
                val pausePI = getServicePendingIntent("com.example.lazyeyetimer.PAUSE", 1)
                val skipPI = getServicePendingIntent("com.example.lazyeyetimer.SKIP", 2)
                val stopEarlyPI = getServicePendingIntent("com.example.lazyeyetimer.END_EARLY", 3)
                
                builder.addAction(android.R.drawable.ic_media_pause, "Pause", pausePI)
                builder.addAction(android.R.drawable.ic_media_next, "Skip", skipPI)
                builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Quit", stopEarlyPI)
            }
            TrainingState.PAUSED -> {
                val resumePI = getServicePendingIntent("com.example.lazyeyetimer.RESUME", 4)
                val skipPI = getServicePendingIntent("com.example.lazyeyetimer.SKIP", 5)
                val stopEarlyPI = getServicePendingIntent("com.example.lazyeyetimer.END_EARLY", 6)

                builder.addAction(android.R.drawable.ic_media_play, "Resume", resumePI)
                builder.addAction(android.R.drawable.ic_media_next, "Skip", skipPI)
                builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Quit", stopEarlyPI)
            }
            TrainingState.EXPIRED_WAITING -> {
                val stopAlarmPI = getServicePendingIntent("com.example.lazyeyetimer.STOP_ALARM", 7)
                val repeatPI = getServicePendingIntent("com.example.lazyeyetimer.REPEAT", 8)
                val nextPI = getServicePendingIntent("com.example.lazyeyetimer.NEXT", 9)

                builder.addAction(android.R.drawable.ic_lock_silent_mode, "Mute", stopAlarmPI)
                builder.addAction(android.R.drawable.ic_menu_revert, "Repeat", repeatPI)
                builder.addAction(
                    android.R.drawable.ic_media_next,
                    if (nextStageExists) "Continue" else "Save Summary",
                    nextPI
                )
            }
            else -> {}
        }

        return builder.build()
    }

    fun updateNotification(notification: Notification) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    fun cancelNotification() {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(NOTIFICATION_ID)
    }
}
