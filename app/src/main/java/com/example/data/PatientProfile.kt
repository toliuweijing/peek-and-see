package com.example.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PatientProfile(
    val id: String,
    val name: String,
    val age: Int,
    val sex: String,
    val soundPref: String,
    val eyepatchPref: String,
    val doctorContact: String
)
