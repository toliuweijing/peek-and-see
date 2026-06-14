package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.animateColorAsState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Stage
import com.example.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineBuilderScreen(
    viewModel: MainViewModel,
    routineId: Long?, // null if creating fresh
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val routines by viewModel.routines.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    var routineName by remember { mutableStateOf("") }
    var routineDescription by remember { mutableStateOf("") }
    var autoRepeat by remember { mutableStateOf(false) }
    val editableStages = remember { mutableStateListOf<EditableStage>() }
    var hasInitialized by remember { mutableStateOf(false) }

    // If editing, load the existing parameters
    if (routineId != null && routineId != 0L && !hasInitialized) {
        val targetRoutine = routines.find { it.routine.id == routineId }
        if (targetRoutine != null) {
            routineName = targetRoutine.routine.name
            routineDescription = targetRoutine.routine.description
            autoRepeat = targetRoutine.routine.autoRepeat
            editableStages.clear()
            targetRoutine.sortedStages.forEach { s ->
                val mins = s.durationSeconds / 60
                val secs = s.durationSeconds % 60
                editableStages.add(
                    EditableStage(
                        id = s.id,
                        name = s.name,
                        durationMinutes = mins.toString(),
                        durationSeconds = secs.toString(),
                        instruction = s.instruction,
                        soundProfileStart = s.soundProfileStart,
                        soundProfileEnd = s.soundProfileEnd,
                        requiresManualProceed = s.requiresManualProceed
                    )
                )
            }
            hasInitialized = true
        }
    } else if (!hasInitialized) {
        // Initialize with default template stages to reduce cold start typing fatigue
        routineName = ""
        routineDescription = ""
        editableStages.clear()
        editableStages.add(
            EditableStage(
                name = "Preparation Cover",
                durationMinutes = "1",
                durationSeconds = "0",
                instruction = "Cover the stronger eye with the patch helper.",
                soundProfileStart = "None",
                soundProfileEnd = "Gentle Chime",
                requiresManualProceed = true
            )
        )
        editableStages.add(
            EditableStage(
                name = "Reading Practice",
                durationMinutes = "15",
                durationSeconds = "0",
                instruction = "Keep eye patch on. Read, draw, or trace near targets.",
                soundProfileStart = "Gentle Chime",
                soundProfileEnd = "Loud Beep",
                requiresManualProceed = true
            )
        )
        hasInitialized = true
    }

    var validationError by remember { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (routineId == null) "Create Routine" else "Edit Routine",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Navigate backward to dashboard"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    titleContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- GENERAL METADATA CARD ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "General Information",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = routineName,
                        onValueChange = { routineName = it; validationError = null },
                        label = { Text("Routine Title (e.g. Near Vision Drill)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("routine_name_input"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = routineDescription,
                        onValueChange = { routineDescription = it; validationError = null },
                        label = { Text("Short Description / Purpose") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("routine_desc_input"),
                        maxLines = 3
                    )

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Auto-Repeat Training Loop",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Restarts automatically from Stage 1 once the final stage concludes.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = autoRepeat,
                            onCheckedChange = { autoRepeat = it },
                            modifier = Modifier.testTag("routine_repeat_switch")
                        )
                    }
                }
            }

            // --- STAGES ROW SECTION ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Exercises Timeline (${editableStages.size} stages)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                
                Button(
                    onClick = {
                        editableStages.add(
                            EditableStage(
                                name = "Exercise ${editableStages.size + 1}",
                                durationMinutes = "5",
                                durationSeconds = "0",
                                instruction = "Clinician instruction details here...",
                                soundProfileStart = "Gentle Chime",
                                soundProfileEnd = "Loud Beep",
                                requiresManualProceed = true
                            )
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    modifier = Modifier.testTag("add_stage_button")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Stage", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Draw individual editable stages
            editableStages.forEachIndexed { index, stage ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("stage_builder_card_$index"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Stage ${index + 1}",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary,
                                fontSize = 14.sp
                            )
                            
                            if (editableStages.size > 1) {
                                IconButton(
                                    onClick = { editableStages.removeAt(index) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Remove stage index",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = stage.name,
                            onValueChange = {
                                editableStages[index] = stage.copy(name = it)
                                validationError = null
                            },
                            label = { Text("Stage Title") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = stage.durationMinutes,
                                onValueChange = { newValue ->
                                    if (newValue.all { it.isDigit() }) {
                                        editableStages[index] = stage.copy(durationMinutes = newValue)
                                        validationError = null
                                    }
                                },
                                label = { Text("Minutes") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = stage.durationSeconds,
                                onValueChange = { newValue ->
                                    if (newValue.all { it.isDigit() }) {
                                        editableStages[index] = stage.copy(durationSeconds = newValue)
                                        validationError = null
                                    }
                                },
                                label = { Text("Seconds") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }

                        // Clinician direction/directions details
                        OutlinedTextField(
                            value = stage.instruction,
                            onValueChange = {
                                editableStages[index] = stage.copy(instruction = it)
                                validationError = null
                            },
                            label = { Text("Instruction direction (e.g. solve 3 mazes)") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 2
                        )

                        // Toggle for manual proceed gate
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Requires manual confirmation",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Wait for user confirmation (gate screen) instead of auto-advancing.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = stage.requiresManualProceed,
                                onCheckedChange = { checked ->
                                    editableStages[index] = stage.copy(requiresManualProceed = checked)
                                },
                                modifier = Modifier.testTag("stage_manual_proceed_switch_$index")
                            )
                        }

                        // Sound alerts configuration
                        var activeSoundTabIndex by remember { mutableStateOf(0) }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Stage Alerts Sound",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            // Modern Segmented Tab UI
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val tabTitles = listOf(
                                    "On Start: ${stage.soundProfileStart}",
                                    "On End: ${stage.soundProfileEnd}"
                                )
                                tabTitles.forEachIndexed { tabIndex, title ->
                                    val selected = activeSoundTabIndex == tabIndex
                                    val backgroundColor by animateColorAsState(
                                        targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                        label = "tabBg"
                                    )
                                    val contentColor by animateColorAsState(
                                        targetValue = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                        label = "tabContent"
                                    )
                                    
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(backgroundColor)
                                            .clickable { activeSoundTabIndex = tabIndex }
                                            .padding(vertical = 8.dp)
                                            .testTag(if (tabIndex == 0) "stage_${index}_sound_start_tab" else "stage_${index}_sound_end_tab"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = title,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                            color = contentColor,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            // Horizontally scrollable selection chips wrapping content
                            val soundProfiles = listOf("None", "Loud Beep", "Gentle Chime", "Victory Gong")
                            val chipScrollState = rememberScrollState()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(chipScrollState)
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                soundProfiles.forEach { profile ->
                                    val selected = if (activeSoundTabIndex == 0) {
                                        stage.soundProfileStart == profile
                                    } else {
                                        stage.soundProfileEnd == profile
                                    }
                                    FilterChip(
                                        selected = selected,
                                        onClick = {
                                            if (activeSoundTabIndex == 0) {
                                                editableStages[index] = stage.copy(soundProfileStart = profile)
                                            } else {
                                                editableStages[index] = stage.copy(soundProfileEnd = profile)
                                            }
                                        },
                                        label = { 
                                            Text(
                                                text = profile, 
                                                style = MaterialTheme.typography.labelSmall
                                            ) 
                                        },
                                        modifier = Modifier.testTag(
                                            if (activeSoundTabIndex == 0) {
                                                "stage_start_sound_${index}_$profile"
                                            } else {
                                                "stage_end_sound_${index}_$profile"
                                            }
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // --- SAVE / NOTIFICATION DISPATCH BAR ---
            if (validationError != null) {
                Text(
                    text = validationError!!,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }

            Button(
                onClick = {
                    // Validations
                    if (routineName.trim().isEmpty()) {
                        validationError = "Please insert a valid Routine title."
                        return@Button
                    }
                    if (editableStages.any { it.name.trim().isEmpty() }) {
                        validationError = "Please write stage names for all intervals."
                        return@Button
                    }

                    // Map editable stages to database entity
                    val dbStages = editableStages.mapIndexed { idx, stage ->
                        val mins = stage.durationMinutes.toIntOrNull() ?: 0
                        val secs = stage.durationSeconds.toIntOrNull() ?: 0
                        val totalSecs = (mins * 60) + secs

                        Stage(
                            routineId = routineId ?: 0,
                            stageOrder = idx + 1,
                            name = stage.name,
                            durationSeconds = if (totalSecs > 0) totalSecs else 10, // Min fallback
                            instruction = stage.instruction,
                            requiresManualProceed = stage.requiresManualProceed,
                            soundProfileStart = stage.soundProfileStart,
                            soundProfileEnd = stage.soundProfileEnd
                        )
                    }

                    if (routineId == null || routineId == 0L) {
                        viewModel.createOrUpdateRoutine(routineName, routineDescription, autoRepeat, dbStages)
                    } else {
                        viewModel.updateRoutineWithDetails(routineId, routineName, routineDescription, autoRepeat, dbStages)
                    }

                    onNavigateBack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("save_routine_button"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(imageVector = Icons.Default.Save, contentDescription = "Commit routine configuration")
                Spacer(modifier = Modifier.width(6.dp))
                Text("Save Exercise Routine", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

private data class EditableStage(
    val id: Long = 0,
    val name: String,
    val durationMinutes: String,
    val durationSeconds: String,
    val instruction: String,
    val soundProfileStart: String,
    val soundProfileEnd: String,
    val requiresManualProceed: Boolean = true
)
