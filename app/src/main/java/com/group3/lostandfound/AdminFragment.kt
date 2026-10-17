package com.group3.lostandfound

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment

/**
 * AdminFragment displays the operations overview, statistics, trend chart,
 * moderation queue banner, quick actions, and recent activity.
 * Built with simple human-written code and meaningful variable names.
 */
class AdminFragment : Fragment() {

    private lateinit var backButton: ImageView
    private lateinit var headerTitle: TextView
    private lateinit var notificationBell: ImageView

    private lateinit var moderationQueueCard: ConstraintLayout
    private lateinit var todayBadge: TextView
    private lateinit var reviewReportsButton: TextView
    private lateinit var manageUsersButton: TextView
    private lateinit var announcementButton: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_admin, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize header controls
        backButton = view.findViewById(R.id.backButton)
        headerTitle = view.findViewById(R.id.headerTitle)
        notificationBell = view.findViewById(R.id.notificationBell)

        // Initialize action buttons and cards
        moderationQueueCard = view.findViewById(R.id.moderationQueueCard)
        todayBadge = view.findViewById(R.id.todayBadge)
        reviewReportsButton = view.findViewById(R.id.reviewReportsButton)
        manageUsersButton = view.findViewById(R.id.manageUsersButton)
        announcementButton = view.findViewById(R.id.announcementButton)

        // Set static text explicitly
        headerTitle.text = "Admin Dashboard"
        todayBadge.text = "+6 today"

        // Set click listeners
        backButton.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        notificationBell.setOnClickListener {
            Toast.makeText(requireContext(), "Admin notifications", Toast.LENGTH_SHORT).show()
        }

        moderationQueueCard.setOnClickListener {
            openModerationScreen()
        }

        reviewReportsButton.setOnClickListener {
            openModerationScreen()
        }

        manageUsersButton.setOnClickListener {
            Toast.makeText(requireContext(), "Manage users", Toast.LENGTH_SHORT).show()
        }

        announcementButton.setOnClickListener {
            Toast.makeText(requireContext(), "Send announcement", Toast.LENGTH_SHORT).show()
        }

<<<<<<< HEAD
        NavigationHelper.setup(this, view, NavigationHelper.Tab.EXPLORE)
=======
        setupBottomNav(view, "profile")
>>>>>>> c05a9c9ed8291e3e368b7e215914556a2659bc9b
    }

    private fun openModerationScreen() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.screenBox, ModerationFragment.newInstance())
            .addToBackStack(null)
            .commit()
    }

    companion object {
        fun newInstance(): AdminFragment {
            return AdminFragment()
        }
    }
}
