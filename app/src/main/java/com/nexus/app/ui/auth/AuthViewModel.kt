package com.nexus.app.ui.auth

import android.app.Activity
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.nexus.app.data.PendingSignup
import com.nexus.app.data.ProfileRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// A ViewModel holds screen data and logic so it survives screen rotation.
class AuthViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()

    private val _user = MutableStateFlow(auth.currentUser)
    val user: StateFlow<FirebaseUser?> = _user

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    // Firebase tells us whenever someone signs in or out.
    // While we create the account AND the profile, don't tell the screen yet,
    // so you don't see a flash of the "finish setting up" page.
    private var signingUp = false

    private val listener = FirebaseAuth.AuthStateListener { if (!signingUp) _user.value = it.currentUser }

    init {
        auth.addAuthStateListener(listener)
    }

    override fun onCleared() {
        auth.removeAuthStateListener(listener)
    }

    fun signIn(email: String, password: String) = launchAuth {
        auth.signInWithEmailAndPassword(email.trim(), password).await()
    }

    fun signUp(firstName: String, lastName: String, username: String, email: String, password: String) = launchAuth {
        val cleanUsername = username.trim().lowercase()
        val fullName = "${firstName.trim()} ${lastName.trim()}".trim()
        signingUp = true
        try {
            val created = auth.createUserWithEmailAndPassword(email.trim(), password).await()
            val newUser = created.user ?: throw IllegalStateException("Could not create the account.")
            PendingSignup.username = cleanUsername
            try {
                newUser.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(fullName).build()).await()
            } catch (e: Exception) {
                // not important
            }
            try {
                ProfileRepository().createProfile(newUser.uid, fullName, cleanUsername)
            } catch (e: Exception) {
                // For example the username was taken. The app will show the
                // "finish setting up your profile" page so they can pick another one.
            }
        } finally {
            signingUp = false
            _user.value = auth.currentUser
        }
    }

    fun signInWithGoogle(activity: Activity) = launchAuth {
        // This id is generated from google-services.json once Google sign-in is enabled in Firebase.
        val id = activity.resources.getIdentifier("default_web_client_id", "string", activity.packageName)
        if (id == 0) {
            throw IllegalStateException(
                "Google sign-in isn't set up yet. Enable Google in Firebase Authentication, " +
                    "add the SHA-1, then download google-services.json again."
            )
        }
        val option = GetSignInWithGoogleOption.Builder(activity.getString(id)).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        val result = CredentialManager.create(activity).getCredential(activity, request)
        val credential = result.credential
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val token = GoogleIdTokenCredential.createFrom(credential.data).idToken
            auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
        } else {
            throw IllegalStateException("Unexpected Google sign-in response.")
        }
    }

    fun signOut() {
        auth.signOut()
    }

    // Runs an auth action with a loading spinner and friendly error messages.
    private fun launchAuth(block: suspend () -> Unit) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _error.value = friendly(e)
            }
            _loading.value = false
        }
    }

    private fun friendly(e: Exception): String = when (e) {
        is FirebaseAuthInvalidCredentialsException -> "Wrong email or password."
        is FirebaseAuthInvalidUserException -> "No account with that email."
        is FirebaseAuthUserCollisionException -> "That email is already registered. Try logging in."
        is FirebaseAuthWeakPasswordException -> "Password must be at least 6 characters."
        is FirebaseNetworkException -> "No internet connection."
        is GetCredentialCancellationException -> "Sign-in cancelled."
        else -> e.message ?: "Something went wrong."
    }
}
