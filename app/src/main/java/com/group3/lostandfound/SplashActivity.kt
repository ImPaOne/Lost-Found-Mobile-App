package com.group3.lostandfound

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * SplashActivity displays the FindIt LK branding screen on application launch.
 * Transitions smoothly to MainActivity after a brief delay or when tapped.
 */
class SplashActivity : AppCompatActivity() {

    private val splashDelayMs = 2000L
    private val handler = Handler(Looper.getMainLooper())
    private var isNavigated = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash)

        val splashRootView = findViewById<View>(R.id.splashRoot)
        val centerBrandingBox = findViewById<View>(R.id.centerBrandingBox)

        ViewCompat.setOnApplyWindowInsetsListener(splashRootView) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Smooth branding entry animation
        centerBrandingBox?.apply {
            alpha = 0f
            scaleX = 0.88f
            scaleY = 0.88f
            animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(900)
                .start()
        }

        // Tap anywhere on the splash screen to skip wait
        splashRootView.setOnClickListener {
            openMainActivity()
        }

        // Automatically navigate after the splash delay
        handler.postDelayed({
            openMainActivity()
        }, splashDelayMs)
    }

    private fun openMainActivity() {
        if (isNavigated) return
        isNavigated = true
        handler.removeCallbacksAndMessages(null)

        val openMainIntent = Intent(this, MainActivity::class.java)
        startActivity(openMainIntent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }
}
