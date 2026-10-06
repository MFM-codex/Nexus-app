package com.nexus.app.ui.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nexus.app.data.Event
import com.nexus.app.data.EventRepository
import com.nexus.app.data.PostRepository
import com.nexus.app.data.Profile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Date

// The list of upcoming events. null = still loading.
class EventsViewModel(private val uid: String) : ViewModel() {
    private val repo = EventRepository()

    private val _items = MutableStateFlow<List<Event>?>(null)
    val items: StateFlow<List<Event>?> = _items

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
                _items.value = repo.upcoming()
            } catch (e: Exception) {
                _message.value = e.message ?: "Could not load events."
                if (_items.value == null) _items.value = emptyList()
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    // onResult(newId, null) on success, or (null, errorMessage)
    fun create(title: String, description: String, location: String, startAt: Date, onResult: (String?, String?) -> Unit) {
        viewModelScope.launch {
            _busy.value = true
            try {
                val id = repo.create(uid, title, description, location, startAt)
                _busy.value = false
                onResult(id, null)
                load()
            } catch (e: Exception) {
                _busy.value = false
                onResult(null, e.message ?: "Could not create the event.")
            }
        }
    }
}

class EventsViewModelFactory(private val uid: String) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = EventsViewModel(uid) as T
}

data class EventDetailState(
    val event: Event? = null,
    val host: Profile? = null,
    val loaded: Boolean = false,
    val myRsvp: String? = null,
    val going: Long = 0,
    val interested: Long = 0,
    val deleted: Boolean = false,
    val message: String? = null,
)

class EventDetailViewModel(private val id: String, private val uid: String) : ViewModel() {
    private val repo = EventRepository()
    private val postRepo = PostRepository()

    private val _state = MutableStateFlow(EventDetailState())
    val state: StateFlow<EventDetailState> = _state

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            try {
                val event = repo.get(id)
                if (event == null) {
                    _state.value = _state.value.copy(loaded = true)
                    return@launch
                }
                val host = try {
                    postRepo.authors(setOf(event.hostId))[event.hostId]
                } catch (e: Exception) {
                    null
                }
                val mine = try {
                    repo.myRsvp(id, uid)
                } catch (e: Exception) {
                    null
                }
                val going = try { repo.count(id, "going") } catch (e: Exception) { 0L }
                val interested = try { repo.count(id, "interested") } catch (e: Exception) { 0L }
                _state.value = EventDetailState(event, host, true, mine, going, interested)
            } catch (e: Exception) {
                _state.value = _state.value.copy(loaded = true, message = e.message ?: "Could not load the event.")
            }
        }
    }

    // Tap "Going" or "Interested". Tapping your current choice again removes it.
    fun rsvp(status: String) {
        val s = _state.value
        val newStatus = if (s.myRsvp == status) null else status
        viewModelScope.launch {
            try {
                repo.setRsvp(id, uid, newStatus)
                load()
            } catch (e: Exception) {
                _state.value = _state.value.copy(message = e.message ?: "Could not save your answer.")
            }
        }
    }

    fun delete() {
        viewModelScope.launch {
            try {
                repo.delete(id)
                _state.value = _state.value.copy(deleted = true)
            } catch (e: Exception) {
                _state.value = _state.value.copy(message = e.message ?: "Could not delete the event.")
            }
        }
    }

    fun clearMessage() {
        _state.value = _state.value.copy(message = null)
    }
}

class EventDetailViewModelFactory(private val id: String, private val uid: String) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = EventDetailViewModel(id, uid) as T
}
