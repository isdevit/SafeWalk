package com.example.safewalk.contacts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.safewalk.models.Contact

@Composable
fun ContactDialog(
    contact: Contact? = null,
    onDismiss: () -> Unit,
    onSave: (name: String, phone: String) -> Unit
) {
    var name by remember { mutableStateOf(contact?.name ?: "") }
    var phone by remember { mutableStateOf(contact?.phone ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (contact == null) "Add Contact" else "Edit Contact") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = { Text("Enter contact name") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = phone,
                    onValueChange = { 
                        // Only allow digits, plus sign, and spaces
                        val filtered = it.filter { char -> 
                            char.isDigit() || char == '+' || char == ' '
                        }
                        phone = filtered
                    },
                    label = { Text("Phone Number") },
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = { Text("Format: +91XXXXXXXXXX (with country code)") },
                    isError = phone.isNotBlank() && !isValidPhoneNumber(phone),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank() && isValidPhoneNumber(phone)) {
                        onSave(name, formatPhoneNumber(phone))
                    }
                },
                enabled = name.isNotBlank() && isValidPhoneNumber(phone)
            ) {
                Text(if (contact == null) "Add" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun isValidPhoneNumber(phone: String): Boolean {
    // Remove spaces and check format
    val cleaned = phone.replace(" ", "")
    // Should start with + followed by country code and 10-12 digits
    return cleaned.matches(Regex("""^\+\d{11,13}$"""))
}

private fun formatPhoneNumber(phone: String): String {
    // Remove any existing spaces and format
    return phone.replace(" ", "")
}
