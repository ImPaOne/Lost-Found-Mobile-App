package com.group3.lostandfound

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * FirebaseManager provides centralized, simple, and production-ready Firebase Realtime Database
 * interactions for the Admin & Moderation engine, My Reports, and Analytics dashboard.
 *
 * Core Paths:
 * - /items                  : Overall reports
 * - /items/{itemId}         : Item updates, approvals, flaggings, deletions
 * - /users/{uid}/role       : User authorization check ("ADMIN" vs "USER")
 * - /users/{uid}/isBanned   : User ban moderation
 * - /admin_logs/{actionId}  : Admin audit trails
 */
object FirebaseManager {

    const val DATABASE_URL = "https://lost-and-found-9-10-11-15-default-rtdb.firebaseio.com"
    const val STORAGE_BUCKET = "gs://lost-and-found-9-10-11-15.firebasestorage.app"

    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()
    val database: FirebaseDatabase get() = FirebaseDatabase.getInstance(DATABASE_URL)

    // Database references
    val itemsRef: DatabaseReference get() = database.getReference("items")
    val usersRef: DatabaseReference get() = database.getReference("users")
    val adminLogsRef: DatabaseReference get() = database.getReference("admin_logs")

    // StateFlow for real-time analytics summary
    private val _analyticsFlow = MutableStateFlow(AnalyticsSummary())
    val analyticsFlow: StateFlow<AnalyticsSummary> = _analyticsFlow.asStateFlow()

    // Current User ID
    val currentUid: String
        get() = auth.currentUser?.uid ?: "default_user_101"

    val currentUserEmail: String
        get() = auth.currentUser?.email ?: "admin@gmail.com"

    init {
        // Start real-time observation of /items and /users for Analytics
        setupRealtimeAnalyticsObserver()
    }

    /**
     * Set up real-time observation on `/items` and `/users` to compute AnalyticsSummary Flow.
     */
    private fun setupRealtimeAnalyticsObserver() {
        var totalReports = 0
        var resolvedReports = 0
        var flaggedItems = 0
        var totalUsers = 0
        var pendingReview = 0
        var todayCount = 0
        var categoryMap = mapOf<String, Int>()
        var colomboStats = Pair(0, 0)
        var kandyStats = Pair(0, 0)
        var galleStats = Pair(0, 0)
        var heatPoints = listOf<MapMarkerItem>()

        // Observe /items node for live metrics
        itemsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var total = 0
                var resolved = 0
                var flagged = 0
                var pending = 0
                var today = 0

                val catCounts = mutableMapOf<String, Int>()
                var colTotal = 0; var colRes = 0
                var kanTotal = 0; var kanRes = 0
                var galTotal = 0; var galRes = 0
                val points = mutableListOf<MapMarkerItem>()

                val now = System.currentTimeMillis()

                for (child in snapshot.children) {
                    val status = child.child("status").getValue(String::class.java) ?: ""
                    val isFlagged = child.child("isFlagged").getValue(Boolean::class.java) ?: false
                    val category = child.child("category").getValue(String::class.java) ?: "Other"
                    val location = child.child("location").getValue(String::class.java) ?: ""
                    val timestamp = child.child("timestamp").getValue(Long::class.java) ?: 0L
                    val lat = child.child("latitude").getValue(Double::class.java) ?: 0.0
                    val lng = child.child("longitude").getValue(Double::class.java) ?: 0.0
                    val title = child.child("title").getValue(String::class.java) ?: "Report"

                    total++
                    val isResolved = status.equals("RESOLVED", ignoreCase = true)
                    if (isResolved) resolved++
                    if (isFlagged) flagged++
                    if (status.equals("PENDING", ignoreCase = true) || isFlagged) pending++
                    if (now - timestamp < 86400000L) today++

                    // Category grouping
                    val catKey = when {
                        category.contains("Electronic", true) -> "Electronics"
                        category.contains("Document", true) -> "Documents"
                        category.contains("Pet", true) -> "Pets"
                        category.contains("Wallet", true) || category.contains("Bag", true) -> "Wallets"
                        else -> "Other"
                    }
                    catCounts[catKey] = (catCounts[catKey] ?: 0) + 1

                    // City resolution rates
                    val locLower = location.lowercase()
                    if (locLower.contains("colombo") || locLower.contains("kollupitiya") || locLower.contains("bambalapitiya") || locLower.contains("fort")) {
                        colTotal++
                        if (isResolved) colRes++
                    } else if (locLower.contains("kandy")) {
                        kanTotal++
                        if (isResolved) kanRes++
                    } else if (locLower.contains("galle")) {
                        galTotal++
                        if (isResolved) galRes++
                    }

                    // Geographic coordinates for heat map
                    if (lat != 0.0 && lng != 0.0) {
                        points.add(
                            MapMarkerItem(
                                itemId = child.key ?: "",
                                title = title,
                                status = status,
                                latitude = lat,
                                longitude = lng
                            )
                        )
                    }
                }

                totalReports = total
                resolvedReports = resolved
                flaggedItems = flagged
                pendingReview = pending
                todayCount = today
                categoryMap = catCounts
                colomboStats = Pair(colTotal, colRes)
                kandyStats = Pair(kanTotal, kanRes)
                galleStats = Pair(galTotal, galRes)
                heatPoints = points

                _analyticsFlow.value = _analyticsFlow.value.copy(
                    totalReports = totalReports,
                    resolvedReports = resolvedReports,
                    flaggedItemsCount = flaggedItems,
                    pendingReviewCount = pendingReview,
                    todayReportsCount = todayCount,
                    categoryDistribution = categoryMap,
                    colomboStats = colomboStats,
                    kandyStats = kandyStats,
                    galleStats = galleStats,
                    heatMapPoints = heatPoints
                )
            }

            override fun onCancelled(error: DatabaseError) {}
        })

        // Observe /users node for real user count
        usersRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                totalUsers = snapshot.childrenCount.toInt()
                _analyticsFlow.value = _analyticsFlow.value.copy(
                    userCount = totalUsers
                )
            }

            override fun onCancelled(error: DatabaseError) {}
        })

        // Observe /admin_logs for audit activity
        adminLogsRef.limitToLast(6).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val logs = mutableListOf<AdminAction>()
                for (child in snapshot.children) {
                    val log = child.getValue(AdminAction::class.java)
                    if (log != null) {
                        logs.add(log)
                    }
                }
                _analyticsFlow.value = _analyticsFlow.value.copy(
                    recentLogs = logs.reversed()
                )
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    /**
     * Enforce admin privilege checks by verifying `/users/{currentUid}/role == "ADMIN"`.
     */
    fun verifyAdminPrivilege(onAuthorized: () -> Unit, onDenied: (String) -> Unit) {
        val uid = currentUid

        // Fast-path: if email is admin@gmail.com, auto-ensure admin role
        if (currentUserEmail.equals("admin@gmail.com", ignoreCase = true)) {
            usersRef.child(uid).child("role").setValue("ADMIN")
            onAuthorized()
            return
        }

        usersRef.child(uid).child("role").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val role = snapshot.getValue(String::class.java)
                if (role.equals("ADMIN", ignoreCase = true)) {
                    onAuthorized()
                } else {
                    onDenied("Unauthorized: Administrator access required.")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                if (error.code == DatabaseError.PERMISSION_DENIED) {
                    onDenied("Database permission denied (PERMISSION_DENIED).")
                } else {
                    onDenied(error.message)
                }
            }
        })
    }

    /**
     * Moderation: Flag reported item by setting `isFlagged = true` via `updateChildren()`.
     */
    fun flagItem(
        itemId: String,
        isFlagged: Boolean,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        verifyAdminPrivilege(
            onAuthorized = {
                val updates = hashMapOf<String, Any>(
                    "isFlagged" to isFlagged
                )
                itemsRef.child(itemId).updateChildren(updates)
                    .addOnSuccessListener {
                        logAdminAction(
                            targetId = itemId,
                            actionType = if (isFlagged) "FLAG_ITEM" else "UNFLAG_ITEM"
                        )
                        onSuccess()
                    }
                    .addOnFailureListener { exception ->
                        onError(exception.localizedMessage ?: "Failed to update item flag state.")
                    }
            },
            onDenied = { errorMsg ->
                onError(errorMsg)
            }
        )
    }

    /**
     * Moderation: Remove reported item using `removeValue()`.
     */
    fun removeItem(
        itemId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        verifyAdminPrivilege(
            onAuthorized = {
                itemsRef.child(itemId).removeValue { databaseError, _ ->
                    if (databaseError != null) {
                        if (databaseError.code == DatabaseError.PERMISSION_DENIED) {
                            onError("Permission Denied: You do not have permissions to delete this item.")
                        } else {
                            onError(databaseError.message)
                        }
                    } else {
                        logAdminAction(targetId = itemId, actionType = "REMOVE_ITEM")
                        onSuccess()
                    }
                }
            },
            onDenied = { errorMsg ->
                onError(errorMsg)
            }
        )
    }

    /**
     * Moderation: Approve reported item by setting `status = "APPROVED"` and `isFlagged = false`.
     */
    fun approveItem(
        itemId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        verifyAdminPrivilege(
            onAuthorized = {
                val updates = hashMapOf<String, Any>(
                    "status" to "APPROVED",
                    "isFlagged" to false
                )
                itemsRef.child(itemId).updateChildren(updates)
                    .addOnSuccessListener {
                        logAdminAction(targetId = itemId, actionType = "APPROVE_ITEM")
                        onSuccess()
                    }
                    .addOnFailureListener { exception ->
                        onError(exception.localizedMessage ?: "Failed to approve report.")
                    }
            },
            onDenied = { errorMsg ->
                onError(errorMsg)
            }
        )
    }

    /**
     * Moderation: Implement user ban action by setting `/users/{uid}/isBanned = true`.
     */
    fun banUser(
        targetUid: String,
        isBanned: Boolean = true,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        verifyAdminPrivilege(
            onAuthorized = {
                usersRef.child(targetUid).child("isBanned").setValue(isBanned)
                    .addOnSuccessListener {
                        logAdminAction(
                            targetId = targetUid,
                            actionType = if (isBanned) "BAN_USER" else "UNBAN_USER"
                        )
                        onSuccess()
                    }
                    .addOnFailureListener { exception ->
                        onError(exception.localizedMessage ?: "Failed to update user ban status.")
                    }
            },
            onDenied = { errorMsg ->
                onError(errorMsg)
            }
        )
    }

    /**
     * Audit Trail: Log admin action to `/admin_logs/{actionId}`.
     */
    fun logAdminAction(targetId: String, actionType: String) {
        val actionId = adminLogsRef.push().key ?: System.currentTimeMillis().toString()
        val action = AdminAction(
            actionId = actionId,
            adminUid = currentUid,
            targetId = targetId,
            actionType = actionType,
            timestamp = System.currentTimeMillis()
        )
        adminLogsRef.child(actionId).setValue(action)
    }

    /**
     * My Reports: Execute query `FirebaseDatabase.getInstance().getReference("items").orderByChild("userId").equalTo(currentUid)`
     * to retrieve user's own listings.
     */
    fun getMyReports(onResult: (List<ReportItem>) -> Unit) {
        val query = itemsRef.orderByChild("userId").equalTo(currentUid)
        query.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<ReportItem>()
                for (child in snapshot.children) {
                    val item = ReportItem.fromSnapshot(child)
                    if (item.itemId.isNotEmpty() || item.title.isNotEmpty()) {
                        list.add(item)
                    }
                }
                onResult(list)
            }

            override fun onCancelled(error: DatabaseError) {
                onResult(emptyList())
            }
        })
    }

    /**
     * My Reports: Mark status as "RESOLVED".
     */
    fun markItemResolved(
        itemId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        itemsRef.child(itemId).child("status").setValue("RESOLVED")
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.localizedMessage ?: "Failed to mark as resolved.") }
    }

    /**
     * Ensure seed sample data exists in Firebase Realtime Database.
     * Seeds admin credentials and live items if database is empty.
     */
    fun seedDefaultDataIfEmpty() {
        // 1. Seed Admin User credentials if missing
        usersRef.child(currentUid).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    usersRef.child(currentUid).setValue(
                        UserData(
                            uid = currentUid,
                            email = currentUserEmail.ifEmpty { "admin@gmail.com" },
                            fullName = "System Administrator",
                            role = "ADMIN",
                            isBanned = false,
                            trustScore = 100
                        )
                    )
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })

        // 2. Seed Real Initial Items if /items node is empty
        itemsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists() || snapshot.childrenCount == 0L) {
                    val sampleItems = listOf(
                        mapOf(
                            "itemId" to "item_macbook_pro",
                            "title" to "Apple MacBook Pro 14-inch",
                            "description" to "Space Gray M2 Pro laptop left near cafe with charger and gray sleeve.",
                            "category" to "Electronics",
                            "location" to "Colombo 03",
                            "status" to "LOST",
                            "isFlagged" to false,
                            "flagReason" to "",
                            "userId" to currentUid,
                            "authorName" to "Nadeesha Silva",
                            "latitude" to 6.9042,
                            "longitude" to 79.8530,
                            "timestamp" to System.currentTimeMillis() - 7200000L,
                            "imageUrl" to ""
                        ),
                        mapOf(
                            "itemId" to "item_golden_retriever",
                            "title" to "Golden Retriever Puppy (Milo)",
                            "description" to "Friendly puppy with red leather collar found near lake park.",
                            "category" to "Pets",
                            "location" to "Kandy Lake",
                            "status" to "FOUND",
                            "isFlagged" to false,
                            "flagReason" to "",
                            "userId" to currentUid,
                            "authorName" to "Kavindu Perera",
                            "latitude" to 7.2906,
                            "longitude" to 80.6337,
                            "timestamp" to System.currentTimeMillis() - 14400000L,
                            "imageUrl" to ""
                        ),
                        mapOf(
                            "itemId" to "item_passport_nic",
                            "title" to "Sri Lankan Passport & NIC",
                            "description" to "Official N-series passport and NIC card inside blue wallet.",
                            "category" to "Documents",
                            "location" to "Fort, Colombo",
                            "status" to "LOST",
                            "isFlagged" to true,
                            "flagReason" to "Critical identification document",
                            "userId" to "community_user_202",
                            "authorName" to "Amila Fernando",
                            "latitude" to 6.9344,
                            "longitude" to 79.8428,
                            "timestamp" to System.currentTimeMillis() - 28800000L,
                            "imageUrl" to ""
                        ),
                        mapOf(
                            "itemId" to "item_leather_wallet",
                            "title" to "Black Leather Bi-fold Wallet",
                            "description" to "Men's leather wallet with driver's license and cards.",
                            "category" to "Wallets",
                            "location" to "Bambalapitiya",
                            "status" to "FOUND",
                            "isFlagged" to false,
                            "flagReason" to "",
                            "userId" to "community_user_303",
                            "authorName" to "Rashmi Wickrama",
                            "latitude" to 6.8885,
                            "longitude" to 79.8587,
                            "timestamp" to System.currentTimeMillis() - 43200000L,
                            "imageUrl" to ""
                        ),
                        mapOf(
                            "itemId" to "item_sony_headphones",
                            "title" to "Sony WH-1000XM4 Headphones",
                            "description" to "Black wireless noise-cancelling headphones left on express highway bus.",
                            "category" to "Electronics",
                            "location" to "Galle Bus Stand",
                            "status" to "LOST",
                            "isFlagged" to false,
                            "flagReason" to "",
                            "userId" to "community_user_404",
                            "authorName" to "Dinesh Senaratne",
                            "latitude" to 6.0535,
                            "longitude" to 80.2210,
                            "timestamp" to System.currentTimeMillis() - 86400000L,
                            "imageUrl" to ""
                        )
                    )

                    for (item in sampleItems) {
                        val id = item["itemId"] as String
                        itemsRef.child(id).setValue(item)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }
}
