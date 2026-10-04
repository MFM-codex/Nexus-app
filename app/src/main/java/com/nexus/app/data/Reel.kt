package com.nexus.app.data

import com.google.firebase.Timestamp
import java.util.Date

// A short video. Stored in Firestore at reels/{reelId}.
data class Reel(
    val id: String,
    val authorId: String,
    val videoUrl: String,
    val caption: String,
    val createdAt: Date?,
    val likeCount: Int,
    val commentCount: Int,
) {
    // A still picture of the first frame, made by Cloudinary (used by the admin panel).
    val thumbnailUrl: String
        get() = videoUrl.replace("/upload/", "/upload/so_0,w_480,c_limit/").substringBeforeLast('.') + ".jpg"
}

data class ReelPage(val reels: List<Reel>, val last: Timestamp?, val end: Boolean)
