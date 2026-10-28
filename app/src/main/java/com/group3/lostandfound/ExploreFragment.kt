package com.group3.lostandfound
//Handles Real time search
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

class ExploreFragment : Fragment() {

    // References to UI components in the XML layout.
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

    // RecyclerView displays reports; Adapter binds report data to cards.
    private lateinit var rvItems: RecyclerView
    private lateinit var exploreAdapter: ExploreAdapter

    // ViewModel manages the Explore screen's UI state.
    private lateinit var exploreViewModel: ExploreViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate and return the Explore screen's XML layout.
        return inflater.inflate(R.layout.fragment_explore, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Create or retrieve the ViewModel for this Fragment.
        exploreViewModel = ViewModelProvider(this)[ExploreViewModel::class.java]

        initViews(view)//connects kotlin varibles to XML components
        setupListeners()// Handle user interactions.
        setupBottomNav(view, "explore")//configure the bottom navigation for the explore screen
        observeViewModel()//state and update UI.
    }

    private fun initViews(view: View) {
        // Connect UI variables to their XML components using IDs
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

        // Create the Adapter with an empty initial list.
        // Clicking an item opens its details.
        exploreAdapter = ExploreAdapter(emptyList(), viewLifecycleOwner.lifecycleScope) { item ->
            openDetails(item)
        }

        // Show two columns in grid mode.
        rvItems.layoutManager = GridLayoutManager(requireContext(), 2)
        // Connect the Adapter to the RecyclerView.
        rvItems.adapter = exploreAdapter
    }

    private fun observeViewModel() {
        // Collect UI state while this Fragment's view is active.
        viewLifecycleOwner.lifecycleScope.launch {
            exploreViewModel.uiState.collect { state ->
                // Refresh the displayed reports when state changes.
                exploreAdapter.updateItems(state.items)

                if (state.isGridMode) {
                    // Grid mode displays two columns.
                    if (rvItems.layoutManager !is GridLayoutManager) {
                        rvItems.layoutManager = GridLayoutManager(requireContext(), 2)
                    }
                    btnGridView.setColorFilter(ContextCompat.getColor(requireContext(), R.color.text_primary))
                    btnListView.setColorFilter(ContextCompat.getColor(requireContext(), R.color.text_secondary))
                } else {
                    // List mode displays one item per row.
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
        // Return to the previous Fragment.
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
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
            .replace(
                R.id.screenBox,
                DetailsFragment.newInstance(
                    title = item.title,
                    desc = item.description,
                    status = item.status,
                    location = if (item.latitude != 0.0 || item.longitude != 0.0) "${item.latitude}, ${item.longitude}" else "Colombo 03",
                    itemId = item.itemId,
                    reporterUserId = item.userId,
                    authorName = item.authorName
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