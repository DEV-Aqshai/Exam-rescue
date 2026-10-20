package com.example.ui.screens.exams

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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ExamRescueTopAppBar
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ExamsScreen(
    viewModel: ExamsViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val now = System.currentTimeMillis()

    Scaffold(
        topBar = {
            ExamRescueTopAppBar(
                title = stringResource(R.string.exams_title),
                onNavigateBack = onNavigateBack
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.setShowAddDialog(true) },
                modifier = Modifier.testTag("add_exam_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = stringResource(R.string.exams_add))
            }
        }
    ) { paddingValues ->
        if (state.exams.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                EmptyStateCard(
                    icon = Icons.Default.EventNote,
                    title = stringResource(R.string.exam_no_upcoming),
                    description = "Add your official GCE O/L exam dates to activate smart countdowns and proximity-based revision planning.",
                    actionLabel = stringResource(R.string.exams_add),
                    onActionClick = { viewModel.setShowAddDialog(true) }
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(state.exams, key = { it.id }) { exam ->
                    val diff = exam.examAt - now
                    val daysLeft = if (diff > 0) (diff / (86_400_000L)).toInt() else 0
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "$daysLeft",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = "days",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = exam.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(Date(exam.examAt)),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (!exam.locationNote.isNullOrBlank()) {
                                    Text(
                                        text = exam.locationNote,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                            IconButton(onClick = { viewModel.deleteExam(exam.id) }) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = stringResource(R.string.action_delete),
                                    tint = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (state.showAddDialog) {
        var title by remember { mutableStateOf("") }
        var selectedSubjectId by remember { mutableStateOf(state.subjects.firstOrNull()?.id ?: "math") }
        var daysAhead by remember { mutableStateOf("60") }

        AlertDialog(
            onDismissRequest = { viewModel.setShowAddDialog(false) },
            title = { Text(stringResource(R.string.exams_add)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Exam Paper Title (e.g., Mathematics Paper I & II)") },
                        modifier = Modifier.fillMaxWidth().testTag("exam_title_input")
                    )
                    OutlinedTextField(
                        value = daysAhead,
                        onValueChange = { daysAhead = it.filter { c -> c.isDigit() } },
                        label = { Text("Days from today") },
                        modifier = Modifier.fillMaxWidth().testTag("exam_days_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            val days = daysAhead.toLongOrNull() ?: 30L
                            val examDate = System.currentTimeMillis() + (days * 86_400_000L)
                            viewModel.addExam(selectedSubjectId, title, examDate, null)
                        }
                    },
                    modifier = Modifier.testTag("confirm_add_exam_button")
                ) {
                    Text(stringResource(R.string.action_add))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.setShowAddDialog(false) }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}
