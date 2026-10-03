package com.nexus.app.ui.comments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nexus.app.data.CommentUi
import com.nexus.app.data.NotificationRepository
import com.nexus.app.data.PostRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CommentsViewModel(private val postId: String, private val uid: String) : ViewModel() {
    private val repo = PostRepository()
    private val notifRepo = NotificationRepository()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _sending = MutableStateFlow(false)
    val sending: StateFlow<Boolean> = _sending

    // Live list of comments, each with its author attached.
    val comments: StateFlow<List<CommentUi>> = repo.observeComments(postId)
        .map { list ->
            val authors = repo.authors(list.map { it.authorId }.toSet())
            list.map { CommentUi(it, authors[it.authorId]) }
        }
        .catch {
            _error.value = it.message ?: "Could not load comments."
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // onDone(true) = comment was saved.
    fun send(text: String, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            _sending.value = true
            _error.value = null
            val ok = try {
                repo.addComment(postId, uid, text)
                // Tell the post's author (failures here don't matter)
                try {
                    val authorId = repo.authorOf(postId)
                    if (authorId != null && authorId != uid) {
                        notifRepo.create(authorId, uid, "comment", postId)
                    }
                } catch (e: Exception) {
                    // ignore
                }
                true
            } catch (e: Exception) {
                _error.value = e.message ?: "Could not send comment."
                false
            }
            _sending.value = false
            onDone(ok)
        }
    }

    fun delete(commentId: String, onDone: () -> Unit) {
        viewModelScope.launch {
            _error.value = null
            try {
                repo.deleteComment(postId, commentId)
                onDone()
            } catch (e: Exception) {
                _error.value = e.message ?: "Could not delete comment."
            }
        }
    }
}

class CommentsViewModelFactory(private val postId: String, private val uid: String) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = CommentsViewModel(postId, uid) as T
}
