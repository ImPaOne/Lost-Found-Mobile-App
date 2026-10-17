package com.group3.lostandfound

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment

/**
 * AdminFragment displays the operations overview, statistics, trend chart,
 * moderation queue banner, quick actions, and recent activity.
 * Built with clean human-written code.
 */
class AdminFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_admin, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnBack = view.findViewById<ImageView>(R.id.backButton)
        val btnBell = view.findViewById<ImageView>(R.id.notificationBell)
        val cardModQueue = view.findViewById<View>(R.id.moderationQueueCard)
        val btnReview = view.findViewById<View>(R.id.reviewReportsButton)
        val btnUsers = view.findViewById<View>(R.id.manageUsersButton)
        val btnAnnounce = view.findViewById<View>(R.id.announcementButton)

        btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        btnBell.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.screenBox, NotificationsFragment.newInstance())
                .addToBackStack(null)
                .commit()
        }

        cardModQueue.setOnClickListener {
            openModerationScreen()
        }

        btnReview.setOnClickListener {
            openModerationScreen()
        }

        btnUsers.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.screenBox, TrustFragment.newInstance())
                .addToBackStack(null)
                .commit()
        }

        btnAnnounce.setOnClickListener {
            Toast.makeText(requireContext(), "Send announcement", Toast.LENGTH_SHORT).show()
        }

        view.findViewById<View>(R.id.trendGraph)?.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.screenBox, AnalyticsFragment.newInstance())
                .addToBackStack(null)
                .commit()
        }

        view.findViewById<View>(R.id.totalReportsNumber)?.setOnClickListener {
            openModerationScreen()
        }

        view.findViewById<View>(R.id.activeCasesNumber)?.setOnClickListener {
            openModerationScreen()
        }

        view.findViewById<View>(R.id.resolvedCasesNumber)?.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.screenBox, AnalyticsFragment.newInstance())
                .addToBackStack(null)
                .commit()
        }

        view.findViewById<View>(R.id.totalUsersNumber)?.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.screenBox, TrustFragment.newInstance())
                .addToBackStack(null)
                .commit()
        }

        view.findViewById<View>(R.id.activityItemApproved)?.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.screenBox, DetailsFragment.newInstance())
                .addToBackStack(null)
                .commit()
        }

        view.findViewById<View>(R.id.activityItemVerification)?.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.screenBox, TrustFragment.newInstance())
                .addToBackStack(null)
                .commit()
        }

        view.findViewById<View>(R.id.activityItemEscalated)?.setOnClickListener {
            openModerationScreen()
        }

        setupBottomNav(view, "admin")
    }

    private fun openModerationScreen() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.screenBox, ModerationFragment.newInstance())
            .addToBackStack(null)
            .commit()
    }

    companion object {
        fun newInstance(): AdminFragment = AdminFragment()
    }
}
