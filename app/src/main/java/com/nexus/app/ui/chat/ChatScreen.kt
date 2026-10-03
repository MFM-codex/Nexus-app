package com.nexus.app.ui.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nexus.app.data.Message
import com.nexus.app.ui.components.Avatar
import com.nexus.app.ui.components.timeAgo

// A live conversation with one friend.
@Composable
fun ChatScreen(vm: ChatViewModel, myUid: String, onBack: () -> Unit, onOpenProfile: () -> Unit) {
    val messages by vm.messages.collectAsState()
    val chat by vm.chat.collectAsState()
    val other by vm.otherProfile.collectAsState()
    val sending by vm.sending.collectAsState()
    val errorText by vm.error.collectAsState()
    var draft by rememberSaveable { mutableStateOf("") }

    // When I have unread messages and this screen is open, mark them as read.
    val unreadMine = chat?.unread?.get(myUid) ?: 0
    LaunchedEffect(unreadMine) {
        if (unreadMine > 0) vm.markRead()
    }

    // Only the latest message I sent shows "Sent" or "Seen".
    val latestMineId = messages.firstOrNull { it.senderId == myUid }?.id
    val otherLastRead = chat?.lastRead?.get(vm.other)

    Column(Modifier.fillMaxSize().imePadding()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(4.dp)) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Avatar(other?.avatarUrl, other?.name ?: "?", size = 36.dp)
            Spacer(Modifier.width(10.dp))
            Text(
                other?.name ?: "Chat",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
        HorizontalDivider()

        // reverseLayout: the first item (the newest message) sits at the bottom
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            reverseLayout = true,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(messages, key = { it.id }) { m ->
                val mine = m.senderId == myUid
                val status = if (m.id == latestMineId) {
                    val created = m.createdAt
                    if (otherLastRead != null && created != null && otherLastRead >= created) "Seen" else "Sent"
                } else null
                MessageBubble(m, mine, status)
            }
            if (messages.isEmpty()) {
                item {
                    Text(
                        "Say hi \uD83D\uDC4B",
                        modifier = Modifier.padding(24.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        errorText?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 12.dp))
        }
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = draft,
                onValueChange = { if (it.length <= 1000) draft = it },
                placeholder = { Text("Message") },
                maxLines = 4,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                enabled = draft.isNotBlank() && !sending,
                onClick = {
                    vm.send(draft.trim()) { ok -> if (ok) draft = "" }
                },
            ) { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send") }
        }
    }
}

@Composable
private fun MessageBubble(m: Message, mine: Boolean, status: String?) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start,
    ) {
        Column(horizontalAlignment = if (mine) Alignment.End else Alignment.Start) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (mine) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.widthIn(max = 280.dp),
            ) {
                Text(m.text, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
            }
            Text(
                timeAgo(m.createdAt) + if (status != null) " · $status" else "",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }
}
