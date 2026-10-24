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