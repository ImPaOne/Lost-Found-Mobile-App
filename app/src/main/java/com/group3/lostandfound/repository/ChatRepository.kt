package com.group3.lostandfound.repository

import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.group3.lostandfound.ChatMessage
import com.group3.lostandfound.ConversationMetadata
import com.group3.lostandfound.FirebaseManager
import com.group3.lostandfound.ResultState
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Production-ready Clean Architecture Repository for Real-Time Messaging.
 * Implements ChildEventListener wrapped in Kotlin callbackFlow for real-time streaming.
 * Uses descriptive, human-readable variable names as requested.
 */
class ChatRepository {

    private val firebaseDatabaseInstance: FirebaseDatabase =
        FirebaseDatabase.getInstance(FirebaseManager.DATABASE_URL)

    /**
     * Helper to compute a consistent conversation ID between two users for a specific item report.
     */
    fun generateChatConversationId(
        firstUserId: String,
        secondUserId: String,
        relatedItemId: String = ""
    ): String {
        val sortedUserPairIdentifier = if (firstUserId < secondUserId) {
            "${firstUserId}_$secondUserId"
        } else {
            "${secondUserId}_$firstUserId"
        }
        return if (relatedItemId.isNotEmpty()) {
            "conv_${sortedUserPairIdentifier}_$relatedItemId"
        } else {
            "conv_$sortedUserPairIdentifier"
        }
    }

    /**
     * Real-time Chat Streaming: Streams incoming ChatMessage instances using ChildEventListener
     * wrapped in a Kotlin callbackFlow for a specific chatConversationId.
     * Path: /chat_conversations/{chatConversationId}/messages/{chatMessageId}
     */
    fun streamChatMessages(chatConversationId: String): Flow<ChatMessage> = callbackFlow {
        val chatMessagesDatabaseReference = firebaseDatabaseInstance
            .getReference("chat_conversations")
            .child(chatConversationId)
            .child("messages")

        val childEventListener = object : ChildEventListener {
            override fun onChildAdded(dataSnapshot: DataSnapshot, previousChildName: String?) {
                try {
                    val incomingChatMessage = dataSnapshot.getValue(ChatMessage::class.java)
                    if (incomingChatMessage != null) {
                        trySend(incomingChatMessage)
                    }
                } catch (exception: Exception) {
                    // Silently handle parse errors to maintain continuous stream
                }
            }

            override fun onChildChanged(dataSnapshot: DataSnapshot, previousChildName: String?) {}

            override fun onChildRemoved(dataSnapshot: DataSnapshot) {}

            override fun onChildMoved(dataSnapshot: DataSnapshot, previousChildName: String?) {}

            override fun onCancelled(databaseError: DatabaseError) {
                close(databaseError.toException())
            }
        }

        chatMessagesDatabaseReference.addChildEventListener(childEventListener)

        awaitClose {
            chatMessagesDatabaseReference.removeEventListener(childEventListener)
        }
    }

    /**
     * Sends a new chat message via setValue() and updates conversation metadata for both
     * the sender and recipient under /user_conversations/{userId}/{chatConversationId}.
     */
    suspend fun sendChatMessage(
        chatConversationId: String,
        senderUserId: String,
        recipientUserId: String,
        chatMessageTextContent: String,
        relatedItemId: String = "",
        otherUserName: String = ""
    ): ResultState<ChatMessage> {
        return try {
            val messagesReference = firebaseDatabaseInstance
                .getReference("chat_conversations")
                .child(chatConversationId)
                .child("messages")

            val generatedMessagePushKey = messagesReference.push().key
                ?: "msg_${System.currentTimeMillis()}"

            val newChatMessage = ChatMessage(
                chatMessageId = generatedMessagePushKey,
                senderUserId = senderUserId,
                recipientUserId = recipientUserId,
                chatMessageTextContent = chatMessageTextContent,
                messageSentTimestamp = System.currentTimeMillis()
            )

            // 1. Store message under /chat_conversations/{chatConversationId}/messages/{chatMessageId}
            messagesReference.child(generatedMessagePushKey).setValue(newChatMessage).await()

            // 2. Update conversation metadata for sender under /user_conversations/{senderUserId}/{chatConversationId}
            val senderMetadataReference = firebaseDatabaseInstance
                .getReference("user_conversations")
                .child(senderUserId)
                .child(chatConversationId)

            val senderConversationMetadata = ConversationMetadata(
                chatConversationId = chatConversationId,
                otherUserId = recipientUserId,
                otherUserName = otherUserName,
                lastMessageTextContent = chatMessageTextContent,
                lastUpdatedTimestamp = System.currentTimeMillis(),
                relatedItemId = relatedItemId
            )
            senderMetadataReference.setValue(senderConversationMetadata).await()

            // 3. Update conversation metadata for recipient under /user_conversations/{recipientUserId}/{chatConversationId}
            val recipientMetadataReference = firebaseDatabaseInstance
                .getReference("user_conversations")
                .child(recipientUserId)
                .child(chatConversationId)

            val recipientConversationMetadata = ConversationMetadata(
                chatConversationId = chatConversationId,
                otherUserId = senderUserId,
                otherUserName = "Sender",
                lastMessageTextContent = chatMessageTextContent,
                lastUpdatedTimestamp = System.currentTimeMillis(),
                relatedItemId = relatedItemId
            )
            recipientMetadataReference.setValue(recipientConversationMetadata).await()

            ResultState.Success(newChatMessage)
        } catch (exception: Exception) {
            ResultState.Error(exception.localizedMessage ?: "Failed to send chat message")
        }
    }
}
