package com.example.safewalk.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.safewalk.utils.PreferencesManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class AuthUiState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val error: String? = null
)

class AuthViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        // Check if user is already logged in
        FirebaseAuth.getInstance().currentUser?.let {
            _uiState.value = _uiState.value.copy(isLoggedIn = true)
        }
    }

    fun login(context: Context, email: String, password: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)

                val authResult = FirebaseAuth.getInstance()
                    .signInWithEmailAndPassword(email, password)
                    .await()

                // Save credentials after successful login
                PreferencesManager.saveUserData(
                    context = context,
                    email = email,
                    password = password,
                    userId = authResult.user?.uid
                )

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isLoggedIn = true,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Login failed"
                )
            }
        }
    }

    fun register(context: Context, username: String, email: String, password: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)

                val auth = FirebaseAuth.getInstance()
                val authResult = auth.createUserWithEmailAndPassword(email, password).await()

                // Save user data to Firestore
                val user = hashMapOf(
                    "username" to username,
                    "email" to email,
                    "createdAt" to System.currentTimeMillis()
                )

                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(authResult.user?.uid ?: "")
                    .set(user)
                    .await()

                // Save credentials for auto-login
                PreferencesManager.saveUserData(
                    context = context,
                    email = email,
                    password = password,
                    username = username,
                    userId = authResult.user?.uid
                )

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isLoggedIn = true,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Registration failed"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun logout(context: Context) {
        FirebaseAuth.getInstance().signOut()
        PreferencesManager.clearUserData(context)
        _uiState.value = AuthUiState()
    }
}
