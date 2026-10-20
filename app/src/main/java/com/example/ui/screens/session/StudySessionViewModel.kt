package com.example.ui.screens.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.database.entities.StudySessionEntity
import com.example.data.repositories.StudyRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

enum class TimerState {
    IDLE, RUNNING, PAUSED, FINISHED
}

data class SessionUiState(
    val timerState: TimerState = TimerState.IDLE,
    val sessionType: String = "REVIEW",
    val totalSecondsPlanned: Int = 25 * 60,
    val secondsElapsed: Int = 0,
    val taskId: String? = null,
    val subjectId: String? = null,
    val taskTitle: String = "Study Session",
    val showSummaryDialog: Boolean = false
)

class StudySessionViewModel(
    private val studyRepository: StudyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SessionUiState())
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var startTimestamp: Long = 0L
    private var accumulatedElapsedSeconds: Int = 0

    fun setupSession(taskId: String?, subjectId: String?) {
        _uiState.update { it.copy(taskId = taskId, subjectId = subjectId) }
        if (!taskId.isNullOrBlank()) {
            viewModelScope.launch {
                val tasks = studyRepository.getAllTasks()
                // Fetch task title if available
            }
        }
    }

    fun startTimer(sessionType: String, durationMinutes: Int = 25) {
        startTimestamp = System.currentTimeMillis()
        accumulatedElapsedSeconds = 0
        _uiState.update {
            it.copy(
                timerState = TimerState.RUNNING,
                sessionType = sessionType,
                totalSecondsPlanned = durationMinutes * 60,
                secondsElapsed = 0,
                showSummaryDialog = false
            )
        }
        startCounting()
    }

    fun pauseTimer() {
        val delta = ((System.currentTimeMillis() - startTimestamp) / 1000).toInt()
        accumulatedElapsedSeconds += delta
        timerJob?.cancel()
        _uiState.update { it.copy(timerState = TimerState.PAUSED, secondsElapsed = accumulatedElapsedSeconds) }
    }

    fun resumeTimer() {
        startTimestamp = System.currentTimeMillis()
        _uiState.update { it.copy(timerState = TimerState.RUNNING) }
        startCounting()
    }

    private fun startCounting() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val delta = ((System.currentTimeMillis() - startTimestamp) / 1000).toInt()
                val currentElapsed = accumulatedElapsedSeconds + delta
                _uiState.update { it.copy(secondsElapsed = currentElapsed) }
            }
        }
    }

    fun finishSession() {
        timerJob?.cancel()
        val delta = if (_uiState.value.timerState == TimerState.RUNNING) {
            ((System.currentTimeMillis() - startTimestamp) / 1000).toInt()
        } else 0
        val finalElapsed = accumulatedElapsedSeconds + delta

        _uiState.update {
            it.copy(
                timerState = TimerState.FINISHED,
                secondsElapsed = finalElapsed,
                showSummaryDialog = true
            )
        }
    }

    fun saveSummaryAndExit(rating: Int, notes: String, onFinished: () -> Unit) {
        viewModelScope.launch {
            val state = _uiState.value
            val durationSecs = state.secondsElapsed
            val session = StudySessionEntity(
                id = UUID.randomUUID().toString(),
                taskId = state.taskId,
                subjectId = state.subjectId,
                startedAt = System.currentTimeMillis() - (durationSecs * 1000L),
                endedAt = System.currentTimeMillis(),
                durationSeconds = durationSecs,
                sessionType = state.sessionType,
                selfRating = rating,
                notes = notes.ifBlank { null }
            )
            studyRepository.insertSession(session)
            if (!state.taskId.isNullOrBlank()) {
                studyRepository.updateTaskStatus(state.taskId, "COMPLETED")
            }
            _uiState.update { it.copy(showSummaryDialog = false, timerState = TimerState.IDLE) }
            onFinished()
        }
    }
}
