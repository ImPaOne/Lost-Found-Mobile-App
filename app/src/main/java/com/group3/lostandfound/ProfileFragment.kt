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
            Toast.makeText(requireContext(), "Saved items: 4 items saved", Toast.LENGTH_SHORT).show()
        }

        rowTrustScore.setOnClickListener {
            navigateTo(TrustFragment.newInstance())
        }

        rowSettings.setOnClickListener {
            navigateTo(SettingsFragment.newInstance())
        }

        rowHelp.setOnClickListener {
            Toast.makeText(requireContext(), "Help & Support Center", Toast.LENGTH_SHORT).show()
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
