package com.nexus.app.data

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

class UsernameTakenException : Exception("username_taken")

// Talks to Firestore about profiles.
// Collections:
//   users/{uid}          -> the profile
//   usernames/{username} -> { uid }  (a lookup table that guarantees usernames are unique)
class ProfileRepository {
    private val db = FirebaseFirestore.getInstance()
    private val users = db.collection("users")
    private val usernames = db.collection("usernames")

    // Live updates of one profile. Emits null if the profile doesn't exist yet.
    fun observe(uid: String): Flow<Profile?> = callbackFlow {
        val registration = users.document(uid).addSnapshotListener { snap, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snap != null && snap.exists()) {
                trySend(
                    Profile(
                        uid = snap.id,
                        username = snap.getString("username") ?: "",
                        name = snap.getString("name") ?: "",
                        bio = snap.getString("bio") ?: "",
                        avatarUrl = snap.getString("avatarUrl") ?: "",
                        coverUrl = snap.getString("coverUrl") ?: "",
                    )
                )
            } else {
                trySend(null)
            }
        }
        awaitClose { registration.remove() }
    }

    // First login: create the profile with a generated username like "john_4821".
    suspend fun ensureProfile(uid: String, email: String?, displayName: String?) {
        val userRef = users.document(uid)
        if (userRef.get().await().exists()) return

        val base = (email?.substringBefore('@') ?: "user")
            .lowercase()
            .replace(Regex("[^a-z0-9_]"), "")
            .take(12)
            .padEnd(3, 'x')
        val name = (displayName?.takeIf { it.isNotBlank() } ?: base).take(50)

        repeat(8) {
            val username = base + "_" + Random.nextInt(1000, 10000)
            try {
                // A transaction = all-or-nothing. Both documents are written, or neither.
                db.runTransaction { tx ->
                    val usernameRef = usernames.document(username)
                    if (tx.get(usernameRef).exists()) throw UsernameTakenException()
                    tx.set(usernameRef, mapOf("uid" to uid))
                    tx.set(
                        userRef,
                        mapOf(
                            "username" to username,
                            "name" to name,
                            "bio" to "",
                            "avatarUrl" to "",
                            "coverUrl" to "",
                            "createdAt" to FieldValue.serverTimestamp(),
                        )
                    )
                    true
                }.await()
                return
            } catch (e: Exception) {
                if (!isUsernameTaken(e)) throw e // only retry when the random username collided
            }
        }
        throw IllegalStateException("Could not create a username. Try again.")
    }

    // Create the profile with the name and username the person chose.
    suspend fun createProfile(uid: String, name: String, username: String) {
        db.runTransaction { tx ->
            val usernameRef = usernames.document(username)
            if (tx.get(usernameRef).exists()) throw UsernameTakenException()
            tx.set(usernameRef, mapOf("uid" to uid))
            tx.set(
                users.document(uid),
                mapOf(
                    "username" to username,
                    "name" to name,
                    "bio" to "",
                    "avatarUrl" to "",
                    "coverUrl" to "",
                    "createdAt" to FieldValue.serverTimestamp(),
                )
            )
            true
        }.await()
    }

    // Is this username still free?
    suspend fun isUsernameFree(username: String): Boolean =
        !usernames.document(username).get().await().exists()

    // Save edits. If the username changed, claim the new one and release the old one.
    suspend fun saveProfile(
        uid: String,
        oldUsername: String,
        name: String,
        username: String,
        bio: String,
        avatarUrl: String,
        coverUrl: String,
    ) {
        db.runTransaction { tx ->
            val changed = username != oldUsername
            val newUsernameRef = usernames.document(username)

            // Firestore transactions: do all reads first, then all writes.
            if (changed) {
                val existing = tx.get(newUsernameRef)
                if (existing.exists() && existing.getString("uid") != uid) {
                    throw UsernameTakenException()
                }
            }
            if (changed) {
                tx.set(newUsernameRef, mapOf("uid" to uid))
                if (oldUsername.isNotEmpty()) tx.delete(usernames.document(oldUsername))
            }
            tx.update(
                users.document(uid),
                mapOf(
                    "username" to username,
                    "name" to name,
                    "bio" to bio,
                    "avatarUrl" to avatarUrl,
                    "coverUrl" to coverUrl,
                )
            )
            true
        }.await()
    }
}

fun isUsernameTaken(e: Throwable): Boolean =
    generateSequence(e) { it.cause }.any {
        it is UsernameTakenException || it.message?.contains("username_taken") == true
    }
