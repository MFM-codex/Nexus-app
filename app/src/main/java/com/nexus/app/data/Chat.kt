package com.nexus.app.data

import java.util.Date

// A conversation between two friends. Stored at chats/{chatId},
// where chatId = the two user ids joined with "_" (same as the friendship id).
data class Chat(
    val id: String,
    val members: List<String>,
    val lastMessage: String,
    val lastMessageAt: Date?,
    val lastSenderId: String,
    val unread: Map<String, Int>,    // how many unread messages each person has
    val lastRead: Map<String, Date>, // when each person last opened the chat
)

// One message. Stored at chats/{chatId}/messages/{messageId}.
data class Message(
    val id: String,
    val senderId: String,
    val text: String,
    val createdAt: Date?,
)

// A notification. Stored at users/{recipientId}/notifications/{id}.
// type is: like, comment, friend_request or friend_accept
data class Notification(
    val id: String,
    val actorId: String, // who did it
    val type: String,
    val postId: String,
    val read: Boolean,
    val createdAt: Date?,
)
