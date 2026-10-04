package com.nexus.app.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

private const val REEL_PAGE_SIZE = 8
private val ESTIMATE = DocumentSnapshot.ServerTimestampBehavior.ESTIMATE

private fun DocumentSnapshot.toReel() = Reel(
    id = id,
    authorId = getString("authorId") ?: "",
    videoUrl = getString("videoUrl") ?: "",
    caption = getString("caption") ?: "",
    createdAt = getTimestamp("createdAt", ESTIMATE)?.toDate(),
    likeCount = (getLong("likeCount") ?: 0L).toInt(),
    commentCount = (getLong("commentCount") ?: 0L).toInt(),
)

// Firestore work for reels. Likes and comments reuse PostRepository("reels").
//   reels/{reelId}   authorId, videoUrl, caption, createdAt, likeCount, commentCount
class ReelRepository {
    private val db = FirebaseFirestore.getInstance()
    private val reels = db.collection("reels")

    // Newest first. Pass the time of the last reel you already have to get the next page.
    suspend fun loadPage(before: Timestamp?): ReelPage {
        var query = reels.orderBy("createdAt", Query.Direction.DESCENDING)
        if (before != null) query = query.startAfter(before)
        val docs = query.limit(REEL_PAGE_SIZE.toLong()).get().await().documents
        return ReelPage(
            reels = docs.map { it.toReel() },
            last = docs.lastOrNull()?.getTimestamp("createdAt", ESTIMATE),
            end = docs.size < REEL_PAGE_SIZE,
        )
    }

    suspend fun get(reelId: String): Reel? {
        val doc = reels.document(reelId).get().await()
        return if (doc.exists()) doc.toReel() else null
    }

    suspend fun create(uid: String, videoUrl: String, caption: String) {
        val batch = db.batch()
        batch.set(
            reels.document(),
            mapOf(
                "authorId" to uid,
                "videoUrl" to videoUrl,
                "caption" to caption,
                "createdAt" to FieldValue.serverTimestamp(),
                "likeCount" to 0,
                "commentCount" to 0,
            )
        )
        // Rate limit: the security rules only accept a reel if this time moves forward by 30+ seconds.
        batch.set(
            db.collection("reelLimits").document(uid),
            mapOf("lastReelAt" to FieldValue.serverTimestamp())
        )
        batch.commit().await()
    }

    suspend fun delete(reelId: String) {
        reels.document(reelId).delete().await()
    }
}
