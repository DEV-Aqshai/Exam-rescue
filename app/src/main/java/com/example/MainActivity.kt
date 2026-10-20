package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.example.core.database.ExamRescueDatabase
import com.example.core.datastore.UserPreferences
import com.example.core.datastore.UserPreferencesRepository
import com.example.core.notifications.StudyReminderManager
import com.example.data.repositories.StudyRepository
import com.example.ui.navigation.AppNavigation
import com.example.ui.navigation.Screen
import com.example.ui.theme.ExamRescueTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var database: ExamRescueDatabase
    private lateinit var preferencesRepository: UserPreferencesRepository
    private lateinit var studyRepository: StudyRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        database = ExamRescueDatabase.getInstance(this)
        preferencesRepository = UserPreferencesRepository(this)
        studyRepository = StudyRepository(database)

        // Seed initial data if first launch & schedule offline study reminder
        lifecycleScope.launch {
            studyRepository.seedInitialDataIfEmpty()
            StudyReminderManager.scheduleDailyReminder(this@MainActivity)
        }

        setContent {
            val userPreferences by preferencesRepository.userPreferencesFlow.collectAsState(initial = null)

            if (userPreferences == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                val prefs = userPreferences!!
                val isDark = when (prefs.selectedTheme) {
                    "LIGHT" -> false
                    "DARK" -> true
                    else -> isSystemInDarkTheme()
                }

                val startDestination = if (prefs.isOnboardingCompleted) {
                    Screen.Home.route
                } else {
                    Screen.Onboarding.route
                }

                ExamRescueTheme(darkTheme = isDark) {
                    AppNavigation(
                        database = database,
                        preferencesRepository = preferencesRepository,
                        studyRepository = studyRepository,
                        startDestination = startDestination
                    )
                }
            }
        }
    }
}
