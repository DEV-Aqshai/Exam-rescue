package com.example.ui.screens.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    onOnboardingFinished: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    Scaffold(
        bottomBar = {
            Surface(
                tonalElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (state.step > 0) {
                        OutlinedButton(
                            onClick = { viewModel.previousStep() },
                            modifier = Modifier.testTag("onboarding_prev_button")
                        ) {
                            Text(stringResource(R.string.action_back))
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    if (state.step < 2) {
                        Button(
                            onClick = { viewModel.nextStep() },
                            modifier = Modifier.testTag("onboarding_next_button")
                        ) {
                            Text(stringResource(R.string.action_next))
                        }
                    } else {
                        Button(
                            onClick = { viewModel.completeOnboarding(onOnboardingFinished) },
                            modifier = Modifier.testTag("onboarding_finish_button")
                        ) {
                            Text(stringResource(R.string.action_get_started))
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (state.step) {
                0 -> WelcomeAndLanguageStep(viewModel, state)
                1 -> SubjectsAndExamStep(viewModel, state)
                2 -> AvailabilityAndPrivacyStep(viewModel, state)
            }
        }
    }
}

@Composable
private fun ColumnScope.WelcomeAndLanguageStep(
    viewModel: OnboardingViewModel,
    state: OnboardingUiState
) {
    Icon(
        imageVector = Icons.Default.School,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(56.dp)
    )
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = stringResource(R.string.onboarding_welcome_title),
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = stringResource(R.string.onboarding_welcome_subtitle),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(20.dp))
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.CloudOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.onboarding_offline_notice),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    Spacer(modifier = Modifier.height(24.dp))
    Text(
        text = stringResource(R.string.onboarding_lang_title),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.align(Alignment.Start)
    )
    Spacer(modifier = Modifier.height(8.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        ChoiceCard(
            title = stringResource(R.string.lang_english),
            isSelected = state.interfaceLanguage == "en",
            onClick = { viewModel.setInterfaceLanguage("en") },
            modifier = Modifier.weight(1f)
        )
        ChoiceCard(
            title = stringResource(R.string.lang_tamil),
            isSelected = state.interfaceLanguage == "ta",
            onClick = { viewModel.setInterfaceLanguage("ta") },
            modifier = Modifier.weight(1f)
        )
    }

    Spacer(modifier = Modifier.height(20.dp))
    Text(
        text = stringResource(R.string.onboarding_medium_title),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.align(Alignment.Start)
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = stringResource(R.string.onboarding_medium_desc),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.align(Alignment.Start)
    )
    Spacer(modifier = Modifier.height(8.dp))
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ChoiceCard(
            title = stringResource(R.string.medium_tamil),
            isSelected = state.learningMedium == "TAMIL",
            onClick = { viewModel.setLearningMedium("TAMIL") },
            modifier = Modifier.fillMaxWidth()
        )
        ChoiceCard(
            title = stringResource(R.string.medium_english),
            isSelected = state.learningMedium == "ENGLISH",
            onClick = { viewModel.setLearningMedium("ENGLISH") },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ColumnScope.SubjectsAndExamStep(
    viewModel: OnboardingViewModel,
    state: OnboardingUiState
) {
    Text(
        text = stringResource(R.string.onboarding_subjects_title),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(6.dp))
    Text(
        text = stringResource(R.string.onboarding_subjects_subtitle),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(16.dp))
    val defaultSubjects = listOf(
        "math" to "Mathematics (கணிதம்)",
        "science" to "Science (விஞ்ஞானம்)",
        "english" to "English Language (ஆங்கிலம்)",
        "history" to "History (வரலாறு)",
        "tamil" to "Tamil Literature (தமிழ்)",
        "ict" to "ICT (தகவல் தொழினுட்பம்)"
    )

    defaultSubjects.forEach { (id, name) ->
        val selected = state.selectedSubjectIds.contains(id)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clickable { viewModel.toggleSubject(id) },
            colors = CardDefaults.cardColors(
                containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = selected,
                    onCheckedChange = { viewModel.toggleSubject(id) }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(20.dp))
    Text(
        text = stringResource(R.string.onboarding_target_exam_title),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.align(Alignment.Start)
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = stringResource(R.string.onboarding_target_exam_subtitle),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.align(Alignment.Start)
    )
    Spacer(modifier = Modifier.height(8.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ChoiceCard(
            title = "Target ~6 Months",
            isSelected = state.isExamDateSet,
            onClick = { viewModel.setExamDate(System.currentTimeMillis() + (180L * 86_400_000L), true) },
            modifier = Modifier.weight(1f)
        )
        ChoiceCard(
            title = stringResource(R.string.onboarding_not_sure),
            isSelected = !state.isExamDateSet,
            onClick = { viewModel.setExamDate(0L, false) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ColumnScope.AvailabilityAndPrivacyStep(
    viewModel: OnboardingViewModel,
    state: OnboardingUiState
) {
    Text(
        text = stringResource(R.string.onboarding_availability_title),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(6.dp))
    Text(
        text = stringResource(R.string.onboarding_availability_subtitle),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(20.dp))
    Text(
        text = "${state.dailyMinutes} mins / day",
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold
    )
    Slider(
        value = state.dailyMinutes.toFloat(),
        onValueChange = { viewModel.setDailyMinutes(it.toInt()) },
        valueRange = 30f..180f,
        steps = 4,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
    )

    Spacer(modifier = Modifier.height(24.dp))
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.onboarding_ai_disclosure_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.onboarding_ai_disclosure_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
private fun ChoiceCard(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.5.dp,
            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
