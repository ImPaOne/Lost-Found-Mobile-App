package com.group3.lostandfound

import android.view.View
import androidx.fragment.app.Fragment

/**
 * Clean navigation helper using human-meaningful variables.
 * Delegates to setupBottomNav.
 */
object NavigationHelper {

    enum class Tab {
        HOME, EXPLORE, POST, MESSAGES, PROFILE, ADMIN, NONE
    }

    fun setup(fragment: Fragment, rootView: View, currentTab: Tab) {
        val tabKey = when (currentTab) {
            Tab.HOME -> "home"
            Tab.EXPLORE -> "explore"
            Tab.POST -> "post"
            Tab.MESSAGES -> "messages"
            Tab.PROFILE -> "profile"
            Tab.ADMIN -> "admin"
            Tab.NONE -> ""
        }
        fragment.setupBottomNav(rootView, tabKey)
    }
}
