package com.example.safewalk.repository

import com.example.safewalk.models.Comment
import com.example.safewalk.models.Incident
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await

class IncidentRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val incidentsCollection = firestore.collection("incidents")

    suspend fun createIncident(incident: Incident): Result<String> {
        return try {
            val docRef = incidentsCollection.add(incident).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getIncidents(): Flow<List<Incident>> = flow {
        try {
            val snapshot = incidentsCollection
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()
            
            val incidents = snapshot.toObjects(Incident::class.java)
            emit(incidents)
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    suspend fun addComment(incidentId: String, comment: Comment): Result<String> {
        return try {
            val commentRef = incidentsCollection
                .document(incidentId)
                .collection("comments")
                .add(comment)
                .await()

            Result.success(commentRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getComments(incidentId: String): Flow<List<Comment>> = flow {
        try {
            val snapshot = incidentsCollection
                .document(incidentId)
                .collection("comments")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .await()
            
            val comments = snapshot.toObjects(Comment::class.java)
            emit(comments)
        } catch (e: Exception) {
            emit(emptyList())
        }
    }
}
