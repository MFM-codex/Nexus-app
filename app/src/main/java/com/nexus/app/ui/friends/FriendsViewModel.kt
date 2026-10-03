package com.nexus.app.ui.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nexus.app.data.FriendRepository
import com.nexus.app.data.Friendship
import com.nexus.app.data.Profile
import com.nexus.app.data.Relation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FriendsState(
    val friendships: List<Friendship> = emptyList(), // every friend + request involving me
    val profiles: Map<String, Profile> = emptyMap(),  // names/avatars of those people
    val query: String = "",                            // what I typed in the search box
    val results: List<Profile> = emptyList(),
    val searching: Boolean = false,
    val error: String? = null,
)

class FriendsViewModel(private val me: String) : ViewModel() {
    private val repo = FriendRepository()

    private val _state = MutableStateFlow(FriendsState())
    val state: StateFlow<FriendsState> = _state

    private var searchJob: Job? = null

    init {
        // Live: updates by itself when someone sends or accepts a request.
        viewModelScope.launch {
            repo.observe(me)
                .catch { e -> _state.update { it.copy(error = e.message ?: "Could not load friends.") } }
                .collect { list ->
                    val others = list.map { it.other(me) }.toSet()
                    val loaded = try {
                        repo.profiles(others)
                    } catch (e: Exception) {
                        emptyMap()
                    }
                    _state.update { it.copy(friendships = list, profiles = it.profiles + loaded) }
                }
        }
    }

    // How am I connected to this person?
    fun relationWith(other: String): Relation {
        val f = _state.value.friendships.firstOrNull { it.other(me) == other } ?: return Relation.NONE
        return when {
            f.status == "accepted" -> Relation.FRIEND
            f.requesterId == me -> Relation.OUTGOING
            else -> Relation.INCOMING
        }
    }

    suspend fun profileOf(uid: String): Profile? =
        _state.value.profiles[uid] ?: try {
            repo.getProfile(uid)
        } catch (e: Exception) {
            null
        }

    // Runs when you type in the search box. Waits a moment so we don't search every letter.
    fun onQueryChange(text: String) {
        _state.update { it.copy(query = text) }
        searchJob?.cancel()
        val clean = text.trim().lowercase().removePrefix("@")
        if (clean.isEmpty()) {
            _state.update { it.copy(results = emptyList(), searching = false) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(400)
            _state.update { it.copy(searching = true) }
            try {
                val found = repo.searchUsers(clean).filter { it.uid != me }
                _state.update { it.copy(results = found, searching = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(searching = false, error = friendly(e)) }
            }
        }
    }

    fun sendRequest(other: String) = launchAction { repo.sendRequest(me, other) }
    fun accept(other: String) = launchAction { repo.accept(me, other) }

    // Used for decline, cancel request and unfriend.
    fun remove(other: String) = launchAction { repo.remove(me, other) }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    private fun launchAction(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(error = friendly(e)) }
            }
        }
    }

    private fun friendly(e: Exception): String =
        if (e.message?.contains("PERMISSION_DENIED") == true) {
            "Not allowed. Check that your Firestore rules are published."
        } else {
            e.message ?: "Something went wrong."
        }
}

class FriendsViewModelFactory(private val me: String) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = FriendsViewModel(me) as T
}
