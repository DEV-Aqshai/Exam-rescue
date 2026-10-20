package com.example.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.userDataStore: DataStore<Preferences> by preferencesDataStore(name = "exam_rescue_prefs")

data class UserPreferences(
    val isOnboardingCompleted: Boolean = false,
    val interfaceLanguage: String = "en", // "en" or "ta"
    val learningMedium: String = "TAMIL", // "TAMIL" or "ENGLISH"
    val targetExamDateMillis: Long = 0L,
    val targetExamName: String = "GCE Ordinary Level",
    val dailyStudyMinutes: Int = 90,
    val preferredSessionLengthMinutes: Int = 25,
    val selectedTheme: String = "SYSTEM", // "SYSTEM", "LIGHT", "DARK"
    val remindersEnabled: Boolean = true,
    val activeAiProviderId: String = "gemini"
)

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val INTERFACE_LANGUAGE = stringPreferencesKey("interface_language")
        val LEARNING_MEDIUM = stringPreferencesKey("learning_medium")
        val TARGET_EXAM_DATE = longPreferencesKey("target_exam_date")
        val TARGET_EXAM_NAME = stringPreferencesKey("target_exam_name")
        val DAILY_STUDY_MINUTES = intPreferencesKey("daily_study_minutes")
        val SESSION_LENGTH_MINUTES = intPreferencesKey("session_length_minutes")
        val SELECTED_THEME = stringPreferencesKey("selected_theme")
        val REMINDERS_ENABLED = booleanPreferencesKey("reminders_enabled")
        val ACTIVE_AI_PROVIDER = stringPreferencesKey("active_ai_provider")
    }

    val userPreferencesFlow: Flow<UserPreferences> = context.userDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            UserPreferences(
                isOnboardingCompleted = preferences[PreferencesKeys.ONBOARDING_COMPLETED] ?: false,
                interfaceLanguage = preferences[PreferencesKeys.INTERFACE_LANGUAGE] ?: "en",
                learningMedium = preferences[PreferencesKeys.LEARNING_MEDIUM] ?: "TAMIL",
                targetExamDateMillis = preferences[PreferencesKeys.TARGET_EXAM_DATE] ?: 0L,
                targetExamName = preferences[PreferencesKeys.TARGET_EXAM_NAME] ?: "GCE Ordinary Level",
                dailyStudyMinutes = preferences[PreferencesKeys.DAILY_STUDY_MINUTES] ?: 90,
                preferredSessionLengthMinutes = preferences[PreferencesKeys.SESSION_LENGTH_MINUTES] ?: 25,
                selectedTheme = preferences[PreferencesKeys.SELECTED_THEME] ?: "SYSTEM",
                remindersEnabled = preferences[PreferencesKeys.REMINDERS_ENABLED] ?: true,
                activeAiProviderId = preferences[PreferencesKeys.ACTIVE_AI_PROVIDER] ?: "gemini"
            )
        }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.userDataStore.edit { it[PreferencesKeys.ONBOARDING_COMPLETED] = completed }
    }

    suspend fun setInterfaceLanguage(language: String) {
        context.userDataStore.edit { it[PreferencesKeys.INTERFACE_LANGUAGE] = language }
    }

    suspend fun setLearningMedium(medium: String) {
        context.userDataStore.edit { it[PreferencesKeys.LEARNING_MEDIUM] = medium }
    }

    suspend fun setTargetExam(name: String, dateMillis: Long) {
        context.userDataStore.edit {
            it[PreferencesKeys.TARGET_EXAM_NAME] = name
            it[PreferencesKeys.TARGET_EXAM_DATE] = dateMillis
        }
    }

    suspend fun setDailyStudyMinutes(minutes: Int) {
        context.userDataStore.edit { it[PreferencesKeys.DAILY_STUDY_MINUTES] = minutes }
    }

    suspend fun setSessionLengthMinutes(minutes: Int) {
        context.userDataStore.edit { it[PreferencesKeys.SESSION_LENGTH_MINUTES] = minutes }
    }

    suspend fun setSelectedTheme(theme: String) {
        context.userDataStore.edit { it[PreferencesKeys.SELECTED_THEME] = theme }
    }

    suspend fun setRemindersEnabled(enabled: Boolean) {
        context.userDataStore.edit { it[PreferencesKeys.REMINDERS_ENABLED] = enabled }
    }

    suspend fun setActiveAiProvider(providerId: String) {
        context.userDataStore.edit { it[PreferencesKeys.ACTIVE_AI_PROVIDER] = providerId }
    }

    suspend fun clearAllPreferences() {
        context.userDataStore.edit { it.clear() }
    }
}
