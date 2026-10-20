package com.example.ui.screens.exams

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.database.entities.ExamEntity
import com.example.core.database.entities.SubjectEntity
import com.example.data.repositories.StudyRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

data class ExamsUiState(
    val exams: List<ExamEntity> = emptyList(),
    val subjects: List<SubjectEntity> = emptyList(),
    val showAddDialog: Boolean = false
)

class ExamsViewModel(
    private val studyRepository: StudyRepository
) : ViewModel() {

    private val _showAddDialog = MutableStateFlow(false)

    val uiState: StateFlow<ExamsUiState> = combine(
        studyRepository.getAllExams(),
        studyRepository.getAllSubjects(),
        _showAddDialog
    ) { exams, subjects, showDialog ->
        ExamsUiState(exams, subjects, showDialog)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ExamsUiState()
    )

    fun setShowAddDialog(show: Boolean) {
        _showAddDialog.value = show
    }

    fun addExam(subjectId: String, title: String, dateMillis: Long, location: String?) {
        viewModelScope.launch {
            val exam = ExamEntity(
                id = UUID.randomUUID().toString(),
                subjectId = subjectId,
                title = title,
                examAt = dateMillis,
                locationNote = location,
                reminderEnabled = true
            )
            studyRepository.insertExam(exam)
            _showAddDialog.value = false
        }
    }

    fun deleteExam(id: String) {
        viewModelScope.launch { studyRepository.deleteExam(id) }
    }
}
