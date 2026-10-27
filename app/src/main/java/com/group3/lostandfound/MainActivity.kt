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

        // Request runtime notification permission on Android 13+ (API 33+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                androidx.core.app.ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1001)
            }
        }

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
            val isAdmin = prefs.getBoolean("is_admin", false) ||
                savedEmail.equals("admin@gmail.com", ignoreCase = true) ||
                FirebaseManager.currentUserEmail.equals("admin@gmail.com", ignoreCase = true) ||
                (firebaseUser?.email ?: "").equals("admin@gmail.com", ignoreCase = true)

            val initialFragment = when {
                !onboardingCompleted -> OnboardingFragment.newInstance()
                isLoggedIn && isAdmin -> AdminFragment.newInstance()
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
    private var isAnnouncementListenerAttached = false
    private val notifiedAlertIds = mutableSetOf<String>()

    fun setupIncomingMessageNotifications() {
        val uid = FirebaseManager.currentUid
        try {
            val serviceIntent = Intent(this, com.group3.lostandfound.service.AppNotificationService::class.java).apply {
                putExtra("user_uid", uid)
            }
            startService(serviceIntent)
        } catch (_: Exception) {}

        val appStartTime = System.currentTimeMillis()
        val database = com.google.firebase.database.FirebaseDatabase.getInstance(FirebaseManager.DATABASE_URL)

        // 1. Global Admin Announcements Listener (all users listen to this)
        if (!isAnnouncementListenerAttached) {
            isAnnouncementListenerAttached = true
            val announcementsRef = database.getReference("announcements")
            announcementsRef.addChildEventListener(object : com.google.firebase.database.ChildEventListener {
                override fun onChildAdded(snapshot: com.google.firebase.database.DataSnapshot, previousChildName: String?) {
                    try {
                        val timestamp = snapshot.child("timestamp").getValue(Long::class.java) ?: 0L
                        val annKey = snapshot.key ?: "ann_$timestamp"
                        if (!notifiedAlertIds.contains(annKey) && timestamp > appStartTime - 15000) {
                            notifiedAlertIds.add(annKey)
                            val title = snapshot.child("title").getValue(String::class.java) ?: "Foundly Official Announcement"
                            val message = snapshot.child("message").getValue(String::class.java) ?: ""
                            val prefs = getSharedPreferences("findit_prefs", MODE_PRIVATE)
                            val notifEnabled = prefs.getBoolean("push_notifications_enabled", true)
                            if (notifEnabled && message.isNotEmpty()) {
                                FCMService.triggerPushNotification(
                                    this@MainActivity,
                                    "📢 $title",
                                    message
                                )
                            }
                        }
                    } catch (_: Exception) {}
                }

                override fun onChildChanged(snapshot: com.google.firebase.database.DataSnapshot, previousChildName: String?) {}
                override fun onChildRemoved(snapshot: com.google.firebase.database.DataSnapshot) {}
                override fun onChildMoved(snapshot: com.google.firebase.database.DataSnapshot, previousChildName: String?) {}
                override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
            })
        }

        if (uid.isEmpty() || isMessageListenerAttached) return
        isMessageListenerAttached = true

        // 2. Real-time Messages from user_conversations
        val convRef = database.getReference("user_conversations").child(uid)
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
                val convKey = "conv_${metadata.chatConversationId}_${metadata.lastUpdatedTimestamp}"
                if (!isSenderSelf && !notifiedAlertIds.contains(convKey) && metadata.lastUpdatedTimestamp > appStartTime - 10000 && metadata.otherUserId.isNotEmpty()) {
                    notifiedAlertIds.add(convKey)
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

        // 3. User notifications under /notifications/{uid} (Chat messages, status updates, reward offers)
        val notifRef = database.getReference("notifications").child(uid)
        notifRef.addChildEventListener(object : com.google.firebase.database.ChildEventListener {
            override fun onChildAdded(snapshot: com.google.firebase.database.DataSnapshot, previousChildName: String?) {
                val notif = try {
                    snapshot.getValue(NotificationItem::class.java)
                } catch (e: Exception) {
                    null
                } ?: return

                val notifKey = notif.id.ifEmpty { "notif_${notif.timestamp}_${notif.title}" }
                if (!notifiedAlertIds.contains(notifKey) && notif.timestamp > appStartTime - 10000 && notif.senderUserId != FirebaseManager.currentUid) {
                    notifiedAlertIds.add(notifKey)
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
                val prefs = getSharedPreferences("findit_prefs", MODE_PRIVATE)
                val isAdmin = prefs.getBoolean("is_admin", false) || FirebaseManager.currentUserEmail.equals("admin@gmail.com", ignoreCase = true)
                val currentTime = System.currentTimeMillis()
                val currentFrag = supportFragmentManager.findFragmentById(R.id.screenBox)

                val isRootScreen = if (isAdmin) currentFrag is AdminFragment else currentFrag is HomeFragment

                if (currentTime - lastBackPressTime < 2000L) {
                    lastBackPressTime = 0L
                    if (isRootScreen) {
                        finish()
                    } else {
                        supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
                        val targetFrag = if (isAdmin) AdminFragment.newInstance() else HomeFragment.newInstance()
                        val targetLabel = if (isAdmin) "Admin Dashboard" else "Homepage"
                        supportFragmentManager.beginTransaction()
                            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                            .replace(R.id.screenBox, targetFrag)
                            .commit()
                        Toast.makeText(this@MainActivity, "Returned to $targetLabel", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    lastBackPressTime = currentTime
                    if (isRootScreen) {
                        Toast.makeText(this@MainActivity, "Tap back again to exit", Toast.LENGTH_SHORT).show()
                    } else {
                        if (supportFragmentManager.backStackEntryCount > 0) {
                            supportFragmentManager.popBackStack()
                        } else {
                            val targetLabel = if (isAdmin) "Admin Dashboard" else "Homepage"
                            Toast.makeText(this@MainActivity, "Double tap back to go to $targetLabel", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        })
    }
}