package com.nexus.app.ui.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.nexus.app.data.Profile

// Outer function: waits until the profile has loaded, then shows the form.
@Composable
fun EditProfileScreen(vm: ProfileViewModel, onDone: () -> Unit) {
    val profile by vm.profile.collectAsState()
    val p = profile
    if (p == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
    } else {
        EditProfileForm(p, vm, onDone)
    }
}

@Composable
private fun EditProfileForm(p: Profile, vm: ProfileViewModel, onDone: () -> Unit) {
    val busy by vm.busy.collectAsState()
    val resolver = LocalContext.current.contentResolver

    var name by rememberSaveable(p.uid) { mutableStateOf(p.name) }
    var username by rememberSaveable(p.uid) { mutableStateOf(p.username) }
    var bio by rememberSaveable(p.uid) { mutableStateOf(p.bio) }
    var avatarUrl by rememberSaveable(p.uid) { mutableStateOf(p.avatarUrl) }
    var coverUrl by rememberSaveable(p.uid) { mutableStateOf(p.coverUrl) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    // The system photo picker. No storage permission needed.
    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            error = null
            vm.uploadImage(resolver, uri) { result ->
                result.onSuccess { avatarUrl = it }.onFailure { error = it.message ?: "Upload failed." }
            }
        }
    }
    val coverPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            error = null
            vm.uploadImage(resolver, uri) { result ->
                result.onSuccess { coverUrl = it }.onFailure { error = it.message ?: "Upload failed." }
            }
        }
    }
    val imageOnly = PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)

    Column(
        Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
    ) {
        ProfileHeader(coverUrl, avatarUrl, name.take(1).uppercase())

        Column(Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { coverPicker.launch(imageOnly) }, enabled = !busy) {
                    Text("Change cover")
                }
                OutlinedButton(onClick = { avatarPicker.launch(imageOnly) }, enabled = !busy) {
                    Text("Change avatar")
                }
            }
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { if (it.length <= 50) name = it },
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
                supportingText = { Text("3-20 characters: a-z, 0-9, underscore") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = bio,
                onValueChange = { if (it.length <= 160) bio = it },
                label = { Text("Bio") },
                supportingText = { Text("${bio.length}/160") },
                maxLines = 4,
                modifier = Modifier.fillMaxWidth(),
            )

            if (busy) {
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            error?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(16.dp))
            Button(
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val cleanUsername = username.trim().lowercase()
                    if (name.isBlank()) {
                        error = "Name can't be empty."
                    } else if (!Regex("^[a-z0-9_]{3,20}$").matches(cleanUsername)) {
                        error = "Username must be 3-20 characters: a-z, 0-9 or underscore."
                    } else {
                        error = null
                        vm.save(name.trim(), cleanUsername, bio.trim(), avatarUrl, coverUrl) { message ->
                            if (message == null) {
                                onDone()
                            } else {
                                error = message
                            }
                        }
                    }
                },
            ) { Text("Save") }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onDone, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                Text("Cancel")
            }
        }
    }
}
