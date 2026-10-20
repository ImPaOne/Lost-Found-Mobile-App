package com.group3.lostandfound

/**
 * Data Models for Lost and Found System & Admin Moderation Engine.
 * Matches production Firebase Realtime Database schema requirements.
 */

// 1. Analytics Summary aggregated in real-time
data class AnalyticsSummary(
    val totalReports: Int = 0,
    val resolvedReports: Int = 0,
    val userCount: Int = 0,
    val flaggedItemsCount: Int = 0
)

// 2. Admin Audit Action model for /admin_logs/{actionId}
data class AdminAction(
    val actionId: String = "",
    val adminUid: String = "",
    val targetId: String = "",
    val actionType: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

// 3. Item Report model for /items/{itemId}
data class ReportItem(
    val itemId: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "Electronics",
    val location: String = "Colombo 03",
    val status: String = "PENDING", // PENDING, APPROVED, RESOLVED, REJECTED
    val isFlagged: Boolean = false,
    val flagReason: String = "",
    val userId: String = "",
    val authorName: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val imageUrl: String = ""
)

// 4. User Moderation model for /users/{uid}
data class UserData(
    val uid: String = "",
    val email: String = "",
    val fullName: String = "",
    val role: String = "USER", // "ADMIN" or "USER"
    val isBanned: Boolean = false,
    val trustScore: Int = 85
)
