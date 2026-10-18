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