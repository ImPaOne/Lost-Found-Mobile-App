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

        if (savedInstanceState == null) {
            val prefs = getSharedPreferences("findit_prefs", MODE_PRIVATE)
            val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            val isLoggedIn = prefs.getBoolean("is_logged_in", false) && firebaseUser != null
            if (firebaseUser == null && prefs.getBoolean("is_logged_in", false)) {
                prefs.edit().putBoolean("is_logged_in", false).apply()
            }
            val onboardingCompleted = prefs.getBoolean("onboarding_completed", false)
            val isAdmin = prefs.getBoolean("is_admin", false)

            val initialFragment = when {
                isLoggedIn && isAdmin -> AdminFragment.newInstance()
                isLoggedIn -> HomeFragment.newInstance()
                !onboardingCompleted -> OnboardingFragment.newInstance()
                else -> LoginFragment.newInstance()
            }

            supportFragmentManager.beginTransaction()
                .replace(R.id.screenBox, initialFragment)
                .commit()
        }
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