package com.nexus.app.data

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

private val ESTIMATE = DocumentSnapshot.ServerTimestampBehavior.ESTIMATE

private fun DocumentSnapshot.toListing() = Listing(
    id = id,
    sellerId = getString("sellerId") ?: "",
    title = getString("title") ?: "",
    price = getLong("price") ?: 0L,
    description = getString("description") ?: "",
    category = getString("category") ?: "Other",
    location = getString("location") ?: "",
    contact = getString("contact") ?: "",
    imageUrl = getString("imageUrl") ?: "",
    sold = getBoolean("sold") ?: false,
    createdAt = getTimestamp("createdAt", ESTIMATE)?.toDate(),
)

// Marketplace listings.
class ListingRepository {
    private val listings = FirebaseFirestore.getInstance().collection("listings")

    // The 60 newest listings. Category and search filters happen on the phone.
    suspend fun latest(): List<Listing> =
        listings.orderBy("createdAt", Query.Direction.DESCENDING).limit(60).get().await()
            .documents.map { it.toListing() }

    suspend fun get(id: String): Listing? {
        val doc = listings.document(id).get().await()
        return if (doc.exists()) doc.toListing() else null
    }

    suspend fun create(
        uid: String, title: String, price: Long, description: String,
        category: String, location: String, contact: String, imageUrl: String,
    ) {
        listings.add(
            mapOf(
                "sellerId" to uid,
                "title" to title,
                "price" to price,
                "description" to description,
                "category" to category,
                "location" to location,
                "contact" to contact,
                "imageUrl" to imageUrl,
                "sold" to false,
                "createdAt" to FieldValue.serverTimestamp(),
            )
        ).await()
    }

    suspend fun setSold(id: String, sold: Boolean) {
        listings.document(id).update("sold", sold).await()
    }

    suspend fun delete(id: String) {
        listings.document(id).delete().await()
    }
}
