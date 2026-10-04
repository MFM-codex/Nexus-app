package com.nexus.app.data

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

// Your private list of saved posts. Stored at users/{you}/saved/{postId}.
class SavedRepository {
    private val db = FirebaseFirestore.getInstance()

    private fun col(uid: String) = db.collection("users").document(uid).collection("saved")

    suspend fun isSaved(uid: String, postId: String): Boolean =
        col(uid).document(postId).get().await().exists()

    suspend fun setSaved(uid: String, postId: String, saved: Boolean) {
        val ref = col(uid).document(postId)
        if (saved) {
            ref.set(mapOf("savedAt" to FieldValue.serverTimestamp())).await()
        } else {
            ref.delete().await()
        }
    }

    // Ids of my saved posts, newest first.
    suspend fun savedIds(uid: String): List<String> =
        col(uid).orderBy("savedAt", Query.Direction.DESCENDING).limit(50).get().await()
            .documents.map { it.id }
}
