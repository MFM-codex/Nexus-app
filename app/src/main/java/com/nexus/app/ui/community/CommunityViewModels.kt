package com.nexus.app.ui.community

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.nexus.app.data.CloudinaryUploader
import com.nexus.app.data.Community
import com.nexus.app.data.CommunityRepository
import com.nexus.app.data.Post
import com.nexus.app.data.PostRepository
import com.nexus.app.data.PostUi
import com.nexus.app.data.ReportRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// One line in the list of groups or pages.
data class CommunityUi(val community: Community, val member: Boolean)

// The list of groups (type = "groups") or pages (type = "pages").
class CommunitiesViewModel(private val type: String, private val uid: String) : ViewModel() {
    private val repo = CommunityRepository(type)

    // null = still loading
    private val _items = MutableStateFlow<List<CommunityUi>?>(null)
    val items: StateFlow<List<CommunityUi>?> = _items

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            try {
                val list = repo.list()
                val flags = list.map { c ->
                    async {
                        try {
                            repo.isMember(c.id, uid)
                        } catch (e: Exception) {
                            false
                        }
                    }
                }.awaitAll()
                _items.value = list.mapIndexed { i, c -> CommunityUi(c, flags[i]) }
            } catch (e: Exception) {
                _message.value = e.message ?: "Could not load the list."
                if (_items.value == null) _items.value = emptyList()
            }
        }
    }

    // Join/leave (or follow/unfollow)
    fun toggle(item: CommunityUi) {
        viewModelScope.launch {
            try {
                if (item.member) repo.leave(item.community.id, uid) else repo.join(item.community.id, uid)
                _items.value = _items.value?.map {
                    if (it.community.id == item.community.id) it.copy(member = !item.member) else it
                }
            } catch (e: Exception) {
                _message.value = e.message ?: "That didn't work."
            }
        }
    }

    // onResult(newId, null) on success, or (null, errorMessage)
    fun create(name: String, description: String, category: String, onResult: (String?, String?) -> Unit) {
        viewModelScope.launch {
            _busy.value = true
            try {
                val id = repo.create(uid, name, description, category)
                _busy.value = false
                onResult(id, null)
                load()
            } catch (e: Exception) {
                _busy.value = false
                onResult(null, e.message ?: "Could not create it.")
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}

class CommunitiesViewModelFactory(private val type: String, private val uid: String) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = CommunitiesViewModel(type, uid) as T
}

data class CommunityDetailState(
    val community: Community? = null,
    val loaded: Boolean = false,
    val member: Boolean = false,
    val memberCount: Long = 0,
    val items: List<PostUi> = emptyList(),
    val postsLoaded: Boolean = false,
    val endReached: Boolean = true,
    val busy: Boolean = false,
    val deleted: Boolean = false,
    val message: String? = null,
)

// One group or page: its details, membership and posts.
class CommunityDetailViewModel(
    private val type: String,
    private val id: String,
    private val uid: String,
) : ViewModel() {
    private val repo = CommunityRepository(type)
    private val posts = PostRepository("$type/$id/posts")
    private val reportRepo = ReportRepository()

    private val _state = MutableStateFlow(CommunityDetailState())
    val state: StateFlow<CommunityDetailState> = _state

    private var cursor: Timestamp? = null
    private var loading = false

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            try {
                val community = repo.get(id)
                if (community == null) {
                    _state.update { it.copy(loaded = true, community = null) }
                    return@launch
                }
                val member = repo.isMember(id, uid)
                val count = try {
                    repo.memberCount(id)
                } catch (e: Exception) {
                    0L
                }
                _state.update { it.copy(community = community, member = member, memberCount = count, loaded = true) }
                loadPosts(member || type == "pages")
            } catch (e: Exception) {
                _state.update { it.copy(loaded = true, message = e.message ?: "Could not load this page.") }
            }
        }
    }

    // Reload the posts (used when coming back from a comments screen).
    fun refreshPosts() {
        val s = _state.value
        if (s.community == null) return
        viewModelScope.launch { loadPosts(s.member || type == "pages") }
    }

    // Only members can read a group's posts; everyone can read a page's posts.
    private suspend fun loadPosts(allowed: Boolean) {
        if (loading) return
        loading = true
        try {
            if (!allowed) {
                _state.update { it.copy(items = emptyList(), postsLoaded = true) }
            } else {
                val page = posts.loadAll(null)
                cursor = page.last
                val items = toUi(page.posts)
                _state.update { it.copy(items = items, postsLoaded = true, endReached = page.end) }
            }
        } catch (e: Exception) {
            _state.update { it.copy(postsLoaded = true, message = e.message ?: "Could not load posts.") }
        }
        loading = false
    }

    fun loadMore() {
        if (loading || _state.value.endReached || cursor == null) return
        loading = true
        viewModelScope.launch {
            try {
                val page = posts.loadAll(cursor)
                cursor = page.last ?: cursor
                val more = toUi(page.posts)
                _state.update {
                    val known = it.items.map { item -> item.post.id }.toSet()
                    it.copy(items = it.items + more.filter { item -> item.post.id !in known }, endReached = page.end)
                }
            } catch (e: Exception) {
                _state.update { it.copy(message = e.message ?: "Could not load more.") }
            }
            loading = false
        }
    }

    private suspend fun toUi(list: List<Post>): List<PostUi> = coroutineScope {
        val authorsJob = async { posts.authors(list.map { it.authorId }.toSet()) }
        val likedJobs = list.map { post -> async { posts.isLiked(post.id, uid) } }
        val authors = authorsJob.await()
        val liked = likedJobs.awaitAll()
        list.mapIndexed { i, post -> PostUi(post, authors[post.authorId], liked[i]) }
    }

    fun toggleMembership() {
        val s = _state.value
        viewModelScope.launch {
            try {
                if (s.member) repo.leave(id, uid) else repo.join(id, uid)
                load()
            } catch (e: Exception) {
                _state.update { it.copy(message = e.message ?: "That didn't work.") }
            }
        }
    }

    fun toggleLike(postId: String) {
        val current = _state.value.items.firstOrNull { it.post.id == postId } ?: return
        val nowLiked = !current.liked
        setLocalLike(postId, nowLiked)
        viewModelScope.launch {
            try {
                posts.setLike(postId, uid, nowLiked)
            } catch (e: Exception) {
                setLocalLike(postId, !nowLiked)
                _state.update { it.copy(message = e.message ?: "Could not update like.") }
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
                posts.editPost(postId, text)
                _state.update { s ->
                    s.copy(items = s.items.map {
                        if (it.post.id == postId) it.copy(post = it.post.copy(text = text, editedAt = java.util.Date())) else it
                    })
                }
            } catch (e: Exception) {
                _state.update { it.copy(message = e.message ?: "Could not edit the post.") }
            }
        }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            try {
                posts.deletePost(postId)
                _state.update { s -> s.copy(items = s.items.filter { it.post.id != postId }) }
            } catch (e: Exception) {
                _state.update { it.copy(message = e.message ?: "Could not delete the post.") }
            }
        }
    }

    fun reportPost(post: Post, reason: String, details: String) {
        viewModelScope.launch {
            val text = try {
                reportRepo.create(uid, "post", post.id, post.authorId, reason, details)
                "Report sent. Thank you."
            } catch (e: Exception) {
                "You already reported this post, or reporting isn't allowed right now."
            }
            _state.update { it.copy(message = text) }
        }
    }

    fun deleteCommunity() {
        viewModelScope.launch {
            try {
                repo.delete(id)
                _state.update { it.copy(deleted = true) }
            } catch (e: Exception) {
                _state.update { it.copy(message = e.message ?: "Could not delete it.") }
            }
        }
    }

    fun uploadImage(resolver: ContentResolver, uri: Uri, onResult: (Result<String>) -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            val result = try {
                Result.success(CloudinaryUploader.upload(resolver, uri))
            } catch (e: Exception) {
                Result.failure(e)
            }
            _state.update { it.copy(busy = false) }
            onResult(result)
        }
    }

    // onResult(null) = success, otherwise an error message
    fun createPost(text: String, imageUrl: String, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            val message = try {
                posts.createPost(uid, text, imageUrl)
                null
            } catch (e: Exception) {
                if (e.message?.contains("PERMISSION_DENIED") == true) {
                    "Couldn't post. If you posted a moment ago, wait 10 seconds and try again."
                } else {
                    e.message ?: "Could not post."
                }
            }
            _state.update { it.copy(busy = false) }
            onResult(message)
            if (message == null) refreshPosts()
        }
    }

    fun clearMessage() {
        _state.update { it.copy(message = null) }
    }
}

class CommunityDetailViewModelFactory(
    private val type: String,
    private val id: String,
    private val uid: String,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = CommunityDetailViewModel(type, id, uid) as T
}
