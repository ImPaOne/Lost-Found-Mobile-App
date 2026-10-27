package com.group3.lostandfound

import android.view.View
import android.widget.ImageView
import android.widget.TextView
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

    val activeColor = ContextCompat.getColor(requireContext(), R.color.text_primary)
    val inactiveColor = ContextCompat.getColor(requireContext(), R.color.text_secondary)

    boxHome?.setBackgroundResource(if (activeTab == "home") R.drawable.bg_nav_tab_active_pill else 0)
    boxExplore?.setBackgroundResource(if (activeTab == "explore") R.drawable.bg_nav_tab_active_pill else 0)
    boxMessages?.setBackgroundResource(if (activeTab == "messages") R.drawable.bg_nav_tab_active_pill else 0)
    boxProfile?.setBackgroundResource(if (activeTab == "profile") R.drawable.bg_nav_tab_active_pill else 0)

    lblHome?.setTextColor(if (activeTab == "home") activeColor else inactiveColor)
    lblExplore?.setTextColor(if (activeTab == "explore") activeColor else inactiveColor)
    lblMessages?.setTextColor(if (activeTab == "messages") activeColor else inactiveColor)
    lblProfile?.setTextColor(if (activeTab == "profile") activeColor else inactiveColor)

    lblHome?.setTypeface(null, if (activeTab == "home") android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
    lblExplore?.setTypeface(null, if (activeTab == "explore") android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
    lblMessages?.setTypeface(null, if (activeTab == "messages") android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
    lblProfile?.setTypeface(null, if (activeTab == "profile") android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)

    imgHome?.setColorFilter(if (activeTab == "home") activeColor else inactiveColor)
    imgExplore?.setColorFilter(if (activeTab == "explore") activeColor else inactiveColor)
    imgMessages?.setColorFilter(if (activeTab == "messages") activeColor else inactiveColor)
    imgProfile?.setColorFilter(if (activeTab == "profile") activeColor else inactiveColor)

    fun navigateTo(dest: Fragment) {
        if (!isAdded) return
        val fragmentManager = parentFragmentManager
        if (dest is HomeFragment) {
            fragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
        }
        fragmentManager.beginTransaction()
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
            .replace(R.id.screenBox, dest)
            .addToBackStack(if (dest is HomeFragment) null else "main_flow")
            .commit()
    }

    tabHome.setOnClickListener {
        if (activeTab != "home") {
            navigateTo(HomeFragment.newInstance())
        }
    }

    tabExplore?.setOnClickListener {
        if (activeTab != "explore") {
            navigateTo(ExploreFragment.newInstance())
        }
    }

    val openPostAction = View.OnClickListener {
        if (activeTab != "post") {
            navigateTo(CreateReportFragment.newInstance())
        }
    }
    tabPost?.setOnClickListener(openPostAction)
    btnPostPill?.setOnClickListener(openPostAction)

    tabMessages?.setOnClickListener {
        if (activeTab != "messages") {
            navigateTo(MessagesFragment.newInstance())
        }
    }

    tabProfile?.setOnClickListener {
        if (activeTab != "profile") {
            navigateTo(ProfileFragment.newInstance())
        }
    }
}
