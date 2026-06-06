package com.example.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "routines")
data class Routine(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "stages")
data class Stage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routineId: Long,
    val stageOrder: Int,
    val name: String,
    val durationSeconds: Int,
    val instruction: String,
    val requiresManualProceed: Boolean = true,
    val soundProfile: String = "Loud Beep"
)

@Entity(tableName = "sessions")
data class Session(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routineId: Long,
    val routineName: String,
    val startedAt: Long,
    val endedAt: Long,
    val status: String, // "Completed", "Abandoned", "In-Progress"
    val plannedSeconds: Int,
    val recordedSeconds: Int,
    val completionPercent: Int,
    val notes: String = ""
)

@Entity(tableName = "stage_records")
data class StageRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val stageName: String,
    val durationSeconds: Int,
    val recordedSeconds: Int, // how long actually spent
    val status: String, // "Completed", "Skipped", "Abandoned"
    val stageOrder: Int
)

data class RoutineWithStages(
    @Embedded val routine: Routine,
    @Relation(
        parentColumn = "id",
        entityColumn = "routineId"
    )
    val stages: List<Stage>
) {
    val sortedStages: List<Stage>
        get() = stages.sortedBy { it.stageOrder }
}
