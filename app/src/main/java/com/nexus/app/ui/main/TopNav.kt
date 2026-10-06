package com.nexus.app.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private data class NavTab(val route: String, val label: String, val icon: ImageVector)

// Same order as Facebook: Home, Friends, Messenger, Reels, Notifications, Marketplace.
private val navTabs = listOf(
    NavTab("home", "Home", Icons.Filled.Home),
    NavTab("friends", "Friends", Icons.Filled.People),
    NavTab("chats", "Messages", Icons.Filled.ChatBubble),
    NavTab("reels", "Reels", Icons.Filled.PlayCircle),
    NavTab("alerts", "Alerts", Icons.Filled.Notifications),
    NavTab("marketplace", "Marketplace", Icons.Filled.Storefront),
)

// The screens that show this bar.
val tabRoutes: List<String> = navTabs.map { it.route }

// What the "+" button can create: label to the screen it opens.
private val createItems = listOf(
    "Post" to "newPost",
    "Reel" to "newReel",
    "Sell something" to "newListing",
    "Event" to "newEvent",
    "Group" to "newCommunity/groups",
    "Page" to "newCommunity/pages",
)

// Top of the app: logo, + search menu, and the six tabs underneath.
@Composable
fun TopNav(
    route: String?,
    unreadChats: Int,
    unreadAlerts: Int,
    friendRequests: Int,
    onTab: (String) -> Unit,
    onSearch: () -> Unit,
    onMenu: () -> Unit,
    onCreate: (String) -> Unit,
) {
    val selected = navTabs.indexOfFirst { it.route == route }.coerceAtLeast(0)
    var createOpen by remember { mutableStateOf(false) }

    Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 3.dp) {
        Column(Modifier.statusBarsPadding()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "nexus",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                Box {
                    FilledTonalIconButton(onClick = { createOpen = true }) {
                        Icon(Icons.Filled.Add, contentDescription = "Create")
                    }
                    DropdownMenu(expanded = createOpen, onDismissRequest = { createOpen = false }) {
                        createItems.forEach { (label, target) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = { createOpen = false; onCreate(target) },
                            )
                        }
                    }
                }
                Spacer(Modifier.width(8.dp))
                FilledTonalIconButton(onClick = onSearch) {
                    Icon(Icons.Filled.Search, contentDescription = "Search people")
                }
                Spacer(Modifier.width(8.dp))
                FilledTonalIconButton(onClick = onMenu) {
                    Icon(Icons.Filled.Menu, contentDescription = "Menu")
                }
            }
            TabRow(
                selectedTabIndex = selected,
                containerColor = MaterialTheme.colorScheme.surface,
            ) {
                navTabs.forEachIndexed { index, tab ->
                    val count = when (tab.route) {
                        "chats" -> unreadChats
                        "alerts" -> unreadAlerts
                        "friends" -> friendRequests
                        else -> 0
                    }
                    Tab(
                        selected = index == selected,
                        onClick = { onTab(tab.route) },
                        icon = {
                            BadgedBox(badge = {
                                if (count > 0) Badge { Text(if (count > 9) "9+" else "$count") }
                            }) {
                                Icon(tab.icon, contentDescription = tab.label)
                            }
                        },
                    )
                }
            }
        }
    }
}
