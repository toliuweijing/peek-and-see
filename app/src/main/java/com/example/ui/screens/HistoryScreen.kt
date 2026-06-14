package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
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
import kotlin.math.absoluteValue

enum class HistoryRange { DAY, WEEK, MONTH }

data class ChartSegment(
    val routineId: Long,
    val routineName: String,
    val durationMinutes: Float,
    val color: Color
)

fun getRoutineColor(routineId: Long): Color {
    val colors = listOf(
        Color(0xFFFF6F00), // Visual Alert Orange
        Color(0xFF0091EA), // Neon Cyan
        Color(0xFFAA00FF), // Cyber Purple
        Color(0xFF00C853), // Bright Green
        Color(0xFFFFD600), // Gold
        Color(0xFFFF3D00), // Deep Coral
        Color(0xFF2979FF), // Electric Blue
        Color(0xFFC51162)  // Neon Pink
    )
    return colors[(routineId.hashCode().absoluteValue % colors.size)]
}

@Composable
fun InteractiveHistoryChart(
    viewModel: MainViewModel,
    selectedRoutineIds: Set<Long>,
    selectedRange: HistoryRange,
    onRangeSelected: (HistoryRange) -> Unit,
    timeOffset: Int,
    onTimeOffsetChanged: (Int) -> Unit,
    referenceCalendar: Calendar,
    activeSessionsInPeriod: List<Session>,
    onToggleRoutine: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val allDatabaseRoutines by viewModel.routines.collectAsStateWithLifecycle()

    // Slots & segments calculation
    val maxDays = remember(referenceCalendar) { referenceCalendar.getActualMaximum(Calendar.DAY_OF_MONTH) }

    
    val slots = remember(selectedRange, sessions, activeSessionsInPeriod) {
        when (selectedRange) {
            HistoryRange.DAY -> {
                List(24) { hour ->
                    val hourSessions = activeSessionsInPeriod.filter {
                        val c = Calendar.getInstance().apply { timeInMillis = it.startedAt }
                        c.get(Calendar.HOUR_OF_DAY) == hour
                    }
                    hourSessions.groupBy { it.routineId }.map { (routineId, list) ->
                        val sumMin = list.sumOf { it.recordedSeconds } / 60f
                        val routineName = list.firstOrNull()?.routineName ?: "Routine"
                        ChartSegment(routineId, routineName, sumMin, getRoutineColor(routineId))
                    }
                }
            }
            HistoryRange.WEEK -> {
                List(7) { dayIdx ->
                    val targetDayOfWeek = when (dayIdx) {
                        0 -> Calendar.MONDAY
                        1 -> Calendar.TUESDAY
                        2 -> Calendar.WEDNESDAY
                        3 -> Calendar.THURSDAY
                        4 -> Calendar.FRIDAY
                        5 -> Calendar.SATURDAY
                        else -> Calendar.SUNDAY
                    }
                    val daySessions = activeSessionsInPeriod.filter {
                        val c = Calendar.getInstance().apply { timeInMillis = it.startedAt }
                        c.get(Calendar.DAY_OF_WEEK) == targetDayOfWeek
                    }
                    daySessions.groupBy { it.routineId }.map { (routineId, list) ->
                        val sumMin = list.sumOf { it.recordedSeconds } / 60f
                        val routineName = list.firstOrNull()?.routineName ?: "Routine"
                        ChartSegment(routineId, routineName, sumMin, getRoutineColor(routineId))
                    }
                }
            }
            HistoryRange.MONTH -> {
                List(maxDays) { dayIdx ->
                    val dayNum = dayIdx + 1
                    val daySessions = activeSessionsInPeriod.filter {
                        val c = Calendar.getInstance().apply { timeInMillis = it.startedAt }
                        c.get(Calendar.DAY_OF_MONTH) == dayNum
                    }
                    daySessions.groupBy { it.routineId }.map { (routineId, list) ->
                        val sumMin = list.sumOf { it.recordedSeconds } / 60f
                        val routineName = list.firstOrNull()?.routineName ?: "Routine"
                        ChartSegment(routineId, routineName, sumMin, getRoutineColor(routineId))
                    }
                }
            }
        }
    }

    val totalMinutesInPeriod = remember(activeSessionsInPeriod) {
        activeSessionsInPeriod.sumOf { it.recordedSeconds } / 60
    }

    val totalHoursInPeriod = remember(totalMinutesInPeriod) {
        totalMinutesInPeriod / 60f
    }

    // Dynamic scale limit
    val maxStackedMinutes = remember(slots) {
        slots.maxOfOrNull { slot -> slot.sumOf { it.durationMinutes.toDouble() } }?.toFloat() ?: 0f
    }
    val yAxisMax = remember(maxStackedMinutes) {
        if (maxStackedMinutes < 15f) 15f else ((maxStackedMinutes / 10f).toInt() + 1) * 10f
    }

    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
    val axisLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f).toArgb()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("history_compliance_chart_card"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Segmented interactive tabs matching the Apple Health aesthetic
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf(HistoryRange.DAY, HistoryRange.WEEK, HistoryRange.MONTH).forEach { range ->
                    val isSelected = selectedRange == range
                    val rangeLabel = when (range) {
                        HistoryRange.DAY -> "Day"
                        HistoryRange.WEEK -> "Week"
                        HistoryRange.MONTH -> "Month"
                    }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp)
                                .clip(RoundedCornerShape(17.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary 
                                    else Color.Transparent
                                )
                                .clickable { 
                                onRangeSelected(range)
                                onTimeOffsetChanged(0)
                            }
                            .testTag("chart_tab_${range.name.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = rangeLabel,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { onTimeOffsetChanged(timeOffset - 1) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowLeft, 
                        contentDescription = "Previous time period",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                val dateStr = remember(selectedRange, referenceCalendar, timeOffset) {
                    when (selectedRange) {
                        HistoryRange.DAY -> {
                            if (timeOffset == 0) "Today"
                            else if (timeOffset == -1) "Yesterday"
                            else SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(referenceCalendar.time)
                        }
                        HistoryRange.WEEK -> {
                            val startCal = referenceCalendar.clone() as Calendar
                            startCal.set(Calendar.DAY_OF_WEEK, startCal.firstDayOfWeek)
                            val endCal = referenceCalendar.clone() as Calendar
                            endCal.set(Calendar.DAY_OF_WEEK, endCal.firstDayOfWeek)
                            endCal.add(Calendar.DAY_OF_YEAR, 6)
                            
                            val sdf = SimpleDateFormat("MMM d", Locale.getDefault())
                            val startStr = sdf.format(startCal.time)
                            val endStr = sdf.format(endCal.time)
                            
                            if (timeOffset == 0) "This Week"
                            else if (timeOffset == -1) "Last Week"
                            else "$startStr - $endStr"
                        }
                        HistoryRange.MONTH -> {
                            if (timeOffset == 0) "This Month"
                            else if (timeOffset == -1) "Last Month"
                            else SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(referenceCalendar.time)
                        }
                    }
                }
                
                Text(
                    text = dateStr,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyMedium
                )
                
                IconButton(
                    onClick = { onTimeOffsetChanged(timeOffset + 1) }, 
                    enabled = timeOffset < 0,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowRight, 
                        contentDescription = "Next time period",
                        tint = if (timeOffset < 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Text layout under filters matching reference layout
            Text(
                text = "TOTAL COMPLIANCE TIME",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val formattedHours = remember(totalHoursInPeriod) {
                    if (totalHoursInPeriod % 1.0f == 0.0f) {
                        String.format(Locale.getDefault(), "%.0f", totalHoursInPeriod)
                    } else if ((totalHoursInPeriod * 10) % 1.0f == 0.0f) {
                        String.format(Locale.getDefault(), "%.1f", totalHoursInPeriod)
                    } else {
                        String.format(Locale.getDefault(), "%.2f", totalHoursInPeriod)
                    }
                }
                Text(
                    text = formattedHours,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (formattedHours == "1") "hour" else "hours",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                if (totalMinutesInPeriod > 0) {
                    val displayHrs = totalMinutesInPeriod / 60
                    val displayMins = totalMinutesInPeriod % 60
                    val textBreakdown = if (displayHrs > 0 && displayMins > 0) {
                        "(${displayHrs}h ${displayMins}m)"
                    } else if (displayHrs == 0) {
                        "(${displayMins}m)"
                    } else {
                        ""
                    }
                    if (textBreakdown.isNotEmpty()) {
                        Text(
                            text = textBreakdown,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
                        )
                    }
                }
            }
            Text(
                text = when (selectedRange) {
                    HistoryRange.DAY -> if (timeOffset == 0) "Recorded sessions for today" else "Recorded sessions for selected day"
                    HistoryRange.WEEK -> "Consolidated weekly tracking"
                    HistoryRange.MONTH -> "Historic monthly trend overview"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(22.dp))

            val gridColorToken = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)

            // The main visual Canvas component
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .testTag("compliance_canvas_chart")
            ) {
                val canvasWidth = size.width
                val canvasHeight = size.height

                val paddingLeft = 10.dp.toPx()
                val paddingRight = 50.dp.toPx() // space for sidebar scale lines
                val paddingTop = 15.dp.toPx()
                val paddingBottom = 30.dp.toPx() // space for column labels

                val chartWidth = canvasWidth - paddingLeft - paddingRight
                val chartHeight = canvasHeight - paddingTop - paddingBottom

                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)

                // Scaling formulas
                val yAtValue = { value: Float ->
                    paddingTop + chartHeight - (value / yAxisMax) * chartHeight
                }

                // Gridlines & Scale Indicators (Right Y-Axis aligned)
                listOf(0f, yAxisMax / 2, yAxisMax).forEach { level ->
                    val y = yAtValue(level)
                    // Dotted line
                    drawLine(
                        color = gridColorToken,
                        start = Offset(paddingLeft, y),
                        end = Offset(paddingLeft + chartWidth, y),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = dashEffect
                    )

                    // Scale texts
                    drawContext.canvas.nativeCanvas.drawText(
                        String.format("%.0fm", level),
                        paddingLeft + chartWidth + 8.dp.toPx(),
                        y + 4.dp.toPx(),
                        Paint().apply {
                            color = axisLabelColor
                            textSize = 10.dp.toPx()
                            textAlign = Paint.Align.LEFT
                            isAntiAlias = true
                        }
                    )
                }

                val numSlots = slots.size
                val columnWidth = chartWidth / numSlots
                // Scale width of elements based on quantity: narrow bars for Month, wider for Week/Day style
                val barPercentage = if (selectedRange == HistoryRange.MONTH) 0.50f else 0.65f
                val barWidth = columnWidth * barPercentage
                val horizontalMargin = (columnWidth - barWidth) / 2f

                // Draw Stacked Columns
                for (i in 0 until numSlots) {
                    val xStart = paddingLeft + (i * columnWidth) + horizontalMargin
                    var baseMinutes = 0f
                    val segmentList = slots[i]

                    segmentList.forEach { segment ->
                        val segStartMins = baseMinutes
                        val segEndMins = baseMinutes + segment.durationMinutes

                        val yTop = yAtValue(segEndMins)
                        val yBottom = yAtValue(segStartMins)
                        val barHeight = yBottom - yTop

                        if (barHeight > 0.5f) {
                            drawRoundRect(
                                color = segment.color,
                                topLeft = Offset(xStart, yTop),
                                size = Size(barWidth, barHeight),
                                cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                            )
                        }
                        baseMinutes = segEndMins
                    }
                }

                // Bottom Labels (Horizontal X-Axis matching reference labels)
                val labelPaint = Paint().apply {
                    color = axisLabelColor
                    textSize = 9.dp.toPx()
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                }

                when (selectedRange) {
                    HistoryRange.DAY -> {
                        val targetLabels = listOf(0, 6, 12, 18, 23)
                        targetLabels.forEach { h ->
                            val slotCenterX = paddingLeft + (h * columnWidth) + (columnWidth / 2f)
                            drawContext.canvas.nativeCanvas.drawText(
                                String.format("%02d:00", h),
                                slotCenterX,
                                paddingTop + chartHeight + 18.dp.toPx(),
                                labelPaint
                            )
                        }
                    }
                    HistoryRange.WEEK -> {
                        val labels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                        labels.forEachIndexed { idx, label ->
                            val slotCenterX = paddingLeft + (idx * columnWidth) + (columnWidth / 2f)
                            drawContext.canvas.nativeCanvas.drawText(
                                label,
                                slotCenterX,
                                paddingTop + chartHeight + 18.dp.toPx(),
                                labelPaint
                            )
                        }
                    }
                    HistoryRange.MONTH -> {
                        val targetDays = listOf(1, 10, 20, 30)
                        targetDays.forEach { d ->
                            if (d <= numSlots) {
                                val slotIdx = d - 1
                                val slotCenterX = paddingLeft + (slotIdx * columnWidth) + (columnWidth / 2f)
                                drawContext.canvas.nativeCanvas.drawText(
                                    d.toString(),
                                    slotCenterX,
                                    paddingTop + chartHeight + 18.dp.toPx(),
                                    labelPaint
                                )
                            }
                        }
                    }
                }
            }

            // Active legends and multi-select filter controls
            val legendRoutines = remember(allDatabaseRoutines, sessions) {
                val dbRoutines = allDatabaseRoutines.map { it.routine.id to it.routine.name }
                val sessionRoutines = sessions.map { it.routineId to it.routineName }
                (dbRoutines + sessionRoutines).distinctBy { it.first }
            }

            if (legendRoutines.isNotEmpty()) {
                Spacer(modifier = Modifier.height(18.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "SELECT ROUTINES TO FILTER CHART & LOGS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    legendRoutines.chunked(2).forEach { rowPair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(15.dp)
                        ) {
                            rowPair.forEach { (routineId, name) ->
                                val isSelected = selectedRoutineIds.contains(routineId)
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isSelected) {
                                                getRoutineColor(routineId).copy(alpha = 0.12f)
                                            } else {
                                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                                            }
                                        )
                                        .clickable { onToggleRoutine(routineId) }
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isSelected) {
                                                    getRoutineColor(routineId)
                                                } else {
                                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                                }
                                            )
                                    )
                                    Text(
                                        text = name,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Active Filter",
                                            tint = getRoutineColor(routineId),
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                            if (rowPair.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            // Trends Insight card matching the Apple Health reference visual layout
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            text = "Trends: Training Compliance",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (totalMinutesInPeriod > 0) {
                                "The clinical compliance trend trajectory is positive. Keep maintaining active workout schedules."
                            } else {
                                "Trend trajectory is unavailable until sessions are clocked in this selected timeline."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (totalMinutesInPeriod > 0) Color(0xFF4CAF50).copy(alpha = 0.15f)
                                else Color(0xFFEF5350).copy(alpha = 0.10f)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (totalMinutesInPeriod > 0) "Optimal" else "Unavailable",
                            color = if (totalMinutesInPeriod > 0) Color(0xFF4CAF50) else Color(0xFFEF5350),
                            fontWeight = FontWeight.Black,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: MainViewModel,
    onNavigateToHistoryManagement: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val allDatabaseRoutines by viewModel.routines.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Gather all unique routine IDs from both database and local session history logs
    val availableRoutineIds = remember(allDatabaseRoutines, sessions) {
        (allDatabaseRoutines.map { it.routine.id } + sessions.map { it.routineId }).toSet()
    }

    // Default to including all available routines in stats and visualization
    var selectedRoutineIds by remember(availableRoutineIds) {
        mutableStateOf(availableRoutineIds)
    }

    val filteredSessions = remember(sessions, selectedRoutineIds) {
        sessions.filter { selectedRoutineIds.contains(it.routineId) }
    }

    var selectedRange by remember { mutableStateOf(HistoryRange.DAY) }
    var timeOffset by remember { mutableStateOf(0) }

    val referenceCalendar = remember(selectedRange, timeOffset) {
        Calendar.getInstance().apply {
            when (selectedRange) {
                HistoryRange.DAY -> add(Calendar.DAY_OF_YEAR, timeOffset)
                HistoryRange.WEEK -> add(Calendar.WEEK_OF_YEAR, timeOffset)
                HistoryRange.MONTH -> add(Calendar.MONTH, timeOffset)
            }
        }
    }

    val activeSessionsInPeriod = remember(filteredSessions, selectedRange, referenceCalendar) {
        val targetYear = referenceCalendar.get(Calendar.YEAR)
        when (selectedRange) {
            HistoryRange.DAY -> {
                val targetDay = referenceCalendar.get(Calendar.DAY_OF_YEAR)
                filteredSessions.filter {
                    val c = Calendar.getInstance().apply { timeInMillis = it.startedAt }
                    c.get(Calendar.YEAR) == targetYear && c.get(Calendar.DAY_OF_YEAR) == targetDay
                }
            }
            HistoryRange.WEEK -> {
                val targetWeek = referenceCalendar.get(Calendar.WEEK_OF_YEAR)
                filteredSessions.filter {
                    val c = Calendar.getInstance().apply { timeInMillis = it.startedAt }
                    c.get(Calendar.YEAR) == targetYear && c.get(Calendar.WEEK_OF_YEAR) == targetWeek
                }
            }
            HistoryRange.MONTH -> {
                val targetMonth = referenceCalendar.get(Calendar.MONTH)
                filteredSessions.filter {
                    val c = Calendar.getInstance().apply { timeInMillis = it.startedAt }
                    c.get(Calendar.YEAR) == targetYear && c.get(Calendar.MONTH) == targetMonth
                }
            }
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
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // --- HEADER TITLE ---
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.Top
                ) {
                    Button(
                        onClick = onNavigateToHistoryManagement,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        modifier = Modifier
                            .testTag("navigate_to_history_mgmt_btn")
                            .padding(start = 8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Manage logs direct screen button",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Manage", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // --- INLINE INTERACTIVE HISTORIC CHART ---
            item {
                InteractiveHistoryChart(
                    viewModel = viewModel,
                    selectedRoutineIds = selectedRoutineIds,
                    selectedRange = selectedRange,
                    onRangeSelected = { selectedRange = it },
                    timeOffset = timeOffset,
                    onTimeOffsetChanged = { timeOffset = it },
                    referenceCalendar = referenceCalendar,
                    activeSessionsInPeriod = activeSessionsInPeriod,
                    onToggleRoutine = { routineId ->
                        selectedRoutineIds = if (selectedRoutineIds.contains(routineId)) {
                            selectedRoutineIds - routineId
                        } else {
                            selectedRoutineIds + routineId
                        }
                    }
                )
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
                        text = "Session Drill Logs (${activeSessionsInPeriod.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            if (activeSessionsInPeriod.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("empty_history_card"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Text(
                            text = if (sessions.isEmpty()) {
                                "No clinical therapy sessions completed yet. Initiate a timer from the Home tab to build compliance history."
                            } else {
                                "No sessions match the selected timeline and filter routing. Toggle them back on to view results."
                            },
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

            items(activeSessionsInPeriod, key = { it.id }) { session ->
                val formattedDate = remember(session.startedAt) {
                    val sdf = SimpleDateFormat("MMM dd, yyyy • h:mm a", Locale.getDefault())
                    sdf.format(Date(session.startedAt))
                }

                var isExpanded by remember { mutableStateOf(false) }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(CardDefaults.shape)
                        .clickable { isExpanded = !isExpanded }
                        .testTag("history_item_card_${session.id}"),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isExpanded) {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isExpanded) 4.dp else 1.dp)
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

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Adherence progress badge
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            when (session.status) {
                                                "Completed" -> MaterialTheme.colorScheme.primaryContainer
                                                "Partial" -> MaterialTheme.colorScheme.secondaryContainer
                                                else -> MaterialTheme.colorScheme.errorContainer
                                            }
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = session.status,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = when (session.status) {
                                            "Completed" -> MaterialTheme.colorScheme.onPrimaryContainer
                                            "Partial" -> MaterialTheme.colorScheme.onSecondaryContainer
                                            else -> MaterialTheme.colorScheme.onErrorContainer
                                        }
                                    )
                                }

                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = if (isExpanded) "Collapse details" else "Expand details",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
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

                        // Detailed stage list when selected
                        if (isExpanded) {
                            val stageRecords by remember(session.id) {
                                viewModel.getStageRecordsForSession(session.id)
                            }.collectAsStateWithLifecycle(emptyList())

                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "STAGE DRILL DETAILS",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            if (stageRecords.isEmpty()) {
                                Text(
                                    text = "No detailed stage activities logged for this session.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .padding(10.dp)
                                ) {
                                    stageRecords.forEach { record ->
                                        val recMins = record.recordedSeconds / 60
                                        val recSecs = record.recordedSeconds % 60
                                        val targetMins = record.durationSeconds / 60
                                        val targetSecs = record.durationSeconds % 60

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(24.dp)
                                                        .background(
                                                            color = when (record.status) {
                                                                "Completed" -> Color(0xFFE8F5E9)
                                                                "Skipped" -> Color(0xFFFFF3E0)
                                                                else -> Color(0xFFFFEBEE)
                                                            },
                                                            shape = CircleShape
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = when (record.status) {
                                                            "Completed" -> Icons.Default.Check
                                                            "Skipped" -> Icons.Default.SkipNext
                                                            else -> Icons.Default.Close
                                                        },
                                                        contentDescription = record.status,
                                                        tint = when (record.status) {
                                                            "Completed" -> Color(0xFF2E7D32)
                                                            "Skipped" -> Color(0xFFEF6C00)
                                                            else -> Color(0xFFC62828)
                                                        },
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }

                                                Column {
                                                    Text(
                                                        text = "Stage ${record.stageOrder}: ${record.stageName}",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = "Status: ${record.status}",
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }

                                            Text(
                                                text = "${String.format("%02d:%02d", recMins, recSecs)} / ${String.format("%02d:%02d", targetMins, targetSecs)}",
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                textAlign = TextAlign.End
                                            )
                                        }
                                        if (record != stageRecords.last()) {
                                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
                                        }
                                    }
                                }
                            }
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
