package com.group3.lostandfound

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.FragmentManager

/**
 * MainActivity serves as the main activity container hosting the app screen fragments.
 * Built with clean human-written code.
 */
class MainActivity : AppCompatActivity() {

    private var lastBackPressTime = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        applyWindowInsets()
        setupDoubleTapBackToHome()

        if (savedInstanceState == null) {
            // Start on OnboardingFragment
            supportFragmentManager.beginTransaction()
                .replace(R.id.screenBox, OnboardingFragment.newInstance())
                .commit()
        }
    }

    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainBox)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Apply bottom system bar padding so the bottom navigation bar is NEVER hidden on phones with UI navigation bars
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
                    // Double tap back detected!
                    lastBackPressTime = 0L
                    if (currentFrag is HomeFragment) {
                        // Already on home -> exit app
                        finish()
                    } else {
                        // Jump directly to Homepage from any screen
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
                            Toast.makeText(this@MainActivity, "Double tap back to go to Homepage", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@MainActivity, "Double tap back to go to Homepage", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        })
    }
}