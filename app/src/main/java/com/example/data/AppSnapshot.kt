package com.example.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AppSnapshot(
    val routines: List<Routine>,
    val stages: List<Stage>,
    val sessions: List<Session>,
    val stageRecords: List<StageRecord>,
    val profiles: List<PatientProfile>
)
