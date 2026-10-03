package com.nexus.app.data

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

private val ESTIMATE = DocumentSnapshot.ServerTimestampBehavior.ESTIMATE

// Notifications live inside each person's own user document:
//   users/{userId}/notifications/{id}   actorId, type, postId, read, createdAt
class NotificationRepository {
    private val db = FirebaseFirestore.getInstance()

    private fun col(userId: String) =
        db.collection("users").document(userId).collection("notifications")

    // Live list of my latest 50 notifications.
    fun observe(me: String): Flow<List<Notification>> = callbackFlow {
        val registration = col(me)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(
                    snap?.documents?.map { d ->
                        Notification(
                            id = d.id,
                            actorId = d.getString("actorId") ?: "",
                            type = d.getString("type") ?: "",
                            postId = d.getString("postId") ?: "",
                            read = d.getBoolean("read") ?: false,
                            createdAt = d.getTimestamp("createdAt", ESTIMATE)?.toDate(),
                        )
                    } ?: emptyList()
                )
            }
        awaitClose { registration.remove() }
    }

    // Tell `recipient` that `actor` did something. Pass `id` to make sure it's only sent once.
    suspend fun create(recipient: String, actor: String, type: String, postId: String = "", id: String? = null) {
        val ref = if (id != null) col(recipient).document(id) else col(recipient).document()
        ref.set(
            mapOf(
                "actorId" to actor,
                "type" to type,
                "postId" to postId,
                "read" to false,
                "createdAt" to FieldValue.serverTimestamp(),
            )
        ).await()
    }

    suspend fun markRead(me: String, ids: List<String>) {
        if (ids.isEmpty()) return
        val batch = db.batch()
        ids.forEach { batch.update(col(me).document(it), "read", true) }
        batch.commit().await()
    }
}
