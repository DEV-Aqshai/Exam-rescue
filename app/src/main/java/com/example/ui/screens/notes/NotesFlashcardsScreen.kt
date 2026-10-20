package com.example.ui.screens.notes

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.core.common.FlashcardRating
import com.example.core.database.entities.FlashcardEntity
import com.example.core.database.entities.NoteEntity
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ExamRescueTopAppBar
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningOrange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesFlashcardsScreen(
    viewModel: NotesFlashcardsViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            ExamRescueTopAppBar(
                title = stringResource(R.string.notes_title),
                onNavigateBack = onNavigateBack
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (state.selectedTab == 0) viewModel.setShowAddNoteDialog(true)
                    else viewModel.setShowAddCardDialog(true)
                },
                modifier = Modifier.testTag("notes_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab Row (Notes vs Flashcards)
            TabRow(selectedTabIndex = state.selectedTab) {
                Tab(
                    selected = state.selectedTab == 0,
                    onClick = { viewModel.selectTab(0) },
                    text = { Text(stringResource(R.string.tab_notes)) }
                )
                Tab(
                    selected = state.selectedTab == 1,
                    onClick = { viewModel.selectTab(1) },
                    text = { Text("${stringResource(R.string.tab_flashcards)} (${state.dueFlashcards.size} due)") }
                )
            }

            if (state.selectedTab == 0) {
                // Notes View
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    OutlinedTextField(
                        value = state.searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = { Text(stringResource(R.string.action_search)) },
                        leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth().testTag("search_notes_input"),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (state.notes.isEmpty()) {
                        EmptyStateCard(
                            icon = Icons.Default.NoteAlt,
                            title = "No Notes Yet",
                            description = stringResource(R.string.notes_empty),
                            actionLabel = stringResource(R.string.notes_add),
                            onActionClick = { viewModel.setShowAddNoteDialog(true) }
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(bottom = 80.dp)
                        ) {
                            items(state.notes, key = { it.id }) { note ->
                                NoteCard(note = note, onDelete = { viewModel.deleteNote(note.id) })
                            }
                        }
                    }
                }
            } else {
                // Flashcards View (Spaced Repetition Review)
                FlashcardsReviewTab(viewModel, state)
            }
        }
    }

    if (state.showAddNoteDialog) {
        var title by remember { mutableStateOf("") }
        var body by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { viewModel.setShowAddNoteDialog(false) },
            title = { Text(stringResource(R.string.notes_add)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text(stringResource(R.string.notes_title_hint)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("note_title_input")
                    )
                    OutlinedTextField(
                        value = body,
                        onValueChange = { body = it },
                        label = { Text(stringResource(R.string.notes_body_hint)) },
                        modifier = Modifier.fillMaxWidth().height(120.dp).testTag("note_body_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            viewModel.addNote(title, body, null)
                        }
                    },
                    modifier = Modifier.testTag("confirm_add_note_button")
                ) {
                    Text(stringResource(R.string.action_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.setShowAddNoteDialog(false) }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    if (state.showAddCardDialog) {
        var front by remember { mutableStateOf("") }
        var back by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { viewModel.setShowAddCardDialog(false) },
            title = { Text(stringResource(R.string.flashcards_add)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = front,
                        onValueChange = { front = it },
                        label = { Text(stringResource(R.string.flashcard_front_hint)) },
                        modifier = Modifier.fillMaxWidth().testTag("card_front_input")
                    )
                    OutlinedTextField(
                        value = back,
                        onValueChange = { back = it },
                        label = { Text(stringResource(R.string.flashcard_back_hint)) },
                        modifier = Modifier.fillMaxWidth().testTag("card_back_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (front.isNotBlank() && back.isNotBlank()) {
                            viewModel.addFlashcard(front, back, null)
                        }
                    },
                    modifier = Modifier.testTag("confirm_add_card_button")
                ) {
                    Text(stringResource(R.string.action_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.setShowAddCardDialog(false) }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun NoteCard(note: NoteEntity, onDelete: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = note.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDelete) {
                    Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.outline)
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = note.body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FlashcardsReviewTab(
    viewModel: NotesFlashcardsViewModel,
    state: NotesFlashcardsUiState
) {
    if (state.dueFlashcards.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            EmptyStateCard(
                icon = Icons.Default.ThumbUp,
                title = "All Caught Up!",
                description = stringResource(R.string.flashcards_all_reviewed),
                actionLabel = stringResource(R.string.flashcards_add),
                onActionClick = { viewModel.setShowAddCardDialog(true) }
            )
        }
    } else {
        val currentCard = state.dueFlashcards[state.currentCardIndex]
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.flashcards_due_count, state.dueFlashcards.size),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            // Flashcard Interactive Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .clickable { viewModel.flipCard() }
                    .testTag("flashcard_flip_card"),
                colors = CardDefaults.cardColors(
                    containerColor = if (state.isCardFlipped) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (state.isCardFlipped) "ANSWER / EXPLANATION" else "QUESTION / CONCEPT",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = if (state.isCardFlipped) currentCard.back else currentCard.front,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.flashcard_tap_flip),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            // SM-2 Review Rating Buttons (Visible when flipped)
            if (state.isCardFlipped) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "How easily did you recall this?",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.reviewCurrentCard(FlashcardRating.AGAIN) },
                            colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                            modifier = Modifier.weight(1f).testTag("card_rating_again")
                        ) {
                            Text("Again", style = MaterialTheme.typography.labelSmall)
                        }
                        Button(
                            onClick = { viewModel.reviewCurrentCard(FlashcardRating.HARD) },
                            colors = ButtonDefaults.buttonColors(containerColor = WarningOrange),
                            modifier = Modifier.weight(1f).testTag("card_rating_hard")
                        ) {
                            Text("Hard", style = MaterialTheme.typography.labelSmall)
                        }
                        Button(
                            onClick = { viewModel.reviewCurrentCard(FlashcardRating.GOOD) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.weight(1f).testTag("card_rating_good")
                        ) {
                            Text("Good", style = MaterialTheme.typography.labelSmall)
                        }
                        Button(
                            onClick = { viewModel.reviewCurrentCard(FlashcardRating.EASY) },
                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                            modifier = Modifier.weight(1f).testTag("card_rating_easy")
                        ) {
                            Text("Easy", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            } else {
                Button(
                    onClick = { viewModel.flipCard() },
                    modifier = Modifier.fillMaxWidth().testTag("flip_card_button")
                ) {
                    Text("Show Answer")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
