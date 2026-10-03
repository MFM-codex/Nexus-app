package com.nexus.app.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date

private val ESTIMATE = DocumentSnapshot.ServerTimestampBehavior.ESTIMATE

private fun DocumentSnapshot.toChat(): Chat {
    val members = (get("members") as? List<*>)?.filterIsInstance<String>() ?: emptyList()

    val unread = mutableMapOf<String, Int>()
    (get("unread") as? Map<*, *>)?.forEach { (k, v) ->
        if (k is String) unread[k] = (v as? Number)?.toInt() ?: 0
    }

    val lastRead = mutableMapOf<String, Date>()
    (get("lastRead", ESTIMATE) as? Map<*, *>)?.forEach { (k, v) ->
        if (k is String && v is Timestamp) lastRead[k] = v.toDate()
    }

    return Chat(
        id = id,
        members = members,
        lastMessage = getString("lastMessage") ?: "",
        lastMessageAt = getTimestamp("lastMessageAt", ESTIMATE)?.toDate(),
        lastSenderId = getString("lastSenderId") ?: "",
        unread = unread,
        lastRead = lastRead,
    )
}

private fun DocumentSnapshot.toMessage() = Message(
    id = id,
    senderId = getString("senderId") ?: "",
    text = getString("text") ?: "",
    createdAt = getTimestamp("createdAt", ESTIMATE)?.toDate(),
)

// All Firestore work for chat.
//   chats/{chatId}                    members, lastMessage, lastMessageAt, lastSenderId, unread, lastRead
//   chats/{chatId}/messages/{id}      senderId, text, createdAt
class ChatRepository {
    private val db = FirebaseFirestore.getInstance()
    private val chats = db.collection("chats")

    fun chatId(a: String, b: String) = if (a < b) "${a}_$b" else "${b}_$a"

    // Live list of my conversations.
    fun observeChats(me: String): Flow<List<Chat>> = callbackFlow {
        val registration = chats.whereArrayContains("members", me)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snap?.documents?.map { it.toChat() } ?: emptyList())
            }
        awaitClose { registration.remove() }
    }

    // Live details of one conversation (null if nobody has written yet).
    fun observeChat(chatId: String): Flow<Chat?> = callbackFlow {
        val registration = chats.document(chatId).addSnapshotListener { snap, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            trySend(if (snap != null && snap.exists()) snap.toChat() else null)
        }
        awaitClose { registration.remove() }
    }

    // Live messages, newest first (the screen flips them so the newest is at the bottom).
    fun observeMessages(chatId: String): Flow<List<Message>> = callbackFlow {
        val registration = chats.document(chatId).collection("messages")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(100)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snap?.documents?.map { it.toMessage() } ?: emptyList())
            }
        awaitClose { registration.remove() }
    }

    suspend fun send(me: String, other: String, text: String) {
        val chatRef = chats.document(chatId(me, other))

        // First message ever? Create the conversation first.
        if (!chatRef.get().await().exists()) {
            chatRef.set(
                mapOf(
                    "members" to listOf(me, other),
                    "lastMessage" to "",
                    "lastMessageAt" to FieldValue.serverTimestamp(),
                    "lastSenderId" to "",
                    "unread" to mapOf(me to 0, other to 0),
                    "lastRead" to emptyMap<String, Any>(),
                )
            ).await()
        }

        // Save the message and update the conversation together.
        val batch = db.batch()
        batch.set(
            chatRef.collection("messages").document(),
            mapOf("senderId" to me, "text" to text, "createdAt" to FieldValue.serverTimestamp())
        )
        batch.update(
            chatRef,
            mapOf(
                "lastMessage" to text.take(100),
                "lastMessageAt" to FieldValue.serverTimestamp(),
                "lastSenderId" to me,
                "unread.$other" to FieldValue.increment(1),
            )
        )
        batch.commit().await()
    }

    // I opened the chat: clear my unread counter and remember when I read.
    suspend fun markRead(me: String, other: String) {
        chats.document(chatId(me, other)).update(
            mapOf("unread.$me" to 0, "lastRead.$me" to FieldValue.serverTimestamp())
        ).await()
    }
}
