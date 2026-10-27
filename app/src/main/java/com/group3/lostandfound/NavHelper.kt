package com.group3.lostandfound

import android.content.Context
import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

/**
 * Clean helper for bottom navigation across all screens.
 * Dynamically adapts navigation tabs and routing based on user authorization:
 * - If Admin: Dashboard (Admin), Moderation Queue, Analytics & Heatmap, User Management, Settings.
 * - If User: Home, Explore, Post Report, Messages, Profile.
 * Guarantees Administrator NEVER sees normal user screens.
 */
fun Fragment.setupBottomNav(rootView: View, activeTab: String = "") {
    val tabHome = rootView.findViewById<View>(R.id.tabHome) ?: return
    val tabExplore = rootView.findViewById<View>(R.id.tabExplore)
    val tabPost = rootView.findViewById<View>(R.id.tabPost)
    val btnPostPill = rootView.findViewById<View>(R.id.btnPostPill)
    val tabMessages = rootView.findViewById<View>(R.id.tabMessages)
    val tabProfile = rootView.findViewById<View>(R.id.tabProfile)

    val lblHome = rootView.findViewById<TextView>(R.id.lblHome)
    val lblExplore = rootView.findViewById<TextView>(R.id.lblExplore)
    val lblMessages = rootView.findViewById<TextView>(R.id.lblMessages)
    val lblProfile = rootView.findViewById<TextView>(R.id.lblProfile)

    val imgHome = rootView.findViewById<ImageView>(R.id.imgHome)
    val imgExplore = rootView.findViewById<ImageView>(R.id.imgExplore)
    val imgMessages = rootView.findViewById<ImageView>(R.id.imgMessages)
    val imgProfile = rootView.findViewById<ImageView>(R.id.imgProfile)

    val boxHome = rootView.findViewById<View>(R.id.boxHome)
    val boxExplore = rootView.findViewById<View>(R.id.boxExplore)
    val boxMessages = rootView.findViewById<View>(R.id.boxMessages)
    val boxProfile = rootView.findViewById<View>(R.id.boxProfile)

    val prefs = requireContext().getSharedPreferences("findit_prefs", Context.MODE_PRIVATE)
    val isAdmin = prefs.getBoolean("is_admin", false) || FirebaseManager.currentUserEmail.equals("admin@gmail.com", ignoreCase = true)

    val activeColor = ContextCompat.getColor(requireContext(), R.color.text_primary)
    val inactiveColor = ContextCompat.getColor(requireContext(), R.color.text_secondary)

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
        // ADMIN SPECIFIC NAVIGATION
        lblHome?.text = "Dashboard"
        lblExplore?.text = "Moderation"
        lblMessages?.text = "Users"
        lblProfile?.text = "Settings"

        imgHome?.setImageResource(R.drawable.ic_nav_home)
        imgExplore?.setImageResource(R.drawable.ic_shield_check)
        imgMessages?.setImageResource(R.drawable.ic_person)
        imgProfile?.setImageResource(R.drawable.ic_gear_settings)

        // Center pill for Analytics
        (btnPostPill as? ViewGroup)?.let { vg ->
            for (i in 0 until vg.childCount) {
                val child = vg.getChildAt(i)
                if (child is TextView) child.text = "Analytics"
                if (child is ImageView) child.setImageResource(R.drawable.ic_star_filled)
            }
        }

        val isTab1 = activeTab == "admin" || activeTab == "dashboard"
        val isTab2 = activeTab == "moderation"
        val isTab3 = activeTab == "analytics"
        val isTab4 = activeTab == "users"
        val isTab5 = activeTab == "settings"

        boxHome?.setBackgroundResource(if (isTab1) R.drawable.bg_nav_tab_active_pill else 0)
        boxExplore?.setBackgroundResource(if (isTab2) R.drawable.bg_nav_tab_active_pill else 0)
        btnPostPill?.setBackgroundResource(if (isTab3) R.drawable.bg_btn_approve else R.drawable.bg_nav_post_pill)
        boxMessages?.setBackgroundResource(if (isTab4) R.drawable.bg_nav_tab_active_pill else 0)
        boxProfile?.setBackgroundResource(if (isTab5) R.drawable.bg_nav_tab_active_pill else 0)

        lblHome?.setTextColor(if (isTab1) activeColor else inactiveColor)
        lblExplore?.setTextColor(if (isTab2) activeColor else inactiveColor)
        lblMessages?.setTextColor(if (isTab4) activeColor else inactiveColor)
        lblProfile?.setTextColor(if (isTab5) activeColor else inactiveColor)

        lblHome?.setTypeface(null, if (isTab1) Typeface.BOLD else Typeface.NORMAL)
        lblExplore?.setTypeface(null, if (isTab2) Typeface.BOLD else Typeface.NORMAL)
        lblMessages?.setTypeface(null, if (isTab4) Typeface.BOLD else Typeface.NORMAL)
        lblProfile?.setTypeface(null, if (isTab5) Typeface.BOLD else Typeface.NORMAL)

        imgHome?.setColorFilter(if (isTab1) activeColor else inactiveColor)
        imgExplore?.setColorFilter(if (isTab2) activeColor else inactiveColor)
        imgMessages?.setColorFilter(if (isTab4) activeColor else inactiveColor)
        imgProfile?.setColorFilter(if (isTab5) activeColor else inactiveColor)

        tabHome.setOnClickListener {
            if (!isTab1) navigateTo(AdminFragment.newInstance())
        }
        tabExplore?.setOnClickListener {
            if (!isTab2) navigateTo(ModerationFragment.newInstance())
        }
        val openAnalytics = View.OnClickListener {
            if (!isTab3) navigateTo(AnalyticsFragment.newInstance())
        }
        tabPost?.setOnClickListener(openAnalytics)
        btnPostPill?.setOnClickListener(openAnalytics)

        tabMessages?.setOnClickListener {
            if (!isTab4) navigateTo(UserManagementFragment.newInstance())
        }
        tabProfile?.setOnClickListener {
            if (!isTab5) navigateTo(SettingsFragment.newInstance())
        }
    } else {
        // NORMAL USER NAVIGATION
        lblHome?.text = "Home"
        lblExplore?.text = "Explore"
        lblMessages?.text = "Messages"
        lblProfile?.text = "Profile"

        imgHome?.setImageResource(R.drawable.ic_nav_home)
        imgExplore?.setImageResource(R.drawable.ic_nav_explore)
        imgMessages?.setImageResource(R.drawable.ic_nav_messages)
        imgProfile?.setImageResource(R.drawable.ic_nav_profile)

        (btnPostPill as? ViewGroup)?.let { vg ->
            for (i in 0 until vg.childCount) {
                val child = vg.getChildAt(i)
                if (child is TextView) child.text = "+ Post"
                if (child is ImageView) child.setImageResource(R.drawable.ic_plus)
            }
        }

        val isTab1 = activeTab == "home"
        val isTab2 = activeTab == "explore"
        val isTab3 = activeTab == "post"
        val isTab4 = activeTab == "messages"
        val isTab5 = activeTab == "profile"

        boxHome?.setBackgroundResource(if (isTab1) R.drawable.bg_nav_tab_active_pill else 0)
        boxExplore?.setBackgroundResource(if (isTab2) R.drawable.bg_nav_tab_active_pill else 0)
        btnPostPill?.setBackgroundResource(R.drawable.bg_nav_post_pill)
        boxMessages?.setBackgroundResource(if (isTab4) R.drawable.bg_nav_tab_active_pill else 0)
        boxProfile?.setBackgroundResource(if (isTab5) R.drawable.bg_nav_tab_active_pill else 0)

        lblHome?.setTextColor(if (isTab1) activeColor else inactiveColor)
        lblExplore?.setTextColor(if (isTab2) activeColor else inactiveColor)
        lblMessages?.setTextColor(if (isTab4) activeColor else inactiveColor)
        lblProfile?.setTextColor(if (isTab5) activeColor else inactiveColor)

        lblHome?.setTypeface(null, if (isTab1) Typeface.BOLD else Typeface.NORMAL)
        lblExplore?.setTypeface(null, if (isTab2) Typeface.BOLD else Typeface.NORMAL)
        lblMessages?.setTypeface(null, if (isTab4) Typeface.BOLD else Typeface.NORMAL)
        lblProfile?.setTypeface(null, if (isTab5) Typeface.BOLD else Typeface.NORMAL)

        imgHome?.setColorFilter(if (isTab1) activeColor else inactiveColor)
        imgExplore?.setColorFilter(if (isTab2) activeColor else inactiveColor)
        imgMessages?.setColorFilter(if (isTab4) activeColor else inactiveColor)
        imgProfile?.setColorFilter(if (isTab5) activeColor else inactiveColor)

        tabHome.setOnClickListener {
            if (!isTab1) navigateTo(HomeFragment.newInstance())
        }
        tabExplore?.setOnClickListener {
            if (!isTab2) navigateTo(ExploreFragment.newInstance())
        }
        val openPostAction = View.OnClickListener {
            if (!isTab3) navigateTo(CreateReportFragment.newInstance())
        }
        tabPost?.setOnClickListener(openPostAction)
        btnPostPill?.setOnClickListener(openPostAction)

        tabMessages?.setOnClickListener {
            if (!isTab4) navigateTo(MessagesFragment.newInstance())
        }
        tabProfile?.setOnClickListener {
            if (!isTab5) navigateTo(ProfileFragment.newInstance())
        }
    }
}
