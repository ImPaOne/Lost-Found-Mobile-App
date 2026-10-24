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
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.group3.lostandfound.util.ImageLoader
import com.group3.lostandfound.util.ModernToast
import kotlinx.coroutines.launch

/**
 * ProfileFragment displays user profile information, activity statistics,
 * and handles real-time synchronization with Firebase Realtime Database at `/users/{uid}`.
 * Provides controls to update displayName and preferredLanguage via updateChildren().
 */
class ProfileFragment : Fragment() {

    private lateinit var userAvatar: ImageView
    private lateinit var boxAvatarContainer: View
    private lateinit var userName: TextView
    private lateinit var userSubtitle: TextView
    private lateinit var countItemsFound: TextView
    private lateinit var countItemsReturned: TextView
    private lateinit var countTrustScore: TextView

    private val currentUid = FirebaseManager.currentUid
    private val userRef = FirebaseDatabase.getInstance(FirebaseManager.DATABASE_URL)
        .getReference("users")
        .child(currentUid)

    private var profileListener: ValueEventListener? = null
    private var itemStatsListener: ValueEventListener? = null
    private var currentDisplayName = "Community Member"
    private var currentLanguage = "en"
    private var currentPhone = ""
    private var currentPhotoUrl = ""
    private var activeDialogAvatarPreview: ImageView? = null

    private val pickProfileAvatarLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            uploadProfileImage(uri)
        }
    }

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
        userAvatar = view.findViewById(R.id.userAvatar)
        boxAvatarContainer = view.findViewById(R.id.boxAvatarContainer)
        userName = view.findViewById(R.id.userName)
        userSubtitle = view.findViewById(R.id.userSubtitle)
        countItemsFound = view.findViewById(R.id.countItemsFound)
        countItemsReturned = view.findViewById(R.id.countItemsReturned)
        countTrustScore = view.findViewById(R.id.countTrustScore)

        countItemsFound.text = "0"
        countItemsReturned.text = "0"
        countTrustScore.text = "50%"

        val rowReports = view.findViewById<LinearLayout>(R.id.rowReports)
        val rowSavedItems = view.findViewById<LinearLayout>(R.id.rowSavedItems)
        val rowTrustScore = view.findViewById<LinearLayout>(R.id.rowTrustScore)
        val rowSettings = view.findViewById<LinearLayout>(R.id.rowSettings)
        val rowHelp = view.findViewById<LinearLayout>(R.id.rowHelp)
        val rowLogout = view.findViewById<LinearLayout>(R.id.rowLogout)

        // Real-time observation of /users/{uid} and user item stats
        observeUserProfile()
        observeUserItemStats()

        // Tap avatar or name to open modern profile editor & photo upload
        boxAvatarContainer.setOnClickListener {
            showEditProfileDialog()
        }
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
            navigateTo(SavedItemsFragment.newInstance())
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
                    currentPhone = snapshot.child("phone").getValue(String::class.java)
                        ?: snapshot.child("phoneNumber").getValue(String::class.java)
                        ?: ""
                    currentPhotoUrl = snapshot.child("photoUrl").getValue(String::class.java)
                        ?: snapshot.child("avatarUrl").getValue(String::class.java)
                        ?: ""
                    val isTrusted = snapshot.child("isTrustedFinder").getValue(Boolean::class.java) ?: false

                    userName.text = currentDisplayName
                    userSubtitle.text = "Language: ${currentLanguage.uppercase()} · ${if (isTrusted) "Trusted Finder" else "Community Member"}"

                    if (currentPhotoUrl.isNotEmpty()) {
                        ImageLoader.load(userAvatar, currentPhotoUrl, R.drawable.ic_user_avatar, viewLifecycleOwner.lifecycleScope)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Silently handle error
            }
        }
        profileListener?.let { userRef.addValueEventListener(it) }
    }

    private fun observeUserItemStats() {
        val uid = currentUid
        itemStatsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded) return
                var foundCount = 0
                var returnedCount = 0

                for (child in snapshot.children) {
                    val report = com.group3.lostandfound.model.ItemReport.fromSnapshot(child)
                    if (report.userId == uid && uid.isNotEmpty()) {
                        val status = report.status.uppercase()
                        if (status == "FOUND") {
                            foundCount++
                        } else if (status == "RESOLVED" || status == "RETURNED") {
                            returnedCount++
                        }
                    }
                }
                
                val calculatedTrust = (50 + (returnedCount * 15) + (foundCount * 5)).coerceIn(50, 99)

                countItemsFound.text = foundCount.toString()
                countItemsReturned.text = returnedCount.toString()
                countTrustScore.text = "$calculatedTrust%"
            }

            override fun onCancelled(error: DatabaseError) {}
        }
        itemStatsListener?.let { FirebaseManager.itemsRef.addValueEventListener(it) }
    }

    private fun uploadProfileImage(uri: android.net.Uri) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                ModernToast.show(requireContext(), "Uploading profile photo...", view)
                val compressedBytes = com.group3.lostandfound.repository.ReportRepository().compressImageUri(
                    requireContext(),
                    uri,
                    maxWidth = 500,
                    maxHeight = 500,
                    quality = 80
                )
                var uploadedUrl = ""
                try {
                    uploadedUrl = com.group3.lostandfound.util.CloudinaryManager.uploadImage(compressedBytes) {}
                } catch (_: Exception) {}

                
                if (!uploadedUrl.startsWith("http://") && !uploadedUrl.startsWith("https://")) {
                    val b64 = android.util.Base64.encodeToString(compressedBytes, android.util.Base64.NO_WRAP)
                    uploadedUrl = "data:image/jpeg;base64,$b64"
                }

                val updates = mapOf<String, Any>(
                    "photoUrl" to uploadedUrl,
                    "avatarUrl" to uploadedUrl
                )
                userRef.updateChildren(updates)
                currentPhotoUrl = uploadedUrl
                ImageLoader.load(userAvatar, uploadedUrl, R.drawable.ic_user_avatar, viewLifecycleOwner.lifecycleScope)
                activeDialogAvatarPreview?.let {
                    ImageLoader.load(it, uploadedUrl, R.drawable.ic_user_avatar, viewLifecycleOwner.lifecycleScope)
                }
                ModernToast.show(requireContext(), "Profile photo updated successfully!", view)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Photo upload error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

     private fun showEditProfileDialog() {
        val bottomSheet = BottomSheetDialog(requireContext())
        val sheetView = layoutInflater.inflate(R.layout.dialog_edit_profile, null)
        bottomSheet.setContentView(sheetView)

        val dialogAvatarPreview = sheetView.findViewById<ImageView>(R.id.dialogAvatarPreview)
        val rowChangeAvatar = sheetView.findViewById<View>(R.id.rowChangeAvatar)
        val etProfileName = sheetView.findViewById<EditText>(R.id.etProfileName)
        val etProfilePhone = sheetView.findViewById<EditText>(R.id.etProfilePhone)
        val btnCancelEdit = sheetView.findViewById<View>(R.id.btnCancelEdit)
        val btnSaveProfile = sheetView.findViewById<View>(R.id.btnSaveProfile)

        activeDialogAvatarPreview = dialogAvatarPreview
        if (currentPhotoUrl.isNotEmpty()) {
            ImageLoader.load(dialogAvatarPreview, currentPhotoUrl, R.drawable.ic_user_avatar, viewLifecycleOwner.lifecycleScope)
        }

        etProfileName.setText(currentDisplayName)
        etProfilePhone.setText(currentPhone)

        rowChangeAvatar.setOnClickListener {
            pickProfileAvatarLauncher.launch("image/*")
        }

        btnCancelEdit.setOnClickListener {
            bottomSheet.dismiss()
        }

        btnSaveProfile.setOnClickListener {
            val newName = etProfileName.text.toString().trim().ifEmpty { currentDisplayName }
            val newPhone = etProfilePhone.text.toString().trim()

            val updates = mutableMapOf<String, Any>(
                "displayName" to newName,
                "fullName" to newName
            )
            if (newPhone.isNotEmpty()) {
                updates["phone"] = newPhone
                updates["phoneNumber"] = newPhone
            }

            userRef.updateChildren(updates).addOnSuccessListener {
                currentDisplayName = newName
                currentPhone = newPhone
                userName.text = newName
                ModernToast.show(requireContext(), "Profile updated in Firebase!", view)
                bottomSheet.dismiss()
            }.addOnFailureListener {
                Toast.makeText(requireContext(), "Update failed: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }

