package com.example.ui.screens.practise

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.database.entities.MistakeEntity
import com.example.core.database.entities.QuestionAttemptEntity
import com.example.core.database.entities.QuestionEntity
import com.example.core.database.entities.SubjectEntity
import com.example.data.repositories.StudyRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.util.UUID

data class PractiseUiState(
    val questions: List<QuestionEntity> = emptyList(),
    val subjects: List<SubjectEntity> = emptyList(),
    val selectedSubjectId: String? = null,
    val currentIndex: Int = 0,
    val selectedOption: String? = null,
    val hasSubmitted: Boolean = false,
    val isCorrect: Boolean = false,
    val showAddDialog: Boolean = false
)

class PractiseViewModel(
    private val studyRepository: StudyRepository
) : ViewModel() {

    private val _selectedSubjectId = MutableStateFlow<String?>(null)
    private val _currentIndex = MutableStateFlow(0)
    private val _selectedOption = MutableStateFlow<String?>(null)
    private val _hasSubmitted = MutableStateFlow(false)
    private val _isCorrect = MutableStateFlow(false)
    private val _showAddDialog = MutableStateFlow(false)

    private data class QuestionFilterData(
        val questions: List<QuestionEntity>,
        val subjects: List<SubjectEntity>,
        val selectedSubjectId: String?
    )

    private data class InteractionData(
        val currentIndex: Int,
        val selectedOption: String?,
        val hasSubmitted: Boolean,
        val isCorrect: Boolean,
        val showAddDialog: Boolean
    )

    private val filterDataFlow = combine(
        studyRepository.getAllQuestions(),
        studyRepository.getAllSubjects(),
        _selectedSubjectId
    ) { questions, subjects, subjId ->
        QuestionFilterData(questions, subjects, subjId)
    }

    private val interactionFlow = combine(
        _currentIndex,
        _selectedOption,
        _hasSubmitted,
        _isCorrect,
        _showAddDialog
    ) { idx, selOpt, submitted, correct, addDialog ->
        InteractionData(idx, selOpt, submitted, correct, addDialog)
    }

    val uiState: StateFlow<PractiseUiState> = combine(
        filterDataFlow,
        interactionFlow
    ) { filterData, interaction ->
        val filtered = if (filterData.selectedSubjectId != null) {
            filterData.questions.filter { it.subjectId == filterData.selectedSubjectId }
        } else {
            filterData.questions
        }
        PractiseUiState(
            questions = filtered,
            subjects = filterData.subjects,
            selectedSubjectId = filterData.selectedSubjectId,
            currentIndex = interaction.currentIndex.coerceIn(0, maxOf(0, filtered.size - 1)),
            selectedOption = interaction.selectedOption,
            hasSubmitted = interaction.hasSubmitted,
            isCorrect = interaction.isCorrect,
            showAddDialog = interaction.showAddDialog
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PractiseUiState()
    )

    fun selectSubject(subjectId: String?) {
        _selectedSubjectId.value = subjectId
        _currentIndex.value = 0
        _selectedOption.value = null
        _hasSubmitted.value = false
    }

    fun selectOption(option: String) {
        if (!_hasSubmitted.value) {
            _selectedOption.value = option
        }
    }

    fun submitAnswer() {
        val state = uiState.value
        val currentQ = state.questions.getOrNull(state.currentIndex) ?: return
        val userChoice = state.selectedOption ?: return

        val correct = userChoice.trim().equals(currentQ.answerText.trim(), ignoreCase = true)
        _isCorrect.value = correct
        _hasSubmitted.value = true

        viewModelScope.launch {
            // Record attempt
            val attempt = QuestionAttemptEntity(
                id = UUID.randomUUID().toString(),
                questionId = currentQ.id,
                responseText = userChoice,
                isCorrect = correct
            )
            studyRepository.recordAttempt(attempt)

            // If incorrect, automatically save snapshot to Mistake Notebook!
            if (!correct) {
                val mistake = MistakeEntity(
                    id = UUID.randomUUID().toString(),
                    questionId = currentQ.id,
                    subjectId = currentQ.subjectId,
                    topicId = currentQ.topicId,
                    promptSnapshot = currentQ.prompt,
                    studentAnswer = userChoice,
                    answerSnapshot = currentQ.answerText,
                    explanationSnapshot = currentQ.explanation,
                    status = "NEEDS_PRACTICE"
                )
                studyRepository.insertMistake(mistake)
            }
        }
    }

    fun nextQuestion() {
        _selectedOption.value = null
        _hasSubmitted.value = false
        _isCorrect.value = false
        _currentIndex.value += 1
    }

    fun previousQuestion() {
        if (_currentIndex.value > 0) {
            _selectedOption.value = null
            _hasSubmitted.value = false
            _isCorrect.value = false
            _currentIndex.value -= 1
        }
    }

    fun setShowAddDialog(show: Boolean) {
        _showAddDialog.value = show
    }

    fun addCustomQuestion(subjectId: String, prompt: String, options: List<String>, answer: String, explanation: String) {
        viewModelScope.launch {
            val q = QuestionEntity(
                id = UUID.randomUUID().toString(),
                subjectId = subjectId,
                prompt = prompt,
                type = "MULTIPLE_CHOICE",
                optionsJson = JSONArray(options).toString(),
                answerText = answer,
                explanation = explanation,
                difficulty = "MEDIUM",
                language = "ENGLISH",
                sourceType = "USER_CREATED",
                sourceAttribution = "Custom student question"
            )
            studyRepository.insertQuestion(q)
            _showAddDialog.value = false
        }
    }
}
