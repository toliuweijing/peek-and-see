package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    onNavigateToHistoryManagement: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Home/Training, 1: History, 2: Profile
    val context = LocalContext.current
    val alertManager = remember { AlertManager(context) }

    val routines by viewModel.routines.collectAsStateWithLifecycle()
    val profileName by viewModel.profileName.collectAsStateWithLifecycle()
    val activeProfileId by viewModel.activeProfileId.collectAsStateWithLifecycle()
    var expandedRoutineId by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = when (selectedTab) {
                            0 -> {
                                if (profileName.isNotBlank() && profileName != "Amblyopia Patient") {
                                    "$profileName's Drills"
                                } else {
                                    "Clinician Drills"
                                }
                            }
                            1 -> "Compliance Logs"
                            else -> "Lazy Eye Profile"
                        },
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = when (selectedTab) {
                            0 -> "Therapeutic gymnastic routines for active eye focus"
                            1 -> "Track historic on-device workout logs & adherence"
                            else -> "Configure offline diagnostics & audio alarm tones"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (selectedTab == 2) {
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = { viewModel.onSaveProfileClick?.invoke() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("save_profile_btn"),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "Save profile info",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .navigationBarsPadding()
                    .testTag("main_bottom_nav_bar"),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(imageVector = if (selectedTab == 0) Icons.Default.Home else Icons.Default.Home, contentDescription = null) },
                    label = { Text("Exercises", style = MaterialTheme.typography.labelMedium) },
                    modifier = Modifier.testTag("nav_tab_home")
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(imageVector = if (selectedTab == 1) Icons.Default.History else Icons.Default.History, contentDescription = null) },
                    label = { Text("Compliance", style = MaterialTheme.typography.labelMedium) },
                    modifier = Modifier.testTag("nav_tab_history")
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(imageVector = if (selectedTab == 2) Icons.Default.Person else Icons.Default.Person, contentDescription = null) },
                    label = { Text("Diagnostics", style = MaterialTheme.typography.labelMedium) },
                    modifier = Modifier.testTag("nav_tab_profile")
                )
            }
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                LargeFloatingActionButton(
                    onClick = onNavigateToCreateRoutine,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.testTag("add_routine_fab")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Create new routine layout",
                        modifier = Modifier.size(36.dp)
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
                        if (routines.isEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp, vertical = 8.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)),
                                    shape = RoundedCornerShape(24.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f))
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(56.dp)
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Visibility,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.secondary,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                        Text(
                                            text = "Design Eye gymnastics Drills",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            text = "No routines designed yet. Touch the (+) button below to create custom clinical exercises, patch intervals, near-focus tracking, and reading drills prescribed for your amblyopia treatment.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                                            textAlign = TextAlign.Center,
                                            lineHeight = 20.sp
                                        )
                                    }
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
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isExpanded) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    width = if (isExpanded) 2.dp else 1.dp,
                                    color = if (isExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                                )
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
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .background(MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(6.dp))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = "${routine.stages.size} Stage${if (routine.stages.size != 1) "s" else ""}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                                    )
                                                }
                                                if (routine.routine.autoRepeat) {
                                                    Box(
                                                        modifier = Modifier
                                                            .background(MaterialTheme.colorScheme.tertiaryContainer, shape = RoundedCornerShape(6.dp))
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Replay,
                                                                contentDescription = null,
                                                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                                                modifier = Modifier.size(11.dp)
                                                            )
                                                            Text(
                                                                text = "Auto-Repeat",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                fontWeight = FontWeight.Bold,
                                                                color = MaterialTheme.colorScheme.onTertiaryContainer
                                                            )
                                                        }
                                                    }
                                                }
                                                Text(
                                                    text = "• $totalMinutes mins total",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Medium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        IconButton(
                                            onClick = { expandedRoutineId = if (isExpanded) null else routine.routine.id },
                                            colors = IconButtonDefaults.iconButtonColors(
                                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        ) {
                                            Icon(
                                                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                                contentDescription = "Expand routine drill tasks"
                                            )
                                        }
                                    }

                                    Text(
                                        text = routine.routine.description.ifEmpty { "Clinical exercise design elements." },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                                            Spacer(modifier = Modifier.height(8.dp))

                                            Text(
                                                text = "Exercises Timeline:",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))

                                            routine.sortedStages.forEachIndexed { stageIdx, stage ->
                                                val durationStr = if (stage.durationSeconds >= 60) "${stage.durationSeconds / 60}m" else "${stage.durationSeconds}s"
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .background(
                                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                                            shape = RoundedCornerShape(8.dp)
                                                        )
                                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                                        .padding(vertical = 2.dp),
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
                                                                .size(20.dp)
                                                                .background(MaterialTheme.colorScheme.secondaryContainer, shape = CircleShape),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text(
                                                                text = "${stageIdx + 1}",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                fontWeight = FontWeight.Bold,
                                                                color = MaterialTheme.colorScheme.onSecondaryContainer
                                                            )
                                                        }
                                                        Text(
                                                            text = stage.name,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            color = MaterialTheme.colorScheme.onSurface,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                    Text(
                                                        text = "$durationStr (Start: ${stage.soundProfileStart} • End: ${stage.soundProfileEnd})",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                            }

                                            Spacer(modifier = Modifier.height(14.dp))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Button(
                                                    onClick = { onStartTraining(routine) },
                                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                                    shape = RoundedCornerShape(10.dp),
                                                    modifier = Modifier
                                                        .weight(2f)
                                                        .height(40.dp)
                                                        .testTag("start_btn_${routine.routine.id}")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.PlayArrow,
                                                        contentDescription = "Start training loop button",
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Start Drill", style = MaterialTheme.typography.labelLarge)
                                                }

                                                IconButton(
                                                    onClick = { onNavigateToEditRoutine(routine.routine.id) },
                                                    colors = IconButtonDefaults.filledIconButtonColors(
                                                        containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                                                    ),
                                                    modifier = Modifier.size(40.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Edit,
                                                        contentDescription = "Edit clinician prescribed stages",
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }

                                                IconButton(
                                                    onClick = { viewModel.deleteRoutine(routine.routine.id) },
                                                    colors = IconButtonDefaults.filledIconButtonColors(
                                                        containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
                                                    ),
                                                    modifier = Modifier.size(40.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = "Delete routine template",
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
                1 -> {
                    HistoryScreen(
                        viewModel = viewModel,
                        onNavigateToHistoryManagement = onNavigateToHistoryManagement
                    )
                }
                2 -> {
                    ProfileScreen(viewModel = viewModel)
                }
            }
        }
    }
}
