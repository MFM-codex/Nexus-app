package com.nexus.app.ui.comments

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nexus.app.data.CommentUi
import com.nexus.app.ui.components.Avatar
import com.nexus.app.ui.components.timeAgo

// All comments on one post, with a box to write a new one.
// onCountChange(+1 or -1) keeps the comment number on the feed correct.
@Composable
fun CommentsScreen(
    vm: CommentsViewModel,
    myUid: String,
    onBack: () -> Unit,
    onCountChange: (Int) -> Unit,
) {
    val comments by vm.comments.collectAsState()
    val sending by vm.sending.collectAsState()
    val errorText by vm.error.collectAsState()
    var draft by rememberSaveable { mutableStateOf("") }

    Column(Modifier.fillMaxSize().imePadding()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(4.dp)) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Comments", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        HorizontalDivider()

        LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
            if (comments.isEmpty()) {
                item {
                    Text(
                        "No comments yet. Be the first.",
                        modifier = Modifier.padding(24.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(comments, key = { it.comment.id }) { c ->
                CommentRow(
                    item = c,
                    mine = c.comment.authorId == myUid,
                    onDelete = { vm.delete(c.comment.id) { onCountChange(-1) } },
                )
            }
        }

        errorText?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 12.dp))
        }
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = draft,
                onValueChange = { if (it.length <= 500) draft = it },
                placeholder = { Text("Write a comment...") },
                maxLines = 3,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                enabled = draft.isNotBlank() && !sending,
                onClick = {
                    vm.send(draft.trim()) { ok ->
                        if (ok) {
                            draft = ""
                            onCountChange(1)
                        }
                    }
                },
            ) { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send") }
        }
    }
}

@Composable
private fun CommentRow(item: CommentUi, mine: Boolean, onDelete: () -> Unit) {
    val name = item.author?.name ?: "Unknown user"
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
        Avatar(item.author?.avatarUrl, name, size = 32.dp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(name, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Text(
                    timeAgo(item.comment.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(item.comment.text)
        }
        if (mine) {
            IconButton(onClick = onDelete) {
                Icon(Icons.Outlined.Delete, contentDescription = "Delete comment")
            }
        }
    }
}
