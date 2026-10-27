package com.group3.lostandfound

import android.content.Context
import android.graphics.Typeface
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

/**
 * Consistent, functional bottom navigation helper across all app screens.
 * Renders 5 standard tabs matching design:
 * User Mode: Home, Explore, (+) Create Report, Messages, Profile.
 * Admin Mode: Dashboard, Moderation, Analytics, Users, Settings.
 */
fun Fragment.setupBottomNav(rootView: View, activeTab: String = "") {
    val tabHome = rootView.findViewById<View>(R.id.tabHome) ?: return
    val tabExplore = rootView.findViewById<View>(R.id.tabExplore)
    val tabPost = rootView.findViewById<View>(R.id.tabPost)
    val btnPostCircle = rootView.findViewById<View>(R.id.btnPostCircle)
    val btnPostPill = rootView.findViewById<View>(R.id.btnPostPill)
    val tabMessages = rootView.findViewById<View>(R.id.tabMessages)
    val tabProfile = rootView.findViewById<View>(R.id.tabProfile)

    val lblHome = rootView.findViewById<TextView>(R.id.lblHome)
    val lblExplore = rootView.findViewById<TextView>(R.id.lblExplore)
    val lblPost = rootView.findViewById<TextView>(R.id.lblPost)
    val lblMessages = rootView.findViewById<TextView>(R.id.lblMessages)
    val lblProfile = rootView.findViewById<TextView>(R.id.lblProfile)

    val imgHome = rootView.findViewById<ImageView>(R.id.imgHome)
    val imgExplore = rootView.findViewById<ImageView>(R.id.imgExplore)
    val imgPostPlus = rootView.findViewById<ImageView>(R.id.imgPostPlus)
    val imgMessages = rootView.findViewById<ImageView>(R.id.imgMessages)
    val imgProfile = rootView.findViewById<ImageView>(R.id.imgProfile)

    val boxHome = rootView.findViewById<View>(R.id.boxHome)
    val boxExplore = rootView.findViewById<View>(R.id.boxExplore)
    val boxMessages = rootView.findViewById<View>(R.id.boxMessages)
    val boxProfile = rootView.findViewById<View>(R.id.boxProfile)

    val prefs = requireContext().getSharedPreferences("findit_prefs", Context.MODE_PRIVATE)
    val isAdmin = prefs.getBoolean("is_admin", false) || FirebaseManager.currentUserEmail.equals("admin@gmail.com", ignoreCase = true)

    val activeColor = ContextCompat.getColor(requireContext(), R.color.text_primary)
    val inactiveColor = ContextCompat.getColor(requireContext(), R.color.text_hint)

    fun navigateTo(dest: Fragment) {
        if (!isAdded) return
        val fragmentManager = parentFragmentManager
        if (dest is HomeFragment || dest is AdminFragment) {
            fragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
        }
        fragmentManager.beginTransaction()
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
            .replace(R.id.screenBox, dest)
            .addToBackStack(if (dest is HomeFragment || dest is AdminFragment) null else "main_flow")
            .commit()
    }

    if (isAdmin) {
        // ADMIN SPECIFIC NAVIGATION: Dashboard, Moderation, Analytics, Users, Settings
        tabHome.visibility = View.VISIBLE
        tabExplore?.visibility = View.VISIBLE
        tabPost?.visibility = View.VISIBLE
        tabMessages?.visibility = View.VISIBLE
        tabProfile?.visibility = View.VISIBLE

        lblHome?.text = "Dashboard"
        lblExplore?.text = "Moderation"
        lblPost?.text = "Analytics"
        lblMessages?.text = "Users"
        lblProfile?.text = "Settings"

        imgHome?.setImageResource(R.drawable.ic_nav_home)
        imgExplore?.setImageResource(R.drawable.ic_shield_check)
        imgPostPlus?.setImageResource(R.drawable.ic_star_filled)
        imgMessages?.setImageResource(R.drawable.ic_person)
        imgProfile?.setImageResource(R.drawable.ic_gear_settings)

        val isTab1 = activeTab == "admin" || activeTab == "dashboard"
        val isTab2 = activeTab == "moderation"
        val isTab3 = activeTab == "analytics"
        val isTab4 = activeTab == "users"
        val isTab5 = activeTab == "settings"

        boxHome?.setBackgroundResource(if (isTab1) R.drawable.bg_nav_tab_active_pill else 0)
        boxExplore?.setBackgroundResource(if (isTab2) R.drawable.bg_nav_tab_active_pill else 0)
        boxMessages?.setBackgroundResource(if (isTab4) R.drawable.bg_nav_tab_active_pill else 0)
        boxProfile?.setBackgroundResource(if (isTab5) R.drawable.bg_nav_tab_active_pill else 0)

        lblHome?.setTextColor(if (isTab1) activeColor else inactiveColor)
        lblExplore?.setTextColor(if (isTab2) activeColor else inactiveColor)
        lblPost?.setTextColor(if (isTab3) activeColor else inactiveColor)
        lblMessages?.setTextColor(if (isTab4) activeColor else inactiveColor)
        lblProfile?.setTextColor(if (isTab5) activeColor else inactiveColor)

        lblHome?.setTypeface(null, if (isTab1) Typeface.BOLD else Typeface.NORMAL)
        lblExplore?.setTypeface(null, if (isTab2) Typeface.BOLD else Typeface.NORMAL)
        lblPost?.setTypeface(null, if (isTab3) Typeface.BOLD else Typeface.NORMAL)
        lblMessages?.setTypeface(null, if (isTab4) Typeface.BOLD else Typeface.NORMAL)
        lblProfile?.setTypeface(null, if (isTab5) Typeface.BOLD else Typeface.NORMAL)

        imgHome?.setColorFilter(if (isTab1) activeColor else inactiveColor)
        imgExplore?.setColorFilter(if (isTab2) activeColor else inactiveColor)
        imgMessages?.setColorFilter(if (isTab4) activeColor else inactiveColor)
        imgProfile?.setColorFilter(if (isTab5) activeColor else inactiveColor)

        tabHome.setOnClickListener {
            if (this !is AdminFragment) navigateTo(AdminFragment.newInstance())
        }
        tabExplore?.setOnClickListener {
            if (this !is ModerationFragment) navigateTo(ModerationFragment.newInstance())
        }
        val openAnalytics = View.OnClickListener {
            if (this !is AnalyticsFragment) navigateTo(AnalyticsFragment.newInstance())
        }
        tabPost?.setOnClickListener(openAnalytics)
        btnPostCircle?.setOnClickListener(openAnalytics)
        btnPostPill?.setOnClickListener(openAnalytics)

        tabMessages?.setOnClickListener {
            if (this !is UserManagementFragment) navigateTo(UserManagementFragment.newInstance())
        }
        tabProfile?.setOnClickListener {
            if (this !is SettingsFragment) navigateTo(SettingsFragment.newInstance())
        }
    } else {
        // STANDARD USER NAVIGATION: Home, Explore, (+) Center Post, Messages, Profile
        tabHome.visibility = View.VISIBLE
        tabExplore?.visibility = View.VISIBLE
        tabPost?.visibility = View.VISIBLE
        tabMessages?.visibility = View.VISIBLE
        tabProfile?.visibility = View.VISIBLE

        lblHome?.text = "Home"
        lblExplore?.text = "Explore"
        lblPost?.text = "+ Post"
        lblMessages?.text = "Messages"
        lblProfile?.text = "Profile"

        imgHome?.setImageResource(R.drawable.ic_nav_home)
        imgExplore?.setImageResource(R.drawable.ic_nav_explore)
        imgPostPlus?.setImageResource(R.drawable.ic_nav_plus)
        imgMessages?.setImageResource(R.drawable.ic_nav_messages)
        imgProfile?.setImageResource(R.drawable.ic_nav_profile)

        val isTabHome = activeTab == "home"
        val isTabExplore = activeTab == "explore"
        val isTabPost = activeTab == "post"
        val isTabMessages = activeTab == "messages"
        val isTabProfile = activeTab == "profile" || activeTab == "settings"

        boxHome?.setBackgroundResource(if (isTabHome) R.drawable.bg_nav_tab_active_pill else 0)
        boxExplore?.setBackgroundResource(if (isTabExplore) R.drawable.bg_nav_tab_active_pill else 0)
        boxMessages?.setBackgroundResource(if (isTabMessages) R.drawable.bg_nav_tab_active_pill else 0)
        boxProfile?.setBackgroundResource(if (isTabProfile) R.drawable.bg_nav_tab_active_pill else 0)

        lblHome?.setTextColor(if (isTabHome) activeColor else inactiveColor)
        lblExplore?.setTextColor(if (isTabExplore) activeColor else inactiveColor)
        lblPost?.setTextColor(if (isTabPost) activeColor else activeColor)
        lblMessages?.setTextColor(if (isTabMessages) activeColor else inactiveColor)
        lblProfile?.setTextColor(if (isTabProfile) activeColor else inactiveColor)

        lblHome?.setTypeface(null, if (isTabHome) Typeface.BOLD else Typeface.NORMAL)
        lblExplore?.setTypeface(null, if (isTabExplore) Typeface.BOLD else Typeface.NORMAL)
        lblMessages?.setTypeface(null, if (isTabMessages) Typeface.BOLD else Typeface.NORMAL)
        lblProfile?.setTypeface(null, if (isTabProfile) Typeface.BOLD else Typeface.NORMAL)

        imgHome?.setColorFilter(if (isTabHome) activeColor else inactiveColor)
        imgExplore?.setColorFilter(if (isTabExplore) activeColor else inactiveColor)
        imgMessages?.setColorFilter(if (isTabMessages) activeColor else inactiveColor)
        imgProfile?.setColorFilter(if (isTabProfile) activeColor else inactiveColor)

        tabHome.setOnClickListener {
            if (this !is HomeFragment) navigateTo(HomeFragment.newInstance())
        }
        tabExplore?.setOnClickListener {
            if (this !is ExploreFragment) navigateTo(ExploreFragment.newInstance())
        }
        val openCreateReport = View.OnClickListener {
            if (this !is CreateReportFragment) navigateTo(CreateReportFragment.newInstance())
        }
        tabPost?.setOnClickListener(openCreateReport)
        btnPostCircle?.setOnClickListener(openCreateReport)
        btnPostPill?.setOnClickListener(openCreateReport)

        tabMessages?.setOnClickListener {
            if (this !is MessagesFragment) navigateTo(MessagesFragment.newInstance())
        }
        tabProfile?.setOnClickListener {
            if (this !is ProfileFragment) navigateTo(ProfileFragment.newInstance())
        }
    }
}
