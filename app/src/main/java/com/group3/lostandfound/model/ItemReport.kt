package com.group3.lostandfound.model

import com.google.firebase.database.DataSnapshot

/**
 * Data Model for Lost and Found item reports stored in Firebase Realtime Database at `/items`.
 */
data class ItemReport(
    val itemId: String = "",
    val userId: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "",
    val status: String = "LOST",
    val imageUrl: String = "",
    val location: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val timestamp: Long = 0L
) {
    /**
     * Converts ItemReport to a map representation for Firebase Realtime Database updates.
     */
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "itemId" to itemId,
            "userId" to userId,
            "title" to title,
            "description" to description,
            "category" to category,
            "status" to status,
            "imageUrl" to imageUrl,
            "location" to if (location.isNotEmpty()) location else "$category, Western Province",
            "latitude" to latitude,
            "longitude" to longitude,
            "timestamp" to timestamp,
            "isFlagged" to false,
            "flagReason" to "",
            "authorName" to "Community Member"
        )
    }

    companion object {
        /**
         * Safely parse a Firebase DataSnapshot into an ItemReport object,
         * handling missing fields and heterogeneous number formats gracefully.
         */
        fun fromSnapshot(snapshot: DataSnapshot): ItemReport {
            val key = snapshot.key.orEmpty()
            val itemId = snapshot.child("itemId").getValue(String::class.java)?.takeIf { it.isNotEmpty() } ?: key
            val userId = snapshot.child("userId").getValue(String::class.java).orEmpty()
            val title = snapshot.child("title").getValue(String::class.java).orEmpty()
            val description = snapshot.child("description").getValue(String::class.java).orEmpty()
            val category = snapshot.child("category").getValue(String::class.java).orEmpty()
            val status = snapshot.child("status").getValue(String::class.java)?.takeIf { it.isNotEmpty() } ?: "LOST"
            val imageUrl = snapshot.child("imageUrl").getValue(String::class.java).orEmpty()
            val location = snapshot.child("location").getValue(String::class.java).orEmpty()

            val rawLat = snapshot.child("latitude").value
            val latitude = when (rawLat) {
                is Number -> rawLat.toDouble()
                is String -> rawLat.toDoubleOrNull() ?: 0.0
                else -> 0.0
            }

            val rawLng = snapshot.child("longitude").value
            val longitude = when (rawLng) {
                is Number -> rawLng.toDouble()
                is String -> rawLng.toDoubleOrNull() ?: 0.0
                else -> 0.0
            }

            val rawTimestamp = snapshot.child("timestamp").value
            val timestamp = when (rawTimestamp) {
                is Number -> rawTimestamp.toLong()
                is String -> rawTimestamp.toLongOrNull() ?: 0L
                else -> 0L
            }

            return ItemReport(
                itemId = itemId,
                userId = userId,
                title = title,
                description = description,
                category = category,
                status = status,
                imageUrl = imageUrl,
                location = location,
                latitude = latitude,
                longitude = longitude,
                timestamp = timestamp
            )
        }
    }
}
