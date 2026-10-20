package com.example.ui.screens.subjects

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.core.database.entities.TopicEntity
import com.example.ui.components.ConfidenceRatingBar
import com.example.ui.components.ExamRescueTopAppBar
import com.example.ui.components.StatusBadge

@Composable
fun SubjectsScreen(
    viewModel: SubjectsViewModel,
    onNavigateToSubjectDetail: (subjectId: String) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            ExamRescueTopAppBar(
                title = stringResource(R.string.subjects_title),
                onNavigateBack = onNavigateBack,
                onNavigateSettings = onNavigateToSettings
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.setShowAddSubjectDialog(true) },
                modifier = Modifier.testTag("add_subject_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = stringResource(R.string.subjects_add_subject))
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.sample_syllabus_notice),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            items(state.subjectsWithTopics, key = { it.subject.id }) { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToSubjectDetail(item.subject.id) }
                        .testTag("subject_card_${item.subject.id}"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.subject.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${item.topics.size} Topics • Avg Rating: ${String.format("%.1f", item.averageConfidence)}/5",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }

    if (state.showAddSubjectDialog) {
        var subjectName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { viewModel.setShowAddSubjectDialog(false) },
            title = { Text(stringResource(R.string.subjects_add_subject)) },
            text = {
                OutlinedTextField(
                    value = subjectName,
                    onValueChange = { subjectName = it },
                    label = { Text("Subject Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("subject_name_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (subjectName.isNotBlank()) {
                            viewModel.addCustomSubject(subjectName)
                        }
                    },
                    modifier = Modifier.testTag("confirm_add_subject_button")
                ) {
                    Text(stringResource(R.string.action_add))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.setShowAddSubjectDialog(false) }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
fun SubjectDetailScreen(
    subjectId: String,
    viewModel: SubjectsViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val subjectItem = state.subjectsWithTopics.firstOrNull { it.subject.id == subjectId }
    var showAddTopicDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            ExamRescueTopAppBar(
                title = subjectItem?.subject?.name ?: stringResource(R.string.subjects_title),
                onNavigateBack = onNavigateBack
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddTopicDialog = true },
                modifier = Modifier.testTag("add_topic_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = stringResource(R.string.subjects_add_topic))
            }
        }
    ) { paddingValues ->
        if (subjectItem == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(subjectItem.topics, key = { it.id }) { topic ->
                    TopicDetailCard(
                        topic = topic,
                        onConfidenceChanged = { rating -> viewModel.updateTopicConfidence(topic.id, rating) }
                    )
                }
            }
        }
    }

    if (showAddTopicDialog) {
        var topicTitle by remember { mutableStateOf("") }
        var topicDesc by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddTopicDialog = false },
            title = { Text(stringResource(R.string.subjects_add_topic)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = topicTitle,
                        onValueChange = { topicTitle = it },
                        label = { Text("Topic Title") },
                        modifier = Modifier.fillMaxWidth().testTag("topic_title_input")
                    )
                    OutlinedTextField(
                        value = topicDesc,
                        onValueChange = { topicDesc = it },
                        label = { Text("Description (Optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (topicTitle.isNotBlank()) {
                            viewModel.addCustomTopic(subjectId, topicTitle, topicDesc)
                            showAddTopicDialog = false
                        }
                    },
                    modifier = Modifier.testTag("confirm_add_topic_button")
                ) {
                    Text(stringResource(R.string.action_add))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTopicDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun TopicDetailCard(
    topic: TopicEntity,
    onConfidenceChanged: (Int) -> Unit
) {
    var rating by remember { mutableStateOf(2) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = topic.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            if (topic.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = topic.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ConfidenceRatingBar(
                    confidence = rating,
                    onConfidenceChanged = { newRating ->
                        rating = newRating
                        onConfidenceChanged(newRating)
                    }
                )
                StatusBadge(
                    status = when {
                        rating >= 4 -> "CONFIDENT"
                        rating >= 2 -> "REVISING"
                        else -> "LEARNING"
                    }
                )
            }
        }
    }
}
