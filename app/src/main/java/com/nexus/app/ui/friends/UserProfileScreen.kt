package com.nexus.app.ui.friends

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nexus.app.data.Profile
import com.nexus.app.data.Relation
import com.nexus.app.ui.profile.ProfileHeader

// Another person's profile, with the right friend button.
@Composable
fun UserProfileScreen(vm: FriendsViewModel, otherUid: String, onBack: () -> Unit, onMessage: () -> Unit) {
    val state by vm.state.collectAsState() // re-draw when the friendship changes
    val loaded by produceState(initialValue = false to (null as Profile?), otherUid) {
        value = true to vm.profileOf(otherUid)
    }
    val profile = loaded.second

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(4.dp)) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        }

        if (!loaded.first) {
            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (profile == null) {
            Text("This user could not be found.", modifier = Modifier.padding(24.dp))
        } else {
            ProfileHeader(profile.coverUrl, profile.avatarUrl, profile.name.take(1).uppercase())
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(profile.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("@${profile.username}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (profile.bio.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(profile.bio)
                }
                Spacer(Modifier.height(16.dp))
                if (vm.relationWith(otherUid) == Relation.FRIEND) {
                    Button(onClick = onMessage) { Text("Message") }
                    Spacer(Modifier.height(8.dp))
                }
                // state.friendships is read above, so this updates the moment a request is sent or accepted
                RelationActions(
                    relation = vm.relationWith(otherUid),
                    name = profile.name,
                    compact = false,
                    onAdd = { vm.sendRequest(otherUid) },
                    onAccept = { vm.accept(otherUid) },
                    onRemove = { vm.remove(otherUid) },
                )
            }
        }
    }
}
