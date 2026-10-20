package com.example.ui.screens.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.common.FlashcardRating
import com.example.core.database.entities.FlashcardEntity
import com.example.core.database.entities.NoteEntity
import com.example.core.database.entities.SubjectEntity
import com.example.data.repositories.StudyRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

data class NotesFlashcardsUiState(
    val selectedTab: Int = 0, // 0=Notes, 1=Flashcards
    val notes: List<NoteEntity> = emptyList(),
    val flashcards: List<FlashcardEntity> = emptyList(),
    val dueFlashcards: List<FlashcardEntity> = emptyList(),
    val subjects: List<SubjectEntity> = emptyList(),
    val searchQuery: String = "",
    val showAddNoteDialog: Boolean = false,
    val showAddCardDialog: Boolean = false,
    val currentCardIndex: Int = 0,
    val isCardFlipped: Boolean = false
)

class NotesFlashcardsViewModel(
    private val studyRepository: StudyRepository
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(0)
    private val _searchQuery = MutableStateFlow("")
    private val _showAddNoteDialog = MutableStateFlow(false)
    private val _showAddCardDialog = MutableStateFlow(false)
    private val _currentCardIndex = MutableStateFlow(0)
    private val _isCardFlipped = MutableStateFlow(false)

    private data class NotesAndCardsData(
        val notes: List<NoteEntity>,
        val flashcards: List<FlashcardEntity>,
        val dueFlashcards: List<FlashcardEntity>,
        val subjects: List<SubjectEntity>
    )

    private data class InteractionData(
        val tab: Int,
        val query: String,
        val addNote: Boolean,
        val addCard: Boolean,
        val cardIdx: Int,
        val flipped: Boolean
    )

    private val dataFlow = combine(
        studyRepository.getAllNotes(),
        studyRepository.getAllFlashcards(),
        studyRepository.getDueFlashcards(),
        studyRepository.getAllSubjects()
    ) { notes, cards, dueCards, subjects ->
        NotesAndCardsData(notes, cards, dueCards, subjects)
    }

    private val interactionFlow = combine(
        _selectedTab,
        _searchQuery,
        _showAddNoteDialog,
        _showAddCardDialog
    ) { tab, query, addNote, addCard ->
        Pair(Pair(tab, query), Pair(addNote, addCard))
    }

    val uiState: StateFlow<NotesFlashcardsUiState> = combine(
        dataFlow,
        interactionFlow,
        _currentCardIndex,
        _isCardFlipped
    ) { data, interactions, cardIdx, flipped ->
        val (tabQuery, dialogs) = interactions
        val (tab, query) = tabQuery
        val (addNote, addCard) = dialogs

        val filteredNotes = if (query.isBlank()) data.notes else {
            data.notes.filter { it.title.contains(query, ignoreCase = true) || it.body.contains(query, ignoreCase = true) }
        }
        NotesFlashcardsUiState(
            selectedTab = tab,
            notes = filteredNotes,
            flashcards = data.flashcards,
            dueFlashcards = data.dueFlashcards,
            subjects = data.subjects,
            searchQuery = query,
            showAddNoteDialog = addNote,
            showAddCardDialog = addCard,
            currentCardIndex = cardIdx.coerceIn(0, maxOf(0, data.dueFlashcards.size - 1)),
            isCardFlipped = flipped
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = NotesFlashcardsUiState()
    )

    fun selectTab(tab: Int) {
        _selectedTab.value = tab
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setShowAddNoteDialog(show: Boolean) {
        _showAddNoteDialog.value = show
    }

    fun setShowAddCardDialog(show: Boolean) {
        _showAddCardDialog.value = show
    }

    fun flipCard() {
        _isCardFlipped.value = !_isCardFlipped.value
    }

    fun reviewCurrentCard(rating: FlashcardRating) {
        val state = uiState.value
        val card = state.dueFlashcards.getOrNull(state.currentCardIndex) ?: return
        viewModelScope.launch {
            studyRepository.reviewFlashcard(card, rating)
            _isCardFlipped.value = false
            _currentCardIndex.value = minOf(state.currentCardIndex + 1, maxOf(0, state.dueFlashcards.size - 1))
        }
    }

    fun addNote(title: String, body: String, subjectId: String?) {
        viewModelScope.launch {
            val note = NoteEntity(
                id = UUID.randomUUID().toString(),
                title = title,
                body = body,
                subjectId = subjectId
            )
            studyRepository.insertNote(note)
            _showAddNoteDialog.value = false
        }
    }

    fun deleteNote(id: String) {
        viewModelScope.launch { studyRepository.deleteNote(id) }
    }

    fun addFlashcard(front: String, back: String, subjectId: String?) {
        viewModelScope.launch {
            val card = FlashcardEntity(
                id = UUID.randomUUID().toString(),
                subjectId = subjectId,
                front = front,
                back = back,
                intervalDays = 1
            )
            studyRepository.insertFlashcard(card)
            _showAddCardDialog.value = false
        }
    }

    fun deleteFlashcard(id: String) {
        viewModelScope.launch { studyRepository.deleteFlashcard(id) }
    }
}
