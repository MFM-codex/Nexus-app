package com.nexus.app.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nexus.app.data.Profile
import com.nexus.app.ui.components.Avatar

// The news feed: "What's on your mind?" box, then posts, newest first.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    vm: FeedViewModel,
    myUid: String,
    myProfile: Profile?,
    onNewPost: () -> Unit,
    onOpenComments: (String) -> Unit,
) {
    val state by vm.state.collectAsState()
    val listState = rememberLazyListState()
    val snackbar = remember { SnackbarHostState() }

    // Infinite scroll: when the last visible item is near the end of the list, load more.
    val nearEnd by remember {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= listState.layoutInfo.totalItemsCount - 3
        }
    }
    LaunchedEffect(nearEnd, state.items.size) {
        if (nearEnd && state.items.isNotEmpty()) vm.loadMore()
    }

    // Show errors as a small message at the bottom.
    LaunchedEffect(state.error) {
        state.error?.let {
            snackbar.showSnackbar(it)
            vm.clearError()
        }
    }

    Box(Modifier.fillMaxSize()) {
        PullToRefreshBox(
            isRefreshing = state.refreshing,
            onRefresh = { vm.refresh() },
            modifier = Modifier.fillMaxSize(),
        ) {
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                item {
                    // "What's on your mind?" box, like Facebook
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Avatar(myProfile?.avatarUrl, myProfile?.name ?: "?", size = 42.dp)
                                Spacer(Modifier.width(10.dp))
                                Surface(
                                    shape = RoundedCornerShape(24.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.weight(1f).clickable(onClick = onNewPost),
                                ) {
                                    Text(
                                        "What's on your mind, ${myProfile?.name?.substringBefore(' ') ?: ""}?".replace(", ?", "?"),
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            HorizontalDivider()
                            TextButton(onClick = onNewPost, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Filled.PhotoLibrary, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Photo")
                            }
                        }
                    }
                }
                if (state.loadedOnce && state.items.isEmpty()) {
                    item {
                        if (state.loadFailed) {
                            Column(Modifier.padding(24.dp)) {
                                Text(
                                    "Couldn't load posts. Check your internet connection.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Button(onClick = { vm.refresh() }, modifier = Modifier.padding(top = 12.dp)) {
                                    Text("Try again")
                                }
                            }
                        } else {
                            Text(
                                "No posts yet. Add friends from the Friends tab, or be the first to post!",
                                modifier = Modifier.padding(24.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                items(state.items, key = { it.post.id }) { item ->
                    // each post is a white card with a grey gap under it
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    ) {
                        PostCard(
                            item = item,
                            isMine = item.post.authorId == myUid,
                            onLike = { vm.toggleLike(item.post.id) },
                            onComment = { onOpenComments(item.post.id) },
                            onEdit = { text -> vm.editPost(item.post.id, text) },
                            onDelete = { vm.deletePost(item.post.id) },
                            onReport = { reason, details -> vm.reportPost(item.post, reason, details) },
                        )
                    }
                }
                if (state.loadingMore) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}
