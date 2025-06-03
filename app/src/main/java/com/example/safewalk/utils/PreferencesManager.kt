package com.example.safewalk.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object PreferencesManager {
    private const val TAG = "PreferencesManager"
    private const val PREF_NAME = "safewalk_prefs"
    private const val KEY_USER_EMAIL = "user_email"
    private const val KEY_USER_PASSWORD = "user_password"
    private const val KEY_USER_NAME = "user_name"
    private const val KEY_USER_ID = "user_id"

    private fun getEncryptedSharedPreferences(context: Context): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                PREF_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error creating EncryptedSharedPreferences", e)
            // Fallback to regular SharedPreferences if encryption fails
            context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        }
    }

    fun saveUserData(
        context: Context,
        email: String,
        password: String,
        username: String? = null,
        userId: String? = null
    ) {
        val prefs = getEncryptedSharedPreferences(context)
        prefs.edit().apply {
            putString(KEY_USER_EMAIL, email)
            putString(KEY_USER_PASSWORD, password)
            username?.let { putString(KEY_USER_NAME, it) }
            userId?.let { putString(KEY_USER_ID, it) }
            apply()
        }
        Log.d(TAG, "User data saved successfully")
    }

    fun getUserData(context: Context): UserData {
        val prefs = getEncryptedSharedPreferences(context)
        return UserData(
            email = prefs.getString(KEY_USER_EMAIL, null),
            password = prefs.getString(KEY_USER_PASSWORD, null),
            username = prefs.getString(KEY_USER_NAME, null),
            userId = prefs.getString(KEY_USER_ID, null)
        )
    }

    fun clearUserData(context: Context) {
        val prefs = getEncryptedSharedPreferences(context)
        prefs.edit().clear().apply()
        Log.d(TAG, "User data cleared")
    }
}

data class UserData(
    val email: String?,
    val password: String?,
    val username: String?,
    val userId: String?
)
