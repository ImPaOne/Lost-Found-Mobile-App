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
    private lateinit var btnAdd: View

    private lateinit var tabAct: TextView
    private lateinit var tabRes: TextView
    private lateinit var tabAll: TextView

    private lateinit var tvCount: TextView
    private lateinit var tvEmptyReports: TextView

    private lateinit var card1: View
    private lateinit var card2: View
    private lateinit var card3: View

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

        // Subtitles are now nested TextViews inside LinearLayouts with pin icons
        sub1 = view.findViewById(R.id.tvSub1)
        sub2 = view.findViewById(R.id.tvSub2)
        sub3 = view.findViewById(R.id.tvSub3)

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
            applyBadgeStyle(tag1, item1.status)
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
            applyBadgeStyle(tag2, item2.status)
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
            applyBadgeStyle(tag3, item3.status)
            ImageLoader.load(img3, item3.imageUrl, R.drawable.ic_item_placeholder, viewLifecycleOwner.lifecycleScope)
            card3.setOnClickListener { openItemDetails(item3) }
            opt3.setOnClickListener { showItemMenu(item3) }
        } else {
            card3.visibility = View.GONE
        }
    }

    /** Applies the correct badge background and text color based on item status. */
    private fun applyBadgeStyle(badge: TextView, status: String) {
        when {
            status.equals("RESOLVED", ignoreCase = true) -> {
                badge.text = "Resolved"
                badge.setBackgroundResource(R.drawable.bg_badge_resolved_green)
                badge.setTextColor(ContextCompat.getColor(requireContext(), R.color.badge_resolved_text))
            }
            status.equals("MATCHED", ignoreCase = true) -> {
                badge.text = "Matched"
                badge.setBackgroundResource(R.drawable.bg_badge_matched)
                badge.setTextColor(ContextCompat.getColor(requireContext(), R.color.badge_matched_text))
            }
            status.equals("FOUND", ignoreCase = true) -> {
                badge.text = "Found"
                badge.setBackgroundResource(R.drawable.bg_badge_found_pill)
                badge.setTextColor(ContextCompat.getColor(requireContext(), R.color.badge_matched_text))
            }
            else -> {
                // LOST or PENDING
                badge.text = if (status.equals("LOST", ignoreCase = true)) "Pending" else status
                badge.setBackgroundResource(R.drawable.bg_badge_pending)
                badge.setTextColor(ContextCompat.getColor(requireContext(), R.color.badge_pending_text))
            }
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
                    1 -> promptResolveItemWithChattedUser(item)
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

    private fun promptResolveItemWithChattedUser(item: ItemReport) {
        val currentUid = FirebaseManager.currentUid
        val convRef = com.google.firebase.database.FirebaseDatabase.getInstance(FirebaseManager.DATABASE_URL)
            .getReference("user_conversations")
            .child(currentUid)

        convRef.addListenerForSingleValueEvent(object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                val chattedUsers = mutableListOf<Pair<String, String>>()
                for (child in snapshot.children) {
                    val relatedId = child.child("relatedItemId").getValue(String::class.java) ?: ""
                    val otherUid = child.child("otherUserId").getValue(String::class.java) ?: ""
                    val otherName = child.child("otherUserName").getValue(String::class.java) ?: "Chat Contact"
                    if (otherUid.isNotEmpty() && otherUid != currentUid) {
                        if (relatedId == item.itemId || relatedId.isEmpty()) {
                            if (!chattedUsers.any { it.first == otherUid }) {
                                chattedUsers.add(Pair(otherUid, otherName))
                            }
                        }
                    }
                }

                if (chattedUsers.isNotEmpty() && isAdded) {
                    val options = mutableListOf<String>()
                    chattedUsers.forEach { user ->
                        options.add("👤 ${user.second} (Award Reunited Hero Achievement)")
                    }
                    options.add("🤝 Resolved offline without chat contact")

                    AlertDialog.Builder(requireContext())
                        .setTitle("Select Who Helped Reunited '${item.title}'")
                        .setItems(options.toTypedArray()) { _, which ->
                            if (which < chattedUsers.size) {
                                val selectedUser = chattedUsers[which]
                                executeResolveReport(item, selectedUser.first, selectedUser.second)
                            } else {
                                executeResolveReport(item, null, null)
                            }
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                } else if (isAdded) {
                    executeResolveReport(item, null, null)
                }
            }

            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                if (isAdded) executeResolveReport(item, null, null)
            }
        })
    }

    private fun executeResolveReport(item: ItemReport, targetUserId: String?, targetUserName: String?) {
        val currentUid = FirebaseManager.currentUid
        val updateData = mutableMapOf<String, Any>(
            "status" to "RESOLVED",
            "resolvedTimestamp" to System.currentTimeMillis()
        )
        if (targetUserId != null && targetUserName != null) {
            updateData["resolvedWithUserId"] = targetUserId
            updateData["resolvedWithUserName"] = targetUserName
            updateData["resolvedByUserId"] = currentUid
        }

        FirebaseManager.itemsRef.child(item.itemId).updateChildren(updateData).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val idx = userReports.indexOfFirst { it.itemId == item.itemId }
                if (idx != -1) {
                    userReports[idx] = userReports[idx].copy(status = "RESOLVED")
                    updateReportsListUI()
                }

                // Award chatted user if chosen
                if (targetUserId != null && targetUserName != null) {
                    val userDbRef = FirebaseManager.usersRef.child(targetUserId)
                    userDbRef.child("itemsReturned").setValue(com.google.firebase.database.ServerValue.increment(1))
                    userDbRef.child("trustScore").setValue(com.google.firebase.database.ServerValue.increment(20))

                    val achievementKey = "reunited_hero_${System.currentTimeMillis()}"
                    val achievement = mapOf(
                        "id" to achievementKey,
                        "title" to "🤝 Reunited Hero",
                        "description" to "Helped resolve '${item.title}' and safely returned to owner!",
                        "timestamp" to System.currentTimeMillis(),
                        "badge" to "HERO_REUNITED",
                        "xpAwarded" to 20
                    )
                    userDbRef.child("achievements").child(achievementKey).setValue(achievement)

                    val notif = mapOf(
                        "id" to "notif_${System.currentTimeMillis()}",
                        "title" to "🏆 Achievement Unlocked: Reunited Hero!",
                        "message" to "You were selected as the hero who helped reunite '${item.title}'. +20 Trust Points awarded!",
                        "timestamp" to System.currentTimeMillis(),
                        "type" to "ACHIEVEMENT",
                        "itemId" to item.itemId,
                        "senderUserId" to currentUid,
                        "read" to false
                    )
                    FirebaseManager.database.getReference("notifications").child(targetUserId).push().setValue(notif)
                }

                // Award reporting user
                if (currentUid.isNotEmpty()) {
                    val reporterRef = FirebaseManager.usersRef.child(currentUid)
                    reporterRef.child("itemsReturned").setValue(com.google.firebase.database.ServerValue.increment(1))
                    reporterRef.child("trustScore").setValue(com.google.firebase.database.ServerValue.increment(15))
                    val reporterAch = mapOf(
                        "id" to "case_resolved_${System.currentTimeMillis()}",
                        "title" to "Honest Finder & Case Closed",
                        "description" to "Successfully resolved '${item.title}' and reunited with owner!",
                        "timestamp" to System.currentTimeMillis()
                    )
                    reporterRef.child("achievements").push().setValue(reporterAch)
                }

                com.group3.lostandfound.service.FCMService.triggerPushNotification(
                    requireContext(),
                    "Item Resolved & Reunited!",
                    "'${item.title}' has been marked as resolved and safely reunited.",
                    item.itemId
                )

                val msg = if (targetUserName != null) {
                    "'${item.title}' marked as RESOLVED with $targetUserName! Achievements awarded."
                } else {
                    "'${item.title}' marked as RESOLVED."
                }
                ModernToast.show(requireContext(), msg, view)
            } else {
                ModernToast.show(requireContext(), "Update failed: ${task.exception?.message}", view, durationLong = true)
            }
        }
    }

    companion object {
        fun newInstance(): ReportsFragment = ReportsFragment()
    }
}
