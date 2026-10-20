package com.group3.lostandfound.repository

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.group3.lostandfound.model.ItemReport
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map

/**
 * Clean Architecture Repository providing real-time item listing, dynamic Firebase queries,
 * client-side Flow filtering, and single-item live status observers with null-safe snapshot parsing.
 */
class ExploreRepository {

    private val db = FirebaseDatabase.getInstance().getReference("items")

    /**
     * Observes the entire /items node in real time.
     * Parses each child snapshot null-safely.
     */
    fun getItemsRealtime(): Flow<List<ItemReport>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val items = mutableListOf<ItemReport>()
                for (child in snapshot.children) {
                    try {
                        val report = ItemReport.fromSnapshot(child)
                        if (report.itemId.isNotEmpty() || report.title.isNotEmpty()) {
                            items.add(report)
                        }
                    } catch (e: Exception) {
                        // Prevent individual snapshot corruption from breaking the stream
                    }
                }
                // Sort newest first
                items.sortByDescending { it.timestamp }
                trySend(items)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        db.addValueEventListener(listener)
        awaitClose { db.removeEventListener(listener) }
    }

    /**
     * Firebase dynamic query using `orderByChild("category")` equal to the specified category.
     */
    fun getItemsByCategory(category: String): Flow<List<ItemReport>> = callbackFlow {
        val query = if (category.isNotEmpty() && category != "ALL") {
            db.orderByChild("category").equalTo(category)
        } else {
            db.orderByChild("timestamp")
        }

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val items = mutableListOf<ItemReport>()
                for (child in snapshot.children) {
                    try {
                        items.add(ItemReport.fromSnapshot(child))
                    } catch (e: Exception) {
                        // Skip corrupted records
                    }
                }
                items.sortByDescending { it.timestamp }
                trySend(items)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        query.addValueEventListener(listener)
        awaitClose { query.removeEventListener(listener) }
    }

    /**
     * Live client-side flow filtering for text matching on title/description and status filters.
     */
    fun getItemsFiltered(
        query: String = "",
        status: String = "ALL",
        category: String = ""
    ): Flow<List<ItemReport>> = getItemsRealtime().map { items ->
        val cleanQuery = query.trim().lowercase()
        items.filter { item ->
            val matchesQuery = if (cleanQuery.isNotEmpty()) {
                item.title.lowercase().contains(cleanQuery) ||
                        item.description.lowercase().contains(cleanQuery)
            } else true

            val matchesStatus = when (status.uppercase()) {
                "ALL" -> true
                "LOST" -> item.status.equals("LOST", ignoreCase = true)
                "FOUND" -> item.status.equals("FOUND", ignoreCase = true)
                "PETS" -> item.category.equals("Pets & Animals", ignoreCase = true) ||
                        item.category.contains("Pet", ignoreCase = true)
                else -> item.status.equals(status, ignoreCase = true)
            }

            val matchesCategory = if (category.isNotEmpty() && category != "ALL") {
                item.category.equals(category, ignoreCase = true)
            } else true

            matchesQuery && matchesStatus && matchesCategory
        }
    }

    /**
     * Read single report object from `/items/{itemId}` using `addValueEventListener` for live status updates.
     */
    fun getItemDetails(itemId: String): Flow<ItemReport?> = callbackFlow {
        val ref = db.child(itemId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val report = ItemReport.fromSnapshot(snapshot)
                    trySend(report)
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
}
