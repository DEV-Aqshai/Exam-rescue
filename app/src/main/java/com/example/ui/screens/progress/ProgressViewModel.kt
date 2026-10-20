package com.example.ui.screens.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.database.entities.StudySessionEntity
import com.example.core.database.entities.TopicProgressEntity
import com.example.data.repositories.StudyRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class ProgressUiState(
    val totalStudySeconds: Int = 0,
    val sessionsCount: Int = 0,
    val confidentTopicsCount: Int = 0,
    val totalTopicsCount: Int = 0,
    val totalAttemptsCount: Int = 0,
    val correctAttemptsCount: Int = 0,
    val accuracyPercentage: Int? = null,
    val sessions: List<StudySessionEntity> = emptyList()
)

class ProgressViewModel(
    studyRepository: StudyRepository
) : ViewModel() {

    val uiState: StateFlow<ProgressUiState> = combine(
        studyRepository.getAllSessions(),
        studyRepository.getAllTopics(),
        studyRepository.getAllProgress(),
        studyRepository.getTotalAttemptsCount(),
        studyRepository.getCorrectAttemptsCount()
    ) { sessions, topics, progressList, totalAttempts, correctAttempts ->
        val totalSecs = sessions.sumOf { it.durationSeconds }
        val confidentCount = progressList.count { it.confidence >= 4 || it.status == "CONFIDENT" }
        val accuracy = if (totalAttempts >= 5) {
            ((correctAttempts.toFloat() / totalAttempts) * 100).toInt()
        } else null

        ProgressUiState(
            totalStudySeconds = totalSecs,
            sessionsCount = sessions.size,
            confidentTopicsCount = confidentCount,
            totalTopicsCount = topics.size,
            totalAttemptsCount = totalAttempts,
            correctAttemptsCount = correctAttempts,
            accuracyPercentage = accuracy,
            sessions = sessions.take(10)
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ProgressUiState()
    )
}
