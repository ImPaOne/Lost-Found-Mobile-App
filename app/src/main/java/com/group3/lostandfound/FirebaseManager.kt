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
    var customUid: String = ""
    var customEmail: String = ""

    val currentUid: String
        get() = auth.currentUser?.uid?.takeIf { it.isNotEmpty() } ?: customUid

    val currentUserEmail: String
        get() = auth.currentUser?.email?.takeIf { it.isNotEmpty() } ?: customEmail

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
                    val resolvedLat: Double
                    val resolvedLng: Double
                    if (lat != 0.0 && lng != 0.0) {
                        resolvedLat = lat
                        resolvedLng = lng
                    } else {
                        val offset = (child.key.hashCode() % 30) * 0.003
                        when {
                            locLower.contains("kandy") || locLower.contains("peradeniya") -> {
                                resolvedLat = 7.2906 + offset
                                resolvedLng = 80.6337 + offset
                            }
                            locLower.contains("galle") || locLower.contains("unawatuna") -> {
                                resolvedLat = 6.0535 + offset
                                resolvedLng = 80.2210 + offset
                            }
                            locLower.contains("jaffna") -> {
                                resolvedLat = 9.6615 + offset
                                resolvedLng = 80.0255 + offset
                            }
                            locLower.contains("negombo") -> {
                                resolvedLat = 7.2008 + offset
                                resolvedLng = 79.8736 + offset
                            }
                            else -> {
                                // Default to Colombo metropolitan area with scatter
                                resolvedLat = 6.9271 + offset
                                resolvedLng = 79.8612 + offset
                            }
                        }
                    }

                    points.add(
                        MapMarkerItem(
                            itemId = child.key ?: "",
                            title = title,
                            status = status,
                            latitude = resolvedLat,
                            longitude = resolvedLng
                        )
                    )
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
     * Moderation: Edit inappropriate or fake post (Admin Functional Requirement 3.2 #3).
     */
    fun editItem(
        itemId: String,
        title: String,
        category: String,
        location: String,
        description: String,
        status: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        verifyAdminPrivilege(
            onAuthorized = {
                val updates = hashMapOf<String, Any>(
                    "title" to title,
                    "category" to category,
                    "location" to location,
                    "description" to description,
                    "status" to status
                )
                itemsRef.child(itemId).updateChildren(updates)
                    .addOnSuccessListener {
                        logAdminAction(itemId, "EDIT_POST")
                        onSuccess()
                    }
                    .addOnFailureListener {
                        onError(it.localizedMessage ?: "Failed to update item.")
                    }
            },
            onDenied = { onError(it) }
        )
    }

    /**
     * User Management: Verify trusted users and assign badges (Admin Functional Requirement 3.2 #5).
     */
    fun verifyAndBadgeUser(
        targetUid: String,
        isTrusted: Boolean,
        badgeName: String,
        trustScore: Int,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        verifyAdminPrivilege(
            onAuthorized = {
                val updates = hashMapOf<String, Any>(
                    "isTrustedFinder" to isTrusted,
                    "isVerified" to true,
                    "badge" to badgeName,
                    "trustScore" to trustScore,
                    "badges/${badgeName.replace(" ", "_")}" to true
                )
                usersRef.child(targetUid).updateChildren(updates)
                    .addOnSuccessListener {
                        logAdminAction(targetUid, "VERIFY_AND_BADGE")
                        onSuccess()
                    }
                    .addOnFailureListener {
                        onError(it.localizedMessage ?: "Failed to verify and assign badge.")
                    }
            },
            onDenied = { onError(it) }
        )
    }

    /**
     * System Maintenance: Backup Database as JSON (Admin Functional Requirement 3.2 #10).
     */
    fun backupDatabase(onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        verifyAdminPrivilege(
            onAuthorized = {
                database.reference.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        try {
                            val backupObj = org.json.JSONObject()
                            backupObj.put("backupTimestamp", System.currentTimeMillis())
                            backupObj.put("exportedBy", currentUserEmail)

                            val itemsObj = org.json.JSONObject()
                            snapshot.child("items").children.forEach { child ->
                                val map = child.value
                                if (map is Map<*, *>) {
                                    itemsObj.put(child.key.orEmpty(), org.json.JSONObject(map))
                                }
                            }
                            backupObj.put("items", itemsObj)

                            val usersObj = org.json.JSONObject()
                            snapshot.child("users").children.forEach { child ->
                                val map = child.value
                                if (map is Map<*, *>) {
                                    usersObj.put(child.key.orEmpty(), org.json.JSONObject(map))
                                }
                            }
                            backupObj.put("users", usersObj)

                            logAdminAction("DATABASE", "BACKUP_DATA")
                            onSuccess(backupObj.toString(2))
                        } catch (e: Exception) {
                            onError("Backup generation failed: ${e.message}")
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        onError(error.message)
                    }
                })
            },
            onDenied = { onError(it) }
        )
    }

    /**
     * System Maintenance: Restore Database from JSON (Admin Functional Requirement 3.2 #10).
     */
    fun restoreDatabase(jsonString: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        verifyAdminPrivilege(
            onAuthorized = {
                try {
                    val json = org.json.JSONObject(jsonString)
                    val itemsObj = json.optJSONObject("items")
                    if (itemsObj != null) {
                        val iter = itemsObj.keys()
                        while (iter.hasNext()) {
                            val key = iter.next()
                            val itemJson = itemsObj.getJSONObject(key)
                            val map = mutableMapOf<String, Any>()
                            val fieldKeys = itemJson.keys()
                            while (fieldKeys.hasNext()) {
                                val f = fieldKeys.next()
                                map[f] = itemJson.get(f)
                            }
                            itemsRef.child(key).updateChildren(map)
                        }
                        logAdminAction("DATABASE", "RESTORE_DATA")
                        onSuccess()
                    } else {
                        onError("Invalid backup format: 'items' node not found.")
                    }
                } catch (e: Exception) {
                    onError("Failed to restore backup: ${e.message}")
                }
            },
            onDenied = { onError(it) }
        )
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
        updateItemStatus(itemId, "RESOLVED", onSuccess, onError)
    }

    /**
     * Updates an item's status (e.g. "RESOLVED", "RETURNED", "LOST", "FOUND").
     */
    fun updateItemStatus(
        itemId: String,
        newStatus: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val updates = mapOf<String, Any>(
            "status" to newStatus,
            "resolvedTimestamp" to if (newStatus.equals("RESOLVED", ignoreCase = true) || newStatus.equals("RETURNED", ignoreCase = true)) System.currentTimeMillis() else 0L
        )
        itemsRef.child(itemId).updateChildren(updates)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.localizedMessage ?: "Failed to update status.") }
    }

    /**
     * Ensure seed sample data exists in Firebase Realtime Database.
     * Seeds admin credentials and live items if database is empty.
     */
    fun seedDefaultDataIfEmpty() {
        val uid = currentUid.ifEmpty { "admin_master_101" }

        // 1. Ensure admin account exists in /users
        val adminMasterUid = "admin_master_101"
        usersRef.child(adminMasterUid).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    usersRef.child(adminMasterUid).setValue(
                        UserData(
                            uid = adminMasterUid,
                            email = "admin@gmail.com",
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

        if (uid.isNotEmpty() && uid != adminMasterUid) {
            usersRef.child(uid).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) {
                        val email = currentUserEmail
                        val defaultName = if (email.isNotEmpty()) email.substringBefore("@").replaceFirstChar { it.uppercase() } else "User"
                        usersRef.child(uid).setValue(
                            UserData(
                                uid = uid,
                                email = email,
                                fullName = defaultName,
                                role = if (email.equals("admin@gmail.com", ignoreCase = true)) "ADMIN" else "USER",
                                isBanned = false,
                                trustScore = 100
                            )
                        )
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
        }

        // 2. Ensure /items has community listings for 100% working live analytics and heatmap
        itemsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists() || snapshot.childrenCount == 0L) {
                    val sampleItems = listOf(
                        mapOf(
                            "itemId" to "item_sample_01",
                            "title" to "MacBook Pro 16\" Space Gray",
                            "category" to "Electronics",
                            "location" to "Colombo Fort Railway Station",
                            "status" to "LOST",
                            "latitude" to 6.9319,
                            "longitude" to 79.8478,
                            "userId" to "sample_user_1",
                            "authorName" to "Kasun Perera",
                            "timestamp" to System.currentTimeMillis() - 86400000L * 2
                        ),
                        mapOf(
                            "itemId" to "item_sample_02",
                            "title" to "Sri Lankan National Identity Card (NIC)",
                            "category" to "Documents",
                            "location" to "Peradeniya Campus, Kandy",
                            "status" to "FOUND",
                            "latitude" to 7.2906,
                            "longitude" to 80.6337,
                            "userId" to "sample_user_2",
                            "authorName" to "Dilshan Silva",
                            "timestamp" to System.currentTimeMillis() - 86400000L
                        ),
                        mapOf(
                            "itemId" to "item_sample_03",
                            "title" to "Golden Retriever Puppy 'Buddy'",
                            "category" to "Pets",
                            "location" to "Galle Dutch Fort",
                            "status" to "LOST",
                            "latitude" to 6.0535,
                            "longitude" to 80.2210,
                            "userId" to "sample_user_3",
                            "authorName" to "Nimali Fernando",
                            "timestamp" to System.currentTimeMillis() - 43200000L
                        ),
                        mapOf(
                            "itemId" to "item_sample_04",
                            "title" to "Brown Leather Bifold Wallet & Driving License",
                            "category" to "Wallets",
                            "location" to "Liberty Plaza, Colombo 03",
                            "status" to "RESOLVED",
                            "latitude" to 6.9040,
                            "longitude" to 79.8530,
                            "userId" to "sample_user_4",
                            "authorName" to "Arjuna Ranatunga",
                            "timestamp" to System.currentTimeMillis() - 86400000L * 5
                        ),
                        mapOf(
                            "itemId" to "item_sample_05",
                            "title" to "Sony WH-1000XM5 Wireless Headphones",
                            "category" to "Electronics",
                            "location" to "Bandaranaike International Airport, Negombo",
                            "status" to "FOUND",
                            "latitude" to 7.1808,
                            "longitude" to 79.8836,
                            "userId" to "sample_user_5",
                            "authorName" to "Pradeep Kumara",
                            "timestamp" to System.currentTimeMillis() - 3600000L * 4
                        ),
                        mapOf(
                            "itemId" to "item_sample_06",
                            "title" to "Academic Certificates & Passport Folder",
                            "category" to "Documents",
                            "location" to "Kollupitiya Junction, Colombo",
                            "status" to "RESOLVED",
                            "latitude" to 6.9120,
                            "longitude" to 79.8510,
                            "userId" to "sample_user_6",
                            "authorName" to "Chamari Atapattu",
                            "timestamp" to System.currentTimeMillis() - 86400000L * 3
                        ),
                        mapOf(
                            "itemId" to "item_sample_07",
                            "title" to "Persian Cat with Red Collar",
                            "category" to "Pets",
                            "location" to "Kandy City Centre",
                            "status" to "FOUND",
                            "latitude" to 7.2930,
                            "longitude" to 80.6350,
                            "userId" to "sample_user_7",
                            "authorName" to "Sajith Warnapura",
                            "timestamp" to System.currentTimeMillis() - 86400000L * 4
                        ),
                        mapOf(
                            "itemId" to "item_sample_08",
                            "title" to "Honda Smart Car Key with Leather Fob",
                            "category" to "Other",
                            "location" to "Gampaha Bus Stand",
                            "status" to "LOST",
                            "latitude" to 7.0840,
                            "longitude" to 79.9940,
                            "userId" to "sample_user_8",
                            "authorName" to "Ramesh Mendis",
                            "timestamp" to System.currentTimeMillis() - 7200000L
                        )
                    )

                    sampleItems.forEach { itemMap ->
                        val id = itemMap["itemId"] as String
                        itemsRef.child(id).setValue(itemMap)
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }
}
