package com.nexus.app.ui.friends

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nexus.app.data.Profile
import com.nexus.app.data.Relation
import com.nexus.app.ui.components.ReportDialog
import com.nexus.app.ui.profile.ProfileHeader

// Another person's profile, with the right friend button and a menu to report or block.
@Composable
fun UserProfileScreen(vm: FriendsViewModel, otherUid: String, onBack: () -> Unit, onMessage: () -> Unit) {
    val state by vm.state.collectAsState() // re-draw when the friendship or block list changes
    val loaded by produceState(initialValue = false to (null as Profile?), otherUid) {
        value = true to vm.profileOf(otherUid)
    }
    val profile = loaded.second
    val isBlocked = state.blocked.contains(otherUid)

    var menuOpen by remember { mutableStateOf(false) }
    var reporting by remember { mutableStateOf(false) }
    var confirmBlock by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf<String?>(null) }

    // Show errors from friend actions here too
    LaunchedEffect(state.error) {
        state.error?.let {
            note = it
            vm.clearError()
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(4.dp),
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Spacer(Modifier.weight(1f))
            if (profile != null) {
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "More")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Report user") },
                            onClick = { menuOpen = false; reporting = true },
                        )
                        DropdownMenuItem(
                            text = { Text(if (isBlocked) "Unblock" else "Block") },
                            onClick = {
                                menuOpen = false
                                if (isBlocked) vm.unblock(otherUid) else confirmBlock = true
                            },
                        )
                    }
                }
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

                if (isBlocked) {
                    Text("You blocked this person.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { vm.unblock(otherUid) }) { Text("Unblock") }
                } else {
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

                note?.let {
                    Spacer(Modifier.height(12.dp))
                    Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }

    if (reporting) {
        ReportDialog(
            title = "Report ${profile?.name ?: "this user"}",
            onDismiss = { reporting = false },
        ) { reason, details ->
            reporting = false
            vm.reportUser(otherUid, reason, details) { message -> note = message }
        }
    }

    if (confirmBlock) {
        AlertDialog(
            onDismissRequest = { confirmBlock = false },
            title = { Text("Block ${profile?.name ?: "this person"}?") },
            text = {
                Text("You'll be unfriended, and they won't be able to send you friend requests or messages.")
            },
            confirmButton = {
                TextButton(onClick = { confirmBlock = false; vm.block(otherUid) }) { Text("Block") }
            },
            dismissButton = { TextButton(onClick = { confirmBlock = false }) { Text("Cancel") } },
        )
    }
}
