package com.group3.lostandfound

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.card.MaterialCardView
import com.group3.lostandfound.model.ItemReport
import com.group3.lostandfound.util.ImageLoader
import com.group3.lostandfound.util.ModernToast
import com.group3.lostandfound.viewmodel.ReportsViewModel
import kotlinx.coroutines.launch

/**
 * ReportsFragment ("My Reports") in MVVM architecture.
 * Delegating state management, stream observation, resolution, and deletion to ReportsViewModel.
 */
class ReportsFragment : Fragment() {

    private lateinit var btnBack: ImageView
    private lateinit var btnAdd: ImageView

    private lateinit var tabAct: TextView
    private lateinit var tabRes: TextView
    private lateinit var tabAll: TextView

    private lateinit var tvCount: TextView
    private lateinit var tvEmptyReports: TextView

    private lateinit var card1: MaterialCardView
    private lateinit var card2: MaterialCardView
    private lateinit var card3: MaterialCardView

    private lateinit var img1: ImageView
    private lateinit var img2: ImageView
    private lateinit var img3: ImageView

    private lateinit var name1: TextView
    private lateinit var name2: TextView
    private lateinit var name3: TextView

    private lateinit var sub1: TextView
    private lateinit var sub2: TextView
    private lateinit var sub3: TextView

    private lateinit var tag1: TextView
    private lateinit var tag2: TextView
    private lateinit var tag3: TextView

    private lateinit var opt1: ImageView
    private lateinit var opt2: ImageView
    private lateinit var opt3: ImageView

    private var currentFilter = "ACTIVE"
    private val userReports = mutableListOf<ItemReport>()
    private lateinit var reportsViewModel: ReportsViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_reports, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        reportsViewModel = ViewModelProvider(this)[ReportsViewModel::class.java]

        initViews(view)
        setupListeners()
        setupBottomNav(view, "profile")
        observeViewModel()
    }

    private fun initViews(view: View) {
        btnBack = view.findViewById(R.id.btnBack)
        btnAdd = view.findViewById(R.id.btnAdd)

        tabAct = view.findViewById(R.id.tabAct)
        tabRes = view.findViewById(R.id.tabRes)
        tabAll = view.findViewById(R.id.tabAll)

        tvCount = view.findViewById(R.id.tvCount)
        tvEmptyReports = view.findViewById(R.id.tvEmptyReports)

        card1 = view.findViewById(R.id.card1)
        card2 = view.findViewById(R.id.card2)
        card3 = view.findViewById(R.id.card3)

        img1 = view.findViewById(R.id.img1)
        img2 = view.findViewById(R.id.img2)
        img3 = view.findViewById(R.id.img3)

        name1 = view.findViewById(R.id.name1)
        name2 = view.findViewById(R.id.name2)
        name3 = view.findViewById(R.id.name3)

        sub1 = view.findViewById(R.id.sub1)
        sub2 = view.findViewById(R.id.sub2)
        sub3 = view.findViewById(R.id.sub3)

        tag1 = view.findViewById(R.id.tag1)
        tag2 = view.findViewById(R.id.tag2)
        tag3 = view.findViewById(R.id.tag3)

        opt1 = view.findViewById(R.id.opt1)
        opt2 = view.findViewById(R.id.opt2)
        opt3 = view.findViewById(R.id.opt3)
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            reportsViewModel.uiState.collect { state ->
                userReports.clear()
                userReports.addAll(state.reports)
                updateReportsListUI()
            }
        }
    }

    private fun updateReportsListUI() {
        val filtered = when (currentFilter) {
            "ACTIVE" -> userReports.filter { !it.status.equals("RESOLVED", ignoreCase = true) }
            "RESOLVED" -> userReports.filter { it.status.equals("RESOLVED", ignoreCase = true) }
            else -> userReports
        }

        tvCount.text = "${filtered.size} reports · updated live"
        tvEmptyReports.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE

        // Item 1
        if (filtered.isNotEmpty()) {
            val item1 = filtered[0]
            card1.visibility = View.VISIBLE
            name1.text = item1.title
            sub1.text = "${item1.location} · Live Post"
            tag1.text = item1.status
            tag1.setTextColor(ContextCompat.getColor(requireContext(), if (item1.status.equals("RESOLVED", ignoreCase = true)) R.color.tag_found_color else R.color.tag_lost_color))
            ImageLoader.load(img1, item1.imageUrl, R.drawable.ic_item_placeholder, viewLifecycleOwner.lifecycleScope)
            card1.setOnClickListener { openItemDetails(item1) }
            opt1.setOnClickListener { showItemMenu(item1) }
        } else {
            card1.visibility = View.GONE
        }

        // Item 2
        if (filtered.size > 1) {
            val item2 = filtered[1]
            card2.visibility = View.VISIBLE
            name2.text = item2.title
            sub2.text = "${item2.location} · Live Post"
            tag2.text = item2.status
            tag2.setTextColor(ContextCompat.getColor(requireContext(), if (item2.status.equals("RESOLVED", ignoreCase = true)) R.color.tag_found_color else R.color.tag_lost_color))
            ImageLoader.load(img2, item2.imageUrl, R.drawable.ic_item_placeholder, viewLifecycleOwner.lifecycleScope)
            card2.setOnClickListener { openItemDetails(item2) }
            opt2.setOnClickListener { showItemMenu(item2) }
        } else {
            card2.visibility = View.GONE
        }

        // Item 3
        if (filtered.size > 2) {
            val item3 = filtered[2]
            card3.visibility = View.VISIBLE
            name3.text = item3.title
            sub3.text = "${item3.location} · Live Post"
            tag3.text = item3.status
            tag3.setTextColor(ContextCompat.getColor(requireContext(), if (item3.status.equals("RESOLVED", ignoreCase = true)) R.color.tag_found_color else R.color.tag_lost_color))
            ImageLoader.load(img3, item3.imageUrl, R.drawable.ic_item_placeholder, viewLifecycleOwner.lifecycleScope)
            card3.setOnClickListener { openItemDetails(item3) }
            opt3.setOnClickListener { showItemMenu(item3) }
        } else {
            card3.visibility = View.GONE
        }
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { parentFragmentManager.popBackStack() }

        btnAdd.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.screenBox, CreateReportFragment.newInstance())
                .addToBackStack(null)
                .commit()
        }

        tabAct.setOnClickListener {
            currentFilter = "ACTIVE"
            pickTab(tabAct)
            updateReportsListUI()
        }

        tabRes.setOnClickListener {
            currentFilter = "RESOLVED"
            pickTab(tabRes)
            updateReportsListUI()
        }

        tabAll.setOnClickListener {
            currentFilter = "ALL"
            pickTab(tabAll)
            updateReportsListUI()
        }
    }

    private fun showItemMenu(item: ItemReport) {
        val options = arrayOf("View Live Details", "Mark as Resolved", "Delete Report")
        AlertDialog.Builder(requireContext())
            .setTitle(item.title)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openItemDetails(item)
                    1 -> {
                        reportsViewModel.markItemResolved(item.itemId) { success ->
                            if (success) {
                                val idx = userReports.indexOfFirst { it.itemId == item.itemId }
                                if (idx != -1) {
                                    userReports[idx] = userReports[idx].copy(status = "RESOLVED")
                                    updateReportsListUI()
                                }
                                com.group3.lostandfound.service.FCMService.triggerPushNotification(
                                    requireContext(),
                                    "Item Resolved & Reunited!",
                                    "'${item.title}' has been marked as resolved and safely reunited.",
                                    item.itemId
                                )
                                ModernToast.show(requireContext(), "'${item.title}' marked as RESOLVED.", view)
                            } else {
                                ModernToast.show(requireContext(), "Update failed.", view, durationLong = true)
                            }
                        }
                    }
                    2 -> {
                        AlertDialog.Builder(requireContext())
                            .setTitle("Delete Listing")
                            .setMessage("Remove '${item.title}' from database?")
                            .setPositiveButton("Delete") { _, _ ->
                                reportsViewModel.deleteItem(item.itemId) { success ->
                                    if (success) {
                                        ModernToast.show(requireContext(), "'${item.title}' deleted.", view)
                                    }
                                }
                            }
                            .setNegativeButton("Cancel", null)
                            .show()
                    }
                }
            }
            .show()
    }

    private fun openItemDetails(item: ItemReport) {
        parentFragmentManager.beginTransaction()
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
            .replace(
                R.id.screenBox,
                DetailsFragment.newInstance(
                    title = item.title,
                    desc = item.description,
                    status = item.status,
                    location = item.location,
                    imageResId = R.drawable.ic_item_placeholder,
                    itemId = item.itemId,
                    reporterUserId = item.userId,
                    authorName = item.authorName
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
        fun newInstance(): ReportsFragment = ReportsFragment()
    }
}
