package com.example.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.datastore.UserPreferencesRepository
import com.example.data.repositories.StudyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OnboardingUiState(
    val step: Int = 0,
    val interfaceLanguage: String = "en",
    val learningMedium: String = "TAMIL",
    val targetExamName: String = "GCE Ordinary Level",
    val targetExamDateMillis: Long = System.currentTimeMillis() + (180L * 86_400_000L),
    val isExamDateSet: Boolean = true,
    val dailyMinutes: Int = 90,
    val sessionLengthMinutes: Int = 25,
    val selectedSubjectIds: Set<String> = setOf("math", "science", "english", "history", "tamil", "ict")
)

class OnboardingViewModel(
    private val preferencesRepository: UserPreferencesRepository,
    private val studyRepository: StudyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun setInterfaceLanguage(lang: String) {
        _uiState.update { it.copy(interfaceLanguage = lang) }
        viewModelScope.launch { preferencesRepository.setInterfaceLanguage(lang) }
    }

    fun setLearningMedium(medium: String) {
        _uiState.update { it.copy(learningMedium = medium) }
    }

    fun setExamDate(dateMillis: Long, isSet: Boolean) {
        _uiState.update { it.copy(targetExamDateMillis = dateMillis, isExamDateSet = isSet) }
    }

    fun setDailyMinutes(minutes: Int) {
        _uiState.update { it.copy(dailyMinutes = minutes) }
    }

    fun toggleSubject(subjectId: String) {
        _uiState.update { state ->
            val updated = if (state.selectedSubjectIds.contains(subjectId)) {
                if (state.selectedSubjectIds.size > 1) state.selectedSubjectIds - subjectId else state.selectedSubjectIds
            } else {
                state.selectedSubjectIds + subjectId
            }
            state.copy(selectedSubjectIds = updated)
        }
    }

    fun nextStep() {
        _uiState.update { it.copy(step = it.step + 1) }
    }

    fun previousStep() {
        _uiState.update { it.copy(step = maxOf(0, it.step - 1)) }
    }

    fun completeOnboarding(onFinished: () -> Unit) {
        viewModelScope.launch {
            val state = _uiState.value
            studyRepository.seedInitialDataIfEmpty()
            preferencesRepository.setInterfaceLanguage(state.interfaceLanguage)
            preferencesRepository.setLearningMedium(state.learningMedium)
            if (state.isExamDateSet) {
                preferencesRepository.setTargetExam(state.targetExamName, state.targetExamDateMillis)
            }
            preferencesRepository.setDailyStudyMinutes(state.dailyMinutes)
            preferencesRepository.setSessionLengthMinutes(state.sessionLengthMinutes)
            preferencesRepository.setOnboardingCompleted(true)
            onFinished()
        }
    }
}
