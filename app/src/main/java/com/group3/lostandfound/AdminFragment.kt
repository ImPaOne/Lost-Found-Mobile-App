package com.group3.lostandfound

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment

import android.widget.TextView
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.CheckBox
import android.content.ClipboardManager
import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.group3.lostandfound.service.FCMService
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * AdminFragment displays the operations overview, statistics, trend chart,
 * moderation queue banner, quick actions, and recent activity.
 * Built with clean human-written code and Firebase Realtime Database integration.
 */
class AdminFragment : Fragment() {

    private lateinit var totalReportsNumber: TextView
    private lateinit var activeCasesNumber: TextView
    private lateinit var resolvedCasesNumber: TextView
    private lateinit var totalUsersNumber: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_admin, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initStatViews(view)
        observeLiveFirebaseStats()
        verifyAdminAccess()

        val btnBack = view.findViewById<ImageView>(R.id.backButton)
        val btnBell = view.findViewById<ImageView>(R.id.notificationBell)
        val cardModQueue = view.findViewById<View>(R.id.moderationQueueCard)
        val btnReview = view.findViewById<View>(R.id.reviewReportsButton)
        val btnUsers = view.findViewById<View>(R.id.manageUsersButton)
        val btnAnnounce = view.findViewById<View>(R.id.announcementButton)

        // Handle the back button: return to the previous screen if available;
// otherwise, display a short dashboard message.
btnBack.setOnClickListener {
    if (parentFragmentManager.backStackEntryCount > 0) {
        parentFragmentManager.popBackStack()
    } else {
        Toast.makeText(requireContext(), "Foundly Administrator Dashboard", Toast.LENGTH_SHORT).show()
    }
}

// Open the Notifications screen and add the current screen to the back stack.
btnBell.setOnClickListener {
    parentFragmentManager.beginTransaction()
        .replace(R.id.screenBox, NotificationsFragment.newInstance())
        .addToBackStack(null)
        .commit()
}

// Open the Moderation screen when the moderation queue card is clicked.
cardModQueue.setOnClickListener {
    openModerationScreen()
}

// Open the Moderation screen when the Review Reports button is clicked.
btnReview.setOnClickListener {
    openModerationScreen()
}

// Navigate to the User Management screen.
btnUsers.setOnClickListener {
    parentFragmentManager.beginTransaction()
        .replace(R.id.screenBox, UserManagementFragment.newInstance())
        .addToBackStack(null)
        .commit()
}

// Open the announcement dialog when the button is tapped.
btnAnnounce.setOnClickListener {
    showPostAnnouncementDialog()
}

// Open the Admin Control Panel when the announcement button is long-pressed.
btnAnnounce.setOnLongClickListener {
    showAdminToolsDialog()
    true
}

// Create a reusable click listener that opens Analytics with fade animations.
val openAnalyticsScreen = View.OnClickListener {
    parentFragmentManager.beginTransaction()
        .setCustomAnimations(
            android.R.anim.fade_in,
            android.R.anim.fade_out,
            android.R.anim.fade_in,
            android.R.anim.fade_out
        )
        .replace(R.id.screenBox, AnalyticsFragment.newInstance())
        .addToBackStack(null)
        .commit()
}

// Open Analytics when the chart time-range badge is clicked, if the view exists.
view.findViewById<View>(R.id.chartTimeRangeBadge)?.setOnClickListener(openAnalyticsScreen)

// Open Analytics with fade animations when the trend graph is clicked.
view.findViewById<View>(R.id.trendGraph)?.setOnClickListener {
    parentFragmentManager.beginTransaction()
        .setCustomAnimations(
            android.R.anim.fade_in,
            android.R.anim.fade_out,
            android.R.anim.fade_in,
            android.R.anim.fade_out
        )
        .replace(R.id.screenBox, AnalyticsFragment.newInstance())
        .addToBackStack(null)
        .commit()
}

// Open Moderation when the total reports statistic is clicked.
view.findViewById<View>(R.id.totalReportsNumber)?.setOnClickListener {
    openModerationScreen()
}

// Open Moderation when the active cases statistic is clicked.
view.findViewById<View>(R.id.activeCasesNumber)?.setOnClickListener {
    openModerationScreen()
}

// Open Analytics when the resolved cases statistic is clicked.
view.findViewById<View>(R.id.resolvedCasesNumber)?.setOnClickListener {
    parentFragmentManager.beginTransaction()
        .replace(R.id.screenBox, AnalyticsFragment.newInstance())
        .addToBackStack(null)
        .commit()
}

// Open User Management when the total users statistic is clicked.
view.findViewById<View>(R.id.totalUsersNumber)?.setOnClickListener {
    parentFragmentManager.beginTransaction()
        .replace(R.id.screenBox, UserManagementFragment.newInstance())
        .addToBackStack(null)
        .commit()
}

// Open the Details screen when the approved activity item is clicked.
view.findViewById<View>(R.id.activityItemApproved)?.setOnClickListener {
    parentFragmentManager.beginTransaction()
        .replace(R.id.screenBox, DetailsFragment.newInstance())
        .addToBackStack(null)
        .commit()
}

// Open the Trust screen when the verification activity item is clicked.
view.findViewById<View>(R.id.activityItemVerification)?.setOnClickListener {
    parentFragmentManager.beginTransaction()
        .replace(R.id.screenBox, TrustFragment.newInstance())
        .addToBackStack(null)
        .commit()
}

// Open Moderation when the escalated activity item is clicked.
view.findViewById<View>(R.id.activityItemEscalated)?.setOnClickListener {
    openModerationScreen()
}

// Configure the bottom navigation bar for the Admin Dashboard.
setupBottomNav(view, "admin")
}

    private lateinit var dateSubtitle: TextView
    private lateinit var awaitingReviewText: TextView
    private lateinit var todayBadge: TextView

    private lateinit var activityItemApproved: View
    private lateinit var activityApprovedText: TextView
    private lateinit var activityApprovedTime: TextView

    private lateinit var activityItemVerification: View
    private lateinit var activityVerificationText: TextView
    private lateinit var activityVerificationTime: TextView

    private lateinit var activityItemEscalated: View
    private lateinit var activityEscalatedText: TextView
    private lateinit var activityEscalatedTime: TextView

    private fun initStatViews(view: View) {
        totalReportsNumber = view.findViewById(R.id.totalReportsNumber)
        activeCasesNumber = view.findViewById(R.id.activeCasesNumber)
        resolvedCasesNumber = view.findViewById(R.id.resolvedCasesNumber)
        totalUsersNumber = view.findViewById(R.id.totalUsersNumber)

        dateSubtitle = view.findViewById(R.id.dateSubtitle)
        awaitingReviewText = view.findViewById(R.id.awaitingReviewText)
        todayBadge = view.findViewById(R.id.todayBadge)

        activityItemApproved = view.findViewById(R.id.activityItemApproved)
        activityApprovedText = view.findViewById(R.id.activityApprovedText)
        activityApprovedTime = view.findViewById(R.id.activityApprovedTime)

        activityItemVerification = view.findViewById(R.id.activityItemVerification)
        activityVerificationText = view.findViewById(R.id.activityVerificationText)
        activityVerificationTime = view.findViewById(R.id.activityVerificationTime)

        activityItemEscalated = view.findViewById(R.id.activityItemEscalated)
        activityEscalatedText = view.findViewById(R.id.activityEscalatedText)
        activityEscalatedTime = view.findViewById(R.id.activityEscalatedTime)

        // Set real dynamic today date
       // Create a date formatter to display the day of the week and day/month.
        val sdf = java.text.SimpleDateFormat("EEEE, dd MMMM", java.util.Locale.US)

        // Set the current date as uppercase text in the date subtitle.
        dateSubtitle.text = sdf.format(java.util.Date()).uppercase()
    }

    private fun observeLiveFirebaseStats() {
        //Runs a coroutine linked to the Fragment's view lifecycle, so it is cancelled when that view's lifecycle ends.
        viewLifecycleOwner.lifecycleScope.launch {
            FirebaseManager.analyticsFlow.collectLatest { summary ->
                val activeCount = (summary.totalReports - summary.resolvedReports).coerceAtLeast(0)
    //Formats numbers with thousands separators, e.g. 1500 becomes 1,500.
                totalReportsNumber.text = String.format("%,d", summary.totalReports)
                activeCasesNumber.text = String.format("%,d", activeCount)
                resolvedCasesNumber.text = String.format("%,d", summary.resolvedReports)
                totalUsersNumber.text = String.format("%,d", summary.userCount)

                // Real moderation queue banner metrics
                awaitingReviewText.text = "${summary.pendingReviewCount} awaiting review"
                todayBadge.text = "+${summary.todayReportsCount} today"

                // Real Recent Activities from Firebase /admin_logs
                bindRecentActivityLogs(summary.recentLogs)
            }
        }
    }
// Displays the latest three admin activities on the dashboard.
    private fun bindRecentActivityLogs(logs: List<AdminAction>) {
        if (logs.isNotEmpty()) {
            val log1 = logs[0]
            activityItemApproved.visibility = View.VISIBLE
            activityApprovedText.text = "${formatActionName(log1.actionType)} · ${log1.targetId}"
            activityApprovedTime.text = formatRelativeTime(log1.timestamp)
        } else {
            activityApprovedText.text = "System initialized · Database active"
            activityApprovedTime.text = "now"
        }

        if (logs.size > 1) {
            val log2 = logs[1]
            activityItemVerification.visibility = View.VISIBLE
            activityVerificationText.text = "${formatActionName(log2.actionType)} · ${log2.targetId}"
            activityVerificationTime.text = formatRelativeTime(log2.timestamp)
        } else {
            activityItemVerification.visibility = View.GONE
        }

        if (logs.size > 2) {
            val log3 = logs[2]
            activityItemEscalated.visibility = View.VISIBLE
            activityEscalatedText.text = "${formatActionName(log3.actionType)} · ${log3.targetId}"
            activityEscalatedTime.text = formatRelativeTime(log3.timestamp)
        } else {
            activityItemEscalated.visibility = View.GONE
        }
    }

    // Convert an action type code into a readable activity name
    private fun formatActionName(actionType: String): String {
        return when (actionType) {
            "APPROVE_ITEM" -> "Report approved"
            "REMOVE_ITEM" -> "Report removed"
            "FLAG_ITEM" -> "Item flagged"
            "UNFLAG_ITEM" -> "Item unflagged"
            "BAN_USER" -> "User account banned"
            "UNBAN_USER" -> "User account unbanned"
            else -> actionType.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
        }
    }


    // Convert a timestamp into a relative time such as "now", "5m", "2h", or "3d".
    private fun formatRelativeTime(timestamp: Long): String {
        val diffMs = System.currentTimeMillis() - timestamp
        val mins = diffMs / 60000
        val hours = mins / 60
        val days = hours / 24
        return when {
            mins < 1 -> "now"
            mins < 60 -> "${mins}m"
            hours < 24 -> "${hours}h"
            else -> "${days}d"
        }
    }

    private fun verifyAdminAccess() {
        // Access the app's private SharedPreferences storage.
        val prefs = requireContext().getSharedPreferences("findit_prefs", android.content.Context.MODE_PRIVATE)
        val isAdminLocally = prefs.getBoolean("is_admin", false) || FirebaseManager.currentUserEmail.equals("admin@gmail.com", ignoreCase = true)
        if (isAdminLocally) return

        FirebaseManager.verifyAdminPrivilege(
            onAuthorized = {
                // Access granted
            },
            onDenied = { errorMsg ->
                if (isAdded) {
                    AlertDialog.Builder(requireContext())
                        .setTitle("Admin Security Alert")
                        .setMessage("$errorMsg\nPlease ensure you are logged in as admin@gmail.com.")
                        .setPositiveButton("OK", null)
                        .show()
                }
            }
        )
    }
// Navigate to the Moderation screen and add the current screen to the back stack.
    private fun openModerationScreen() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.screenBox, ModerationFragment.newInstance())
            .addToBackStack(null)
            .commit()
    }

    /**
     * Admin Functional Requirements 3.2 Master Action Hub
     */
    private fun showAdminToolsDialog() {
        val adminOptions = arrayOf(
            "1. Post Global Announcement",
            "2. Manage Notification Settings",
            "3. System Configuration & Categories",
            "4. Contact Requests Inbox (Support)",
            "5. Backup & Restore Database (JSON)",
            "6. Export Reports for Law Enforcement",
            "7. Real-Time System Audit Logs"
        )
        AlertDialog.Builder(requireContext())
            .setTitle("Admin Control Panel")
            .setItems(adminOptions) { _, which ->
                when (which) {
                    0 -> showPostAnnouncementDialog()
                    1 -> showNotificationSettingsDialog()
                    2 -> showManageCategoriesDialog()
                    3 -> showContactRequestsInboxDialog()
                    4 -> showBackupRestoreDialog()
                    5 -> showExportLawEnforcementReports()
                    6 -> showRealtimeActivityLogsDialog()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Admin Req 3.2 #13: Post global announcements with push notification broadcast
     */
    private fun showPostAnnouncementDialog() {
        val input = EditText(requireContext()).apply {
            hint = "Enter announcement message for all users..."
            minLines = 3
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Post Global Announcement")
            .setView(input)
            .setPositiveButton("Publish Broadcast") { _, _ ->
                val text = input.text.toString().trim()
                if (text.isNotEmpty()) {
                    // Create a new announcement reference with a unique Firebase-generated ID.
                    val annRef = FirebaseManager.database.getReference("announcements").push()
                    // Get the Firebase-generated ID, or create a timestamp-based fallback if the key is null.
                    val annId = annRef.key ?: "ann_${System.currentTimeMillis()}"
                    val announcement = mapOf(
                        "id" to annId,
                        "title" to "Foundly Official Announcement",
                        "message" to text,
                        "timestamp" to System.currentTimeMillis()
                    )
                    annRef.setValue(announcement)

                    // Also broadcast to each user's notifications node
                    FirebaseManager.database.getReference("users").addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            for (userSnap in snapshot.children) {
                                val uid = userSnap.key ?: continue
                                val notif = mapOf(
                                    "id" to "ann_${annId}_$uid",
                                    "title" to "📢 Foundly Official Announcement",
                                    "message" to text,
                                    "timestamp" to System.currentTimeMillis(),
                                    "read" to false,
                                    "type" to "ANNOUNCEMENT"
                                )
            // Save a new notification under the specified user's notifications in Firebase.
                                FirebaseManager.database.getReference("notifications").child(uid).push().setValue(notif)
                            }
                        }
                        // Handle errors when the Firebase database listener is cancelled.
                        override fun onCancelled(error: DatabaseError) {}
                    })
                    // Trigger a push notification using the FCM service
                    FCMService.triggerPushNotification(
                        requireContext(),
                        "📢 Foundly Official Announcement",
                        text
                    )
                    FirebaseManager.logAdminAction("ANNOUNCEMENT", "POST_GLOBAL_ANNOUNCEMENT")
                    com.group3.lostandfound.util.ModernToast.show(requireContext(), "Global announcement broadcasted.", view)
                }
            }
            // Add a Cancel button that closes the dialog without performing an action.
                .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Admin Req 3.2 #7: Manage notification settings
     */
    private fun showNotificationSettingsDialog() {
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
    // Convert 16dp into pixels for consistent UI padding across screen densities.
//If the screen density is 2.0, then 16 × 2.0 = 32 pixels.
            val pad = (16 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad, pad, pad)
        }
// Create a CheckBox for enabling or disabling urgent alerts.
        val cbUrgentAlerts = CheckBox(requireContext()).apply {
            text = "Broadcast Urgent Lost/Found Push Alerts"
            isChecked = true
        }
        val cbAudioChime = CheckBox(requireContext()).apply {
            text = "Play Audible Chime on New Report Published"
            isChecked = true
        }
        val cbChatSounds = CheckBox(requireContext()).apply {
            text = "Enable Real-time Chat Sound Notifications"
            isChecked = true
        }

        val tvRadius = TextView(requireContext()).apply {
            text = "Geofenced Notification Radius: 25 km"
            textSize = 13f
            setPadding(0, (8 * resources.displayMetrics.density).toInt(), 0, (4 * resources.displayMetrics.density).toInt())
        }

        layout.addView(cbUrgentAlerts)
        layout.addView(cbAudioChime)
        layout.addView(cbChatSounds)
        layout.addView(tvRadius)

        AlertDialog.Builder(requireContext())
            .setTitle("Notification Settings (Admin 3.2 #7)")
            .setView(layout)
            .setPositiveButton("Save Preferences") { _, _ ->
                val settings = mapOf(
                    "urgentAlerts" to cbUrgentAlerts.isChecked,
                    "audioChime" to cbAudioChime.isChecked,
                    "chatSounds" to cbChatSounds.isChecked,
                    "radiusKm" to 25,
                    "updatedAt" to System.currentTimeMillis()
                )
                FirebaseManager.database.getReference("config").child("notification_settings").setValue(settings)
                FirebaseManager.logAdminAction("CONFIG", "UPDATE_NOTIFICATION_SETTINGS")
                com.group3.lostandfound.util.ModernToast.show(requireContext(), "Notification settings saved successfully", view)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
/**
 * Admin Req 3.2 #9: Manage system configuration and categories
 */
// Load categories from Firebase and allow the admin to add or remove them.
private fun showManageCategoriesDialog() {

    // Read the categories stored in Firebase.
    FirebaseManager.database.getReference("config").child("categories")
        .addListenerForSingleValueEvent(object : ValueEventListener {

            // Process the categories returned by Firebase.
            override fun onDataChange(snapshot: DataSnapshot) {

                // Create a list to store category names.
                val categoriesList = mutableListOf<String>()

                // Read each category from Firebase if the data exists.
                if (snapshot.exists()) {
                    for (child in snapshot.children) {
                        val cat = child.getValue(String::class.java)
                        if (!cat.isNullOrEmpty()) categoriesList.add(cat)
                    }
                }

                // Use default categories if Firebase contains none.
                if (categoriesList.isEmpty()) {
                    categoriesList.addAll(
                        listOf("Electronics", "Wallets", "Keys", "Documents", "Pets",
                            "Bags", "Jewelry", "Vehicles", "Clothing", "Other")
                    )
                }

                // Add an option for creating a new category to the dialog list.
                val catArray = (categoriesList + listOf("+ Add New Category")).toTypedArray()

                // Display all categories and handle the selected option.
                AlertDialog.Builder(requireContext())
                    .setTitle("Manage Categories (${categoriesList.size})")
                    .setItems(catArray) { _, which ->

                        // Open the add-category dialog if the admin selects the final option.
                        if (which == categoriesList.size) {

                            // Create a text field for entering the new category name.
                            val input = EditText(requireContext()).apply {
                                hint = "e.g. Sporting Goods"
                            }

                            // Ask the admin to enter and confirm the new category.
                            AlertDialog.Builder(requireContext())
                                .setTitle("Add New Category")
                                .setView(input)
                                .setPositiveButton("Add") { _, _ ->

                                    // Read the input and remove extra spaces from its ends.
                                    val newCat = input.text.toString().trim()

                                    // Add the category only if it is non-empty and not already listed.
                                    if (newCat.isNotEmpty() && !categoriesList.contains(newCat)) {
                                        categoriesList.add(newCat)

                                        // Save the updated category list to Firebase.
                                        FirebaseManager.database.getReference("config")
                                            .child("categories").setValue(categoriesList)

                                        // Record the category addition in the admin activity log.
                                        FirebaseManager.logAdminAction("CONFIG", "ADD_CATEGORY_$newCat")

                                        // Show confirmation to the admin.
                                        com.group3.lostandfound.util.ModernToast.show(
                                            requireContext(), "Added category: $newCat", view
                                        )
                                    }
                                }
                                .setNegativeButton("Cancel", null) // Close without adding a category.
                                .show()

                        } else {

                            // Get the category selected for deletion.
                            val toRemove = categoriesList[which]

                            // Ask the admin to confirm deleting the selected category.
                            AlertDialog.Builder(requireContext())
                                .setTitle("Delete Category")
                                .setMessage("Remove category '$toRemove' from system?")
                                .setPositiveButton("Remove") { _, _ ->

                                    // Remove the category from the local list.
                                    categoriesList.remove(toRemove)

                                    // Save the updated list to Firebase.
                                    FirebaseManager.database.getReference("config")
                                        .child("categories").setValue(categoriesList)

                                    // Record the category removal in the admin activity log.
                                    FirebaseManager.logAdminAction("CONFIG", "REMOVE_CATEGORY_$toRemove")

                                    // Show confirmation to the admin.
                                    com.group3.lostandfound.util.ModernToast.show(
                                        requireContext(), "Removed category: $toRemove", view
                                    )
                                }
                                .setNegativeButton("Cancel", null) // Close without deleting.
                                .show()
                        }
                    }
                    .setNegativeButton("Close", null) // Close the category list dialog.
                    .show()
            }

            // Display an error if Firebase cannot load the categories.
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    requireContext(),
                    "Failed to load categories: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
}
    /**
     * Admin Req 3.2 #11: Respond to contact requests from users
     */// Load user contact requests from Firebase and allow the admin to respond.
private fun showContactRequestsInboxDialog() {

    // Retrieve all contact requests from the database.
    FirebaseManager.database.getReference("contact_requests")
        .addListenerForSingleValueEvent(object : ValueEventListener {

            // Process the contact requests returned by Firebase.
            override fun onDataChange(snapshot: DataSnapshot) {

                // Store request details and their display text.
                val requests = mutableListOf<Map<String, Any>>()
                val displayList = mutableListOf<String>()

                // Read each contact request and extract its details.
                for (child in snapshot.children) {
                    val key = child.key ?: ""
                    val email = child.child("userEmail").getValue(String::class.java) ?: "User"
                    val msg = child.child("message").getValue(String::class.java) ?: ""
                    val status = child.child("status").getValue(String::class.java) ?: "OPEN"
                    val userId = child.child("userId").getValue(String::class.java) ?: ""

                    // Group the request details into one map for easy access.
                    val map = mapOf(
                        "key" to key,
                        "email" to email,
                        "message" to msg,
                        "status" to status,
                        "userId" to userId
                    )
                    requests.add(map)

                    // Create a short summary for the request selection list.
                    displayList.add("[$status] $email: ${msg.take(28)}...")
                }

                // Inform the admin if there are no contact requests.
                if (requests.isEmpty()) {
                    AlertDialog.Builder(requireContext())
                        .setTitle("Contact Requests Inbox")
                        .setMessage("No user contact inquiries at this time.")
                        .setPositiveButton("OK", null)
                        .show()
                } else {

                    // Display all requests and handle the admin's selection.
                    AlertDialog.Builder(requireContext())
                        .setTitle("User Support Inquiries (${requests.size})")
                        .setItems(displayList.toTypedArray()) { _, which ->

                            // Retrieve the selected request's details.
                            val selected = requests[which]
                            val ticketId = selected["key"] as String
                            val userEmail = selected["email"] as String
                            val userMsg = selected["message"] as String
                            val userId = selected["userId"] as String

                            // Create a text field for the admin's response.
                            val replyInput = EditText(requireContext()).apply {
                                hint = "Type official response to $userEmail..."
                                minLines = 3
                            }

                            // Display the user's inquiry and allow the admin to reply.
                            AlertDialog.Builder(requireContext())
                                .setTitle("Inquiry from $userEmail")
                                .setMessage("User Inquiry:\n\"$userMsg\"")
                                .setView(replyInput)
                                .setPositiveButton("Send Response") { _, _ ->

                                    // Read the reply and remove leading or trailing spaces.
                                    val reply = replyInput.text.toString().trim()

                                    // Save the response and mark the request as resolved.
                                    if (reply.isNotEmpty()) {
                                        FirebaseManager.database.getReference("contact_requests")
                                            .child(ticketId).updateChildren(
                                                mapOf(
                                                    "status" to "RESOLVED",
                                                    "adminReply" to reply,
                                                    "resolvedAt" to System.currentTimeMillis()
                                                )
                                            )

                                        // Send an in-app notification to the user, if their ID exists.
                                        if (userId.isNotEmpty()) {
                                            val notificationDoc = mapOf(
                                                "title" to "Foundly Support Response",
                                                "message" to reply,
                                                "timestamp" to System.currentTimeMillis(),
                                                "read" to false
                                            )
                                            FirebaseManager.database.getReference("notifications")
                                                .child(userId).push().setValue(notificationDoc)
                                        }

                                        // Record the admin's response in the activity log.
                                        FirebaseManager.logAdminAction(
                                            ticketId, "RESPOND_CONTACT_REQUEST"
                                        )

                                        // Show confirmation that the response was submitted.
                                        com.group3.lostandfound.util.ModernToast.show(
                                            requireContext(), "Response sent to user.", view
                                        )
                                    }
                                }
                                .setNegativeButton("Cancel", null) // Close without sending a response.
                                .show()
                        }
                        .setNegativeButton("Close", null) // Close the request list.
                        .show()
                }
            }

            // Display an error if Firebase cannot retrieve the contact requests.
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    requireContext(), "Error: ${error.message}", Toast.LENGTH_SHORT
                ).show()
            }
        })
}

    /**
     * Admin Req 3.2 #10: Backup and restore data
     */
  
private fun showBackupRestoreDialog() {
    // Define the two available database operations.
    val options = arrayOf(
        "Export Full Database Backup (JSON)",
        "Restore Database from JSON"
    )

    // Display a dialog allowing the admin to choose an operation.
    AlertDialog.Builder(requireContext())
        .setTitle("Database Backup & Restore (Admin 3.2 #10)")
        .setItems(options) { _, which ->

            // Option 0: Export the current database as a JSON backup.
            if (which == 0) {
                FirebaseManager.backupDatabase(
                    onSuccess = { jsonString ->

                        // Copy the backup JSON to the device clipboard.
                        val clipboard = requireContext()
                            .getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(
                            ClipData.newPlainText("Foundly Database Backup", jsonString)
                        )

                        // Open Android's share menu so the admin can export the JSON.
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "application/json"
                            putExtra(Intent.EXTRA_SUBJECT, "Foundly Database Backup JSON")
                            putExtra(Intent.EXTRA_TEXT, jsonString)
                        }
                        startActivity(
                            Intent.createChooser(shareIntent, "Save or Export Backup via")
                        )

                        // Notify the admin that the backup was copied and shared.
                        com.group3.lostandfound.util.ModernToast.show(
                            requireContext(),
                            "Database backup copied to clipboard & exported",
                            view
                        )
                    },

                    // Show an error dialog if the backup operation fails.
                    onError = { error ->
                        AlertDialog.Builder(requireContext())
                            .setTitle("Backup Failed")
                            .setMessage(error)
                            .setPositiveButton("OK", null)
                            .show()
                    }
                )
            } else {
                // Option 1: Create a text field for the admin to paste backup JSON.
                val input = EditText(requireContext()).apply {
                    hint = "Paste JSON backup data here..."
                    minLines = 6
                }

                // Ask for confirmation before restoring the database.
                AlertDialog.Builder(requireContext())
                    .setTitle("Restore Database from JSON")
                    .setMessage("Warning: This will update/restore items from the backup.")
                    .setView(input)
                    .setPositiveButton("Restore") { _, _ ->

                        // Read the pasted JSON and remove surrounding whitespace.
                        val jsonText = input.text.toString().trim()

                        // Continue only if the input is not empty.
                        if (jsonText.isNotEmpty()) {
                            FirebaseManager.restoreDatabase(
                                jsonString = jsonText,

                                // Notify the admin when restoration succeeds.
                                onSuccess = {
                                    com.group3.lostandfound.util.ModernToast.show(
                                        requireContext(),
                                        "Database restored successfully!",
                                        view
                                    )
                                },

                                // Show an error dialog if restoration fails.
                                onError = { err ->
                                    AlertDialog.Builder(requireContext())
                                        .setTitle("Restore Error")
                                        .setMessage(err)
                                        .setPositiveButton("OK", null)
                                        .show()
                                }
                            )
                        }
                    }
                    // Close the restore dialog without restoring.
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
        // Close the main backup/restore menu.
        .setNegativeButton("Cancel", null)
        .show()
}


    /**
     * Admin Req 3.2 #12: Export reports for law enforcement
     */
private fun showExportLawEnforcementReports() {
    // Retrieve all reported items from Firebase before generating the export.
    FirebaseManager.itemsRef.get().addOnSuccessListener { snapshot ->

        // Build a formatted text audit report containing incident and admin details.
        val exportText = buildString {
            appendLine("==================================================")
            appendLine("FOUNDLY OFFICIAL POLICE & LAW ENFORCEMENT AUDIT")
            appendLine("Date Generated : ${java.util.Date()}")
            appendLine("Admin Officer  : ${FirebaseManager.currentUserEmail}")
            appendLine("Total Incidents: ${snapshot.childrenCount}")
            appendLine("==================================================")

            // Number each incident included in the export.
            var index = 1

            // Read the details of each reported item from Firebase.
            for (child in snapshot.children) {
                val title = child.child("title").getValue(String::class.java) ?: "Report"
                val loc = child.child("location").getValue(String::class.java) ?: "Sri Lanka"
                val status = child.child("status").getValue(String::class.java) ?: "PENDING"
                val cat = child.child("category").getValue(String::class.java) ?: "General"
                val author = child.child("authorName").getValue(String::class.java) ?: "Unknown"
                val time = child.child("timestamp").getValue(Long::class.java) ?: 0L

                // Convert the timestamp into a readable date and time.
                val dateStr = if (time > 0)
                    java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.US)
                        .format(java.util.Date(time))
                else
                    "N/A"

                // Append the incident's details to the audit report.
                appendLine("$index. [$status] $title")
                appendLine("   Category: $cat | Location: $loc")
                appendLine("   Reported by: $author | Timestamp: $dateStr")
                appendLine("   Item ID: ${child.key}")
                appendLine("--------------------------------------------------")

                index++
            }

            // Mark the end of the generated audit report.
            appendLine("END OF OFFICIAL AUDIT LOG")
        }

        // Open Android's share menu to export the generated report as plain text.
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(
                Intent.EXTRA_SUBJECT,
                "Foundly Law Enforcement Export - ${java.util.Date()}"
            )
            putExtra(Intent.EXTRA_TEXT, exportText)
        }
        startActivity(Intent.createChooser(shareIntent, "Export reports to"))

        // Record that the admin initiated a law-enforcement report export.
        FirebaseManager.logAdminAction("REPORTS", "EXPORT_LAW_ENFORCEMENT")

    // Display an error message if Firebase fails to retrieve the reports.
    }.addOnFailureListener {
        Toast.makeText(
            requireContext(),
            "Failed to export reports: ${it.message}",
            Toast.LENGTH_SHORT
        ).show()
    }
}


    /**
     * Admin Req 3.2 #8: Monitor real-time system activity & audit logs
     */
private fun showRealtimeActivityLogsDialog() {
    // Retrieve the latest 40 admin activity logs from Firebase.
    FirebaseManager.adminLogsRef.limitToLast(40)
        .addListenerForSingleValueEvent(object : ValueEventListener {

            // Process the retrieved logs and display them in a dialog.
            override fun onDataChange(snapshot: DataSnapshot) {
                val logList = mutableListOf<String>()

                // Define the date and time format used for each log entry.
                val sdf = java.text.SimpleDateFormat(
                    "MM-dd HH:mm:ss",
                    java.util.Locale.US
                )

                // Read each log's action, target ID, and timestamp.
                for (child in snapshot.children) {
                    val action = child.child("actionType")
                        .getValue(String::class.java) ?: "ACTION"
                    val target = child.child("targetId")
                        .getValue(String::class.java) ?: ""
                    val time = child.child("timestamp")
                        .getValue(Long::class.java) ?: 0L

                    // Format the timestamp or use a fallback if it is missing.
                    val timeStr = if (time > 0)
                        sdf.format(java.util.Date(time))
                    else
                        "recent"

                    // Add the newest entries to the beginning of the displayed list.
                    logList.add(
                        0,
                        "[$timeStr] $action (Target: ${target.take(16)}) by admin"
                    )
                }

                // Display a fallback message when no activity logs exist.
                if (logList.isEmpty()) {
                    logList.add("No admin actions logged yet.")
                }

                // Show the activity logs in a dialog with a Close button.
                AlertDialog.Builder(requireContext())
                    .setTitle("Real-Time System Activity (Admin 3.2 #8)")
                    .setItems(logList.toTypedArray(), null)
                    .setPositiveButton("Close", null)
                    .show()
            }

            // Show an error message if Firebase cancels or fails the read.
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    requireContext(),
                    "Failed to load audit logs: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
}

companion object {
    // Provide a simple factory method for creating an AdminFragment instance.
    fun newInstance(): AdminFragment = AdminFragment()
}

}

