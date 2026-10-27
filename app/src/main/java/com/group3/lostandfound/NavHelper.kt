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
 * Renders 5 standard tabs matching the reference design:
 * Home, Explore, (+) Post, Messages, Profile.
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

    // Always standard 5 tabs: Home, Explore, + Post, Messages, Profile
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
        if (isAdmin) {
            if (this !is AdminFragment) navigateTo(AdminFragment.newInstance())
        } else {
            if (this !is HomeFragment) navigateTo(HomeFragment.newInstance())
        }
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
