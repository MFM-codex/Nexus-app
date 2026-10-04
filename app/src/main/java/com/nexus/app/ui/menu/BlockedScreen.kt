package com.nexus.app.ui.menu

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.nexus.app.ui.components.Avatar
import com.nexus.app.ui.friends.FriendsViewModel

// Menu > Settings > Blocked people
@Composable
fun BlockedScreen(vm: FriendsViewModel, onBack: () -> Unit) {
    val state by vm.state.collectAsState()
    val blocked = state.blocked.toList()

    Column(Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(4.dp)) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Blocked people", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        HorizontalDivider()

        LazyColumn(Modifier.fillMaxSize()) {
            if (blocked.isEmpty()) {
                item {
                    Text(
                        "You haven't blocked anyone.",
                        modifier = Modifier.padding(24.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(blocked, key = { it }) { uid ->
                BlockedRow(uid, vm)
            }
        }
    }
}

@Composable
private fun BlockedRow(uid: String, vm: FriendsViewModel) {
    val profile by produceState<Profile?>(initialValue = null, uid) {
        value = vm.profileOf(uid)
    }
    val name = profile?.name ?: "Loading..."
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(profile?.avatarUrl, name)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(name, fontWeight = FontWeight.Bold)
            Text(
                "@${profile?.username ?: ""}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        OutlinedButton(onClick = { vm.unblock(uid) }) { Text("Unblock") }
    }
}
