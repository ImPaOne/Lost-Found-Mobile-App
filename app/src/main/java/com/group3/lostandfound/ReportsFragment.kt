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
        setupBottomNav(view, "reports")
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
    // Observes ViewModel state changes and refreshes the local reports list.
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            reportsViewModel.uiState.collect { state ->
                userReports.clear()
                userReports.addAll(state.reports)
                updateReportsListUI()
            }
        }
    }

    // Filters reports and updates the three report cards and empty-state UI.
    private fun updateReportsListUI() {
        val filtered = when (currentFilter) {
            "ACTIVE" -> userReports.filter { !it.status.equals("RESOLVED", ignoreCase = true) }
            "RESOLVED" -> userReports.filter { it.status.equals("RESOLVED", ignoreCase = true) }
            else -> userReports
        }

        // Updates the report count and shows the empty-state message when no reports match.
        tvCount.text = "${filtered.size} reports · updated live"
        tvEmptyReports.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE

        // Item 1: Displays the first filtered report and configures its click actions.
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

        // Item 2: Displays the second filtered report when available.
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

        // Item 3: Displays the third filtered report when available.
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
        // Selects the badge label and appearance according to the report status.
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
                // LOST or PENDING: Uses the pending badge style for other statuses.
                badge.text = if (status.equals("LOST", ignoreCase = true)) "Pending" else status
                badge.setBackgroundResource(R.drawable.bg_badge_pending)
                badge.setTextColor(ContextCompat.getColor(requireContext(), R.color.badge_pending_text))
            }
        }
    }

    // Registers click listeners for navigation, report creation, and filtering tabs.
    private fun setupListeners() {
        btnBack.setOnClickListener { parentFragmentManager.popBackStack() }

        // Opens the report creation screen and adds the current screen to the back stack.
        btnAdd.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.screenBox, CreateReportFragment.newInstance())
                .addToBackStack(null)
                .commit()
        }

        // Filters the list to show active reports.
        tabAct.setOnClickListener {
            currentFilter = "ACTIVE"
            pickTab(tabAct)
            updateReportsListUI()
        }

        // Filters the list to show resolved reports.
        tabRes.setOnClickListener {
            currentFilter = "RESOLVED"
            pickTab(tabRes)
            updateReportsListUI()
        }

        // Removes the status filter to display all reports.
        tabAll.setOnClickListener {
            currentFilter = "ALL"
            pickTab(tabAll)
            updateReportsListUI()
        }
    }

    // Displays available actions for the selected report.
    private fun showItemMenu(item: ItemReport) {
        val options = arrayOf("View Live Details", "Mark as Resolved", "Delete Report")
        AlertDialog.Builder(requireContext())
            .setTitle(item.title)
            .setItems(options) { _, which ->
                // Performs the action selected from the report menu.
                when (which) {
                    0 -> openItemDetails(item)
                    1 -> promptResolveItemWithChattedUser(item)
                    2 -> {
                        // Requests confirmation before permanently deleting the report.
                        AlertDialog.Builder(requireContext())
                            .setTitle("Delete Listing")
                            .setMessage("Remove '${item.title}' from database?")
                            .setPositiveButton("Delete") { _, _ ->
                                reportsViewModel.deleteItem(item.itemId) { success ->
                                    // Displays confirmation when the deletion succeeds.
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

    // Navigates to the details screen for the selected report.
    private fun openItemDetails(item: ItemReport) {
        parentFragmentManager.beginTransaction()
            // Applies fade animations when opening and closing the details screen.
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
            // Replaces the current fragment with a DetailsFragment populated with the report data.
            .replace(
                R.id.screenBox,
                DetailsFragment.newInstance(
                    itemId = item.itemId,
                    title = item.title,
                    desc = item.description,
                    status = item.status,
                    location = item.location,
                    imageUrl = item.imageUrl,
                    reporterUserId = item.userId,
                    authorName = item.authorName
                )
            )
            .addToBackStack(null)
            .commit()
    }

    // Updates the selected tab's styling and resets the appearance of the other tabs.
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

    // Finds users who have chatted about the report so the administrator can select who helped reunite the item.
    private fun promptResolveItemWithChattedUser(item: ItemReport) {
        val currentUid = FirebaseManager.currentUid
        // References the current user's conversations in the Firebase database.
        val convRef = com.google.firebase.database.FirebaseDatabase.getInstance(FirebaseManager.DATABASE_URL)
            .getReference("user_conversations")
            .child(currentUid)

        // Fetches the conversations once to identify potential helpers.
        convRef.addListenerForSingleValueEvent(object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                // Stores each eligible helper's user ID and display name.
                val chattedUsers = mutableListOf<Pair<String, String>>()
                // Checks each conversation for a matching report and another participant.
                for (child in snapshot.children) {
                    val relatedId = child.child("relatedItemId").getValue(String::class.java) ?: ""
                    val otherUid = child.child("otherUserId").getValue(String::class.java) ?: ""
                    val otherName = child.child("otherUserName").getValue(String::class.java) ?: "Chat Contact"
                    if (otherUid.isNotEmpty() && otherUid != currentUid) {
                        if (relatedId == item.itemId || relatedId.isEmpty()) {
                            // Avoids adding the same user more than once.
                            if (!chattedUsers.any { it.first == otherUid }) {
                                chattedUsers.add(Pair(otherUid, otherName))
                            }
                        }
                    }
                }

                // Displays helper options if eligible chat contacts exist and the fragment is attached.
                if (chattedUsers.isNotEmpty() && isAdded) {
                    val options = mutableListOf<String>()
                    // Adds an option for each potential helper.
                    chattedUsers.forEach { user ->
                        options.add("👤 ${user.second} (Award Reunited Hero Achievement)")
                    }
                    // Allows the report to be resolved without selecting a chat contact.
                    options.add("🤝 Resolved offline without chat contact")

                    AlertDialog.Builder(requireContext())
                        .setTitle("Select Who Helped Reunited '${item.title}'")
                        .setItems(options.toTypedArray()) { _, which ->
                            // Awards the selected helper if one was chosen; otherwise resolves without a helper.
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
                    // Resolves the report without a helper when no eligible chat contacts are found.
                    executeResolveReport(item, null, null)
                }
            }

            // Falls back to resolving the report without a helper if loading conversations fails.
            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                if (isAdded) executeResolveReport(item, null, null)
            }
        })
    }

    // Marks a report as resolved and awards achievements to the selected helper and reporting user.
    private fun executeResolveReport(item: ItemReport, targetUserId: String?, targetUserName: String?) {
        val currentUid = FirebaseManager.currentUid
        // Prepares the database fields required to mark the report as resolved.
        val updateData = mutableMapOf<String, Any>(
            "status" to "RESOLVED",
            "resolvedTimestamp" to System.currentTimeMillis()
        )
        // Records helper details when a helper has been selected.
        if (targetUserId != null && targetUserName != null) {
            updateData["resolvedWithUserId"] = targetUserId
            updateData["resolvedWithUserName"] = targetUserName
            updateData["resolvedByUserId"] = currentUid
        }

        // Updates the report in Firebase and handles the result when the operation completes.
        FirebaseManager.itemsRef.child(item.itemId).updateChildren(updateData).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // Finds the report in the local list and updates its status immediately.
                val idx = userReports.indexOfFirst { it.itemId == item.itemId }
                if (idx != -1) {
                    userReports[idx] = userReports[idx].copy(status = "RESOLVED")
                    updateReportsListUI()
                }

                // Award chatted user if chosen
                if (targetUserId != null && targetUserName != null) {
                    // References the selected helper's Firebase user record.
                    val userDbRef = FirebaseManager.usersRef.child(targetUserId)
                    // Increments the helper's returned-item count and trust score.
                    userDbRef.child("itemsReturned").setValue(com.google.firebase.database.ServerValue.increment(1))
                    userDbRef.child("trustScore").setValue(com.google.firebase.database.ServerValue.increment(20))

                    // Creates a unique ID for the helper's achievement.
                    val achievementKey = "reunited_hero_${System.currentTimeMillis()}"
                    // Defines the achievement details to be saved to Firebase.
                    val achievement = mapOf(
                        "id" to achievementKey,
                        "title" to "🤝 Reunited Hero",
                        "description" to "Helped resolve '${item.title}' and safely returned to owner!",
                        "timestamp" to System.currentTimeMillis(),
                        "badge" to "HERO_REUNITED",
                        "xpAwarded" to 20
                    )
                    // Saves the Reunited Hero achievement to the helper's account.
                    userDbRef.child("achievements").child(achievementKey).setValue(achievement)

                    // Creates a notification informing the helper about the achievement and trust points.
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
                    // Sends the achievement notification to the selected helper's notification records.
                    FirebaseManager.database.getReference("notifications").child(targetUserId).push().setValue(notif)
                }

                // Award reporting user
                if (currentUid.isNotEmpty()) {
                    // References the current user's Firebase record to award report-resolution credit.
                    val reporterRef = FirebaseManager.usersRef.child(currentUid)
                    // Increments the reporting user's returned-item count and trust score.
                    reporterRef.child("itemsReturned").setValue(com.google.firebase.database.ServerValue.increment(1))
                    reporterRef.child("trustScore").setValue(com.google.firebase.database.ServerValue.increment(15))
                    // Defines the achievement details awarded to the reporting user.
                    val reporterAch = mapOf(
                        "id" to "case_resolved_${System.currentTimeMillis()}",
                        "title" to "Honest Finder & Case Closed",
                        "description" to "Successfully resolved '${item.title}' and reunited with owner!",
                        "timestamp" to System.currentTimeMillis()
                    )
                    // Saves the achievement to the reporting user's account.
                    reporterRef.child("achievements").push().setValue(reporterAch)
                }

                // Sends a push notification that the item has been resolved and reunited.
                com.group3.lostandfound.service.FCMService.triggerPushNotification(
                    requireContext(),
                    "Item Resolved & Reunited!",
                    "'${item.title}' has been marked as resolved and safely reunited.",
                    item.itemId
                )

                // Creates a success message based on whether a helper was selected.
                val msg = if (targetUserName != null) {
                    "'${item.title}' marked as RESOLVED with $targetUserName! Achievements awarded."
                } else {
                    "'${item.title}' marked as RESOLVED."
                }
                ModernToast.show(requireContext(), msg, view)
            } else {
                // Displays the database error if the report update fails.
                ModernToast.show(requireContext(), "Update failed: ${task.exception?.message}", view, durationLong = true)
            }
        }
    }

    // Provides a factory method for creating a ReportsFragment instance.
    companion object {
        fun newInstance(): ReportsFragment = ReportsFragment()
    }
}