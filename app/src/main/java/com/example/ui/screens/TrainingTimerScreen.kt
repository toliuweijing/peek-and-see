package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.viewmodel.MainViewModel
import com.example.viewmodel.TrainingState

@Composable
fun KeepScreenOn() {
    val currentView = LocalView.current
    DisposableEffect(currentView) {
        currentView.keepScreenOn = true
        onDispose {
            currentView.keepScreenOn = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingTimerScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    // Keep our device screen awake during workouts so children/parents don't lose progress
    KeepScreenOn()

    val trainingState by viewModel.trainingState.collectAsStateWithLifecycle()
    val activeRoutine by viewModel.activeRoutine.collectAsStateWithLifecycle()
    val currentStageIdx by viewModel.currentStageIndex.collectAsStateWithLifecycle()
    val remainingSecs by viewModel.currentStageRemainingSeconds.collectAsStateWithLifecycle()
    val progress by viewModel.currentStageProgress.collectAsStateWithLifecycle()

    var showCancelDialog by remember { mutableStateOf(false) }

    val routine = activeRoutine ?: return
    val currentStage = routine.sortedStages.getOrNull(currentStageIdx) ?: return

    // Standard Android back press handler -> prompts warning dialog
    BackHandler {
        showCancelDialog = true
    }

    // Flash background animation when stage expires
    val infiniteTransition = rememberInfiniteTransition(label = "expire_flash")
    val badgeColor by infiniteTransition.animateColor(
        initialValue = MaterialTheme.colorScheme.errorContainer,
        targetValue = MaterialTheme.colorScheme.error,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flash_color"
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = routine.routine.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "${currentStageIdx + 1}/${routine.stages.size}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            if (routine.routine.autoRepeat) {
                                Icon(
                                    imageVector = Icons.Default.Replay,
                                    contentDescription = "Auto-repeat active",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
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
                .background(
                    if (trainingState == TrainingState.EXPIRED_WAITING) {
                        badgeColor.copy(alpha = 0.08f)
                    } else {
                        MaterialTheme.colorScheme.background
                    }
                )
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            
            // --- HEADER STAGE LABEL ---
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = currentStage.name,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("timer_stage_title")
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = "Alert sound effect",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "Sounds - Start: ${currentStage.soundProfileStart} • End: ${currentStage.soundProfileEnd}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            // --- LARGE CIRCULAR COUNTDOWN GRAPHIC ---
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(240.dp)
                    .testTag("circular_timer_box")
            ) {
                // Background Track
                CircularProgressIndicator(
                    progress = { 1.0f },
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    strokeWidth = 14.dp
                )

                // Active Progress Track
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxSize(),
                    color = when (trainingState) {
                        TrainingState.PAUSED -> MaterialTheme.colorScheme.secondary
                        TrainingState.EXPIRED_WAITING -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.primary
                    },
                    strokeWidth = 14.dp
                )

                // Countdown Text Content
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val minutes = remainingSecs / 60
                    val seconds = remainingSecs % 60
                    
                    Text(
                        text = String.format("%02d:%02d", minutes, seconds),
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Serif,
                        color = if (trainingState == TrainingState.EXPIRED_WAITING) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.testTag("timer_countdown_text")
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = when (trainingState) {
                            TrainingState.PAUSED -> "PAUSED"
                            TrainingState.EXPIRED_WAITING -> "TIME EXPIRED"
                            else -> "REMAINING"
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp,
                        color = when (trainingState) {
                            TrainingState.PAUSED -> MaterialTheme.colorScheme.secondary
                            TrainingState.EXPIRED_WAITING -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        }
                    )
                }
            }

            // --- CLINICAL INSTRUCTION DIALS ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (trainingState == TrainingState.EXPIRED_WAITING) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Directions,
                            contentDescription = "Guideline target action description icon",
                            tint = if (trainingState == TrainingState.EXPIRED_WAITING) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (trainingState == TrainingState.EXPIRED_WAITING) "Interval Complete! Wait until done before clicking" else "Clinician Instructions",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (trainingState == TrainingState.EXPIRED_WAITING) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = if (trainingState == TrainingState.EXPIRED_WAITING) {
                            "The alarm sound is ringing loudly so you don't miss it. Ensure full compliance, then click 'Continue' to advance manually."
                        } else {
                            currentStage.instruction.ifEmpty { "Perform the eye focus and patching drills as recommended." }
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (trainingState == TrainingState.EXPIRED_WAITING) MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // --- PHYSICAL GATED CONTROLS BUTTONS ---
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (trainingState) {
                    TrainingState.RUNNING -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { viewModel.pauseSession() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("pause_timer_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Icon(imageVector = Icons.Default.Pause, contentDescription = "Pause session timer")
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Pause Drill", fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = { viewModel.skipCurrentStage() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("skip_stage_btn")
                            ) {
                                Icon(imageVector = Icons.Default.SkipNext, contentDescription = "Skip this exercise step")
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Skip Stage", fontWeight = FontWeight.Medium)
                            }
                        }
                    }

                    TrainingState.PAUSED -> {
                        Button(
                            onClick = { viewModel.resumeSession() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("resume_timer_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Resume session timer")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Resume Drill", fontWeight = FontWeight.Bold)
                        }
                    }

                    TrainingState.EXPIRED_WAITING -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { viewModel.proceedToNextStage(repeat = false) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .testTag("gate_proceed_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                val isNotLast = (currentStageIdx + 1) < routine.stages.size
                                Icon(
                                    imageVector = if (isNotLast || routine.routine.autoRepeat) Icons.Default.NavigateNext else Icons.Default.Check,
                                    contentDescription = "Advance to the next clinic stage manually"
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isNotLast) {
                                        "Continue to Next Stage"
                                    } else if (routine.routine.autoRepeat) {
                                        "Repeat Routine (Stage 1)"
                                    } else {
                                        "Proceed to Log Summary"
                                    },
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.proceedToNextStage(repeat = true) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(46.dp)
                                        .testTag("gate_repeat_btn")
                                ) {
                                    Icon(imageVector = Icons.Default.Replay, contentDescription = "Rerun current interval step")
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Repeat Stage", fontSize = 13.sp)
                                }

                                OutlinedButton(
                                    onClick = { viewModel.stopAudioAlarm() },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(46.dp)
                                        .testTag("gate_mute_btn"),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Icon(imageVector = Icons.Default.VolumeOff, contentDescription = "Silence audio tone ringing")
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Mute Alert", fontSize = 13.sp)
                                }
                            }
                        }
                    }
                    else -> {}
                }

                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 4.dp))

                // --- TERMINATE / QUIT TRIGGER ---
                TextButton(
                    onClick = { showCancelDialog = true },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(imageVector = Icons.Default.Stop, contentDescription = "Exit active workout early")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("End Session Early", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // --- DIALOGS CONTROLS ---
    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = {
                Text(
                    text = "End session early?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(text = "Are you sure you want to stop this training session? We will still save a partial compliance log history for your clinician, but you will lose detailed progression tracking on current stage.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCancelDialog = false
                        viewModel.endSessionEarly()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("End & Log Workout", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
