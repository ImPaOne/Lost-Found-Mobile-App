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