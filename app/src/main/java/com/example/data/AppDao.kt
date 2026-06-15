package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    // --- ROUTINES ---
    @Transaction
    @Query("SELECT * FROM routines ORDER BY createdAt DESC")
    fun getRoutinesWithStages(): Flow<List<RoutineWithStages>>

    @Query("SELECT * FROM routines")
    suspend fun getAllRoutinesSync(): List<Routine>

    @Query("SELECT * FROM stages")
    suspend fun getAllStagesSync(): List<Stage>


    @Transaction
    @Query("SELECT * FROM routines WHERE id = :id LIMIT 1")
    fun getRoutineWithStages(id: Long): Flow<RoutineWithStages?>

    @Query("SELECT * FROM routines WHERE id = :id LIMIT 1")
    suspend fun getRoutineById(id: Long): Routine?

    @Query("SELECT * FROM stages WHERE routineId = :routineId ORDER BY stageOrder ASC")
    fun getStagesForRoutine(routineId: Long): Flow<List<Stage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutine(routine: Routine): Long

    @Update
    suspend fun updateRoutine(routine: Routine)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStage(stage: Stage): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStages(stages: List<Stage>)

    @Query("DELETE FROM routines")
    suspend fun deleteAllRoutines()

    @Query("DELETE FROM stages")
    suspend fun deleteAllStages()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutinesSync(routines: List<Routine>)


    @Query("DELETE FROM stages WHERE routineId = :routineId")
    suspend fun deleteStagesForRoutine(routineId: Long)

    @Query("DELETE FROM routines WHERE id = :id")
    suspend fun deleteRoutineById(id: Long)

    @Transaction
    suspend fun saveRoutineWithStages(routine: Routine, stages: List<Stage>): Long {
        val routineId = if (routine.id == 0L) {
            insertRoutine(routine)
        } else {
            updateRoutine(routine)
            deleteStagesForRoutine(routine.id)
            routine.id
        }
        val stagesToInsert = stages.map { it.copy(routineId = routineId) }
        insertStages(stagesToInsert)
        return routineId
    }

    @Transaction
    suspend fun deleteRoutineAndStages(routineId: Long) {
        deleteStagesForRoutine(routineId)
        deleteRoutineById(routineId)
    }

    // --- SESSIONS ---
    @Query("SELECT * FROM sessions ORDER BY startedAt DESC")
    fun getAllSessions(): Flow<List<Session>>

    @Query("SELECT * FROM sessions")
    suspend fun getAllSessionsSync(): List<Session>


    @Query("SELECT * FROM sessions WHERE id = :id LIMIT 1")
    suspend fun getSessionById(id: Long): Session?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: Session): Long

    @Update
    suspend fun updateSession(session: Session)

    @Delete
    suspend fun deleteSession(session: Session)

    @Query("DELETE FROM sessions WHERE id = :id")
    suspend fun deleteSessionById(id: Long)

    @Query("DELETE FROM stage_records WHERE sessionId = :sessionId")
    suspend fun deleteStageRecordsBySessionId(sessionId: Long)

    @Query("DELETE FROM sessions")
    suspend fun deleteAllSessions()

    @Query("DELETE FROM stage_records")
    suspend fun deleteAllStageRecords()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessionsSync(sessions: List<Session>)

    // --- STAGE RECORDS ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStageRecord(record: StageRecord)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStageRecordsSync(records: List<StageRecord>)

    @Query("SELECT * FROM stage_records")
    suspend fun getAllStageRecordsSync(): List<StageRecord>


    @Query("SELECT * FROM stage_records WHERE sessionId = :sessionId ORDER BY stageOrder ASC")
    fun getStageRecordsForSession(sessionId: Long): Flow<List<StageRecord>>

    @Query("SELECT * FROM stage_records WHERE sessionId = :sessionId ORDER BY stageOrder ASC")
    suspend fun getStageRecordsForSessionSync(sessionId: Long): List<StageRecord>
}
