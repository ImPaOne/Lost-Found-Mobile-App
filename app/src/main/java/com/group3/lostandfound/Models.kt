package com.group3.lostandfound

/**
 * Data Models for Lost and Found System, Team Developer Prompts,
 * and Admin Moderation Engine.
 * Matches production Firebase Realtime Database schema requirements.
 */

// 1. Pawan Tharusha (GAHNDSE261F-011): User Data Model for /users/{uid}
data class User(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val preferredLanguage: String = "en",
    val isTrustedFinder: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

// Result state wrapper for Clean Architecture & Repository operations
sealed class ResultState<out T> {
    object Loading : ResultState<Nothing>()
    data class Success<out T>(val data: T) : ResultState<T>()
    data class Error(val message: String) : ResultState<Nothing>()
}

// 2. Anuththara Wickramasekara (GAHNDSE261F-009): Analytics & Admin Models
data class AnalyticsSummary(
    val totalReports: Int = 0,
    val resolvedReports: Int = 0,
    val userCount: Int = 0,
    val flaggedItemsCount: Int = 0
)

data class AdminAction(
    val actionId: String = "",
    val adminUid: String = "",
    val targetId: String = "",
    val actionType: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

// 3. Pabasara Madhawa (GAHNDSE261F-010): Item Report model for /items/{itemId}
data class ReportItem(
    val itemId: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "Electronics",
    val location: String = "Colombo 03",
    val status: String = "PENDING", // PENDING, APPROVED, RESOLVED, REJECTED, LOST, FOUND
    val isFlagged: Boolean = false,
    val flagReason: String = "",
    val userId: String = "",
    val authorName: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val imageUrl: String = ""
)

// User Moderation model for /users/{uid}
data class UserData(
    val uid: String = "",
    val email: String = "",
    val fullName: String = "",
    val role: String = "USER", // "ADMIN" or "USER"
    val isBanned: Boolean = false,
    val trustScore: Int = 85
)

// 4. Yasindu Niroshan (GAHNDSE261F-015): Map, Rewards & Notifications Models
data class MapMarkerItem(
    val itemId: String = "",
    val title: String = "",
    val status: String = "LOST",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)

data class RewardOffer(
    val rewardId: String = "",
    val itemId: String = "",
    val amount: Double = 0.0,
    val currency: String = "LKR",
    val description: String = ""
)

data class NotificationItem(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val read: Boolean = false
)
