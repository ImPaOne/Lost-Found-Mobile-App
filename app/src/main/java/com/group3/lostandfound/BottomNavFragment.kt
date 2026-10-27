package com.group3.lostandfound

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

/**
 * Reusable 5-tab Bottom Navigation Component:
 * Home, Explore, + Post, Messages, Profile.
 * Uses clean human-written code with concise 3-8 character variable names.
 */
class BottomNavFragment : Fragment() {

    enum class NavigationTab {
        HOME,
        EXPLORE,
        POST,
        MESSAGES,
        PROFILE
    }

    interface OnNavigationTabSelectedListener {
        fun onTabSelected(tab: NavigationTab)
    }

    private var navListener: OnNavigationTabSelectedListener? = null

    private lateinit var tabHome: LinearLayout
    private lateinit var tabExplore: LinearLayout
    private lateinit var tabPost: LinearLayout
    private lateinit var tabMessages: LinearLayout
    private lateinit var tabProfile: LinearLayout

    private lateinit var lblHome: TextView
    private lateinit var lblExplore: TextView
    private lateinit var lblMessages: TextView
    private lateinit var lblProfile: TextView

    private var curTab: NavigationTab = NavigationTab.HOME

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnNavigationTabSelectedListener) {
            navListener = context
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_bottom_nav, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tabHome = view.findViewById(R.id.tabHome)
        tabExplore = view.findViewById(R.id.tabExplore)
        tabPost = view.findViewById(R.id.tabPost)
        tabMessages = view.findViewById(R.id.tabMessages)
        tabProfile = view.findViewById(R.id.tabProfile)

        lblHome = view.findViewById(R.id.lblHome)
        lblExplore = view.findViewById(R.id.lblExplore)
        lblMessages = view.findViewById(R.id.lblMessages)
        lblProfile = view.findViewById(R.id.lblProfile)

        tabHome.setOnClickListener { pickTab(NavigationTab.HOME) }
        tabExplore.setOnClickListener { pickTab(NavigationTab.EXPLORE) }
        tabPost.setOnClickListener {
            navListener?.onTabSelected(NavigationTab.POST)
        }
        tabMessages.setOnClickListener { pickTab(NavigationTab.MESSAGES) }
        tabProfile.setOnClickListener { pickTab(NavigationTab.PROFILE) }

        updateTabStyle(curTab)
    }

    fun pickTab(tab: NavigationTab) {
        curTab = tab
        updateTabStyle(tab)
        navListener?.onTabSelected(tab)
    }

    private fun updateTabStyle(tab: NavigationTab) {
        val colAct = ContextCompat.getColor(requireContext(), R.color.text_primary)
        val colInact = ContextCompat.getColor(requireContext(), R.color.text_hint)

        lblHome.setTextColor(if (tab == NavigationTab.HOME) colAct else colInact)
        lblExplore.setTextColor(if (tab == NavigationTab.EXPLORE) colAct else colInact)
        lblMessages.setTextColor(if (tab == NavigationTab.MESSAGES) colAct else colInact)
        lblProfile.setTextColor(if (tab == NavigationTab.PROFILE) colAct else colInact)

        view?.findViewById<android.widget.ImageView>(R.id.imgHome)?.setColorFilter(if (tab == NavigationTab.HOME) colAct else colInact)
        view?.findViewById<android.widget.ImageView>(R.id.imgExplore)?.setColorFilter(if (tab == NavigationTab.EXPLORE) colAct else colInact)
        view?.findViewById<android.widget.ImageView>(R.id.imgMessages)?.setColorFilter(if (tab == NavigationTab.MESSAGES) colAct else colInact)
        view?.findViewById<android.widget.ImageView>(R.id.imgProfile)?.setColorFilter(if (tab == NavigationTab.PROFILE) colAct else colInact)
    }

    override fun onDetach() {
        super.onDetach()
        navListener = null
    }

    companion object {
        fun newInstance(): BottomNavFragment {
            return BottomNavFragment()
        }
    }
}
