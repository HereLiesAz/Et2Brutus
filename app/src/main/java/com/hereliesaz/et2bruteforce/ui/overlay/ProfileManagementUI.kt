package com.hereliesaz.et2bruteforce.ui.overlay

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hereliesaz.et2bruteforce.model.Profile

@Composable
fun ProfileManagementDialog(
    profiles: List<Profile>,
    saveError: String?,
    onLoadProfile: (Profile) -> Unit,
    onSaveProfile: (String) -> Unit,
    onDeleteProfile: (Profile) -> Unit,
    onRenameProfile: (Profile, String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Profile Management") },
        text = {
            Column {
                var newProfileName by remember { mutableStateOf("") }
                var showError by remember { mutableStateOf(false) }
                OutlinedTextField(
                    value = newProfileName,
                    onValueChange = {
                        newProfileName = it
                        if (showError && it.isNotBlank()) showError = false
                    },
                    label = { Text("New Profile Name") },
                    isError = showError && newProfileName.isBlank()
                )
                if (showError && newProfileName.isBlank()) {
                    Text(
                        text = "Profile name cannot be empty.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                if (saveError != null) {
                    Text(
                        text = saveError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                Button(
                    onClick = {
                        if (newProfileName.isBlank()) {
                            showError = true
                        } else {
                            onSaveProfile(newProfileName)
                            showError = false
                        }
                    },
                    enabled = newProfileName.isNotBlank()
                ) {
                    Text("Save Current as New Profile")
                }
                LazyColumn {
                    items(profiles) { profile ->
                        ProfileListItem(
                            profile = profile,
                            onLoadProfile = onLoadProfile,
                            onDeleteProfile = onDeleteProfile,
                            onRenameProfile = onRenameProfile
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun ProfileListItem(
    profile: Profile,
    onLoadProfile: (Profile) -> Unit,
    onDeleteProfile: (Profile) -> Unit,
    onRenameProfile: (Profile, String) -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf(profile.name) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        if (isEditing) {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text("New Name") }
            )
            Button(onClick = {
                onRenameProfile(profile, newName)
                isEditing = false
            }) {
                Text("Save")
            }
        } else {
            Text(profile.name)
            Row {
                IconButton(onClick = { onLoadProfile(profile) }) {
                    Icon(Icons.Default.Restore, contentDescription = "Load")
                }
                IconButton(onClick = { isEditing = true }) {
                    Icon(Icons.Default.Edit, contentDescription = "Rename")
                }
                IconButton(onClick = { onDeleteProfile(profile) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
            }
        }
    }
}
