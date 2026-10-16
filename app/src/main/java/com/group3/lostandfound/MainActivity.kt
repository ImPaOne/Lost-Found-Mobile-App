package com.group3.lostandfound

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {

    private lateinit var navHomeTab: LinearLayout
    private lateinit var navExploreTab: LinearLayout
    private lateinit var navPostTab: LinearLayout
    private lateinit var navMessagesTab: LinearLayout
    private lateinit var navProfileTab: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        applyWindowInsets()
        initBottomNavigation()

        // Load the HomeFragment as the default screen
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainerView, HomeFragment.newInstance())
                .commit()
        }
    }

    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainRootLayout)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }
    }

    private fun initBottomNavigation() {
        navHomeTab = findViewById(R.id.navHomeTab)
        navExploreTab = findViewById(R.id.navExploreTab)
        navPostTab = findViewById(R.id.navPostTab)
        navMessagesTab = findViewById(R.id.navMessagesTab)
        navProfileTab = findViewById(R.id.navProfileTab)

        navHomeTab.setOnClickListener {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainerView, HomeFragment.newInstance())
                .commit()
        }

        navExploreTab.setOnClickListener {
            Toast.makeText(this, "Explore", Toast.LENGTH_SHORT).show()
        }

        navPostTab.setOnClickListener {
            Toast.makeText(this, "+ Post New Item", Toast.LENGTH_SHORT).show()
        }

        navMessagesTab.setOnClickListener {
            Toast.makeText(this, "Messages", Toast.LENGTH_SHORT).show()
        }

        navProfileTab.setOnClickListener {
            Toast.makeText(this, "Profile", Toast.LENGTH_SHORT).show()
        }
    }
}