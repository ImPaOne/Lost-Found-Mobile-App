package com.group3.lostandfound

import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

/**
 * NavigationHelper provides simple, reusable bottom navigation across all screens.
 * Uses clear, human-meaningful variable names and simple code.
 */
object NavigationHelper {

    enum class Tab {
        HOME, EXPLORE, POST, MESSAGES, PROFILE
    }

    fun setup(fragment: Fragment, rootView: View, currentTab: Tab) {
        val tabHome = rootView.findViewById<LinearLayout>(R.id.tabHome) ?: return
        val tabExplore = rootView.findViewById<LinearLayout>(R.id.tabExplore) ?: return
        val tabPost = rootView.findViewById<LinearLayout>(R.id.tabPost) ?: return
        val tabMessages = rootView.findViewById<LinearLayout>(R.id.tabMessages) ?: return
        val tabProfile = rootView.findViewById<LinearLayout>(R.id.tabProfile) ?: return

        val labelHome = rootView.findViewById<TextView>(R.id.lblHome)
        val labelExplore = rootView.findViewById<TextView>(R.id.lblExplore)
        val labelMessages = rootView.findViewById<TextView>(R.id.lblMessages)
        val labelProfile = rootView.findViewById<TextView>(R.id.lblProfile)

        val activeColor = ContextCompat.getColor(fragment.requireContext(), R.color.text_primary)
        val inactiveColor = ContextCompat.getColor(fragment.requireContext(), R.color.see_all_color)

        labelHome?.setTextColor(if (currentTab == Tab.HOME) activeColor else inactiveColor)
        labelExplore?.setTextColor(if (currentTab == Tab.EXPLORE) activeColor else inactiveColor)
        labelMessages?.setTextColor(if (currentTab == Tab.MESSAGES) activeColor else inactiveColor)
        labelProfile?.setTextColor(if (currentTab == Tab.PROFILE) activeColor else inactiveColor)

        tabHome.setOnClickListener {
            if (currentTab != Tab.HOME) {
                switchScreen(fragment, HomeFragment.newInstance())
            }
        }

        tabExplore.setOnClickListener {
            if (currentTab != Tab.EXPLORE) {
                switchScreen(fragment, AdminFragment.newInstance())
            }
        }

        tabPost.setOnClickListener {
            Toast.makeText(fragment.requireContext(), "+ Post", Toast.LENGTH_SHORT).show()
        }

        tabMessages.setOnClickListener {
            if (currentTab != Tab.MESSAGES) {
                switchScreen(fragment, AnalyticsFragment.newInstance())
            }
        }

        tabProfile.setOnClickListener {
            if (currentTab != Tab.PROFILE) {
                switchScreen(fragment, ReportsFragment.newInstance())
            }
        }
    }

    private fun switchScreen(fragment: Fragment, destinationFragment: Fragment) {
        fragment.parentFragmentManager.beginTransaction()
            .replace(R.id.screenBox, destinationFragment)
            .addToBackStack(null)
            .commit()
    }
}
