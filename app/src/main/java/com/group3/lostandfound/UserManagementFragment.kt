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
        setupBottomNav(view, "admin")
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
                allUsersList.clear()
                for (child in snapshot.children) {
                    try {
                        val uid = child.key ?: child.child("uid").getValue(String::class.java).orEmpty()
                        val email = child.child("email").getValue(String::class.java) ?: "user@foundly.lk"
                        val fullName = child.child("fullName").getValue(String::class.java)
                            ?: child.child("displayName").getValue(String::class.java)
                            ?: "Registered User"
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
                Toast.makeText(requireContext(), "Failed to sync users: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateSummaryCounts() {
        val total = allUsersList.size
        val banned = allUsersList.count { it.isBanned }
        val active = total - banned

        tvTotalUsersCount.text = "$total Users Registered"
        tvActiveUsersCount.text = "· $active Active"
        tvBannedUsersCount.text = "· $banned Banned"
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
        }

        val contentLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (14 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad, pad, pad)
        }

        // Row 1: Name and Role Badge
        val row1 = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
        }

        val tvName = TextView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            text = user.fullName
            textSize = 16f
            setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        row1.addView(tvName)

        val tvRoleBadge = TextView(requireContext()).apply {
            text = user.role.uppercase()
            textSize = 11f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            val isAdm = user.role.equals("ADMIN", ignoreCase = true)
            setTextColor(if (isAdm) android.graphics.Color.WHITE else android.graphics.Color.parseColor("#4A3B32"))
            setBackgroundResource(if (isAdm) R.drawable.bg_badge_admin else R.drawable.bg_badge_user)
            val hPad = (8 * resources.displayMetrics.density).toInt()
            val vPad = (3 * resources.displayMetrics.density).toInt()
            setPadding(hPad, vPad, hPad, vPad)
        }
        row1.addView(tvRoleBadge)
        contentLayout.addView(row1)

        // Row 2: Email & UID
        val tvEmail = TextView(requireContext()).apply {
            text = "${user.email} · Trust: ${user.trustScore}/100"
            textSize = 13f
            setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
            setPadding(0, (4 * resources.displayMetrics.density).toInt(), 0, (8 * resources.displayMetrics.density).toInt())
        }
        contentLayout.addView(tvEmail)

        // Row 3: Status & Action buttons
        val actionsRow = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
        }

        // Status Badge
        val tvStatus = TextView(requireContext()).apply {
            text = if (user.isBanned) "● BANNED" else "● ACTIVE"
            textSize = 12f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(if (user.isBanned) android.graphics.Color.parseColor("#DC2626") else android.graphics.Color.parseColor("#059669"))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        actionsRow.addView(tvStatus)

        // Ban / Unban Button
        val btnBan = TextView(requireContext()).apply {
            text = if (user.isBanned) "Unban" else "Ban User"
            textSize = 13f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(if (user.isBanned) android.graphics.Color.parseColor("#059669") else android.graphics.Color.parseColor("#DC2626"))
            setBackgroundResource(R.drawable.bg_btn_outline)
            val hPad = (12 * resources.displayMetrics.density).toInt()
            val vPad = (6 * resources.displayMetrics.density).toInt()
            setPadding(hPad, vPad, hPad, vPad)
            setOnClickListener {
                toggleBanStatus(user)
            }
        }
        actionsRow.addView(btnBan)

        // Role toggle button (Admin vs User)
        val btnRole = TextView(requireContext()).apply {
            val isAdm = user.role.equals("ADMIN", ignoreCase = true)
            text = if (isAdm) "Make User" else "Make Admin"
            textSize = 13f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(android.graphics.Color.parseColor("#4A3B32"))
            setBackgroundResource(R.drawable.bg_btn_outline)
            val hPad = (10 * resources.displayMetrics.density).toInt()
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

        // Verify & Assign Badge button (Admin Functional Requirement 3.2 #5)
        val btnBadge = TextView(requireContext()).apply {
            text = "Verify & Badge"
            textSize = 13f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
            setBackgroundResource(R.drawable.bg_btn_outline)
            val hPad = (10 * resources.displayMetrics.density).toInt()
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

        contentLayout.addView(actionsRow)
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
        val inputName = EditText(requireContext()).apply { hint = "Full Name" }
        val inputEmail = EditText(requireContext()).apply { hint = "Email Address" }

        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (16 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad, pad, pad)
            addView(inputName)
            addView(inputEmail)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Create Community User")
            .setView(layout)
            .setPositiveButton("Save User") { _, _ ->
                val name = inputName.text.toString().trim()
                val email = inputEmail.text.toString().trim()
                if (name.isNotEmpty() && email.isNotEmpty()) {
                    val uid = "user_" + System.currentTimeMillis()
                    val userDoc = mapOf(
                        "uid" to uid,
                        "fullName" to name,
                        "email" to email,
                        "role" to "USER",
                        "isBanned" to false,
                        "trustScore" to 85,
                        "createdAt" to System.currentTimeMillis()
                    )
                    FirebaseManager.usersRef.child(uid).setValue(userDoc)
                    Toast.makeText(requireContext(), "User created in database.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }


    companion object {
        fun newInstance(): UserManagementFragment = UserManagementFragment()
    }
}
