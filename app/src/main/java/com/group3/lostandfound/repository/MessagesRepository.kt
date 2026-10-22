package com.group3.lostandfound.repository

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.group3.lostandfound.ConversationMetadata
import com.group3.lostandfound.FirebaseManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * MessagesRepository streams user conversations from Firebase Realtime Database.
 */
class MessagesRepository(
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance(FirebaseManager.DATABASE_URL)
) {

    /**
     * Streams conversation threads for a specific user ID.
     */
    fun streamUserConversations(userId: String): Flow<List<ConversationMetadata>> = callbackFlow {
        if (userId.isEmpty()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val ref = database.getReference("user_conversations").child(userId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<ConversationMetadata>()
                for (child in snapshot.children) {
                    try {
                        val conv = child.getValue(ConversationMetadata::class.java)
                        if (conv != null && conv.chatConversationId.isNotEmpty()) {
                            list.add(conv)
                        } else {
                            val id = child.child("chatConversationId").getValue(String::class.java) ?: child.key.orEmpty()
                            val otherUser = child.child("otherUserId").getValue(String::class.java).orEmpty()
                            val otherName = child.child("otherUserName").getValue(String::class.java).orEmpty()
                            val lastMsg = child.child("lastMessageTextContent").getValue(String::class.java).orEmpty()
                            val timestamp = child.child("lastUpdatedTimestamp").getValue(Long::class.java) ?: 0L
                            val itemId = child.child("relatedItemId").getValue(String::class.java).orEmpty()
                            if (id.isNotEmpty()) {
                                list.add(
                                    ConversationMetadata(
                                        chatConversationId = id,
                                        otherUserId = otherUser,
                                        otherUserName = otherName,
                                        lastMessageTextContent = lastMsg,
                                        lastUpdatedTimestamp = timestamp,
                                        relatedItemId = itemId
                                    )
                                )
                            }
                        }
                    } catch (e: Exception) {
                        // Skip corrupted item
                    }
                }
                list.sortByDescending { it.lastUpdatedTimestamp }
                trySend(list)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }
}
