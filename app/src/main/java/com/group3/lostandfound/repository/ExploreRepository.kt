package com.group3.lostandfound.repository

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.group3.lostandfound.FirebaseManager
import com.group3.lostandfound.model.ItemReport
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map

// Repository handles communication between the ViewModel and Firebase.
class ExploreRepository {

    // Reference to the "items" node in Firebase.
    private val db = FirebaseDatabase.getInstance(FirebaseManager.DATABASE_URL).getReference("items")

    // Gets all item reports and keeps listening for database changes.
    fun getItemsRealtime(): Flow<List<ItemReport>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val items = mutableListOf<ItemReport>()
                // Convert Firebase records into ItemReport objects.
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

        // Start listening for Firebase changes.
        db.addValueEventListener(listener)
        // Remove the listener when the Flow is cancelled.
        awaitClose { db.removeEventListener(listener) }
    }

    // Gets item reports by category, such as a specific category or ALL.
    fun getItemsByCategory(category: String): Flow<List<ItemReport>> = callbackFlow {
        // Build a Firebase query when a specific category is selected.
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
        // Clean up the Firebase query listener.
        awaitClose { query.removeEventListener(listener) }
    }

    // Filters the live item list by search text, status, and optional category.
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

    // Retrieves one item by its Firebase key and listens for changes.
    fun getItemDetails(itemId: String): Flow<ItemReport?> = callbackFlow {
        // Get the database reference for one specific item.
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

        // Start listening for changes to this individual item.
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }
}
