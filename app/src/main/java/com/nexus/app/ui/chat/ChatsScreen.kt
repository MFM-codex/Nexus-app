package com.nexus.app.ui.chat

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Badge
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nexus.app.ui.components.Avatar
import com.nexus.app.ui.components.timeAgo

// The Chats tab: your conversations, newest first.
@Composable
fun ChatsScreen(vm: ChatsViewModel, myUid: String, onOpenChat: (String) -> Unit) {
    val loadedRows by vm.rows.collectAsState()
    val rows = loadedRows.orEmpty()

    Column(Modifier.fillMaxSize()) {
        Text(
            "Chats",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp),
        )
        HorizontalDivider()
        LazyColumn(Modifier.fillMaxSize()) {
            if (loadedRows == null) {
                item {
                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            } else if (rows.isEmpty()) {
                item {
                    Text(
                        "No conversations yet. Open a friend's profile and tap Message.",
                        modifier = Modifier.padding(24.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(rows, key = { it.chat.id }) { row ->
                val name = row.other?.name ?: "Unknown user"
                val preview = (if (row.chat.lastSenderId == myUid) "You: " else "") + row.chat.lastMessage
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onOpenChat(row.otherUid) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Avatar(row.other?.avatarUrl, name, size = 48.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            name,
                            fontWeight = if (row.unread > 0) FontWeight.Bold else FontWeight.Normal,
                        )
                        Text(
                            preview,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (row.unread > 0) FontWeight.Bold else FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            timeAgo(row.chat.lastMessageAt),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (row.unread > 0) {
                            Badge { Text(if (row.unread > 9) "9+" else "${row.unread}") }
                        }
                    }
                }
                HorizontalDivider()
            }
        }
    }
}
