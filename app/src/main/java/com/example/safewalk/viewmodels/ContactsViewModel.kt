package com.example.safewalk.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.safewalk.models.Contact
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class ContactsUiState(
    val isLoading: Boolean = false,
    val contacts: List<Contact> = emptyList(),
    val error: String? = null
)

class ContactsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ContactsUiState())
    val uiState: StateFlow<ContactsUiState> = _uiState.asStateFlow()

    init {
        loadContacts()
    }

    private fun loadContacts() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)

                val userId = FirebaseAuth.getInstance().currentUser?.uid
                    ?: throw Exception("User not logged in")

                val snapshot = FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(userId)
                    .collection("contacts")
                    .get()
                    .await()

                val contacts = snapshot.documents.map { doc ->
                    Contact(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        phone = doc.getString("phone") ?: ""
                    )
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    contacts = contacts,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Error loading contacts"
                )
            }
        }
    }

    fun addContact(name: String, phone: String) {
        viewModelScope.launch {
            try {
                val userId = FirebaseAuth.getInstance().currentUser?.uid
                    ?: throw Exception("User not logged in")

                val contactData = hashMapOf(
                    "name" to name,
                    "phone" to phone
                )

                val docRef = FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(userId)
                    .collection("contacts")
                    .add(contactData)
                    .await()

                val newContact = Contact(docRef.id, name, phone)
                val currentContacts = _uiState.value.contacts.toMutableList()
                currentContacts.add(newContact)

                _uiState.value = _uiState.value.copy(
                    contacts = currentContacts,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Error adding contact"
                )
            }
        }
    }

    fun updateContact(contact: Contact) {
        viewModelScope.launch {
            try {
                val userId = FirebaseAuth.getInstance().currentUser?.uid
                    ?: throw Exception("User not logged in")

                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(userId)
                    .collection("contacts")
                    .document(contact.id)
                    .update(
                        mapOf(
                            "name" to contact.name,
                            "phone" to contact.phone
                        )
                    )
                    .await()

                val currentContacts = _uiState.value.contacts.toMutableList()
                val index = currentContacts.indexOfFirst { it.id == contact.id }
                if (index != -1) {
                    currentContacts[index] = contact
                    _uiState.value = _uiState.value.copy(
                        contacts = currentContacts,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Error updating contact"
                )
            }
        }
    }

    fun deleteContact(contact: Contact) {
        viewModelScope.launch {
            try {
                val userId = FirebaseAuth.getInstance().currentUser?.uid
                    ?: throw Exception("User not logged in")

                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(userId)
                    .collection("contacts")
                    .document(contact.id)
                    .delete()
                    .await()

                val currentContacts = _uiState.value.contacts.filter { it.id != contact.id }
                _uiState.value = _uiState.value.copy(
                    contacts = currentContacts,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Error deleting contact"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
