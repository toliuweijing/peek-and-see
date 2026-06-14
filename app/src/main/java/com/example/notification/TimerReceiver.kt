package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log

class TimerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d("TimerReceiver", "Alarm broadcast received at ${System.currentTimeMillis()}")
        Log.d("TimerReceiver", "Alarm broadcast received with action: $action")
        
        if (action == TimerService.ACTION_STAGE_EXPIRED) {
            // Acquire a temporary partial wake lock for 5 seconds to ensure the service executes.
            var wakeLock: PowerManager.WakeLock? = null
            try {
                val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                wakeLock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "LazyEyeTimer:ReceiverWakeLock")
                wakeLock?.acquire(5000L) // Safe auto-release in 5 seconds
                Log.d("TimerReceiver", "Receiver transient wake lock acquired")
            } catch (e: Exception) {
                Log.e("TimerReceiver", "Failed to acquire receiver wake lock: ${e.message}")
            }

            val serviceIntent = Intent(context, TimerService::class.java).apply {
                this.action = TimerService.ACTION_STAGE_EXPIRED
            }
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            } catch (e: Exception) {
                Log.e("TimerReceiver", "Failed to start TimerService upon alarm: ${e.message}", e)
                try {
                    wakeLock?.let {
                        if (it.isHeld) {
                            it.release()
                        }
                    }
                } catch (ex: Exception) {
                    // ignore
                }
            }
        }
    }
}
