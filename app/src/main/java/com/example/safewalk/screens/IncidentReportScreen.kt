package com.example.safewalk.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.safewalk.viewmodels.IncidentViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncidentReportScreen(
    onBack: () -> Unit,
    viewModel: IncidentViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("") }

    val incidentTypes = listOf(
        "Harassment",
        "Suspicious Activity",
        "Unsafe Area",
        "Other"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Report Incident") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Incident Type
            ExposedDropdownMenuBox(
                expanded = uiState.showTypeMenu,
                onExpandedChange = { viewModel.setShowTypeMenu(it) }
            ) {
                OutlinedTextField(
                    value = type,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Incident Type") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )

                ExposedDropdownMenu(
                    expanded = uiState.showTypeMenu,
                    onDismissRequest = { viewModel.setShowTypeMenu(false) }
                ) {
                    incidentTypes.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                type = option
                                viewModel.setShowTypeMenu(false)
                            }
                        )
                    }
                }
            }

            // Title
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth()
            )

            // Description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                maxLines = 10
            )

            // Submit Button
            Button(
                onClick = {
                    viewModel.submitIncident(
                        context = context,
                        title = title,
                        description = description,
                        type = type
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = title.isNotBlank() && description.isNotBlank() && type.isNotBlank()
            ) {
                Icon(Icons.Default.Send, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Submit Report")
            }
        }
    }

    // Show loading dialog
    if (uiState.isLoading) {
        AlertDialog(
            onDismissRequest = { },
            confirmButton = { },
            title = { Text("Submitting Report") },
            text = { 
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    CircularProgressIndicator()
                    Text("Please wait...")
                }
            }
        )
    }

    // Show success/error dialog
    LaunchedEffect(uiState.message, uiState.error) {
        uiState.message?.let { message ->
            // Clear form and go back
            title = ""
            description = ""
            type = ""
            onBack()
        }
        uiState.error?.let { error ->
            // Show error snackbar
        }
    }
}
