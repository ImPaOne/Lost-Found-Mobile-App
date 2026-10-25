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

    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(com.group3.lostandfound.util.LocaleHelper.applyLocale(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val themePrefs = getSharedPreferences("findit_prefs", MODE_PRIVATE)
        when (themePrefs.getString("selected_theme_label", "System Default")) {
            "Dark Mode" -> androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES)
            "Light Mode" -> androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO)
            else -> androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        applyWindowInsets()
        setupDoubleTapBackToHome()

        // Initialize notification channel with Apple-like sound & vibration
        FCMService.createNotificationChannel(this)

        // Sync FCM device token to /users/{uid}/fcmToken
        FCMService.syncUserToken(this)

        // Seed default live Firebase items and admin credentials if empty
        FirebaseManager.seedDefaultDataIfEmpty()

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

        // Set up real-time incoming message notification listener AFTER user credentials are restored
        setupIncomingMessageNotifications()

        if (savedInstanceState == null) {
            val onboardingCompleted = prefs.getBoolean("onboarding_completed", false)
            val isExplicitAdmin = savedEmail.equals("admin@gmail.com", ignoreCase = true) && prefs.getBoolean("is_admin", false)

            if (!isExplicitAdmin && prefs.getBoolean("is_admin", false)) {
                prefs.edit().putBoolean("is_admin", false).apply()
            }

            val initialFragment = when {
                !onboardingCompleted -> OnboardingFragment.newInstance()
                isLoggedIn && isExplicitAdmin -> AdminFragment.newInstance()
                isLoggedIn -> HomeFragment.newInstance()
                else -> LoginFragment.newInstance()
            }

            supportFragmentManager.beginTransaction()
                .replace(R.id.screenBox, initialFragment)
                .commit()
        }

        handleDeepLinkOrNotification(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLinkOrNotification(intent)
    }

    private fun handleDeepLinkOrNotification(intent: Intent?) {
        val uri = intent?.data
        var targetItemId = intent?.getStringExtra("item_id") ?: intent?.getStringExtra("itemId") ?: ""
        if (uri != null) {
            val paramId = uri.getQueryParameter("id") ?: uri.getQueryParameter("itemId")
            if (!paramId.isNullOrEmpty()) {
                targetItemId = paramId
            } else {
                val segments = uri.pathSegments
                if (segments.isNotEmpty()) {
                    val last = segments.last()
                    if (last != "item") {
                        targetItemId = last
                    }
                } else if (!uri.host.isNullOrEmpty() && uri.host != "item" && uri.host != "foundly.lk" && uri.host != "foundly.app") {
                    targetItemId = uri.host.orEmpty()
                }
            }
        }
        if (targetItemId.isNotEmpty()) {
            supportFragmentManager.beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.screenBox, DetailsFragment.newInstance(itemId = targetItemId))
                .addToBackStack(null)
                .commit()
        }
    }

    private var isMessageListenerAttached = false

    fun setupIncomingMessageNotifications() {
        val uid = FirebaseManager.currentUid
        try {
            startService(android.content.Intent(this, com.group3.lostandfound.service.AppNotificationService::class.java))
        } catch (e: Exception) {}

        if (uid.isEmpty() || isMessageListenerAttached) return
        isMessageListenerAttached = true

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
                val isSenderSelf = if (metadata.lastSenderId.isNotEmpty()) {
                    metadata.lastSenderId == FirebaseManager.currentUid
                } else {
                    false
                }
                if (!isSenderSelf && metadata.lastUpdatedTimestamp > appStartTime && metadata.otherUserId.isNotEmpty()) {
                    val prefs = getSharedPreferences("findit_prefs", MODE_PRIVATE)
                    val notifEnabled = prefs.getBoolean("push_notifications_enabled", true)
                    if (notifEnabled) {
                        val senderTitle = if (metadata.otherUserName.isNotEmpty()) metadata.otherUserName else "Foundly Community"
                        val notifTitle = if (metadata.relatedItemTitle.isNotBlank()) {
                            "$senderTitle: '${metadata.relatedItemTitle}'"
                        } else {
                            "Message from $senderTitle"
                        }
                        FCMService.triggerPushNotification(
                            this@MainActivity,
                            notifTitle,
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

        // Also listen to /notifications/{uid} for other incoming alerts (reward offers, status changes)
        val notifRef = com.google.firebase.database.FirebaseDatabase.getInstance(FirebaseManager.DATABASE_URL)
            .getReference("notifications")
            .child(uid)

        notifRef.addChildEventListener(object : com.google.firebase.database.ChildEventListener {
            override fun onChildAdded(snapshot: com.google.firebase.database.DataSnapshot, previousChildName: String?) {
                val notif = try {
                    snapshot.getValue(NotificationItem::class.java)
                } catch (e: Exception) {
                    null
                } ?: return

                if (notif.timestamp > appStartTime && notif.senderUserId != FirebaseManager.currentUid && notif.type != "CHAT_MESSAGE") {
                    val prefs = getSharedPreferences("findit_prefs", MODE_PRIVATE)
                    val notifEnabled = prefs.getBoolean("push_notifications_enabled", true)
                    if (notifEnabled) {
                        FCMService.triggerPushNotification(
                            this@MainActivity,
                            notif.title,
                            notif.message,
                            notif.itemId
                        )
                    }
                }
            }

            override fun onChildChanged(snapshot: com.google.firebase.database.DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: com.google.firebase.database.DataSnapshot) {}
            override fun onChildMoved(snapshot: com.google.firebase.database.DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
        })
    }

    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainBox)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            val bottomPadding = if (ime.bottom > 0) ime.bottom else systemBars.bottom
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, bottomPadding)
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