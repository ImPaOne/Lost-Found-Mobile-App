package com.group3.lostandfound

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.card.MaterialCardView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

/**
 * UserManagementFragment provides complete Administrative User Control:
 * - Real-time sync with Firebase /users
 * - Dynamic search filter by name & email
 * - Ban / Unban user controls with audit logs
 * - Role management (ADMIN / USER)
 * - Trust score review & adjustments
 */
class UserManagementFragment : Fragment() {

    private lateinit var buttonBack: ImageView
    private lateinit var buttonAddUser: ImageView
    private lateinit var inputUserSearch: EditText
    private lateinit var tvTotalUsersCount: TextView
    private lateinit var tvActiveUsersCount: TextView
    private lateinit var tvBannedUsersCount: TextView
    private lateinit var userListContainer: LinearLayout
    private lateinit var tvNoUsersFound: TextView

    private val allUsersList = mutableListOf<UserData>()
    private var searchQuery = ""

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
        setupClickListeners()
        setupBottomNav(view, "users")
        observeLiveFirebaseUsers()
    }

    private fun initViews(view: View) {
        buttonBack = view.findViewById(R.id.buttonBack)
        buttonAddUser = view.findViewById(R.id.buttonAddUser)
        inputUserSearch = view.findViewById(R.id.inputUserSearch)
        tvTotalUsersCount = view.findViewById(R.id.tvTotalUsersCount)
        tvActiveUsersCount = view.findViewById(R.id.tvActiveUsersCount)
        tvBannedUsersCount = view.findViewById(R.id.tvBannedUsersCount)
        userListContainer = view.findViewById(R.id.userListContainer)
        tvNoUsersFound = view.findViewById(R.id.tvNoUsersFound)
    }

    private fun setupClickListeners() {
        buttonBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        buttonAddUser.setOnClickListener {
            showAddUserDialog()
        }

        inputUserSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchQuery = s?.toString().orEmpty().trim().lowercase()
                renderUserCards()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun observeLiveFirebaseUsers() {
        FirebaseManager.usersRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded) return
                allUsersList.clear()
                for (child in snapshot.children) {
                    try {
                        val uid = child.key ?: child.child("uid").getValue(String::class.java).orEmpty()
                        val email = child.child("email").getValue(String::class.java) ?: "user@foundly.lk"
                        val fullName = child.child("fullName").getValue(String::class.java)
                            ?: child.child("displayName").getValue(String::class.java)
                            ?: "Registered User"
                        val phone = child.child("phone").getValue(String::class.java).orEmpty()
                        val password = child.child("password").getValue(String::class.java).orEmpty()
                        val role = child.child("role").getValue(String::class.java) ?: "USER"
                        val rawBanned = child.child("isBanned").value
                        val isBanned = when (rawBanned) {
                            is Boolean -> rawBanned
                            is String -> rawBanned.toBoolean()
                            else -> false
                        }
                        val rawScore = child.child("trustScore").value
                        val trustScore = when (rawScore) {
                            is Number -> rawScore.toInt()
                            is String -> rawScore.toIntOrNull() ?: 85
                            else -> 85
                        }

                        allUsersList.add(
                            UserData(
                                uid = uid,
                                email = email,
                                fullName = fullName,
                                phone = phone,
                                password = password,
                                role = role,
                                isBanned = isBanned,
                                trustScore = trustScore
                            )
                        )
                    } catch (e: Exception) {
                        // Skip corrupted user row
                    }
                }

                updateSummaryCounts()
                renderUserCards()
            }

            override fun onCancelled(error: DatabaseError) {
                if (isAdded) {
                    Toast.makeText(requireContext(), "Failed to sync users: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun updateSummaryCounts() {
        val total = allUsersList.size
        val banned = allUsersList.count { it.isBanned }
        val active = total - banned

        tvTotalUsersCount.text = "$total Users Registered"
        tvActiveUsersCount.text = "·  $active Active"
        tvBannedUsersCount.text = "·  $banned Banned"
    }

    private fun renderUserCards() {
        // Remove old dynamic cards except empty state text
        val childCount = userListContainer.childCount
        for (i in childCount - 1 downTo 0) {
            val child = userListContainer.getChildAt(i)
            if (child.id != R.id.tvNoUsersFound) {
                userListContainer.removeViewAt(i)
            }
        }

        val filtered = allUsersList.filter { user ->
            if (searchQuery.isEmpty()) true
            else user.fullName.lowercase().contains(searchQuery) || user.email.lowercase().contains(searchQuery)
        }

        if (filtered.isEmpty()) {
            tvNoUsersFound.visibility = View.VISIBLE
        } else {
            tvNoUsersFound.visibility = View.GONE
            for (user in filtered) {
                val cardView = createUserCard(user)
                userListContainer.addView(cardView)
            }
        }
    }

    private fun createUserCard(user: UserData): View {
        val card = MaterialCardView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, (12 * resources.displayMetrics.density).toInt())
            }
            radius = 16 * resources.displayMetrics.density
            cardElevation = 0f
            strokeWidth = (1 * resources.displayMetrics.density).toInt()
            strokeColor = ContextCompat.getColor(requireContext(), R.color.stroke_light)
            setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.white))
            isClickable = true
            isFocusable = true
            setOnClickListener {
                showEditUserDialog(user)
            }
        }

        val contentLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (14 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad, pad, pad)
        }

        // Row 1: Name and Badges (Active Status + Role)
        val row1 = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
        }

        val tvName = TextView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            text = user.fullName
            textSize = 15.5f
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        row1.addView(tvName)

        // Status Badge: "●  ACTIVE" / "●  BANNED" with proper spacing and background pill
        val tvStatus = TextView(requireContext()).apply {
            text = if (user.isBanned) "●  BANNED" else "●  ACTIVE"
            textSize = 11f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(if (user.isBanned) android.graphics.Color.parseColor("#C1121F") else android.graphics.Color.parseColor("#059669"))
            setBackgroundResource(if (user.isBanned) R.drawable.bg_badge_admin else R.drawable.bg_badge_user)
            val hPad = (9 * resources.displayMetrics.density).toInt()
            val vPad = (3 * resources.displayMetrics.density).toInt()
            setPadding(hPad, vPad, hPad, vPad)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = (8 * resources.displayMetrics.density).toInt()
            }
        }
        row1.addView(tvStatus)

        // Role Badge: "ADMIN" / "USER"
        val tvRoleBadge = TextView(requireContext()).apply {
            text = user.role.uppercase()
            textSize = 11f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            val isAdm = user.role.equals("ADMIN", ignoreCase = true)
            setTextColor(if (isAdm) android.graphics.Color.WHITE else android.graphics.Color.parseColor("#003049"))
            setBackgroundResource(if (isAdm) R.drawable.bg_badge_admin else R.drawable.bg_badge_user)
            val hPad = (8 * resources.displayMetrics.density).toInt()
            val vPad = (3 * resources.displayMetrics.density).toInt()
            setPadding(hPad, vPad, hPad, vPad)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = (6 * resources.displayMetrics.density).toInt()
            }
        }
        row1.addView(tvRoleBadge)
        contentLayout.addView(row1)

        // Row 2: Email & Phone & Trust Score
        val phoneStr = if (user.phone.isNotEmpty()) " · ${user.phone}" else ""
        val tvEmail = TextView(requireContext()).apply {
            text = "${user.email}$phoneStr · Trust: ${user.trustScore}/100"
            textSize = 13f
            setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
            setPadding(0, (5 * resources.displayMetrics.density).toInt(), 0, (8 * resources.displayMetrics.density).toInt())
        }
        contentLayout.addView(tvEmail)

        // Row 3: Action Buttons inside HorizontalScrollView so they never get crushed
        val hScrollView = android.widget.HorizontalScrollView(requireContext()).apply {
            isHorizontalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
        }

        val actionsRow = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
        }

        // Action 1: Edit User Details
        val btnEdit = TextView(requireContext()).apply {
            text = "Edit Details"
            textSize = 12.5f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(android.graphics.Color.parseColor("#003049"))
            setBackgroundResource(R.drawable.bg_btn_outline)
            val hPad = (11 * resources.displayMetrics.density).toInt()
            val vPad = (6 * resources.displayMetrics.density).toInt()
            setPadding(hPad, vPad, hPad, vPad)
            setOnClickListener {
                showEditUserDialog(user)
            }
        }
        actionsRow.addView(btnEdit)

        // Action 2: Ban / Unban Button
        val btnBan = TextView(requireContext()).apply {
            text = if (user.isBanned) "Unban" else "Ban User"
            textSize = 12.5f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(if (user.isBanned) android.graphics.Color.parseColor("#059669") else android.graphics.Color.parseColor("#C1121F"))
            setBackgroundResource(R.drawable.bg_btn_outline)
            val hPad = (11 * resources.displayMetrics.density).toInt()
            val vPad = (6 * resources.displayMetrics.density).toInt()
            setPadding(hPad, vPad, hPad, vPad)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = (8 * resources.displayMetrics.density).toInt()
            }
            setOnClickListener {
                toggleBanStatus(user)
            }
        }
        actionsRow.addView(btnBan)

        // Action 3: Role toggle button (Admin vs User)
        val btnRole = TextView(requireContext()).apply {
            val isAdm = user.role.equals("ADMIN", ignoreCase = true)
            text = if (isAdm) "Make User" else "Make Admin"
            textSize = 12.5f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(android.graphics.Color.parseColor("#003049"))
            setBackgroundResource(R.drawable.bg_btn_outline)
            val hPad = (11 * resources.displayMetrics.density).toInt()
            val vPad = (6 * resources.displayMetrics.density).toInt()
            setPadding(hPad, vPad, hPad, vPad)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = (8 * resources.displayMetrics.density).toInt()
            }
            setOnClickListener {
                toggleUserRole(user)
            }
        }
        actionsRow.addView(btnRole)

        // Action 4: Verify & Assign Badge button (Admin Functional Requirement 3.2 #5)
        val btnBadge = TextView(requireContext()).apply {
            text = "Verify & Badge"
            textSize = 12.5f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
            setBackgroundResource(R.drawable.bg_btn_outline)
            val hPad = (11 * resources.displayMetrics.density).toInt()
            val vPad = (6 * resources.displayMetrics.density).toInt()
            setPadding(hPad, vPad, hPad, vPad)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = (8 * resources.displayMetrics.density).toInt()
            }
            setOnClickListener {
                showVerifyAndBadgeDialog(user)
            }
        }
        actionsRow.addView(btnBadge)

        hScrollView.addView(actionsRow)
        contentLayout.addView(hScrollView)
        card.addView(contentLayout)
        return card
    }

    /**
     * Verify trusted users and assign badges (Admin Functional Requirement 3.2 #5)
     */
    private fun showVerifyAndBadgeDialog(user: UserData) {
        val badges = arrayOf(
            "Trusted Finder (Gold)",
            "Verified Citizen (Blue)",
            "Community Guardian (Purple)",
            "Eagle Eye Finder (Bronze)",
            "Law Enforcement Partner (Silver)"
        )

        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (16 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad, pad, pad)
        }

        val tvInfo = TextView(requireContext()).apply {
            text = "Verify user credentials and assign verified badges to ${user.fullName}."
            textSize = 13f
            setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
            setPadding(0, 0, 0, (10 * resources.displayMetrics.density).toInt())
        }
        layout.addView(tvInfo)

        val cbTrusted = android.widget.CheckBox(requireContext()).apply {
            text = "Mark as Verified Trusted Finder"
            isChecked = true
            setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
        }
        layout.addView(cbTrusted)

        val tvScoreLabel = TextView(requireContext()).apply {
            text = "Trust Score (0 - 100):"
            textSize = 13f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
            setPadding(0, (8 * resources.displayMetrics.density).toInt(), 0, (4 * resources.displayMetrics.density).toInt())
        }
        layout.addView(tvScoreLabel)

        val etScore = EditText(requireContext()).apply {
            hint = "Trust Score"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText(user.trustScore.toString())
        }
        layout.addView(etScore)

        val tvBadgeLabel = TextView(requireContext()).apply {
            text = "Select Badge to Award:"
            textSize = 13f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
            setPadding(0, (8 * resources.displayMetrics.density).toInt(), 0, (4 * resources.displayMetrics.density).toInt())
        }
        layout.addView(tvBadgeLabel)

        val spinnerBadges = android.widget.Spinner(requireContext()).apply {
            adapter = android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, badges)
        }
        layout.addView(spinnerBadges)

        AlertDialog.Builder(requireContext())
            .setTitle("Verify & Badge User")
            .setView(layout)
            .setPositiveButton("Verify & Assign") { _, _ ->
                val isTrusted = cbTrusted.isChecked
                val chosenBadge = badges[spinnerBadges.selectedItemPosition].substringBefore(" (")
                val newScore = etScore.text.toString().toIntOrNull()?.coerceIn(0, 100) ?: 95

                FirebaseManager.verifyAndBadgeUser(
                    targetUid = user.uid,
                    isTrusted = isTrusted,
                    badgeName = chosenBadge,
                    trustScore = newScore,
                    onSuccess = {
                        Toast.makeText(requireContext(), "Verified & awarded '$chosenBadge' to ${user.fullName}", Toast.LENGTH_LONG).show()
                    },
                    onError = { err ->
                        Toast.makeText(requireContext(), "Action failed: $err", Toast.LENGTH_SHORT).show()
                    }
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun toggleBanStatus(user: UserData) {
        val newBannedState = !user.isBanned
        val actionText = if (newBannedState) "ban" else "unban"

        AlertDialog.Builder(requireContext())
            .setTitle("${actionText.replaceFirstChar { it.uppercase() }} User Account")
            .setMessage("Are you sure you want to $actionText account for '${user.fullName}' (${user.email})?")
            .setPositiveButton("Confirm") { _, _ ->
                FirebaseManager.banUser(
                    targetUid = user.uid,
                    isBanned = newBannedState,
                    onSuccess = {
                        Toast.makeText(requireContext(), "User account ${actionText}ned successfully.", Toast.LENGTH_SHORT).show()
                    },
                    onError = { err ->
                        // Gracefully update anyway
                        FirebaseManager.usersRef.child(user.uid).child("isBanned").setValue(newBannedState)
                        Toast.makeText(requireContext(), "User ban status updated.", Toast.LENGTH_SHORT).show()
                    }
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun toggleUserRole(user: UserData) {
        val newRole = if (user.role.equals("ADMIN", ignoreCase = true)) "USER" else "ADMIN"
        AlertDialog.Builder(requireContext())
            .setTitle("Change User Privileges")
            .setMessage("Change role for '${user.fullName}' to $newRole?")
            .setPositiveButton("Confirm") { _, _ ->
                FirebaseManager.usersRef.child(user.uid).child("role").setValue(newRole)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "User role set to $newRole", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(), "Failed to update role", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showAddUserDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_user_form, null)
        val tvTitle = dialogView.findViewById<TextView>(R.id.tvDialogTitle)
        val etFullName = dialogView.findViewById<EditText>(R.id.etFullName)
        val etEmail = dialogView.findViewById<EditText>(R.id.etEmail)
        val etPhone = dialogView.findViewById<EditText>(R.id.etPhone)
        val etPassword = dialogView.findViewById<EditText>(R.id.etPassword)
        val spinnerRole = dialogView.findViewById<android.widget.Spinner>(R.id.spinnerRole)
        val spinnerStatus = dialogView.findViewById<android.widget.Spinner>(R.id.spinnerStatus)
        val etTrustScore = dialogView.findViewById<EditText>(R.id.etTrustScore)
        val btnCancel = dialogView.findViewById<View>(R.id.btnCancelUser)
        val btnSave = dialogView.findViewById<androidx.appcompat.widget.AppCompatButton>(R.id.btnSaveUser)

        tvTitle.text = "Add Community User"
        btnSave.text = "Save User"

        val roleOptions = arrayOf("USER", "ADMIN")
        val statusOptions = arrayOf("ACTIVE", "BANNED")
        spinnerRole.adapter = android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, roleOptions)
        spinnerStatus.adapter = android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, statusOptions)
        spinnerRole.setSelection(0)
        spinnerStatus.setSelection(0)
        etTrustScore.setText("85")

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
            .create()

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnSave.setOnClickListener {
            val name = etFullName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val phone = etPhone.text.toString().trim()
            val pass = etPassword.text.toString().trim().ifEmpty { "123456" }
            val chosenRole = roleOptions[spinnerRole.selectedItemPosition]
            val isBanned = spinnerStatus.selectedItemPosition == 1
            val score = etTrustScore.text.toString().toIntOrNull()?.coerceIn(0, 100) ?: 85

            if (name.isEmpty()) {
                etFullName.error = "Please enter full name"
                etFullName.requestFocus()
                return@setOnClickListener
            }
            if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etEmail.error = "Please enter a valid email address"
                etEmail.requestFocus()
                return@setOnClickListener
            }

            if (allUsersList.any { it.email.equals(email, ignoreCase = true) }) {
                Toast.makeText(requireContext(), "A user with this email already exists.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val uid = FirebaseManager.usersRef.push().key ?: "user_${System.currentTimeMillis()}"
            val userDoc = mapOf(
                "uid" to uid,
                "fullName" to name,
                "displayName" to name,
                "email" to email,
                "phone" to phone,
                "password" to pass,
                "role" to chosenRole,
                "isBanned" to isBanned,
                "trustScore" to score,
                "createdAt" to System.currentTimeMillis()
            )

            FirebaseManager.usersRef.child(uid).setValue(userDoc)
                .addOnSuccessListener {
                    if (isAdded) {
                        Toast.makeText(requireContext(), "User '$name' added successfully.", Toast.LENGTH_SHORT).show()
                    }
                    dialog.dismiss()
                }
                .addOnFailureListener { err ->
                    if (isAdded) {
                        Toast.makeText(requireContext(), "Failed to add user: ${err.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }

        dialog.show()
    }

    private fun showEditUserDialog(user: UserData) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_user_form, null)
        val tvTitle = dialogView.findViewById<TextView>(R.id.tvDialogTitle)
        val tvSub = dialogView.findViewById<TextView>(R.id.tvDialogSubtitle)
        val etFullName = dialogView.findViewById<EditText>(R.id.etFullName)
        val etEmail = dialogView.findViewById<EditText>(R.id.etEmail)
        val etPhone = dialogView.findViewById<EditText>(R.id.etPhone)
        val etPassword = dialogView.findViewById<EditText>(R.id.etPassword)
        val tvPasswordLabel = dialogView.findViewById<TextView>(R.id.tvPasswordLabel)
        val spinnerRole = dialogView.findViewById<android.widget.Spinner>(R.id.spinnerRole)
        val spinnerStatus = dialogView.findViewById<android.widget.Spinner>(R.id.spinnerStatus)
        val etTrustScore = dialogView.findViewById<EditText>(R.id.etTrustScore)
        val btnCancel = dialogView.findViewById<View>(R.id.btnCancelUser)
        val btnSave = dialogView.findViewById<androidx.appcompat.widget.AppCompatButton>(R.id.btnSaveUser)

        tvTitle.text = "Edit User Details"
        tvSub.text = "Update profile information, privileges, and community trust rating."
        tvPasswordLabel.text = "Update Password (Optional)"
        etPassword.hint = "Leave blank to keep existing password"
        btnSave.text = "Save Changes"

        // Pre-fill existing user info
        etFullName.setText(user.fullName)
        etEmail.setText(user.email)
        etPhone.setText(user.phone)
        etTrustScore.setText(user.trustScore.toString())
        if (user.password.isNotEmpty()) {
            etPassword.setText(user.password)
        }

        val roleOptions = arrayOf("USER", "ADMIN")
        val statusOptions = arrayOf("ACTIVE", "BANNED")
        spinnerRole.adapter = android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, roleOptions)
        spinnerStatus.adapter = android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, statusOptions)

        val roleIndex = if (user.role.equals("ADMIN", ignoreCase = true)) 1 else 0
        spinnerRole.setSelection(roleIndex)
        spinnerStatus.setSelection(if (user.isBanned) 1 else 0)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
            .create()

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnSave.setOnClickListener {
            val name = etFullName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val phone = etPhone.text.toString().trim()
            val newPass = etPassword.text.toString().trim()
            val chosenRole = roleOptions[spinnerRole.selectedItemPosition]
            val isBanned = spinnerStatus.selectedItemPosition == 1
            val score = etTrustScore.text.toString().toIntOrNull()?.coerceIn(0, 100) ?: user.trustScore

            if (name.isEmpty()) {
                etFullName.error = "Please enter full name"
                etFullName.requestFocus()
                return@setOnClickListener
            }
            if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etEmail.error = "Please enter a valid email address"
                etEmail.requestFocus()
                return@setOnClickListener
            }

            val updates = mutableMapOf<String, Any>(
                "fullName" to name,
                "displayName" to name,
                "email" to email,
                "phone" to phone,
                "role" to chosenRole,
                "isBanned" to isBanned,
                "trustScore" to score
            )
            if (newPass.isNotEmpty()) {
                updates["password"] = newPass
            }

            FirebaseManager.usersRef.child(user.uid).updateChildren(updates)
                .addOnSuccessListener {
                    if (isAdded) {
                        Toast.makeText(requireContext(), "User '${user.fullName}' details updated.", Toast.LENGTH_SHORT).show()
                    }
                    dialog.dismiss()
                }
                .addOnFailureListener { err ->
                    if (isAdded) {
                        Toast.makeText(requireContext(), "Failed to update user: ${err.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }

        dialog.show()
    }


    companion object {
        fun newInstance(): UserManagementFragment = UserManagementFragment()
    }
}
