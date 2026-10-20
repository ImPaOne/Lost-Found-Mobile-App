package com.group3.lostandfound

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

/**
 * ReportsFragment displays user's lost & found reports.
 * Executes: FirebaseDatabase.getInstance().getReference("items").orderByChild("userId").equalTo(currentUid)
 * Allows user to manage listings and mark them as RESOLVED in real time.
 */
class ReportsFragment : Fragment() {

    private lateinit var btnBack: ImageView
    private lateinit var btnAdd: ImageView

    private lateinit var tabAct: TextView
    private lateinit var tabRes: TextView
    private lateinit var tabAll: TextView

    private lateinit var tvCount: TextView

    private lateinit var card1: View
    private lateinit var card2: View
    private lateinit var card3: View

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

    private val userReports = mutableListOf<ReportItem>()
    private var currentFilter = "ACTIVE"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_reports, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupListeners()
        setupBottomNav(view, "profile")
        loadMyReportsFromFirebase()
    }

    private fun initViews(view: View) {
        btnBack = view.findViewById(R.id.btnBack)
        btnAdd = view.findViewById(R.id.btnAdd)

        tabAct = view.findViewById(R.id.tabAct)
        tabRes = view.findViewById(R.id.tabRes)
        tabAll = view.findViewById(R.id.tabAll)

        tvCount = view.findViewById(R.id.tvCount)

        card1 = view.findViewById(R.id.card1)
        card2 = view.findViewById(R.id.card2)
        card3 = view.findViewById(R.id.card3)

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

    /**
     * Executes required Firebase query:
     * FirebaseDatabase.getInstance().getReference("items").orderByChild("userId").equalTo(currentUid)
     */
    private fun loadMyReportsFromFirebase() {
        val currentUid = FirebaseManager.currentUid
        val query = FirebaseDatabase.getInstance(FirebaseManager.DATABASE_URL)
            .getReference("items")
            .orderByChild("userId")
            .equalTo(currentUid)

        query.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                userReports.clear()
                for (child in snapshot.children) {
                    val item = child.getValue(ReportItem::class.java)
                    if (item != null) {
                        userReports.add(item)
                    }
                }

                updateReportsListUI()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Failed to load reports: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateReportsListUI() {
        val filtered = when (currentFilter) {
            "ACTIVE" -> userReports.filter { !it.status.equals("RESOLVED", ignoreCase = true) }
            "RESOLVED" -> userReports.filter { it.status.equals("RESOLVED", ignoreCase = true) }
            else -> userReports
        }

        tvCount.text = "${filtered.size} reports · updated live"

        // Item 1
        if (filtered.isNotEmpty()) {
            val item1 = filtered[0]
            card1.visibility = View.VISIBLE
            name1.text = item1.title
            sub1.text = item1.location
            tag1.text = item1.status
            card1.setOnClickListener { openItemDetails(item1) }
            opt1.setOnClickListener { showOptionsMenu(item1) }
        } else {
            card1.visibility = View.GONE
        }

        // Item 2
        if (filtered.size > 1) {
            val item2 = filtered[1]
            card2.visibility = View.VISIBLE
            name2.text = item2.title
            sub2.text = item2.location
            tag2.text = item2.status
            card2.setOnClickListener { openItemDetails(item2) }
            opt2.setOnClickListener { showOptionsMenu(item2) }
        } else {
            card2.visibility = View.GONE
        }

        // Item 3
        if (filtered.size > 2) {
            val item3 = filtered[2]
            card3.visibility = View.VISIBLE
            name3.text = item3.title
            sub3.text = item3.location
            tag3.text = item3.status
            card3.setOnClickListener { openItemDetails(item3) }
            opt3.setOnClickListener { showOptionsMenu(item3) }
        } else {
            card3.visibility = View.GONE
        }
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

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

    private fun showOptionsMenu(item: ReportItem) {
        val options = arrayOf(
            "View report details",
            "Mark as resolved (Firebase)",
            "Delete report (Firebase)"
        )

        AlertDialog.Builder(requireContext())
            .setTitle(item.title)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openItemDetails(item)
                    1 -> {
                        // Mark status as "RESOLVED" in Firebase Realtime DB
                        FirebaseManager.itemsRef
                            .child(item.itemId)
                            .child("status")
                            .setValue("RESOLVED")
                            .addOnSuccessListener {
                                com.group3.lostandfound.util.ModernToast.show(requireContext(), "'${item.title}' marked as RESOLVED.", view)
                            }
                            .addOnFailureListener {
                                com.group3.lostandfound.util.ModernToast.show(requireContext(), "Update failed: ${it.message}", view, durationLong = true)
                            }
                    }
                    2 -> {
                        // Delete report using removeValue()
                        AlertDialog.Builder(requireContext())
                            .setTitle("Delete Listing")
                            .setMessage("Remove '${item.title}' from database?")
                            .setPositiveButton("Delete") { _, _ ->
                                FirebaseManager.itemsRef
                                    .child(item.itemId)
                                    .removeValue()
                                    .addOnSuccessListener {
                                        com.group3.lostandfound.util.ModernToast.show(requireContext(), "'${item.title}' deleted.", view)
                                    }
                            }
                            .setNegativeButton("Cancel", null)
                            .show()
                    }
                }
            }
            .show()
    }

    private fun openItemDetails(item: ReportItem) {
        parentFragmentManager.beginTransaction()
            .replace(
                R.id.screenBox,
                DetailsFragment.newInstance(
                    title = item.title,
                    desc = item.description,
                    status = item.status,
                    location = item.location,
                    imageResId = R.drawable.img_wallet_details
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
