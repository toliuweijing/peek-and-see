package com.example.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextAlign
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
fun HistoryScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Helper functions for calendar status
    val todayCompleted = remember(sessions) {
        val todayStr = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        sessions.any {
            val dateStr = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date(it.startedAt))
            it.status == "Completed" && dateStr == todayStr
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("history_screen_root")
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- HEADER TITLE ---
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Clinical History Logs",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Track on-device compliance logs and export them to your child's ophthalmologist.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // --- ADHERENCE CALENDAR CHECK ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("history_adherence_calendar"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Weekly Compliance Roll",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            if (todayCompleted) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Done Today Indicator",
                                        tint = Color(0xFF2E7D32),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Today Completed",
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2E7D32),
                                        fontSize = 12.sp
                                    )
                                }
                            } else {
                                Text(
                                    text = "Today Outstanding",
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))

                        // Draw visual 7-day row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val daysOfWeek = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                            
                            daysOfWeek.forEachIndexed { idx, day ->
                                val dayOfWeekTarget = when (idx) {
                                    0 -> Calendar.MONDAY
                                    1 -> Calendar.TUESDAY
                                    2 -> Calendar.WEDNESDAY
                                    3 -> Calendar.THURSDAY
                                    4 -> Calendar.FRIDAY
                                    5 -> Calendar.SATURDAY
                                    else -> Calendar.SUNDAY
                                }

                                val isSpecificDayActiveCompleted = sessions.any {
                                    val c = Calendar.getInstance().apply { timeInMillis = it.startedAt }
                                    it.status == "Completed" && c.get(Calendar.DAY_OF_WEEK) == dayOfWeekTarget
                                }

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = day,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isSpecificDayActiveCompleted) Color(0xFF2E7D32)
                                                else MaterialTheme.colorScheme.surface
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSpecificDayActiveCompleted) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Completed checkbox icon",
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.3f))
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // --- HISTORY LOG COMPLIANCE CHECKS ---
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "History indicator icon",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = "Session Drill Logs (${sessions.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            if (sessions.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("empty_history_card"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Text(
                            text = "No clinical therapy sessions completed yet. Initiate a timer from the Home tab to build compliance history.",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            items(sessions, key = { it.id }) { session ->
                val formattedDate = remember(session.startedAt) {
                    val sdf = SimpleDateFormat("MMM dd, yyyy • h:mm a", Locale.getDefault())
                    sdf.format(Date(session.startedAt))
                }

                Card(
                    modifier = Modifier.fillMaxWidth().testTag("history_item_card_${session.id}"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = session.routineName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = formattedDate,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Adherence progress badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        when (session.status) {
                                            "Completed" -> Color(0xFFE8F5E9)
                                            "Partial" -> Color(0xFFFFF3E0)
                                            else -> Color(0xFFFFEBEE)
                                        }
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = session.status,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = when (session.status) {
                                        "Completed" -> Color(0xFF2E7D32)
                                        "Partial" -> Color(0xFFEF6C00)
                                        else -> Color(0xFFC62828)
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Column {
                                Text("Compliance", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${session.completionPercent}%", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Column {
                                Text("Recorded Time", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                val recordedMins = session.recordedSeconds / 60
                                val recordedSecs = session.recordedSeconds % 60
                                Text(String.format("%02d:%02d", recordedMins, recordedSecs), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Column {
                                Text("Prescribed Target", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                val plannedMins = session.plannedSeconds / 60
                                val plannedSecs = session.plannedSeconds % 60
                                Text(String.format("%02d:%02d", plannedMins, plannedSecs), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }

                        if (session.notes.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Observations: ${session.notes}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                        Spacer(modifier = Modifier.height(4.dp))

                        // Share direct compliance report text with clinician/parent
                        TextButton(
                            onClick = {
                                shareComplianceReport(context, session, formattedDate)
                            },
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.height(30.dp).testTag("export_doc_report_btn_${session.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Export report details to Doctor",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Export Report to Doctor", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

private fun shareComplianceReport(context: Context, session: Session, dateStr: String) {
    val recordedMins = session.recordedSeconds / 60
    val recordedSecs = session.recordedSeconds % 60
    val plannedMins = session.plannedSeconds / 60
    val plannedSecs = session.plannedSeconds % 60

    val reportText = """
        🩺 CLINICAL ADHERENCE LOG REPORT (Amblyopia Training Tracker)
        -------------------------------------------------------------
        Patient Session Time: $dateStr
        Prescribed Routine: ${session.routineName}
        Session Status: ${session.status}
        Adherence Rating: ${session.completionPercent}% Complete
        
        ⏱ Planned Workout Time: ${String.format("%02d:%02d", plannedMins, plannedSecs)}
        ⏱ Logged Actual Workout: ${String.format("%02d:%02d", recordedMins, recordedSecs)}
        
        📝 Parent/Supervisor Clinical Notes:
        "${session.notes}"
        
        Generated securely via Amblyopia Timer.
    """.trimIndent()

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Amblyopia Adherence Report — ${session.routineName}")
        putExtra(Intent.EXTRA_TEXT, reportText)
    }
    context.startActivity(Intent.createChooser(intent, "Share Clinical History Log"))
}
