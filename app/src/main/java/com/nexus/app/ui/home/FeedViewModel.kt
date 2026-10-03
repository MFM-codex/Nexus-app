package com.nexus.app.ui.home

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.nexus.app.data.CloudinaryUploader
import com.nexus.app.data.FriendRepository
import com.nexus.app.data.PostRepository
import com.nexus.app.data.PostUi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Everything the feed screen needs to draw itself.
data class FeedState(
    val items: List<PostUi> = emptyList(),
    val refreshing: Boolean = false,
    val loadingMore: Boolean = false,
    val endReached: Boolean = false,
    val loadedOnce: Boolean = false,
    val error: String? = null,
)

class FeedViewModel(private val uid: String) : ViewModel() {
    private val repo = PostRepository()
    private val friendRepo = FriendRepository()

    private val _state = MutableStateFlow(FeedState())
    val state: StateFlow<FeedState> = _state

    // True while a photo is uploading or a post is being saved.
    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy

    private var cursor: Timestamp? = null // time of the last post loaded
    private var authorIds: List<String> = listOf(uid) // whose posts we show: me + my friends
    private var loading = false

    init {
        refresh()
    }

    // Reload from the top (also used by pull-to-refresh).
    fun refresh() {
        if (loading) return
        loading = true
        viewModelScope.launch {
            _state.update { it.copy(refreshing = true, error = null) }
            try {
                authorIds = listOf(uid) + friendRepo.friendIds(uid)
                val page = repo.loadPage(authorIds, null)
                cursor = page.last
                val items = toUi(page.posts)
                _state.update {
                    it.copy(items = items, endReached = page.end, refreshing = false, loadedOnce = true)
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(refreshing = false, loadedOnce = true, error = e.message ?: "Could not load posts.")
                }
            }
            loading = false
        }
    }

    // Load the next page (called when you scroll near the bottom).
    fun loadMore() {
        if (loading || _state.value.endReached || cursor == null) return
        loading = true
        viewModelScope.launch {
            _state.update { it.copy(loadingMore = true) }
            try {
                val page = repo.loadPage(authorIds, cursor)
                cursor = page.last ?: cursor
                val more = toUi(page.posts)
                _state.update {
                    // skip anything we already have, in case a post shifted between pages
                    val known = it.items.map { item -> item.post.id }.toSet()
                    it.copy(
                        items = it.items + more.filter { item -> item.post.id !in known },
                        endReached = page.end,
                        loadingMore = false,
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(loadingMore = false, error = e.message ?: "Could not load more.") }
            }
            loading = false
        }
    }

    // Attach each post's author and whether I liked it.
    private suspend fun toUi(posts: List<com.nexus.app.data.Post>): List<PostUi> = coroutineScope {
        val authorsJob = async { repo.authors(posts.map { it.authorId }.toSet()) }
        val likedJobs = posts.map { post -> async { repo.isLiked(post.id, uid) } }
        val authors = authorsJob.await()
        val liked = likedJobs.awaitAll()
        posts.mapIndexed { i, post -> PostUi(post, authors[post.authorId], liked[i]) }
    }

    // Like/unlike. The heart changes instantly; if saving fails we undo it.
    fun toggleLike(postId: String) {
        val current = _state.value.items.firstOrNull { it.post.id == postId } ?: return
        val nowLiked = !current.liked
        setLocalLike(postId, nowLiked)
        viewModelScope.launch {
            try {
                repo.setLike(postId, uid, nowLiked)
            } catch (e: Exception) {
                setLocalLike(postId, !nowLiked)
                _state.update { it.copy(error = e.message ?: "Could not update like.") }
            }
        }
    }

    private fun setLocalLike(postId: String, liked: Boolean) {
        val delta = if (liked) 1 else -1
        _state.update { s ->
            s.copy(items = s.items.map {
                if (it.post.id == postId) {
                    it.copy(
                        liked = liked,
                        post = it.post.copy(likeCount = (it.post.likeCount + delta).coerceAtLeast(0)),
                    )
                } else it
            })
        }
    }

    fun editPost(postId: String, text: String) {
        viewModelScope.launch {
            try {
                repo.editPost(postId, text)
                _state.update { s ->
                    s.copy(items = s.items.map {
                        if (it.post.id == postId) {
                            it.copy(post = it.post.copy(text = text, editedAt = java.util.Date()))
                        } else it
                    })
                }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message ?: "Could not edit post.") }
            }
        }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            try {
                repo.deletePost(postId)
                _state.update { s -> s.copy(items = s.items.filter { it.post.id != postId }) }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message ?: "Could not delete post.") }
            }
        }
    }

    // Called by the comments screen so the number on the feed stays correct.
    fun adjustCommentCount(postId: String, delta: Int) {
        _state.update { s ->
            s.copy(items = s.items.map {
                if (it.post.id == postId) {
                    it.copy(post = it.post.copy(commentCount = (it.post.commentCount + delta).coerceAtLeast(0)))
                } else it
            })
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    fun uploadImage(resolver: ContentResolver, uri: Uri, onResult: (Result<String>) -> Unit) {
        viewModelScope.launch {
            _busy.value = true
            val result = try {
                Result.success(CloudinaryUploader.upload(resolver, uri))
            } catch (e: Exception) {
                Result.failure(e)
            }
            _busy.value = false
            onResult(result)
        }
    }

    // onResult(null) = success, otherwise an error message.
    fun createPost(text: String, imageUrl: String, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            _busy.value = true
            val message = try {
                repo.createPost(uid, text, imageUrl)
                null
            } catch (e: Exception) {
                if (e.message?.contains("PERMISSION_DENIED") == true) {
                    "Not allowed. Check that your Firestore rules are published."
                } else {
                    e.message ?: "Could not post."
                }
            }
            _busy.value = false
            onResult(message)
            if (message == null) refresh()
        }
    }
}

class FeedViewModelFactory(private val uid: String) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = FeedViewModel(uid) as T
}
