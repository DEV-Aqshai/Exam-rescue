package com.example.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.database.entities.ExamEntity
import com.example.core.database.entities.StudyTaskEntity
import com.example.core.database.entities.TopicEntity
import com.example.core.database.entities.TopicProgressEntity
import com.example.core.datastore.UserPreferences
import com.example.core.datastore.UserPreferencesRepository
import com.example.data.repositories.StudyRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class HomeUiState(
    val nextExam: ExamEntity? = null,
    val daysUntilExam: Int? = null,
    val todayTasks: List<StudyTaskEntity> = emptyList(),
    val completedMinutesToday: Int = 0,
    val plannedMinutesToday: Int = 0,
    val weakTopics: List<Pair<TopicEntity, TopicProgressEntity>> = emptyList(),
    val overdueTopics: List<Pair<TopicEntity, TopicProgressEntity>> = emptyList(),
    val userPreferences: UserPreferences = UserPreferences(),
    val isLoading: Boolean = false
)

class HomeViewModel(
    private val studyRepository: StudyRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        studyRepository.getNextExam(),
        studyRepository.getAllTasks(),
        studyRepository.getAllTopics(),
        studyRepository.getAllProgress(),
        preferencesRepository.userPreferencesFlow
    ) { nextExam, allTasks, topics, allProgress, prefs ->
        val now = System.currentTimeMillis()
        val daysUntilExam = nextExam?.let {
            val diff = it.examAt - now
            if (diff > 0) (diff / (86_400_000L)).toInt() else 0
        }

        // Today's tasks (pending or completed today)
        val todayTasks = allTasks.filter {
            it.status == "PENDING" || (it.completedAt != null && it.completedAt >= now - 86_400_000L)
        }.take(5)

        val completedMinutes = todayTasks.filter { it.status == "COMPLETED" }.sumOf { it.plannedMinutes }
        val plannedMinutes = todayTasks.sumOf { it.plannedMinutes }

        val progressMap = allProgress.associateBy { it.topicId }
        val topicsWithProgress = topics.map { it to (progressMap[it.id] ?: TopicProgressEntity(topicId = it.id)) }

        val weak = topicsWithProgress.filter { it.second.confidence <= 2 }.take(3)
        val overdue = topicsWithProgress.filter {
            it.second.nextReviewAt != null && it.second.nextReviewAt!! < now
        }.take(3)

        HomeUiState(
            nextExam = nextExam,
            daysUntilExam = daysUntilExam,
            todayTasks = todayTasks,
            completedMinutesToday = completedMinutes,
            plannedMinutesToday = maxOf(plannedMinutes, prefs.dailyStudyMinutes),
            weakTopics = weak,
            overdueTopics = overdue,
            userPreferences = prefs,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState(isLoading = true)
    )

    fun toggleTaskComplete(task: StudyTaskEntity) {
        viewModelScope.launch {
            val newStatus = if (task.status == "COMPLETED") "PENDING" else "COMPLETED"
            studyRepository.updateTaskStatus(task.id, newStatus)
        }
    }

    fun generateAutoPlan() {
        viewModelScope.launch {
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
        }
    }
}
