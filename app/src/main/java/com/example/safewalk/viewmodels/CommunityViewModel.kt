package com.example.safewalk.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.safewalk.models.Comment
import com.example.safewalk.models.Incident
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class CommunityUiState(
    val isLoading: Boolean = false,
    val incidents: List<Incident> = emptyList(),
    val comments: Map<String, List<Comment>> = emptyMap(),
    val error: String? = null
)

class CommunityViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(CommunityUiState())
    val uiState: StateFlow<CommunityUiState> = _uiState.asStateFlow()

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    init {
        loadIncidents()
    }

    private fun loadIncidents() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)

                // Listen for real-time updates on incidents
                db.collection("incidents")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .addSnapshotListener { snapshot, e ->
                        if (e != null) {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = e.message
                            )
                            return@addSnapshotListener
                        }

                        val incidents = snapshot?.documents?.mapNotNull { doc ->
                            doc.toObject(Incident::class.java)?.copy(id = doc.id)
                        } ?: emptyList()

                        _uiState.value = _uiState.value.copy(
                            incidents = incidents
                        )

                        // Load comments for each incident
                        incidents.forEach { incident ->
                            loadCommentsForIncident(incident.id)
                        }
                    }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    private fun loadCommentsForIncident(incidentId: String) {
        db.collection("incidents")
            .document(incidentId)
            .collection("comments")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    _uiState.value = _uiState.value.copy(
                        error = "Error loading comments: ${e.message}"
                    )
                    return@addSnapshotListener
                }

                val comments = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Comment::class.java)?.copy(id = doc.id)
                } ?: emptyList()

                _uiState.value = _uiState.value.copy(
                    comments = _uiState.value.comments + (incidentId to comments)
                )
            }
    }

    fun addComment(incidentId: String, content: String) {
        viewModelScope.launch {
            try {
                val user = auth.currentUser ?: throw Exception("User not logged in")
                
                // Get user's username from Firestore
                val userDoc = db.collection("users")
                    .document(user.uid)
                    .get()
                    .await()
                
                val username = userDoc.getString("username") ?: user.email ?: "Anonymous"

                val comment = Comment(
                    userId = user.uid,
                    username = username,
                    content = content,
                    timestamp = com.google.firebase.Timestamp.now()
                )

                // Add comment to the incident's comments subcollection
                db.collection("incidents")
                    .document(incidentId)
                    .collection("comments")
                    .add(comment)
                    .await()

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
