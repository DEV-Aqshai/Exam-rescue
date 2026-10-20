package com.example.ui.screens.subjects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.database.entities.SubjectEntity
import com.example.core.database.entities.TopicEntity
import com.example.core.database.entities.TopicProgressEntity
import com.example.data.repositories.StudyRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

data class SubjectWithTopics(
    val subject: SubjectEntity,
    val topics: List<TopicEntity>,
    val averageConfidence: Float
)

data class SubjectsUiState(
    val subjectsWithTopics: List<SubjectWithTopics> = emptyList(),
    val showAddSubjectDialog: Boolean = false
)

class SubjectsViewModel(
    private val studyRepository: StudyRepository
) : ViewModel() {

    private val _showAddSubjectDialog = MutableStateFlow(false)

    val uiState: StateFlow<SubjectsUiState> = combine(
        studyRepository.getAllSubjects(),
        studyRepository.getAllTopics(),
        studyRepository.getAllProgress(),
        _showAddSubjectDialog
    ) { subjects, topics, progressList, showDialog ->
        val progressMap = progressList.associateBy { it.topicId }
        val topicsBySubject = topics.groupBy { it.subjectId }

        val list = subjects.map { subject ->
            val subjTopics = topicsBySubject[subject.id] ?: emptyList()
            val totalConf = subjTopics.sumOf { progressMap[it.id]?.confidence ?: 1 }
            val avgConf = if (subjTopics.isNotEmpty()) totalConf.toFloat() / subjTopics.size else 0f
            SubjectWithTopics(subject, subjTopics, avgConf)
        }

        SubjectsUiState(list, showDialog)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SubjectsUiState()
    )

    fun setShowAddSubjectDialog(show: Boolean) {
        _showAddSubjectDialog.value = show
    }

    fun addCustomSubject(name: String) {
        viewModelScope.launch {
            val id = "custom_" + UUID.randomUUID().toString().take(8)
            val subject = SubjectEntity(
                id = id,
                nameKey = id,
                name = name,
                isCustom = true,
                sortOrder = 99
            )
            studyRepository.insertSubject(subject)
            _showAddSubjectDialog.value = false
        }
    }

    fun deleteSubject(id: String) {
        viewModelScope.launch { studyRepository.deleteSubject(id) }
    }

    fun updateTopicConfidence(topicId: String, confidence: Int) {
        viewModelScope.launch {
            val status = when {
                confidence >= 4 -> "CONFIDENT"
                confidence >= 2 -> "REVISING"
                else -> "LEARNING"
            }
            studyRepository.setTopicConfidence(topicId, confidence, status)
        }
    }

    fun addCustomTopic(subjectId: String, title: String, description: String) {
        viewModelScope.launch {
            val id = "topic_" + UUID.randomUUID().toString().take(8)
            val topic = TopicEntity(
                id = id,
                subjectId = subjectId,
                title = title,
                description = description
            )
            studyRepository.insertTopic(topic)
        }
    }
}
