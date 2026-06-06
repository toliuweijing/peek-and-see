package com.example.ui.screens

import androidx.compose.foundation.background
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

    val scrollState = rememberScrollState()

    var nameInput by remember(profileName) { mutableStateOf(profileName) }
    var ageInput by remember(profileAge) { mutableStateOf(profileAge.toString()) }
    var sexInput by remember(profileSex) { mutableStateOf(profileSex) }
    var soundPrefInput by remember(profileSoundPref) { mutableStateOf(profileSoundPref) }
    var eyepatchPrefInput by remember(profileEyepatchPref) { mutableStateOf(profileEyepatchPref) }
    var doctorContactInput by remember(profileDoctorContact) { mutableStateOf(profileDoctorContact) }

    var saveConfirmationMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(16.dp)
            .testTag("profile_screen_root"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- HEADER ---
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Patient Profile",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Configure patient diagnostics, medical goals, and alarm sound choices.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // --- USER TRUST & ON-DEVICE SECURE BADGE ---
        Card(
            modifier = Modifier.fillMaxWidth().testTag("profile_trust_card"),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(12.dp)
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

        // --- SAVE BUTTON ---
        Button(
            onClick = {
                val age = ageInput.toIntOrNull() ?: 0
                viewModel.saveProfile(
                    name = nameInput.trim(),
                    age = age,
                    sex = sexInput,
                    soundPref = soundPrefInput,
                    eyepatchPref = eyepatchPrefInput,
                    doctorContact = doctorContactInput.trim()
                )
                saveConfirmationMessage = "Profile updated securely! All changes saved offline."
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("save_profile_btn"),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(imageVector = Icons.Default.Save, contentDescription = "Saves profile properties")
            Spacer(modifier = Modifier.width(6.dp))
            Text("Save Profile Preferences", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
