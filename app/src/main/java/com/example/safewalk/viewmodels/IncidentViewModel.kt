package com.example.safewalk.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.safewalk.models.Incident
import com.example.safewalk.repository.IncidentRepository
import com.google.android.gms.location.LocationServices
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class IncidentUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val showTypeMenu: Boolean = false
)

class IncidentViewModel : ViewModel() {
    private val repository = IncidentRepository()
    private val _uiState = MutableStateFlow(IncidentUiState())
    val uiState: StateFlow<IncidentUiState> = _uiState.asStateFlow()

    fun setShowTypeMenu(show: Boolean) {
        _uiState.value = _uiState.value.copy(showTypeMenu = show)
    }

    fun submitIncident(
        context: Context,
        title: String,
        description: String,
        type: String
    ) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)

                val userId = FirebaseAuth.getInstance().currentUser?.uid
                    ?: throw Exception("User not authenticated")

                // Get username
                val userDoc = FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(userId)
                    .get()
                    .await()

                val username = userDoc.getString("username")
                    ?: throw Exception("Username not found")

                // Get current location using FusedLocationProviderClient
                val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                val location = try {
                    fusedLocationClient.lastLocation.await()
                        ?: throw Exception("Location not available")
                } catch (e: SecurityException) {
                    throw Exception("Location permission not granted")
                }

                val geoPoint = GeoPoint(location.latitude, location.longitude)

                val incident = Incident(
                    userId = userId,
                    username = username,
                    title = title,
                    description = description,
                    location = geoPoint,
                    type = type,
                    timestamp = Timestamp.now()
                )

                repository.createIncident(incident)
                    .onSuccess {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            message = "Incident reported successfully",
                            error = null
                        )
                    }
                    .onFailure { error ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = error.message ?: "Failed to report incident"
                        )
                    }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "An error occurred"
                )
            }
        }
    }
}
