package com.example.ui.screens.planner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.database.entities.StudyTaskEntity
import com.example.core.database.entities.SubjectEntity
import com.example.core.datastore.UserPreferencesRepository
import com.example.data.repositories.StudyRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

data class PlannerUiState(
    val isWeekView: Boolean = false,
    val tasks: List<StudyTaskEntity> = emptyList(),
    val subjects: List<SubjectEntity> = emptyList(),
    val isGenerating: Boolean = false,
    val showAddTaskDialog: Boolean = false
)

class PlannerViewModel(
    private val studyRepository: StudyRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _isWeekView = MutableStateFlow(false)
    private val _isGenerating = MutableStateFlow(false)
    private val _showAddTaskDialog = MutableStateFlow(false)

    val uiState: StateFlow<PlannerUiState> = combine(
        studyRepository.getAllTasks(),
        studyRepository.getAllSubjects(),
        _isWeekView,
        _isGenerating,
        _showAddTaskDialog
    ) { tasks, subjects, isWeekView, isGenerating, showAddDialog ->
        PlannerUiState(
            isWeekView = isWeekView,
            tasks = tasks,
            subjects = subjects,
            isGenerating = isGenerating,
            showAddTaskDialog = showAddDialog
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PlannerUiState()
    )

    fun setWeekView(weekView: Boolean) {
        _isWeekView.value = weekView
    }

    fun setShowAddTaskDialog(show: Boolean) {
        _showAddTaskDialog.value = show
    }

    fun toggleTaskComplete(task: StudyTaskEntity) {
        viewModelScope.launch {
            val newStatus = if (task.status == "COMPLETED") "PENDING" else "COMPLETED"
            studyRepository.updateTaskStatus(task.id, newStatus)
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch { studyRepository.deleteTask(taskId) }
    }

    fun generatePlan() {
        viewModelScope.launch {
            _isGenerating.value = true
            val prefs = preferencesRepository.userPreferencesFlow.first()
            val proposed = studyRepository.generateDailyTasks(
                dailyMinutes = prefs.dailyStudyMinutes,
                sessionLengthMinutes = prefs.preferredSessionLengthMinutes
            )
            val newTasks = proposed.map {
                StudyTaskEntity(
                    id = it.id,
                    subjectId = it.subjectId,
                    topicId = it.topicId,
                    title = "${it.subjectName}: ${it.topicTitle}",
                    taskType = it.taskType,
                    plannedMinutes = it.plannedMinutes,
                    priority = it.priorityLevel,
                    status = "PENDING",
                    createdBy = "LOCAL_RULES",
                    reasonExplanation = it.primaryReason
                )
            }
            studyRepository.insertTasks(newTasks)
            _isGenerating.value = false
        }
    }

    fun createCustomTask(subjectId: String, title: String, minutes: Int, taskType: String, priority: Int) {
        viewModelScope.launch {
            val task = StudyTaskEntity(
                id = UUID.randomUUID().toString(),
                subjectId = subjectId,
                title = title,
                plannedMinutes = minutes,
                taskType = taskType,
                priority = priority,
                status = "PENDING",
                createdBy = "USER",
                reasonExplanation = "Custom task created by student"
            )
            studyRepository.insertTask(task)
            _showAddTaskDialog.value = false
        }
    }
}
