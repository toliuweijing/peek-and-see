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

    private val _profileName = MutableStateFlow(sharedPrefs.getString("profile_name", "Amblyopia Patient") ?: "Amblyopia Patient")
    val profileName: StateFlow<String> = _profileName.asStateFlow()

    private val _profileAge = MutableStateFlow(sharedPrefs.getInt("profile_age", 6))
    val profileAge: StateFlow<Int> = _profileAge.asStateFlow()

    private val _profileSex = MutableStateFlow(sharedPrefs.getString("profile_sex", "Not Specified") ?: "Not Specified")
    val profileSex: StateFlow<String> = _profileSex.asStateFlow()

    private val _profileSoundPref = MutableStateFlow(sharedPrefs.getString("profile_sound_pref", "Gentle Chime") ?: "Gentle Chime")
    val profileSoundPref: StateFlow<String> = _profileSoundPref.asStateFlow()

    private val _profileEyepatchPref = MutableStateFlow(sharedPrefs.getString("profile_eyepatch_pref", "Right Eye Patched") ?: "Right Eye Patched")
    val profileEyepatchPref: StateFlow<String> = _profileEyepatchPref.asStateFlow()

    private val _profileDoctorContact = MutableStateFlow(sharedPrefs.getString("profile_doctor_contact", "") ?: "")
    val profileDoctorContact: StateFlow<String> = _profileDoctorContact.asStateFlow()

    fun saveProfile(
        name: String,
        age: Int,
        sex: String,
        soundPref: String,
        eyepatchPref: String,
        doctorContact: String
    ) {
        sharedPrefs.edit().apply {
            putString("profile_name", name)
            putInt("profile_age", age)
            putString("profile_sex", sex)
            putString("profile_sound_pref", soundPref)
            putString("profile_eyepatch_pref", eyepatchPref)
            putString("profile_doctor_contact", doctorContact)
            apply()
        }
        _profileName.value = name
        _profileAge.value = age
        _profileSex.value = sex
        _profileSoundPref.value = soundPref
        _profileEyepatchPref.value = eyepatchPref
        _profileDoctorContact.value = doctorContact
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
            val sessionId = repository.insertSession(updatedSummary)
            records.forEach { record ->
                repository.insertStageRecord(record.copy(sessionId = sessionId))
            }
            
            // Clean active session
            TimerServiceState.activeRoutine.value = null
            TimerServiceState.completedSessionSummary.value = null
            TimerServiceState.completedStageRecords.value = emptyList()
            TimerServiceState.trainingState.value = TrainingState.IDLE
        }
    }

    fun discardSessionSummary() {
        TimerServiceState.activeRoutine.value = null
        TimerServiceState.completedSessionSummary.value = null
        TimerServiceState.completedStageRecords.value = emptyList()
        TimerServiceState.trainingState.value = TrainingState.IDLE
    }

    // --- ROUTINE MAKER ACTION WORKFLOWS ---
    fun deleteRoutine(routineId: Long) {
        viewModelScope.launch {
            repository.deleteRoutine(routineId)
        }
    }

    fun createOrUpdateRoutine(name: String, description: String, stages: List<Stage>) {
        viewModelScope.launch {
            val routine = Routine(name = name, description = description)
            repository.saveRoutineWithStages(routine, stages)
        }
    }

    fun updateRoutineWithDetails(routineId: Long, name: String, description: String, stages: List<Stage>) {
        viewModelScope.launch {
            val routine = Routine(id = routineId, name = name, description = description)
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
