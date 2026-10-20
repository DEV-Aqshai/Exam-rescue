package com.example.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ai.models.ProviderType
import com.example.ui.components.ExamRescueTopAppBar
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.SuccessGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    onResetToOnboarding: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    var isKeyVisible by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            ExamRescueTopAppBar(
                title = stringResource(R.string.settings_title),
                onNavigateBack = onNavigateBack
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Section: Language & Medium
            SettingsSectionHeader(title = "Language & Study Medium")

            // Interface Language
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = stringResource(R.string.settings_interface_lang), style = MaterialTheme.typography.bodyLarge)
                SingleChoiceSegmentedButtonRow {
                    SegmentedButton(
                        selected = state.preferences.interfaceLanguage == "en",
                        onClick = { viewModel.setInterfaceLanguage("en") },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) {
                        Text("English")
                    }
                    SegmentedButton(
                        selected = state.preferences.interfaceLanguage == "ta",
                        onClick = { viewModel.setInterfaceLanguage("ta") },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) {
                        Text("தமிழ்")
                    }
                }
            }

            // Learning Medium
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = stringResource(R.string.settings_learning_medium), style = MaterialTheme.typography.bodyLarge)
                SingleChoiceSegmentedButtonRow {
                    SegmentedButton(
                        selected = state.preferences.learningMedium == "TAMIL",
                        onClick = { viewModel.setLearningMedium("TAMIL") },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) {
                        Text("Tamil")
                    }
                    SegmentedButton(
                        selected = state.preferences.learningMedium == "ENGLISH",
                        onClick = { viewModel.setLearningMedium("ENGLISH") },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) {
                        Text("English")
                    }
                }
            }

            HorizontalDivider()

            // Section: Daily Study Goal
            SettingsSectionHeader(title = "Study Schedule & Goals")
            Text(
                text = "${stringResource(R.string.settings_daily_minutes)}: ${state.preferences.dailyStudyMinutes} mins",
                style = MaterialTheme.typography.bodyMedium
            )
            Slider(
                value = state.preferences.dailyStudyMinutes.toFloat(),
                onValueChange = { viewModel.setDailyMinutes(it.toInt()) },
                valueRange = 30f..180f,
                steps = 4,
                modifier = Modifier.fillMaxWidth().testTag("settings_daily_minutes_slider")
            )

            HorizontalDivider()

            // Section: AI Assistant Configuration
            SettingsSectionHeader(title = stringResource(R.string.settings_ai_section))

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = stringResource(R.string.settings_ai_keystore_notice),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.settings_ai_cost_warning),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            // Provider Dropdown
            var providerMenuExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = providerMenuExpanded,
                onExpandedChange = { providerMenuExpanded = it }
            ) {
                OutlinedTextField(
                    value = state.selectedProvider.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.settings_ai_provider)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = providerMenuExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth().testTag("ai_provider_dropdown")
                )
                ExposedDropdownMenu(
                    expanded = providerMenuExpanded,
                    onDismissRequest = { providerMenuExpanded = false }
                ) {
                    ProviderType.entries.forEach { provider ->
                        DropdownMenuItem(
                            text = { Text(provider.displayName) },
                            onClick = {
                                viewModel.selectProvider(provider)
                                providerMenuExpanded = false
                            }
                        )
                    }
                }
            }

            // API Key Input
            OutlinedTextField(
                value = state.apiKey,
                onValueChange = { viewModel.updateApiKey(it) },
                label = { Text(stringResource(R.string.settings_ai_key)) },
                visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { isKeyVisible = !isKeyVisible }) {
                        Icon(
                            imageVector = if (isKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Toggle visibility"
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().testTag("api_key_input"),
                singleLine = true
            )

            // Model ID
            OutlinedTextField(
                value = state.modelId,
                onValueChange = { viewModel.updateModelId(it) },
                label = { Text(stringResource(R.string.settings_ai_model)) },
                supportingText = { Text(stringResource(R.string.settings_ai_model_hint)) },
                modifier = Modifier.fillMaxWidth().testTag("model_id_input"),
                singleLine = true
            )

            // Endpoint (editable only for custom)
            OutlinedTextField(
                value = state.endpoint,
                onValueChange = { if (state.selectedProvider.isCustom) viewModel.updateEndpoint(it) },
                label = { Text(stringResource(R.string.settings_ai_endpoint)) },
                readOnly = !state.selectedProvider.isCustom,
                modifier = Modifier.fillMaxWidth().testTag("endpoint_input"),
                singleLine = true
            )

            // Save Key & Test Connection Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.saveProviderConfig() },
                    modifier = Modifier.weight(1f).testTag("save_ai_config_button")
                ) {
                    Text(stringResource(R.string.action_save))
                }

                OutlinedButton(
                    onClick = { viewModel.testConnection() },
                    enabled = !state.isTestingConnection && state.apiKey.isNotBlank(),
                    modifier = Modifier.weight(1f).testTag("test_connection_button")
                ) {
                    if (state.isTestingConnection) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text(stringResource(R.string.action_test_connection))
                    }
                }

                if (state.isKeySaved) {
                    OutlinedButton(
                        onClick = { viewModel.removeApiKey() },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
                        modifier = Modifier.testTag("remove_key_button")
                    ) {
                        Text(stringResource(R.string.action_remove_key))
                    }
                }
            }

            // Test Connection Feedback
            if (state.testConnectionMessage != null) {
                Surface(
                    color = if (state.isTestSuccess == true) SuccessGreen.copy(alpha = 0.15f) else ErrorRed.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = state.testConnectionMessage!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (state.isTestSuccess == true) SuccessGreen else ErrorRed,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            HorizontalDivider()

            // Section: Data Management & Backup
            SettingsSectionHeader(title = stringResource(R.string.settings_data_section))
            Text(
                text = stringResource(R.string.settings_export_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { viewModel.exportData() },
                    modifier = Modifier.weight(1f).testTag("export_data_button")
                ) {
                    Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.action_export))
                }

                OutlinedButton(
                    onClick = { showImportDialog = true },
                    modifier = Modifier.weight(1f).testTag("import_data_button")
                ) {
                    Icon(imageVector = Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.action_import))
                }
            }

            if (state.importMessage != null) {
                Text(
                    text = state.importMessage!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Export result display
            if (state.lastExportedJson != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = "Backup JSON Generated (${state.lastExportedJson!!.length} bytes):", style = MaterialTheme.typography.labelSmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = state.lastExportedJson!!.take(200) + "...",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // Reset All Data Button
            Button(
                onClick = { viewModel.setShowDeleteConfirm(true) },
                colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                modifier = Modifier.fillMaxWidth().testTag("delete_all_data_button")
            ) {
                Icon(imageVector = Icons.Default.DeleteForever, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.action_delete_all))
            }

            HorizontalDivider()

            // About Section
            SettingsSectionHeader(title = stringResource(R.string.settings_about_title))
            Text(
                text = stringResource(R.string.settings_about_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(R.string.settings_version),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.height(30.dp))
        }
    }

    // Delete Confirmation Dialog
    if (state.showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { viewModel.setShowDeleteConfirm(false) },
            title = { Text(stringResource(R.string.settings_delete_confirm_title)) },
            text = { Text(stringResource(R.string.settings_delete_confirm_body)) },
            confirmButton = {
                Button(
                    onClick = { viewModel.deleteAllData(onResetToOnboarding) },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                    modifier = Modifier.testTag("confirm_delete_all_button")
                ) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.setShowDeleteConfirm(false) }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    // Import Data Dialog
    if (showImportDialog) {
        var importJson by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text(stringResource(R.string.action_import)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.settings_import_desc), style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = importJson,
                        onValueChange = { importJson = it },
                        placeholder = { Text("Paste valid backup JSON here...") },
                        modifier = Modifier.fillMaxWidth().height(140.dp).testTag("import_json_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (importJson.isNotBlank()) {
                            viewModel.importData(importJson)
                            showImportDialog = false
                        }
                    },
                    modifier = Modifier.testTag("confirm_import_button")
                ) {
                    Text(stringResource(R.string.action_import))
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
}
