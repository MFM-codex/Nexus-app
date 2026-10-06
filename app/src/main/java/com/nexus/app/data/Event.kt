package com.nexus.app.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// An event. Stored at events/{id}. RSVPs are at events/{id}/rsvps/{uid}.
data class Event(
    val id: String,
    val hostId: String,
    val title: String,
    val description: String,
    val location: String,
    val startAt: Date?,
    val createdAt: Date?,
)

// "Sat, 18 Oct 2026 at 15:00"
fun formatEventTime(date: Date?): String =
    if (date == null) "" else SimpleDateFormat("EEE, d MMM yyyy 'at' HH:mm", Locale.getDefault()).format(date)
