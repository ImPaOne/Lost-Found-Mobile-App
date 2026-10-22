package com.group3.lostandfound

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.group3.lostandfound.adapter.ExploreAdapter
import com.group3.lostandfound.model.ItemReport
import com.group3.lostandfound.viewmodel.ExploreViewModel
import kotlinx.coroutines.launch

/**
 * ExploreFragment displays the Explore Search Screen in MVVM architecture:
 * - Realtime search with client-side Flow filtering on title & description via ExploreViewModel
 * - Status & Category filter chips (All, Lost, Found, Pets)
 * - Grid (2 cols) and List (1 col) layout toggles
 * - Live Firebase Realtime Database item stream with null-safe parsing
 * - Navigation to live DetailsFragment passing itemId
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
    private lateinit var exploreViewModel: ExploreViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_explore, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        exploreViewModel = ViewModelProvider(this)[ExploreViewModel::class.java]

        initViews(view)
        setupListeners()
        setupBottomNav(view, "explore")
        observeViewModel()
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

        exploreAdapter = ExploreAdapter(emptyList(), viewLifecycleOwner.lifecycleScope) { item ->
            openDetails(item)
        }

        rvItems.layoutManager = GridLayoutManager(requireContext(), 2)
        rvItems.adapter = exploreAdapter
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            exploreViewModel.uiState.collect { state ->
                exploreAdapter.updateItems(state.items)

                if (state.isGridMode) {
                    if (rvItems.layoutManager !is GridLayoutManager) {
                        rvItems.layoutManager = GridLayoutManager(requireContext(), 2)
                    }
                    btnGridView.setColorFilter(ContextCompat.getColor(requireContext(), R.color.text_primary))
                    btnListView.setColorFilter(ContextCompat.getColor(requireContext(), R.color.text_secondary))
                } else {
                    if (rvItems.layoutManager !is LinearLayoutManager || rvItems.layoutManager is GridLayoutManager) {
                        rvItems.layoutManager = LinearLayoutManager(requireContext())
                    }
                    btnListView.setColorFilter(ContextCompat.getColor(requireContext(), R.color.text_primary))
                    btnGridView.setColorFilter(ContextCompat.getColor(requireContext(), R.color.text_secondary))
                }
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
            Toast.makeText(requireContext(), "Showing filter options", Toast.LENGTH_SHORT).show()
        }

        inputSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                exploreViewModel.setSearchQuery(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        chipAll.setOnClickListener {
            selectChip(chipAll)
            exploreViewModel.setFilterStatus("ALL")
        }
        chipLost.setOnClickListener {
            selectChip(chipLost)
            exploreViewModel.setFilterStatus("LOST")
        }
        chipFound.setOnClickListener {
            selectChip(chipFound)
            exploreViewModel.setFilterStatus("FOUND")
        }
        chipPets.setOnClickListener {
            selectChip(chipPets)
            exploreViewModel.setFilterStatus("PETS")
        }

        btnGridView.setOnClickListener {
            exploreViewModel.setViewMode(true)
        }

        btnListView.setOnClickListener {
            exploreViewModel.setViewMode(false)
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

    private fun openDetails(item: ItemReport) {
        parentFragmentManager.beginTransaction()
            .replace(
                R.id.screenBox,
                DetailsFragment.newInstance(
                    title = item.title,
                    desc = item.description,
                    status = item.status,
                    location = if (item.latitude != 0.0 || item.longitude != 0.0) "${item.latitude}, ${item.longitude}" else "Colombo 03",
                    itemId = item.itemId
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