package com.example.notification

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
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
}

class TimerService : Service() {
    private lateinit var alertManager: AlertManager
    private lateinit var notificationHelper: NotificationHelper
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var tickerJob: Job? = null

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
    }

    override fun onDestroy() {
        tickerJob?.cancel()
        serviceScope.cancel()
        alertManager.stopAll()
        notificationHelper.cancelNotification()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> handleStart()
            ACTION_PAUSE -> handlePause()
            ACTION_RESUME -> handleResume()
            ACTION_NEXT -> handleNext()
            ACTION_SKIP -> handleSkip()
            ACTION_STOP_ALARM -> handleStopAlarm()
            ACTION_REPEAT -> handleRepeat()
            ACTION_END_EARLY -> handleEndEarly()
        }
        return START_NOT_STICKY
    }

    private fun handleStart() {
        val routine = TimerServiceState.activeRoutine.value ?: return
        
        alertManager.stopAll()
        notificationHelper.cancelNotification()
        
        TimerServiceState.completedStageRecords.value = emptyList()
        TimerServiceState.completedSessionSummary.value = null
        temporaryStageRecords.clear()
        
        TimerServiceState.currentStageIndex.value = 0
        TimerServiceState.trainingState.value = TrainingState.RUNNING
        
        sessionStartTime = System.currentTimeMillis()
        currentStageStartTime = sessionStartTime
        
        val stageSecs = routine.sortedStages.firstOrNull()?.durationSeconds ?: 0
        targetStageEndTime = sessionStartTime + (stageSecs * 1000L)
        
        TimerServiceState.currentStageRemainingSeconds.value = stageSecs
        TimerServiceState.currentStageProgress.value = 1.0f
        
        plannedTotalSeconds = routine.stages.sumOf { it.durationSeconds }
        actualAccumulatedSeconds = 0
        pausesCount = 0
        completedStagesCount = 0
        lastNotifiedSeconds = -1
        
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
        val nextStageExists = (currentStageIndexVal + 1) < routine.stages.size
        
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
        TimerServiceState.trainingState.value = TrainingState.EXPIRED_WAITING
        TimerServiceState.currentStageRemainingSeconds.value = 0
        TimerServiceState.currentStageProgress.value = 0f
        
        val planSecs = getActiveStageSeconds()
        actualAccumulatedSeconds += planSecs
        
        val routine = TimerServiceState.activeRoutine.value ?: return
        val currentStageIndexVal = TimerServiceState.currentStageIndex.value
        val stage = routine.sortedStages.getOrNull(currentStageIndexVal) ?: return
        
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
        
        alertManager.playLoudAlert(stage.soundProfile)
        
        // Post stage expired notification
        val nextStageExists = (currentStageIndexVal + 1) < routine.stages.size
        val expiredNotification = notificationHelper.buildTimerNotification(
            routineName = routine.routine.name,
            stageName = "Completed: ${stage.name}",
            timeLeftFormatted = "Time Expired",
            state = TrainingState.EXPIRED_WAITING,
            nextStageExists = nextStageExists
        )
        notificationHelper.updateNotification(expiredNotification)
    }

    private fun handlePause() {
        if (TimerServiceState.trainingState.value != TrainingState.RUNNING) return
        TimerServiceState.trainingState.value = TrainingState.PAUSED
        pausesCount++
        
        val now = System.currentTimeMillis()
        pausedRemainingSeconds = ((targetStageEndTime - now) / 1000).toInt().coerceAtLeast(0)
        startForegroundServiceProgress(isInitial = false)
    }

    private fun handleResume() {
        if (TimerServiceState.trainingState.value != TrainingState.PAUSED) return
        TimerServiceState.trainingState.value = TrainingState.RUNNING
        lastNotifiedSeconds = -1
        
        targetStageEndTime = System.currentTimeMillis() + (pausedRemainingSeconds * 1000L)
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
        
        TimerServiceState.trainingState.value = TrainingState.RUNNING
        lastNotifiedSeconds = -1
        targetStageEndTime = System.currentTimeMillis() + (stage.durationSeconds * 1000L)
        TimerServiceState.currentStageRemainingSeconds.value = stage.durationSeconds
        TimerServiceState.currentStageProgress.value = 1.0f
        currentStageStartTime = System.currentTimeMillis()
        
        startForegroundServiceProgress(isInitial = false)
    }

    private fun handleNext() {
        alertManager.stopAll()
        val routine = TimerServiceState.activeRoutine.value ?: return
        val nextIdx = TimerServiceState.currentStageIndex.value + 1
        
        if (nextIdx < routine.sortedStages.size) {
            TimerServiceState.currentStageIndex.value = nextIdx
            val nextStage = routine.sortedStages[nextIdx]
            
            TimerServiceState.trainingState.value = TrainingState.RUNNING
            lastNotifiedSeconds = -1
            targetStageEndTime = System.currentTimeMillis() + (nextStage.durationSeconds * 1000L)
            TimerServiceState.currentStageRemainingSeconds.value = nextStage.durationSeconds
            TimerServiceState.currentStageProgress.value = 1.0f
            currentStageStartTime = System.currentTimeMillis()
            
            startForegroundServiceProgress(isInitial = false)
        } else {
            finishSession()
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
            
            TimerServiceState.trainingState.value = TrainingState.RUNNING
            lastNotifiedSeconds = -1
            targetStageEndTime = System.currentTimeMillis() + (nextStage.durationSeconds * 1000L)
            TimerServiceState.currentStageRemainingSeconds.value = nextStage.durationSeconds
            TimerServiceState.currentStageProgress.value = 1.0f
            currentStageStartTime = System.currentTimeMillis()
            
            startForegroundServiceProgress(isInitial = false)
        } else {
            finishSession()
        }
    }

    private fun handleEndEarly() {
        alertManager.stopAll()
        tickerJob?.cancel()
        
        val routine = TimerServiceState.activeRoutine.value
        if (routine != null) {
            val now = System.currentTimeMillis()
            val finalPercent = if (plannedTotalSeconds > 0) {
                (actualAccumulatedSeconds * 100 / plannedTotalSeconds).coerceAtMost(100)
            } else 0
            
            for (idx in TimerServiceState.currentStageIndex.value until routine.sortedStages.size) {
                val stage = routine.sortedStages[idx]
                if (idx == TimerServiceState.currentStageIndex.value && TimerServiceState.trainingState.value == TrainingState.RUNNING) {
                    val elapsed = ((System.currentTimeMillis() - currentStageStartTime) / 1000).toInt()
                        .coerceAtMost(stage.durationSeconds)
                    temporaryStageRecords.add(
                        StageRecord(
                            sessionId = 0,
                            stageName = stage.name,
                            durationSeconds = stage.durationSeconds,
                            recordedSeconds = elapsed,
                            status = "Abandoned",
                            stageOrder = stage.stageOrder
                        )
                    )
                } else if (idx > TimerServiceState.currentStageIndex.value) {
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
                recordedSeconds = actualAccumulatedSeconds,
                completionPercent = finalPercent,
                notes = "Session terminated early by user."
            )
            
            TimerServiceState.completedSessionSummary.value = sessionReport
            TimerServiceState.completedStageRecords.value = ArrayList(temporaryStageRecords)
            TimerServiceState.trainingState.value = TrainingState.COMPLETED
        } else {
            TimerServiceState.trainingState.value = TrainingState.IDLE
        }
        
        stopSelf()
    }

    private fun finishSession() {
        alertManager.stopAll()
        tickerJob?.cancel()
        
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

        TimerServiceState.completedSessionSummary.value = summary
        TimerServiceState.completedStageRecords.value = ArrayList(temporaryStageRecords)
        TimerServiceState.trainingState.value = TrainingState.COMPLETED
        
        stopSelf()
    }
}
