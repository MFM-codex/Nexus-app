package com.nexus.app.ui.saved

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nexus.app.data.Post
import com.nexus.app.data.PostRepository
import com.nexus.app.data.Profile
import com.nexus.app.data.SavedRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class SavedUi(val post: Post, val author: Profile?)

// Your saved posts. items = null means "still loading".
class SavedViewModel(private val uid: String) : ViewModel() {
    private val savedRepo = SavedRepository()
    private val postRepo = PostRepository()

    private val _items = MutableStateFlow<List<SavedUi>?>(null)
    val items: StateFlow<List<SavedUi>?> = _items

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        viewModelScope.launch {
            try {
                val ids = savedRepo.savedIds(uid)
                // A post may have been deleted, or you may not be friends anymore: skip those
                val posts = ids.mapNotNull { id ->
                    try {
                        postRepo.getPost(id)
                    } catch (e: Exception) {
                        null
                    }
                }
                val authors = postRepo.authors(posts.map { it.authorId }.toSet())
                _items.value = posts.map { SavedUi(it, authors[it.authorId]) }
            } catch (e: Exception) {
                _error.value = e.message ?: "Could not load your saved posts."
                _items.value = emptyList()
            }
        }
    }

    fun unsave(postId: String) {
        viewModelScope.launch {
            try {
                savedRepo.setSaved(uid, postId, false)
                _items.value = _items.value?.filter { it.post.id != postId }
            } catch (e: Exception) {
                _error.value = e.message ?: "Could not remove it."
            }
        }
    }
}

class SavedViewModelFactory(private val uid: String) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = SavedViewModel(uid) as T
}
