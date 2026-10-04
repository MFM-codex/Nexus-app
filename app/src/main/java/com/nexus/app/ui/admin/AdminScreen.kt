package com.nexus.app.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.nexus.app.ui.components.reportReasons
import com.nexus.app.ui.components.timeAgo

// The admin panel: open reports with buttons to dismiss, delete the post, or ban the user.
@Composable
fun AdminScreen(vm: AdminViewModel, onBack: () -> Unit, onOpenUser: (String) -> Unit) {
    val reports by vm.reports.collectAsState()
    val message by vm.message.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    // "delete" or "ban", plus the report it applies to
    var confirm by remember { mutableStateOf<Pair<String, ReportUi>?>(null) }

    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            vm.clearMessage()
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(4.dp)) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(
                    "Moderation (${reports.size} open)",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
            HorizontalDivider()

            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (reports.isEmpty()) {
                    item {
                        Text(
                            "No open reports.",
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                items(reports, key = { it.report.id }) { r ->
                    ReportCard(
                        item = r,
                        onOpenUser = { onOpenUser(r.report.targetUserId) },
                        onDismiss = { vm.dismiss(r.report) },
                        onDeletePost = { confirm = "delete" to r },
                        onBan = { confirm = "ban" to r },
                    )
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }

    confirm?.let { (action, r) ->
        val who = r.target?.name ?: "this user"
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text(if (action == "delete") "Delete this?" else "Ban $who?") },
            text = {
                Text(
                    if (action == "delete") "It is removed for everyone."
                    else "They won't be able to post, comment, message or send friend requests."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirm = null
                    if (action == "delete") {
                        if (r.report.targetType == "reel") vm.deleteReel(r.report) else vm.deletePost(r.report)
                    } else {
                        vm.ban(r.report)
                    }
                }) { Text(if (action == "delete") "Delete" else "Ban") }
            },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun ReportCard(
    item: ReportUi,
    onOpenUser: () -> Unit,
    onDismiss: () -> Unit,
    onDeletePost: () -> Unit,
    onBan: () -> Unit,
) {
    val r = item.report
    val reasonLabel = reportReasons.firstOrNull { it.first == r.reason }?.second ?: r.reason
    val targetName = item.target?.let { "${it.name} (@${it.username})" } ?: "Unknown user"

    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(reasonLabel, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(
                    timeAgo(r.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                "Reported by ${item.reporter?.let { "@" + it.username } ?: "unknown"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))

            if (r.targetType == "post") {
                Text("Post by $targetName", fontWeight = FontWeight.Medium)
                val post = item.post
                if (post == null) {
                    Text("(this post no longer exists)", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    if (post.text.isNotBlank()) Text(post.text.take(300))
                    if (post.imageUrl.isNotEmpty()) {
                        AsyncImage(
                            model = post.imageUrl,
                            contentDescription = "Reported photo",
                            contentScale = ContentScale.FillWidth,
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .fillMaxWidth()
                                .heightIn(max = 200.dp)
                                .clip(RoundedCornerShape(8.dp)),
                        )
                    }
                }
            } else if (r.targetType == "reel") {
                Text("Reel by $targetName", fontWeight = FontWeight.Medium)
                val reel = item.reel
                if (reel == null) {
                    Text("(this reel no longer exists)", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    if (reel.caption.isNotBlank()) Text(reel.caption.take(300))
                    AsyncImage(
                        model = reel.thumbnailUrl,
                        contentDescription = "Reel preview",
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                            .clip(RoundedCornerShape(8.dp)),
                    )
                }
            } else {
                Text("User: $targetName", fontWeight = FontWeight.Medium)
            }

            if (r.details.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text("\u201C${r.details}\u201D", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(Modifier.height(8.dp))
            Row {
                TextButton(onClick = onOpenUser) { Text("Open profile") }
                TextButton(onClick = onDismiss) { Text("Dismiss") }
            }
            Row {
                if (r.targetType == "post" && item.post != null) {
                    TextButton(onClick = onDeletePost) { Text("Delete post") }
                }
                if (r.targetType == "reel" && item.reel != null) {
                    TextButton(onClick = onDeletePost) { Text("Delete reel") }
                }
                TextButton(onClick = onBan) { Text("Ban user") }
            }
        }
    }
}
