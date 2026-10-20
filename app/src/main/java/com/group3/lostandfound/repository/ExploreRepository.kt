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

class ExploreRepository {

    private val db = FirebaseDatabase.getInstance().getReference("items")

    fun getItemsRealtime(): Flow<List<ItemReport>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val items = mutableListOf<ItemReport>()
                for (child in snapshot.children) {
                    val item = child.getValue(ItemReport::class.java)
                    if (item != null) {
                        items.add(item)
                    }
                }
                trySend(items)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        db.addValueEventListener(listener)
        awaitClose { db.removeEventListener(listener) }
    }

    fun getItemsFiltered(
        query: String = "",
        status: String = "ALL"
    ): Flow<List<ItemReport>> = getItemsRealtime().map { items ->
        items.filter { item ->
            val matchesQuery = if (query.isNotEmpty()) {
                item.title.contains(query, ignoreCase = true) || item.description.contains(query, ignoreCase = true)
            } else true
            val matchesStatus = if (status != "ALL") {
                item.status == status
            } else true
            
            matchesQuery && matchesStatus
        }
    }
    
    fun getItemDetails(itemId: String): Flow<ItemReport?> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val item = snapshot.getValue(ItemReport::class.java)
                trySend(item)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        val ref = db.child(itemId)
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }
}
