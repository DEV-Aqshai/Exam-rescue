package com.example.ui.screens.tutor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.agents.AgentExecutionResult
import com.example.ai.agents.AgentOrchestrator
import com.example.ai.agents.AgentType
import com.example.ai.models.AiMessage
import com.example.ai.models.AiResult
import com.example.core.database.entities.AiMessageEntity
import com.example.core.database.entities.AiProviderConfigEntity
import com.example.core.database.entities.FlashcardEntity
import com.example.core.database.entities.NoteEntity
import com.example.core.database.entities.StudyTaskEntity
import com.example.core.database.entities.TopicEntity
import com.example.core.datastore.UserPreferencesRepository
import com.example.core.security.KeystoreManager
import com.example.data.repositories.StudyRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: String, // "user" or "tutor"
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class TutorUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isSending: Boolean = false,
    val selectedLanguage: String = "TAMIL", // TAMIL or ENGLISH
    val activeProviderConfig: AiProviderConfigEntity? = null,
    val hasApiKey: Boolean = false,
    val activeAgent: AgentType = AgentType.BILINGUAL_CONCEPT_TUTOR,
    val includeNotesContext: Boolean = false,
    val includeMistakesContext: Boolean = false,
    val topics: List<TopicEntity> = emptyList(),
    val selectedTopicId: String? = null,
    val pendingProposalResult: AgentExecutionResult? = null,
    val errorMessage: String? = null
)

class TutorViewModel(
    private val studyRepository: StudyRepository,
    private val preferencesRepository: UserPreferencesRepository,
    private val orchestrator: AgentOrchestrator
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                sender = "tutor",
                text = "வணக்கம்! நான் உங்கள் க.பொ.த சா/த ஆசான். நீங்கள் கேட்கும் தலைப்பினை எளிய தமிழ் மற்றும் ஆங்கிலத்தில் தெளிவாக விளக்குவேன். ஏதேனும் சந்தேகங்களை கேட்கலாம் அல்லது கீழே உள்ள விரைவுச் செயல்களைப் பயன்படுத்தலாம்."
            )
        )
    )
    private val _isSending = MutableStateFlow(false)
    private val _selectedLanguage = MutableStateFlow("TAMIL")
    private val _activeAgent = MutableStateFlow(AgentType.BILINGUAL_CONCEPT_TUTOR)
    private val _includeNotesContext = MutableStateFlow(false)
    private val _includeMistakesContext = MutableStateFlow(false)
    private val _selectedTopicId = MutableStateFlow<String?>(null)
    private val _pendingProposal = MutableStateFlow<AgentExecutionResult?>(null)
    private val _errorMessage = MutableStateFlow<String?>(null)

    private data class TutorSettingsData(
        val lang: String,
        val agent: AgentType,
        val incNotes: Boolean,
        val incMistakes: Boolean,
        val topicId: String?
    )

    private val settingsFlow = combine(
        _selectedLanguage,
        _activeAgent,
        _includeNotesContext,
        _includeMistakesContext,
        _selectedTopicId
    ) { lang, agent, incNotes, incMistakes, topicId ->
        TutorSettingsData(lang, agent, incNotes, incMistakes, topicId)
    }

    val uiState: StateFlow<TutorUiState> = combine(
        _messages,
        _isSending,
        settingsFlow,
        studyRepository.getAllTopics(),
        _pendingProposal
    ) { msgs, sending, settings, topics, proposal ->
        TutorUiState(
            messages = msgs,
            isSending = sending,
            selectedLanguage = settings.lang,
            activeProviderConfig = null,
            hasApiKey = true,
            activeAgent = settings.agent,
            includeNotesContext = settings.incNotes,
            includeMistakesContext = settings.incMistakes,
            topics = topics,
            selectedTopicId = settings.topicId,
            pendingProposalResult = proposal,
            errorMessage = _errorMessage.value
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TutorUiState()
    )

    fun setLanguage(lang: String) {
        _selectedLanguage.value = lang
    }

    fun setActiveAgent(agent: AgentType) {
        _activeAgent.value = agent
    }

    fun toggleNotesContext() {
        _includeNotesContext.value = !_includeNotesContext.value
    }

    fun toggleMistakesContext() {
        _includeMistakesContext.value = !_includeMistakesContext.value
    }

    fun selectTopic(topicId: String?) {
        _selectedTopicId.value = topicId
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun sendMessage(inputText: String, activeConfig: AiProviderConfigEntity?) {
        if (inputText.isBlank() || _isSending.value) return

        val userMsg = ChatMessage(sender = "user", text = inputText)
        _messages.update { it + userMsg }

        if (activeConfig == null || activeConfig.encryptedKeyBlob.isNullOrBlank()) {
            _messages.update {
                it + ChatMessage(
                    sender = "tutor",
                    text = "Notice: No AI API Key is configured yet. Please enter your API key under Settings > AI Assistant. All local study features (planner, flashcards, questions) are active completely offline!"
                )
            }
            return
        }

        viewModelScope.launch {
            _isSending.value = true
            _errorMessage.value = null

            val history = _messages.value.dropLast(1).map {
                AiMessage(role = if (it.sender == "user") "user" else "assistant", content = it.text)
            }

            val result = orchestrator.executeAgent(
                agentType = _activeAgent.value,
                userInput = inputText,
                conversationHistory = history,
                config = activeConfig,
                language = _selectedLanguage.value,
                includeNotesContext = _includeNotesContext.value,
                includeMistakesContext = _includeMistakesContext.value,
                selectedTopicId = _selectedTopicId.value
            )

            when (result) {
                is AiResult.Success -> {
                    _messages.update {
                        it + ChatMessage(sender = "tutor", text = result.data.replyText)
                    }
                    if (result.data.proposedTasks.isNotEmpty() || result.data.proposedFlashcards.isNotEmpty()) {
                        _pendingProposal.value = result.data
                    }
                }
                is AiResult.InvalidKey -> {
                    _errorMessage.value = "Authentication failed: Invalid API key. Please check your key in Settings."
                }
                is AiResult.QuotaExceeded -> {
                    _errorMessage.value = "Rate limit or quota exceeded with your provider."
                }
                is AiResult.Offline -> {
                    _errorMessage.value = "Offline: Could not connect to AI provider. Local notes and questions are available offline."
                }
                is AiResult.Timeout -> {
                    _errorMessage.value = "Request timed out. Please try again."
                }
                is AiResult.ProviderError -> {
                    _errorMessage.value = "Provider error (${result.code}): ${result.message}"
                }
                is AiResult.MalformedResponse -> {
                    _errorMessage.value = "Failed to parse provider response: ${result.details}"
                }
            }
            _isSending.value = false
        }
    }

    fun approveProposals() {
        val proposal = _pendingProposal.value ?: return
        viewModelScope.launch {
            if (proposal.proposedTasks.isNotEmpty()) {
                studyRepository.insertTasks(proposal.proposedTasks)
            }
            if (proposal.proposedFlashcards.isNotEmpty()) {
                proposal.proposedFlashcards.forEach { studyRepository.insertFlashcard(it) }
            }
            _pendingProposal.value = null
            _messages.update {
                it + ChatMessage(
                    sender = "tutor",
                    text = "Saved ${proposal.proposedTasks.size} tasks and ${proposal.proposedFlashcards.size} flashcards to your local study database!"
                )
            }
        }
    }

    fun discardProposals() {
        _pendingProposal.value = null
    }

    fun saveLastReplyToNotes() {
        val lastReply = _messages.value.lastOrNull { it.sender == "tutor" }?.text ?: return
        viewModelScope.launch {
            val note = NoteEntity(
                id = UUID.randomUUID().toString(),
                title = "Tutor Explanation: ${_activeAgent.value.name}",
                body = lastReply,
                topicId = _selectedTopicId.value
            )
            studyRepository.insertNote(note)
            _messages.update {
                it + ChatMessage(sender = "tutor", text = "This explanation has been saved to your Notes.")
            }
        }
    }
}
