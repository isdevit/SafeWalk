package com.example.safewalk.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.SmsManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object SmsUtils {
    private const val MAPS_URL = "https://www.google.com/maps/search/?api=1&query="
    private const val TAG = "SmsUtils"
    
    // Fallback emergency contacts for testing
    private val DEFAULT_EMERGENCY_CONTACTS = listOf(
        "+917666892132"
        // Add default emergency contacts here
    )

    suspend fun sendSOSAlert(context: Context, location: LatLng): Result<Unit> {
        return try {
            Log.d(TAG, "Starting SOS alert process")
            
            // Check SMS permission
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "SMS permission not granted")
                return Result.failure(SecurityException("SMS permission not granted"))
            }

            val userId = FirebaseAuth.getInstance().currentUser?.uid
                ?: return Result.failure(Exception("User not authenticated"))

            // Get username
            val userDoc = FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .get()
                .await()

            val username = userDoc.getString("username") ?: "A SafeWalk user"

            // Get emergency contacts
            val contacts = FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .collection("contacts")
                .get()
                .await()
                .documents
                .mapNotNull { it.getString("phone") }

            if (contacts.isEmpty()) {
                Log.d(TAG, "Using default emergency contacts")
                return sendSOSAlertToDefaultContacts(context, location, username)
            }

            val locationUrl = "$MAPS_URL${location.latitude},${location.longitude}"
            val message = "SOS ALERT: $username needs immediate help! They are at this location: $locationUrl"

            try {
                // Use SmsManager.getDefault() for better compatibility
                val smsManager = SmsManager.getDefault()
                contacts.forEach { phoneNumber ->
                    try {
                        // Split message if it's too long
                        val messageParts = smsManager.divideMessage(message)
                        smsManager.sendMultipartTextMessage(
                            phoneNumber,
                            null,
                            messageParts,
                            null,
                            null
                        )
                        Log.d(TAG, "SMS sent successfully to $phoneNumber")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to send SMS to $phoneNumber", e)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing SmsManager", e)
                return Result.failure(e)
            }

            Log.d(TAG, "SOS alert process completed successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error in sendSOSAlert", e)
            e.printStackTrace()
            Result.failure(e)
        }
    }

    private suspend fun sendSOSAlertToDefaultContacts(context: Context, location: LatLng, username: String): Result<Unit> {
        return try {
            val locationUrl = "$MAPS_URL${location.latitude},${location.longitude}"
            val message = "SOS ALERT: $username needs immediate help! They are at this location: $locationUrl"

            try {
                val smsManager = SmsManager.getDefault()
                DEFAULT_EMERGENCY_CONTACTS.forEach { phoneNumber ->
                    try {
                        val messageParts = smsManager.divideMessage(message)
                        smsManager.sendMultipartTextMessage(
                            phoneNumber,
                            null,
                            messageParts,
                            null,
                            null
                        )
                        Log.d(TAG, "SMS sent successfully to default contact $phoneNumber")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to send SMS to default contact $phoneNumber", e)
                    }
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing SmsManager for default contacts", e)
                Result.failure(e)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in sendSOSAlertToDefaultContacts", e)
            Result.failure(e)
        }
    }

    suspend fun sendFalseAlarm(context: Context, location: LatLng): Result<Unit> {
        return try {
            Log.d(TAG, "Starting false alarm process")
            
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                return Result.failure(SecurityException("SMS permission not granted"))
            }

            val userId = FirebaseAuth.getInstance().currentUser?.uid
                ?: return Result.failure(Exception("User not authenticated"))

            val userDoc = FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .get()
                .await()

            val username = userDoc.getString("username") ?: "A SafeWalk user"

            val contacts = FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .collection("contacts")
                .get()
                .await()
                .documents
                .mapNotNull { it.getString("phone") }

            if (contacts.isEmpty()) {
                return sendFalseAlarmToDefaultContacts(context, location, username)
            }

            val locationUrl = "$MAPS_URL${location.latitude},${location.longitude}"
            val message = "FALSE ALARM: $username is safe. Previous SOS alert at this location can be disregarded: $locationUrl"

            try {
                val smsManager = SmsManager.getDefault()
                contacts.forEach { phoneNumber ->
                    try {
                        val messageParts = smsManager.divideMessage(message)
                        smsManager.sendMultipartTextMessage(
                            phoneNumber,
                            null,
                            messageParts,
                            null,
                            null
                        )
                        Log.d(TAG, "False alarm SMS sent successfully to $phoneNumber")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to send false alarm SMS to $phoneNumber", e)
                    }
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing SmsManager for false alarm", e)
                Result.failure(e)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in sendFalseAlarm", e)
            Result.failure(e)
        }
    }

    private suspend fun sendFalseAlarmToDefaultContacts(context: Context, location: LatLng, username: String): Result<Unit> {
        return try {
            val locationUrl = "$MAPS_URL${location.latitude},${location.longitude}"
            val message = "FALSE ALARM: $username is safe. Previous SOS alert at this location can be disregarded: $locationUrl"

            try {
                val smsManager = SmsManager.getDefault()
                DEFAULT_EMERGENCY_CONTACTS.forEach { phoneNumber ->
                    try {
                        val messageParts = smsManager.divideMessage(message)
                        smsManager.sendMultipartTextMessage(
                            phoneNumber,
                            null,
                            messageParts,
                            null,
                            null
                        )
                        Log.d(TAG, "False alarm SMS sent successfully to default contact $phoneNumber")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to send false alarm SMS to default contact $phoneNumber", e)
                    }
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing SmsManager for default contacts false alarm", e)
                Result.failure(e)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in sendFalseAlarmToDefaultContacts", e)
            Result.failure(e)
        }
    }
}
