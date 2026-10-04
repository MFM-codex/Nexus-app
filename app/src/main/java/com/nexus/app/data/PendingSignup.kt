package com.nexus.app.data

// Remembers the username typed on the sign-up form, in case we need to ask again
// on the "finish setting up your profile" screen (for example if it was already taken).
object PendingSignup {
    var username: String = ""
}
