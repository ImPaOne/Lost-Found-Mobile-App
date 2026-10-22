package com.group3.lostandfound

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

/**
 * ProfileFragment displays user profile information, activity statistics,
 * and handles real-time synchronization with Firebase Realtime Database at `/users/{uid}`.
 * Provides controls to update displayName and preferredLanguage via updateChildren().
 */
class ProfileFragment : Fragment() {

    private lateinit var userName: TextView
    private lateinit var userSubtitle: TextView
    private lateinit var countItemsFound: TextView

    private val currentUid = FirebaseManager.currentUid
    private val userRef = FirebaseDatabase.getInstance(FirebaseManager.DATABASE_URL)
        .getReference("users")
        .child(currentUid)

    private var profileListener: ValueEventListener? = null
    private var currentDisplayName = "Community Member"
    private var currentLanguage = "en"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnClose = view.findViewById<ImageView>(R.id.btnClose)
        val btnMenu = view.findViewById<ImageView>(R.id.btnMenu)
        userName = view.findViewById(R.id.userName)
        userSubtitle = view.findViewById(R.id.userSubtitle)
        countItemsFound = view.findViewById(R.id.countItemsFound)

        val rowReports = view.findViewById<LinearLayout>(R.id.rowReports)
        val rowSavedItems = view.findViewById<LinearLayout>(R.id.rowSavedItems)
        val rowTrustScore = view.findViewById<LinearLayout>(R.id.rowTrustScore)
        val rowSettings = view.findViewById<LinearLayout>(R.id.rowSettings)
        val rowHelp = view.findViewById<LinearLayout>(R.id.rowHelp)
        val rowLogout = view.findViewById<LinearLayout>(R.id.rowLogout)

        // Real-time observation of /users/{uid}
        observeUserProfile()

        // UI control: Tap name to update displayName & preferredLanguage via updateChildren()
        userName.setOnClickListener {
            showEditProfileDialog()
        }

        btnClose.setOnClickListener {
            if (parentFragmentManager.backStackEntryCount > 0) {
                parentFragmentManager.popBackStack()
            } else {
                navigateTo(HomeFragment.newInstance())
            }
        }

        btnMenu.setOnClickListener {
            val prefs = requireContext().getSharedPreferences("findit_prefs", android.content.Context.MODE_PRIVATE)
            val isAdmin = prefs.getBoolean("is_admin", false) && FirebaseManager.currentUserEmail.equals("admin@gmail.com", ignoreCase = true)
            if (isAdmin) {
                navigateTo(AdminFragment.newInstance())
            } else {
                navigateTo(SettingsFragment.newInstance())
            }
        }

        rowReports.setOnClickListener {
            navigateTo(ReportsFragment.newInstance())
        }

        rowSavedItems.setOnClickListener {
            try {
                showSavedItemsDialog()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Saved items error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        rowTrustScore.setOnClickListener {
            try {
                navigateTo(TrustFragment.newInstance())
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Trust screen error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        rowSettings.setOnClickListener {
            try {
                navigateTo(SettingsFragment.newInstance())
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Settings screen error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        rowHelp.setOnClickListener {
            try {
                showHelpCenterDialog()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Help Center error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        rowLogout.setOnClickListener {
            requireContext().getSharedPreferences("findit_prefs", android.content.Context.MODE_PRIVATE)
                .edit()
                .putBoolean("is_logged_in", false)
                .putBoolean("is_admin", false)
                .apply()
            com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
            Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show()
            parentFragmentManager.beginTransaction()
                .replace(R.id.screenBox, LoginFragment.newInstance())
                .commit()
        }

        setupBottomNav(view, "profile")
    }

    private fun observeUserProfile() {
        profileListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    currentDisplayName = snapshot.child("displayName").getValue(String::class.java)
                        ?: snapshot.child("fullName").getValue(String::class.java)
                        ?: "Community Member"
                    currentLanguage = snapshot.child("preferredLanguage").getValue(String::class.java) ?: "en"
                    val isTrusted = snapshot.child("isTrustedFinder").getValue(Boolean::class.java) ?: false

                    userName.text = currentDisplayName
                    userSubtitle.text = "Language: ${currentLanguage.uppercase()} · ${if (isTrusted) "Trusted Finder" else "Community Member"}"
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Silently handle error
            }
        }
        profileListener?.let { userRef.addValueEventListener(it) }
    }

    private fun showEditProfileDialog() {
        val input = EditText(requireContext()).apply {
            setText(currentDisplayName)
            setSelection(text.length)
            hint = "Enter display name"
        }

        val languages = arrayOf("English (en)", "Sinhala (si)", "Tamil (ta)")
        var selectedLangIndex = when (currentLanguage) {
            "si" -> 1
            "ta" -> 2
            else -> 0
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Profile")
            .setView(input)
            .setSingleChoiceItems(languages, selectedLangIndex) { _, which ->
                selectedLangIndex = which
            }
            .setPositiveButton("Save") { _, _ ->
                val newName = input.text.toString().trim().ifEmpty { currentDisplayName }
                val newLang = when (selectedLangIndex) {
                    1 -> "si"
                    2 -> "ta"
                    else -> "en"
                }

                // Update /users/{uid} using updateChildren()
                val updates = mapOf(
                    "displayName" to newName,
                    "preferredLanguage" to newLang
                )
                userRef.updateChildren(updates).addOnSuccessListener {
                    Toast.makeText(requireContext(), "Profile updated in Firebase!", Toast.LENGTH_SHORT).show()
                }.addOnFailureListener {
                    Toast.makeText(requireContext(), "Failed: ${it.message}", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showSavedItemsDialog() {
        val prefs = requireContext().getSharedPreferences("findit_bookmarks", android.content.Context.MODE_PRIVATE)
        val savedIds = prefs.getStringSet("bookmarked_ids", emptySet()) ?: emptySet()

        if (savedIds.isEmpty()) {
            AlertDialog.Builder(requireContext())
                .setTitle("Saved Items (0)")
                .setMessage("No bookmarked reports yet.\n\nBrowse items and tap the bookmark ribbon icon on any item detail page to save it here for instant access.")
                .setPositiveButton("Browse Items") { _, _ ->
                    navigateTo(HomeFragment.newInstance())
                }
                .setNegativeButton("Close", null)
                .show()
            return
        }

        FirebaseManager.itemsRef.get().addOnSuccessListener { snapshot ->
            val matchingItems = mutableListOf<com.group3.lostandfound.model.ItemReport>()
            for (child in snapshot.children) {
                val id = child.child("itemId").getValue(String::class.java) ?: child.key.orEmpty()
                if (savedIds.contains(id)) {
                    val report = child.getValue(com.group3.lostandfound.model.ItemReport::class.java)
                    if (report != null) {
                        matchingItems.add(report.copy(itemId = id))
                    }
                }
            }

            if (matchingItems.isEmpty()) {
                AlertDialog.Builder(requireContext())
                    .setTitle("Saved Items")
                    .setMessage("Saved reports are no longer available in the active directory.")
                    .setPositiveButton("OK", null)
                    .show()
            } else {
                val itemTitles = matchingItems.map { "${it.title} (${it.status}) · ${it.location}" }.toTypedArray()
                AlertDialog.Builder(requireContext())
                    .setTitle("Saved Items (${matchingItems.size})")
                    .setItems(itemTitles) { _, which ->
                        val selected = matchingItems[which]
                        navigateTo(DetailsFragment.newInstance(selected.itemId, selected.title, selected.description, selected.status, selected.location, selected.imageUrl, selected.userId))
                    }
                    .setNeutralButton("Clear All") { _, _ ->
                        prefs.edit().remove("bookmarked_ids").apply()
                        Toast.makeText(requireContext(), "Saved bookmarks cleared", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("Close", null)
                    .show()
            }
        }.addOnFailureListener {
            Toast.makeText(requireContext(), "Failed to load saved items: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showHelpCenterDialog() {
        val helpTopics = arrayOf(
            "Frequently Asked Questions (FAQ)",
            "Safety & Handover Guidelines",
            "Emergency Community Helpline (1919)",
            "Submit Support Request / Contact Us"
        )

        AlertDialog.Builder(requireContext())
            .setTitle("Foundly Support Center")
            .setItems(helpTopics) { _, which ->
                when (which) {
                    0 -> {
                        AlertDialog.Builder(requireContext())
                            .setTitle("Foundly FAQs")
                            .setMessage("Q: How do I report a lost item?\nA: Tap 'Report Lost Item' on Home or '+ Post' tab, follow the 5 simple steps, and publish.\n\nQ: How do I contact a finder?\nA: Open the item details and tap 'Contact Reporter' to initiate real-time chat with photo attachments.\n\nQ: How does Trust Score work?\nA: You earn Trust Points by submitting verified reports and confirming reunifications.")
                            .setPositiveButton("Got It", null)
                            .show()
                    }
                    1 -> {
                        AlertDialog.Builder(requireContext())
                            .setTitle("Safety & Verification Guidelines")
                            .setMessage("1. Always meet in well-lit public places (e.g. police stations, malls, transit hubs).\n2. Request verifiable proof of ownership before handing over items.\n3. Never share sensitive banking PINs or OTPs.")
                            .setPositiveButton("I Understand", null)
                            .show()
                    }
                    2 -> {
                        AlertDialog.Builder(requireContext())
                            .setTitle("Emergency Helpline")
                            .setMessage("Sri Lanka National Emergency Response: 119\nGovernment Information Center: 1919\nFoundly 24/7 Community Desk: support@foundly.lk")
                            .setPositiveButton("Call 1919") { _, _ ->
                                try {
                                    val dialIntent = android.content.Intent(android.content.Intent.ACTION_DIAL).apply {
                                        data = android.net.Uri.parse("tel:1919")
                                    }
                                    startActivity(dialIntent)
                                } catch (_: Exception) {}
                            }
                            .setNegativeButton("Dismiss", null)
                            .show()
                    }
                    3 -> {
                        val input = EditText(requireContext()).apply {
                            hint = "Describe your issue or feedback..."
                            minLines = 3
                        }
                        AlertDialog.Builder(requireContext())
                            .setTitle("Contact Support")
                            .setView(input)
                            .setPositiveButton("Submit") { _, _ ->
                                val msg = input.text.toString().trim()
                                if (msg.isNotEmpty()) {
                                    val ticket = mapOf(
                                        "userEmail" to FirebaseManager.currentUserEmail,
                                        "userId" to FirebaseManager.currentUid,
                                        "message" to msg,
                                        "timestamp" to System.currentTimeMillis(),
                                        "status" to "OPEN"
                                    )
                                    FirebaseManager.database.getReference("contact_requests").push().setValue(ticket)
                                    Toast.makeText(requireContext(), "Support inquiry submitted. Administrator will respond shortly.", Toast.LENGTH_LONG).show()
                                }
                            }
                            .setNegativeButton("Cancel", null)
                            .show()
                    }
                }
            }
            .setNegativeButton("Close", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        profileListener?.let { userRef.removeEventListener(it) }
    }

    private fun navigateTo(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.screenBox, fragment)
            .addToBackStack(null)
            .commit()
    }

    companion object {
        fun newInstance(): ProfileFragment = ProfileFragment()
    }
}
