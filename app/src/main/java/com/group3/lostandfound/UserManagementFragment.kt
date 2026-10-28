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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_user_management, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupRecyclerView()
        setupListeners()
        setupSearch()
        loadRealUsersFromFirebase()
        setupBottomNav(view, "profile")
    }

    private fun initViews(view: View) {
        btnBack = view.findViewById(R.id.btnBack)
        btnShield = view.findViewById(R.id.btnShield)
        searchField = view.findViewById(R.id.searchField)

        chipAllUsers = view.findViewById(R.id.chipAllUsers)
        chipRole = view.findViewById(R.id.chipRole)
        lblChipRole = view.findViewById(R.id.lblChipRole)
        chipStatus = view.findViewById(R.id.chipStatus)
        lblChipStatus = view.findViewById(R.id.lblChipStatus)
        btnAuditLog = view.findViewById(R.id.btnAuditLog)

        pbLoadingUsers = view.findViewById(R.id.pbLoadingUsers)
        layoutEmptyUsers = view.findViewById(R.id.layoutEmptyUsers)
        tvEmptyTitle = view.findViewById(R.id.tvEmptyTitle)
        rvUsersList = view.findViewById(R.id.rvUsersList)
    }

    private fun setupRecyclerView() {
        adapter = UserManagementAdapter(
            users = displayedUsersList,
            onExpandToggle = { position ->
                val user = displayedUsersList[position]
                user.isExpanded = !user.isExpanded
                adapter.notifyItemChanged(position)
            },
            onSaveAccess = { user, newRole, newAccess ->
                saveUserAccess(user, newRole, newAccess)
            },
            onReviewSuspension = { user ->
                showSuspensionDialog(user)
            }
        )
        rvUsersList.layoutManager = LinearLayoutManager(requireContext())
        rvUsersList.adapter = adapter
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        btnShield.setOnClickListener {
            showSecurityMetricsDialog()
        }

        btnAuditLog.setOnClickListener {
            showAuditLogDialog()
        }

        chipAllUsers.setOnClickListener {
            selectedRoleFilter = "All"
            selectedStatusFilter = "All"
            searchField.setText("")
            updateFilterChipsUi()
            applyFilters()
        }

        chipRole.setOnClickListener {
            showRoleFilterDialog()
        }

        chipStatus.setOnClickListener {
            showStatusFilterDialog()
        }
    }

    private fun setupSearch() {
        searchField.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchQuery = s?.toString()?.trim()?.lowercase().orEmpty()
                applyFilters()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun loadRealUsersFromFirebase() {
        pbLoadingUsers.visibility = View.VISIBLE

        usersListener?.let { FirebaseManager.usersRef.removeEventListener(it) }
        usersListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded) return

                val userList = mutableListOf<ManageableUser>()
                val currentUid = FirebaseManager.currentUid
                val currentEmail = FirebaseManager.currentUserEmail

                for (child in snapshot.children) {
                    val uid = child.key.orEmpty()
                    if (uid.isEmpty()) continue

                    val email = child.child("email").getValue(String::class.java).orEmpty()
                    val fullName = child.child("fullName").getValue(String::class.java)
                        ?: child.child("displayName").getValue(String::class.java)
                        ?: child.child("name").getValue(String::class.java)
                        ?: if (email.isNotEmpty()) email.substringBefore("@").replaceFirstChar { it.uppercase() } else "Community Member"

                    val phone = child.child("phone").getValue(String::class.java)
                        ?: child.child("phoneNumber").getValue(String::class.java)
                        ?: "Not provided"

                    val roleRaw = child.child("role").getValue(String::class.java).orEmpty().uppercase()
                    val role = when {
                        roleRaw == "ADMIN" || email.equals("admin@gmail.com", ignoreCase = true) -> "Admin"
                        roleRaw == "MODERATOR" -> "Moderator"
                        else -> "Member"
                    }

                    val isBanned = child.child("isBanned").getValue(Boolean::class.java) == true
                        || child.child("status").getValue(String::class.java).equals("BANNED", ignoreCase = true)
                        || child.child("status").getValue(String::class.java).equals("SUSPENDED", ignoreCase = true)
                        || child.child("access").getValue(String::class.java).equals("Suspended", ignoreCase = true)

                    val isRestricted = child.child("isRestricted").getValue(Boolean::class.java) == true
                        || child.child("access").getValue(String::class.java).equals("Restricted", ignoreCase = true)

                    val access = when {
                        isBanned -> "Suspended"
                        isRestricted -> "Restricted"
                        else -> "Active"
                    }

                    val trustScore = child.child("trustScore").getValue(Int::class.java)
                        ?: if (role == "Admin") 100 else 85

                    val isCurrentAdmin = (uid == currentUid && currentUid.isNotEmpty())
                        || (email.isNotEmpty() && email.equals(currentEmail, ignoreCase = true))
                        || email.equals("admin@gmail.com", ignoreCase = true)

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

                // If Firebase has very few users, ensure standard demo community users are seeded so the admin has users to test with
                if (userList.size < 3) {
                    seedDefaultCommunityUsersIfEmpty(userList)
                }

                // Always sort: current admin first, then Admins, then Moderators, then Members
                userList.sortWith(compareByDescending<ManageableUser> { it.isCurrentAdmin }
                    .thenBy { if (it.role == "Admin") 0 else if (it.role == "Moderator") 1 else 2 }
                    .thenBy { it.fullName })

                allUsersList.clear()
                allUsersList.addAll(userList)

                pbLoadingUsers.visibility = View.GONE
                applyFilters()
            }

            override fun onCancelled(error: DatabaseError) {
                if (!isAdded) return
                pbLoadingUsers.visibility = View.GONE
                Toast.makeText(requireContext(), "Failed to load users: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }

        FirebaseManager.usersRef.addValueEventListener(usersListener as ValueEventListener)
    }

    private fun seedDefaultCommunityUsersIfEmpty(currentUsers: MutableList<ManageableUser>) {
        val sampleUsers = listOf(
            UserData(uid = "user_tharindu_001", fullName = "Tharindu Perera", email = "tharindu.p@gmail.com", phone = "+94 77 123 4567", role = "USER", isBanned = false, trustScore = 95),
            UserData(uid = "user_ama_002", fullName = "Ama Fernando", email = "ama.fernando@gmail.com", phone = "+94 71 987 6543", role = "MODERATOR", isBanned = false, trustScore = 92),
            UserData(uid = "user_kasun_003", fullName = "Kasun Jayasinghe", email = "kasun.j@gmail.com", phone = "+94 75 456 7890", role = "USER", isBanned = false, trustScore = 78)
        )

        for (u in sampleUsers) {
            if (currentUsers.none { it.email.equals(u.email, ignoreCase = true) }) {
                FirebaseManager.usersRef.child(u.uid).setValue(u)
            }
        }
    }

    private fun applyFilters() {
        displayedUsersList.clear()

        val filtered = allUsersList.filter { user ->
            val matchesSearch = if (searchQuery.isEmpty()) true else {
                user.fullName.lowercase().contains(searchQuery) ||
                user.email.lowercase().contains(searchQuery) ||
                user.phone.lowercase().contains(searchQuery) ||
                user.role.lowercase().contains(searchQuery) ||
                user.access.lowercase().contains(searchQuery)
            }

            val matchesRole = if (selectedRoleFilter == "All") true else {
                user.role.equals(selectedRoleFilter, ignoreCase = true)
            }

            val matchesStatus = if (selectedStatusFilter == "All") true else {
                user.access.equals(selectedStatusFilter, ignoreCase = true)
            }

            matchesSearch && matchesRole && matchesStatus
        }

        displayedUsersList.addAll(filtered)
        adapter.notifyDataSetChanged()

        if (displayedUsersList.isEmpty()) {
            layoutEmptyUsers.visibility = View.VISIBLE
            tvEmptyTitle.text = if (searchQuery.isNotEmpty()) {
                "No users matching \"$searchQuery\""
            } else {
                "No users match the selected filters"
            }
        } else {
            layoutEmptyUsers.visibility = View.GONE
        }
    }

    private fun updateFilterChipsUi() {
        val isAllDefault = selectedRoleFilter == "All" && selectedStatusFilter == "All"

        if (isAllDefault) {
            chipAllUsers.setBackgroundResource(R.drawable.bg_pill_dark)
            chipAllUsers.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
        } else {
            chipAllUsers.setBackgroundResource(R.drawable.bg_pill_outline)
            chipAllUsers.setTextColor(ContextCompat.getColor(requireContext(), R.color.deep_blue))
        }

        lblChipRole.text = if (selectedRoleFilter == "All") "Role" else "Role: $selectedRoleFilter"
        lblChipStatus.text = if (selectedStatusFilter == "All") "Status" else "Status: $selectedStatusFilter"
    }

    private fun showRoleFilterDialog() {
        val roles = arrayOf("All Roles", "Admin", "Moderator", "Member")
        AlertDialog.Builder(requireContext())
            .setTitle("Filter by Role")
            .setItems(roles) { _, which ->
                selectedRoleFilter = when (which) {
                    1 -> "Admin"
                    2 -> "Moderator"
                    3 -> "Member"
                    else -> "All"
                }
                updateFilterChipsUi()
                applyFilters()
            }
            .show()
    }

    private fun showStatusFilterDialog() {
        val statuses = arrayOf("All Statuses", "Active", "Restricted", "Suspended")
        AlertDialog.Builder(requireContext())
            .setTitle("Filter by Status")
            .setItems(statuses) { _, which ->
                selectedStatusFilter = when (which) {
                    1 -> "Active"
                    2 -> "Restricted"
                    3 -> "Suspended"
                    else -> "All"
                }
                updateFilterChipsUi()
                applyFilters()
            }
            .show()
    }

    private fun saveUserAccess(user: ManageableUser, newRole: String, newAccess: String) {
        if (user.isCurrentAdmin && (newRole != "Admin" || newAccess != "Active")) {
            Toast.makeText(requireContext(), "You cannot alter your own admin privileges.", Toast.LENGTH_SHORT).show()
            return
        }

        user.role = newRole
        user.access = newAccess

        val updates = mapOf<String, Any>(
            "role" to newRole.uppercase(),
            "isBanned" to (newAccess == "Suspended"),
            "isRestricted" to (newAccess == "Restricted"),
            "status" to newAccess.uppercase(),
            "access" to newAccess
        )

        FirebaseManager.usersRef.child(user.uid).updateChildren(updates)
            .addOnSuccessListener {
                if (!isAdded) return@addOnSuccessListener
                Toast.makeText(requireContext(), "Updated access for ${user.fullName}: $newRole ($newAccess)", Toast.LENGTH_SHORT).show()

                // Log audit action to Firebase /admin_logs
                val logId = FirebaseManager.adminLogsRef.push().key ?: "log_${System.currentTimeMillis()}"
                val log = AdminAction(
                    actionId = logId,
                    adminUid = FirebaseManager.currentUid,
                    targetId = "${user.fullName} (${user.uid}) · $newRole ($newAccess)",
                    actionType = "USER_ACCESS_UPDATE",
                    timestamp = System.currentTimeMillis()
                )
                FirebaseManager.adminLogsRef.child(logId).setValue(log)
            }
            .addOnFailureListener { e ->
                if (!isAdded) return@addOnFailureListener
                Toast.makeText(requireContext(), "Failed to update: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showSuspensionDialog(user: ManageableUser) {
        if (user.isCurrentAdmin) {
            Toast.makeText(requireContext(), "You cannot suspend your own admin account.", Toast.LENGTH_SHORT).show()
            return
        }

        if (user.access == "Suspended") {
            AlertDialog.Builder(requireContext())
                .setTitle("Reactivate Account")
                .setMessage("Lift suspension for ${user.fullName} and restore active status?")
                .setPositiveButton("Reactivate") { _, _ ->
                    saveUserAccess(user, user.role, "Active")
                    val index = displayedUsersList.indexOfFirst { it.uid == user.uid }
                    if (index >= 0) adapter.notifyItemChanged(index)
                }
                .setNegativeButton("Cancel", null)
                .show()
            return
        }

        val reasons = arrayOf(
            "Spam or false report submissions",
            "Multiple scam reports flagged by community",
            "Violation of community safety policy",
            "Harassment in live chat"
        )

        AlertDialog.Builder(requireContext())
            .setTitle("Suspend ${user.fullName}")
            .setItems(reasons) { _, which ->
                val reason = reasons[which]
                saveUserAccess(user, user.role, "Suspended")
                val index = displayedUsersList.indexOfFirst { it.uid == user.uid }
                if (index >= 0) adapter.notifyItemChanged(index)

                Toast.makeText(requireContext(), "Suspended ${user.fullName} for: $reason", Toast.LENGTH_LONG).show()
            }
            .setNegativeButton("Cancel", null)
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

            holder.rowUserHeader.setOnClickListener {
                onExpandToggle(holder.bindingAdapterPosition)
            }

            // Role selection dropdown
            holder.boxRoleSelect.setOnClickListener {
                val roles = arrayOf("Member", "Moderator", "Admin")
                AlertDialog.Builder(context)
                    .setTitle("Select Role for ${user.fullName}")
                    .setItems(roles) { _, which ->
                        val selectedRole = roles[which]
                        holder.lblRoleValue.text = selectedRole
                        user.role = selectedRole
                    }
                    .show()
            }

            // Access selection dropdown
            holder.boxAccessSelect.setOnClickListener {
                val accessOptions = arrayOf("Active", "Restricted", "Suspended")
                AlertDialog.Builder(context)
                    .setTitle("Select Access Status for ${user.fullName}")
                    .setItems(accessOptions) { _, which ->
                        val selectedAccess = accessOptions[which]
                        holder.lblAccessValue.text = selectedAccess
                        user.access = selectedAccess
                    }
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
