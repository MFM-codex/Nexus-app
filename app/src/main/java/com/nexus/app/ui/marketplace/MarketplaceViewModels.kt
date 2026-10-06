package com.nexus.app.ui.marketplace

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nexus.app.data.CloudinaryUploader
import com.nexus.app.data.Listing
import com.nexus.app.data.ListingRepository
import com.nexus.app.data.PostRepository
import com.nexus.app.data.Profile
import com.nexus.app.data.ReportRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// The Marketplace tab: the newest listings. items = null means "still loading".
class MarketplaceViewModel(private val uid: String) : ViewModel() {
    private val repo = ListingRepository()

    private val _items = MutableStateFlow<List<Listing>?>(null)
    val items: StateFlow<List<Listing>?> = _items

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            try {
                _items.value = repo.latest()
            } catch (e: Exception) {
                _error.value = e.message ?: "Could not load Marketplace."
                if (_items.value == null) _items.value = emptyList()
            }
        }
    }

    fun clearError() {
        _error.value = null
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

    // onResult(null) = success, otherwise an error message
    fun createListing(
        title: String, price: Long, description: String, category: String,
        location: String, contact: String, imageUrl: String, onResult: (String?) -> Unit,
    ) {
        viewModelScope.launch {
            _busy.value = true
            val message = try {
                repo.create(uid, title, price, description, category, location, contact, imageUrl)
                null
            } catch (e: Exception) {
                if (e.message?.contains("PERMISSION_DENIED") == true) {
                    "Couldn't post the listing. Check the details and try again."
                } else {
                    e.message ?: "Could not post the listing."
                }
            }
            _busy.value = false
            onResult(message)
            if (message == null) refresh()
        }
    }
}

class MarketplaceViewModelFactory(private val uid: String) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = MarketplaceViewModel(uid) as T
}

// One listing's page.
class ListingViewModel(private val id: String, private val uid: String) : ViewModel() {
    private val repo = ListingRepository()
    private val postRepo = PostRepository()
    private val reportRepo = ReportRepository()

    private val _listing = MutableStateFlow<Listing?>(null)
    val listing: StateFlow<Listing?> = _listing

    private val _seller = MutableStateFlow<Profile?>(null)
    val seller: StateFlow<Profile?> = _seller

    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    private val _deleted = MutableStateFlow(false)
    val deleted: StateFlow<Boolean> = _deleted

    init {
        viewModelScope.launch {
            try {
                val item = repo.get(id)
                _listing.value = item
                if (item != null) {
                    _seller.value = try {
                        postRepo.authors(setOf(item.sellerId))[item.sellerId]
                    } catch (e: Exception) {
                        null
                    }
                }
            } catch (e: Exception) {
                _message.value = e.message ?: "Could not load this listing."
            }
            _loaded.value = true
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    fun toggleSold() {
        val current = _listing.value ?: return
        viewModelScope.launch {
            try {
                repo.setSold(id, !current.sold)
                _listing.value = current.copy(sold = !current.sold)
            } catch (e: Exception) {
                _message.value = e.message ?: "Could not update the listing."
            }
        }
    }

    fun delete() {
        viewModelScope.launch {
            try {
                repo.delete(id)
                _deleted.value = true
            } catch (e: Exception) {
                _message.value = e.message ?: "Could not delete the listing."
            }
        }
    }

    fun report(reason: String, details: String) {
        val current = _listing.value ?: return
        viewModelScope.launch {
            _message.value = try {
                reportRepo.create(uid, "listing", id, current.sellerId, reason, details)
                "Report sent. Thank you."
            } catch (e: Exception) {
                "You already reported this listing, or reporting isn't allowed right now."
            }
        }
    }
}

class ListingViewModelFactory(private val id: String, private val uid: String) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = ListingViewModel(id, uid) as T
}
