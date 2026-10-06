package com.nexus.app.data

import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

private val ESTIMATE = DocumentSnapshot.ServerTimestampBehavior.ESTIMATE

// Groups and Pages work the same way, so one class handles both.
//   groups/{id}  or  pages/{id}      ownerId, name, description, category, createdAt
//     members/{uid} (groups) or followers/{uid} (pages)    joinedAt
//     posts/{postId}                                         same as normal posts
class CommunityRepository(val type: String) {
    private val db = FirebaseFirestore.getInstance()
    private val col = db.collection(type)
    private val memberCollection = if (type == "groups") "members" else "followers"

    private fun DocumentSnapshot.toCommunity() = Community(
        id = id,
        type = type,
        name = getString("name") ?: "",
        description = getString("description") ?: "",
        category = getString("category") ?: "",
        ownerId = getString("ownerId") ?: "",
        createdAt = getTimestamp("createdAt", ESTIMATE)?.toDate(),
    )

    suspend fun list(): List<Community> =
        col.orderBy("createdAt", Query.Direction.DESCENDING).limit(50).get().await()
            .documents.map { it.toCommunity() }

    suspend fun get(id: String): Community? {
        val doc = col.document(id).get().await()
        return if (doc.exists()) doc.toCommunity() else null
    }

    // Creates it and makes the creator the first member, together. Returns the new id.
    suspend fun create(uid: String, name: String, description: String, category: String): String {
        val ref = col.document()
        val batch = db.batch()
        batch.set(
            ref,
            mapOf(
                "ownerId" to uid,
                "name" to name,
                "description" to description,
                "category" to category,
                "createdAt" to FieldValue.serverTimestamp(),
            )
        )
        batch.set(
            ref.collection(memberCollection).document(uid),
            mapOf("joinedAt" to FieldValue.serverTimestamp())
        )
        batch.commit().await()
        return ref.id
    }

    suspend fun isMember(id: String, uid: String): Boolean =
        col.document(id).collection(memberCollection).document(uid).get().await().exists()

    suspend fun join(id: String, uid: String) {
        col.document(id).collection(memberCollection).document(uid)
            .set(mapOf("joinedAt" to FieldValue.serverTimestamp())).await()
    }

    suspend fun leave(id: String, uid: String) {
        col.document(id).collection(memberCollection).document(uid).delete().await()
    }

    suspend fun memberCount(id: String): Long =
        col.document(id).collection(memberCollection).count().get(AggregateSource.SERVER).await().count

    suspend fun delete(id: String) {
        col.document(id).delete().await()
    }
}
