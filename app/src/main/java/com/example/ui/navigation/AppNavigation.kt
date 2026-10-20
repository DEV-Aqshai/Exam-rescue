package com.example.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.ai.agents.AgentOrchestrator
import com.example.core.database.ExamRescueDatabase
import com.example.core.datastore.UserPreferencesRepository
import com.example.data.repositories.StudyRepository
import com.example.ui.screens.exams.ExamsScreen
import com.example.ui.screens.exams.ExamsViewModel
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.home.HomeViewModel
import com.example.ui.screens.mistakes.MistakesScreen
import com.example.ui.screens.mistakes.MistakesViewModel
import com.example.ui.screens.notes.NotesFlashcardsScreen
import com.example.ui.screens.notes.NotesFlashcardsViewModel
import com.example.ui.screens.onboarding.OnboardingScreen
import com.example.ui.screens.onboarding.OnboardingViewModel
import com.example.ui.screens.planner.PlannerScreen
import com.example.ui.screens.planner.PlannerViewModel
import com.example.ui.screens.practise.PractiseScreen
import com.example.ui.screens.practise.PractiseViewModel
import com.example.ui.screens.progress.ProgressScreen
import com.example.ui.screens.progress.ProgressViewModel
import com.example.ui.screens.session.StudySessionScreen
import com.example.ui.screens.session.StudySessionViewModel
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.settings.SettingsViewModel
import com.example.ui.screens.subjects.SubjectDetailScreen
import com.example.ui.screens.subjects.SubjectsScreen
import com.example.ui.screens.subjects.SubjectsViewModel
import com.example.ui.screens.tutor.TutorScreen
import com.example.ui.screens.tutor.TutorViewModel

@Composable
fun AppNavigation(
    database: ExamRescueDatabase,
    preferencesRepository: UserPreferencesRepository,
    studyRepository: StudyRepository,
    startDestination: String
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Check if bottom bar should be visible
    val showBottomBar = currentRoute in BottomNavItems.map { it.route }

    // Shared orchestrator & ViewModels
    val orchestrator = remember { AgentOrchestrator(studyRepository) }
    val homeViewModel = remember { HomeViewModel(studyRepository, preferencesRepository) }
    val plannerViewModel = remember { PlannerViewModel(studyRepository, preferencesRepository) }
    val practiseViewModel = remember { PractiseViewModel(studyRepository) }
    val subjectsViewModel = remember { SubjectsViewModel(studyRepository) }
    val examsViewModel = remember { ExamsViewModel(studyRepository) }
    val sessionViewModel = remember { StudySessionViewModel(studyRepository) }
    val mistakesViewModel = remember { MistakesViewModel(studyRepository) }
    val notesViewModel = remember { NotesFlashcardsViewModel(studyRepository) }
    val progressViewModel = remember { ProgressViewModel(studyRepository) }
    val tutorViewModel = remember { TutorViewModel(studyRepository, preferencesRepository, orchestrator) }
    val settingsViewModel = remember {
        SettingsViewModel(preferencesRepository, studyRepository, database.aiProviderConfigDao())
    }

    val activeConfig by database.aiProviderConfigDao().getActiveConfigFlow().collectAsState(initial = null)

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    BottomNavItems.forEach { screen ->
                        val selected = currentRoute == screen.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (currentRoute != screen.route) {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                if (screen.icon != null) {
                                    Icon(
                                        imageVector = screen.icon,
                                        contentDescription = stringResource(screen.titleResId)
                                    )
                                }
                            },
                            label = { Text(stringResource(screen.titleResId)) },
                            modifier = Modifier.testTag("nav_item_${screen.route}")
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.Onboarding.route) {
                val onboardingVm = remember { OnboardingViewModel(preferencesRepository, studyRepository) }
                OnboardingScreen(
                    viewModel = onboardingVm,
                    onOnboardingFinished = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = homeViewModel,
                    onNavigateToPlan = { navController.navigate(Screen.Plan.route) },
                    onNavigateToPractise = { navController.navigate(Screen.Practise.route) },
                    onNavigateToTutor = { navController.navigate(Screen.Tutor.route) },
                    onNavigateToSession = { taskId ->
                        navController.navigate(Screen.StudySession.createRoute(taskId, null))
                    },
                    onNavigateToExams = { navController.navigate(Screen.Exams.route) },
                    onNavigateToSubjects = { navController.navigate(Screen.Subjects.route) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
                )
            }

            composable(Screen.Plan.route) {
                PlannerScreen(
                    viewModel = plannerViewModel,
                    onNavigateToSession = { taskId ->
                        navController.navigate(Screen.StudySession.createRoute(taskId, null))
                    },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
                )
            }

            composable(Screen.Practise.route) {
                PractiseScreen(
                    viewModel = practiseViewModel,
                    onNavigateToMistakes = { navController.navigate(Screen.Mistakes.route) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
                )
            }

            composable(Screen.Tutor.route) {
                TutorScreen(
                    viewModel = tutorViewModel,
                    activeProviderConfig = activeConfig,
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
                )
            }

            composable(Screen.Progress.route) {
                ProgressScreen(
                    viewModel = progressViewModel,
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
                )
            }

            // Sub-screens
            composable(Screen.Subjects.route) {
                SubjectsScreen(
                    viewModel = subjectsViewModel,
                    onNavigateToSubjectDetail = { subjectId ->
                        navController.navigate(Screen.SubjectDetail.createRoute(subjectId))
                    },
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
                )
            }

            composable(
                route = Screen.SubjectDetail.route,
                arguments = listOf(navArgument("subjectId") { type = NavType.StringType })
            ) { backStackEntry ->
                val subjectId = backStackEntry.arguments?.getString("subjectId") ?: ""
                SubjectDetailScreen(
                    subjectId = subjectId,
                    viewModel = subjectsViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Exams.route) {
                ExamsScreen(
                    viewModel = examsViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.StudySession.route,
                arguments = listOf(
                    navArgument("taskId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument("subjectId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val taskId = backStackEntry.arguments?.getString("taskId")
                val subjectId = backStackEntry.arguments?.getString("subjectId")
                StudySessionScreen(
                    taskId = taskId,
                    subjectId = subjectId,
                    viewModel = sessionViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Mistakes.route) {
                MistakesScreen(
                    viewModel = mistakesViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Notes.route) {
                NotesFlashcardsScreen(
                    viewModel = notesViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onResetToOnboarding = {
                        navController.navigate(Screen.Onboarding.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}
