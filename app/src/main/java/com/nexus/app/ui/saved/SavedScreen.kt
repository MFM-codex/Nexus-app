package com.nexus.app.ui.saved

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.nexus.app.ui.components.Avatar
import com.nexus.app.ui.components.timeAgo

// Menu > Saved: the posts you bookmarked.
@Composable
fun SavedScreen(vm: SavedViewModel, onBack: () -> Unit) {
    val loaded by vm.items.collectAsState()
    val errorText by vm.error.collectAsState()
    val list = loaded.orEmpty()

    Column(Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(4.dp)) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Saved", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        HorizontalDivider()

        if (loaded == null) {
            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                if (list.isEmpty()) {
                    item {
                        Text(
                            errorText ?: "Nothing saved yet. Tap the bookmark on a post to save it here.",
                            modifier = Modifier.padding(24.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                items(list, key = { it.post.id }) { saved ->
                    val post = saved.post
                    val name = saved.author?.name ?: "Unknown user"
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Avatar(saved.author?.avatarUrl, name)
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(name, fontWeight = FontWeight.Bold)
                                    Text(
                                        timeAgo(post.createdAt),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
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
                            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                TextButton(onClick = { vm.unsave(post.id) }) { Text("Remove from saved") }
                            }
                        }
                    }
                }
            }
        }
    }
}
