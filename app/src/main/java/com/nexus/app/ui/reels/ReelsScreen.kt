package com.nexus.app.ui.reels

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.nexus.app.data.AppSettings
import com.nexus.app.ui.components.Avatar
import com.nexus.app.ui.components.LoadingDots
import com.nexus.app.ui.components.ReportDialog

// Reels: full-screen videos. Swipe up for the next one, tap to pause.
@Composable
fun ReelsScreen(
    vm: ReelsViewModel,
    myUid: String,
    onNewReel: () -> Unit,
    onOpenComments: (String) -> Unit,
) {
    val state by vm.state.collectAsState()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val dataSaver by AppSettings.dataSaver.collectAsState()

    // One video player is shared by all the reels (cheaper than one per reel)
    val player = remember {
        ExoPlayer.Builder(context).build().apply { repeatMode = Player.REPEAT_MODE_ONE }
    }
    DisposableEffect(Unit) { onDispose { player.release() } }

    var isPlaying by remember { mutableStateOf(false) }
    var buffering by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf(false) }
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                buffering = playbackState == Player.STATE_BUFFERING
            }

            override fun onPlayerError(error: PlaybackException) {
                failed = true
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    // Pause when the app goes to the background
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) player.pause()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbar.showSnackbar(it)
            vm.clearError()
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        val items = state.items

        if (items.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (!state.loadedOnce) {
                    LoadingDots(Color.White)
                } else if (state.loadFailed) {
                    Text("Couldn't load reels. Check your internet connection.", color = Color.White)
                    Button(onClick = { vm.refresh() }, modifier = Modifier.padding(top = 12.dp)) { Text("Try again") }
                } else {
                    Text("No reels yet. Be the first to post one!", color = Color.White)
                }
            }
        } else {
            val pagerState = rememberPagerState(pageCount = { state.items.size })
            val current = items.getOrNull(pagerState.currentPage)

            // Play the reel that is on screen
            LaunchedEffect(current?.reel?.id) {
                if (current != null) {
                    failed = false
                    if (dataSaver) player.stop() // Data saver: don't download until the person taps
                    player.setMediaItem(MediaItem.fromUri(current.reel.videoUrl))
                    if (dataSaver) {
                        player.playWhenReady = false
                    } else {
                        player.prepare()
                        player.playWhenReady = true
                    }
                }
            }
            // Load more when near the end
            LaunchedEffect(pagerState.currentPage, items.size) {
                if (pagerState.currentPage >= items.size - 3) vm.loadMore()
            }

            VerticalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                key = { index -> state.items.getOrNull(index)?.reel?.id ?: index },
            ) { page ->
                val item = state.items.getOrNull(page)
                if (item != null) {
                    ReelPage(
                        item = item,
                        isCurrent = page == pagerState.currentPage,
                        isMine = item.reel.authorId == myUid,
                        sharedPlayer = player,
                        isPlaying = isPlaying,
                        buffering = buffering,
                        failed = failed,
                        onTogglePlay = {
                            if (player.playbackState == Player.STATE_IDLE) player.prepare()
                            player.playWhenReady = !player.playWhenReady
                        },
                        onLike = { vm.toggleLike(item.reel.id) },
                        onComments = { onOpenComments(item.reel.id) },
                        onDelete = { vm.deleteReel(item.reel.id) },
                        onReport = { reason, details -> vm.reportReel(item.reel, reason, details) },
                    )
                }
            }
        }

        // "+" button: make a new reel
        IconButton(
            onClick = onNewReel,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
                .background(Color.Black.copy(alpha = 0.45f), CircleShape),
        ) {
            Icon(Icons.Filled.Add, contentDescription = "New reel", tint = Color.White)
        }

        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun ReelPage(
    item: ReelUi,
    isCurrent: Boolean,
    isMine: Boolean,
    sharedPlayer: Player,
    isPlaying: Boolean,
    buffering: Boolean,
    failed: Boolean,
    onTogglePlay: () -> Unit,
    onLike: () -> Unit,
    onComments: () -> Unit,
    onDelete: () -> Unit,
    onReport: (String, String) -> Unit,
) {
    val reel = item.reel
    val name = item.author?.name ?: "Unknown user"
    var menuOpen by remember { mutableStateOf(false) }
    var reporting by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        // The video itself (only the reel on screen holds the player)
        if (isCurrent) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        useController = false
                        this.player = sharedPlayer
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
        }

        // Tap anywhere to pause or play
        Box(
            Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onTogglePlay,
                )
        )

        if (isCurrent && buffering && !failed) {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.align(Alignment.Center))
        }
        if (isCurrent && failed) {
            Text("Couldn't play this video", color = Color.White, modifier = Modifier.align(Alignment.Center))
        }
        if (isCurrent && !isPlaying && !buffering && !failed) {
            Icon(
                Icons.Filled.PlayArrow,
                contentDescription = "Paused",
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.align(Alignment.Center).size(72.dp),
            )
        }

        // Author and caption, bottom left
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 12.dp, end = 84.dp, bottom = 24.dp)
                .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                .padding(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(item.author?.avatarUrl, name, size = 32.dp)
                Spacer(Modifier.width(8.dp))
                Text(name, color = Color.White, fontWeight = FontWeight.Bold)
            }
            if (reel.caption.isNotBlank()) {
                Text(
                    reel.caption,
                    color = Color.White,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }

        // Like, comment and more, bottom right
        Column(
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 8.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            IconButton(onClick = onLike) {
                Icon(
                    imageVector = if (item.liked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "Like",
                    tint = if (item.liked) Color(0xFFFF4D5A) else Color.White,
                    modifier = Modifier.size(32.dp),
                )
            }
            Text("${reel.likeCount}", color = Color.White)

            IconButton(onClick = onComments) {
                Icon(
                    Icons.Outlined.ChatBubbleOutline,
                    contentDescription = "Comments",
                    tint = Color.White,
                    modifier = Modifier.size(30.dp),
                )
            }
            Text("${reel.commentCount}", color = Color.White)

            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "More", tint = Color.White)
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    if (isMine) {
                        DropdownMenuItem(
                            text = { Text("Delete reel") },
                            onClick = { menuOpen = false; confirmDelete = true },
                        )
                    } else {
                        DropdownMenuItem(
                            text = { Text("Report reel") },
                            onClick = { menuOpen = false; reporting = true },
                        )
                    }
                }
            }
        }
    }

    if (reporting) {
        ReportDialog(
            title = "Report this reel",
            onDismiss = { reporting = false },
        ) { reason, details ->
            reporting = false
            onReport(reason, details)
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this reel?") },
            text = { Text("This can't be undone.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete() }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}
