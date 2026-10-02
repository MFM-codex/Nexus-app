package com.nexus.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@Composable
fun ProfileScreen(vm: ProfileViewModel, onEdit: () -> Unit, onSignOut: () -> Unit) {
    val profile by vm.profile.collectAsState()
    val setupError by vm.setupError.collectAsState()
    val p = profile

    if (p == null) {
        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            if (setupError != null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Could not load your profile.", fontWeight = FontWeight.Bold)
                    Text(setupError ?: "", color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(onClick = onSignOut) { Text("Sign out") }
                }
            } else {
                CircularProgressIndicator()
            }
        }
        return
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        ProfileHeader(p.coverUrl, p.avatarUrl, p.name.take(1).uppercase())
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(p.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("@${p.username}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (p.bio.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(p.bio)
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onEdit) { Text("Edit profile") }
                OutlinedButton(onClick = onSignOut) { Text("Sign out") }
            }
        }
    }
}

// Cover photo with the round avatar overlapping its bottom-left corner.
@Composable
fun ProfileHeader(coverUrl: String, avatarUrl: String, initial: String) {
    Box(Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(160.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            if (coverUrl.isNotEmpty()) {
                AsyncImage(
                    model = coverUrl,
                    contentDescription = "Cover photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Box(
            modifier = Modifier
                .padding(start = 16.dp, top = 120.dp)
                .size(84.dp)
                .clip(CircleShape)
                .border(3.dp, MaterialTheme.colorScheme.background, CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            if (avatarUrl.isNotEmpty()) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = "Avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Text(initial, style = MaterialTheme.typography.headlineMedium)
            }
        }
    }
}
