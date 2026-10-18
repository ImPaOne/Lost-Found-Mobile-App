package com.group3.lostandfound

import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

/**
 * ExploreFragment displays the Explore screen with:
 * - Search bar with filter options
 * - Category filter chips (All, Lost, Found, Pets)
 * - Result count with toggle for Grid & List view
 * - 2-column grid of lost and found item cards
 * - Bottom navigation bar
 */
class ExploreFragment : Fragment() {

    private lateinit var btnClose: FrameLayout
    private lateinit var btnMapView: ImageView

    private lateinit var inputSearch: EditText
    private lateinit var btnFilter: ImageView

    private lateinit var chipAll: TextView
    private lateinit var chipLost: TextView
    private lateinit var chipFound: TextView
    private lateinit var chipPets: TextView

    private lateinit var btnGridView: ImageView
    private lateinit var btnListView: ImageView

    private lateinit var cardPhone: View
    private lateinit var cardPassport: View
    private lateinit var cardCat: View
    private lateinit var cardAirpods: View

    private lateinit var tvPhoneStatus: TextView
    private lateinit var tvPassportStatus: TextView
    private lateinit var tvCatStatus: TextView
    private lateinit var tvAirpodsStatus: TextView

        override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_explore, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupStatusBadges()
        setupListeners()
        setupBottomNav(view, "explore")
    }
        private fun initViews(view: View) {
        btnClose = view.findViewById(R.id.btnClose)
        btnMapView = view.findViewById(R.id.btnMapView)

        inputSearch = view.findViewById(R.id.inputSearch)
        btnFilter = view.findViewById(R.id.btnFilter)

        chipAll = view.findViewById(R.id.chipAll)
        chipLost = view.findViewById(R.id.chipLost)
        chipFound = view.findViewById(R.id.chipFound)
        chipPets = view.findViewById(R.id.chipPets)

        btnGridView = view.findViewById(R.id.btnGridView)
        btnListView = view.findViewById(R.id.btnListView)

        cardPhone = view.findViewById(R.id.cardPhone)
        cardPassport = view.findViewById(R.id.cardPassport)
        cardCat = view.findViewById(R.id.cardCat)
        cardAirpods = view.findViewById(R.id.cardAirpods)

        tvPhoneStatus = view.findViewById(R.id.tvPhoneStatus)
        tvPassportStatus = view.findViewById(R.id.tvPassportStatus)
        tvCatStatus = view.findViewById(R.id.tvCatStatus)
        tvAirpodsStatus = view.findViewById(R.id.tvAirpodsStatus)
    }
        private fun setupStatusBadges() {
        // Formats "LOST · 2h" and "FOUND · 2h" with two-tone colors
        val lostColor = ContextCompat.getColor(requireContext(), R.color.tag_lost_color)
        val foundColor = ContextCompat.getColor(requireContext(), R.color.text_primary)
        val mutedColor = ContextCompat.getColor(requireContext(), R.color.text_secondary)

        formatBadge(tvPhoneStatus, "LOST", " · 2h", lostColor, mutedColor)
        formatBadge(tvPassportStatus, "FOUND", " · 2h", foundColor, mutedColor)
        formatBadge(tvCatStatus, "LOST", " · 2h", lostColor, mutedColor)
        formatBadge(tvAirpodsStatus, "FOUND", " · 2h", foundColor, mutedColor)
    }

    private fun formatBadge(textView: TextView, status: String, time: String, statusColor: Int, timeColor: Int) {
        val span = SpannableString("$status$time")
        span.setSpan(ForegroundColorSpan(statusColor), 0, status.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        span.setSpan(StyleSpan(Typeface.BOLD), 0, status.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        span.setSpan(ForegroundColorSpan(timeColor), status.length, span.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        textView.text = span
    }
        private fun setupListeners() {
        btnClose.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        btnMapView.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.screenBox, MapFragment.newInstance())
                .addToBackStack(null)
                .commit()
        }

        btnFilter.setOnClickListener {
            Toast.makeText(requireContext(), "Filter items", Toast.LENGTH_SHORT).show()
        }

        chipAll.setOnClickListener { selectChip(chipAll) }
        chipLost.setOnClickListener { selectChip(chipLost) }
        chipFound.setOnClickListener { selectChip(chipFound) }
        chipPets.setOnClickListener { selectChip(chipPets) }

        btnGridView.setOnClickListener {
            Toast.makeText(requireContext(), "Grid view active", Toast.LENGTH_SHORT).show()
        }

        btnListView.setOnClickListener {
            Toast.makeText(requireContext(), "Switching to list view", Toast.LENGTH_SHORT).show()
        }

        cardPhone.setOnClickListener {
            openDetails(
                title = "Blue Samsung phone",
                desc = "Blue Samsung Galaxy with transparent protective case. Lost on bus route 138.",
                status = "LOST",
                location = "Bambalapitiya",
                imageResId = R.drawable.img_samsung_phone
            )
        }
        cardPassport.setOnClickListener {
            openDetails(
                title = "Sri Lankan passport",
                desc = "Found maroon Sri Lankan passport near ticket counter at Fort Railway Station.",
                status = "FOUND",
                location = "Fort Railway Station",
                imageResId = R.drawable.img_passport
            )
        }
        cardCat.setOnClickListener {
            openDetails(
                title = "Brown tabby cat",
                desc = "Lost domestic tabby cat, friendly, answers to 'Ginger'. Wearing green collar.",
                status = "LOST",
                location = "Dehiwala",
                imageResId = R.drawable.img_tabby_cat
            )
        }
        cardAirpods.setOnClickListener {
            openDetails(
                title = "AirPods Pro Case",
                desc = "White AirPods Pro case found on coffee table. Please describe engraving to claim.",
                status = "FOUND",
                location = "Kollupitiya",
                imageResId = R.drawable.img_wallet_details
            )
        }
    }

    private fun selectChip(active: TextView) {
        val chips = listOf(chipAll, chipLost, chipFound, chipPets)
        for (chip in chips) {
            if (chip == active) {
                chip.setBackgroundResource(R.drawable.bg_chip_selected)
                chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            } else {
                chip.setBackgroundResource(R.drawable.bg_chip_unselected)
                chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
            }
        }
    }

    private fun openDetails(
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

    companion object {
        fun newInstance(): ExploreFragment {
            return ExploreFragment()
        }
    }
}