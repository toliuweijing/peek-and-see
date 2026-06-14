package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.viewmodel.MainViewModel
import com.example.data.PatientProfile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val profileName by viewModel.profileName.collectAsStateWithLifecycle()
    val profileAge by viewModel.profileAge.collectAsStateWithLifecycle()
    val profileSex by viewModel.profileSex.collectAsStateWithLifecycle()
    val profileSoundPref by viewModel.profileSoundPref.collectAsStateWithLifecycle()
    val profileEyepatchPref by viewModel.profileEyepatchPref.collectAsStateWithLifecycle()
    val profileDoctorContact by viewModel.profileDoctorContact.collectAsStateWithLifecycle()

    val patientProfiles by viewModel.patientProfiles.collectAsStateWithLifecycle()
    val activeProfileId by viewModel.activeProfileId.collectAsStateWithLifecycle()
    var showAddPatientDialog by remember { mutableStateOf(false) }
    var profileToDelete by remember { mutableStateOf<PatientProfile?>(null) }

    val scrollState = rememberScrollState()

    var nameInput by remember(profileName) { mutableStateOf(profileName) }
    var ageInput by remember(profileAge) { mutableStateOf(profileAge.toString()) }
    var sexInput by remember(profileSex) { mutableStateOf(profileSex) }
    var soundPrefInput by remember(profileSoundPref) { mutableStateOf(profileSoundPref) }
    var eyepatchPrefInput by remember(profileEyepatchPref) { mutableStateOf(profileEyepatchPref) }
    var doctorContactInput by remember(profileDoctorContact) { mutableStateOf(profileDoctorContact) }

    var saveConfirmationMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(nameInput, ageInput, sexInput, soundPrefInput, eyepatchPrefInput, doctorContactInput) {
        val age = ageInput.toIntOrNull() ?: 0
        viewModel.saveProfile(
            name = nameInput.trim(),
            age = age,
            sex = sexInput,
            soundPref = soundPrefInput,
            eyepatchPref = eyepatchPrefInput,
            doctorContact = doctorContactInput.trim()
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 24.dp)
            .testTag("profile_screen_root"),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // --- HEADER ---
        // (Removed to align with global topbar logic)

        // --- USER TRUST & ON-DEVICE SECURE BADGE ---
        Card(
            modifier = Modifier.fillMaxWidth().testTag("profile_trust_card"),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(24.dp)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Secure lock badge indicator",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Trust-by-Design Privacy",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "To preserve compliance trust, child demographics, routine configurations, and workout history are stored exclusively offline. No cloud trackers, zero remote logins.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // --- FAMILY & PATIENTS PROFILES SELECTION CARD ---
        Card(
            modifier = Modifier.fillMaxWidth().testTag("profile_profiles_card"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Group,
                            contentDescription = "Family/Multiple patients profiles icon",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Family & Patient Profiles",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    TextButton(
                        onClick = { showAddPatientDialog = true },
                        modifier = Modifier.testTag("add_patient_profile_btn"),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Patient", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    patientProfiles.forEach { p ->
                        val isActive = p.id == activeProfileId
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    viewModel.selectActiveProfile(p.id)
                                    saveConfirmationMessage = "Switched active profile to ${p.name}!"
                                }
                                .testTag("patient_profile_item_${p.id}"),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isActive) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                }
                            ),
                            border = if (isActive) {
                                androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                            } else {
                                null
                            }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(
                                                if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = p.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Age: ${p.age} • ${p.sex} • ${p.eyepatchPref}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (isActive) {
                                        Card(
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.primary
                                            ),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "ACTIVE",
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 9.sp,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    if (p.id != "default") {
                                        IconButton(
                                            onClick = { profileToDelete = p },
                                            modifier = Modifier.size(32.dp).testTag("delete_profile_btn_${p.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete patient profile",
                                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- ADD PATIENT DIALOG ---
        if (showAddPatientDialog) {
            var newName by remember { mutableStateOf("") }
            var newAge by remember { mutableStateOf("6") }
            var newSex by remember { mutableStateOf("Male") }
            var newEyePatchPref by remember { mutableStateOf("Right eye patch") }
            var newSoundPref by remember { mutableStateOf("Gentle Chime") }

            AlertDialog(
                onDismissRequest = { showAddPatientDialog = false },
                title = {
                    Text(
                        text = "Register New Patient",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = newName,
                            onValueChange = { newName = it },
                            label = { Text("Patient Name / Nickname") },
                            modifier = Modifier.fillMaxWidth().testTag("add_profile_name_input"),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = newAge,
                            onValueChange = { age ->
                                if (age.all { it.isDigit() }) {
                                    newAge = age
                                }
                            },
                            label = { Text("Age") },
                            modifier = Modifier.fillMaxWidth().testTag("add_profile_age_input"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )

                        // Sex Selector Button Row
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Patient Gender/Sex",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf("Male", "Female", "Other").forEach { sexOption ->
                                    val isSelected = newSex == sexOption
                                    Button(
                                        onClick = { newSex = sexOption },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        modifier = Modifier.weight(1f).testTag("add_profile_sex_$sexOption"),
                                        contentPadding = PaddingValues(vertical = 4.dp)
                                    ) {
                                        Text(sexOption, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // Eye Patch Side Selector Choice
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Eye Patch Side Configuration:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf("Right eye patch", "Left eye patch", "Alternating").forEach { patchOption ->
                                    val isSelected = newEyePatchPref == patchOption
                                    Button(
                                        onClick = { newEyePatchPref = patchOption },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant,
                                            contentColor = if (isSelected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        modifier = Modifier.weight(1f).testTag("add_profile_patch_$patchOption"),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                                    ) {
                                        Text(patchOption, fontSize = 9.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newName.isNotBlank()) {
                                viewModel.addProfile(
                                    name = newName.trim(),
                                    age = newAge.toIntOrNull() ?: 6,
                                    sex = newSex,
                                    soundPref = newSoundPref,
                                    eyepatchPref = newEyePatchPref,
                                    doctorContact = ""
                                )
                                showAddPatientDialog = false
                                saveConfirmationMessage = "Added and activated profile for $newName!"
                            }
                        },
                        modifier = Modifier.testTag("add_profile_confirm_btn")
                    ) {
                        Text("Add Profile")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showAddPatientDialog = false },
                        modifier = Modifier.testTag("add_profile_cancel_btn")
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }

        // --- DELETE PROFILE CONFIRMATION DIALOG ---
        profileToDelete?.let { profile ->
            AlertDialog(
                onDismissRequest = { profileToDelete = null },
                title = {
                    Text(
                        text = "Delete Patient Profile?",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text("Are you sure you want to permanently delete the profile for \"${profile.name}\"? This action will remove their child-specific clinical setup.")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteProfile(profile.id)
                            profileToDelete = null
                            saveConfirmationMessage = "Profile deleted successfully!"
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.testTag("delete_profile_confirm_btn")
                    ) {
                        Text("Permanently Delete", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { profileToDelete = null },
                        modifier = Modifier.testTag("delete_profile_cancel_btn")
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }

        // --- CLINICAL DEMOGRAPHICS SECTION ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Demographics Section Icon",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Patient Information",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it; saveConfirmationMessage = null },
                    label = { Text("Patient Name / Nickname") },
                    modifier = Modifier.fillMaxWidth().testTag("profile_name_input"),
                    singleLine = true,
                    leadingIcon = { Icon(imageVector = Icons.Default.Badge, contentDescription = null) }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = ageInput,
                        onValueChange = { age ->
                            if (age.all { it.isDigit() }) {
                                ageInput = age
                                saveConfirmationMessage = null
                            }
                        },
                        label = { Text("Age (e.g. 6)") },
                        modifier = Modifier.weight(1f).testTag("profile_age_input"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        leadingIcon = { Icon(imageVector = Icons.Default.ChildCare, contentDescription = null) }
                    )

                    Column(
                        modifier = Modifier.weight(1.5f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Patient Sex/Gender:",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf("Male", "Female", "Other").forEach { sex ->
                                val isSelected = sexInput == sex
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { sexInput = sex; saveConfirmationMessage = null },
                                    label = { Text(sex, fontSize = 10.sp) },
                                    modifier = Modifier.testTag("profile_sex_chip_$sex")
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- PARENT PREFERENCES SECTION ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Preferences Section Icon",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Treatment Preferences",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                // Eyepatch Choice
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Eye Patch Side Configuration:",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("Right eye patch", "Left eye patch", "Alternating").forEach { side ->
                            val isSelected = eyepatchPrefInput == side
                            FilterChip(
                                selected = isSelected,
                                onClick = { eyepatchPrefInput = side; saveConfirmationMessage = null },
                                label = { Text(side, fontSize = 10.sp) },
                                modifier = Modifier.weight(1f).testTag("profile_patch_chip_$side")
                            )
                        }
                    }
                }

                // Default Sound Alarm Spinner
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Default Alarm Ring Tone:",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("Gentle Chime", "Loud Beep", "Victory Gong").forEach { tone ->
                            val isSelected = soundPrefInput == tone
                            FilterChip(
                                selected = isSelected,
                                onClick = { soundPrefInput = tone; saveConfirmationMessage = null },
                                label = { Text(tone, fontSize = 10.sp) },
                                modifier = Modifier.weight(1f).testTag("profile_sound_chip_$tone")
                            )
                        }
                    }
                }
            }
        }

        // --- CLINICIAN/PHYSICIAN CONTACT INFO ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MedicalServices,
                        contentDescription = "Clinician Section Icon",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Ophthalmologist Reference",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                OutlinedTextField(
                    value = doctorContactInput,
                    onValueChange = { doctorContactInput = it; saveConfirmationMessage = null },
                    label = { Text("Prescribing Doctor name or medical clinic") },
                    modifier = Modifier.fillMaxWidth().testTag("profile_doctor_input"),
                    singleLine = true,
                    leadingIcon = { Icon(imageVector = Icons.Default.ContactPhone, contentDescription = null) }
                )
            }
        }

        // --- SAVE FLOW STATUS ---
        if (saveConfirmationMessage != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = saveConfirmationMessage!!,
                    color = Color(0xFF2E7D32),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
