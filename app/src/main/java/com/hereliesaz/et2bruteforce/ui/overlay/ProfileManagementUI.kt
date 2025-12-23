package com.hereliesaz.et2bruteforce.ui.overlay

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Restore
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.hereliesaz.et2bruteforce.R
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
    var profileToDelete by remember { mutableStateOf<Profile?>(null) }

    if (profileToDelete != null) {
        AlertDialog(
            onDismissRequest = { profileToDelete = null },
            title = { Text(stringResource(R.string.profile_delete_confirm_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.profile_delete_confirm_message,
                        profileToDelete?.name ?: ""
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        profileToDelete?.let { onDeleteProfile(it) }
                        profileToDelete = null
                    }
                ) {
                    Text(stringResource(R.string.profile_delete_confirm_yes))
                }
            },
            dismissButton = {
                TextButton(onClick = { profileToDelete = null }) {
                    Text(stringResource(R.string.profile_delete_confirm_no))
                }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Profile Management") },
        text = {
            Column {
                var newProfileName by remember { mutableStateOf("") }
                var showError by remember { mutableStateOf(false) }

                val onSaveAttempt = {
                    if (newProfileName.isBlank()) {
                        showError = true
                    } else {
                        onSaveProfile(newProfileName)
                        showError = false
                    }
                }

                OutlinedTextField(
                    value = newProfileName,
                    onValueChange = {
                        newProfileName = it
                        if (showError && it.isNotBlank()) showError = false
                    },
                    label = { Text("New Profile Name") },
                    isError = showError && newProfileName.isBlank(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onSaveAttempt() })
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
                    onClick = onSaveAttempt
                    // Enabled allows user to click and see error if empty
                ) {
                    Text("Save Current as New Profile")
                }
                if (profiles.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            stringResource(R.string.profile_empty_state),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    LazyColumn {
                        items(profiles) { profile ->
                            ProfileListItem(
                                profile = profile,
                                onLoadProfile = onLoadProfile,
                                onDeleteProfile = { profileToDelete = it },
                                onRenameProfile = onRenameProfile
                            )
                        }
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
            val onSaveRename = {
                onRenameProfile(profile, newName)
                isEditing = false
            }
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text("New Name") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onSaveRename() })
            )
            Button(onClick = onSaveRename) {
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
