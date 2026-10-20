package com.example.ui.screens.planner

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.core.database.entities.StudyTaskEntity
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ExamRescueTopAppBar
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningOrange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannerScreen(
    viewModel: PlannerViewModel,
    onNavigateToSession: (taskId: String?) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            ExamRescueTopAppBar(
                title = stringResource(R.string.planner_title),
                onNavigateSettings = onNavigateToSettings
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.setShowAddTaskDialog(true) },
                modifier = Modifier.testTag("add_task_fab"),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = stringResource(R.string.home_add_task))
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // View Switcher & Action Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SingleChoiceSegmentedButtonRow {
                    SegmentedButton(
                        selected = !state.isWeekView,
                        onClick = { viewModel.setWeekView(false) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) {
                        Text(stringResource(R.string.planner_day_view))
                    }
                    SegmentedButton(
                        selected = state.isWeekView,
                        onClick = { viewModel.setWeekView(true) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) {
                        Text(stringResource(R.string.planner_week_view))
                    }
                }

                Button(
                    onClick = { viewModel.generatePlan() },
                    enabled = !state.isGenerating,
                    modifier = Modifier.testTag("generate_plan_button")
                ) {
                    if (state.isGenerating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(imageVector = Icons.Default.AutoMode, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.action_generate_plan), style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            // Tasks List
            if (state.tasks.isEmpty()) {
                EmptyStateCard(
                    icon = Icons.Default.CalendarToday,
                    title = "No Planned Tasks Yet",
                    description = "Generate a prioritized study schedule based on your O/L exam dates and weak topics, or create your own custom tasks.",
                    actionLabel = stringResource(R.string.action_generate_plan),
                    onActionClick = { viewModel.generatePlan() }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(state.tasks, key = { it.id }) { task ->
                        PlannerTaskCard(
                            task = task,
                            onToggleComplete = { viewModel.toggleTaskComplete(task) },
                            onStartSession = { onNavigateToSession(task.id) },
                            onDelete = { viewModel.deleteTask(task.id) }
                        )
                    }
                }
            }
        }
    }

    if (state.showAddTaskDialog) {
        AddTaskDialog(
            subjects = state.subjects,
            onDismiss = { viewModel.setShowAddTaskDialog(false) },
            onConfirm = { subjectId, title, minutes, taskType, priority ->
                viewModel.createCustomTask(subjectId, title, minutes, taskType, priority)
            }
        )
    }
}

@Composable
private fun PlannerTaskCard(
    task: StudyTaskEntity,
    onToggleComplete: () -> Unit,
    onStartSession: () -> Unit,
    onDelete: () -> Unit
) {
    val isCompleted = task.status == "COMPLETED"
    val (priorityLabel, priorityColor) = when (task.priority) {
        3 -> "High" to ErrorRed
        2 -> "Medium" to WarningOrange
        else -> "Low" to SuccessGreen
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = isCompleted,
                    onCheckedChange = { onToggleComplete() },
                    modifier = Modifier.testTag("planner_task_check_${task.id}")
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = priorityColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = priorityLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = priorityColor,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${task.plannedMinutes} mins • ${task.taskType}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (!isCompleted) {
                    IconButton(onClick = onStartSession) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Start Timer",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = stringResource(R.string.action_delete),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            if (!task.reasonExplanation.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Why suggested: ${task.reasonExplanation}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                    modifier = Modifier.padding(start = 40.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddTaskDialog(
    subjects: List<com.example.core.database.entities.SubjectEntity>,
    onDismiss: () -> Unit,
    onConfirm: (subjectId: String, title: String, minutes: Int, taskType: String, priority: Int) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedSubjectId by remember { mutableStateOf(subjects.firstOrNull()?.id ?: "math") }
    var selectedType by remember { mutableStateOf("REVIEW") }
    var minutes by remember { mutableStateOf("25") }
    var priority by remember { mutableStateOf(2) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.home_add_task)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("task_title_input")
                )
                OutlinedTextField(
                    value = minutes,
                    onValueChange = { minutes = it.filter { char -> char.isDigit() } },
                    label = { Text("Planned Duration (Minutes)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("task_duration_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onConfirm(selectedSubjectId, title, minutes.toIntOrNull() ?: 25, selectedType, priority)
                    }
                },
                modifier = Modifier.testTag("confirm_add_task_button")
            ) {
                Text(stringResource(R.string.action_add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
