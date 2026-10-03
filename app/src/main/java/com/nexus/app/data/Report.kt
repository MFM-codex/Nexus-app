package com.nexus.app.data

import java.util.Date

// A report about a user or a post. Stored at reports/{id}. Only admins can read them.
data class Report(
    val id: String,
    val reporterId: String,
    val targetType: String, // "user" or "post"
    val targetId: String,   // a user id or a post id
    val targetUserId: String, // the person being reported (or the post's author)
    val reason: String,
    val details: String,
    val createdAt: Date?,
)
