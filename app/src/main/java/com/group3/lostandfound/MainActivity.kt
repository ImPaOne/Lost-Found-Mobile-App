package com.group3.lostandfound

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment

class MainActivity : AppCompatActivity(), BottomNavFragment.OnNavigationTabSelectedListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        applyWindowInsets()

        if (savedInstanceState == null) {
            // Load reusable bottom navigation fragment and the initial screen fragment
            supportFragmentManager.beginTransaction()
                .replace(R.id.bottomNavFragmentContainer, BottomNavFragment.newInstance())
                .replace(R.id.screenFragmentContainer, HomeFragment.newInstance())
                .commit()
        }
    }

    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainRootLayout)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    override fun onTabSelected(tab: BottomNavFragment.NavigationTab) {
        when (tab) {
            BottomNavFragment.NavigationTab.HOME -> {
                loadScreenFragment(HomeFragment.newInstance())
            }
            BottomNavFragment.NavigationTab.EXPLORE -> {
                // Explore tab navigates to Moderation dashboard
                loadScreenFragment(ModerationFragment.newInstance())
            }
            BottomNavFragment.NavigationTab.POST -> {
                Toast.makeText(this, "+ Post action triggered", Toast.LENGTH_SHORT).show()
            }
            BottomNavFragment.NavigationTab.MESSAGES -> {
                Toast.makeText(this, "Messages screen selected", Toast.LENGTH_SHORT).show()
            }
            BottomNavFragment.NavigationTab.PROFILE -> {
                Toast.makeText(this, "Profile screen selected", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadScreenFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.screenFragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }
}