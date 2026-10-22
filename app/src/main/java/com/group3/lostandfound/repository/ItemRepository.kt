package com.group3.lostandfound.repository

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.group3.lostandfound.FirebaseManager
import com.group3.lostandfound.ResultState
import com.group3.lostandfound.model.ItemReport
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Centralized Item repository for reports management in MVVM architecture.
 */
class ItemRepository(
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance(FirebaseManager.DATABASE_URL)
) {
    private val itemsRef = database.getReference("items")

    /**
     * Streams all active items from /items in real time, sorted newest first.
     */
    fun streamAllItems(): Flow<List<ItemReport>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<ItemReport>()
                for (child in snapshot.children) {
                    try {
                        val report = ItemReport.fromSnapshot(child)
                        if (report.itemId.isNotEmpty() || report.title.isNotEmpty()) {
                            list.add(report)
                        }
                    } catch (e: Exception) {
                        // Safe skip
                    }
                }
                list.sortByDescending { it.timestamp }
                trySend(list)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        itemsRef.addValueEventListener(listener)
        awaitClose { itemsRef.removeEventListener(listener) }
    }

    /**
     * Streams reports created by a specific user.
     */
    fun streamUserReports(userId: String): Flow<List<ItemReport>> = callbackFlow {
        if (userId.isEmpty()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<ItemReport>()
                for (child in snapshot.children) {
                    try {
                        val report = ItemReport.fromSnapshot(child)
                        if (report.userId == userId) {
                            list.add(report)
                        }
                    } catch (e: Exception) {
                        // Safe skip
                    }
                }
                list.sortByDescending { it.timestamp }
                trySend(list)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        itemsRef.addValueEventListener(listener)
        awaitClose { itemsRef.removeEventListener(listener) }
    }

    /**
     * Streams a single item by ID.
     */
    fun streamItemById(itemId: String): Flow<ItemReport?> = callbackFlow {
        if (itemId.isEmpty()) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val ref = itemsRef.child(itemId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    trySend(ItemReport.fromSnapshot(snapshot))
                } else {
                    trySend(null)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    /**
     * Marks an item as RESOLVED.
     */
    suspend fun markItemResolved(itemId: String): ResultState<Unit> {
        return try {
            itemsRef.child(itemId).child("status").setValue("RESOLVED").await()
            ResultState.Success(Unit)
        } catch (e: Exception) {
            ResultState.Error(e.localizedMessage ?: "Failed to mark item as resolved.")
        }
    }

    /**
     * Deletes an item from /items.
     */
    suspend fun deleteItem(itemId: String): ResultState<Unit> {
        return try {
            itemsRef.child(itemId).removeValue().await()
            ResultState.Success(Unit)
        } catch (e: Exception) {
            ResultState.Error(e.localizedMessage ?: "Failed to delete item.")
        }
    }
}
