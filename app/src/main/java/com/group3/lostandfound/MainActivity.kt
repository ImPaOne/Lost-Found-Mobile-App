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
            // Load persistent bottom navigation bar once and start on AdminFragment / HomeFragment
            supportFragmentManager.beginTransaction()
                .replace(R.id.navBox, BottomNavFragment.newInstance())
                .replace(R.id.screenBox, AdminFragment.newInstance())
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

    override fun onTabSelected(tab: BottomNavFragment.NavigationTab) {
        when (tab) {
            BottomNavFragment.NavigationTab.HOME -> {
                loadScreen(HomeFragment.newInstance())
            }
            BottomNavFragment.NavigationTab.EXPLORE -> {
                loadScreen(AdminFragment.newInstance())
            }
            BottomNavFragment.NavigationTab.POST -> {
                Toast.makeText(this, "+ Post", Toast.LENGTH_SHORT).show()
            }
            BottomNavFragment.NavigationTab.MESSAGES -> {
                loadScreen(AnalyticsFragment.newInstance())
            }
            BottomNavFragment.NavigationTab.PROFILE -> {
                loadScreen(ReportsFragment.newInstance())
            }
        }
    }

    private fun loadScreen(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.screenBox, fragment)
            .addToBackStack(null)
            .commit()
    }
}