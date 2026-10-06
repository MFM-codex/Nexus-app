package com.nexus.app.ui.community

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.nexus.app.ui.components.Avatar
import com.nexus.app.ui.home.PostCard

// One group or page: info, join/follow button, and its posts.
@Composable
fun CommunityDetailScreen(
    type: String,
    vm: CommunityDetailViewModel,
    myUid: String,
    onBack: () -> Unit,
    onNewPost: () -> Unit,
    onOpenComments: (String) -> Unit,
) {
    val isGroups = type == "groups"
    val state by vm.state.collectAsState()
    val community = state.community
    val snackbar = remember { SnackbarHostState() }
    var menuOpen by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            vm.clearMessage()
        }
    }
    LaunchedEffect(state.deleted) {
        if (state.deleted) onBack()
    }

    // Refresh the posts when coming back from the comments screen
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) vm.refreshPosts()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val canPost = community != null &&
        ((isGroups && state.member) || (!isGroups && community.ownerId == myUid))
    val canReadPosts = state.member || !isGroups

    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize()) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(4.dp),
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Spacer(Modifier.weight(1f))
                    if (community != null && community.ownerId == myUid) {
                        Box {
                            IconButton(onClick = { menuOpen = true }) {
                                Icon(Icons.Filled.MoreVert, contentDescription = "More")
                            }
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                DropdownMenuItem(
                                    text = { Text(if (isGroups) "Delete group" else "Delete page") },
                                    onClick = { menuOpen = false; confirmDelete = true },
                                )
                            }
                        }
                    }
                }
            }

            if (!state.loaded) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            } else if (community == null) {
                item { Text("This ${if (isGroups) "group" else "page"} no longer exists.", modifier = Modifier.padding(24.dp)) }
            } else {
                item {
                    Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Avatar(null, community.name, size = 64.dp)
                                Spacer(Modifier.width(14.dp))
                                Column {
                                    Text(
                                        community.name,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(
                                        (if (isGroups) "${state.memberCount} members" else "${state.memberCount} followers") +
                                            (if (!isGroups && community.category.isNotBlank()) " \u00B7 ${community.category}" else ""),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            if (community.description.isNotBlank()) {
                                Spacer(Modifier.height(12.dp))
                                Text(community.description)
                            }
                            Spacer(Modifier.height(12.dp))
                            if (state.member) {
                                OutlinedButton(onClick = { vm.toggleMembership() }, modifier = Modifier.fillMaxWidth()) {
                                    Text(if (isGroups) "Leave group" else "Unfollow")
                                }
                            } else {
                                Button(onClick = { vm.toggleMembership() }, modifier = Modifier.fillMaxWidth()) {
                                    Text(if (isGroups) "Join group" else "Follow")
                                }
                            }
                        }
                    }
                }

                if (canPost) {
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        ) {
                            TextButton(onClick = onNewPost, modifier = Modifier.fillMaxWidth().padding(4.dp)) {
                                Text("Write something...")
                            }
                        }
                    }
                }

                if (!canReadPosts) {
                    item {
                        Text(
                            "Join this group to see and write posts.",
                            modifier = Modifier.padding(24.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else if (!state.postsLoaded) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                } else if (state.items.isEmpty()) {
                    item {
                        Text(
                            "No posts yet.",
                            modifier = Modifier.padding(24.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                items(state.items, key = { it.post.id }) { item ->
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    ) {
                        PostCard(
                            item = item,
                            isMine = item.post.authorId == myUid,
                            onLike = { vm.toggleLike(item.post.id) },
                            onComment = { onOpenComments(item.post.id) },
                            onEdit = { text -> vm.editPost(item.post.id, text) },
                            onDelete = { vm.deletePost(item.post.id) },
                            onReport = { reason, details -> vm.reportPost(item.post, reason, details) },
                            onSave = {},
                            showSave = false,
                        )
                    }
                }
                if (canReadPosts && !state.endReached) {
                    item {
                        Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.Center) {
                            TextButton(onClick = { vm.loadMore() }) { Text("Load more") }
                        }
                    }
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(if (isGroups) "Delete this group?" else "Delete this page?") },
            text = { Text("This can't be undone.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; vm.deleteCommunity() }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}
