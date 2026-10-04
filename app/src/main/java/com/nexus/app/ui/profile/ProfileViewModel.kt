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
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val uid: String,
    email: String?,
    displayName: String?,
) : ViewModel() {
    private val repo = ProfileRepository()

    // The signed-in user's profile, updated live from Firestore.
    // True once Firestore has answered (even if the answer is "no profile yet").
    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded

    val profile: StateFlow<Profile?> = repo.observe(uid)
        .catch { emit(null) }
        .onEach { _loaded.value = true }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy

    // Set if creating the profile failed (for example, rules not published yet).
    private val _setupError = MutableStateFlow<String?>(null)
    val setupError: StateFlow<String?> = _setupError

    // Used by the "finish setting up your profile" page.
    suspend fun isUsernameFree(username: String): Boolean = repo.isUsernameFree(username)

    // onResult(null) = success, otherwise an error message.
    fun createProfile(firstName: String, lastName: String, username: String, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            _busy.value = true
            val name = "$firstName $lastName".trim().take(50)
            val message = try {
                repo.createProfile(uid, name, username)
                null
            } catch (e: Exception) {
                when {
                    isUsernameTaken(e) -> "That username is taken. Try another."
                    e.message?.contains("PERMISSION_DENIED") == true ->
                        "Not allowed. Check that your Firestore rules are published."
                    else -> e.message ?: "Could not create your profile."
                }
            }
            _busy.value = false
            onResult(message)
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
