package com.nexus.app.ui.menu

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nexus.app.data.Profile
import com.nexus.app.ui.components.Avatar
import kotlinx.coroutines.launch

private data class Tile(
    val label: String,
    val icon: ImageVector,
    val badge: Int = 0,
    val soon: Boolean = false,
    val onClick: () -> Unit = {},
)

// The Menu page (opened with the hamburger button): profile, shortcuts, settings, help, log out.
@Composable
fun MenuScreen(
    profile: Profile?,
    isAdmin: Boolean,
    unreadChats: Int,
    onBack: () -> Unit,
    onOpenProfile: () -> Unit,
    onMessages: () -> Unit,
    onGroups: () -> Unit,
    onFriends: () -> Unit,
    onReels: () -> Unit,
    onMarketplace: () -> Unit,
    onPages: () -> Unit,
    onSaved: () -> Unit,
    onEvents: () -> Unit,
    onAdmin: () -> Unit,
    onSettings: () -> Unit,
    onHelp: () -> Unit,
    onLogout: () -> Unit,
) {
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var confirmLogout by remember { mutableStateOf(false) }
    val name = profile?.name ?: "Your profile"

    val tiles = buildList {
        add(Tile("Messages", Icons.Filled.ChatBubble, badge = unreadChats, onClick = onMessages))
        add(Tile("Groups", Icons.Filled.Groups, onClick = onGroups))
        add(Tile("Friends", Icons.Filled.People, onClick = onFriends))
        add(Tile("Reels", Icons.Filled.PlayCircle, onClick = onReels))
        add(Tile("Marketplace", Icons.Filled.Storefront, onClick = onMarketplace))
        add(Tile("Pages", Icons.Filled.Flag, onClick = onPages))
        add(Tile("Saved", Icons.Filled.Bookmark, onClick = onSaved))
        add(Tile("Events", Icons.Filled.Event, onClick = onEvents))
        add(Tile("Memories", Icons.Filled.History, soon = true))
        add(Tile("Birthdays", Icons.Filled.Cake, soon = true))
        if (isAdmin) add(Tile("Admin panel", Icons.Filled.AdminPanelSettings, onClick = onAdmin))
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(4.dp)) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text("Menu", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            }

            Column(Modifier.padding(horizontal = 12.dp)) {
                // Your profile
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenProfile),
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Avatar(profile?.avatarUrl, name, size = 52.dp)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(name, fontWeight = FontWeight.Bold)
                            Text("View your profile", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                // Invite friends
                Spacer(Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth().clickable {
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "Join me on Nexus! Ask me for the app link.")
                        }
                        context.startActivity(Intent.createChooser(send, "Invite friends"))
                    },
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Favorite, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(14.dp))
                        Text("Invite friends", fontWeight = FontWeight.Medium)
                    }
                }

                // The shortcut grid
                tiles.chunked(2).forEach { pair ->
                    Row(
                        Modifier.fillMaxWidth().padding(top = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        pair.forEach { tile ->
                            ShortcutTile(
                                tile = tile,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    if (tile.soon) {
                                        scope.launch { snackbar.showSnackbar("${tile.label} is coming soon") }
                                    } else {
                                        tile.onClick()
                                    }
                                },
                            )
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }

                Spacer(Modifier.height(16.dp))
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column {
                        MenuRow(Icons.Filled.Settings, "Settings & privacy", onSettings)
                        HorizontalDivider()
                        MenuRow(Icons.Filled.Info, "Help & support", onHelp)
                    }
                }

                Spacer(Modifier.height(16.dp))
                OutlinedButton(
                    onClick = { confirmLogout = true },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                ) { Text("Log out") }
                Spacer(Modifier.height(24.dp))
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }

    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            title = { Text("Log out?") },
            text = { Text("You'll need to log in again to use Nexus.") },
            confirmButton = {
                TextButton(onClick = { confirmLogout = false; onLogout() }) { Text("Log out") }
            },
            dismissButton = { TextButton(onClick = { confirmLogout = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun ShortcutTile(tile: Tile, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier.alpha(if (tile.soon) 0.6f else 1f).clickable(onClick = onClick),
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            BadgedBox(badge = {
                if (tile.badge > 0) Badge { Text(if (tile.badge > 9) "9+" else "${tile.badge}") }
            }) {
                Icon(
                    tile.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(30.dp),
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(tile.label, fontWeight = FontWeight.SemiBold)
            if (tile.soon) {
                Text(
                    "Coming soon",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun MenuRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(14.dp))
        Text(label, fontWeight = FontWeight.Medium)
    }
}
