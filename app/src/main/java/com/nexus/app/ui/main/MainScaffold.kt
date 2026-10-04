package com.nexus.app.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.firebase.auth.FirebaseUser
import com.nexus.app.ui.admin.AdminScreen
import com.nexus.app.ui.admin.AdminViewModel
import com.nexus.app.ui.admin.AdminViewModelFactory
import com.nexus.app.ui.alerts.AlertsScreen
import com.nexus.app.ui.alerts.NotificationsViewModel
import com.nexus.app.ui.alerts.NotificationsViewModelFactory
import com.nexus.app.ui.chat.ChatScreen
import com.nexus.app.ui.chat.ChatViewModel
import com.nexus.app.ui.chat.ChatViewModelFactory
import com.nexus.app.ui.chat.ChatsScreen
import com.nexus.app.ui.chat.ChatsViewModel
import com.nexus.app.ui.chat.ChatsViewModelFactory
import com.nexus.app.ui.comments.CommentsScreen
import com.nexus.app.ui.comments.CommentsViewModel
import com.nexus.app.ui.comments.CommentsViewModelFactory
import com.nexus.app.ui.components.LoadingDots
import com.nexus.app.ui.friends.FriendsScreen
import com.nexus.app.ui.friends.FriendsViewModel
import com.nexus.app.ui.friends.FriendsViewModelFactory
import com.nexus.app.ui.friends.UserProfileScreen
import com.nexus.app.ui.home.FeedViewModel
import com.nexus.app.ui.menu.BlockedScreen
import com.nexus.app.ui.menu.HelpScreen
import com.nexus.app.ui.menu.MenuScreen
import com.nexus.app.ui.menu.SettingsScreen
import com.nexus.app.ui.saved.SavedScreen
import com.nexus.app.ui.saved.SavedViewModel
import com.nexus.app.ui.saved.SavedViewModelFactory
import com.nexus.app.ui.home.FeedViewModelFactory
import com.nexus.app.ui.home.HomeScreen
import com.nexus.app.ui.home.NewPostScreen
import com.nexus.app.ui.profile.EditProfileScreen
import com.nexus.app.ui.profile.ProfileScreen
import com.nexus.app.ui.profile.ProfileViewModel
import com.nexus.app.ui.profile.ProfileViewModelFactory
import com.nexus.app.ui.profile.SetupProfileScreen
import com.nexus.app.ui.reels.NewReelScreen
import com.nexus.app.ui.reels.ReelsScreen
import com.nexus.app.ui.reels.ReelsViewModel
import com.nexus.app.ui.reels.ReelsViewModelFactory

// If an admin banned this account we show a "suspended" page instead of the app.
@Composable
fun MainScaffold(user: FirebaseUser, onSignOut: () -> Unit) {
    val adminVm: AdminViewModel = viewModel(
        key = "admin_${user.uid}",
        factory = AdminViewModelFactory(user.uid),
    )
    val banned by adminVm.banned.collectAsState()
    if (banned) {
        BannedScreen(onSignOut)
    } else {
        ProfileGate(user, adminVm, onSignOut)
    }
}

// Waits for the profile to load. If there isn't one yet, asks the person to set it up.
@Composable
private fun ProfileGate(user: FirebaseUser, adminVm: AdminViewModel, onSignOut: () -> Unit) {
    // key = uid so a different account never sees the previous account's data
    val profileVm: ProfileViewModel = viewModel(
        key = user.uid,
        factory = ProfileViewModelFactory(user.uid, user.email, user.displayName),
    )
    val loaded by profileVm.loaded.collectAsState()
    val profile by profileVm.profile.collectAsState()

    when {
        !loaded -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { LoadingDots() }
        profile == null -> SetupProfileScreen(profileVm, user.displayName, user.email, onSignOut)
        else -> MainContent(user, adminVm, profileVm, onSignOut)
    }
}

// The signed-in app: top bar with tabs + the screen for the selected tab.
@Composable
private fun MainContent(
    user: FirebaseUser,
    adminVm: AdminViewModel,
    profileVm: ProfileViewModel,
    onSignOut: () -> Unit,
) {
    val feedVm: FeedViewModel = viewModel(
        key = "feed_${user.uid}",
        factory = FeedViewModelFactory(user.uid),
    )
    val reelsVm: ReelsViewModel = viewModel(
        key = "reels_${user.uid}",
        factory = ReelsViewModelFactory(user.uid),
    )
    val friendsVm: FriendsViewModel = viewModel(
        key = "friends_${user.uid}",
        factory = FriendsViewModelFactory(user.uid),
    )
    val chatsVm: ChatsViewModel = viewModel(
        key = "chats_${user.uid}",
        factory = ChatsViewModelFactory(user.uid),
    )
    val notifVm: NotificationsViewModel = viewModel(
        key = "notifs_${user.uid}",
        factory = NotificationsViewModelFactory(user.uid),
    )
    val unreadChats by chatsVm.totalUnread.collectAsState()
    val unreadAlerts by notifVm.unreadCount.collectAsState()
    val isAdmin by adminVm.isAdmin.collectAsState()
    val myProfile by profileVm.profile.collectAsState()

    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route

    // Switch tabs without piling up screens
    fun goTab(target: String) {
        nav.navigate(target) {
            popUpTo("home") { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        topBar = {
            // Only the four main tabs show the top bar; other pages are full screen
            if (route != null && route in tabRoutes) {
                TopNav(
                    route = route,
                    unreadChats = unreadChats,
                    unreadAlerts = unreadAlerts,
                    onTab = { goTab(it) },
                    onSearch = { goTab("friends") },
                    onChats = { nav.navigate("chats") { launchSingleTop = true } },
                )
            }
        },
    ) { padding ->
        NavHost(nav, startDestination = "home", modifier = Modifier.padding(padding)) {
            composable("home") {
                HomeScreen(
                    vm = feedVm,
                    myUid = user.uid,
                    myProfile = myProfile,
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
            composable("reels") {
                ReelsScreen(
                    vm = reelsVm,
                    myUid = user.uid,
                    onNewReel = { nav.navigate("newReel") },
                    onOpenComments = { reelId -> nav.navigate("reelComments/$reelId") },
                )
            }
            composable("newReel") {
                NewReelScreen(vm = reelsVm, onDone = { nav.popBackStack() })
            }
            composable(
                route = "reelComments/{reelId}",
                arguments = listOf(navArgument("reelId") { type = NavType.StringType }),
            ) { entry ->
                val reelId = entry.arguments?.getString("reelId") ?: return@composable
                val reelCommentsVm: CommentsViewModel = viewModel(
                    key = "reelcomments_$reelId",
                    factory = CommentsViewModelFactory(reelId, user.uid, "reels"),
                )
                CommentsScreen(
                    vm = reelCommentsVm,
                    myUid = user.uid,
                    onBack = { nav.popBackStack() },
                    onCountChange = { delta -> reelsVm.adjustCommentCount(reelId, delta) },
                )
            }
            composable("friends") {
                FriendsScreen(
                    vm = friendsVm,
                    myUid = user.uid,
                    onOpenUser = { id -> nav.navigate("user/$id") },
                )
            }
            composable(
                route = "user/{uid}",
                arguments = listOf(navArgument("uid") { type = NavType.StringType }),
            ) { entry ->
                val otherUid = entry.arguments?.getString("uid") ?: return@composable
                UserProfileScreen(
                    vm = friendsVm,
                    otherUid = otherUid,
                    onBack = { nav.popBackStack() },
                    onMessage = { nav.navigate("chat/$otherUid") },
                )
            }
            composable("chats") {
                ChatsScreen(
                    vm = chatsVm,
                    myUid = user.uid,
                    onBack = { nav.popBackStack() },
                    onOpenChat = { otherId -> nav.navigate("chat/$otherId") },
                )
            }
            composable("alerts") {
                AlertsScreen(
                    vm = notifVm,
                    onOpenUser = { id -> nav.navigate("user/$id") },
                    onOpenPost = { postId -> nav.navigate("comments/$postId") },
                )
            }
            composable(
                route = "chat/{uid}",
                arguments = listOf(navArgument("uid") { type = NavType.StringType }),
            ) { entry ->
                val otherUid = entry.arguments?.getString("uid") ?: return@composable
                val chatVm: ChatViewModel = viewModel(
                    key = "chat_${user.uid}_$otherUid",
                    factory = ChatViewModelFactory(user.uid, otherUid),
                )
                ChatScreen(
                    vm = chatVm,
                    myUid = user.uid,
                    onBack = { nav.popBackStack() },
                    onOpenProfile = { nav.navigate("user/$otherUid") },
                )
            }
            composable("profile") {
                ProfileScreen(
                    vm = profileVm,
                    onEdit = { nav.navigate("editProfile") },
                    onSignOut = onSignOut,
                    isAdmin = isAdmin,
                    onOpenAdmin = { nav.navigate("admin") },
                    onBack = { nav.popBackStack() },
                )
            }
            composable("menu") {
                MenuScreen(
                    profile = myProfile,
                    isAdmin = isAdmin,
                    unreadChats = unreadChats,
                    unreadAlerts = unreadAlerts,
                    onOpenProfile = { nav.navigate("profile") },
                    onFriends = { goTab("friends") },
                    onReels = { goTab("reels") },
                    onChats = { nav.navigate("chats") { launchSingleTop = true } },
                    onAlerts = { goTab("alerts") },
                    onSaved = { nav.navigate("saved") },
                    onAdmin = { nav.navigate("admin") },
                    onSettings = { nav.navigate("settings") },
                    onHelp = { nav.navigate("help") },
                    onLogout = onSignOut,
                )
            }
            composable("saved") {
                val savedVm: SavedViewModel = viewModel(
                    key = "saved_${user.uid}",
                    factory = SavedViewModelFactory(user.uid),
                )
                SavedScreen(vm = savedVm, onBack = { nav.popBackStack() })
            }
            composable("settings") {
                SettingsScreen(
                    onBack = { nav.popBackStack() },
                    onEditProfile = { nav.navigate("editProfile") },
                    onBlocked = { nav.navigate("blocked") },
                )
            }
            composable("blocked") {
                BlockedScreen(vm = friendsVm, onBack = { nav.popBackStack() })
            }
            composable("help") {
                HelpScreen(onBack = { nav.popBackStack() })
            }
            composable("admin") {
                AdminScreen(
                    vm = adminVm,
                    onBack = { nav.popBackStack() },
                    onOpenUser = { id -> nav.navigate("user/$id") },
                )
            }
            composable("editProfile") {
                EditProfileScreen(vm = profileVm, onDone = { nav.popBackStack() })
            }
        }
    }
}
