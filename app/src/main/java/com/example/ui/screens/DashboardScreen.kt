package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.alert.AlertManager
import com.example.data.RoutineWithStages
import com.example.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onStartTraining: (RoutineWithStages) -> Unit,
    onNavigateToCreateRoutine: () -> Unit,
    onNavigateToEditRoutine: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Home/Training, 1: History, 2: Profile
    val context = LocalContext.current
    val alertManager = remember { AlertManager(context) }

    val routines by viewModel.routines.collectAsStateWithLifecycle()
    var expandedRoutineId by remember { mutableStateOf<Long?>(null) }
    var showSafetyNotice by remember { mutableStateOf(true) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (selectedTab) {
                            0 -> "Amblyopia Timer"
                            1 -> "Adherence History"
                            else -> "Patient Profile"
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                },
                actions = {
                    if (selectedTab == 0) {
                        IconButton(
                            onClick = {
                                alertManager.playLoudAlert("Gentle Chime")
                                // Stop after 2 seconds automatically to prevent alarm hanging
                                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                    alertManager.stopAll()
                                }, 2000)
                            },
                            modifier = Modifier.testTag("test_sound_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Test alarm volume"
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
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .navigationBarsPadding()
                    .testTag("main_bottom_nav_bar")
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(imageVector = Icons.Default.Home, contentDescription = null) },
                    label = { Text("Home", fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.testTag("nav_tab_home")
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(imageVector = Icons.Default.History, contentDescription = null) },
                    label = { Text("History", fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.testTag("nav_tab_history")
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(imageVector = Icons.Default.Person, contentDescription = null) },
                    label = { Text("Profile", fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.testTag("nav_tab_profile")
                )
            }
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = onNavigateToCreateRoutine,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("add_routine_fab")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Create new routine layout"
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
            when (selectedTab) {
                0 -> {
                    // Home/Routines view content
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // --- CLINICAL SAFETY COMPLIANCE HEADER ---
                        if (showSafetyNotice) {
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
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
                                                    imageVector = Icons.Default.Info,
                                                    contentDescription = "Medical Alert Indicator",
                                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                                )
                                                Text(
                                                    text = "Clinician Guideline / Safety Check",
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onErrorContainer
                                                )
                                            }
                                            IconButton(
                                                onClick = { showSafetyNotice = false },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Dismiss advice panel",
                                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "This app is an adherence tracker for prescribed clinical programs and eye gymnastics. It does not replace professional diagnosis. Use ONLY with a personalized routine from an ophthalmologist or optometrist.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                }
                            }
                        }

                        // --- SECTION HEADER ---
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ListAlt,
                                    contentDescription = "Routines section label",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Text(
                                    text = "Your Training Routines",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        if (routines.isEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Text(
                                        text = "No routines designed. Use the (+) button below to create your eye gymnastics drills.",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        items(routines, key = { it.routine.id }) { routine ->
                            val isExpanded = expandedRoutineId == routine.routine.id
                            val totalMinutes = routine.stages.sumOf { it.durationSeconds } / 60

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        expandedRoutineId = if (isExpanded) null else routine.routine.id
                                    }
                                    .testTag("routine_card_${routine.routine.id}"),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isExpanded) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = routine.routine.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp,
                                                color = if (isExpanded) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "${routine.stages.size} Stage${if (routine.stages.size != 1) "s" else ""} • $totalMinutes mins total",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = if (isExpanded) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        IconButton(onClick = { expandedRoutineId = if (isExpanded) null else routine.routine.id }) {
                                            Icon(
                                                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                                contentDescription = "Expand routine drill tasks"
                                            )
                                        }
                                    }

                                    Text(
                                        text = routine.routine.description,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (isExpanded) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = if (isExpanded) Int.MAX_VALUE else 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(top = 8.dp)
                                    )

                                    AnimatedVisibility(
                                        visible = isExpanded,
                                        enter = fadeIn() + expandVertically(),
                                        exit = fadeOut() + shrinkVertically()
                                    ) {
                                        Column(modifier = Modifier.padding(top = 12.dp)) {
                                            HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
                                            Spacer(modifier = Modifier.height(8.dp))

                                            Text(
                                                text = "Exercises Timeline:",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )

                                            routine.sortedStages.forEachIndexed { stageIdx, stage ->
                                                val durationStr = if (stage.durationSeconds >= 60) "${stage.durationSeconds / 60}m" else "${stage.durationSeconds}s"
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 4.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "${stageIdx + 1}. ${stage.name}",
                                                        fontSize = 13.sp,
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                        modifier = Modifier.weight(1f),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = "$durationStr (${stage.soundProfile})",
                                                        fontWeight = FontWeight.Medium,
                                                        fontSize = 12.sp,
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(14.dp))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Button(
                                                    onClick = { onStartTraining(routine) },
                                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .testTag("start_btn_${routine.routine.id}")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.PlayArrow,
                                                        contentDescription = "Start training loop button",
                                                        modifier = Modifier.padding(end = 4.dp)
                                                    )
                                                    Text("Start Drill", fontWeight = FontWeight.Bold)
                                                }

                                                IconButton(
                                                    onClick = { onNavigateToEditRoutine(routine.routine.id) },
                                                    colors = IconButtonDefaults.filledIconButtonColors(
                                                        containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                                                    )
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Edit,
                                                        contentDescription = "Edit clinician prescribed stages",
                                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                                    )
                                                }

                                                IconButton(
                                                    onClick = { viewModel.deleteRoutine(routine.routine.id) },
                                                    colors = IconButtonDefaults.filledIconButtonColors(
                                                        containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                                                    )
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = "Delete routine template",
                                                        tint = MaterialTheme.colorScheme.error
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
                1 -> {
                    HistoryScreen(viewModel = viewModel)
                }
                2 -> {
                    ProfileScreen(viewModel = viewModel)
                }
            }
        }
    }
}
