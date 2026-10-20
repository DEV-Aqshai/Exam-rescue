package com.example.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.models.AiResult
import com.example.ai.models.ProviderType
import com.example.ai.providers.AiProviderFactory
import com.example.core.database.entities.AiProviderConfigEntity
import com.example.core.datastore.UserPreferences
import com.example.core.datastore.UserPreferencesRepository
import com.example.core.importexport.BackupManager
import com.example.core.importexport.ExportResult
import com.example.core.importexport.ImportResult
import com.example.core.security.KeystoreManager
import com.example.data.repositories.StudyRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SettingsUiState(
    val preferences: UserPreferences = UserPreferences(),
    val selectedProvider: ProviderType = ProviderType.GEMINI,
    val apiKey: String = "",
    val isKeySaved: Boolean = false,
    val modelId: String = "gemini-2.5-flash",
    val endpoint: String = "https://generativelanguage.googleapis.com/v1beta",
    val isTestingConnection: Boolean = false,
    val testConnectionMessage: String? = null,
    val isTestSuccess: Boolean? = null,
    val lastExportedJson: String? = null,
    val importMessage: String? = null,
    val showDeleteConfirm: Boolean = false
)

class SettingsViewModel(
    private val preferencesRepository: UserPreferencesRepository,
    private val studyRepository: StudyRepository,
    private val aiProviderConfigDao: com.example.core.database.daos.AiProviderConfigDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            preferencesRepository.userPreferencesFlow.collect { prefs ->
                val providerType = ProviderType.fromId(prefs.activeAiProviderId)
                val config = aiProviderConfigDao.getConfigById(providerType.id)
                val decryptedKey = if (config?.encryptedKeyBlob != null) {
                    KeystoreManager.decrypt(config.encryptedKeyBlob)
                } else ""

                _uiState.update {
                    it.copy(
                        preferences = prefs,
                        selectedProvider = providerType,
                        apiKey = decryptedKey,
                        isKeySaved = decryptedKey.isNotBlank(),
                        modelId = config?.modelId ?: providerType.defaultModel,
                        endpoint = config?.endpoint ?: providerType.defaultEndpoint
                    )
                }
            }
        }
    }

    fun setInterfaceLanguage(lang: String) {
        viewModelScope.launch { preferencesRepository.setInterfaceLanguage(lang) }
    }

    fun setLearningMedium(medium: String) {
        viewModelScope.launch { preferencesRepository.setLearningMedium(medium) }
    }

    fun setTheme(theme: String) {
        viewModelScope.launch { preferencesRepository.setSelectedTheme(theme) }
    }

    fun setDailyMinutes(minutes: Int) {
        viewModelScope.launch { preferencesRepository.setDailyStudyMinutes(minutes) }
    }

    fun selectProvider(provider: ProviderType) {
        viewModelScope.launch {
            val config = aiProviderConfigDao.getConfigById(provider.id)
            val decryptedKey = if (config?.encryptedKeyBlob != null) {
                KeystoreManager.decrypt(config.encryptedKeyBlob)
            } else ""

            preferencesRepository.setActiveAiProvider(provider.id)
            aiProviderConfigDao.setActiveProvider(provider.id)

            _uiState.update {
                it.copy(
                    selectedProvider = provider,
                    apiKey = decryptedKey,
                    isKeySaved = decryptedKey.isNotBlank(),
                    modelId = config?.modelId ?: provider.defaultModel,
                    endpoint = config?.endpoint ?: provider.defaultEndpoint,
                    testConnectionMessage = null,
                    isTestSuccess = null
                )
            }
        }
    }

    fun updateApiKey(key: String) {
        _uiState.update { it.copy(apiKey = key) }
    }

    fun updateModelId(model: String) {
        _uiState.update { it.copy(modelId = model) }
    }

    fun updateEndpoint(endpoint: String) {
        _uiState.update { it.copy(endpoint = endpoint) }
    }

    fun saveProviderConfig() {
        viewModelScope.launch {
            val state = _uiState.value
            val encrypted = if (state.apiKey.isNotBlank()) KeystoreManager.encrypt(state.apiKey) else null
            val config = AiProviderConfigEntity(
                providerId = state.selectedProvider.id,
                displayName = state.selectedProvider.displayName,
                endpoint = state.endpoint,
                modelId = state.modelId,
                encryptedKeyBlob = encrypted,
                enabled = true
            )
            aiProviderConfigDao.upsertConfig(config)
            aiProviderConfigDao.setActiveProvider(state.selectedProvider.id)
            _uiState.update { it.copy(isKeySaved = state.apiKey.isNotBlank()) }
        }
    }

    fun removeApiKey() {
        viewModelScope.launch {
            aiProviderConfigDao.clearKeyForProvider(_uiState.value.selectedProvider.id)
            _uiState.update { it.copy(apiKey = "", isKeySaved = false, testConnectionMessage = null, isTestSuccess = null) }
        }
    }

    fun testConnection() {
        val state = _uiState.value
        if (state.apiKey.isBlank()) {
            _uiState.update { it.copy(testConnectionMessage = "Please enter an API key to test.", isTestSuccess = false) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isTestingConnection = true, testConnectionMessage = null) }
            val config = AiProviderConfigEntity(
                providerId = state.selectedProvider.id,
                displayName = state.selectedProvider.displayName,
                endpoint = state.endpoint,
                modelId = state.modelId,
                encryptedKeyBlob = null
            )
            val provider = AiProviderFactory.getProvider(state.selectedProvider.id)
            when (val result = provider.testConnection(config, state.apiKey)) {
                is AiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isTestingConnection = false,
                            isTestSuccess = true,
                            testConnectionMessage = "Connection successful! Provider responded correctly."
                        )
                    }
                }
                is AiResult.InvalidKey -> {
                    _uiState.update {
                        it.copy(
                            isTestingConnection = false,
                            isTestSuccess = false,
                            testConnectionMessage = "Authentication failed: Invalid API key."
                        )
                    }
                }
                is AiResult.QuotaExceeded -> {
                    _uiState.update {
                        it.copy(
                            isTestingConnection = false,
                            isTestSuccess = false,
                            testConnectionMessage = "Rate limit or quota exceeded with provider."
                        )
                    }
                }
                is AiResult.Offline -> {
                    _uiState.update {
                        it.copy(
                            isTestingConnection = false,
                            isTestSuccess = false,
                            testConnectionMessage = "Offline: No internet connection detected."
                        )
                    }
                }
                is AiResult.Timeout -> {
                    _uiState.update {
                        it.copy(
                            isTestingConnection = false,
                            isTestSuccess = false,
                            testConnectionMessage = "Request timed out."
                        )
                    }
                }
                is AiResult.ProviderError -> {
                    _uiState.update {
                        it.copy(
                            isTestingConnection = false,
                            isTestSuccess = false,
                            testConnectionMessage = "Provider error (${result.code}): ${result.message}"
                        )
                    }
                }
                is AiResult.MalformedResponse -> {
                    _uiState.update {
                        it.copy(
                            isTestingConnection = false,
                            isTestSuccess = false,
                            testConnectionMessage = "Malformed response: ${result.details}"
                        )
                    }
                }
            }
        }
    }

    fun exportData() {
        viewModelScope.launch {
            val result = BackupManager.exportDataToJson(studyRepository)
            _uiState.update { it.copy(lastExportedJson = result.jsonString) }
        }
    }

    fun importData(jsonString: String) {
        viewModelScope.launch {
            when (val result = BackupManager.importDataFromJson(jsonString, studyRepository)) {
                is ImportResult.Success -> {
                    _uiState.update {
                        it.copy(importMessage = "Successfully imported ${result.tasksCount} tasks, ${result.notesCount} notes, and ${result.flashcardsCount} flashcards.")
                    }
                }
                is ImportResult.Error -> {
                    _uiState.update { it.copy(importMessage = "Import failed: ${result.reason}") }
                }
            }
        }
    }

    fun setShowDeleteConfirm(show: Boolean) {
        _uiState.update { it.copy(showDeleteConfirm = show) }
    }

    fun deleteAllData(onComplete: () -> Unit) {
        viewModelScope.launch {
            studyRepository.clearAllData()
            preferencesRepository.clearAllPreferences()
            _uiState.update { it.copy(showDeleteConfirm = false) }
            onComplete()
        }
    }
}
