package com.nexus.app.ui.friends

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nexus.app.data.Profile
import com.nexus.app.ui.components.Avatar

// The Friends tab, like Facebook's: friend requests on top, then Suggestions / Your friends.
@Composable
fun FriendsScreen(vm: FriendsViewModel, myUid: String, onOpenUser: (String) -> Unit) {
    val state by vm.state.collectAsState()
    var chip by rememberSaveable { mutableIntStateOf(0) } // 0 Suggestions, 1 Your friends, 2 Sent requests
    var searching by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbar.showSnackbar(it)
            vm.clearError()
        }
    }
    LaunchedEffect(chip, state.friendships.size, state.blocked.size) {
        if (chip == 0) vm.loadSuggestions()
    }

    val friends = state.friendships.filter { it.status == "accepted" }
    val incoming = state.friendships.filter { it.status == "pending" && it.addresseeId == myUid }
    val outgoing = state.friendships.filter { it.status == "pending" && it.requesterId == myUid }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Friends",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = {
                    searching = !searching
                    if (!searching) vm.onQueryChange("")
                }) {
                    Icon(
                        if (searching) Icons.Filled.Close else Icons.Filled.Search,
                        contentDescription = "Search people",
                    )
                }
            }
            if (searching) {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = { vm.onQueryChange(it) },
                    placeholder = { Text("Search people by username") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                )
            }

            if (searching && state.query.isNotBlank()) {
                // ----- search results -----
                if (state.searching) {
                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (state.results.isEmpty()) {
                    Text(
                        "No one found. Try the start of their username.",
                        modifier = Modifier.padding(24.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    LazyColumn(Modifier.fillMaxSize()) {
                        items(state.results, key = { it.uid }) { person ->
                            PersonRow(person, onClick = { onOpenUser(person.uid) }) {
                                RelationActions(
                                    relation = vm.relationWith(person.uid),
                                    name = person.name,
                                    compact = true,
                                    onAdd = { vm.sendRequest(person.uid) },
                                    onAccept = { vm.accept(person.uid) },
                                    onRemove = { vm.remove(person.uid) },
                                )
                            }
                        }
                    }
                }
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    item {
                        Row(
                            Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            FilterChip(selected = chip == 0, onClick = { chip = 0 }, label = { Text("Suggestions") })
                            FilterChip(selected = chip == 1, onClick = { chip = 1 }, label = { Text("Your friends (${friends.size})") })
                            FilterChip(selected = chip == 2, onClick = { chip = 2 }, label = { Text("Sent (${outgoing.size})") })
                        }
                    }

                    if (!state.loaded) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                    }

                    // ----- friend requests -----
                    if (incoming.isNotEmpty()) {
                        item { SectionTitle("Friend requests (${incoming.size})") }
                        items(incoming, key = { "in_" + it.id }) { f ->
                            val other = f.other(myUid)
                            RequestRow(
                                profile = state.profiles[other],
                                onOpen = { onOpenUser(other) },
                                onConfirm = { vm.accept(other) },
                                onDelete = { vm.remove(other) },
                            )
                        }
                    }

                    // ----- the chosen list -----
                    when (chip) {
                        0 -> {
                            item { SectionTitle("People you may know") }
                            if (state.suggestions.isEmpty()) {
                                item {
                                    Text(
                                        "No suggestions right now. Use the search icon to find people by username.",
                                        modifier = Modifier.padding(16.dp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            items(state.suggestions, key = { "s_" + it.uid }) { person ->
                                PersonRow(person, onClick = { onOpenUser(person.uid) }) {
                                    Button(onClick = { vm.sendRequest(person.uid) }) { Text("Add friend") }
                                }
                            }
                        }
                        1 -> {
                            if (state.loaded && friends.isEmpty()) {
                                item {
                                    Text(
                                        "No friends yet. Check Suggestions to add some.",
                                        modifier = Modifier.padding(16.dp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            items(friends, key = { "f_" + it.id }) { f ->
                                val other = f.other(myUid)
                                PersonRow(state.profiles[other], onClick = { onOpenUser(other) }) {}
                            }
                        }
                        else -> {
                            if (state.loaded && outgoing.isEmpty()) {
                                item {
                                    Text(
                                        "You haven't sent any requests.",
                                        modifier = Modifier.padding(16.dp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            items(outgoing, key = { "o_" + it.id }) { f ->
                                val other = f.other(myUid)
                                PersonRow(state.profiles[other], onClick = { onOpenUser(other) }) {
                                    OutlinedButton(onClick = { vm.remove(other) }) { Text("Cancel") }
                                }
                            }
                        }
                    }
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
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}

// A big friend request: picture, name, Confirm and Delete.
@Composable
private fun RequestRow(profile: Profile?, onOpen: () -> Unit, onConfirm: () -> Unit, onDelete: () -> Unit) {
    val name = profile?.name ?: "Unknown user"
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(profile?.avatarUrl, name, size = 76.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onConfirm, modifier = Modifier.weight(1f)) { Text("Confirm") }
                OutlinedButton(onClick = onDelete, modifier = Modifier.weight(1f)) { Text("Delete") }
            }
        }
    }
}

// One person in a list: avatar, name, @username, and something on the right.
@Composable
private fun PersonRow(profile: Profile?, onClick: () -> Unit, trailing: @Composable () -> Unit) {
    val name = profile?.name ?: "Unknown user"
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(profile?.avatarUrl, name)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(name, fontWeight = FontWeight.Bold)
            Text(
                "@${profile?.username ?: ""}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        trailing()
    }
}
