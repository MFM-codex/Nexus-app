package com.nexus.app.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Date

private val ESTIMATE = DocumentSnapshot.ServerTimestampBehavior.ESTIMATE

private fun DocumentSnapshot.toEvent() = Event(
    id = id,
    hostId = getString("hostId") ?: "",
    title = getString("title") ?: "",
    description = getString("description") ?: "",
    location = getString("location") ?: "",
    startAt = getTimestamp("startAt", ESTIMATE)?.toDate(),
    createdAt = getTimestamp("createdAt", ESTIMATE)?.toDate(),
)

class EventRepository {
    private val events = FirebaseFirestore.getInstance().collection("events")

    // Events that haven't started yet, soonest first.
    suspend fun upcoming(): List<Event> =
        events.whereGreaterThan("startAt", Timestamp.now()).orderBy("startAt").limit(50).get().await()
            .documents.map { it.toEvent() }

    suspend fun get(id: String): Event? {
        val doc = events.document(id).get().await()
        return if (doc.exists()) doc.toEvent() else null
    }

    suspend fun create(host: String, title: String, description: String, location: String, startAt: Date): String {
        val ref = events.document()
        ref.set(
            mapOf(
                "hostId" to host,
                "title" to title,
                "description" to description,
                "location" to location,
                "startAt" to Timestamp(startAt),
                "createdAt" to FieldValue.serverTimestamp(),
            )
        ).await()
        return ref.id
    }

    suspend fun delete(id: String) {
        events.document(id).delete().await()
    }

    // "going", "interested" or null
    suspend fun myRsvp(id: String, uid: String): String? =
        events.document(id).collection("rsvps").document(uid).get().await().getString("status")

    suspend fun setRsvp(id: String, uid: String, status: String?) {
        val ref = events.document(id).collection("rsvps").document(uid)
        if (status == null) {
            ref.delete().await()
        } else {
            ref.set(mapOf("status" to status, "updatedAt" to FieldValue.serverTimestamp())).await()
        }
    }

    suspend fun count(id: String, status: String): Long =
        events.document(id).collection("rsvps").whereEqualTo("status", status)
            .count().get(AggregateSource.SERVER).await().count
}
