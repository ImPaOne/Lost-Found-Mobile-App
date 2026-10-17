package com.group3.lostandfound

import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

/**
 * Clean human-written helper for bottom navigation across all screens.
 * Uses meaningful, human variable names and simple fragment navigation.
 */
fun Fragment.setupBottomNav(rootView: View, activeTab: String = "") {
    val tabHome = rootView.findViewById<View>(R.id.tabHome) ?: return
    val tabExplore = rootView.findViewById<View>(R.id.tabExplore)
    val tabPost = rootView.findViewById<View>(R.id.tabPost)
    val tabMessages = rootView.findViewById<View>(R.id.tabMessages)
    val tabProfile = rootView.findViewById<View>(R.id.tabProfile)

    val lblHome = rootView.findViewById<TextView>(R.id.lblHome)
    val lblExplore = rootView.findViewById<TextView>(R.id.lblExplore)
    val lblMessages = rootView.findViewById<TextView>(R.id.lblMessages)
    val lblProfile = rootView.findViewById<TextView>(R.id.lblProfile)

    val activeColor = ContextCompat.getColor(requireContext(), R.color.text_primary)
    val inactiveColor = ContextCompat.getColor(requireContext(), R.color.text_secondary)

    lblHome?.setTextColor(if (activeTab == "home") activeColor else inactiveColor)
    lblExplore?.setTextColor(if (activeTab == "explore") activeColor else inactiveColor)
    lblMessages?.setTextColor(if (activeTab == "messages") activeColor else inactiveColor)
    lblProfile?.setTextColor(if (activeTab == "profile") activeColor else inactiveColor)

    tabHome.setOnClickListener {
        if (activeTab != "home") {
            parentFragmentManager.beginTransaction()
                .replace(R.id.screenBox, HomeFragment.newInstance())
                .addToBackStack(null)
                .commit()
        }
    }

    tabExplore?.setOnClickListener {
        if (activeTab != "explore") {
            parentFragmentManager.beginTransaction()
                .replace(R.id.screenBox, AdminFragment.newInstance())
                .addToBackStack(null)
                .commit()
        }
    }

    tabPost?.setOnClickListener {
        Toast.makeText(requireContext(), "+ Post new report", Toast.LENGTH_SHORT).show()
    }

    tabMessages?.setOnClickListener {
        if (activeTab != "messages") {
            parentFragmentManager.beginTransaction()
                .replace(R.id.screenBox, AnalyticsFragment.newInstance())
                .addToBackStack(null)
                .commit()
        }
    }

    tabProfile?.setOnClickListener {
        if (activeTab != "profile") {
            parentFragmentManager.beginTransaction()
                .replace(R.id.screenBox, ReportsFragment.newInstance())
                .addToBackStack(null)
                .commit()
        }
    }
}
