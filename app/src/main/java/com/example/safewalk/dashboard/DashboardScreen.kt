package com.example.safewalk.dashboard

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.safewalk.R
import com.example.safewalk.contacts.ContactsScreen
import com.example.safewalk.screens.CommunityScreen
import com.example.safewalk.screens.IncidentReportScreen
import com.example.safewalk.viewmodels.AuthViewModel
import com.example.safewalk.viewmodels.DashboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToLogin: () -> Unit,
    viewModel: DashboardViewModel = viewModel(),
    viewModel2: AuthViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sosAlertSent by viewModel.sosAlertSent.collectAsStateWithLifecycle()
    
    var showSosDialog by remember { mutableStateOf(false) }
    var showContactsDialog by remember { mutableStateOf(false) }
    var showMapScreen by remember { mutableStateOf(false) }
    var showIncidentReport by remember { mutableStateOf(false) }
    var showCommunitySupport by remember { mutableStateOf(false) }

    if (showMapScreen) {
        MapScreen(
            viewModel = viewModel,
            onBack = { showMapScreen = false }
        )
    } else if (showIncidentReport) {
        IncidentReportScreen(
            onBack = { showIncidentReport = false }
        )
    } else if (showCommunitySupport) {
        CommunityScreen(
            onBack = { showCommunitySupport = false }
        )
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            // Background gradient
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surface,
                                MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Top App Bar with modern design
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "SafeWalk",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = {
                                viewModel2.logout(context)
                                onNavigateToLogin()
                            },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Icon(Icons.Rounded.ExitToApp, "Logout")
                        }
                    }
                }

                // SOS Button with pulsating animation
                Box(
                    modifier = Modifier.size(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = { showSosDialog = true },
                        modifier = Modifier.size(140.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        shape = CircleShape
                    ) {
                        Text(
                            "SOS",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Feature Cards Grid
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                    ),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            "Quick Actions",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(8.dp)
                        )

                        // First Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            FeatureCard(
                                title = "Location",
                                icon = Icons.Rounded.LocationOn,
                                onClick = { showMapScreen = true },
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.primary
                            )

                            FeatureCard(
                                title = "Report",
                                icon = Icons.Rounded.Warning,
                                onClick = { showIncidentReport = true },
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }

                        // Second Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            FeatureCard(
                                title = "Community",
                                icon = Icons.Rounded.Home,
                                onClick = { showCommunitySupport = true },
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.tertiary
                            )

                            if (sosAlertSent) {
                                FeatureCard(
                                    title = "False Alarm",
                                    icon = Icons.Rounded.Close,
                                    onClick = { viewModel.sendFalseAlarm(context) },
                                    modifier = Modifier.weight(1f),
                                    color = MaterialTheme.colorScheme.error
                                )
                            } else {
                                FeatureCard(
                                    title = "Contacts",
                                    icon = Icons.Rounded.Phone,
                                    onClick = { showContactsDialog = true },
                                    modifier = Modifier.weight(1f),
                                    color = MaterialTheme.colorScheme.surfaceTint
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Show SOS confirmation dialog with improved design
    if (showSosDialog) {
        AlertDialog(
            onDismissRequest = { showSosDialog = false },
            title = { 
                Text(
                    "Send SOS Alert",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            },
            text = { 
                Text(
                    "Are you sure you want to send an SOS alert to your emergency contacts?",
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.sendSOSAlert(context)
                        showSosDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Send Alert", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showSosDialog = false },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Show contacts dialog
    if (showContactsDialog) {
        ContactsScreen(onDismiss = { showContactsDialog = false })
    }
}

@ExperimentalMaterial3Api
@Composable
private fun FeatureCard(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .height(120.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = color
            )
        }
    }
}
