package com.example.notification

import android.app.AlarmManager
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import com.example.alert.AlertManager
import com.example.data.*
import com.example.viewmodel.TrainingState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow

object TimerServiceState {
    val trainingState = MutableStateFlow(TrainingState.IDLE)
    val activeRoutine = MutableStateFlow<RoutineWithStages?>(null)
    val currentStageIndex = MutableStateFlow(0)
    val currentStageRemainingSeconds = MutableStateFlow(0)
    val currentStageProgress = MutableStateFlow(1.0f)
    val completedSessionSummary = MutableStateFlow<Session?>(null)
    val completedStageRecords = MutableStateFlow<List<StageRecord>>(emptyList())
    val testAlarmStatus = MutableStateFlow<String>("NOT_STARTED") // "NOT_STARTED", "PENDING", "SUCCESS", "FAILED"
}

class TimerService : Service() {
    private lateinit var alertManager: AlertManager
    private lateinit var notificationHelper: NotificationHelper
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var tickerJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null

    // Analytics / Tracker variables
    private var sessionStartTime: Long = 0
    private var currentStageStartTime: Long = 0
    private var targetStageEndTime: Long = 0
    private var pausedRemainingSeconds: Int = 0
    private var plannedTotalSeconds: Int = 0
    private var actualAccumulatedSeconds: Int = 0
    private var pausesCount: Int = 0
    private var completedStagesCount: Int = 0
    private val temporaryStageRecords = mutableListOf<StageRecord>()
    private var lastNotifiedSeconds: Int = -1

    companion object {
        const val ACTION_START = "com.example.lazyeyetimer.START"
        const val ACTION_PAUSE = "com.example.lazyeyetimer.PAUSE"
        const val ACTION_RESUME = "com.example.lazyeyetimer.RESUME"
        const val ACTION_NEXT = "com.example.lazyeyetimer.NEXT"
        const val ACTION_SKIP = "com.example.lazyeyetimer.SKIP"
        const val ACTION_STOP_ALARM = "com.example.lazyeyetimer.STOP_ALARM"
        const val ACTION_END_EARLY = "com.example.lazyeyetimer.END_EARLY"
        const val ACTION_REPEAT = "com.example.lazyeyetimer.REPEAT"
        const val ACTION_STAGE_EXPIRED = "com.example.lazyeyetimer.STAGE_EXPIRED"
        const val ACTION_TEST_ALARM = "com.example.lazyeyetimer.TEST_ALARM"
        
        fun startService(context: Context, routine: RoutineWithStages) {
            TimerServiceState.activeRoutine.value = routine
            
            val intent = Intent(context, TimerService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        alertManager = AlertManager(applicationContext)
        notificationHelper = NotificationHelper(applicationContext)
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "LazyEyeTimer:TrainingWakeLock")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // Ensure alarms don't keep ringing if the user force-closes the app by swiping it away during an alert
        alertManager.stopAll()
        if (TimerServiceState.trainingState.value == TrainingState.EXPIRED_WAITING) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        tickerJob?.cancel()
        serviceScope.cancel()
        cancelExpiryAlarm()
        alertManager.stopAll()
        notificationHelper.cancelNotification()
        if (wakeLock?.isHeld == true) {
            try {
                wakeLock?.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_TEST_ALARM) {
            android.util.Log.d("TimerService", "ACTION_TEST_ALARM received at ${System.currentTimeMillis()}")
            val testNotification = androidx.core.app.NotificationCompat.Builder(applicationContext, NotificationHelper.ALARM_CHANNEL_ID)
                .setContentTitle("Lock-screen Alarm Test")
                .setContentText("Test Successful! Alarm received correctly.")
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                .setCategory(androidx.core.app.NotificationCompat.CATEGORY_ALARM)
                .setVisibility(androidx.core.app.NotificationCompat.VISIBILITY_PUBLIC)
                .build()
            startForeground(NotificationHelper.NOTIFICATION_ID, testNotification)
            handleTestAlarm()
            return START_NOT_STICKY
        }

        if (action == ACTION_STAGE_EXPIRED) {
            android.util.Log.d("TimerService", "ACTION_STAGE_EXPIRED received at ${System.currentTimeMillis()}")
            
            val routine = TimerServiceState.activeRoutine.value
            if (routine == null) {
                // Safeguard against rare system-kill scenarios: create base placeholder to keep system happy, then self-terminate.
                val fallbackNotification = androidx.core.app.NotificationCompat.Builder(applicationContext, NotificationHelper.TIMER_CHANNEL_ID)
                    .setContentTitle("Lazy Eye Training")
                    .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                    .setContentText("Workout complete or inactive.")
                    .build()
                startForeground(NotificationHelper.NOTIFICATION_ID, fallbackNotification)
                stopSelf()
                return START_NOT_STICKY
            } else {
                startForegroundServiceProgress(isInitial = true)
            }
        }

        when (action) {
            ACTION_START -> handleStart()
            ACTION_PAUSE -> handlePause()
            ACTION_RESUME -> handleResume()
            ACTION_NEXT -> handleNext()
            ACTION_SKIP -> handleSkip()
            ACTION_STOP_ALARM -> handleStopAlarm()
            ACTION_REPEAT -> handleRepeat()
            ACTION_END_EARLY -> handleEndEarly()
            ACTION_STAGE_EXPIRED -> handleStageExpiredFromAlarm()
        }
        return START_NOT_STICKY
    }

    private fun startStageRunning(stage: Stage) {
        alertManager.stopAll()
        TimerServiceState.trainingState.value = TrainingState.RUNNING
        lastNotifiedSeconds = -1
        currentStageStartTime = System.currentTimeMillis()
        targetStageEndTime = currentStageStartTime + (stage.durationSeconds * 1000L)
        TimerServiceState.currentStageRemainingSeconds.value = stage.durationSeconds
        TimerServiceState.currentStageProgress.value = 1.0f
        
        scheduleExpiryAlarm(targetStageEndTime)
        
        alertManager.playLoudAlert(stage.soundProfileStart)
    }

    private fun handleStart() {
        val routine = TimerServiceState.activeRoutine.value ?: return
        
        if (wakeLock?.isHeld != true) {
            try {
                wakeLock?.acquire(2 * 60 * 60 * 1000L) // limit of 2 hours, safe fallback
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        alertManager.stopAll()
        notificationHelper.cancelNotification()
        
        TimerServiceState.completedStageRecords.value = emptyList()
        TimerServiceState.completedSessionSummary.value = null
        temporaryStageRecords.clear()
        
        TimerServiceState.currentStageIndex.value = 0
        sessionStartTime = System.currentTimeMillis()
        
        val firstStage = routine.sortedStages.firstOrNull() ?: return
        startStageRunning(firstStage)
        
        plannedTotalSeconds = routine.stages.sumOf { it.durationSeconds }
        actualAccumulatedSeconds = 0
        pausesCount = 0
        completedStagesCount = 0
        
        startForegroundServiceProgress(isInitial = true)
        startTicker()
    }

    private fun startForegroundServiceProgress(isInitial: Boolean) {
        val routine = TimerServiceState.activeRoutine.value ?: return
        val currentStageIndexVal = TimerServiceState.currentStageIndex.value
        val stage = routine.sortedStages.getOrNull(currentStageIndexVal) ?: return
        val remainingSecs = TimerServiceState.currentStageRemainingSeconds.value
        
        if (!isInitial && remainingSecs == lastNotifiedSeconds && TimerServiceState.trainingState.value == TrainingState.RUNNING) {
            return
        }
        lastNotifiedSeconds = remainingSecs
        
        val minutes = remainingSecs / 60
        val seconds = remainingSecs % 60
        val timeLeftFormatted = String.format("%02d:%02d", minutes, seconds)
        val nextStageExists = (currentStageIndexVal + 1) < routine.stages.size || routine.routine.autoRepeat
        
        val notification = notificationHelper.buildTimerNotification(
            routineName = routine.routine.name,
            stageName = "Stage ${currentStageIndexVal + 1}/${routine.stages.size}: ${stage.name}",
            timeLeftFormatted = timeLeftFormatted,
            state = TimerServiceState.trainingState.value,
            nextStageExists = nextStageExists
        )
        
        if (isInitial && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NotificationHelper.NOTIFICATION_ID, 
                notification, 
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            if (isInitial) {
                startForeground(NotificationHelper.NOTIFICATION_ID, notification)
            } else {
                notificationHelper.updateNotification(notification)
            }
        }
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = serviceScope.launch {
            while (true) {
                if (TimerServiceState.trainingState.value == TrainingState.RUNNING) {
                    val now = System.currentTimeMillis()
                    val target = targetStageEndTime
                    val remainingMillis = target - now
                    val remainingSecs = (remainingMillis + 999) / 1000
                    
                    if (remainingMillis <= 0) {
                        onStageExpired()
                    } else {
                        TimerServiceState.currentStageRemainingSeconds.value = remainingSecs.toInt()
                        val currentStageTotal = getActiveStageSeconds()
                        if (currentStageTotal > 0) {
                            TimerServiceState.currentStageProgress.value = remainingMillis.toFloat() / (currentStageTotal * 1000f)
                        }
                        startForegroundServiceProgress(isInitial = false)
                    }
                }
                delay(250)
            }
        }
    }

    private fun getActiveStageSeconds(): Int {
        val routine = TimerServiceState.activeRoutine.value ?: return 0
        val stage = routine.sortedStages.getOrNull(TimerServiceState.currentStageIndex.value) ?: return 0
        return stage.durationSeconds
    }

    private fun onStageExpired() {
        if (TimerServiceState.trainingState.value != TrainingState.RUNNING) return
        
        // Prevent concurrent/re-entrant triggers from ticker while processing expiration
        TimerServiceState.trainingState.value = TrainingState.EXPIRED_WAITING
        cancelExpiryAlarm()
        
        val routine = TimerServiceState.activeRoutine.value ?: return
        val currentStageIndexVal = TimerServiceState.currentStageIndex.value
        val stage = routine.sortedStages.getOrNull(currentStageIndexVal) ?: return
        
        val planSecs = getActiveStageSeconds()
        actualAccumulatedSeconds += planSecs
        
        temporaryStageRecords.add(
            StageRecord(
                sessionId = 0,
                stageName = stage.name,
                durationSeconds = stage.durationSeconds,
                recordedSeconds = stage.durationSeconds,
                status = "Completed",
                stageOrder = stage.stageOrder
            )
        )
        completedStagesCount++
        
        // Play sound when ending active stage
        alertManager.playLoudAlert(stage.soundProfileEnd)
        
        if (stage.requiresManualProceed) {
            TimerServiceState.currentStageRemainingSeconds.value = 0
            TimerServiceState.currentStageProgress.value = 0f

            // Post stage expired notification
            val nextStageExists = (currentStageIndexVal + 1) < routine.stages.size || routine.routine.autoRepeat
            val expiredNotification = notificationHelper.buildTimerNotification(
                routineName = routine.routine.name,
                stageName = "Completed: ${stage.name}",
                timeLeftFormatted = "Time Expired",
                state = TrainingState.EXPIRED_WAITING,
                nextStageExists = nextStageExists
            )
            notificationHelper.updateNotification(expiredNotification)
        } else {
            // Safe, immediate auto-advance dispatched to the Main loop
            serviceScope.launch(Dispatchers.Main) {
                handleNext()
            }
        }
    }

    private fun handlePause() {
        if (TimerServiceState.trainingState.value != TrainingState.RUNNING) return
        TimerServiceState.trainingState.value = TrainingState.PAUSED
        pausesCount++
        
        cancelExpiryAlarm()
        
        val now = System.currentTimeMillis()
        pausedRemainingSeconds = ((targetStageEndTime - now) / 1000).toInt().coerceAtLeast(0)
        startForegroundServiceProgress(isInitial = false)
    }

    private fun handleResume() {
        if (TimerServiceState.trainingState.value != TrainingState.PAUSED) return
        TimerServiceState.trainingState.value = TrainingState.RUNNING
        lastNotifiedSeconds = -1
        
        if (wakeLock?.isHeld != true) {
            try {
                wakeLock?.acquire(2 * 60 * 60 * 1000L)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        targetStageEndTime = System.currentTimeMillis() + (pausedRemainingSeconds * 1000L)
        scheduleExpiryAlarm(targetStageEndTime)
        startForegroundServiceProgress(isInitial = false)
    }

    private fun handleStopAlarm() {
        alertManager.stopAll()
        startForegroundServiceProgress(isInitial = false)
    }

    private fun handleRepeat() {
        alertManager.stopAll()
        val routine = TimerServiceState.activeRoutine.value ?: return
        val currentIdx = TimerServiceState.currentStageIndex.value
        val stage = routine.sortedStages.getOrNull(currentIdx) ?: return
        
        startStageRunning(stage)
        startForegroundServiceProgress(isInitial = false)
    }

    private fun handleNext() {
        alertManager.stopAll()
        val routine = TimerServiceState.activeRoutine.value ?: return
        val nextIdx = TimerServiceState.currentStageIndex.value + 1
        
        if (nextIdx < routine.sortedStages.size) {
            TimerServiceState.currentStageIndex.value = nextIdx
            val nextStage = routine.sortedStages[nextIdx]
            startStageRunning(nextStage)
            startForegroundServiceProgress(isInitial = false)
        } else {
            if (routine.routine.autoRepeat && routine.sortedStages.isNotEmpty()) {
                TimerServiceState.currentStageIndex.value = 0
                val firstStage = routine.sortedStages[0]
                startStageRunning(firstStage)
                startForegroundServiceProgress(isInitial = false)
            } else {
                finishSession()
            }
        }
    }

    private fun handleSkip() {
        alertManager.stopAll()
        val routine = TimerServiceState.activeRoutine.value ?: return
        val currentIdx = TimerServiceState.currentStageIndex.value
        val stage = routine.sortedStages.getOrNull(currentIdx)
        
        if (stage != null) {
            temporaryStageRecords.add(
                StageRecord(
                    sessionId = 0,
                    stageName = stage.name,
                    durationSeconds = stage.durationSeconds,
                    recordedSeconds = 0,
                    status = "Skipped",
                    stageOrder = stage.stageOrder
                )
            )
        }
        
        val nextIdx = currentIdx + 1
        if (nextIdx < routine.sortedStages.size) {
            TimerServiceState.currentStageIndex.value = nextIdx
            val nextStage = routine.sortedStages[nextIdx]
            startStageRunning(nextStage)
            startForegroundServiceProgress(isInitial = false)
        } else {
            if (routine.routine.autoRepeat && routine.sortedStages.isNotEmpty()) {
                TimerServiceState.currentStageIndex.value = 0
                val firstStage = routine.sortedStages[0]
                startStageRunning(firstStage)
                startForegroundServiceProgress(isInitial = false)
            } else {
                finishSession()
            }
        }
    }

    private fun handleEndEarly() {
        alertManager.stopAll()
        tickerJob?.cancel()
        cancelExpiryAlarm()
        
        val routine = TimerServiceState.activeRoutine.value
        if (routine != null) {
            val now = System.currentTimeMillis()
            val currentStageIndexVal = TimerServiceState.currentStageIndex.value
            val stateVal = TimerServiceState.trainingState.value
            
            // 1. Calculate active stage's actual elapsed seconds if it was running or paused
            var activeElapsed = 0
            if (stateVal == TrainingState.RUNNING || stateVal == TrainingState.PAUSED) {
                val stage = routine.sortedStages.getOrNull(currentStageIndexVal)
                if (stage != null) {
                    activeElapsed = if (stateVal == TrainingState.RUNNING) {
                        ((System.currentTimeMillis() - currentStageStartTime) / 1000).toInt()
                            .coerceIn(0, stage.durationSeconds)
                    } else {
                        (stage.durationSeconds - pausedRemainingSeconds)
                            .coerceIn(0, stage.durationSeconds)
                    }
                }
            }
            
            val totalRecordedSeconds = actualAccumulatedSeconds + activeElapsed
            val finalPercent = if (plannedTotalSeconds > 0) {
                (totalRecordedSeconds * 100 / plannedTotalSeconds).coerceIn(0, 100)
            } else 0
            
            // 2. Build stage records for current and remaining stages
            for (idx in currentStageIndexVal until routine.sortedStages.size) {
                val stage = routine.sortedStages[idx]
                if (idx == currentStageIndexVal) {
                    if (stateVal == TrainingState.RUNNING || stateVal == TrainingState.PAUSED) {
                        temporaryStageRecords.add(
                            StageRecord(
                                sessionId = 0,
                                stageName = stage.name,
                                durationSeconds = stage.durationSeconds,
                                recordedSeconds = activeElapsed,
                                status = "Abandoned",
                                stageOrder = stage.stageOrder
                            )
                        )
                    }
                    // If stateVal was EXPIRED_WAITING, it was already added during onStageExpired()
                } else {
                    temporaryStageRecords.add(
                        StageRecord(
                            sessionId = 0,
                            stageName = stage.name,
                            durationSeconds = stage.durationSeconds,
                            recordedSeconds = 0,
                            status = "Abandoned",
                            stageOrder = stage.stageOrder
                        )
                    )
                }
            }

            val sessionReport = Session(
                routineId = routine.routine.id,
                routineName = routine.routine.name,
                startedAt = sessionStartTime,
                endedAt = now,
                status = "Abandoned",
                plannedSeconds = plannedTotalSeconds,
                recordedSeconds = totalRecordedSeconds,
                completionPercent = finalPercent,
                notes = "Session terminated early by user."
            )
            
            autoSaveSessionAndRecords(sessionReport, ArrayList(temporaryStageRecords))
        } else {
            TimerServiceState.trainingState.value = TrainingState.IDLE
            stopSelf()
        }
    }

    private fun finishSession() {
        alertManager.stopAll()
        tickerJob?.cancel()
        cancelExpiryAlarm()
        
        val routine = TimerServiceState.activeRoutine.value ?: return
        val now = System.currentTimeMillis()
        
        val percent = if (plannedTotalSeconds > 0) {
            (actualAccumulatedSeconds * 100 / plannedTotalSeconds).coerceAtMost(100)
        } else 100

        val summary = Session(
            routineId = routine.routine.id,
            routineName = routine.routine.name,
            startedAt = sessionStartTime,
            endedAt = now,
            status = if (percent >= 80) "Completed" else "Partial",
            plannedSeconds = plannedTotalSeconds,
            recordedSeconds = actualAccumulatedSeconds,
            completionPercent = percent,
            notes = "Completed $completedStagesCount/${routine.stages.size} tasks. Paused $pausesCount times."
        )

        autoSaveSessionAndRecords(summary, ArrayList(temporaryStageRecords))
    }

    private fun autoSaveSessionAndRecords(session: Session, records: List<StageRecord>) {
        serviceScope.launch {
            try {
                val db = AppDatabase.getDatabase(applicationContext)
                val repository = AppRepository(db.appDao())
                val sessionId = repository.insertSession(session)
                records.forEach { record ->
                    repository.insertStageRecord(record.copy(sessionId = sessionId))
                }
                
                val savedSession = session.copy(id = sessionId)
                val savedRecords = records.map { it.copy(sessionId = sessionId) }
                
                withContext(Dispatchers.Main) {
                    TimerServiceState.completedSessionSummary.value = savedSession
                    TimerServiceState.completedStageRecords.value = savedRecords
                    TimerServiceState.trainingState.value = TrainingState.COMPLETED
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    TimerServiceState.completedSessionSummary.value = session
                    TimerServiceState.completedStageRecords.value = records
                    TimerServiceState.trainingState.value = TrainingState.COMPLETED
                }
            } finally {
                withContext(Dispatchers.Main) {
                    stopSelf()
                }
            }
        }
    }

    private fun handleStageExpiredFromAlarm() {
        if (TimerServiceState.trainingState.value == TrainingState.RUNNING) {
            onStageExpired()
        }
    }

    private fun handleTestAlarm() {
        TimerServiceState.testAlarmStatus.value = "SUCCESS"
        alertManager.playLoudAlert("Loud Beep")
        serviceScope.launch {
            delay(4000L)
            alertManager.stopAll()
            stopSelf()
        }
    }

    private fun scheduleExpiryAlarm(timeInMillis: Long) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val canScheduleExact = alarmManager.canScheduleExactAlarms()
                android.util.Log.d("TimerService", "Preparing scheduleExpiryAlarm: canScheduleExactAlarms=$canScheduleExact")
            } catch (e: Exception) {
                android.util.Log.e("TimerService", "Error reading canScheduleExactAlarms", e)
            }
        }

        android.util.Log.d("TimerService", "Scheduling setAlarmClock at $timeInMillis")
        val intent = Intent(applicationContext, TimerReceiver::class.java).apply {
            action = ACTION_STAGE_EXPIRED
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getBroadcast(applicationContext, 8888, intent, flags)
        
        val showIntent = Intent(applicationContext, com.example.MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val showFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val showPendingIntent = PendingIntent.getActivity(applicationContext, 0, showIntent, showFlags)
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val alarmClockInfo = AlarmManager.AlarmClockInfo(timeInMillis, showPendingIntent)
                alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
                android.util.Log.d("TimerService", "setAlarmClock SUCCESS at ${System.currentTimeMillis()}")
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
                }
                android.util.Log.d("TimerService", "setExact SUCCESS completed")
            }
        } catch (e: SecurityException) {
            android.util.Log.e("TimerService", "Exact alarm scheduling FAILED with SecurityException. Permission revoked. Not silently degrading.", e)
            // We do not silently fall back. The UI should have warned or blocked the user.
        } catch (e: Exception) {
            android.util.Log.e("TimerService", "Error in scheduleExpiryAlarm", e)
        }
    }

    private fun canScheduleScheduleExactAlarmsHelper(alarmManager: AlarmManager): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    private fun cancelExpiryAlarm() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(applicationContext, TimerReceiver::class.java).apply {
            action = ACTION_STAGE_EXPIRED
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getBroadcast(applicationContext, 8888, intent, flags)
        try {
            alarmManager.cancel(pendingIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
