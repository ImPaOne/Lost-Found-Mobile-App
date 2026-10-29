package com.group3.lostandfound

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

class UserManagementFragment : Fragment() {

    private lateinit var btnBack: View
    private lateinit var btnShield: View
    private lateinit var searchField: EditText

    private lateinit var chipAllUsers: TextView
    private lateinit var chipRole: View
    private lateinit var lblChipRole: TextView
    private lateinit var chipStatus: View
    private lateinit var lblChipStatus: TextView
    private lateinit var btnAuditLog: TextView

    private lateinit var pbLoadingUsers: ProgressBar
    private lateinit var layoutEmptyUsers: View
    private lateinit var tvEmptyTitle: TextView
    private lateinit var rvUsersList: RecyclerView

    private val allUsersList = mutableListOf<ManageableUser>()
    private val displayedUsersList = mutableListOf<ManageableUser>()
    private lateinit var adapter: UserManagementAdapter

    private var selectedRoleFilter = "All"
    private var selectedStatusFilter = "All"
    private var searchQuery = ""

    private var usersListener: ValueEventListener? = null
    // Data class representing a user whose role and access status can be managed by an administrator.
    data class ManageableUser(
        val uid: String,
        val fullName: String,
        val email: String,
        val phone: String,
        var role: String, // "Admin", "Moderator", "Member"
        var access: String, // "Active", "Restricted", "Suspended"
        val trustScore: Int,
        val isCurrentAdmin: Boolean,
        var isExpanded: Boolean = false
    )

    // Inflates the XML layout for the user management screen.
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_user_management, container, false)
    }

    // Runs after the fragment's view has been created.
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Connects the Kotlin variables to their XML views.
        initViews(view)
        // Configures the RecyclerView used to display users.
        setupRecyclerView()
        // Registers click listeners for the screen's buttons and filter chips.
        setupListeners()
        // Configures the search field to filter users as text changes.
        setupSearch()
        // Loads user records from Firebase.
        loadRealUsersFromFirebase()
        // Sets up the bottom navigation with the profile section selected.
        setupBottomNav(view, "profile")
    }

    // Finds and initializes the views defined in the XML layout.
    private fun initViews(view: View) {
        btnBack = view.findViewById(R.id.btnBack)
        btnShield = view.findViewById(R.id.btnShield)
        searchField = view.findViewById(R.id.searchField)

        // Initializes the user and filter selection chips.
        chipAllUsers = view.findViewById(R.id.chipAllUsers)
        chipRole = view.findViewById(R.id.chipRole)
        lblChipRole = view.findViewById(R.id.lblChipRole)
        chipStatus = view.findViewById(R.id.chipStatus)
        lblChipStatus = view.findViewById(R.id.lblChipStatus)
        btnAuditLog = view.findViewById(R.id.btnAuditLog)

        // Initializes the loading indicator, empty-state views, and users list.
        pbLoadingUsers = view.findViewById(R.id.pbLoadingUsers)
        layoutEmptyUsers = view.findViewById(R.id.layoutEmptyUsers)
        tvEmptyTitle = view.findViewById(R.id.tvEmptyTitle)
        rvUsersList = view.findViewById(R.id.rvUsersList)
    }

    // Configures the RecyclerView and connects it to the user management adapter.
    private fun setupRecyclerView() {
        adapter = UserManagementAdapter(
            users = displayedUsersList,
            // Expands or collapses the selected user's details.
            onExpandToggle = { position ->
                if (position in 0 until displayedUsersList.size) {
                    val user = displayedUsersList[position]
                    user.isExpanded = !user.isExpanded
                    adapter.notifyItemChanged(position)
                }
            },
            // Saves changes to the selected user's role and access status.
            onSaveAccess = { user, newRole, newAccess ->
                saveUserAccess(user, newRole, newAccess)
            },
            // Opens the dialog for reviewing a user's suspension.
            onReviewSuspension = { user ->
                showSuspensionDialog(user)
            }
        )
        // Sets a vertical layout manager for the user list.
        rvUsersList.layoutManager = LinearLayoutManager(requireContext())
        // Attaches the adapter to the RecyclerView.
        rvUsersList.adapter = adapter
    }

    // Sets up click listeners for navigation, security, audit logs, and filters.
    private fun setupListeners() {
        // Returns to the previous fragment when the back button is clicked.
        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Opens the security metrics dialog.
        btnShield.setOnClickListener {
            showSecurityMetricsDialog()
        }

        // Opens the administrator audit log dialog.
        btnAuditLog.setOnClickListener {
            showAuditLogDialog()
        }

        // Resets the role and status filters and clears the search field.
        chipAllUsers.setOnClickListener {
            selectedRoleFilter = "All"
            selectedStatusFilter = "All"
            searchField.setText("")
            updateFilterChipsUi()
            applyFilters()
        }

        // Opens the dialog for filtering users by role.
        chipRole.setOnClickListener {
            showRoleFilterDialog()
        }

        // Opens the dialog for filtering users by access status.
        chipStatus.setOnClickListener {
            showStatusFilterDialog()
        }
    }

    // Listens for text changes in the search field.
    private fun setupSearch() {
        searchField.addTextChangedListener(object : TextWatcher {
            // Called before the text in the search field changes.
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            // Updates the search query and refreshes the filtered user list.
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchQuery = s?.toString()?.trim()?.lowercase().orEmpty()
                applyFilters()
            }
            // Called after the text in the search field changes.
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    // Loads user records from Firebase and listens for real-time database changes.
    private fun loadRealUsersFromFirebase() {
        // Displays the loading indicator while users are being retrieved.
        pbLoadingUsers.visibility = View.VISIBLE

        // Removes the previous Firebase listener to avoid duplicate listeners.
        usersListener?.let { FirebaseManager.usersRef.removeEventListener(it) }
        // Creates a listener to receive updates to the users database.
        usersListener = object : ValueEventListener {
            // Processes the user records whenever Firebase data changes.
            override fun onDataChange(snapshot: DataSnapshot) {
                // Stops processing if the fragment is no longer attached.
                if (!isAdded) return

                // Creates a temporary list to hold the users retrieved from Firebase.
                val userList = mutableListOf<ManageableUser>()
                // Retrieves the currently authenticated user's ID and email.
                val currentUid = FirebaseManager.currentUid
                val currentEmail = FirebaseManager.currentUserEmail

                // Iterates through each user record in the Firebase snapshot.
                for (child in snapshot.children) {
                    // Retrieves the user's unique ID from the database key.
                    val uid = child.key.orEmpty()
                    // Skips records that do not have a user ID.
                    if (uid.isEmpty()) continue

                    // Retrieves the user's email or uses an empty string if unavailable.
                    val email = child.child("email").getValue(String::class.java).orEmpty()

                    // Retrieves the user's name from available database fields, using a fallback if necessary.
                    val fullName = child.child("fullName").getValue(String::class.java)
                        ?: child.child("displayName").getValue(String::class.java)
                        ?: child.child("name").getValue(String::class.java)
                        ?: if (email.isNotEmpty()) email.substringBefore("@").replaceFirstChar { it.uppercase() } else "Community Member"

                    // Purge and ignore dummy/sample test records if they were saved in Firebase
                    val isDummySeededUser = uid == "user_tharindu_001" || uid == "user_ama_002" || uid == "user_kasun_003"
                        || uid.startsWith("user_tharindu") || uid.startsWith("user_ama") || uid.startsWith("user_kasun")
                        || uid.contains("tharindu", ignoreCase = true) || uid.contains("ama", ignoreCase = true) || uid.contains("kasun", ignoreCase = true)
                        || email.contains("tharindu", ignoreCase = true)
                        || email.contains("ama.fernando", ignoreCase = true)
                        || email.contains("kasun.j", ignoreCase = true)
                        || fullName.contains("Tharindu", ignoreCase = true)
                        || fullName.contains("Ama Fernando", ignoreCase = true)
                        || fullName.contains("Kasun Jayawardena", ignoreCase = true)
                    if (isDummySeededUser) {
                        FirebaseManager.usersRef.child(uid).removeValue()
                        continue
                    }

                    // Retrieves the user's phone number from either supported field.
                    val phone = child.child("phone").getValue(String::class.java)
                        ?: child.child("phoneNumber").getValue(String::class.java)
                        ?: "Not provided"

                    // Reads the role and converts it to uppercase for consistent comparison.
                    val roleRaw = child.child("role").getValue(String::class.java).orEmpty().uppercase()
                    // Converts the stored role into the role labels used by the interface.
                    val role = when {
                        roleRaw == "ADMIN" || email.equals("admin@gmail.com", ignoreCase = true) -> "Admin"
                        roleRaw == "MODERATOR" -> "Moderator"
                        else -> "Member"
                    }

                    // Checks whether the user is banned or has a suspended status.
                    val isBanned = child.child("isBanned").getValue(Boolean::class.java) == true
                        || child.child("status").getValue(String::class.java).equals("BANNED", ignoreCase = true)
                        || child.child("status").getValue(String::class.java).equals("SUSPENDED", ignoreCase = true)
                        || child.child("access").getValue(String::class.java).equals("Suspended", ignoreCase = true)

                    // Checks whether the user has been restricted.
                    val isRestricted = child.child("isRestricted").getValue(Boolean::class.java) == true
                        || child.child("access").getValue(String::class.java).equals("Restricted", ignoreCase = true)

                    // Determines the displayed access status based on the user's restrictions.
                    val access = when {
                        isBanned -> "Suspended"
                        isRestricted -> "Restricted"
                        else -> "Active"
                    }

                    // Retrieves the user's trust score or assigns a default based on their role.
                    val trustScore = child.child("trustScore").getValue(Int::class.java)
                        ?: if (role == "Admin") 100 else 85

                    // Identifies the current administrator using the user ID, email, or configured admin email.
                    val isCurrentAdmin = (uid == currentUid && currentUid.isNotEmpty())
                        || (email.isNotEmpty() && email.equals(currentEmail, ignoreCase = true))
                        || email.equals("admin@gmail.com", ignoreCase = true)

                    // Creates a ManageableUser object and adds it to the retrieved user list.
                    userList.add(
                        ManageableUser(
                            uid = uid,
                            fullName = fullName,
                            email = email,
                            phone = phone,
                            role = role,
                            access = access,
                            trustScore = trustScore,
                            isCurrentAdmin = isCurrentAdmin
                        )
                    )
                }

                // Always sort: current admin first, then Admins, then Moderators, then Members
                userList.sortWith(compareByDescending<ManageableUser> { it.isCurrentAdmin }
                    .thenBy { if (it.role == "Admin") 0 else if (it.role == "Moderator") 1 else 2 }
                    .thenBy { it.fullName })

                // Replaces the previous user data with the newly retrieved records.
                allUsersList.clear()
                allUsersList.addAll(userList)

                // Hides the loading indicator after processing the records.
                pbLoadingUsers.visibility = View.GONE
                // Refreshes the list using the current search and filter settings.
                applyFilters()
            }

            // Handles errors that occur while retrieving users from Firebase.
            override fun onCancelled(error: DatabaseError) {
                // Stops processing if the fragment is no longer attached.
                if (!isAdded) return
                // Hides the loading indicator after the request fails.
                pbLoadingUsers.visibility = View.GONE
                // Displays an error message describing the failed request.
                Toast.makeText(requireContext(), "Failed to load users: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }

        // Registers the listener so Firebase can deliver initial data and subsequent updates.
        FirebaseManager.usersRef.addValueEventListener(usersListener as ValueEventListener)
    }

    // Filters the user list according to the current search query, role, and access status.
    private fun applyFilters() {
        // Clears the previous displayed results before applying the filters.
        displayedUsersList.clear()

        // Selects users who match all active filtering conditions.
        val filtered = allUsersList.filter { user ->
            // Checks whether the search query matches any supported user field.
            val matchesSearch = if (searchQuery.isEmpty()) true else {
                user.fullName.lowercase().contains(searchQuery) ||
                user.email.lowercase().contains(searchQuery) ||
                user.phone.lowercase().contains(searchQuery) ||
                user.role.lowercase().contains(searchQuery) ||
                user.access.lowercase().contains(searchQuery)
            }

            // Checks whether the user matches the selected role filter.
            val matchesRole = when (selectedRoleFilter) {
                "All" -> true
                "Admin" -> user.role.equals("Admin", ignoreCase = true)
                "Moderator" -> user.role.equals("Moderator", ignoreCase = true)
                "Member" -> user.role.equals("Member", ignoreCase = true) || user.role.equals("User", ignoreCase = true)
                else -> true
            }

            // Checks whether the user matches the selected access status filter.
            val matchesStatus = when (selectedStatusFilter) {
                "All" -> true
                "Active" -> user.access.equals("Active", ignoreCase = true)
                "Restricted" -> user.access.equals("Restricted", ignoreCase = true)
                "Suspended" -> user.access.equals("Suspended", ignoreCase = true) || user.access.equals("Banned", ignoreCase = true)
                else -> true
            }

            // Includes the user only when the search, role, and status conditions all match.
            matchesSearch && matchesRole && matchesStatus
        }

        // Adds the filtered users to the list displayed by the RecyclerView.
        displayedUsersList.addAll(filtered)
        // Notifies the adapter that the displayed data has changed.
        adapter.notifyDataSetChanged()

        // Shows an empty-state message when no users match the current filters.
        if (displayedUsersList.isEmpty()) {
            layoutEmptyUsers.visibility = View.VISIBLE
            // Displays a message appropriate to whether a search query is active.
            tvEmptyTitle.text = if (searchQuery.isNotEmpty()) {
                "No users matching \"$searchQuery\""
            } else {
                "No users match the selected filters"
            }
        } else {
            // Hides the empty-state view when matching users are available.
            layoutEmptyUsers.visibility = View.GONE
        }
    }

    // Updates the appearance and labels of the role and status filter chips.
    private fun updateFilterChipsUi() {
        // Checks whether both role and status filters are set to their default values.
        val isAllDefault = selectedRoleFilter == "All" && selectedStatusFilter == "All"

        // Highlights the All Users chip when no specific filters are active.
        if (isAllDefault) {
            chipAllUsers.setBackgroundResource(R.drawable.bg_pill_dark)
            chipAllUsers.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
        } else {
            // Uses the outlined style when a specific filter is active.
            chipAllUsers.setBackgroundResource(R.drawable.bg_pill_outline)
            chipAllUsers.setTextColor(ContextCompat.getColor(requireContext(), R.color.deep_blue))
        }

        // Updates the role chip label to show the selected role, if any.
        lblChipRole.text = if (selectedRoleFilter == "All") "Role" else "Role: $selectedRoleFilter"
        // Updates the status chip label to show the selected status, if any.
        lblChipStatus.text = if (selectedStatusFilter == "All") "Status" else "Status: $selectedStatusFilter"
    }

    // Displays a dialog allowing the administrator to filter users by role.
    private fun showRoleFilterDialog() {
        val roles = arrayOf("All Roles", "Admin", "Moderator", "Member")
        val currentIdx = when (selectedRoleFilter) {
            "Admin" -> 1
            "Moderator" -> 2
            "Member" -> 3
            else -> 0
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Filter by Role")
            .setSingleChoiceItems(roles, currentIdx) { dialog, which ->
                dialog.dismiss()
                selectedRoleFilter = when (which) {
                    1 -> "Admin"
                    2 -> "Moderator"
                    3 -> "Member"
                    else -> "All"
                }
                updateFilterChipsUi()
                applyFilters()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // Displays a dialog allowing the administrator to filter users by access status.
    private fun showStatusFilterDialog() {
        val statuses = arrayOf("All Statuses", "Active", "Restricted", "Suspended")
        val currentIdx = when (selectedStatusFilter) {
            "Active" -> 1
            "Restricted" -> 2
            "Suspended" -> 3
            else -> 0
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Filter by Status")
            .setSingleChoiceItems(statuses, currentIdx) { dialog, which ->
                dialog.dismiss()
                selectedStatusFilter = when (which) {
                    1 -> "Active"
                    2 -> "Restricted"
                    3 -> "Suspended"
                    else -> "All"
                }
                updateFilterChipsUi()
                applyFilters()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // Updates a user's role and access status in Firebase.
    private fun saveUserAccess(user: ManageableUser, newRole: String, newAccess: String) {
        // Prevents the current administrator from removing or restricting their own admin access.
        if (user.isCurrentAdmin && (newRole != "Admin" || newAccess != "Active")) {
            Toast.makeText(requireContext(), "You cannot alter your own admin privileges.", Toast.LENGTH_SHORT).show()
            return
        }

        // Updates the user object with the newly selected role and access status.
        user.role = newRole
        user.access = newAccess

        // Prepares the Firebase fields that must be updated for the selected user.
        val updates = mapOf<String, Any>(
            "role" to newRole.uppercase(),
            "isBanned" to (newAccess == "Suspended"),
            "isRestricted" to (newAccess == "Restricted"),
            "status" to newAccess.uppercase(),
            "access" to newAccess
        )

        // Immediately refresh the item in adapter for instant responsiveness
        val localIndex = displayedUsersList.indexOfFirst { it.uid == user.uid }
        if (localIndex >= 0) adapter.notifyItemChanged(localIndex)

        // Updates the selected user's record in the Firebase database.
        FirebaseManager.usersRef.child(user.uid).updateChildren(updates)
            // Handles a successful database update.
            .addOnSuccessListener {
                // Stops processing if the fragment is no longer attached.
                if (!isAdded) return@addOnSuccessListener
                // Confirms the updated role and access status to the administrator.
                Toast.makeText(requireContext(), "Updated ${user.fullName} to $newRole ($newAccess)", Toast.LENGTH_SHORT).show()
                val freshIndex = displayedUsersList.indexOfFirst { it.uid == user.uid }
                if (freshIndex >= 0) adapter.notifyItemChanged(freshIndex)

                // Log audit action to Firebase /admin_logs
                // Generates a unique ID for the audit record, with a fallback if unavailable.
                val logId = FirebaseManager.adminLogsRef.push().key ?: "log_${System.currentTimeMillis()}"
                // Creates an audit record describing the administrator's action.
                val log = AdminAction(
                    actionId = logId,
                    adminUid = FirebaseManager.currentUid,
                    targetId = "${user.fullName} (${user.uid}) · $newRole ($newAccess)",
                    actionType = "USER_ACCESS_UPDATE",
                    timestamp = System.currentTimeMillis()
                )
                // Saves the audit record to the Firebase admin logs.
                FirebaseManager.adminLogsRef.child(logId).setValue(log)
            }
            // Handles errors that occur while updating the user's record.
            .addOnFailureListener { e ->
                // Stops processing if the fragment is no longer attached.
                if (!isAdded) return@addOnFailureListener
                // Displays the database update error.
                Toast.makeText(requireContext(), "Failed to update: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Displays the appropriate dialog for suspending or reactivating a user.
    private fun showSuspensionDialog(user: ManageableUser) {
        // Prevents the current administrator from suspending their own account.
        if (user.isCurrentAdmin) {
            Toast.makeText(requireContext(), "You cannot suspend your own admin account.", Toast.LENGTH_SHORT).show()
            return
        }

        // Checks whether the user is already suspended and offers to reactivate the account.
        if (user.access == "Suspended") {
            AlertDialog.Builder(requireContext())
                // Sets the dialog title for reactivating the account.
                .setTitle("Reactivate Account")
                // Asks the administrator to confirm lifting the suspension.
                .setMessage("Lift suspension for ${user.fullName} and restore active status?")
                // Restores the user's access to Active when confirmed.
                .setPositiveButton("Reactivate") { _, _ ->
                    saveUserAccess(user, user.role, "Active")
                    // Finds the user's position in the displayed list.
                    val index = displayedUsersList.indexOfFirst { it.uid == user.uid }
                    // Refreshes the user's row if it is still present in the list.
                    if (index >= 0) adapter.notifyItemChanged(index)
                }
                // Closes the dialog without changing the user's status.
                .setNegativeButton("Cancel", null)
                // Displays the reactivation confirmation dialog.
                .show()
            return
        }

        // Defines common reasons for suspending a user account.
        val reasons = arrayOf(
            "Spam or false report submissions",
            "Multiple scam reports flagged by community",
            "Violation of community safety policy",
            "Harassment in live chat"
        )

        // Displays the available suspension reasons.
        AlertDialog.Builder(requireContext())
            // Sets the dialog title to include the user's name.
            .setTitle("Suspend ${user.fullName}")
            // Suspends the user when an administrator selects a reason.
            .setItems(reasons) { _, which ->
                // Retrieves the selected suspension reason.
                val reason = reasons[which]
                // Updates the user's access status to Suspended.
                saveUserAccess(user, user.role, "Suspended")
                // Finds the user's position in the displayed list.
                val index = displayedUsersList.indexOfFirst { it.uid == user.uid }
                // Refreshes the user's row if it is still present in the list.
                if (index >= 0) adapter.notifyItemChanged(index)

                // Displays a message explaining why the account was suspended.
                Toast.makeText(requireContext(), "Suspended ${user.fullName} for: $reason", Toast.LENGTH_LONG).show()
            }
            // Closes the dialog without suspending the account.
            .setNegativeButton("Cancel", null)
            // Displays the suspension dialog.
            .show()
    }
    private fun showSecurityMetricsDialog() {
        val total = allUsersList.size
        val admins = allUsersList.count { it.role == "Admin" }
        val moderators = allUsersList.count { it.role == "Moderator" }
        val active = allUsersList.count { it.access == "Active" }
        val suspended = allUsersList.count { it.access == "Suspended" }

        val info = """
            • Total Registered Users: $total
            • Active Accounts: $active
            • System Administrators: $admins
            • Community Moderators: $moderators
            • Suspended / Banned: $suspended
            • Real-time Firebase Database: Connected
        """.trimIndent()

        AlertDialog.Builder(requireContext())
            .setTitle("Security & Access Overview")
            .setMessage(info)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showAuditLogDialog() {
        pbLoadingUsers.visibility = View.VISIBLE
        FirebaseManager.adminLogsRef.limitToLast(15).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded) return
                pbLoadingUsers.visibility = View.GONE

                val logs = mutableListOf<String>()
                for (child in snapshot.children) {
                    val action = child.child("actionType").getValue(String::class.java) ?: "ACTION"
                    val target = child.child("targetId").getValue(String::class.java) ?: ""
                    val time = child.child("timestamp").getValue(Long::class.java) ?: System.currentTimeMillis()
                    val sdf = java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.US)
                    val timeStr = sdf.format(java.util.Date(time))
                    logs.add("$action · $target\n$timeStr")
                }

                if (logs.isEmpty()) {
                    logs.add("No audit actions recorded yet. All system activities will appear here.")
                }

                AlertDialog.Builder(requireContext())
                    .setTitle("Audit Logs (${logs.size})")
                    .setItems(logs.reversed().toTypedArray(), null)
                    .setPositiveButton("Close", null)
                    .show()
            }

            override fun onCancelled(error: DatabaseError) {
                if (!isAdded) return
                pbLoadingUsers.visibility = View.GONE
                Toast.makeText(requireContext(), "Failed to load audit logs: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        usersListener?.let { FirebaseManager.usersRef.removeEventListener(it) }
    }

    private class UserManagementAdapter(
        private val users: List<ManageableUser>,
        private val onExpandToggle: (Int) -> Unit,
        private val onSaveAccess: (ManageableUser, String, String) -> Unit,
        private val onReviewSuspension: (ManageableUser) -> Unit
    ) : RecyclerView.Adapter<UserManagementAdapter.UserViewHolder>() {

        class UserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val cardUser: View = view.findViewById(R.id.cardUser)
            val rowUserHeader: View = view.findViewById(R.id.rowUserHeader)
            val tvUserAvatar: TextView = view.findViewById(R.id.tvUserAvatar)
            val tvUserName: TextView = view.findViewById(R.id.tvUserName)
            val badgeYou: TextView = view.findViewById(R.id.badgeYou)
            val badgeStatusFlag: TextView = view.findViewById(R.id.badgeStatusFlag)
            val tvUserSubtitle: TextView = view.findViewById(R.id.tvUserSubtitle)
            val iconChevron: ImageView = view.findViewById(R.id.iconChevron)

            val boxUserDetails: View = view.findViewById(R.id.boxUserDetails)
            val tvUserEmail: TextView = view.findViewById(R.id.tvUserEmail)
            val tvTrustScore: TextView = view.findViewById(R.id.tvTrustScore)

            val boxRoleSelect: View = view.findViewById(R.id.boxRoleSelect)
            val lblRoleValue: TextView = view.findViewById(R.id.lblRoleValue)
            val boxAccessSelect: View = view.findViewById(R.id.boxAccessSelect)
            val lblAccessValue: TextView = view.findViewById(R.id.lblAccessValue)

            val btnReviewSuspension: TextView = view.findViewById(R.id.btnReviewSuspension)
            val btnSaveAccess: TextView = view.findViewById(R.id.btnSaveAccess)
            val tvAccessNote: TextView = view.findViewById(R.id.tvAccessNote)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_user_management_card, parent, false)
            return UserViewHolder(view)
        }

        override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
            val user = users[position]
            val context = holder.itemView.context

            // Initials avatar
            val initials = user.fullName.split(" ")
                .filter { it.isNotBlank() }
                .take(2)
                .map { it.first().uppercase() }
                .joinToString("")
                .ifEmpty { "U" }
            holder.tvUserAvatar.text = initials

            // Avatar styling based on Role / Status
            when {
                user.access == "Suspended" -> {
                    holder.tvUserAvatar.setBackgroundResource(R.drawable.bg_circle_avatar_kj)
                    holder.tvUserAvatar.setTextColor(0xFFE11D48.toInt())
                }
                user.role == "Admin" -> {
                    holder.tvUserAvatar.setBackgroundResource(R.drawable.bg_circle_avatar_ns)
                    holder.tvUserAvatar.setTextColor(0xFF1D4ED8.toInt())
                }
                user.role == "Moderator" -> {
                    holder.tvUserAvatar.setBackgroundResource(R.drawable.bg_circle_avatar_af)
                    holder.tvUserAvatar.setTextColor(0xFFEA580C.toInt())
                }
                else -> {
                    holder.tvUserAvatar.setBackgroundResource(R.drawable.bg_circle_avatar_tp)
                    holder.tvUserAvatar.setTextColor(0xFF059669.toInt())
                }
            }

            holder.tvUserName.text = user.fullName
            holder.badgeYou.visibility = if (user.isCurrentAdmin) View.VISIBLE else View.GONE

            if (user.access == "Suspended") {
                holder.badgeStatusFlag.visibility = View.VISIBLE
                holder.badgeStatusFlag.text = "Suspended"
                holder.badgeStatusFlag.setTextColor(0xFFDC2626.toInt())
            } else if (user.access == "Restricted") {
                holder.badgeStatusFlag.visibility = View.VISIBLE
                holder.badgeStatusFlag.text = "Restricted"
                holder.badgeStatusFlag.setTextColor(0xFFB45309.toInt())
            } else {
                holder.badgeStatusFlag.visibility = View.GONE
            }

            holder.tvUserSubtitle.text = "${user.role} · ${user.access} · ${user.email}"
            holder.tvUserEmail.text = user.email.ifEmpty { "No email associated" }
            holder.tvTrustScore.text = "${user.trustScore} / 100"

            holder.lblRoleValue.text = user.role
            holder.lblAccessValue.text = user.access

            // Expand / Collapse state
            holder.boxUserDetails.visibility = if (user.isExpanded) View.VISIBLE else View.GONE
            holder.iconChevron.setImageResource(
                if (user.isExpanded) R.drawable.ic_chevron_up else R.drawable.ic_chevron_down
            )

            val toggleExpand = View.OnClickListener {
                val pos = holder.bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION && pos < users.size) {
                    onExpandToggle(pos)
                }
            }
            holder.rowUserHeader.setOnClickListener(toggleExpand)
            holder.iconChevron.setOnClickListener(toggleExpand)
            holder.cardUser.setOnClickListener(toggleExpand)

            // Role selection dropdown
            holder.boxRoleSelect.setOnClickListener {
                val roles = arrayOf("Member", "Moderator", "Admin")
                val curRoleIdx = roles.indexOf(user.role).let { if (it >= 0) it else 0 }
                AlertDialog.Builder(context)
                    .setTitle("Change Role for ${user.fullName}")
                    .setSingleChoiceItems(roles, curRoleIdx) { d, which ->
                        d.dismiss()
                        val selectedRole = roles[which]
                        holder.lblRoleValue.text = selectedRole
                        user.role = selectedRole
                        holder.tvUserSubtitle.text = "${user.role} · ${user.access} · ${user.email}"
                        when (selectedRole) {
                            "Admin" -> {
                                holder.tvUserAvatar.setBackgroundResource(R.drawable.bg_circle_avatar_ns)
                                holder.tvUserAvatar.setTextColor(0xFF1D4ED8.toInt())
                            }
                            "Moderator" -> {
                                holder.tvUserAvatar.setBackgroundResource(R.drawable.bg_circle_avatar_af)
                                holder.tvUserAvatar.setTextColor(0xFFEA580C.toInt())
                            }
                            else -> {
                                holder.tvUserAvatar.setBackgroundResource(R.drawable.bg_circle_avatar_tp)
                                holder.tvUserAvatar.setTextColor(0xFF059669.toInt())
                            }
                        }
                        val pos = holder.bindingAdapterPosition
                        if (pos != RecyclerView.NO_POSITION && pos < users.size) {
                            notifyItemChanged(pos)
                        }
                        onSaveAccess(user, selectedRole, user.access)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }

            // Access selection dropdown
            holder.boxAccessSelect.setOnClickListener {
                val accessOptions = arrayOf("Active", "Restricted", "Suspended")
                val curAccessIdx = accessOptions.indexOf(user.access).let { if (it >= 0) it else 0 }
                AlertDialog.Builder(context)
                    .setTitle("Change Access Status for ${user.fullName}")
                    .setSingleChoiceItems(accessOptions, curAccessIdx) { d, which ->
                        d.dismiss()
                        val selectedAccess = accessOptions[which]
                        holder.lblAccessValue.text = selectedAccess
                        user.access = selectedAccess
                        holder.tvUserSubtitle.text = "${user.role} · ${user.access} · ${user.email}"
                        if (user.access == "Suspended") {
                            holder.badgeStatusFlag.visibility = View.VISIBLE
                            holder.badgeStatusFlag.text = "Suspended"
                            holder.badgeStatusFlag.setTextColor(0xFFDC2626.toInt())
                            holder.tvUserAvatar.setBackgroundResource(R.drawable.bg_circle_avatar_kj)
                            holder.tvUserAvatar.setTextColor(0xFFE11D48.toInt())
                        } else if (user.access == "Restricted") {
                            holder.badgeStatusFlag.visibility = View.VISIBLE
                            holder.badgeStatusFlag.text = "Restricted"
                            holder.badgeStatusFlag.setTextColor(0xFFB45309.toInt())
                        } else {
                            holder.badgeStatusFlag.visibility = View.GONE
                        }
                        val pos = holder.bindingAdapterPosition
                        if (pos != RecyclerView.NO_POSITION && pos < users.size) {
                            notifyItemChanged(pos)
                        }
                        onSaveAccess(user, user.role, selectedAccess)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }

            holder.btnSaveAccess.setOnClickListener {
                onSaveAccess(user, holder.lblRoleValue.text.toString(), holder.lblAccessValue.text.toString())
            }

            if (user.isCurrentAdmin) {
                holder.btnReviewSuspension.visibility = View.GONE
                holder.tvAccessNote.text = "Protected: You cannot alter your own admin privileges."
            } else {
                holder.btnReviewSuspension.visibility = View.VISIBLE
                holder.btnReviewSuspension.text = if (user.access == "Suspended") "Lift suspension" else "Review suspension"
                holder.tvAccessNote.text = "Suspensions require a reason and confirmation."

                holder.btnReviewSuspension.setOnClickListener {
                    onReviewSuspension(user)
                }
            }
        }

        override fun getItemCount(): Int = users.size
    }

    companion object {
        fun newInstance(): UserManagementFragment = UserManagementFragment()
    }
}
