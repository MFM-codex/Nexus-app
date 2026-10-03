package com.nexus.app.ui.profile

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nexus.app.data.CloudinaryUploader
import com.nexus.app.data.Profile
import com.nexus.app.data.ProfileRepository
import com.nexus.app.data.isUsernameTaken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val uid: String,
    email: String?,
    displayName: String?,
) : ViewModel() {
    private val repo = ProfileRepository()

    // The signed-in user's profile, updated live from Firestore.
    val profile: StateFlow<Profile?> = repo.observe(uid)
        .catch { emit(null) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy

    // Set if creating the profile failed (for example, rules not published yet).
    private val _setupError = MutableStateFlow<String?>(null)
    val setupError: StateFlow<String?> = _setupError

    init {
        viewModelScope.launch {
            try {
                repo.ensureProfile(uid, email, displayName)
            } catch (e: Exception) {
                _setupError.value = e.message ?: "Could not create your profile."
            }
        }
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

    // onResult(null) means success; otherwise it gets an error message to show.
    fun save(
        name: String,
        username: String,
        bio: String,
        avatarUrl: String,
        coverUrl: String,
        onResult: (String?) -> Unit,
    ) {
        val old = profile.value?.username ?: ""
        viewModelScope.launch {
            _busy.value = true
            val message = try {
                repo.saveProfile(uid, old, name, username, bio, avatarUrl, coverUrl)
                null
            } catch (e: Exception) {
                when {
                    isUsernameTaken(e) -> "That username is taken."
                    e.message?.contains("PERMISSION_DENIED") == true ->
                        "Not allowed. Check that your Firestore rules are published."
                    else -> e.message ?: "Could not save."
                }
            }
            _busy.value = false
            onResult(message)
        }
    }
}

// Lets us create a ProfileViewModel that needs the user's id.
class ProfileViewModelFactory(
    private val uid: String,
    private val email: String?,
    private val displayName: String?,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        ProfileViewModel(uid, email, displayName) as T
}
