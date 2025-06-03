package com.example.safewalk.utils

import android.content.Context
import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.cos
import kotlin.math.PI

data class SafePlace(
    val id: String,
    val name: String,
    val type: String,
    val location: LatLng,
    val distance: Float,
    val rating: Double?
)

class PlacesService(context: Context, apiKey: String) {
    private val placesClient: PlacesClient
    
    init {
        if (!Places.isInitialized()) {
            Places.initialize(context, apiKey)
        }
        placesClient = Places.createClient(context)
    }
    
    suspend fun getNearbyPlaces(
        location: LatLng,
        radius: Double = 1500.0 // 1.5km radius
    ): List<SafePlace> = suspendCancellableCoroutine { continuation ->
        try {
            // Calculate bounds for the search area
            val latRadian = location.latitude * PI / 180
            val degLatKm = 110.574 // km per degree of latitude
            val degLongKm = 111.320 * cos(latRadian) // km per degree of longitude
            val deltaLat = radius / 1000 / degLatKm
            val deltaLong = radius / 1000 / degLongKm

            val bounds = RectangularBounds.newInstance(
                LatLng(location.latitude - deltaLat, location.longitude - deltaLong),
                LatLng(location.latitude + deltaLat, location.longitude + deltaLong)
            )

            val types = listOf("police", "hospital", "pharmacy", "fire_station")
            val safetyPlaces = mutableListOf<SafePlace>()

            for (type in types) {
                val request = FindAutocompletePredictionsRequest.builder()
                    .setLocationBias(bounds)
                    .setTypesFilter(listOf(type))
                    .setQuery("")  // Empty query to get all places of this type
                    .build()

                placesClient.findAutocompletePredictions(request)
                    .addOnSuccessListener { response ->
                        val predictions = response.autocompletePredictions
                        for (prediction in predictions) {
                            val placeFields = listOf(
                                Place.Field.ID,
                                Place.Field.NAME,
                                Place.Field.LAT_LNG,
                                Place.Field.TYPES,
                                Place.Field.RATING
                            )

                            val fetchPlaceRequest = FetchPlaceRequest.builder(
                                prediction.placeId, 
                                placeFields
                            ).build()

                            placesClient.fetchPlace(fetchPlaceRequest)
                                .addOnSuccessListener { fetchResponse ->
                                    val place = fetchResponse.place
                                    place.latLng?.let { placeLocation ->
                                        safetyPlaces.add(
                                            SafePlace(
                                                id = place.id ?: "",
                                                name = place.name ?: "",
                                                type = type.split("_").joinToString(" ") { word ->
                                                    word.lowercase().replaceFirstChar { it.uppercase() }
                                                },
                                                location = placeLocation,
                                                distance = calculateDistance(location, placeLocation),
                                                rating = place.rating
                                            )
                                        )
                                    }
                                    
                                    // If we've processed all places, return the results
                                    if (safetyPlaces.size >= predictions.size * types.indexOf(type) + predictions.size) {
                                        continuation.resume(safetyPlaces.sortedBy { it.distance })
                                    }
                                }
                                .addOnFailureListener { exception ->
                                    Log.e("PlacesService", "Error fetching place details", exception)
                                }
                        }
                    }
                    .addOnFailureListener { exception ->
                        Log.e("PlacesService", "Error finding places", exception)
                        continuation.resumeWithException(exception)
                    }
            }
        } catch (e: Exception) {
            Log.e("PlacesService", "Error in getNearbyPlaces", e)
            continuation.resumeWithException(e)
        }
    }
    
    private fun calculateDistance(from: LatLng, to: LatLng): Float {
        val results = FloatArray(1)
        android.location.Location.distanceBetween(
            from.latitude,
            from.longitude,
            to.latitude,
            to.longitude,
            results
        )
        return results[0]
    }
}
