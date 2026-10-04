package com.nexus.app.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nexus.app.data.AdminRepository
import com.nexus.app.data.Post
import com.nexus.app.data.PostRepository
import com.nexus.app.data.Profile
import com.nexus.app.data.Reel
import com.nexus.app.data.ReelRepository
import com.nexus.app.data.Report
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// A report plus the people and post it is about, ready to draw.
data class ReportUi(
    val report: Report,
    val reporter: Profile?,
    val target: Profile?,
    val post: Post?,
    val reel: Reel? = null,
)

// Knows whether I'm an admin, whether I'm banned, and (for admins) the open reports.
@OptIn(ExperimentalCoroutinesApi::class)
class AdminViewModel(private val me: String) : ViewModel() {
    private val repo = AdminRepository()
    private val postRepo = PostRepository()
    private val reelRepo = ReelRepository()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    // You become an admin when a document with your user id exists in the "admins" collection.
    val isAdmin: StateFlow<Boolean> = repo.observeExists("admins", me)
        .catch { emit(false) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    // You are suspended when a document with your user id exists in "banned".
    val banned: StateFlow<Boolean> = repo.observeExists("banned", me)
        .catch { emit(false) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    // Only admins load the reports.
    val reports: StateFlow<List<ReportUi>> = isAdmin
        .flatMapLatest { admin ->
            if (!admin) {
                flowOf(emptyList())
            } else {
                repo.observeOpenReports()
                    .map { list -> enrich(list) }
                    .catch {
                        _message.value = it.message ?: "Could not load reports."
                        emit(emptyList())
                    }
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Add names, avatars and the reported post to each report.
    private suspend fun enrich(list: List<Report>): List<ReportUi> {
        val sorted = list.sortedByDescending { it.createdAt?.time ?: 0L }
        val people = try {
            postRepo.authors(sorted.flatMap { listOf(it.reporterId, it.targetUserId) }.toSet())
        } catch (e: Exception) {
            emptyMap()
        }
        return sorted.map { r ->
            val post = if (r.targetType == "post") {
                try {
                    postRepo.getPost(r.targetId)
                } catch (e: Exception) {
                    null
                }
            } else {
                null
            }
            val reel = if (r.targetType == "reel") {
                try {
                    reelRepo.get(r.targetId)
                } catch (e: Exception) {
                    null
                }
            } else {
                null
            }
            ReportUi(r, people[r.reporterId], people[r.targetUserId], post, reel)
        }
    }

    fun dismiss(report: Report) = act("Report dismissed.") {
        repo.setStatus(report.id, "dismissed")
    }

    fun deletePost(report: Report) = act("Post deleted.") {
        repo.deletePost(report.targetId)
        repo.setStatus(report.id, "resolved")
    }

    fun deleteReel(report: Report) = act("Reel deleted.") {
        repo.deleteReel(report.targetId)
        repo.setStatus(report.id, "resolved")
    }

    fun ban(report: Report) = act("User banned.") {
        repo.ban(report.targetUserId, me)
        repo.setStatus(report.id, "resolved")
    }

    fun clearMessage() {
        _message.value = null
    }

    private fun act(done: String, block: suspend () -> Unit) {
        viewModelScope.launch {
            _message.value = try {
                block()
                done
            } catch (e: Exception) {
                e.message ?: "That didn't work."
            }
        }
    }
}

class AdminViewModelFactory(private val me: String) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = AdminViewModel(me) as T
}
