package com.nexus.app.ui.friends

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nexus.app.data.Profile
import com.nexus.app.ui.components.Avatar

// The Friends tab: search box on top, then "Friends" and "Requests" tabs.
@Composable
fun FriendsScreen(vm: FriendsViewModel, myUid: String, onOpenUser: (String) -> Unit) {
    val state by vm.state.collectAsState()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbar.showSnackbar(it)
            vm.clearError()
        }
    }

    val friends = state.friendships.filter { it.status == "accepted" }
    val incoming = state.friendships.filter { it.status == "pending" && it.addresseeId == myUid }
    val outgoing = state.friendships.filter { it.status == "pending" && it.requesterId == myUid }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            OutlinedTextField(
                value = state.query,
                onValueChange = { vm.onQueryChange(it) },
                placeholder = { Text("Search people by username") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(12.dp),
            )

            if (state.query.isNotBlank()) {
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
                // ----- friends / requests tabs -----
                TabRow(selectedTabIndex = tab) {
                    Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Friends (${friends.size})") })
                    Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Requests (${incoming.size})") })
                }

                if (tab == 0) {
                    LazyColumn(Modifier.fillMaxSize()) {
                        if (!state.loaded) {
                            item {
                                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator()
                                }
                            }
                        } else if (friends.isEmpty()) {
                            item {
                                Text(
                                    "No friends yet. Search for people above and send a request.",
                                    modifier = Modifier.padding(24.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        items(friends, key = { it.id }) { f ->
                            val other = f.other(myUid)
                            PersonRow(state.profiles[other], onClick = { onOpenUser(other) }) {}
                        }
                    }
                } else {
                    LazyColumn(Modifier.fillMaxSize()) {
                        if (state.loaded && incoming.isEmpty() && outgoing.isEmpty()) {
                            item {
                                Text(
                                    "No friend requests.",
                                    modifier = Modifier.padding(24.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        if (incoming.isNotEmpty()) {
                            item { SectionTitle("Requests received") }
                            items(incoming, key = { it.id }) { f ->
                                val other = f.other(myUid)
                                PersonRow(state.profiles[other], onClick = { onOpenUser(other) }) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(onClick = { vm.accept(other) }) { Text("Accept") }
                                        OutlinedButton(onClick = { vm.remove(other) }) { Text("Decline") }
                                    }
                                }
                            }
                        }
                        if (outgoing.isNotEmpty()) {
                            item { SectionTitle("Requests sent") }
                            items(outgoing, key = { it.id }) { f ->
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
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 12.dp, top = 16.dp, bottom = 4.dp),
    )
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
