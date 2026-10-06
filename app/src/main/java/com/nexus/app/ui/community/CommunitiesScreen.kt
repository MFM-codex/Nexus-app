package com.nexus.app.ui.community

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.nexus.app.ui.components.Avatar

// "Groups" or "Pages": the ones you belong to, then others you can discover.
@Composable
fun CommunitiesScreen(
    type: String,
    vm: CommunitiesViewModel,
    onBack: () -> Unit,
    onCreate: () -> Unit,
    onOpen: (String) -> Unit,
) {
    val isGroups = type == "groups"
    val loaded by vm.items.collectAsState()
    val message by vm.message.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val list = loaded.orEmpty()

    // Reload the list when coming back to this screen (for example after deleting one)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) vm.load()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

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
                    if (isGroups) "Groups" else "Pages",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                Button(onClick = onCreate, modifier = Modifier.padding(end = 8.dp)) {
                    Text(if (isGroups) "Create group" else "Create page")
                }
            }
            HorizontalDivider()

            LazyColumn(Modifier.fillMaxSize()) {
                if (loaded == null) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                } else if (list.isEmpty()) {
                    item {
                        Text(
                            if (isGroups) "No groups yet. Create the first one!" else "No pages yet. Create the first one!",
                            modifier = Modifier.padding(24.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                val mine = list.filter { it.member }
                val others = list.filter { !it.member }
                if (mine.isNotEmpty()) {
                    item { SectionTitle(if (isGroups) "Your groups" else "Pages you follow") }
                    items(mine, key = { "m_" + it.community.id }) { item -> CommunityRow(item, isGroups, vm, onOpen) }
                }
                if (others.isNotEmpty()) {
                    item { SectionTitle("Discover") }
                    items(others, key = { "o_" + it.community.id }) { item -> CommunityRow(item, isGroups, vm, onOpen) }
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun CommunityRow(item: CommunityUi, isGroups: Boolean, vm: CommunitiesViewModel, onOpen: (String) -> Unit) {
    val c = item.community
    Row(
        Modifier.fillMaxWidth().clickable { onOpen(c.id) }.padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(null, c.name, size = 52.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(c.name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val line = if (!isGroups && c.category.isNotBlank()) c.category else c.description
            if (line.isNotBlank()) {
                Text(
                    line,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        if (item.member) {
            OutlinedButton(onClick = { vm.toggle(item) }) { Text(if (isGroups) "Joined" else "Following") }
        } else {
            Button(onClick = { vm.toggle(item) }) { Text(if (isGroups) "Join" else "Follow") }
        }
    }
}
