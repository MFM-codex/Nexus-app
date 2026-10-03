package com.nexus.app.ui.home

import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.nexus.app.data.PostUi
import com.nexus.app.ui.components.Avatar
import com.nexus.app.ui.components.timeAgo

// One post in the feed.
@Composable
fun PostCard(
    item: PostUi,
    isMine: Boolean,
    onLike: () -> Unit,
    onComment: () -> Unit,
    onEdit: (String) -> Unit,
    onDelete: () -> Unit,
) {
    val post = item.post
    val name = item.author?.name ?: "Unknown user"
    var menuOpen by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxWidth().padding(12.dp)) {
        // Header: avatar, name, time, and a menu for your own posts
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(item.author?.avatarUrl, name)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(name, fontWeight = FontWeight.Bold)
                val handle = item.author?.username?.let { "@$it · " } ?: ""
                val edited = if (post.editedAt != null) " · edited" else ""
                Text(
                    handle + timeAgo(post.createdAt) + edited,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (isMine) {
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "More")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            onClick = { menuOpen = false; editing = true },
                        )
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            onClick = { menuOpen = false; confirmDelete = true },
                        )
                    }
                }
            }
        }

        if (post.text.isNotBlank()) {
            Text(post.text, modifier = Modifier.padding(top = 8.dp))
        }
        if (post.imageUrl.isNotEmpty()) {
            AsyncImage(
                model = post.imageUrl,
                contentDescription = "Post photo",
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .clip(RoundedCornerShape(8.dp)),
            )
        }

        // Like and comment buttons with counts
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(
                onClick = onLike,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
            ) {
                Icon(
                    imageVector = if (item.liked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "Like",
                    tint = if (item.liked) Color(0xFFE53935) else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(6.dp))
                Text("${post.likeCount}")
            }
            TextButton(
                onClick = onComment,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
            ) {
                Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = "Comments")
                Spacer(Modifier.width(6.dp))
                Text("${post.commentCount}")
            }
        }
    }

    if (editing) {
        var draft by remember { mutableStateOf(post.text) }
        AlertDialog(
            onDismissRequest = { editing = false },
            title = { Text("Edit post") },
            text = {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { if (it.length <= 2000) draft = it },
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    enabled = draft.isNotBlank() || post.imageUrl.isNotEmpty(),
                    onClick = { editing = false; onEdit(draft.trim()) },
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { editing = false }) { Text("Cancel") } },
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this post?") },
            text = { Text("This can't be undone.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete() }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}
