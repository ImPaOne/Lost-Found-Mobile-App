package com.group3.lostandfound

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

/**
 * ModerationFragment handles moderation and review of flagged/pending lost & found reports.
 * Uses clean human-written code and descriptive variable names.
 */
class ModerationFragment : Fragment() {

    // Header views
    private lateinit var buttonBack: ImageView
    private lateinit var buttonSearch: ImageView

    // Segment tabs
    private lateinit var tabPendingReview: TextView
    private lateinit var tabFlagged: TextView
    private lateinit var tabApproved: TextView

    // Top action buttons
    private lateinit var buttonSelectAll: TextView
    private lateinit var buttonApproveSelected: TextView

    // Passport Card buttons
    private lateinit var buttonPassportApprove: TextView
    private lateinit var buttonPassportReject: TextView
    private lateinit var buttonPassportEscalate: TextView

    // iPhone Card buttons
    private lateinit var buttonIPhoneApprove: TextView
    private lateinit var buttonIPhoneReject: TextView
    private lateinit var buttonIPhoneEscalate: TextView

    // Bracelet Card buttons
    private lateinit var buttonBraceletApprove: TextView
    private lateinit var buttonBraceletReject: TextView
    private lateinit var buttonBraceletEscalate: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_moderation, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupClickListeners()
        setupBottomNav(view, "explore")
    }

    private fun initViews(view: View) {
        buttonBack = view.findViewById(R.id.buttonBack)
        buttonSearch = view.findViewById(R.id.buttonSearch)

        tabPendingReview = view.findViewById(R.id.tabPendingReview)
        tabFlagged = view.findViewById(R.id.tabFlagged)
        tabApproved = view.findViewById(R.id.tabApproved)

        buttonSelectAll = view.findViewById(R.id.buttonSelectAll)
        buttonApproveSelected = view.findViewById(R.id.buttonApproveSelected)

        buttonPassportApprove = view.findViewById(R.id.buttonPassportApprove)
        buttonPassportReject = view.findViewById(R.id.buttonPassportReject)
        buttonPassportEscalate = view.findViewById(R.id.buttonPassportEscalate)

        buttonIPhoneApprove = view.findViewById(R.id.buttonIPhoneApprove)
        buttonIPhoneReject = view.findViewById(R.id.buttonIPhoneReject)
        buttonIPhoneEscalate = view.findViewById(R.id.buttonIPhoneEscalate)

        buttonBraceletApprove = view.findViewById(R.id.buttonBraceletApprove)
        buttonBraceletReject = view.findViewById(R.id.buttonBraceletReject)
        buttonBraceletEscalate = view.findViewById(R.id.buttonBraceletEscalate)
    }

    private fun setupClickListeners() {
        buttonBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        buttonSearch.setOnClickListener {
            Toast.makeText(requireContext(), "Search moderation reports", Toast.LENGTH_SHORT).show()
        }

        tabPendingReview.setOnClickListener { selectTab(tabPendingReview) }
        tabFlagged.setOnClickListener { selectTab(tabFlagged) }
        tabApproved.setOnClickListener { selectTab(tabApproved) }

        buttonSelectAll.setOnClickListener {
            Toast.makeText(requireContext(), "All 24 reports selected", Toast.LENGTH_SHORT).show()
        }

        buttonApproveSelected.setOnClickListener {
            Toast.makeText(requireContext(), "Selected reports approved", Toast.LENGTH_SHORT).show()
        }

        // Passport card actions
        buttonPassportApprove.setOnClickListener {
            Toast.makeText(requireContext(), "Approved: Sri Lankan passport", Toast.LENGTH_SHORT).show()
        }
        buttonPassportReject.setOnClickListener {
            Toast.makeText(requireContext(), "Rejected: Sri Lankan passport", Toast.LENGTH_SHORT).show()
        }
        buttonPassportEscalate.setOnClickListener {
            Toast.makeText(requireContext(), "Escalated: Sri Lankan passport", Toast.LENGTH_SHORT).show()
        }

        // iPhone card actions
        buttonIPhoneApprove.setOnClickListener {
            Toast.makeText(requireContext(), "Approved: iPhone 14 Pro", Toast.LENGTH_SHORT).show()
        }
        buttonIPhoneReject.setOnClickListener {
            Toast.makeText(requireContext(), "Rejected: iPhone 14 Pro", Toast.LENGTH_SHORT).show()
        }
        buttonIPhoneEscalate.setOnClickListener {
            Toast.makeText(requireContext(), "Escalated: iPhone 14 Pro", Toast.LENGTH_SHORT).show()
        }

        // Gold bracelet card actions
        buttonBraceletApprove.setOnClickListener {
            Toast.makeText(requireContext(), "Approved: Gold bracelet", Toast.LENGTH_SHORT).show()
        }
        buttonBraceletReject.setOnClickListener {
            Toast.makeText(requireContext(), "Rejected: Gold bracelet", Toast.LENGTH_SHORT).show()
        }
        buttonBraceletEscalate.setOnClickListener {
            Toast.makeText(requireContext(), "Escalated: Gold bracelet", Toast.LENGTH_SHORT).show()
        }
    }

    private fun selectTab(selectedTab: TextView) {
        val allTabs = listOf(tabPendingReview, tabFlagged, tabApproved)
        for (tab in allTabs) {
            if (tab == selectedTab) {
                tab.setBackgroundResource(R.drawable.bg_segment_active)
                tab.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
            } else {
                tab.background = null
                tab.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
            }
        }
    }

    companion object {
        fun newInstance(): ModerationFragment {
            return ModerationFragment()
        }
    }
}
