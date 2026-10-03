package com.nexus.app.data

import com.google.firebase.firestore.DocumentSnapshot

// Turns a Firestore "users" document into a Profile.
fun DocumentSnapshot.toProfile() = Profile(
    uid = id,
    username = getString("username") ?: "",
    name = getString("name") ?: "",
    bio = getString("bio") ?: "",
    avatarUrl = getString("avatarUrl") ?: "",
    coverUrl = getString("coverUrl") ?: "",
)
