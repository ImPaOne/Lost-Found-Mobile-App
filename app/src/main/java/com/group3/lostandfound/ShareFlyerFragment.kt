package com.group3.lostandfound

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

class ShareFlyerFragment : Fragment() {

    private lateinit var btnBack: View
    private lateinit var btnBookmark: View
    private lateinit var btnClose: View
    private lateinit var dimOverlay: View

    private lateinit var chipPng: TextView
    private lateinit var chipPdf: TextView

    private lateinit var btnSaveFlyer: View
    private lateinit var btnShareFlyer: View

    private var selectedFormat = "PNG" // "PNG" or "PDF"
    private var isBookmarked = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_share_flyer, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize header controls
        btnBack = view.findViewById(R.id.btnBack)
        btnBookmark = view.findViewById(R.id.btnBookmark)
        btnClose = view.findViewById(R.id.btnClose)
        dimOverlay = view.findViewById(R.id.dimOverlay)

        // Initialize export format chips
        chipPng = view.findViewById(R.id.chipPng)
        chipPdf = view.findViewById(R.id.chipPdf)

        // Initialize action buttons
        btnSaveFlyer = view.findViewById(R.id.btnSaveFlyer)
        btnShareFlyer = view.findViewById(R.id.btnShareFlyer)

        // Navigation actions
        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        btnClose.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        dimOverlay.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        btnBookmark.setOnClickListener {
            isBookmarked = !isBookmarked
            Toast.makeText(
                requireContext(),
                if (isBookmarked) "Item saved to bookmarks" else "Item removed from bookmarks",
                Toast.LENGTH_SHORT
            ).show()
        }

        // Export format selection
        chipPng.setOnClickListener {
            selectedFormat = "PNG"
            updateFormatChips()
        }

        chipPdf.setOnClickListener {
            selectedFormat = "PDF"
            updateFormatChips()
        }

        // Save flyer action
        btnSaveFlyer.setOnClickListener {
            Toast.makeText(
                requireContext(),
                "Flyer saved as $selectedFormat file",
                Toast.LENGTH_SHORT
            ).show()
        }

        // Share flyer action
        btnShareFlyer.setOnClickListener {
            val shareText = "📢 LOST: Black leather wallet\n📍 Colombo 03 · Near Duplication Road\n📅 12 Sep · 6:30 PM\n👤 Reported by Tharindu Perera\n\nHelp find this item on Foundly: https://foundly.lk/item/wallet-01"
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "LOST: Black leather wallet - Foundly LK")
                putExtra(Intent.EXTRA_TEXT, shareText)
            }
            startActivity(Intent.createChooser(shareIntent, "Share Item Flyer"))
        }

        // Bottom Navigation Bar with Explore tab active
        setupBottomNav(view, "explore")
    }

    private fun updateFormatChips() {
        val darkBg = ContextCompat.getDrawable(requireContext(), R.drawable.bg_pill_dark)
        val outlineBg = ContextCompat.getDrawable(requireContext(), R.drawable.bg_pill_outline)

        val whiteColor = ContextCompat.getColor(requireContext(), R.color.white)
        val darkColor = 0xFF0B1B2B.toInt()

        if (selectedFormat == "PNG") {
            chipPng.background = darkBg
            chipPng.setTextColor(whiteColor)

            chipPdf.background = outlineBg
            chipPdf.setTextColor(darkColor)
        } else {
            chipPdf.background = darkBg
            chipPdf.setTextColor(whiteColor)

            chipPng.background = outlineBg
            chipPng.setTextColor(darkColor)
        }
    }

    companion object {
        fun newInstance(): ShareFlyerFragment = ShareFlyerFragment()
    }
}
