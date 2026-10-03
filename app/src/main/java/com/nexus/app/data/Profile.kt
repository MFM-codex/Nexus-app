package com.nexus.app.data

// One user's public profile. Stored in Firestore at users/{uid}.
data class Profile(
    val uid: String = "",
    val username: String = "",
    val name: String = "",
    val bio: String = "",
    val avatarUrl: String = "",
    val coverUrl: String = "",
)
