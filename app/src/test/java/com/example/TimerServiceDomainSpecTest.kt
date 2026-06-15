package com.example

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.Robolectric
import org.robolectric.shadows.ShadowLog
import com.example.data.*
import com.example.notification.TimerService
import com.example.notification.TimerServiceState
import com.example.viewmodel.TrainingState

/**
 * Executable product spec for the TimerService domain model.
 * This verifies the expected behavior when a stage expires and manual action is taken.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TimerServiceDomainSpecTest {

  @Test
  fun `given a routine with multiple stages, when stage expires, it waits for user decision and can transition to next stage`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    
    // 1. Arrange: Setup domain model expecting a multiple-stage routine
    val routine = RoutineWithStages(
        routine = Routine(id = 1, name = "Test Routine", description = "Test Spec", autoRepeat = false),
        stages = listOf(
            Stage(
                id = 101,
                stageOrder = 1,
                routineId = 1,
                name = "Stage 1",
                durationSeconds = 10,
                instruction = "Do this",
                soundProfileStart = "Beep",
                soundProfileEnd = "Beep",
                requiresManualProceed = true // Wait for user decision
            ),
            Stage(
                id = 102,
                stageOrder = 2,
                routineId = 1,
                name = "Stage 2",
                durationSeconds = 10,
                instruction = "Do that",
                soundProfileStart = "None",
                soundProfileEnd = "Beep",
                requiresManualProceed = false
            )
        )
    )

    TimerServiceState.activeRoutine.value = routine
    
    val service = Robolectric.buildService(TimerService::class.java).create()

    // 2. Act: Start the routine
    val startIntent = Intent(context, TimerService::class.java).apply { action = TimerService.ACTION_START }
    service.get().onStartCommand(startIntent, 0, 1)

    // Assert: State should be RUNNING for stage 1
    assertEquals(TrainingState.RUNNING, TimerServiceState.trainingState.value)
    assertEquals(0, TimerServiceState.currentStageIndex.value)
    
    val alertLogsAfterStart = ShadowLog.getLogsForTag("AlertManager").map { it.msg }
    if (!alertLogsAfterStart.any { it.contains("MediaPlayer start() called") || it.contains("Ringtone play() called") }) {
        throw AssertionError("Alarm start sound should have played. Found logs: $alertLogsAfterStart")
    }
    ShadowLog.clear()

    // 3. Act: Simulate time expiration for the current stage (timer firing)
    val expireIntent = Intent(context, TimerService::class.java).apply { action = TimerService.ACTION_STAGE_EXPIRED }
    service.get().onStartCommand(expireIntent, 0, 2)

    // Assert: App transitions to EXPIRED_WAITING state
    // At this point, the alarm sound fires (AlertManager.playLoudAlert is called)
    assertEquals(TrainingState.EXPIRED_WAITING, TimerServiceState.trainingState.value)
    
    val alertLogsAfterExpire = ShadowLog.getLogsForTag("AlertManager").map { it.msg }
    if (!alertLogsAfterExpire.any { it.contains("MediaPlayer start() called") || it.contains("Ringtone play() called") }) {
        throw AssertionError("Alarm end sound should have started when stage expires. Found logs: $alertLogsAfterExpire")
    }
    ShadowLog.clear()
    
    // 4. Act: User decides to continue to the next stage
    val nextIntent = Intent(context, TimerService::class.java).apply { action = TimerService.ACTION_NEXT }
    service.get().onStartCommand(nextIntent, 0, 3)

    // Assert: App transitions to RUNNING state for the next stage (Stage 2)
    // The previously playing alarm sound is stopped via AlertManager.stopAll() in handleNext()
    assertEquals(TrainingState.RUNNING, TimerServiceState.trainingState.value)
    assertEquals(1, TimerServiceState.currentStageIndex.value)
    
    val alertLogsAfterNext = ShadowLog.getLogsForTag("AlertManager").map { it.msg }
    assertTrue("Alarm should be stopped when transitioning to the next stage", alertLogsAfterNext.any { it.contains("stopAll() called") })
    
    // 5. Cleanup
    service.destroy()
  }
}
