package com.group3.lostandfound

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.FragmentManager
import com.group3.lostandfound.service.FCMService

/**
 * MainActivity serves as the main activity container hosting the app screen fragments.
 * Handles FCM token sync and push notification routing to DetailsFragment.
 */
class MainActivity : AppCompatActivity() {

    private var lastBackPressTime = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        applyWindowInsets()
        setupDoubleTapBackToHome()

        // Initialize notification channel with sound & vibration
        FCMService.createNotificationChannel(this)

        // Request runtime permission for notifications on Android 13+ (API 33+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                androidx.core.app.ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    101
                )
            }
        }

        // Sync FCM device token to /users/{uid}/fcmToken
        FCMService.syncUserToken(this)

        // Seed default live Firebase items and admin credentials if empty
        FirebaseManager.seedDefaultDataIfEmpty()

        // Set up real-time incoming message notification listener
        setupIncomingMessageNotifications()

        if (savedInstanceState == null) {
            val prefs = getSharedPreferences("findit_prefs", MODE_PRIVATE)
            val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            val localUid = prefs.getString("user_uid", "") ?: ""
            val savedEmail = prefs.getString("saved_email", "") ?: ""
            val isLoggedIn = prefs.getBoolean("is_logged_in", false) && (firebaseUser != null || localUid.isNotEmpty())
            if (localUid.isNotEmpty()) {
                FirebaseManager.customUid = localUid
                FirebaseManager.customEmail = savedEmail
            } else if (firebaseUser == null && prefs.getBoolean("is_logged_in", false)) {
                prefs.edit().putBoolean("is_logged_in", false).apply()
            }
            val onboardingCompleted = prefs.getBoolean("onboarding_completed", false)
            val isExplicitAdmin = savedEmail.equals("admin@gmail.com", ignoreCase = true) && prefs.getBoolean("is_admin", false)

            if (!isExplicitAdmin && prefs.getBoolean("is_admin", false)) {
                prefs.edit().putBoolean("is_admin", false).apply()
            }

            val initialFragment = when {
                isLoggedIn && isExplicitAdmin -> AdminFragment.newInstance()
                isLoggedIn -> HomeFragment.newInstance()
                !onboardingCompleted -> OnboardingFragment.newInstance()
                else -> LoginFragment.newInstance()
            }

            supportFragmentManager.beginTransaction()
                .replace(R.id.screenBox, initialFragment)
                .commit()
        }
    }

    private fun setupIncomingMessageNotifications() {
        val uid = FirebaseManager.currentUid
        if (uid.isEmpty()) return

        val appStartTime = System.currentTimeMillis()
        val convRef = com.google.firebase.database.FirebaseDatabase.getInstance(FirebaseManager.DATABASE_URL)
            .getReference("user_conversations")
            .child(uid)

        convRef.addChildEventListener(object : com.google.firebase.database.ChildEventListener {
            override fun onChildAdded(snapshot: com.google.firebase.database.DataSnapshot, previousChildName: String?) {
                handleConvUpdate(snapshot)
            }

            override fun onChildChanged(snapshot: com.google.firebase.database.DataSnapshot, previousChildName: String?) {
                handleConvUpdate(snapshot)
            }

            private fun handleConvUpdate(snapshot: com.google.firebase.database.DataSnapshot) {
                val metadata = snapshot.getValue(ConversationMetadata::class.java) ?: return
                if (metadata.lastUpdatedTimestamp > appStartTime && metadata.otherUserId != FirebaseManager.currentUid) {
                    val prefs = getSharedPreferences("findit_prefs", MODE_PRIVATE)
                    val notifEnabled = prefs.getBoolean("push_notifications_enabled", true)
                    if (notifEnabled) {
                        val senderTitle = if (metadata.otherUserName.isNotEmpty()) metadata.otherUserName else "Foundly Community"
                        FCMService.triggerPushNotification(
                            this@MainActivity,
                            "💬 Message from $senderTitle",
                            metadata.lastMessageTextContent.ifEmpty { "New message regarding item." },
                            metadata.relatedItemId
                        )
                    }
                }
            }

            override fun onChildRemoved(snapshot: com.google.firebase.database.DataSnapshot) {}
            override fun onChildMoved(snapshot: com.google.firebase.database.DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
        })
    }

    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainBox)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupDoubleTapBackToHome() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val currentTime = System.currentTimeMillis()
                val currentFrag = supportFragmentManager.findFragmentById(R.id.screenBox)

                if (currentTime - lastBackPressTime < 2000L) {
                    lastBackPressTime = 0L
                    if (currentFrag is HomeFragment) {
                        finish()
                    } else {
                        supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
                        supportFragmentManager.beginTransaction()
                            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                            .replace(R.id.screenBox, HomeFragment.newInstance())
                            .commit()
                        Toast.makeText(this@MainActivity, "Returned to Homepage", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    lastBackPressTime = currentTime
                    if (currentFrag is HomeFragment) {
                        Toast.makeText(this@MainActivity, "Tap back again to exit", Toast.LENGTH_SHORT).show()
                    } else {
                        if (supportFragmentManager.backStackEntryCount > 0) {
                            supportFragmentManager.popBackStack()
                        } else {
                            Toast.makeText(this@MainActivity, "Double tap back to go to Homepage", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        })
    }
}