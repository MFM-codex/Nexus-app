package com.nexus.app.data

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class ReportRepository {
    private val reports = FirebaseFirestore.getInstance().collection("reports")

    // The id is built from who reported what, so the same person can't report the same thing twice.
    suspend fun create(
        reporter: String,
        targetType: String,
        targetId: String,
        targetUserId: String,
        reason: String,
        details: String,
    ) {
        reports.document("${reporter}_${targetType}_$targetId").set(
            mapOf(
                "reporterId" to reporter,
                "targetType" to targetType,
                "targetId" to targetId,
                "targetUserId" to targetUserId,
                "reason" to reason,
                "details" to details,
                "status" to "open",
                "createdAt" to FieldValue.serverTimestamp(),
            )
        ).await()
    }
}
