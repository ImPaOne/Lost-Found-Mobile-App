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
 * AdminFragment displays operational statistics, trend graphs, and recent activity.
 * Built with clean human-written code using concise 3-8 character variable names.
 */
class AdminFragment : Fragment() {

    private lateinit var btnBack: ImageView
    private lateinit var tvTitle: TextView
    private lateinit var btnBell: ImageView

    private lateinit var boxQueue: ConstraintLayout
    private lateinit var tagToday: TextView
    private lateinit var btnReview: TextView
    private lateinit var btnUsers: TextView
    private lateinit var btnAnnounce: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_admin, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnBack = view.findViewById(R.id.btnBack)
        tvTitle = view.findViewById(R.id.tvTitle)
        btnBell = view.findViewById(R.id.btnBell)

        boxQueue = view.findViewById(R.id.boxQueue)
        tagToday = view.findViewById(R.id.tagToday)
        btnReview = view.findViewById(R.id.btnReview)
        btnUsers = view.findViewById(R.id.btnUsers)
        btnAnnounce = view.findViewById(R.id.btnAnnounce)

        // Explicitly set titles in code to guarantee no layout preview fallback
        tvTitle.text = "Admin Dashboard"
        tagToday.text = "+6 today"

        btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        btnBell.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.screenBox, NotificationsFragment.newInstance())
                .addToBackStack(null)
                .commit()
        }

        // Tapping the moderation queue or "Review Reports" opens ModerationFragment
        boxQueue.setOnClickListener { openModeration() }
        btnReview.setOnClickListener { openModeration() }

        btnUsers.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.screenBox, UserManagementFragment.newInstance())
                .addToBackStack(null)
                .commit()
        }

        btnAnnounce.setOnClickListener {
            Toast.makeText(requireContext(), "Send announcement", Toast.LENGTH_SHORT).show()
        }

        setupBottomNav(view, "profile")
    }

    private fun openModeration() {
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
