package com.nexus.app.data

import java.text.NumberFormat
import java.util.Date
import java.util.Locale

val listingCategories = listOf("Phones", "Fashion", "Electronics", "Home", "Vehicles", "Food", "Other")

// Something for sale. Stored in Firestore at listings/{id}.
data class Listing(
    val id: String,
    val sellerId: String,
    val title: String,
    val price: Long,       // in naira
    val description: String,
    val category: String,
    val location: String,
    val contact: String,   // phone / WhatsApp number (optional)
    val imageUrl: String,
    val sold: Boolean,
    val createdAt: Date?,
)

// 95000 -> "NGN95,000"
fun formatPrice(price: Long): String = "NGN" + NumberFormat.getIntegerInstance(Locale.US).format(price)

// A WhatsApp link for a Nigerian number: 0803... -> https://wa.me/234803...
fun whatsappLink(raw: String): String? {
    val digits = raw.filter { it.isDigit() }
    if (digits.isEmpty()) return null
    val full = if (digits.startsWith("0") && digits.length == 11) "234" + digits.drop(1) else digits
    return "https://wa.me/$full"
}
