package com.example.ui.screens.tutor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import com.example.ai.agents.AgentType
import com.example.core.database.entities.AiProviderConfigEntity
import com.example.ui.components.ExamRescueTopAppBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TutorScreen(
    viewModel: TutorViewModel,
    activeProviderConfig: AiProviderConfigEntity?,
    onNavigateToSettings: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var inputQuery by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            ExamRescueTopAppBar(
                title = stringResource(R.string.tutor_title),
                onNavigateSettings = onNavigateToSettings,
                actions = {
                    // Language Switcher Badge (Tamil / English)
                    FilterChip(
                        selected = state.selectedLanguage == "TAMIL",
                        onClick = {
                            viewModel.setLanguage(if (state.selectedLanguage == "TAMIL") "ENGLISH" else "TAMIL")
                        },
                        label = {
                            Text(if (state.selectedLanguage == "TAMIL") "தமிழ்" else "English")
                        },
                        modifier = Modifier.padding(end = 8.dp).testTag("tutor_lang_switch")
                    )
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Quick Action Prompts
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    ) {
                        item {
                            SuggestionChip(
                                onClick = {
                                    inputQuery = "Explain this topic in simple terms with a real-life analogy."
                                    viewModel.setActiveAgent(AgentType.BILINGUAL_CONCEPT_TUTOR)
                                },
                                label = { Text(stringResource(R.string.tutor_quick_explain)) }
                            )
                        }
                        item {
                            SuggestionChip(
                                onClick = {
                                    inputQuery = "Give a worked Sri Lankan GCE O/L example with step-by-step solution."
                                    viewModel.setActiveAgent(AgentType.BILINGUAL_CONCEPT_TUTOR)
                                },
                                label = { Text(stringResource(R.string.tutor_quick_example)) }
                            )
                        }
                        item {
                            SuggestionChip(
                                onClick = {
                                    inputQuery = "Ask me one active recall question on this topic."
                                    viewModel.setActiveAgent(AgentType.ACTIVE_RECALL_COACH)
                                },
                                label = { Text(stringResource(R.string.tutor_quick_quiz)) }
                            )
                        }
                        item {
                            SuggestionChip(
                                onClick = {
                                    inputQuery = "Create 3 high-yield flashcard pairs from this concept."
                                    viewModel.setActiveAgent(AgentType.FLASHCARD_BUILDER)
                                },
                                label = { Text(stringResource(R.string.tutor_quick_flashcards)) }
                            )
                        }
                    }

                    // Input Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = inputQuery,
                            onValueChange = { inputQuery = it },
                            placeholder = { Text(stringResource(R.string.tutor_input_hint)) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("tutor_input_field"),
                            shape = RoundedCornerShape(24.dp),
                            maxLines = 3
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                val text = inputQuery
                                inputQuery = ""
                                viewModel.sendMessage(text, activeProviderConfig)
                            },
                            enabled = inputQuery.isNotBlank() && !state.isSending,
                            modifier = Modifier.testTag("tutor_send_button")
                        ) {
                            if (state.isSending) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = stringResource(R.string.tutor_send),
                                    tint = if (inputQuery.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Context Disclosure & Disclaimer Banner
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.tutor_disclaimer),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Error Bar if any
            if (state.errorMessage != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = state.errorMessage!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { viewModel.clearError() }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Dismiss", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // Chat Messages List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(state.messages, key = { it.id }) { msg ->
                    ChatBubble(message = msg, onSaveToNotes = { viewModel.saveLastReplyToNotes() })
                }

                // AI Proposal Approval Card
                if (state.pendingProposalResult != null) {
                    item {
                        ProposalCard(
                            result = state.pendingProposalResult!!,
                            onApprove = { viewModel.approveProposals() },
                            onDiscard = { viewModel.discardProposals() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage, onSaveToNotes: () -> Unit) {
    val isUser = message.sender == "user"
    val align = if (isUser) Alignment.End else Alignment.Start

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = align
    ) {
        Surface(
            color = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (!isUser) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = onSaveToNotes,
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(imageVector = Icons.Default.BookmarkAdd, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.action_save_notes), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProposalCard(
    result: com.example.ai.agents.AgentExecutionResult,
    onApprove: () -> Unit,
    onDiscard: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().testTag("proposal_approval_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.tutor_proposal_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(6.dp))

            if (result.proposedTasks.isNotEmpty()) {
                Text(
                    text = "• ${result.proposedTasks.size} Study Tasks proposed",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            if (result.proposedFlashcards.isNotEmpty()) {
                Text(
                    text = "• ${result.proposedFlashcards.size} Flashcards proposed",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDiscard) {
                    Text(stringResource(R.string.tutor_proposal_discard))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = onApprove, modifier = Modifier.testTag("approve_proposal_button")) {
                    Text(stringResource(R.string.tutor_proposal_approve))
                }
            }
        }
    }
}
