package com.example.ui.screens

import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Session
import com.example.data.StageRecord
import com.example.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val summary by viewModel.completedSessionSummary.collectAsStateWithLifecycle()
    val records by viewModel.completedStageRecords.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val scrollState = rememberScrollState()

    var notesText by remember { mutableStateOf("") }
    var notePlaceholderSet by remember { mutableStateOf(false) }

    BackHandler {
        viewModel.finalizeAndSaveSession(notesText)
        onNavigateBack()
    }

    val activeSummary = summary ?: return

    // If pre-populated, pull the default notes, but allow editing
    if (!notePlaceholderSet && activeSummary.notes.isNotEmpty()) {
        notesText = activeSummary.notes
        notePlaceholderSet = true
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Session Summary",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            viewModel.finalizeAndSaveSession(notesText)
                            onNavigateBack()
                        },
                        modifier = Modifier.testTag("back_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Navigate back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.discardSessionSummary()
                            onNavigateBack()
                        },
                        modifier = Modifier.testTag("delete_summary_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete from history",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
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
            
            // --- MAIN CELEBRATION SHIELD ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = if (activeSummary.status == "Completed") Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = "Status representation badge",
                        tint = if (activeSummary.status == "Completed") Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(56.dp)
                    )

                    Text(
                        text = if (activeSummary.status == "Completed") "Great Job! Training Saved" else "Session Ended Partially",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Text(
                        text = activeSummary.routineName,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }

            // --- VISUAL METRICS DIALS GRID ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Adherence", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "${activeSummary.completionPercent}%",
                            fontWeight = FontWeight.Black,
                            fontSize = 24.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.testTag("summary_adherence_text")
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Actual Time", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        val loggedMins = activeSummary.recordedSeconds / 60
                        val loggedSecs = activeSummary.recordedSeconds % 60
                        Text(
                            text = String.format("%02d:%02d", loggedMins, loggedSecs),
                            fontWeight = FontWeight.Black,
                            fontSize = 24.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Planned Target", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        val plannedMins = activeSummary.plannedSeconds / 60
                        val plannedSecs = activeSummary.plannedSeconds % 60
                        Text(
                            text = String.format("%02d:%02d", plannedMins, plannedSecs),
                            fontWeight = FontWeight.Black,
                            fontSize = 24.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // --- HONEST CLINCAL TRANSITIONS AUDIT LOG ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Honest Log Audit Report",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Text(
                        text = "*This log records physical milestones which is helpful for parent audits and ophthalmologist evidence reviews.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                    records.forEachIndexed { idx, rec ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${idx + 1}. ${rec.stageName}",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Planned: ${rec.durationSeconds}s • Actual: ${rec.recordedSeconds}s",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Interval status badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        when (rec.status) {
                                            "Completed" -> Color(0xFFE8F5E9)
                                            "Skipped" -> Color(0xFFFFF3E0)
                                            else -> Color(0xFFFFEBEE)
                                        }
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = rec.status,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    color = when (rec.status) {
                                        "Completed" -> Color(0xFF2E7D32)
                                        "Skipped" -> Color(0xFFEF6C00)
                                        else -> Color(0xFFC62828)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // --- PARENT / CLINICIAN FIELD NOTE ENTRY ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Visual Observation Details (Optional)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Log observations like physical behavior, level of compliance, eye watering, complaints of strain, or visual tasks performed.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = notesText,
                        onValueChange = { notesText = it },
                        label = { Text("Parent / supervisor compliance comments...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .testTag("summary_notes_input"),
                        maxLines = 4
                    )
                }
            }

            // --- BOTTOM PRIMARY ACTIONS ---
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Auto-saved",
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Session auto-saved to history",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                OutlinedButton(
                    onClick = {
                        // Rapid direct clinical dispatch representation
                        val dateString = SimpleDateFormat("MMM dd, yyyy h:mm a", Locale.getDefault()).format(Date())
                        shareDirectSummaryText(context, activeSummary, records, dateString, notesText)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("summary_share_button")
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = "Share text report directly")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Share Results / Send to Doctor", fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

private fun shareDirectSummaryText(
    context: Context,
    session: Session,
    records: List<StageRecord>,
    dateStr: String,
    parentNotes: String
) {
    val recordedMins = session.recordedSeconds / 60
    val recordedSecs = session.recordedSeconds % 60
    val plannedMins = session.plannedSeconds / 60
    val plannedSecs = session.plannedSeconds % 60

    val auditLog = StringBuilder()
    records.forEachIndexed { i, rec ->
        auditLog.append("  • Stage ${i + 1}: ${rec.stageName} — ${rec.status} (${rec.recordedSeconds}s elapsed of ${rec.durationSeconds}s)\n")
    }

    val reportText = """
        🩺 EXERCISE COMPLIANCE SUMMARY LOG (Amblyopia Timer Project)
        -------------------------------------------------------------
        Date Accomplished: $dateStr
        Prescribing Routine: ${session.routineName}
        Compliance Score: ${session.completionPercent}% Complete
        Status: ${session.status}
        
        ⏱ Planned Duration: ${String.format("%02d:%02d", plannedMins, plannedSecs)}
        ⏱ Logged Duration:  ${String.format("%02d:%02d", recordedMins, recordedSecs)}
        
        📉 Stage Transition Log Audit:
$auditLog
        📝 Parent/Supervisor Clinical Observations:
        "$parentNotes"
        
        Delivered via Amblyopia Timer.
    """.trimIndent()

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Amblyopia Patient Compliance Record")
        putExtra(Intent.EXTRA_TEXT, reportText)
    }
    context.startActivity(Intent.createChooser(intent, "Transmit Doctor Compliance Record"))
}
