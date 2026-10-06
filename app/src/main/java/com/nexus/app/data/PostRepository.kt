package com.nexus.app.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.concurrent.ConcurrentHashMap

private const val PAGE_SIZE = 10

// Just-written documents have no server time yet; ask Firestore to guess it.
private val ESTIMATE = DocumentSnapshot.ServerTimestampBehavior.ESTIMATE

private fun DocumentSnapshot.toPost() = Post(
    id = id,
    authorId = getString("authorId") ?: "",
    text = getString("text") ?: "",
    imageUrl = getString("imageUrl") ?: "",
    createdAt = getTimestamp("createdAt", ESTIMATE)?.toDate(),
    editedAt = getTimestamp("editedAt", ESTIMATE)?.toDate(),
    likeCount = (getLong("likeCount") ?: 0L).toInt(),
    commentCount = (getLong("commentCount") ?: 0L).toInt(),
)

private fun DocumentSnapshot.toComment() = Comment(
    id = id,
    authorId = getString("authorId") ?: "",
    text = getString("text") ?: "",
    createdAt = getTimestamp("createdAt", ESTIMATE)?.toDate(),
)

// All Firestore work for posts, likes and comments.
//   posts/{postId}                    authorId, text, imageUrl, createdAt, likeCount, commentCount
//   posts/{postId}/likes/{uid}        createdAt   (doc id = who liked, so nobody can like twice)
//   posts/{postId}/comments/{id}      authorId, text, createdAt
class PostRepository(collection: String = "posts") {
    private val db = FirebaseFirestore.getInstance()
    private val posts = db.collection(collection)
    private val users = db.collection("users")

    // Remember authors we already loaded so we don't fetch them again.
    private val authorCache = ConcurrentHashMap<String, Profile>()

    // Newest posts first, from the given authors (you + your friends).
    // `before` = time of the last post already loaded (null for the first page).
    suspend fun loadPage(authorIds: List<String>, before: Timestamp?): PostPage = coroutineScope {
        // One query per author: Firestore's security rules need the author fixed in each query.
        val jobs = authorIds.map { author ->
            async {
                var query = posts.whereEqualTo("authorId", author)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                if (before != null) query = query.startAfter(before)
                query.limit((PAGE_SIZE + 1).toLong()).get().await().documents
            }
        }
        val merged = jobs.awaitAll().flatten()
            .sortedByDescending { it.getTimestamp("createdAt", ESTIMATE) }
        val page = merged.take(PAGE_SIZE)
        PostPage(
            posts = page.map { it.toPost() },
            last = page.lastOrNull()?.getTimestamp("createdAt", ESTIMATE),
            end = merged.size <= PAGE_SIZE,
        )
    }

    // All posts in this collection, newest first (used inside groups and pages).
    suspend fun loadAll(before: Timestamp?): PostPage {
        var query = posts.orderBy("createdAt", Query.Direction.DESCENDING)
        if (before != null) query = query.startAfter(before)
        val docs = query.limit(PAGE_SIZE.toLong()).get().await().documents
        return PostPage(
            posts = docs.map { it.toPost() },
            last = docs.lastOrNull()?.getTimestamp("createdAt", ESTIMATE),
            end = docs.size < PAGE_SIZE,
        )
    }

    // Look up the profiles of the given users (name, avatar...), 10 at a time.
    suspend fun authors(uids: Set<String>): Map<String, Profile> {
        val missing = uids.filter { it.isNotEmpty() && !authorCache.containsKey(it) }
        missing.chunked(10).forEach { chunk ->
            val snap = users.whereIn(FieldPath.documentId(), chunk).get().await()
            snap.documents.forEach { d ->
                authorCache[d.id] = Profile(
                    uid = d.id,
                    username = d.getString("username") ?: "",
                    name = d.getString("name") ?: "",
                    bio = d.getString("bio") ?: "",
                    avatarUrl = d.getString("avatarUrl") ?: "",
                    coverUrl = d.getString("coverUrl") ?: "",
                )
            }
        }
        return uids.mapNotNull { id -> authorCache[id]?.let { id to it } }.toMap()
    }

    // Load one post (used by the admin panel).
    suspend fun getPost(postId: String): Post? {
        val doc = posts.document(postId).get().await()
        return if (doc.exists()) doc.toPost() else null
    }

    // Who wrote this post? (used to notify them about comments)
    suspend fun authorOf(postId: String): String? =
        posts.document(postId).get().await().getString("authorId")

    suspend fun createPost(uid: String, text: String, imageUrl: String) {
        val batch = db.batch()
        batch.set(
            posts.document(),
            mapOf(
                "authorId" to uid,
                "text" to text,
                "imageUrl" to imageUrl,
                "createdAt" to FieldValue.serverTimestamp(),
                "likeCount" to 0,
                "commentCount" to 0,
            )
        )
        // Rate limit: the security rules only accept a post if this time stamp
        // moves forward by at least 10 seconds since your last post.
        batch.set(
            db.collection("rateLimits").document(uid),
            mapOf("lastPostAt" to FieldValue.serverTimestamp())
        )
        batch.commit().await()
    }

    suspend fun editPost(postId: String, text: String) {
        posts.document(postId).update(
            mapOf("text" to text, "editedAt" to FieldValue.serverTimestamp())
        ).await()
    }

    suspend fun deletePost(postId: String) {
        posts.document(postId).delete().await()
    }

    suspend fun isLiked(postId: String, uid: String): Boolean =
        posts.document(postId).collection("likes").document(uid).get().await().exists()

    // A batch = several writes that succeed or fail together.
    suspend fun setLike(postId: String, uid: String, liked: Boolean) {
        val postRef = posts.document(postId)
        val likeRef = postRef.collection("likes").document(uid)
        val batch = db.batch()
        if (liked) {
            batch.set(likeRef, mapOf("createdAt" to FieldValue.serverTimestamp()))
            batch.update(postRef, "likeCount", FieldValue.increment(1))
        } else {
            batch.delete(likeRef)
            batch.update(postRef, "likeCount", FieldValue.increment(-1))
        }
        batch.commit().await()
    }

    // Live list of comments for one post (updates by itself when someone comments).
    fun observeComments(postId: String): Flow<List<Comment>> = callbackFlow {
        val registration = posts.document(postId).collection("comments")
            .orderBy("createdAt")
            .limit(200)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snap?.documents?.map { it.toComment() } ?: emptyList())
            }
        awaitClose { registration.remove() }
    }

    suspend fun addComment(postId: String, uid: String, text: String) {
        val postRef = posts.document(postId)
        val batch = db.batch()
        batch.set(
            postRef.collection("comments").document(),
            mapOf("authorId" to uid, "text" to text, "createdAt" to FieldValue.serverTimestamp())
        )
        batch.update(postRef, "commentCount", FieldValue.increment(1))
        batch.commit().await()
    }

    suspend fun deleteComment(postId: String, commentId: String) {
        val postRef = posts.document(postId)
        val batch = db.batch()
        batch.delete(postRef.collection("comments").document(commentId))
        batch.update(postRef, "commentCount", FieldValue.increment(-1))
        batch.commit().await()
    }
}
