package com.group3.lostandfound

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment

/**
 * AnalyticsFragment displays application metrics:
 * - Timeframe dropdown selection & Export action
 * - Category distribution donut chart
 * - Resolution rates by major cities (Colombo, Kandy, Galle)
 * - Key performance metric cards (Median response, Time to resolve, Monthly growth)
 * - Geographic heat map radar
 */
class AnalyticsFragment : Fragment() {

    // Header views
    private lateinit var buttonBack: ImageView
    private lateinit var buttonDownloadReport: ImageView

    // Controls
    private lateinit var buttonTimeframeSelector: LinearLayout
    private lateinit var textSelectedTimeframe: TextView
    private lateinit var buttonExport: TextView

    // Metric cards
    private lateinit var cardMedianResponse: LinearLayout
    private lateinit var cardTimeToResolve: LinearLayout
    private lateinit var cardMonthlyGrowth: LinearLayout

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_analytics, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupClickListeners()
    }

    private fun initViews(view: View) {
        buttonBack = view.findViewById(R.id.buttonBack)
        buttonDownloadReport = view.findViewById(R.id.buttonDownloadReport)

        buttonTimeframeSelector = view.findViewById(R.id.buttonTimeframeSelector)
        textSelectedTimeframe = view.findViewById(R.id.textSelectedTimeframe)
        buttonExport = view.findViewById(R.id.buttonExport)

        cardMedianResponse = view.findViewById(R.id.cardMedianResponse)
        cardTimeToResolve = view.findViewById(R.id.cardTimeToResolve)
        cardMonthlyGrowth = view.findViewById(R.id.cardMonthlyGrowth)
    }

    private fun setupClickListeners() {
        buttonBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        buttonDownloadReport.setOnClickListener {
            Toast.makeText(requireContext(), "Downloading analytics report...", Toast.LENGTH_SHORT).show()
        }

        buttonTimeframeSelector.setOnClickListener {
            Toast.makeText(requireContext(), "Select timeframe: Last 30 days", Toast.LENGTH_SHORT).show()
        }

        buttonExport.setOnClickListener {
            Toast.makeText(requireContext(), "Analytics exported successfully", Toast.LENGTH_SHORT).show()
        }

        cardMedianResponse.setOnClickListener {
            Toast.makeText(requireContext(), "Median response: 2.8 hours", Toast.LENGTH_SHORT).show()
        }

        cardTimeToResolve.setOnClickListener {
            Toast.makeText(requireContext(), "Time to resolve: 1.6 days", Toast.LENGTH_SHORT).show()
        }

        cardMonthlyGrowth.setOnClickListener {
            Toast.makeText(requireContext(), "Growth: +12% vs last month", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        fun newInstance(): AnalyticsFragment {
            return AnalyticsFragment()
        }
    }
}
