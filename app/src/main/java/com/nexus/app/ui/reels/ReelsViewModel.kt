package com.nexus.app.ui.reels

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.nexus.app.data.CloudinaryUploader
import com.nexus.app.data.PostRepository
import com.nexus.app.data.Profile
import com.nexus.app.data.Reel
import com.nexus.app.data.ReelRepository
import com.nexus.app.data.ReportRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// A reel plus who made it and whether I liked it.
data class ReelUi(val reel: Reel, val author: Profile?, val liked: Boolean)

data class ReelsState(
    val items: List<ReelUi> = emptyList(),
    val refreshing: Boolean = false,
    val loadedOnce: Boolean = false,
    val loadFailed: Boolean = false,
    val endReached: Boolean = false,
    val error: String? = null, // also used for small info messages
)

class ReelsViewModel(private val uid: String) : ViewModel() {
    private val repo = ReelRepository()
    private val social = PostRepository("reels") // likes, comments and author lookups
    private val reportRepo = ReportRepository()

    private val _state = MutableStateFlow(ReelsState())
    val state: StateFlow<ReelsState> = _state

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy

    // 0.0 to 1.0 while a video is uploading, otherwise null
    private val _progress = MutableStateFlow<Float?>(null)
    val progress: StateFlow<Float?> = _progress

    private var cursor: Timestamp? = null
    private var loading = false

    init {
        refresh()
    }

    fun refresh() {
        if (loading) return
        loading = true
        viewModelScope.launch {
            _state.update { it.copy(refreshing = true, error = null) }
            try {
                val page = repo.loadPage(null)
                cursor = page.last
                val items = toUi(page.reels)
                _state.update {
                    it.copy(items = items, endReached = page.end, refreshing = false, loadedOnce = true, loadFailed = false)
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(refreshing = false, loadedOnce = true, loadFailed = true, error = e.message ?: "Could not load reels.")
                }
            }
            loading = false
        }
    }

    // Called when you swipe near the last loaded reel.
    fun loadMore() {
        if (loading || _state.value.endReached || cursor == null) return
        loading = true
        viewModelScope.launch {
            try {
                val page = repo.loadPage(cursor)
                cursor = page.last ?: cursor
                val more = toUi(page.reels)
                _state.update {
                    val known = it.items.map { item -> item.reel.id }.toSet()
                    it.copy(
                        items = it.items + more.filter { item -> item.reel.id !in known },
                        endReached = page.end,
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message ?: "Could not load more reels.") }
            }
            loading = false
        }
    }

    private suspend fun toUi(reels: List<Reel>): List<ReelUi> = coroutineScope {
        val authorsJob = async { social.authors(reels.map { it.authorId }.toSet()) }
        val likedJobs = reels.map { reel -> async { social.isLiked(reel.id, uid) } }
        val authors = authorsJob.await()
        val liked = likedJobs.awaitAll()
        reels.mapIndexed { i, reel -> ReelUi(reel, authors[reel.authorId], liked[i]) }
    }

    // The heart changes instantly; if saving fails we undo it.
    fun toggleLike(reelId: String) {
        val current = _state.value.items.firstOrNull { it.reel.id == reelId } ?: return
        val nowLiked = !current.liked
        setLocalLike(reelId, nowLiked)
        viewModelScope.launch {
            try {
                social.setLike(reelId, uid, nowLiked)
            } catch (e: Exception) {
                setLocalLike(reelId, !nowLiked)
                _state.update { it.copy(error = e.message ?: "Could not update like.") }
            }
        }
    }

    private fun setLocalLike(reelId: String, liked: Boolean) {
        val delta = if (liked) 1 else -1
        _state.update { s ->
            s.copy(items = s.items.map {
                if (it.reel.id == reelId) {
                    it.copy(
                        liked = liked,
                        reel = it.reel.copy(likeCount = (it.reel.likeCount + delta).coerceAtLeast(0)),
                    )
                } else it
            })
        }
    }

    // Called by the comments screen so the number on the reel stays correct.
    fun adjustCommentCount(reelId: String, delta: Int) {
        _state.update { s ->
            s.copy(items = s.items.map {
                if (it.reel.id == reelId) {
                    it.copy(reel = it.reel.copy(commentCount = (it.reel.commentCount + delta).coerceAtLeast(0)))
                } else it
            })
        }
    }

    fun deleteReel(reelId: String) {
        viewModelScope.launch {
            try {
                repo.delete(reelId)
                _state.update { s -> s.copy(items = s.items.filter { it.reel.id != reelId }) }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message ?: "Could not delete the reel.") }
            }
        }
    }

    fun reportReel(reel: Reel, reason: String, details: String) {
        viewModelScope.launch {
            val message = try {
                reportRepo.create(uid, "reel", reel.id, reel.authorId, reason, details)
                "Report sent. Thank you."
            } catch (e: Exception) {
                "You already reported this reel, or reporting isn't allowed right now."
            }
            _state.update { it.copy(error = message) }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    // Uploads the video (with progress), then saves the reel. onResult(null) = success.
    fun uploadReel(resolver: ContentResolver, uri: Uri, caption: String, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            _busy.value = true
            _progress.value = 0f
            val message = try {
                val url = CloudinaryUploader.uploadVideo(resolver, uri) { fraction -> _progress.value = fraction }
                repo.create(uid, url, caption)
                null
            } catch (e: Exception) {
                if (e.message?.contains("PERMISSION_DENIED") == true) {
                    "Couldn't post. If you posted a reel a moment ago, wait 30 seconds and try again."
                } else {
                    e.message ?: "Could not post the reel."
                }
            }
            _busy.value = false
            _progress.value = null
            onResult(message)
            if (message == null) refresh()
        }
    }
}

class ReelsViewModelFactory(private val uid: String) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = ReelsViewModel(uid) as T
}
