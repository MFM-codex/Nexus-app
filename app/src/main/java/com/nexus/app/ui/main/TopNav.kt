package com.nexus.app.ui.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private data class NavTab(val route: String, val label: String, val icon: ImageVector)

private val navTabs = listOf(
    NavTab("home", "Home", Icons.Filled.Home),
    NavTab("reels", "Reels", Icons.Filled.PlayCircle),
    NavTab("friends", "Friends", Icons.Filled.People),
    NavTab("alerts", "Alerts", Icons.Filled.Notifications),
    NavTab("profile", "Profile", Icons.Filled.Person),
)

// The screens that show this bar.
val tabRoutes: List<String> = navTabs.map { it.route }

// Top of the app: logo + search + chats, and the tab icons underneath.
@Composable
fun TopNav(
    route: String?,
    unreadChats: Int,
    unreadAlerts: Int,
    onTab: (String) -> Unit,
    onSearch: () -> Unit,
    onChats: () -> Unit,
) {
    val selected = navTabs.indexOfFirst { it.route == route }.coerceAtLeast(0)

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
                FilledTonalIconButton(onClick = onSearch) {
                    Icon(Icons.Filled.Search, contentDescription = "Search people")
                }
                Spacer(Modifier.width(8.dp))
                FilledTonalIconButton(onClick = onChats) {
                    BadgedBox(badge = {
                        if (unreadChats > 0) Badge { Text(if (unreadChats > 9) "9+" else "$unreadChats") }
                    }) {
                        Icon(Icons.Filled.ChatBubble, contentDescription = "Chats")
                    }
                }
            }
            TabRow(
                selectedTabIndex = selected,
                containerColor = MaterialTheme.colorScheme.surface,
            ) {
                navTabs.forEachIndexed { index, tab ->
                    val count = if (tab.route == "alerts") unreadAlerts else 0
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
