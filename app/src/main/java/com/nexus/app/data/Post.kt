package com.nexus.app.data

import com.google.firebase.firestore.DocumentSnapshot
import java.util.Date

// A post. Stored in Firestore at posts/{postId}.
data class Post(
    val id: String,
    val authorId: String,
    val text: String,
    val imageUrl: String,
    val createdAt: Date?,
    val editedAt: Date?,
    val likeCount: Int,
    val commentCount: Int,
)

// A comment. Stored at posts/{postId}/comments/{commentId}.
data class Comment(
    val id: String,
    val authorId: String,
    val text: String,
    val createdAt: Date?,
)

// What the screen needs to draw one post: the post + who wrote it + did I like it.
data class PostUi(val post: Post, val author: Profile?, val liked: Boolean)

data class CommentUi(val comment: Comment, val author: Profile?)

// One page of posts, plus a bookmark (last) so we know where the next page starts.
data class PostPage(val posts: List<Post>, val last: DocumentSnapshot?, val end: Boolean)
