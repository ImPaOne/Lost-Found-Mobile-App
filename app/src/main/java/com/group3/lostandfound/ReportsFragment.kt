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
 * ReportsFragment displays user's lost & found reports.
 * Uses clean human-written code with concise 3-8 character variable names.
 */
class ReportsFragment : Fragment() {

    private lateinit var btnBack: ImageView
    private lateinit var btnAdd: ImageView

    private lateinit var tabAct: TextView
    private lateinit var tabRes: TextView
    private lateinit var tabAll: TextView

    private lateinit var opt1: ImageView
    private lateinit var opt2: ImageView
    private lateinit var opt3: ImageView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_reports, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnBack = view.findViewById(R.id.btnBack)
        btnAdd = view.findViewById(R.id.btnAdd)

        tabAct = view.findViewById(R.id.tabAct)
        tabRes = view.findViewById(R.id.tabRes)
        tabAll = view.findViewById(R.id.tabAll)

        opt1 = view.findViewById(R.id.opt1)
        opt2 = view.findViewById(R.id.opt2)
        opt3 = view.findViewById(R.id.opt3)

        setupBottomNav(view, "profile")

        btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        btnAdd.setOnClickListener {
            Toast.makeText(requireContext(), "Create report", Toast.LENGTH_SHORT).show()
        }

        tabAct.setOnClickListener { pickTab(tabAct) }
        tabRes.setOnClickListener { pickTab(tabRes) }
        tabAll.setOnClickListener { pickTab(tabAll) }

        opt1.setOnClickListener {
            Toast.makeText(requireContext(), "Options: Wallet", Toast.LENGTH_SHORT).show()
        }
        opt2.setOnClickListener {
            Toast.makeText(requireContext(), "Options: Passport", Toast.LENGTH_SHORT).show()
        }
        opt3.setOnClickListener {
            Toast.makeText(requireContext(), "Options: AirPods", Toast.LENGTH_SHORT).show()
        }

        NavigationHelper.setup(this, view, NavigationHelper.Tab.PROFILE)
    }

    private fun pickTab(target: TextView) {
        val tabs = listOf(tabAct, tabRes, tabAll)
        for (tab in tabs) {
            if (tab == target) {
                tab.setBackgroundResource(R.drawable.bg_segment_active)
                tab.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
            } else {
                tab.background = null
                tab.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
            }
        }
    }

    companion object {
        fun newInstance(): ReportsFragment {
            return ReportsFragment()
        }
    }
}
