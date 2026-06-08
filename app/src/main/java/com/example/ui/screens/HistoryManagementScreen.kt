package com.example.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Session
import com.example.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryManagementScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val routines by viewModel.routines.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // State for bulk selection
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }
    var inSelectionMode by remember { mutableStateOf(false) }

    // Dialog state controllers
    var showAddDialog by remember { mutableStateOf(false) }
    var editingSession by remember { mutableStateOf<Session?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf<List<Long>?>(null) }

    // Form inputs for Add/Edit Dialog
    var selectedRoutineId by remember { mutableStateOf(0L) }
    var selectedRoutineName by remember { mutableStateOf("") }
    var formStartedAtMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var formDurationMins by remember { mutableStateOf("20") }
    var formNotes by remember { mutableStateOf("") }
    var formStatus by remember { mutableStateOf("Completed") }
    var formCompletionPercent by remember { mutableStateOf(100f) }

    // Helper to open form with a routine pre-selected or reset
    fun openAddForm() {
        if (routines.isNotEmpty()) {
            selectedRoutineId = routines.first().routine.id
            selectedRoutineName = routines.first().routine.name
        } else {
            selectedRoutineId = -1L
            selectedRoutineName = "Manual Routine"
        }
        formStartedAtMillis = System.currentTimeMillis()
        formDurationMins = "20"
        formNotes = ""
        formStatus = "Completed"
        formCompletionPercent = 100f
        showAddDialog = true
    }

    fun openEditForm(session: Session) {
        selectedRoutineId = session.routineId
        selectedRoutineName = session.routineName
        formStartedAtMillis = session.startedAt
        formDurationMins = (session.recordedSeconds / 60).toString()
        formNotes = session.notes
        formStatus = session.status
        formCompletionPercent = session.completionPercent.toFloat()
        editingSession = session
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (inSelectionMode) "${selectedIds.size} Selected" else "Manage Therapy Logs",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (inSelectionMode) {
                                selectedIds = emptySet()
                                inSelectionMode = false
                            } else {
                                onNavigateBack()
                            }
                        },
                        modifier = Modifier.testTag("history_mgmt_back_button")
                    ) {
                        Icon(
                            imageVector = if (inSelectionMode) Icons.Default.Close else Icons.Default.ArrowBack,
                            contentDescription = "Navigate back or clear selection"
                        )
                    }
                },
                actions = {
                    if (inSelectionMode) {
                        IconButton(
                            onClick = {
                                selectedIds = sessions.map { it.id }.toSet()
                            },
                            modifier = Modifier.testTag("select_all_logs_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Select all logs"
                            )
                        }

                        IconButton(
                            onClick = {
                                showDeleteConfirmDialog = selectedIds.toList()
                            },
                            modifier = Modifier.testTag("bulk_delete_logs_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Bulk delete selected logs",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    } else if (sessions.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                inSelectionMode = true
                            },
                            modifier = Modifier.testTag("enter_selection_mode_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.List,
                                contentDescription = "Enter multi-selection mode"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    titleContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        },
        floatingActionButton = {
            if (!inSelectionMode) {
                FloatingActionButton(
                    onClick = { openAddForm() },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("add_manual_log_fab")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Insert manual compliance log"
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (sessions.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No Session Logs Found",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "You can manually insert a log using the button below or trigger a clinical automatic timer from the Home screen.",
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { openAddForm() },
                        modifier = Modifier.testTag("empty_add_log_button")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Manual Compliance Log")
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Direct Database Adjustments",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Manually record sessions when a smart timer wasn't running, delete mistaken records, or edit pediatric observations.",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }

                    items(sessions, key = { it.id }) { session ->
                        val isSelected = selectedIds.contains(session.id)
                        val formattedDate = remember(session.startedAt) {
                            val sdf = SimpleDateFormat("MMM dd, h:mm a", Locale.getDefault())
                            sdf.format(Date(session.startedAt))
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(CardDefaults.shape)
                                .clickable {
                                    if (inSelectionMode) {
                                        selectedIds = if (isSelected) {
                                            selectedIds - session.id
                                        } else {
                                            selectedIds + session.id
                                        }
                                        if (selectedIds.isEmpty()) {
                                            inSelectionMode = false
                                        }
                                    } else {
                                        openEditForm(session)
                                    }
                                }
                                .testTag("mgmt_log_item_${session.id}"),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                } else {
                                    MaterialTheme.colorScheme.surface
                                }
                            ),
                            border = if (isSelected) {
                                androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                            } else {
                                null
                            }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (inSelectionMode) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = { checked ->
                                            selectedIds = if (checked == true) {
                                                selectedIds + session.id
                                            } else {
                                                selectedIds - session.id
                                            }
                                            if (selectedIds.isEmpty()) {
                                                inSelectionMode = false
                                            }
                                        },
                                        modifier = Modifier.testTag("mgmt_checkbox_${session.id}")
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                when (session.status) {
                                                    "Completed" -> Color(0xFFE8F5E9)
                                                    "Partial" -> Color(0xFFFFF3E0)
                                                    else -> Color(0xFFFFEBEE)
                                                }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = when (session.status) {
                                                "Completed" -> Icons.Default.CheckCircle
                                                "Partial" -> Icons.Default.PauseCircle
                                                else -> Icons.Default.Cancel
                                            },
                                            contentDescription = null,
                                            tint = when (session.status) {
                                                "Completed" -> Color(0xFF2E7D32)
                                                "Partial" -> Color(0xFFEF6C00)
                                                else -> Color(0xFFC62828)
                                            }
                                        )
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = session.routineName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = formattedDate,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "•",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "${session.recordedSeconds / 60}m recorded",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                    if (session.notes.isNotEmpty()) {
                                        Text(
                                            text = "“${session.notes}”",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "${session.completionPercent}%",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    if (!inSelectionMode) {
                                        IconButton(
                                            onClick = { openEditForm(session) },
                                            modifier = Modifier.size(36.dp).testTag("edit_log_icon_btn_${session.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Edit session log",
                                                tint = MaterialTheme.colorScheme.secondary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = { showDeleteConfirmDialog = listOf(session.id) },
                                            modifier = Modifier.size(36.dp).testTag("delete_log_icon_btn_${session.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete session log",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(18.dp)
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
    }

    // --- ADD / EDIT SESSION LOG DIALOG ---
    if (showAddDialog || editingSession != null) {
        val isEdit = editingSession != null
        val calendar = remember { Calendar.getInstance().apply { timeInMillis = formStartedAtMillis } }

        val datePickerDialog = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                formStartedAtMillis = calendar.timeInMillis
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        val timePickerDialog = TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                calendar.set(Calendar.MINUTE, minute)
                formStartedAtMillis = calendar.timeInMillis
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            false
        )

        AlertDialog(
            onDismissRequest = {
                showAddDialog = false
                editingSession = null
            },
            title = {
                Text(
                    text = if (isEdit) "Edit Therapy Session Details" else "Log Manual Therapy Session",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Clinical Target Routine Dropdown Selector (Dropdown Menu)
                    item {
                        var expandedDropdown by remember { mutableStateOf(false) }
                        Column {
                            Text(
                                text = "Select Associated Routine",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { if (!isEdit) expandedDropdown = true }
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = selectedRoutineName.ifEmpty { "Manual Entry" },
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (!isEdit) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = null
                                        )
                                    }
                                }

                                DropdownMenu(
                                    expanded = expandedDropdown,
                                    onDismissRequest = { expandedDropdown = false }
                                ) {
                                    routines.forEach { r ->
                                        DropdownMenuItem(
                                            text = { Text(r.routine.name) },
                                            onClick = {
                                                selectedRoutineId = r.routine.id
                                                selectedRoutineName = r.routine.name
                                                expandedDropdown = false
                                            }
                                        )
                                    }
                                    DropdownMenuItem(
                                        text = { Text("Manual Entry / Custom") },
                                        onClick = {
                                            selectedRoutineId = -1L
                                            selectedRoutineName = "Manual Entry"
                                            expandedDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Start Time & Date Adjusters
                    item {
                        Column {
                            Text(
                                text = "Therapy Start Date & Time",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val dateSdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                                val timeSdf = SimpleDateFormat("h:mm a", Locale.getDefault())

                                OutlinedButton(
                                    onClick = { datePickerDialog.show() },
                                    modifier = Modifier.weight(1f).testTag("form_date_picker")
                                ) {
                                    Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(dateSdf.format(Date(formStartedAtMillis)), fontSize = 12.sp)
                                }

                                OutlinedButton(
                                    onClick = { timePickerDialog.show() },
                                    modifier = Modifier.weight(1f).testTag("form_time_picker")
                                ) {
                                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(timeSdf.format(Date(formStartedAtMillis)), fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    // Duration Input Fields
                    item {
                        Column {
                            Text(
                                text = "Duration (Minutes)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = formDurationMins,
                                onValueChange = { formDurationMins = it.filter { char -> char.isDigit() } },
                                modifier = Modifier.fillMaxWidth().testTag("form_duration_input"),
                                shape = RoundedCornerShape(8.dp),
                                trailingIcon = { Text("min") }
                            )
                        }
                    }

                    // Status segmented layout choices (Completed, Partial/Paused, Abandoned)
                    item {
                        Column {
                            Text(
                                text = "Session Result Status",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf("Completed", "Partial", "Abandoned").forEach { statusOption ->
                                    val isSelected = formStatus == statusOption
                                    Button(
                                        onClick = {
                                            formStatus = statusOption
                                            // auto set recommended compliance percentage
                                            formCompletionPercent = when (statusOption) {
                                                "Completed" -> 100f
                                                "Partial" -> 60f
                                                else -> 10f
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isSelected) {
                                                when (statusOption) {
                                                    "Completed" -> Color(0xFF2E7D32)
                                                    "Partial" -> Color(0xFFEF6C00)
                                                    else -> Color(0xFFC62828)
                                                }
                                            } else {
                                                MaterialTheme.colorScheme.surfaceVariant
                                            },
                                            contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        modifier = Modifier.weight(1f).testTag("status_selector_$statusOption"),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                                    ) {
                                        Text(statusOption, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // Manual Compliance rating slider
                    item {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Adherence Level (%)",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "${formCompletionPercent.toInt()}%",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                            Slider(
                                value = formCompletionPercent,
                                onValueChange = { formCompletionPercent = it },
                                valueRange = 0f..100f,
                                modifier = Modifier.fillMaxWidth().testTag("form_completion_slider")
                            )
                        }
                    }

                    // Pediatric and clinical observation notes entries
                    item {
                        Column {
                            Text(
                                text = "Ophthalmology / Pediatric Observations",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = formNotes,
                                onValueChange = { formNotes = it },
                                placeholder = { Text("e.g. child stayed compliant while tracing the blocks, patch stayed secure") },
                                modifier = Modifier.fillMaxWidth().testTag("form_notes_input"),
                                minLines = 2,
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val duration = formDurationMins.toIntOrNull() ?: 20
                        val rawSeconds = duration * 60

                        if (isEdit) {
                            val original = editingSession!!
                            val updatedSession = original.copy(
                                routineId = selectedRoutineId,
                                routineName = selectedRoutineName,
                                startedAt = formStartedAtMillis,
                                endedAt = formStartedAtMillis + (rawSeconds * 1000L),
                                status = formStatus,
                                plannedSeconds = rawSeconds,
                                recordedSeconds = rawSeconds,
                                completionPercent = formCompletionPercent.toInt(),
                                notes = formNotes
                            )
                            viewModel.updateSessionDetails(updatedSession)
                            editingSession = null
                        } else {
                            viewModel.addManualSession(
                                routineId = selectedRoutineId,
                                routineName = selectedRoutineName,
                                startedAt = formStartedAtMillis,
                                durationSeconds = rawSeconds,
                                notes = formNotes,
                                status = formStatus,
                                completionPercent = formCompletionPercent.toInt()
                            )
                            showAddDialog = false
                        }
                    },
                    modifier = Modifier.testTag("form_submit_button")
                ) {
                    Text(if (isEdit) "Update Log" else "Prepend manual entry")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAddDialog = false
                        editingSession = null
                    },
                    modifier = Modifier.testTag("form_cancel_button")
                ) {
                    Text("Discard Changes")
                }
            }
        )
    }

    // --- DELETE CONFIRMATION DIALOG ---
    showDeleteConfirmDialog?.let { sessionIds ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = null },
            title = {
                Text(
                    text = if (sessionIds.size > 1) "Confirm Bulk Deletion?" else "Delete Compliance Session Record?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (sessionIds.size > 1) {
                        "Are you absolutely certain you want to purge these ${sessionIds.size} session logs from memory? This clinical data will be permanently cleared."
                    } else {
                        "Are you certain you want to delete this specific therapy session log? This cannot be undone."
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSessions(sessionIds)
                        selectedIds = selectedIds - sessionIds.toSet()
                        if (selectedIds.isEmpty()) {
                            inSelectionMode = false
                        }
                        showDeleteConfirmDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_delete_log_btn")
                ) {
                    Text("Permanently Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirmDialog = null },
                    modifier = Modifier.testTag("cancel_delete_log_btn")
                ) {
                    Text("Retain logs")
                }
            }
        )
    }
}
