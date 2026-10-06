package com.nexus.app.data

import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

// Firestore work for friends.
//   friendships/{pairId}   requesterId, addresseeId, members [a, b], status, createdAt
// Decline, cancel and unfriend all simply delete the document.
class FriendRepository {
    private val db = FirebaseFirestore.getInstance()
    private val friendships = db.collection("friendships")
    private val users = db.collection("users")

    // Same two people always give the same id, so there is never a duplicate.
    fun pairId(a: String, b: String) = if (a < b) "${a}_$b" else "${b}_$a"

    // Live list of every friendship and request that involves me.
    fun observe(me: String): Flow<List<Friendship>> = callbackFlow {
        val registration = friendships.whereArrayContains("members", me)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(
                    snap?.documents?.map { d ->
                        Friendship(
                            id = d.id,
                            requesterId = d.getString("requesterId") ?: "",
                            addresseeId = d.getString("addresseeId") ?: "",
                            status = d.getString("status") ?: "pending",
                        )
                    } ?: emptyList()
                )
            }
        awaitClose { registration.remove() }
    }

    // The ids of my accepted friends (used to build my feed).
    suspend fun friendIds(me: String): List<String> =
        friendships.whereArrayContains("members", me).get().await().documents
            .filter { it.getString("status") == "accepted" }
            .map { d ->
                val requester = d.getString("requesterId") ?: ""
                if (requester == me) d.getString("addresseeId") ?: "" else requester
            }
            .filter { it.isNotEmpty() }

    suspend fun sendRequest(me: String, other: String) {
        friendships.document(pairId(me, other)).set(
            mapOf(
                "requesterId" to me,
                "addresseeId" to other,
                "members" to listOf(me, other),
                "status" to "pending",
                "createdAt" to FieldValue.serverTimestamp(),
            )
        ).await()
    }

    suspend fun accept(me: String, other: String) {
        friendships.document(pairId(me, other)).update("status", "accepted").await()
    }

    suspend fun remove(me: String, other: String) {
        friendships.document(pairId(me, other)).delete().await()
    }

    // Live list of the people I blocked. Stored at users/{me}/blocked/{theirId}.
    fun observeBlocked(me: String): Flow<Set<String>> = callbackFlow {
        val registration = users.document(me).collection("blocked")
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snap?.documents?.map { it.id }?.toSet() ?: emptySet())
            }
        awaitClose { registration.remove() }
    }

    // Blocking also removes any friendship or request between us.
    suspend fun block(me: String, other: String, hadRelation: Boolean) {
        val batch = db.batch()
        batch.set(
            users.document(me).collection("blocked").document(other),
            mapOf("createdAt" to FieldValue.serverTimestamp())
        )
        if (hadRelation) batch.delete(friendships.document(pairId(me, other)))
        batch.commit().await()
    }

    suspend fun unblock(me: String, other: String) {
        users.document(me).collection("blocked").document(other).delete().await()
    }

    // "People you may know": some users who are not me, friends, pending or blocked.
    suspend fun suggestions(exclude: Set<String>): List<Profile> =
        users.limit(60).get().await().documents
            .map { it.toProfile() }
            .filter { it.uid !in exclude }
            .take(20)

    // Find people whose username starts with the text typed.
    suspend fun searchUsers(prefix: String): List<Profile> =
        users.orderBy("username")
            .startAt(prefix)
            .endAt(prefix + "\uf8ff")
            .limit(20)
            .get().await()
            .documents.map { it.toProfile() }

    suspend fun getProfile(uid: String): Profile? {
        val doc = users.document(uid).get().await()
        return if (doc.exists()) doc.toProfile() else null
    }

    // Load several profiles at once (10 at a time).
    suspend fun profiles(uids: Set<String>): Map<String, Profile> {
        val result = mutableMapOf<String, Profile>()
        uids.filter { it.isNotEmpty() }.chunked(10).forEach { chunk ->
            val snap = users.whereIn(FieldPath.documentId(), chunk).get().await()
            snap.documents.forEach { result[it.id] = it.toProfile() }
        }
        return result
    }
}
