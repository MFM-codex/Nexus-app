package com.nexus.app.data

import java.util.Date

// A group or a page. type is the Firestore collection: "groups" or "pages".
data class Community(
    val id: String,
    val type: String,
    val name: String,
    val description: String,
    val category: String,
    val ownerId: String,
    val createdAt: Date?,
)
