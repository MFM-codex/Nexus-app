package com.nexus.app.ui.alerts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nexus.app.ui.components.Avatar
import com.nexus.app.ui.components.timeAgo

// The Alerts tab. Unread ones are highlighted; tap one to open it.
@Composable
fun AlertsScreen(
    vm: NotificationsViewModel,
    onOpenUser: (String) -> Unit,
    onOpenPost: (String) -> Unit,
    isIncomingRequest: (String) -> Boolean = { false },
    onConfirm: (String) -> Unit = {},
    onDecline: (String) -> Unit = {},
) {
    val loadedItems by vm.items.collectAsState()
    val items = loadedItems.orEmpty()
    val unread by vm.unreadCount.collectAsState()

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Alerts",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { vm.markAllRead() }, enabled = unread > 0) { Text("Mark all read") }
        }
        HorizontalDivider()

        LazyColumn(Modifier.fillMaxSize()) {
            if (loadedItems == null) {
                item {
                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            } else if (items.isEmpty()) {
                item {
                    Text(
                        "Nothing yet. Likes, comments and friend requests show up here.",
                        modifier = Modifier.padding(24.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(items, key = { it.n.id }) { item ->
                val n = item.n
                val name = item.actor?.name ?: "Someone"
                val message = when (n.type) {
                    "like" -> "$name liked your post"
                    "comment" -> "$name commented on your post"
                    "friend_request" -> "$name sent you a friend request"
                    "friend_accept" -> "$name accepted your friend request"
                    else -> "$name did something"
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(
                            if (n.read) MaterialTheme.colorScheme.background
                            else MaterialTheme.colorScheme.secondaryContainer
                        )
                        .clickable {
                            vm.markRead(n.id)
                            if (n.type == "like" || n.type == "comment") onOpenPost(n.postId)
                            else onOpenUser(n.actorId)
                        }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Avatar(item.actor?.avatarUrl, name)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(message, fontWeight = if (n.read) FontWeight.Normal else FontWeight.Bold)
                        Text(
                            timeAgo(n.createdAt),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        // like Facebook: answer a friend request right here
                        if (n.type == "friend_request" && isIncomingRequest(n.actorId)) {
                            Row(
                                Modifier.padding(top = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Button(onClick = { vm.markRead(n.id); onConfirm(n.actorId) }) { Text("Confirm") }
                                OutlinedButton(onClick = { vm.markRead(n.id); onDecline(n.actorId) }) { Text("Delete") }
                            }
                        }
                    }
                }
                HorizontalDivider()
            }
        }
    }
}
