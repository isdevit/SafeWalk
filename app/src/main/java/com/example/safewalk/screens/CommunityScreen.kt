package com.example.safewalk.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.safewalk.models.Incident
import com.example.safewalk.viewmodels.CommunityViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreen(
    onBack: () -> Unit,
    viewModel: CommunityViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedIncident by remember { mutableStateOf<Incident?>(null) }
    var showCommentDialog by remember { mutableStateOf(false) }
    var commentText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Community Support") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(uiState.incidents) { incident ->
                    IncidentCard(
                        incident = incident,
                        onCommentClick = {
                            selectedIncident = incident
                            showCommentDialog = true
                        }
                    )
                }
            }

            // Loading indicator
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }

    // Comment Dialog
    if (showCommentDialog && selectedIncident != null) {
        AlertDialog(
            onDismissRequest = { 
                showCommentDialog = false
                selectedIncident = null
                commentText = ""
            },
            title = { Text("Add Comment") },
            text = {
                OutlinedTextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    label = { Text("Your comment") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addComment(selectedIncident!!.id, commentText)
                        showCommentDialog = false
                        selectedIncident = null
                        commentText = ""
                    },
                    enabled = commentText.isNotBlank()
                ) {
                    Text("Post")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showCommentDialog = false
                        selectedIncident = null
                        commentText = ""
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IncidentCard(
    incident: Incident,
    onCommentClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { /* Show full incident details */ }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = incident.type,
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    text = dateFormat.format(incident.timestamp.toDate()),
                    style = MaterialTheme.typography.labelSmall
                )
            }

            // Title
            Text(
                text = incident.title,
                style = MaterialTheme.typography.titleMedium
            )

            // Description
            Text(
                text = incident.description,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            // Posted by
            Text(
                text = "Posted by ${incident.username}",
                style = MaterialTheme.typography.labelSmall
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Comments
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${incident.comments.size} comments",
                    style = MaterialTheme.typography.labelMedium
                )
                TextButton(onClick = onCommentClick) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Comment")
                }
            }
        }
    }
}
