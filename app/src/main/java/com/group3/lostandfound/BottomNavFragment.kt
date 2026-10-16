package com.group3.lostandfound

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

/**
 * Reusable Bottom Navigation Fragment component.
 * Dispatches tab selections to the parent Activity via [OnNavigationTabSelectedListener].
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

    private var navigationListener: OnNavigationTabSelectedListener? = null

    // Navigation tab containers
    private lateinit var tabHome: LinearLayout
    private lateinit var tabExplore: LinearLayout
    private lateinit var tabPost: LinearLayout
    private lateinit var tabMessages: LinearLayout
    private lateinit var tabProfile: LinearLayout

    // Tab icons & text labels for state styling
    private lateinit var imageHomeIcon: ImageView
    private lateinit var textHomeLabel: TextView

    private lateinit var imageExploreIcon: ImageView
    private lateinit var textExploreLabel: TextView

    private lateinit var imageMessagesIcon: ImageView
    private lateinit var textMessagesLabel: TextView

    private lateinit var imageProfileIcon: ImageView
    private lateinit var textProfileLabel: TextView

    private var activeTab: NavigationTab = NavigationTab.HOME

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnNavigationTabSelectedListener) {
            navigationListener = context
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

        initViews(view)
        setupTabClickListeners()
        updateTabStyles(activeTab)
    }

    private fun initViews(view: View) {
        tabHome = view.findViewById(R.id.tabHome)
        tabExplore = view.findViewById(R.id.tabExplore)
        tabPost = view.findViewById(R.id.tabPost)
        tabMessages = view.findViewById(R.id.tabMessages)
        tabProfile = view.findViewById(R.id.tabProfile)

        imageHomeIcon = view.findViewById(R.id.imageHomeIcon)
        textHomeLabel = view.findViewById(R.id.textHomeLabel)

        imageExploreIcon = view.findViewById(R.id.imageExploreIcon)
        textExploreLabel = view.findViewById(R.id.textExploreLabel)

        imageMessagesIcon = view.findViewById(R.id.imageMessagesIcon)
        textMessagesLabel = view.findViewById(R.id.textMessagesLabel)

        imageProfileIcon = view.findViewById(R.id.imageProfileIcon)
        textProfileLabel = view.findViewById(R.id.textProfileLabel)
    }

    private fun setupTabClickListeners() {
        tabHome.setOnClickListener {
            selectTab(NavigationTab.HOME)
        }

        tabExplore.setOnClickListener {
            selectTab(NavigationTab.EXPLORE)
        }

        tabPost.setOnClickListener {
            // Post opens creation flow or dialog
            navigationListener?.onTabSelected(NavigationTab.POST)
        }

        tabMessages.setOnClickListener {
            selectTab(NavigationTab.MESSAGES)
        }

        tabProfile.setOnClickListener {
            selectTab(NavigationTab.PROFILE)
        }
    }

    fun selectTab(tab: NavigationTab) {
        activeTab = tab
        updateTabStyles(tab)
        navigationListener?.onTabSelected(tab)
    }

    private fun updateTabStyles(selectedTab: NavigationTab) {
        val colorActive = ContextCompat.getColor(requireContext(), R.color.text_primary)
        val colorInactive = ContextCompat.getColor(requireContext(), R.color.text_secondary)

        // Reset all tabs to inactive color
        textHomeLabel.setTextColor(colorInactive)
        textExploreLabel.setTextColor(colorInactive)
        textMessagesLabel.setTextColor(colorInactive)
        textProfileLabel.setTextColor(colorInactive)

        // Apply active color to selected tab
        when (selectedTab) {
            NavigationTab.HOME -> {
                textHomeLabel.setTextColor(colorActive)
            }
            NavigationTab.EXPLORE -> {
                textExploreLabel.setTextColor(colorActive)
            }
            NavigationTab.MESSAGES -> {
                textMessagesLabel.setTextColor(colorActive)
            }
            NavigationTab.PROFILE -> {
                textProfileLabel.setTextColor(colorActive)
            }
            NavigationTab.POST -> {
                // Post has fixed pill styling
            }
        }
    }

    override fun onDetach() {
        super.onDetach()
        navigationListener = null
    }

    companion object {
        fun newInstance(): BottomNavFragment {
            return BottomNavFragment()
        }
    }
}
