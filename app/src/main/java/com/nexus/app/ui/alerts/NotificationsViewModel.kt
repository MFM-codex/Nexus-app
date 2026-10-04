package com.nexus.app.ui.alerts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nexus.app.data.Notification
import com.nexus.app.data.NotificationRepository
import com.nexus.app.data.PostRepository
import com.nexus.app.data.Profile
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class NotifUi(val n: Notification, val actor: Profile?)

// My notifications, live. Also gives the bottom bar the unread count.
class NotificationsViewModel(private val me: String) : ViewModel() {
    private val repo = NotificationRepository()
    private val postRepo = PostRepository()

    // null = still loading
    val items: StateFlow<List<NotifUi>?> = repo.observe(me)
        .map { list ->
            val actors = try {
                postRepo.authors(list.map { it.actorId }.toSet())
            } catch (e: Exception) {
                emptyMap()
            }
            list.map { NotifUi(it, actors[it.actorId]) }
        }
        .catch { emit(emptyList()) }
        .stateIn<List<NotifUi>?>(viewModelScope, SharingStarted.Eagerly, null)

    val unreadCount: StateFlow<Int> = items
        .map { list -> list.orEmpty().count { !it.n.read } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    fun markRead(id: String) {
        viewModelScope.launch {
            try {
                repo.markRead(me, listOf(id))
            } catch (e: Exception) {
                // not important
            }
        }
    }

    fun markAllRead() {
        val ids = items.value.orEmpty().filter { !it.n.read }.map { it.n.id }
        viewModelScope.launch {
            try {
                repo.markRead(me, ids)
            } catch (e: Exception) {
                // not important
            }
        }
    }
}

class NotificationsViewModelFactory(private val me: String) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = NotificationsViewModel(me) as T
}
