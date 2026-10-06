package com.nexus.app.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.nexus.app.data.Relation
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
import com.nexus.app.ui.community.CommunitiesScreen
import com.nexus.app.ui.community.CommunitiesViewModel
import com.nexus.app.ui.community.CommunitiesViewModelFactory
import com.nexus.app.ui.community.CommunityDetailScreen
import com.nexus.app.ui.community.CommunityDetailViewModel
import com.nexus.app.ui.community.CommunityDetailViewModelFactory
import com.nexus.app.ui.community.NewCommunityPostScreen
import com.nexus.app.ui.community.NewCommunityScreen
import com.nexus.app.ui.components.LoadingDots
import com.nexus.app.ui.events.EventDetailScreen
import com.nexus.app.ui.events.EventDetailViewModel
import com.nexus.app.ui.events.EventDetailViewModelFactory
import com.nexus.app.ui.events.EventsScreen
import com.nexus.app.ui.events.EventsViewModel
import com.nexus.app.ui.events.EventsViewModelFactory
import com.nexus.app.ui.events.NewEventScreen
import com.nexus.app.ui.friends.FriendsScreen
import com.nexus.app.ui.friends.FriendsViewModel
import com.nexus.app.ui.friends.FriendsViewModelFactory
import com.nexus.app.ui.friends.UserProfileScreen
import com.nexus.app.ui.home.FeedViewModel
import com.nexus.app.ui.home.FeedViewModelFactory
import com.nexus.app.ui.home.HomeScreen
import com.nexus.app.ui.home.NewPostScreen
import com.nexus.app.ui.marketplace.ListingScreen
import com.nexus.app.ui.marketplace.ListingViewModel
import com.nexus.app.ui.marketplace.ListingViewModelFactory
import com.nexus.app.ui.marketplace.MarketplaceScreen
import com.nexus.app.ui.marketplace.MarketplaceViewModel
import com.nexus.app.ui.marketplace.MarketplaceViewModelFactory
import com.nexus.app.ui.marketplace.NewListingScreen
import com.nexus.app.ui.menu.BlockedScreen
import com.nexus.app.ui.menu.HelpScreen
import com.nexus.app.ui.menu.MenuScreen
import com.nexus.app.ui.menu.SettingsScreen
import com.nexus.app.ui.profile.EditProfileScreen
import com.nexus.app.ui.profile.ProfileScreen
import com.nexus.app.ui.profile.ProfileViewModel
import com.nexus.app.ui.profile.ProfileViewModelFactory
import com.nexus.app.ui.profile.SetupProfileScreen
import com.nexus.app.ui.reels.NewReelScreen
import com.nexus.app.ui.reels.ReelsScreen
import com.nexus.app.ui.reels.ReelsViewModel
import com.nexus.app.ui.reels.ReelsViewModelFactory
import com.nexus.app.ui.saved.SavedScreen
import com.nexus.app.ui.saved.SavedViewModel
import com.nexus.app.ui.saved.SavedViewModelFactory

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
    val uid = user.uid
    val feedVm: FeedViewModel = viewModel(key = "feed_$uid", factory = FeedViewModelFactory(uid))
    val reelsVm: ReelsViewModel = viewModel(key = "reels_$uid", factory = ReelsViewModelFactory(uid))
    val friendsVm: FriendsViewModel = viewModel(key = "friends_$uid", factory = FriendsViewModelFactory(uid))
    val chatsVm: ChatsViewModel = viewModel(key = "chats_$uid", factory = ChatsViewModelFactory(uid))
    val notifVm: NotificationsViewModel = viewModel(key = "notifs_$uid", factory = NotificationsViewModelFactory(uid))
    val marketVm: MarketplaceViewModel = viewModel(key = "market_$uid", factory = MarketplaceViewModelFactory(uid))
    val eventsVm: EventsViewModel = viewModel(key = "events_$uid", factory = EventsViewModelFactory(uid))
    val groupsVm: CommunitiesViewModel = viewModel(key = "groups_$uid", factory = CommunitiesViewModelFactory("groups", uid))
    val pagesVm: CommunitiesViewModel = viewModel(key = "pages_$uid", factory = CommunitiesViewModelFactory("pages", uid))

    val unreadChats by chatsVm.totalUnread.collectAsState()
    val unreadAlerts by notifVm.unreadCount.collectAsState()
    val isAdmin by adminVm.isAdmin.collectAsState()
    val myProfile by profileVm.profile.collectAsState()
    val friendsState by friendsVm.state.collectAsState()
    val friendRequests = friendsState.friendships.count { it.status == "pending" && it.addresseeId == uid }

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
            // Only the six main tabs show the top bar; other pages are full screen
            if (route != null && route in tabRoutes) {
                TopNav(
                    route = route,
                    unreadChats = unreadChats,
                    unreadAlerts = unreadAlerts,
                    friendRequests = friendRequests,
                    onTab = { goTab(it) },
                    onSearch = { goTab("friends") },
                    onMenu = { nav.navigate("menu") { launchSingleTop = true } },
                    onCreate = { target -> nav.navigate(target) },
                )
            }
        },
    ) { padding ->
        NavHost(nav, startDestination = "home", modifier = Modifier.padding(padding)) {

            // ---------------- the six tabs ----------------
            composable("home") {
                HomeScreen(
                    vm = feedVm,
                    myUid = uid,
                    myProfile = myProfile,
                    onNewPost = { nav.navigate("newPost") },
                    onOpenComments = { postId -> nav.navigate("comments/$postId") },
                )
            }
            composable("friends") {
                FriendsScreen(vm = friendsVm, myUid = uid, onOpenUser = { id -> nav.navigate("user/$id") })
            }
            composable("chats") {
                ChatsScreen(
                    vm = chatsVm,
                    myUid = uid,
                    onBack = { nav.popBackStack() },
                    onOpenChat = { otherId -> nav.navigate("chat/$otherId") },
                )
            }
            composable("reels") {
                ReelsScreen(
                    vm = reelsVm,
                    myUid = uid,
                    onNewReel = { nav.navigate("newReel") },
                    onOpenComments = { reelId -> nav.navigate("reelComments/$reelId") },
                )
            }
            composable("alerts") {
                AlertsScreen(
                    vm = notifVm,
                    onOpenUser = { id -> nav.navigate("user/$id") },
                    onOpenPost = { postId -> nav.navigate("comments/$postId") },
                    isIncomingRequest = { id -> friendsVm.relationWith(id) == Relation.INCOMING },
                    onConfirm = { id -> friendsVm.accept(id) },
                    onDecline = { id -> friendsVm.remove(id) },
                )
            }
            composable("marketplace") {
                MarketplaceScreen(
                    vm = marketVm,
                    onSell = { nav.navigate("newListing") },
                    onOpen = { id -> nav.navigate("listing/$id") },
                )
            }

            // ---------------- menu pages ----------------
            composable("menu") {
                MenuScreen(
                    profile = myProfile,
                    isAdmin = isAdmin,
                    unreadChats = unreadChats,
                    onBack = { nav.popBackStack() },
                    onOpenProfile = { nav.navigate("profile") },
                    onMessages = { goTab("chats") },
                    onGroups = { nav.navigate("communities/groups") },
                    onFriends = { goTab("friends") },
                    onReels = { goTab("reels") },
                    onMarketplace = { goTab("marketplace") },
                    onPages = { nav.navigate("communities/pages") },
                    onSaved = { nav.navigate("saved") },
                    onEvents = { nav.navigate("events") },
                    onAdmin = { nav.navigate("admin") },
                    onSettings = { nav.navigate("settings") },
                    onHelp = { nav.navigate("help") },
                    onLogout = onSignOut,
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
            composable("editProfile") {
                EditProfileScreen(vm = profileVm, onDone = { nav.popBackStack() })
            }
            composable("saved") {
                val savedVm: SavedViewModel = viewModel(key = "saved_$uid", factory = SavedViewModelFactory(uid))
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

            // ---------------- posts, people, chat ----------------
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
                    factory = CommentsViewModelFactory(postId, uid),
                )
                CommentsScreen(
                    vm = commentsVm,
                    myUid = uid,
                    onBack = { nav.popBackStack() },
                    onCountChange = { delta -> feedVm.adjustCommentCount(postId, delta) },
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
            composable(
                route = "chat/{uid}",
                arguments = listOf(navArgument("uid") { type = NavType.StringType }),
            ) { entry ->
                val otherUid = entry.arguments?.getString("uid") ?: return@composable
                val chatVm: ChatViewModel = viewModel(
                    key = "chat_${uid}_$otherUid",
                    factory = ChatViewModelFactory(uid, otherUid),
                )
                ChatScreen(
                    vm = chatVm,
                    myUid = uid,
                    onBack = { nav.popBackStack() },
                    onOpenProfile = { nav.navigate("user/$otherUid") },
                )
            }

            // ---------------- reels ----------------
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
                    factory = CommentsViewModelFactory(reelId, uid, "reels"),
                )
                CommentsScreen(
                    vm = reelCommentsVm,
                    myUid = uid,
                    onBack = { nav.popBackStack() },
                    onCountChange = { delta -> reelsVm.adjustCommentCount(reelId, delta) },
                )
            }

            // ---------------- marketplace ----------------
            composable("newListing") {
                NewListingScreen(vm = marketVm, onDone = { nav.popBackStack() })
            }
            composable(
                route = "listing/{id}",
                arguments = listOf(navArgument("id") { type = NavType.StringType }),
            ) { entry ->
                val listingId = entry.arguments?.getString("id") ?: return@composable
                val listingVm: ListingViewModel = viewModel(
                    key = "listing_$listingId",
                    factory = ListingViewModelFactory(listingId, uid),
                )
                ListingScreen(vm = listingVm, myUid = uid, onBack = { nav.popBackStack() })
            }

            // ---------------- groups and pages ----------------
            composable(
                route = "communities/{type}",
                arguments = listOf(navArgument("type") { type = NavType.StringType }),
            ) { entry ->
                val type = entry.arguments?.getString("type") ?: return@composable
                CommunitiesScreen(
                    type = type,
                    vm = if (type == "groups") groupsVm else pagesVm,
                    onBack = { nav.popBackStack() },
                    onCreate = { nav.navigate("newCommunity/$type") },
                    onOpen = { id -> nav.navigate("community/$type/$id") },
                )
            }
            composable(
                route = "newCommunity/{type}",
                arguments = listOf(navArgument("type") { type = NavType.StringType }),
            ) { entry ->
                val type = entry.arguments?.getString("type") ?: return@composable
                NewCommunityScreen(
                    type = type,
                    vm = if (type == "groups") groupsVm else pagesVm,
                    onBack = { nav.popBackStack() },
                    onCreated = { id ->
                        nav.navigate("community/$type/$id") {
                            popUpTo("newCommunity/{type}") { inclusive = true }
                        }
                    },
                )
            }
            composable(
                route = "community/{type}/{id}",
                arguments = listOf(
                    navArgument("type") { type = NavType.StringType },
                    navArgument("id") { type = NavType.StringType },
                ),
            ) { entry ->
                val type = entry.arguments?.getString("type") ?: return@composable
                val id = entry.arguments?.getString("id") ?: return@composable
                val detailVm: CommunityDetailViewModel = viewModel(
                    key = "community_${type}_$id",
                    factory = CommunityDetailViewModelFactory(type, id, uid),
                )
                CommunityDetailScreen(
                    type = type,
                    vm = detailVm,
                    myUid = uid,
                    onBack = { nav.popBackStack() },
                    onNewPost = { nav.navigate("communityPost/$type/$id") },
                    onOpenComments = { postId -> nav.navigate("communityComments/$type/$id/$postId") },
                )
            }
            composable(
                route = "communityPost/{type}/{id}",
                arguments = listOf(
                    navArgument("type") { type = NavType.StringType },
                    navArgument("id") { type = NavType.StringType },
                ),
            ) { entry ->
                val type = entry.arguments?.getString("type") ?: return@composable
                val id = entry.arguments?.getString("id") ?: return@composable
                // same view model as the page behind it, so the new post shows up there
                val parentEntry = remember(entry) { nav.getBackStackEntry("community/{type}/{id}") }
                val detailVm: CommunityDetailViewModel = viewModel(
                    viewModelStoreOwner = parentEntry,
                    key = "community_${type}_$id",
                    factory = CommunityDetailViewModelFactory(type, id, uid),
                )
                NewCommunityPostScreen(vm = detailVm, onDone = { nav.popBackStack() })
            }
            composable(
                route = "communityComments/{type}/{id}/{postId}",
                arguments = listOf(
                    navArgument("type") { type = NavType.StringType },
                    navArgument("id") { type = NavType.StringType },
                    navArgument("postId") { type = NavType.StringType },
                ),
            ) { entry ->
                val type = entry.arguments?.getString("type") ?: return@composable
                val id = entry.arguments?.getString("id") ?: return@composable
                val postId = entry.arguments?.getString("postId") ?: return@composable
                val communityCommentsVm: CommentsViewModel = viewModel(
                    key = "communitycomments_${type}_${id}_$postId",
                    factory = CommentsViewModelFactory(postId, uid, "$type/$id/posts"),
                )
                CommentsScreen(
                    vm = communityCommentsVm,
                    myUid = uid,
                    onBack = { nav.popBackStack() },
                    onCountChange = {},
                )
            }

            // ---------------- events ----------------
            composable("events") {
                EventsScreen(
                    vm = eventsVm,
                    onBack = { nav.popBackStack() },
                    onCreate = { nav.navigate("newEvent") },
                    onOpen = { id -> nav.navigate("event/$id") },
                )
            }
            composable("newEvent") {
                NewEventScreen(
                    vm = eventsVm,
                    onBack = { nav.popBackStack() },
                    onCreated = { id ->
                        nav.navigate("event/$id") { popUpTo("newEvent") { inclusive = true } }
                    },
                )
            }
            composable(
                route = "event/{id}",
                arguments = listOf(navArgument("id") { type = NavType.StringType }),
            ) { entry ->
                val eventId = entry.arguments?.getString("id") ?: return@composable
                val eventVm: EventDetailViewModel = viewModel(
                    key = "event_$eventId",
                    factory = EventDetailViewModelFactory(eventId, uid),
                )
                EventDetailScreen(vm = eventVm, myUid = uid, onBack = { nav.popBackStack() })
            }
        }
    }
}
