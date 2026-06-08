package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.notification.TimerService
import com.example.notification.TimerServiceState
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class TrainingState {
    IDLE,
    RUNNING,
    EXPIRED_WAITING,
    PAUSED,
    COMPLETED
}

class MainViewModel(
    application: Application,
    private val repository: AppRepository
) : AndroidViewModel(application) {

    // --- USER PROFILE STATE (SharedPreferences) ---
    private val sharedPrefs = application.getSharedPreferences("amblyopia_timer_prefs", Context.MODE_PRIVATE)

    private val _activeProfileId = MutableStateFlow(sharedPrefs.getString("active_patient_id", "default") ?: "default")
    val activeProfileId: StateFlow<String> = _activeProfileId.asStateFlow()

    private val _patientProfiles = MutableStateFlow<List<PatientProfile>>(emptyList())
    val patientProfiles: StateFlow<List<PatientProfile>> = _patientProfiles.asStateFlow()

    private val _profileName = MutableStateFlow("")
    val profileName: StateFlow<String> = _profileName.asStateFlow()

    private val _profileAge = MutableStateFlow(6)
    val profileAge: StateFlow<Int> = _profileAge.asStateFlow()

    private val _profileSex = MutableStateFlow("Not Specified")
    val profileSex: StateFlow<String> = _profileSex.asStateFlow()

    private val _profileSoundPref = MutableStateFlow("Gentle Chime")
    val profileSoundPref: StateFlow<String> = _profileSoundPref.asStateFlow()

    private val _profileEyepatchPref = MutableStateFlow("Right Eye Patched")
    val profileEyepatchPref: StateFlow<String> = _profileEyepatchPref.asStateFlow()

    private val _profileDoctorContact = MutableStateFlow("")
    val profileDoctorContact: StateFlow<String> = _profileDoctorContact.asStateFlow()

    private fun getProfileIdList(): List<String> {
        val idsString = sharedPrefs.getString("patient_profiles_ids", "") ?: ""
        if (idsString.isEmpty()) return emptyList()
        return idsString.split(",")
    }

    private fun saveProfileIdList(ids: List<String>) {
        sharedPrefs.edit().putString("patient_profiles_ids", ids.joinToString(",")).apply()
    }

    private fun readProfile(id: String): PatientProfile {
        if (id == "default") {
            return PatientProfile(
                id = "default",
                name = sharedPrefs.getString("profile_name", "Amblyopia Patient") ?: "Amblyopia Patient",
                age = sharedPrefs.getInt("profile_age", 6),
                sex = sharedPrefs.getString("profile_sex", "Not Specified") ?: "Not Specified",
                soundPref = sharedPrefs.getString("profile_sound_pref", "Gentle Chime") ?: "Gentle Chime",
                eyepatchPref = sharedPrefs.getString("profile_eyepatch_pref", "Right Eye Patched") ?: "Right Eye Patched",
                doctorContact = sharedPrefs.getString("profile_doctor_contact", "") ?: ""
            )
        }
        return PatientProfile(
            id = id,
            name = sharedPrefs.getString("profile_${id}_name", "") ?: "",
            age = sharedPrefs.getInt("profile_${id}_age", 6),
            sex = sharedPrefs.getString("profile_${id}_sex", "Not Specified") ?: "Not Specified",
            soundPref = sharedPrefs.getString("profile_${id}_sound_pref", "Gentle Chime") ?: "Gentle Chime",
            eyepatchPref = sharedPrefs.getString("profile_${id}_eyepatch_pref", "Right Eye Patched") ?: "Right Eye Patched",
            doctorContact = sharedPrefs.getString("profile_${id}_doctor_contact", "") ?: ""
        )
    }

    private fun writeProfile(profile: PatientProfile) {
        sharedPrefs.edit().apply {
            if (profile.id == "default") {
                putString("profile_name", profile.name)
                putInt("profile_age", profile.age)
                putString("profile_sex", profile.sex)
                putString("profile_sound_pref", profile.soundPref)
                putString("profile_eyepatch_pref", profile.eyepatchPref)
                putString("profile_doctor_contact", profile.doctorContact)
            } else {
                putString("profile_${profile.id}_name", profile.name)
                putInt("profile_${profile.id}_age", profile.age)
                putString("profile_${profile.id}_sex", profile.sex)
                putString("profile_${profile.id}_sound_pref", profile.soundPref)
                putString("profile_${profile.id}_eyepatch_pref", profile.eyepatchPref)
                putString("profile_${profile.id}_doctor_contact", profile.doctorContact)
            }
            apply()
        }
    }

    private fun loadProfilesFromPrefs() {
        var ids = getProfileIdList()
        if (ids.isEmpty()) {
            ids = listOf("default")
            saveProfileIdList(ids)
        }
        val profilesList = ids.map { readProfile(it) }
        _patientProfiles.value = profilesList

        val currentActiveId = _activeProfileId.value
        if (!ids.contains(currentActiveId)) {
            _activeProfileId.value = "default"
            sharedPrefs.edit().putString("active_patient_id", "default").apply()
        }
        updateActiveStateFlows()
    }

    private fun updateActiveStateFlows() {
        val activeProfile = readProfile(_activeProfileId.value)
        _profileName.value = activeProfile.name
        _profileAge.value = activeProfile.age
        _profileSex.value = activeProfile.sex
        _profileSoundPref.value = activeProfile.soundPref
        _profileEyepatchPref.value = activeProfile.eyepatchPref
        _profileDoctorContact.value = activeProfile.doctorContact
    }

    fun saveProfile(
        name: String,
        age: Int,
        sex: String,
        soundPref: String,
        eyepatchPref: String,
        doctorContact: String
    ) {
        val id = _activeProfileId.value
        val profile = PatientProfile(id, name, age, sex, soundPref, eyepatchPref, doctorContact)
        writeProfile(profile)
        loadProfilesFromPrefs()
    }

    fun selectActiveProfile(id: String) {
        _activeProfileId.value = id
        sharedPrefs.edit().putString("active_patient_id", id).apply()
        updateActiveStateFlows()
    }

    fun addProfile(
        name: String,
        age: Int,
        sex: String,
        soundPref: String,
        eyepatchPref: String,
        doctorContact: String
    ) {
        val newId = "patient_${System.currentTimeMillis()}"
        val newProfile = PatientProfile(newId, name, age, sex, soundPref, eyepatchPref, doctorContact)
        writeProfile(newProfile)

        val ids = getProfileIdList().toMutableList()
        ids.add(newId)
        saveProfileIdList(ids)

        _activeProfileId.value = newId
        sharedPrefs.edit().putString("active_patient_id", newId).apply()
        loadProfilesFromPrefs()
    }

    fun deleteProfile(id: String) {
        if (id == "default") return
        val ids = getProfileIdList().toMutableList()
        if (ids.contains(id)) {
            ids.remove(id)
            saveProfileIdList(ids)

            sharedPrefs.edit().apply {
                remove("profile_${id}_name")
                remove("profile_${id}_age")
                remove("profile_${id}_sex")
                remove("profile_${id}_sound_pref")
                remove("profile_${id}_eyepatch_pref")
                remove("profile_${id}_doctor_contact")
                apply()
            }

            if (_activeProfileId.value == id) {
                _activeProfileId.value = "default"
                sharedPrefs.edit().putString("active_patient_id", "default").apply()
            }
            loadProfilesFromPrefs()
        }
    }

    // --- DATABASE FLOWS ---
    val routines: StateFlow<List<RoutineWithStages>> = repository.allRoutinesWithStages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sessions: StateFlow<List<Session>> = repository.allSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- ACTIVE SESSION STATE MACHINE FROM TIMERSERVICE ---
    val trainingState: StateFlow<TrainingState> = TimerServiceState.trainingState.asStateFlow()
    val activeRoutine: StateFlow<RoutineWithStages?> = TimerServiceState.activeRoutine.asStateFlow()
    val currentStageIndex: StateFlow<Int> = TimerServiceState.currentStageIndex.asStateFlow()
    val currentStageRemainingSeconds: StateFlow<Int> = TimerServiceState.currentStageRemainingSeconds.asStateFlow()
    val currentStageProgress: StateFlow<Float> = TimerServiceState.currentStageProgress.asStateFlow()

    val completedSessionSummary: StateFlow<Session?> = TimerServiceState.completedSessionSummary.asStateFlow()
    val completedStageRecords: StateFlow<List<StageRecord>> = TimerServiceState.completedStageRecords.asStateFlow()

    init {
        loadProfilesFromPrefs()
        // Pre-populate clinic standard templates if database is fresh
        viewModelScope.launch {
            repository.prepopulateDefaultRoutinesIfNeeded()
        }
    }

    // --- SERVICE ACTIONS DISPATCHER ---
    fun startSession(routine: RoutineWithStages) {
        TimerService.startService(getApplication(), routine)
    }

    fun pauseSession() {
        val intent = Intent(getApplication(), TimerService::class.java).apply {
            action = TimerService.ACTION_PAUSE
        }
        getApplication<Application>().startService(intent)
    }

    fun resumeSession() {
        val intent = Intent(getApplication(), TimerService::class.java).apply {
            action = TimerService.ACTION_RESUME
        }
        getApplication<Application>().startService(intent)
    }

    fun stopAudioAlarm() {
        val intent = Intent(getApplication(), TimerService::class.java).apply {
            action = TimerService.ACTION_STOP_ALARM
        }
        getApplication<Application>().startService(intent)
    }

    fun proceedToNextStage(repeat: Boolean) {
        val intent = Intent(getApplication(), TimerService::class.java).apply {
            action = if (repeat) TimerService.ACTION_REPEAT else TimerService.ACTION_NEXT
        }
        getApplication<Application>().startService(intent)
    }

    fun skipCurrentStage() {
        val intent = Intent(getApplication(), TimerService::class.java).apply {
            action = TimerService.ACTION_SKIP
        }
        getApplication<Application>().startService(intent)
    }

    fun endSessionEarly() {
        val intent = Intent(getApplication(), TimerService::class.java).apply {
            action = TimerService.ACTION_END_EARLY
        }
        getApplication<Application>().startService(intent)
    }

    // --- FINAL REPORT PERSISTENCE ---
    fun finalizeAndSaveSession(notes: String) {
        val summary = TimerServiceState.completedSessionSummary.value ?: return
        val records = TimerServiceState.completedStageRecords.value
        
        viewModelScope.launch {
            val updatedSummary = summary.copy(notes = notes)
            if (updatedSummary.id > 0) {
                repository.updateSession(updatedSummary)
            } else {
                val sessionId = repository.insertSession(updatedSummary)
                records.forEach { record ->
                    repository.insertStageRecord(record.copy(sessionId = sessionId))
                }
            }
            
            // Clean active session
            TimerServiceState.activeRoutine.value = null
            TimerServiceState.completedSessionSummary.value = null
            TimerServiceState.completedStageRecords.value = emptyList()
            TimerServiceState.trainingState.value = TrainingState.IDLE
        }
    }

    fun discardSessionSummary() {
        val summary = TimerServiceState.completedSessionSummary.value
        if (summary != null && summary.id > 0) {
            viewModelScope.launch {
                repository.deleteSessionAndRecords(summary.id)
            }
        }
        TimerServiceState.activeRoutine.value = null
        TimerServiceState.completedSessionSummary.value = null
        TimerServiceState.completedStageRecords.value = emptyList()
        TimerServiceState.trainingState.value = TrainingState.IDLE
    }

    fun getStageRecordsForSession(sessionId: Long): Flow<List<StageRecord>> {
        return repository.getStageRecordsForSession(sessionId)
    }

    // --- SESSION ADHERENCE LOG MANAGEMENT ---
    fun deleteSession(sessionId: Long) {
        viewModelScope.launch {
            repository.deleteSessionAndRecords(sessionId)
        }
    }

    fun deleteSessions(sessionIds: List<Long>) {
        viewModelScope.launch {
            sessionIds.forEach { id ->
                repository.deleteSessionAndRecords(id)
            }
        }
    }

    fun addManualSession(
        routineId: Long,
        routineName: String,
        startedAt: Long,
        durationSeconds: Int,
        notes: String,
        status: String,
        completionPercent: Int
    ) {
        viewModelScope.launch {
            val session = Session(
                routineId = routineId,
                routineName = routineName,
                startedAt = startedAt,
                endedAt = startedAt + (durationSeconds * 1000L),
                status = status,
                plannedSeconds = durationSeconds,
                recordedSeconds = durationSeconds,
                completionPercent = completionPercent,
                notes = notes
            )
            repository.insertSession(session)
        }
    }

    fun updateSessionDetails(session: Session) {
        viewModelScope.launch {
            repository.updateSession(session)
        }
    }

    // --- ROUTINE MAKER ACTION WORKFLOWS ---
    fun deleteRoutine(routineId: Long) {
        viewModelScope.launch {
            repository.deleteRoutine(routineId)
        }
    }

    fun createOrUpdateRoutine(name: String, description: String, autoRepeat: Boolean, stages: List<Stage>) {
        viewModelScope.launch {
            val routine = Routine(name = name, description = description, autoRepeat = autoRepeat)
            repository.saveRoutineWithStages(routine, stages)
        }
    }

    fun updateRoutineWithDetails(routineId: Long, name: String, description: String, autoRepeat: Boolean, stages: List<Stage>) {
        viewModelScope.launch {
            val routine = Routine(id = routineId, name = name, description = description, autoRepeat = autoRepeat)
            repository.saveRoutineWithStages(routine, stages)
        }
    }
}

class MainViewModelFactory(
    private val application: Application,
    private val repository: AppRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
