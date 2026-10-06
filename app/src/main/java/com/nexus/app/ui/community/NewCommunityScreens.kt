package com.nexus.app.ui.community

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

// Create a group or a page.
@Composable
fun NewCommunityScreen(type: String, vm: CommunitiesViewModel, onBack: () -> Unit, onCreated: (String) -> Unit) {
    val isGroups = type == "groups"
    val busy by vm.busy.collectAsState()
    var name by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                if (isGroups) "Create group" else "Create page",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = name,
            onValueChange = { if (it.length <= 50) name = it },
            label = { Text(if (isGroups) "Group name" else "Page name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        if (!isGroups) {
            OutlinedTextField(
                value = category,
                onValueChange = { if (it.length <= 30) category = it },
                label = { Text("Category (for example Business)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
        }
        OutlinedTextField(
            value = description,
            onValueChange = { if (it.length <= 300) description = it },
            label = { Text("About") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
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
            enabled = !busy && name.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                error = null
                vm.create(name.trim(), description.trim(), category.trim()) { id, message ->
                    if (id != null) onCreated(id) else error = message
                }
            },
        ) { Text("Create") }
    }
}

// Write a post inside a group (or as the owner of a page).
@Composable
fun NewCommunityPostScreen(vm: CommunityDetailViewModel, onDone: () -> Unit) {
    val state by vm.state.collectAsState()
    val resolver = LocalContext.current.contentResolver
    var text by rememberSaveable { mutableStateOf("") }
    var imageUrl by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            error = null
            vm.uploadImage(resolver, uri) { result ->
                result.onSuccess { imageUrl = it }.onFailure { error = it.message ?: "Upload failed." }
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onDone) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("New post", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = text,
            onValueChange = { if (it.length <= 2000) text = it },
            placeholder = { Text("Write something...") },
            minLines = 4,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        if (imageUrl.isNotEmpty()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = "Selected photo",
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp).clip(RoundedCornerShape(8.dp)),
            )
            TextButton(onClick = { imageUrl = "" }) { Text("Remove photo") }
        }
        OutlinedButton(
            onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            enabled = !state.busy,
        ) { Text(if (imageUrl.isEmpty()) "Add photo" else "Change photo") }

        if (state.busy) {
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        error?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(16.dp))
        Button(
            enabled = !state.busy && (text.isNotBlank() || imageUrl.isNotEmpty()),
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                vm.createPost(text.trim(), imageUrl) { message ->
                    if (message == null) onDone() else error = message
                }
            },
        ) { Text("Post") }
    }
}
