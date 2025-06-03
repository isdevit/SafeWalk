package com.example.safewalk.viewmodels

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import com.example.safewalk.utils.PlacesService
import com.example.safewalk.utils.SafePlace
import com.example.safewalk.utils.SmsUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class DashboardUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val safePlaces: List<SafePlace> = emptyList(),
    val selectedPlace: SafePlace? = null,
    val showDirections: Boolean = false,
    val lastSosTime: Long? = null, // Track when last SOS was sent
    val destinationLocation: LatLng? = null
)

class DashboardViewModel : ViewModel() {
    private val _currentLocation = MutableStateFlow<LatLng?>(null)
    val currentLocation: StateFlow<LatLng?> = _currentLocation.asStateFlow()

    private val _lastSosTime = MutableStateFlow<Long?>(null)
    val lastSosTime: StateFlow<Long?> = _lastSosTime.asStateFlow()

    private val _sosAlertSent = MutableStateFlow(false)
    val sosAlertSent: StateFlow<Boolean> = _sosAlertSent.asStateFlow()

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var placesService: PlacesService? = null
    private var fusedLocationClient: FusedLocationProviderClient? = null

    fun initialize(context: Context, apiKey: String) {
        if (placesService == null) {
            placesService = PlacesService(context, apiKey)
        }
        if (fusedLocationClient == null) {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        }
        // Fetch location immediately upon initialization
        updateLocation(context)
    }

    fun updateLocation(context: Context) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)

                // Check location permissions
                val fineLocation = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
                val coarseLocation = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )

                if (fineLocation != PackageManager.PERMISSION_GRANTED &&
                    coarseLocation != PackageManager.PERMISSION_GRANTED) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Location permission not granted"
                    )
                    return@launch
                }
                
                val locationClient = fusedLocationClient ?: run {
                    fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                    fusedLocationClient!!
                }

                try {
                    val locationResult = locationClient.getCurrentLocation(
                        Priority.PRIORITY_HIGH_ACCURACY,
                        null
                    ).await()

                    locationResult?.let { location ->
                        val latLng = LatLng(location.latitude, location.longitude)
                        _currentLocation.value = latLng
                        
                        // Get nearby safe places
                        try {
                            placesService?.let { service ->
                                val places = service.getNearbyPlaces(latLng)
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    safePlaces = places,
                                    error = null
                                )
                            } ?: run {
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    error = "Places service not initialized"
                                )
                            }
                        } catch (e: Exception) {
                            Log.e("DashboardViewModel", "Error getting safe places", e)
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = "Error loading nearby safe places: ${e.message}"
                            )
                        }
                    } ?: run {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Could not get current location"
                        )
                    }
                } catch (e: SecurityException) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Location permission denied"
                    )
                }
            } catch (e: Exception) {
                Log.e("DashboardViewModel", "Error updating location", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error updating location: ${e.message}"
                )
            }
        }
    }

    fun sendSOSAlert(context: Context) {
        viewModelScope.launch {
            try {
                val location = currentLocation.value ?: return@launch
                SmsUtils.sendSOSAlert(context, location)
                    .onSuccess {
                        _lastSosTime.value = System.currentTimeMillis()
                        _sosAlertSent.value = true
                    }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun sendFalseAlarm(context: Context) {
        viewModelScope.launch {
            try {
                val location = currentLocation.value ?: return@launch
                SmsUtils.sendFalseAlarm(context, location)
                    .onSuccess {
                        _lastSosTime.value = null
                        _sosAlertSent.value = false
                    }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun selectPlace(place: SafePlace?) {
        _uiState.value = _uiState.value.copy(
            selectedPlace = place,
            showDirections = place != null
        )
    }

    fun searchLocation(context: Context, query: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val geocoder = Geocoder(context)
                val addresses = geocoder.getFromLocationName(query, 1)
                
                if (!addresses.isNullOrEmpty()) {
                    val location = addresses[0]
                    val latLng = LatLng(location.latitude, location.longitude)
                    _uiState.value = _uiState.value.copy(
                        destinationLocation = latLng,
                        showDirections = true,
                        isLoading = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        error = "Location not found",
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Error searching location: ${e.message}",
                    isLoading = false
                )
            }
        }
    }

    fun clearDirections() {
        _uiState.value = _uiState.value.copy(
            showDirections = false,
            selectedPlace = null,
            destinationLocation = null
        )
    }

}
