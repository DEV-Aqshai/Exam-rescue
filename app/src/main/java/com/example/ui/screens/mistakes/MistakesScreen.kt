package com.example.ui.screens.mistakes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.core.database.entities.MistakeEntity
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ExamRescueTopAppBar
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.SuccessGreen

@Composable
fun MistakesScreen(
    viewModel: MistakesViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            ExamRescueTopAppBar(
                title = stringResource(R.string.mistakes_title),
                onNavigateBack = onNavigateBack
            )
        }
    ) { paddingValues ->
        if (state.mistakes.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                EmptyStateCard(
                    icon = Icons.Default.CheckCircleOutline,
                    title = "Mistake Notebook is Clean!",
                    description = stringResource(R.string.mistakes_empty)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = stringResource(R.string.mistakes_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                items(state.mistakes, key = { it.id }) { mistake ->
                    MistakeItemCard(
                        mistake = mistake,
                        onMarkUnderstood = { viewModel.markUnderstood(mistake.id) },
                        onMarkNeedsPractice = { viewModel.markNeedsPractice(mistake.id) },
                        onConvertToFlashcard = { viewModel.convertToFlashcard(mistake) },
                        onConvertToTask = { viewModel.convertToStudyTask(mistake) },
                        onDelete = { viewModel.deleteMistake(mistake.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MistakeItemCard(
    mistake: MistakeEntity,
    onMarkUnderstood: () -> Unit,
    onMarkNeedsPractice: () -> Unit,
    onConvertToFlashcard: () -> Unit,
    onConvertToTask: () -> Unit,
    onDelete: () -> Unit
) {
    val isUnderstood = mistake.status == "UNDERSTOOD"

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().testTag("mistake_card_${mistake.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = if (isUnderstood) SuccessGreen.copy(alpha = 0.15f) else ErrorRed.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (isUnderstood) stringResource(R.string.mistakes_status_understood) else stringResource(R.string.mistakes_status_active),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isUnderstood) SuccessGreen else ErrorRed,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.outline)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = mistake.promptSnapshot,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Student's Wrong Answer
            Text(
                text = stringResource(R.string.mistakes_your_answer, mistake.studentAnswer),
                style = MaterialTheme.typography.bodyMedium,
                color = ErrorRed
            )
            // Correct Reference Answer
            Text(
                text = stringResource(R.string.mistakes_correct_answer, mistake.answerSnapshot),
                style = MaterialTheme.typography.bodyMedium,
                color = SuccessGreen,
                fontWeight = FontWeight.Medium
            )

            if (mistake.explanationSnapshot.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = mistake.explanationSnapshot,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isUnderstood) {
                    FilledTonalButton(
                        onClick = onMarkUnderstood,
                        modifier = Modifier.weight(1f).testTag("mark_understood_button")
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.action_understood), style = MaterialTheme.typography.labelSmall)
                    }
                } else {
                    OutlinedButton(
                        onClick = onMarkNeedsPractice,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.action_practise_again), style = MaterialTheme.typography.labelSmall)
                    }
                }

                AssistChip(
                    onClick = onConvertToFlashcard,
                    label = { Text("Make Card", style = MaterialTheme.typography.labelSmall) },
                    leadingIcon = { Icon(imageVector = Icons.Default.Style, contentDescription = null, modifier = Modifier.size(14.dp)) }
                )
                AssistChip(
                    onClick = onConvertToTask,
                    label = { Text("Add Task", style = MaterialTheme.typography.labelSmall) },
                    leadingIcon = { Icon(imageVector = Icons.Default.AddAlarm, contentDescription = null, modifier = Modifier.size(14.dp)) }
                )
            }
        }
    }
}
