package com.nexus.app.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.firebase.auth.FirebaseUser
import com.nexus.app.ui.comments.CommentsScreen
import com.nexus.app.ui.comments.CommentsViewModel
import com.nexus.app.ui.comments.CommentsViewModelFactory
import com.nexus.app.ui.home.FeedViewModel
import com.nexus.app.ui.home.FeedViewModelFactory
import com.nexus.app.ui.home.HomeScreen
import com.nexus.app.ui.home.NewPostScreen
import com.nexus.app.ui.profile.EditProfileScreen
import com.nexus.app.ui.profile.ProfileScreen
import com.nexus.app.ui.profile.ProfileViewModel
import com.nexus.app.ui.profile.ProfileViewModelFactory

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("home", "Home", Icons.Filled.Home),
    Tab("friends", "Friends", Icons.Filled.People),
    Tab("chats", "Chats", Icons.Filled.ChatBubble),
    Tab("alerts", "Alerts", Icons.Filled.Notifications),
    Tab("profile", "Profile", Icons.Filled.Person),
)

// The signed-in app: bottom navigation bar + the screen for the selected tab.
@Composable
fun MainScaffold(user: FirebaseUser, onSignOut: () -> Unit) {
    // key = uid so a different account never sees the previous account's data
    val profileVm: ProfileViewModel = viewModel(
        key = user.uid,
        factory = ProfileViewModelFactory(user.uid, user.email, user.displayName),
    )
    val feedVm: FeedViewModel = viewModel(
        key = "feed_${user.uid}",
        factory = FeedViewModelFactory(user.uid),
    )
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route

    // Full-screen pages hide the bottom bar
    val hideBar = route == "editProfile" || route == "newPost" || route?.startsWith("comments") == true

    Scaffold(
        bottomBar = {
            if (!hideBar) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = route == tab.route,
                            onClick = {
                                nav.navigate(tab.route) {
                                    popUpTo("home") { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(nav, startDestination = "home", modifier = Modifier.padding(padding)) {
            composable("home") {
                HomeScreen(
                    vm = feedVm,
                    myUid = user.uid,
                    onNewPost = { nav.navigate("newPost") },
                    onOpenComments = { postId -> nav.navigate("comments/$postId") },
                )
            }
            composable("newPost") {
                NewPostScreen(vm = feedVm, onDone = { nav.popBackStack() })
            }
            composable(
                route = "comments/{postId}",
                arguments = listOf(navArgument("postId") { type = NavType.StringType }),
            ) { entry ->
                val postId = entry.arguments?.getString("postId") ?: return@composable
                val commentsVm: CommentsViewModel = viewModel(
                    key = "comments_$postId",
                    factory = CommentsViewModelFactory(postId, user.uid),
                )
                CommentsScreen(
                    vm = commentsVm,
                    myUid = user.uid,
                    onBack = { nav.popBackStack() },
                    onCountChange = { delta -> feedVm.adjustCommentCount(postId, delta) },
                )
            }
            composable("friends") { Placeholder("Friends", "Search and friend requests arrive in Phase 3.") }
            composable("chats") { Placeholder("Chats", "Messaging arrives in Phase 4.") }
            composable("alerts") { Placeholder("Alerts", "Notifications arrive in Phase 4.") }
            composable("profile") {
                ProfileScreen(
                    vm = profileVm,
                    onEdit = { nav.navigate("editProfile") },
                    onSignOut = onSignOut,
                )
            }
            composable("editProfile") {
                EditProfileScreen(vm = profileVm, onDone = { nav.popBackStack() })
            }
        }
    }
}

@Composable
private fun Placeholder(title: String, subtitle: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
