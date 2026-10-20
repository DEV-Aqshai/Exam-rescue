package com.example.ui.screens.practise

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.core.database.entities.QuestionEntity
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ExamRescueTopAppBar
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.SuccessGreen
import org.json.JSONArray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PractiseScreen(
    viewModel: PractiseViewModel,
    onNavigateToMistakes: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            ExamRescueTopAppBar(
                title = stringResource(R.string.practise_title),
                onNavigateSettings = onNavigateToSettings,
                actions = {
                    IconButton(onClick = onNavigateToMistakes) {
                        Icon(imageVector = Icons.Default.BookmarkBorder, contentDescription = "Mistakes")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.setShowAddDialog(true) },
                modifier = Modifier.testTag("add_custom_question_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = stringResource(R.string.practise_add_custom))
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Subject Filter Chips
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = state.selectedSubjectId == null,
                        onClick = { viewModel.selectSubject(null) },
                        label = { Text(stringResource(R.string.action_all)) }
                    )
                }
                items(state.subjects, key = { it.id }) { subject ->
                    FilterChip(
                        selected = state.selectedSubjectId == subject.id,
                        onClick = { viewModel.selectSubject(subject.id) },
                        label = { Text(subject.name) }
                    )
                }
            }

            if (state.questions.isEmpty()) {
                EmptyStateCard(
                    icon = Icons.Default.HelpOutline,
                    title = "No Questions Available",
                    description = stringResource(R.string.practise_no_questions),
                    actionLabel = stringResource(R.string.action_all),
                    onActionClick = { viewModel.selectSubject(null) }
                )
            } else {
                val currentQuestion = state.questions[state.currentIndex]
                val options = parseOptions(currentQuestion.optionsJson)

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Question Counter & Source Tag
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Question ${state.currentIndex + 1} of ${state.questions.size}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = currentQuestion.sourceAttribution ?: stringResource(R.string.practise_sample_tag),
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Question Prompt Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = currentQuestion.prompt,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(18.dp)
                        )
                    }

                    // Options List
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        options.forEach { option ->
                            val isSelected = state.selectedOption == option
                            val isCorrectAnswer = state.hasSubmitted && option.trim().equals(currentQuestion.answerText.trim(), ignoreCase = true)
                            val isWrongSelected = state.hasSubmitted && isSelected && !state.isCorrect

                            val (cardColor, borderColor) = when {
                                isCorrectAnswer -> SuccessGreen.copy(alpha = 0.15f) to SuccessGreen
                                isWrongSelected -> ErrorRed.copy(alpha = 0.15f) to ErrorRed
                                isSelected -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.surface to MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                            }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !state.hasSubmitted) { viewModel.selectOption(option) }
                                    .testTag("option_$option"),
                                colors = CardDefaults.cardColors(containerColor = cardColor),
                                border = BorderStroke(1.dp, borderColor),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { if (!state.hasSubmitted) viewModel.selectOption(option) },
                                        enabled = !state.hasSubmitted
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = option,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }

                    // Check Answer / Next Buttons
                    if (!state.hasSubmitted) {
                        Button(
                            onClick = { viewModel.submitAnswer() },
                            enabled = state.selectedOption != null,
                            modifier = Modifier.fillMaxWidth().testTag("check_answer_button")
                        ) {
                            Text(stringResource(R.string.practise_check_answer))
                        }
                    } else {
                        // Feedback & Solution Explanation Card
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (state.isCorrect) SuccessGreen.copy(alpha = 0.1f) else ErrorRed.copy(alpha = 0.1f)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = if (state.isCorrect) stringResource(R.string.practise_correct) else stringResource(R.string.practise_incorrect),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (state.isCorrect) SuccessGreen else ErrorRed
                                )
                                if (!state.isCorrect) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = stringResource(R.string.practise_saved_to_mistakes),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (currentQuestion.explanation.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = stringResource(R.string.practise_explanation),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = currentQuestion.explanation,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }

                        // Next Question Navigation Button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            if (state.currentIndex > 0) {
                                OutlinedButton(onClick = { viewModel.previousQuestion() }) {
                                    Text(stringResource(R.string.action_back))
                                }
                            } else Spacer(modifier = Modifier.width(1.dp))

                            if (state.currentIndex < state.questions.size - 1) {
                                Button(onClick = { viewModel.nextQuestion() }, modifier = Modifier.testTag("next_question_button")) {
                                    Text(stringResource(R.string.action_next))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }

    if (state.showAddDialog) {
        AddQuestionDialog(
            subjects = state.subjects,
            onDismiss = { viewModel.setShowAddDialog(false) },
            onConfirm = { subjectId, prompt, options, answer, explanation ->
                viewModel.addCustomQuestion(subjectId, prompt, options, answer, explanation)
            }
        )
    }
}

private fun parseOptions(json: String?): List<String> {
    if (json.isNullOrBlank()) return emptyList()
    return try {
        val array = JSONArray(json)
        val list = mutableListOf<String>()
        for (i in 0 until array.length()) {
            list.add(array.getString(i))
        }
        list
    } catch (_: Exception) {
        emptyList()
    }
}

@Composable
private fun AddQuestionDialog(
    subjects: List<com.example.core.database.entities.SubjectEntity>,
    onDismiss: () -> Unit,
    onConfirm: (subjectId: String, prompt: String, options: List<String>, answer: String, explanation: String) -> Unit
) {
    var prompt by remember { mutableStateOf("") }
    var option1 by remember { mutableStateOf("") }
    var option2 by remember { mutableStateOf("") }
    var option3 by remember { mutableStateOf("") }
    var option4 by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf("") }
    var explanation by remember { mutableStateOf("") }
    var selectedSubjectId by remember { mutableStateOf(subjects.firstOrNull()?.id ?: "math") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.practise_add_custom)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    label = { Text("Question Prompt") },
                    modifier = Modifier.fillMaxWidth().testTag("add_q_prompt_input")
                )
                OutlinedTextField(
                    value = option1,
                    onValueChange = { option1 = it },
                    label = { Text("Option A") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = option2,
                    onValueChange = { option2 = it },
                    label = { Text("Option B") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = option3,
                    onValueChange = { option3 = it },
                    label = { Text("Option C") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = option4,
                    onValueChange = { option4 = it },
                    label = { Text("Option D") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = answer,
                    onValueChange = { answer = it },
                    label = { Text("Correct Answer Text") },
                    modifier = Modifier.fillMaxWidth().testTag("add_q_answer_input")
                )
                OutlinedTextField(
                    value = explanation,
                    onValueChange = { explanation = it },
                    label = { Text("Explanation (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (prompt.isNotBlank() && answer.isNotBlank()) {
                        val opts = listOf(option1, option2, option3, option4).filter { it.isNotBlank() }
                        onConfirm(selectedSubjectId, prompt, opts, answer, explanation)
                    }
                },
                modifier = Modifier.testTag("confirm_add_question_button")
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
