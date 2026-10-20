package com.example.ui.screens.session

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.components.ConfidenceRatingBar
import com.example.ui.components.ExamRescueTopAppBar

@Composable
fun StudySessionScreen(
    taskId: String?,
    subjectId: String?,
    viewModel: StudySessionViewModel,
    onNavigateBack: () -> Unit
) {
    LaunchedEffect(taskId, subjectId) {
        viewModel.setupSession(taskId, subjectId)
    }

    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            ExamRescueTopAppBar(
                title = stringResource(R.string.session_title),
                onNavigateBack = onNavigateBack
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Session Type Selector when Idle
            if (state.timerState == TimerState.IDLE) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Choose Study Focus",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    val types = listOf(
                        "LEARN" to stringResource(R.string.session_type_learn),
                        "RECALL" to stringResource(R.string.session_type_recall),
                        "QUESTIONS" to stringResource(R.string.session_type_questions),
                        "REVIEW" to stringResource(R.string.session_type_review)
                    )
                    types.forEach { (typeKey, label) ->
                        OutlinedButton(
                            onClick = { viewModel.startTimer(typeKey, 25) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .testTag("start_session_type_$typeKey"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(label, modifier = Modifier.padding(vertical = 4.dp))
                        }
                    }
                }
            } else {
                // Large Digital Timer Display
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 40.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "FOCUS: ${state.sessionType}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    val minutes = state.secondsElapsed / 60
                    val seconds = state.secondsElapsed % 60
                    Text(
                        text = String.format("%02d:%02d", minutes, seconds),
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontSize = 64.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.testTag("timer_display_text")
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = when (state.timerState) {
                            TimerState.RUNNING -> "Session in progress..."
                            TimerState.PAUSED -> "Session paused"
                            else -> "Session complete"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Timer Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (state.timerState == TimerState.RUNNING) {
                        FilledTonalIconButton(
                            onClick = { viewModel.pauseTimer() },
                            modifier = Modifier.size(64.dp).testTag("pause_timer_button")
                        ) {
                            Icon(imageVector = Icons.Default.Pause, contentDescription = "Pause", modifier = Modifier.size(32.dp))
                        }
                    } else if (state.timerState == TimerState.PAUSED) {
                        FilledTonalIconButton(
                            onClick = { viewModel.resumeTimer() },
                            modifier = Modifier.size(64.dp).testTag("resume_timer_button")
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Resume", modifier = Modifier.size(32.dp))
                        }
                    }

                    Button(
                        onClick = { viewModel.finishSession() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = CircleShape,
                        modifier = Modifier.height(56.dp).padding(horizontal = 16.dp).testTag("finish_session_button")
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.session_finish))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (state.showSummaryDialog) {
        var rating by remember { mutableStateOf(4) }
        var reflectionNotes by remember { mutableStateOf("") }
        val minutesStudied = maxOf(1, state.secondsElapsed / 60)

        AlertDialog(
            onDismissRequest = { /* Require student confirmation */ },
            title = { Text(stringResource(R.string.session_completed_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = stringResource(R.string.session_duration_label, minutesStudied),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(R.string.session_rate_understanding),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    ConfidenceRatingBar(
                        confidence = rating,
                        onConfidenceChanged = { rating = it }
                    )
                    OutlinedTextField(
                        value = reflectionNotes,
                        onValueChange = { reflectionNotes = it },
                        placeholder = { Text(stringResource(R.string.session_notes_placeholder)) },
                        modifier = Modifier.fillMaxWidth().testTag("session_notes_input"),
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveSummaryAndExit(rating, reflectionNotes, onNavigateBack)
                    },
                    modifier = Modifier.testTag("save_session_summary_button")
                ) {
                    Text(stringResource(R.string.session_save_summary))
                }
            }
        )
    }
}
