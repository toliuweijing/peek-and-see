package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class AppRepository(private val appDao: AppDao) {

    val allRoutinesWithStages: Flow<List<RoutineWithStages>> = appDao.getRoutinesWithStages()
    val allSessions: Flow<List<Session>> = appDao.getAllSessions()

    fun getRoutineWithStages(id: Long): Flow<RoutineWithStages?> {
        return appDao.getRoutineWithStages(id)
    }

    suspend fun saveRoutineWithStages(routine: Routine, stages: List<Stage>): Long {
        return appDao.saveRoutineWithStages(routine, stages)
    }

    suspend fun deleteRoutine(routineId: Long) {
        appDao.deleteRoutineAndStages(routineId)
    }

    suspend fun insertSession(session: Session): Long {
        return appDao.insertSession(session)
    }

    suspend fun updateSession(session: Session) {
        appDao.updateSession(session)
    }

    suspend fun getSessionById(id: Long): Session? {
        return appDao.getSessionById(id)
    }

    suspend fun insertStageRecord(record: StageRecord) {
        appDao.insertStageRecord(record)
    }

    fun getStageRecordsForSession(sessionId: Long): Flow<List<StageRecord>> {
        return appDao.getStageRecordsForSession(sessionId)
    }

    suspend fun getStageRecordsForSessionSync(sessionId: Long): List<StageRecord> {
        return appDao.getStageRecordsForSessionSync(sessionId)
    }

    suspend fun prepopulateDefaultRoutinesIfNeeded() {
        val existing = allRoutinesWithStages.first()
        if (existing.isEmpty()) {
            // Let's create the clinic-recommended templates securely
            val patchRoutineId = appDao.insertRoutine(
                Routine(
                    name = "Patch Training Routine",
                    description = "[Example] A multi-stage clinic-type routine for patching therapy paired with active near-visual task exercises."
                )
            )
            appDao.insertStages(
                listOf(
                    Stage(
                        routineId = patchRoutineId,
                        stageOrder = 1,
                        name = "Prepare & Pitch",
                        durationSeconds = 10, // short for test/onboarding, wait logic applies
                        instruction = "Safely cover the stronger helper eye with your prescribed patch. Confirm when ready.",
                        requiresManualProceed = true,
                        soundProfile = "Gentle Chime"
                    ),
                    Stage(
                        routineId = patchRoutineId,
                        stageOrder = 2,
                        name = "Near Work (Reading/Drawing)",
                        durationSeconds = 1200, // 20 minutes
                        instruction = "Keep the patch on. Engage in near visual activities, e.g., reading a book, coloring, or tracing shapes.",
                        requiresManualProceed = true,
                        soundProfile = "Loud Beep"
                    ),
                    Stage(
                        routineId = patchRoutineId,
                        stageOrder = 3,
                        name = "Visual Break / Blink Rest",
                        durationSeconds = 300, // 5 minutes
                        instruction = "Give the active lazy eye a gentle rest. Look at distant scenery, and close/blink eyes soft and slow.",
                        requiresManualProceed = true,
                        soundProfile = "Loud Beep"
                    ),
                    Stage(
                        routineId = patchRoutineId,
                        stageOrder = 4,
                        name = "Active Tracking (Near Focus)",
                        durationSeconds = 900, // 15 minutes
                        instruction = "Perform detailed visual focus drills (solving puzzles, maze tracing, or playing tablet target-focus games).",
                        requiresManualProceed = true,
                        soundProfile = "Loud Beep"
                    ),
                    Stage(
                        routineId = patchRoutineId,
                        stageOrder = 5,
                        name = "Complete Session & Remove Patch",
                        durationSeconds = 10,
                        instruction = "Excellent effort! You can now safely remove the patch from the stronger eye.",
                        requiresManualProceed = true,
                        soundProfile = "Victory Gong"
                    )
                )
            )

            // Let's create an onboarding test routine so they can test permissions/alerts immediately
            val testRoutineId = appDao.insertRoutine(
                Routine(
                    name = "Onboarding Demo & Alarm Test",
                    description = "[Test] A rapid 30-second multi-stage timer to test permission alerts, loud sounds, and the manual gating experience safely."
                )
            )
            appDao.insertStages(
                listOf(
                    Stage(
                        routineId = testRoutineId,
                        stageOrder = 1,
                        name = "Quick Eye Exercise Intro",
                        durationSeconds = 10,
                        instruction = "Test stage 1. Get ready to experience the countdown and manual continuation gate.",
                        requiresManualProceed = true,
                        soundProfile = "Gentle Chime"
                    ),
                    Stage(
                        routineId = testRoutineId,
                        stageOrder = 2,
                        name = "Demo Near Drill",
                        durationSeconds = 12,
                        instruction = "Test stage 2. Let the timer drain to trigger a loud notification sound. The alarm won't advance until you tap button.",
                        requiresManualProceed = true,
                        soundProfile = "Loud Beep"
                    ),
                    Stage(
                        routineId = testRoutineId,
                        stageOrder = 3,
                        name = "Wrap Up & Done",
                        durationSeconds = 8,
                        instruction = "Test stage 3. The demo routine will complete and save a record to local history.",
                        requiresManualProceed = true,
                        soundProfile = "Victory Gong"
                    )
                )
            )

            // Let's create the 20-20-20 Eye Strain Routine
            val ruleRoutineId = appDao.insertRoutine(
                Routine(
                    name = "20-20-20 Eye Stretch Rest",
                    description = "[Example] Reduce digital eye strain. Look away from screens at distance of 20+ feet for 20 seconds, every 20 minutes."
                )
            )
            appDao.insertStages(
                listOf(
                    Stage(
                        routineId = ruleRoutineId,
                        stageOrder = 1,
                        name = "Distant Focus Shift",
                        durationSeconds = 20,
                        instruction = "Set your gaze on an object at least 20 feet (6 meters) away. Let your focus adjust gently.",
                        requiresManualProceed = true,
                        soundProfile = "Gentle Chime"
                    ),
                    Stage(
                        routineId = ruleRoutineId,
                        stageOrder = 2,
                        name = "Deep Blink Hydration",
                        durationSeconds = 15,
                        instruction = "Blink slowly and gently several times to rebuild tear film humidity and release eye lid stiffness.",
                        requiresManualProceed = true,
                        soundProfile = "Loud Beep"
                    )
                )
            )
        }
    }
}
