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
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.group3.lostandfound.adapter.ExploreAdapter
import com.group3.lostandfound.repository.ExploreRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

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

    private lateinit var rvItems: RecyclerView
    private lateinit var exploreAdapter: ExploreAdapter
    private val exploreRepository = ExploreRepository()
    private var currentFilterStatus = "ALL"

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
        setupListeners()
        setupBottomNav(view, "explore")
        fetchItems()
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

        rvItems = view.findViewById(R.id.rvItems)
        
        exploreAdapter = ExploreAdapter(emptyList()) { item ->
            // Assume imageResId is normally 0 or loaded from URL in DetailsFragment
            openDetails(
                title = item.title,
                desc = item.description,
                status = item.status,
                location = "${item.latitude}, ${item.longitude}",
                imageResId = R.drawable.img_samsung_phone
            )
        }
        rvItems.layoutManager = GridLayoutManager(requireContext(), 2)
        rvItems.adapter = exploreAdapter
    }
    
    private fun fetchItems() {
        lifecycleScope.launch {
            val query = inputSearch.text.toString()
            exploreRepository.getItemsFiltered(query, currentFilterStatus).collectLatest { items ->
                exploreAdapter.updateItems(items)
            }
        }
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

        chipAll.setOnClickListener { 
            currentFilterStatus = "ALL"
            selectChip(chipAll)
            fetchItems()
        }
        chipLost.setOnClickListener { 
            currentFilterStatus = "LOST"
            selectChip(chipLost)
            fetchItems()
        }
        chipFound.setOnClickListener { 
            currentFilterStatus = "FOUND"
            selectChip(chipFound)
            fetchItems()
        }
        chipPets.setOnClickListener { 
            currentFilterStatus = "PETS"
            selectChip(chipPets)
            fetchItems()
        }

        btnGridView.setOnClickListener {
            Toast.makeText(requireContext(), "Grid view active", Toast.LENGTH_SHORT).show()
        }

        btnListView.setOnClickListener {
            Toast.makeText(requireContext(), "Switching to list view", Toast.LENGTH_SHORT).show()
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