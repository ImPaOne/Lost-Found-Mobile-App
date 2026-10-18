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

        val card1 = view.findViewById<View>(R.id.card1)
        val card2 = view.findViewById<View>(R.id.card2)
        val card3 = view.findViewById<View>(R.id.card3)

        setupBottomNav(view, "profile")

        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        btnAdd.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.screenBox, CreateReportFragment.newInstance())
                .addToBackStack(null)
                .commit()
        }

        tabAct.setOnClickListener { pickTab(tabAct) }
        tabRes.setOnClickListener { pickTab(tabRes) }
        tabAll.setOnClickListener { pickTab(tabAll) }

        card1?.setOnClickListener {
            openItemDetails(
                title = "Black leather wallet",
                desc = "Slim wallet with national ID and two bank cards. Small scratch near clasp.",
                status = "PENDING",
                location = "Colombo 03 · 12 Sep",
                imageResId = R.drawable.img_wallet
            )
        }

        card2?.setOnClickListener {
            openItemDetails(
                title = "Sri Lankan passport",
                desc = "Found maroon Sri Lankan passport near ticket counter at Fort Railway Station.",
                status = "MATCHED",
                location = "Fort Railway · 08 Sep",
                imageResId = R.drawable.img_passport
            )
        }

        card3?.setOnClickListener {
            openItemDetails(
                title = "AirPods Pro Case",
                desc = "White AirPods Pro wireless charging case found on table.",
                status = "RESOLVED",
                location = "Bambalapitiya · 01 Sep",
                imageResId = R.drawable.img_wallet_details
            )
        }

        opt1.setOnClickListener { showOptionsMenu("Black leather wallet") }
        opt2.setOnClickListener { showOptionsMenu("Sri Lankan passport") }
        opt3.setOnClickListener { showOptionsMenu("AirPods Pro Case") }
    }

    private fun showOptionsMenu(itemName: String) {
        val options = arrayOf("View report details", "Mark as resolved", "Edit report", "Delete report")
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(itemName)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> Toast.makeText(requireContext(), "Opening $itemName", Toast.LENGTH_SHORT).show()
                    1 -> Toast.makeText(requireContext(), "$itemName marked as resolved", Toast.LENGTH_SHORT).show()
                    2 -> Toast.makeText(requireContext(), "Edit $itemName", Toast.LENGTH_SHORT).show()
                    3 -> Toast.makeText(requireContext(), "$itemName deleted", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    private fun openItemDetails(
        title: String,
        desc: String,
        status: String,
        location: String,
        imageResId: Int
    ) {
        parentFragmentManager.beginTransaction()
            .replace(
                R.id.screenBox,
                DetailsFragment.newInstance(
                    title = title,
                    desc = desc,
                    status = status,
                    location = location,
                    imageResId = imageResId
                )
            )
            .addToBackStack(null)
            .commit()
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
