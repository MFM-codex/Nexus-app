package com.nexus.app.data

// A friend request or friendship between two people.
// Stored at friendships/{pairId} where pairId = the two user ids joined with "_".
// status is "pending" (request sent) or "accepted" (you are friends).
data class Friendship(
    val id: String,
    val requesterId: String, // who sent the request
    val addresseeId: String, // who received it
    val status: String,
) {
    // The other person in this friendship, from my point of view.
    fun other(me: String) = if (requesterId == me) addresseeId else requesterId
}

// How I am connected to someone else.
enum class Relation { NONE, FRIEND, OUTGOING, INCOMING }
