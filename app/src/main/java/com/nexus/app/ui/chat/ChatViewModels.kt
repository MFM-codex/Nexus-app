package com.nexus.app.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nexus.app.data.Chat
import com.nexus.app.data.ChatRepository
import com.nexus.app.data.Message
import com.nexus.app.data.PostRepository
import com.nexus.app.data.Profile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// One line in the conversation list.
data class ChatRow(val chat: Chat, val otherUid: String, val other: Profile?, val unread: Int)

// The list of my conversations (also tells the bottom bar how many unread messages I have).
class ChatsViewModel(private val me: String) : ViewModel() {
    private val repo = ChatRepository()
    private val postRepo = PostRepository()

    // null = still loading
    val rows: StateFlow<List<ChatRow>?> = repo.observeChats(me)
        .map { list ->
            // hide conversations where nobody has written yet
            val active = list
                .filter { it.lastMessage.isNotEmpty() }
                .sortedByDescending { it.lastMessageAt?.time ?: 0L }
            val others = active.map { chat -> chat.members.firstOrNull { it != me } ?: "" }
            val profiles = try {
                postRepo.authors(others.toSet())
            } catch (e: Exception) {
                emptyMap()
            }
            active.mapIndexed { i, chat ->
                ChatRow(chat, others[i], profiles[others[i]], chat.unread[me] ?: 0)
            }
        }
        .catch { emit(emptyList()) }
        .stateIn<List<ChatRow>?>(viewModelScope, SharingStarted.Eagerly, null)

    val totalUnread: StateFlow<Int> = rows
        .map { list -> list.orEmpty().sumOf { it.unread } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)
}

class ChatsViewModelFactory(private val me: String) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = ChatsViewModel(me) as T
}

// One conversation with one friend.
class ChatViewModel(private val me: String, val other: String) : ViewModel() {
    private val repo = ChatRepository()
    private val postRepo = PostRepository()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _sending = MutableStateFlow(false)
    val sending: StateFlow<Boolean> = _sending

    private val chatId = repo.chatId(me, other)

    // Newest first.
    val messages: StateFlow<List<Message>> = repo.observeMessages(chatId)
        .catch {
            _error.value = it.message ?: "Could not load messages."
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val chat: StateFlow<Chat?> = repo.observeChat(chatId)
        .catch { emit(null) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _otherProfile = MutableStateFlow<Profile?>(null)
    val otherProfile: StateFlow<Profile?> = _otherProfile

    init {
        viewModelScope.launch {
            _otherProfile.value = try {
                postRepo.authors(setOf(other))[other]
            } catch (e: Exception) {
                null
            }
        }
    }

    // onDone(true) = message was sent.
    fun send(text: String, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            _sending.value = true
            _error.value = null
            val ok = try {
                repo.send(me, other, text)
                true
            } catch (e: Exception) {
                _error.value = if (e.message?.contains("PERMISSION_DENIED") == true) {
                    "You can only message people who are your friends."
                } else {
                    e.message ?: "Could not send."
                }
                false
            }
            _sending.value = false
            onDone(ok)
        }
    }

    fun markRead() {
        viewModelScope.launch {
            try {
                repo.markRead(me, other)
            } catch (e: Exception) {
                // not important; we'll try again next time
            }
        }
    }
}

class ChatViewModelFactory(private val me: String, private val other: String) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = ChatViewModel(me, other) as T
}
