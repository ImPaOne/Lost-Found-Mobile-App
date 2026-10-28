package com.group3.lostandfound

import com.google.firebase.database.DataSnapshot

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
    val flaggedItemsCount: Int = 0,
    val pendingReviewCount: Int = 0,
    val todayReportsCount: Int = 0,
    val categoryDistribution: Map<String, Int> = emptyMap(),
    val colomboStats: Pair<Int, Int> = Pair(0, 0), // (total, resolved)
    val kandyStats: Pair<Int, Int> = Pair(0, 0),
    val galleStats: Pair<Int, Int> = Pair(0, 0),
    val heatMapPoints: List<MapMarkerItem> = emptyList(),
    val recentLogs: List<AdminAction> = emptyList()
)

data class AdminAction(
    val actionId: String = "",
    val adminUid: String = "",
    val targetId: String = "",
    val actionType: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

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
) {
    companion object {
        fun fromSnapshot(child: DataSnapshot): ReportItem {
            return try {
                val key = child.key.orEmpty()
                val itemId = child.child("itemId").getValue(String::class.java)?.takeIf { it.isNotEmpty() } ?: key
                val title = child.child("title").getValue(String::class.java).orEmpty()
                val description = child.child("description").getValue(String::class.java).orEmpty()
                val category = child.child("category").getValue(String::class.java)?.takeIf { it.isNotEmpty() } ?: "Electronics"
                val location = child.child("location").getValue(String::class.java)?.takeIf { it.isNotEmpty() } ?: "Colombo 03"
                val status = child.child("status").getValue(String::class.java)?.takeIf { it.isNotEmpty() } ?: "LOST"

                val rawFlagged = child.child("isFlagged").value
                val isFlagged = when (rawFlagged) {
                    is Boolean -> rawFlagged
                    is String -> rawFlagged.toBoolean()
                    else -> false
                }

                val flagReason = child.child("flagReason").getValue(String::class.java).orEmpty()
                val userId = child.child("userId").getValue(String::class.java).orEmpty()
                val authorName = child.child("authorName").getValue(String::class.java)?.takeIf { it.isNotEmpty() } ?: "Community Member"

                val rawTime = child.child("timestamp").value
                val timestamp = when (rawTime) {
                    is Number -> rawTime.toLong()
                    is String -> rawTime.toLongOrNull() ?: System.currentTimeMillis()
                    else -> System.currentTimeMillis()
                }
                val imageUrl = child.child("imageUrl").getValue(String::class.java).orEmpty()

                ReportItem(
                    itemId = itemId,
                    title = title,
                    description = description,
                    category = category,
                    location = location,
                    status = status,
                    isFlagged = isFlagged,
                    flagReason = flagReason,
                    userId = userId,
                    authorName = authorName,
                    timestamp = timestamp,
                    imageUrl = imageUrl
                )
            } catch (e: Exception) {
                ReportItem()
            }
        }
    }
}

// User Moderation model for /users/{uid}
data class UserData(
    val uid: String = "",
    val email: String = "",
    val fullName: String = "",
    val phone: String = "",
    val password: String = "",
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
    val read: Boolean = false,
    val type: String = "",
    val itemId: String = "",
    val conversationId: String = "",
    val senderUserId: String = "",
    val senderName: String = ""
)

data class ChatMessage(
    val chatMessageId: String = "",
    val senderUserId: String = "",
    val recipientUserId: String = "",
    val chatMessageTextContent: String = "",
    val messageSentTimestamp: Long = 0L,
    val imageUrl: String = "",
    val replyToMessageId: String = "",
    val replyToSenderName: String = "",
    val replyToText: String = "",
    val isDeletedForEveryone: Boolean = false,
    val deletedForUserIds: List<String> = emptyList()
) {
    companion object {
        fun fromSnapshot(child: DataSnapshot): ChatMessage {
            return try {
                val key = child.key.orEmpty()
                val id = child.child("chatMessageId").getValue(String::class.java)?.takeIf { it.isNotEmpty() } ?: key
                val sender = child.child("senderUserId").getValue(String::class.java)
                    ?: child.child("senderId").getValue(String::class.java)
                    ?: ""
                val recipient = child.child("recipientUserId").getValue(String::class.java)
                    ?: child.child("recipientId").getValue(String::class.java)
                    ?: ""
                val text = child.child("chatMessageTextContent").getValue(String::class.java)
                    ?: child.child("message").getValue(String::class.java)
                    ?: child.child("text").getValue(String::class.java)
                    ?: ""
                val imgUrl = child.child("imageUrl").getValue(String::class.java)
                    ?: child.child("image").getValue(String::class.java)
                    ?: ""
                val replyId = child.child("replyToMessageId").getValue(String::class.java).orEmpty()
                val replySender = child.child("replyToSenderName").getValue(String::class.java).orEmpty()
                val replyText = child.child("replyToText").getValue(String::class.java).orEmpty()
                val isDeleted = child.child("isDeletedForEveryone").getValue(Boolean::class.java) ?: false
                val deletedList = mutableListOf<String>()
                child.child("deletedForUserIds").children.forEach { snap ->
                    snap.key?.let { deletedList.add(it) }
                }

                val rawTime = child.child("messageSentTimestamp").value
                    ?: child.child("timestamp").value
                val time = when (rawTime) {
                    is Number -> rawTime.toLong()
                    is String -> rawTime.toLongOrNull() ?: System.currentTimeMillis()
                    else -> System.currentTimeMillis()
                }
                ChatMessage(
                    chatMessageId = id,
                    senderUserId = sender,
                    recipientUserId = recipient,
                    chatMessageTextContent = text,
                    messageSentTimestamp = time,
                    imageUrl = imgUrl,
                    replyToMessageId = replyId,
                    replyToSenderName = replySender,
                    replyToText = replyText,
                    isDeletedForEveryone = isDeleted,
                    deletedForUserIds = deletedList
                )
            } catch (e: Exception) {
                ChatMessage()
            }
        }
    }
}

data class ConversationMetadata(
    val chatConversationId: String = "",
    val otherUserId: String = "",
    val otherUserName: String = "",
    val lastMessageTextContent: String = "",
    val lastUpdatedTimestamp: Long = 0L,
    val relatedItemId: String = "",
    val relatedItemTitle: String = "",
    val lastSenderId: String = "",
    val unreadCount: Int = 0,
    val isResolved: Boolean = false,
    val itemImageUrl: String = "",
    val itemType: String = "LOST",
    val isOutgoingRead: Boolean = false
)

