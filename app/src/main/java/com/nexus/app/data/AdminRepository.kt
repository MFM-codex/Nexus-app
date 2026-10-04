package com.nexus.app.data

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

private val ESTIMATE = DocumentSnapshot.ServerTimestampBehavior.ESTIMATE

// Moderation.
//   admins/{uid}   exists  -> that person is an admin (you add it by hand in the Firebase console)
//   banned/{uid}   exists  -> that person is suspended
//   reports/{id}   status "open" / "resolved" / "dismissed"
class AdminRepository {
    private val db = FirebaseFirestore.getInstance()

    // Does the document collection/id exist? (used for "am I an admin?" and "am I banned?")
    fun observeExists(collection: String, id: String): Flow<Boolean> = callbackFlow {
        val registration = db.collection(collection).document(id)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snap?.exists() == true)
            }
        awaitClose { registration.remove() }
    }

    // Live list of reports nobody has handled yet.
    fun observeOpenReports(): Flow<List<Report>> = callbackFlow {
        val registration = db.collection("reports")
            .whereEqualTo("status", "open")
            .limit(100)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(
                    snap?.documents?.map { d ->
                        Report(
                            id = d.id,
                            reporterId = d.getString("reporterId") ?: "",
                            targetType = d.getString("targetType") ?: "",
                            targetId = d.getString("targetId") ?: "",
                            targetUserId = d.getString("targetUserId") ?: "",
                            reason = d.getString("reason") ?: "",
                            details = d.getString("details") ?: "",
                            createdAt = d.getTimestamp("createdAt", ESTIMATE)?.toDate(),
                        )
                    } ?: emptyList()
                )
            }
        awaitClose { registration.remove() }
    }

    suspend fun setStatus(reportId: String, status: String) {
        db.collection("reports").document(reportId).update("status", status).await()
    }

    suspend fun deletePost(postId: String) {
        db.collection("posts").document(postId).delete().await()
    }

    suspend fun deleteReel(reelId: String) {
        db.collection("reels").document(reelId).delete().await()
    }

    suspend fun ban(userId: String, adminId: String) {
        db.collection("banned").document(userId).set(
            mapOf("by" to adminId, "createdAt" to FieldValue.serverTimestamp())
        ).await()
    }
}
