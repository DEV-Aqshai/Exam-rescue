package com.example.ui.screens.mistakes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.database.entities.FlashcardEntity
import com.example.core.database.entities.MistakeEntity
import com.example.core.database.entities.StudyTaskEntity
import com.example.data.repositories.StudyRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class MistakesUiState(
    val mistakes: List<MistakeEntity> = emptyList(),
    val activeMistakesCount: Int = 0
)

class MistakesViewModel(
    private val studyRepository: StudyRepository
) : ViewModel() {

    val uiState: StateFlow<MistakesUiState> = studyRepository.getAllMistakes()
        .map { list ->
            MistakesUiState(
                mistakes = list,
                activeMistakesCount = list.count { it.status == "NEEDS_PRACTICE" }
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = MistakesUiState()
        )

    fun markUnderstood(mistakeId: String) {
        viewModelScope.launch { studyRepository.updateMistakeStatus(mistakeId, "UNDERSTOOD") }
    }

    fun markNeedsPractice(mistakeId: String) {
        viewModelScope.launch { studyRepository.updateMistakeStatus(mistakeId, "NEEDS_PRACTICE") }
    }

    fun convertToFlashcard(mistake: MistakeEntity) {
        viewModelScope.launch {
            val flashcard = FlashcardEntity(
                id = UUID.randomUUID().toString(),
                subjectId = mistake.subjectId,
                topicId = mistake.topicId,
                front = mistake.promptSnapshot,
                back = "Correct Answer:\n${mistake.answerSnapshot}\n\nExplanation:\n${mistake.explanationSnapshot}",
                intervalDays = 1
            )
            studyRepository.insertFlashcard(flashcard)
            studyRepository.updateMistakeStatus(mistake.id, "UNDERSTOOD")
        }
    }

    fun convertToStudyTask(mistake: MistakeEntity) {
        viewModelScope.launch {
            val task = StudyTaskEntity(
                id = UUID.randomUUID().toString(),
                subjectId = mistake.subjectId,
                topicId = mistake.topicId,
                title = "Revise Mistake: ${mistake.promptSnapshot.take(30)}...",
                taskType = "RECALL",
                plannedMinutes = 20,
                priority = 3, // High priority
                status = "PENDING",
                createdBy = "USER",
                reasonExplanation = "Follow-up revision from Mistake Notebook"
            )
            studyRepository.insertTask(task)
        }
    }

    fun deleteMistake(mistakeId: String) {
        viewModelScope.launch { studyRepository.deleteMistake(mistakeId) }
    }
}
