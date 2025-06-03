package com.example.safewalk.models

import com.google.android.gms.maps.model.LatLng
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.GeoPoint

data class Incident(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val username: String = "",
    val title: String = "",
    val description: String = "",
    val location: GeoPoint? = null,
    val timestamp: Timestamp = Timestamp.now(),
    val type: String = "",
    val comments: List<Comment> = emptyList()
)

data class Comment(
    val id: String = "",
    val userId: String = "",
    val username: String = "",
    val content: String = "",
    val timestamp: Timestamp = Timestamp.now()
)

fun GeoPoint.toLatLng(): LatLng = LatLng(latitude, longitude)
fun LatLng.toGeoPoint(): GeoPoint = GeoPoint(latitude, longitude)
