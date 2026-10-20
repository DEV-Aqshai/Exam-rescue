package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.R

sealed class Screen(
    val route: String,
    val titleResId: Int,
    val icon: ImageVector? = null
) {
    object Onboarding : Screen("onboarding", R.string.onboarding_welcome_title)
    object Home : Screen("home", R.string.nav_home, Icons.Default.Home)
    object Plan : Screen("plan", R.string.nav_plan, Icons.Default.CalendarMonth)
    object Practise : Screen("practise", R.string.nav_practise, Icons.Default.Quiz)
    object Tutor : Screen("tutor", R.string.nav_tutor, Icons.Default.AutoAwesome)
    object Progress : Screen("progress", R.string.nav_progress, Icons.Default.Insights)

    // Sub-screens
    object Subjects : Screen("subjects", R.string.subjects_title)
    object SubjectDetail : Screen("subject_detail/{subjectId}", R.string.subjects_title) {
        fun createRoute(subjectId: String) = "subject_detail/$subjectId"
    }
    object Exams : Screen("exams", R.string.exams_title)
    object StudySession : Screen("session?taskId={taskId}&subjectId={subjectId}", R.string.session_title) {
        fun createRoute(taskId: String? = null, subjectId: String? = null) =
            "session?taskId=${taskId ?: ""}&subjectId=${subjectId ?: ""}"
    }
    object Mistakes : Screen("mistakes", R.string.mistakes_title)
    object Notes : Screen("notes", R.string.notes_title)
    object Settings : Screen("settings", R.string.nav_settings)
}

val BottomNavItems = listOf(
    Screen.Home,
    Screen.Plan,
    Screen.Practise,
    Screen.Tutor,
    Screen.Progress
)
