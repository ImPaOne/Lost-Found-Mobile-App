package com.group3.lostandfound

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

/**
 * ModerationFragment handles live moderation and review of lost & found reports:
 * - Live real-time sync with Firebase Realtime Database (/items)
 * - Lists ALL real community reports dynamically in RecyclerView
 * - Tabbed filtering: Pending Review, Flagged, Approved
 * - Approve reports using FirebaseManager.approveItem()
 * - Reject/remove reports using FirebaseManager.removeItem()
 * - Escalate, Edit, Flag/Unflag, Ban User via moderation options dialog
 * - Bulk selection and batch approval
 * - Logs audit trails to /admin_logs/{actionId}
 * - Admin authorization checks with PERMISSION_DENIED alerts
 */
class ModerationFragment : Fragment() {

    // Header views
    private lateinit var buttonBack: ImageView
    private lateinit var buttonSearch: ImageView

    // Segment tabs
    private lateinit var tabPendingReview: TextView
    private lateinit var tabFlagged: TextView
    private lateinit var tabApproved: TextView

    // Top action buttons
    private lateinit var buttonSelectAll: TextView
    private lateinit var buttonApproveSelected: TextView
    private lateinit var textReportsCountSummary: TextView

    // Dynamic List Views
    private lateinit var pbLoadingModeration: ProgressBar
    private lateinit var layoutEmptyModeration: View
    private lateinit var textEmptyModerationTitle: TextView
    private lateinit var textEmptyModerationSubtitle: TextView
    private lateinit var rvModerationReports: RecyclerView

    private lateinit var adapter: ModerationReportsAdapter
    private val allModerationReports = mutableListOf<ReportItem>()
    private val displayedReports = mutableListOf<ReportItem>()
    private val selectedReportIds = mutableSetOf<String>()

    private var activeTabName = "PENDING"
    private var allSelected = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_moderation, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupRecyclerView()
        setupClickListeners()
        setupBottomNav(view, "moderation")
        observeFirebaseModerationQueue()
    }

    private fun initViews(view: View) {
        buttonBack = view.findViewById(R.id.buttonBack)
        buttonSearch = view.findViewById(R.id.buttonSearch)

        tabPendingReview = view.findViewById(R.id.tabPendingReview)
        tabFlagged = view.findViewById(R.id.tabFlagged)
        tabApproved = view.findViewById(R.id.tabApproved)

        buttonSelectAll = view.findViewById(R.id.buttonSelectAll)
        buttonApproveSelected = view.findViewById(R.id.buttonApproveSelected)
        textReportsCountSummary = view.findViewById(R.id.textReportsCountSummary)

        pbLoadingModeration = view.findViewById(R.id.pbLoadingModeration)
        layoutEmptyModeration = view.findViewById(R.id.layoutEmptyModeration)
        textEmptyModerationTitle = view.findViewById(R.id.textEmptyModerationTitle)
        textEmptyModerationSubtitle = view.findViewById(R.id.textEmptyModerationSubtitle)
        rvModerationReports = view.findViewById(R.id.rvModerationReports)
    }

    private fun setupRecyclerView() {
        adapter = ModerationReportsAdapter(
            reports = displayedReports,
            selectedIds = selectedReportIds,
            onApprove = { report ->
                approveReport(report.itemId, report.title)
            },
            onReject = { report ->
                confirmRemoveReport(report.itemId, report.title)
            },
            onEscalate = { report ->
                showEscalationDialog(report)
            },
            onItemClick = { report ->
                showEscalationDialog(report)
            },
            onSelectToggle = { report, isChecked ->
                if (isChecked) {
                    selectedReportIds.add(report.itemId)
                } else {
                    selectedReportIds.remove(report.itemId)
                }
                updateSelectAllButtonText()
            }
        )

        rvModerationReports.layoutManager = LinearLayoutManager(requireContext())
        rvModerationReports.adapter = adapter
    }

    private fun updateSelectAllButtonText() {
        val allChecked = displayedReports.isNotEmpty() && selectedReportIds.containsAll(displayedReports.map { it.itemId })
        allSelected = allChecked
        buttonSelectAll.text = if (allChecked) "Deselect all" else "Select all"
    }

    /**
     * Realtime Database observation for moderation queue
     */
    private fun observeFirebaseModerationQueue() {
        pbLoadingModeration.visibility = View.VISIBLE

        FirebaseManager.itemsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded) return

                allModerationReports.clear()

                for (child in snapshot.children) {
                    val report = ReportItem.fromSnapshot(child)
                    if (report.itemId.isNotEmpty() || report.title.isNotEmpty()) {
                        allModerationReports.add(report)
                    }
                }

                // Sort newest reports first
                allModerationReports.sortByDescending { it.timestamp }

                pbLoadingModeration.visibility = View.GONE
                updateModerationCardsVisibility()
            }

            override fun onCancelled(error: DatabaseError) {
                if (!isAdded) return
                pbLoadingModeration.visibility = View.GONE
                if (error.code == DatabaseError.PERMISSION_DENIED) {
                    showPermissionDeniedAlert()
                } else {
                    Toast.makeText(requireContext(), "Failed to load moderation queue: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    // Filters reports according to active tab and updates UI
    private fun updateModerationCardsVisibility() {
        val filtered = when (activeTabName) {
            "FLAGGED" -> allModerationReports.filter {
                it.isFlagged || it.status.equals("FLAGGED", ignoreCase = true) || it.flagReason.isNotBlank()
            }
            "APPROVED" -> allModerationReports.filter {
                it.status.equals("APPROVED", ignoreCase = true) || it.status.equals("RESOLVED", ignoreCase = true)
            }
            else -> allModerationReports.filter {
                !it.status.equals("APPROVED", ignoreCase = true) &&
                !it.status.equals("RESOLVED", ignoreCase = true) &&
                !it.status.equals("REJECTED", ignoreCase = true)
            }
        }

        displayedReports.clear()
        displayedReports.addAll(filtered)
        adapter.notifyDataSetChanged()

        // Update Summary Text
        textReportsCountSummary.text = when (activeTabName) {
            "FLAGGED" -> "• ${filtered.size} flagged report${if (filtered.size == 1) "" else "s"} requiring attention"
            "APPROVED" -> "• ${filtered.size} approved & resolved report${if (filtered.size == 1) "" else "s"}"
            else -> "• ${filtered.size} report${if (filtered.size == 1) "" else "s"} awaiting review"
        }

        // Empty state handling
        if (filtered.isEmpty()) {
            layoutEmptyModeration.visibility = View.VISIBLE
            rvModerationReports.visibility = View.GONE

            textEmptyModerationTitle.text = when (activeTabName) {
                "FLAGGED" -> "No flagged reports"
                "APPROVED" -> "No approved reports yet"
                else -> "No reports awaiting review"
            }

            textEmptyModerationSubtitle.text = when (activeTabName) {
                "FLAGGED" -> "No community reports currently have active flags."
                "APPROVED" -> "Reports approved by moderators will appear here."
                else -> "All submitted community reports have been reviewed."
            }
        } else {
            layoutEmptyModeration.visibility = View.GONE
            rvModerationReports.visibility = View.VISIBLE
        }

        updateSelectAllButtonText()
    }

    // Sets up click listeners for navigation, tabs, and bulk actions.
    private fun setupClickListeners() {
        buttonBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        buttonSearch.setOnClickListener {
            com.group3.lostandfound.util.ModernToast.show(requireContext(), "Filtering moderation queue...", view)
        }

        // Tabs
        tabPendingReview.setOnClickListener {
            activeTabName = "PENDING"
            selectTab(tabPendingReview)
            selectedReportIds.clear()
            updateModerationCardsVisibility()
        }

        tabFlagged.setOnClickListener {
            activeTabName = "FLAGGED"
            selectTab(tabFlagged)
            selectedReportIds.clear()
            updateModerationCardsVisibility()
        }

        tabApproved.setOnClickListener {
            activeTabName = "APPROVED"
            selectTab(tabApproved)
            selectedReportIds.clear()
            updateModerationCardsVisibility()
        }

        // Bulk Actions
        buttonSelectAll.setOnClickListener {
            if (displayedReports.isEmpty()) {
                com.group3.lostandfound.util.ModernToast.show(requireContext(), "No reports to select", view)
                return@setOnClickListener
            }

            if (allSelected) {
                selectedReportIds.clear()
                allSelected = false
                buttonSelectAll.text = "Select all"
                com.group3.lostandfound.util.ModernToast.show(requireContext(), "Selection cleared", view)
            } else {
                selectedReportIds.addAll(displayedReports.map { it.itemId })
                allSelected = true
                buttonSelectAll.text = "Deselect all"
                com.group3.lostandfound.util.ModernToast.show(requireContext(), "All ${displayedReports.size} reports selected", view)
            }
            adapter.notifyDataSetChanged()
        }

        buttonApproveSelected.setOnClickListener {
            approveBatchReports()
        }
    }

    /**
     * Approve Report using updateChildren()
     */
    private fun approveReport(itemId: String, title: String) {
        FirebaseManager.approveItem(
            itemId = itemId,
            onSuccess = {
                if (isAdded) {
                    com.group3.lostandfound.util.ModernToast.show(requireContext(), "Approved report: $title", view)
                }
            },
            onError = { error ->
                if (isAdded) {
                    showAuthorizationAlert(error)
                }
            }
        )
    }

    /**
     * Remove / Reject Report using removeValue()
     */
    private fun confirmRemoveReport(itemId: String, title: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Remove Report")
            .setMessage("Are you sure you want to permanently remove '$title' from the database?")
            .setPositiveButton("Remove") { _, _ ->
                FirebaseManager.removeItem(
                    itemId = itemId,
                    onSuccess = {
                        if (isAdded) {
                            com.group3.lostandfound.util.ModernToast.show(requireContext(), "Removed item: $title", view)
                        }
                    },
                    onError = { error ->
                        if (isAdded) {
                            showAuthorizationAlert(error)
                        }
                    }
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Displays an escalation dialog offering Edit Post, Flag Item, Unflag Item, Delete Post, or Ban User.
     */
    private fun showEscalationDialog(report: ReportItem) {
        val actions = arrayOf(
            "View Full Details",
            "Edit Post Details (Fake/Inappropriate)",
            if (report.isFlagged) "Unflag Item" else "Flag Item for Review",
            "Permanently Delete Post",
            "Ban User Account"
        )

        AlertDialog.Builder(requireContext())
            .setTitle("Manage: ${report.title.take(24)}")
            .setItems(actions) { _, which ->
                when (which) {
                    0 -> {
                        // Open DetailsFragment
                        parentFragmentManager.beginTransaction()
                            .replace(
                                R.id.screenBox,
                                DetailsFragment.newInstance(
                                    itemId = report.itemId,
                                    title = report.title,
                                    desc = report.description,
                                    status = report.status,
                                    location = report.location,
                                    imageUrl = report.imageUrl,
                                    reporterUserId = report.userId,
                                    authorName = report.authorName
                                )
                            )
                            .addToBackStack(null)
                            .commit()
                    }
                    1 -> {
                        showEditPostDialog(report)
                    }
                    2 -> {
                        val newFlagState = !report.isFlagged
                        FirebaseManager.flagItem(
                            itemId = report.itemId,
                            isFlagged = newFlagState,
                            onSuccess = {
                                if (isAdded) {
                                    com.group3.lostandfound.util.ModernToast.show(
                                        requireContext(),
                                        if (newFlagState) "'${report.title}' marked as Flagged" else "'${report.title}' unflagged",
                                        view
                                    )
                                }
                            },
                            onError = { showAuthorizationAlert(it) }
                        )
                    }
                    3 -> {
                        confirmRemoveReport(report.itemId, report.title)
                    }
                    4 -> {
                        confirmBanUser(report.userId)
                    }
                }
            }
            .setNegativeButton("Close", null)
            .show()
    }

    /**
     * Dialog to edit post details if information is misleading or inappropriate
     */
    private fun showEditPostDialog(report: ReportItem) {
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (20 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad, pad, pad)
        }

        val editTitle = EditText(requireContext()).apply {
            hint = "Item Title"
            setText(report.title)
        }
        val editCategory = EditText(requireContext()).apply {
            hint = "Category (Electronics, Documents, etc.)"
            setText(report.category)
        }
        val editLocation = EditText(requireContext()).apply {
            hint = "Location"
            setText(report.location)
        }
        val editDescription = EditText(requireContext()).apply {
            hint = "Description"
            setText(report.description)
        }
        val editStatus = EditText(requireContext()).apply {
            hint = "Status (PENDING, APPROVED, RESOLVED, LOST, FOUND)"
            setText(report.status)
        }

        val tvHeader = TextView(requireContext()).apply {
            text = "Edit Post Information"
            textSize = 15f
            setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
            setPadding(0, 0, 0, (8 * resources.displayMetrics.density).toInt())
        }

        layout.addView(tvHeader)
        layout.addView(editTitle)
        layout.addView(editCategory)
        layout.addView(editLocation)
        layout.addView(editDescription)
        layout.addView(editStatus)

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Post: ${report.itemId.take(16)}")
            .setView(layout)
            .setPositiveButton("Save Changes") { _, _ ->
                val newTitle = editTitle.text.toString().trim()
                val newCat = editCategory.text.toString().trim()
                val newLoc = editLocation.text.toString().trim()
                val newDesc = editDescription.text.toString().trim()
                val newStat = editStatus.text.toString().trim().uppercase()

                if (newTitle.isNotEmpty()) {
                    FirebaseManager.editItem(
                        itemId = report.itemId,
                        title = newTitle,
                        category = newCat,
                        location = newLoc,
                        description = newDesc,
                        status = newStat,
                        onSuccess = {
                            if (isAdded) {
                                com.group3.lostandfound.util.ModernToast.show(requireContext(), "Report updated successfully", view)
                            }
                        },
                        onError = { showAuthorizationAlert(it) }
                    )
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Confirms the user ban and sets /users/{uid}/isBanned = true through FirebaseManager.
     */
    private fun confirmBanUser(targetUid: String) {
        if (targetUid.isBlank()) {
            Toast.makeText(requireContext(), "No author ID associated with this report.", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Ban User Account")
            .setMessage("Ban user '$targetUid' from creating new reports or contacting members?")
            .setPositiveButton("Ban User") { _, _ ->
                FirebaseManager.banUser(
                    targetUid = targetUid,
                    isBanned = true,
                    onSuccess = {
                        if (isAdded) {
                            com.group3.lostandfound.util.ModernToast.show(requireContext(), "User '$targetUid' has been banned", view, durationLong = true)
                        }
                    },
                    onError = { showAuthorizationAlert(it) }
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Approve batch reports
     */
    private fun approveBatchReports() {
        val targets = if (selectedReportIds.isNotEmpty()) {
            displayedReports.filter { selectedReportIds.contains(it.itemId) }
        } else {
            displayedReports.filter { !it.status.equals("APPROVED", true) && !it.status.equals("RESOLVED", true) }
        }

        if (targets.isEmpty()) {
            com.group3.lostandfound.util.ModernToast.show(requireContext(), "No pending reports selected to approve", view)
            return
        }

        var count = 0
        targets.forEach { report ->
            if (report.itemId.isNotEmpty()) {
                FirebaseManager.approveItem(report.itemId, {
                    count++
                }, {})
            }
        }

        selectedReportIds.clear()
        allSelected = false
        buttonSelectAll.text = "Select all"
        com.group3.lostandfound.util.ModernToast.show(requireContext(), "Approved ${targets.size} reports", view)
    }

    /**
     * Display authorization alert on DatabaseError.PERMISSION_DENIED or failed checks
     */
    private fun showAuthorizationAlert(error: String) {
        if (!isAdded) return
        AlertDialog.Builder(requireContext())
            .setTitle("Authorization Alert")
            .setMessage("Action Failed: $error\nEnsure your account role is set to 'ADMIN'.")
            .setPositiveButton("Dismiss", null)
            .show()
    }

    private fun showPermissionDeniedAlert() {
        if (!isAdded) return
        AlertDialog.Builder(requireContext())
            .setTitle("Permission Denied")
            .setMessage("Firebase DatabaseError.PERMISSION_DENIED: Access restricted to administrators.")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun selectTab(selectedTab: TextView) {
        val allTabs = listOf(tabPendingReview, tabFlagged, tabApproved)
        for (tab in allTabs) {
            if (tab == selectedTab) {
                tab.setBackgroundResource(R.drawable.bg_segment_active)
                tab.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            } else {
                tab.background = null
                tab.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
            }
        }
    }

    /**
     * Dynamic Moderation Reports Adapter for RecyclerView
     */
    class ModerationReportsAdapter(
        private val reports: List<ReportItem>,
        private val selectedIds: MutableSet<String>,
        private val onApprove: (ReportItem) -> Unit,
        private val onReject: (ReportItem) -> Unit,
        private val onEscalate: (ReportItem) -> Unit,
        private val onItemClick: (ReportItem) -> Unit,
        private val onSelectToggle: (ReportItem, Boolean) -> Unit
    ) : RecyclerView.Adapter<ModerationReportsAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val cardReport: View = view.findViewById(R.id.cardReport)
            val cbSelectReport: CheckBox = view.findViewById(R.id.cbSelectReport)
            val textReportTitle: TextView = view.findViewById(R.id.textReportTitle)
            val badgeReportStatus: TextView = view.findViewById(R.id.badgeReportStatus)
            val textReportAuthorLocation: TextView = view.findViewById(R.id.textReportAuthorLocation)
            val bannerReportWarning: View = view.findViewById(R.id.bannerReportWarning)
            val textReportWarningNote: TextView = view.findViewById(R.id.textReportWarningNote)
            val buttonReportApprove: TextView = view.findViewById(R.id.buttonReportApprove)
            val buttonReportReject: TextView = view.findViewById(R.id.buttonReportReject)
            val buttonReportEscalate: TextView = view.findViewById(R.id.buttonReportEscalate)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_moderation_report_card, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val report = reports[position]
            val context = holder.itemView.context

            holder.textReportTitle.text = report.title.ifEmpty { "Community Item Report" }

            val author = report.authorName.ifEmpty { "Community Member" }
            val location = report.location.ifEmpty { "Colombo" }
            val category = report.category.ifEmpty { "General" }
            holder.textReportAuthorLocation.text = "$author · $location · $category"

            // Status Badge Appearance
            val isApproved = report.status.equals("APPROVED", ignoreCase = true) || report.status.equals("RESOLVED", ignoreCase = true)
            val isFlagged = report.isFlagged || report.status.equals("FLAGGED", ignoreCase = true) || report.flagReason.isNotBlank()

            when {
                isApproved -> {
                    holder.badgeReportStatus.text = "Approved"
                    holder.badgeReportStatus.setBackgroundResource(R.drawable.bg_badge_resolved_green)
                    holder.badgeReportStatus.setTextColor(ContextCompat.getColor(context, R.color.white))
                }
                isFlagged -> {
                    holder.badgeReportStatus.text = "Flagged"
                    holder.badgeReportStatus.setBackgroundResource(R.drawable.bg_badge_flagged)
                    holder.badgeReportStatus.setTextColor(ContextCompat.getColor(context, R.color.badge_flagged_text))
                }
                else -> {
                    holder.badgeReportStatus.text = "Pending"
                    holder.badgeReportStatus.setBackgroundResource(R.drawable.bg_badge_review_due)
                    holder.badgeReportStatus.setTextColor(0xFFB45309.toInt())
                }
            }

            // Warning Alert Banner
            if (isFlagged) {
                holder.bannerReportWarning.visibility = View.VISIBLE
                val reason = report.flagReason.ifEmpty { "Flagged for administrative review" }
                holder.textReportWarningNote.text = reason
            } else {
                holder.bannerReportWarning.visibility = View.GONE
            }

            // Checkbox selection
            holder.cbSelectReport.setOnCheckedChangeListener(null)
            holder.cbSelectReport.isChecked = selectedIds.contains(report.itemId)
            holder.cbSelectReport.setOnCheckedChangeListener { _, isChecked ->
                onSelectToggle(report, isChecked)
            }

            // Action Buttons
            if (isApproved) {
                holder.buttonReportApprove.text = "Approved ✓"
                holder.buttonReportApprove.alpha = 0.65f
                holder.buttonReportApprove.setOnClickListener {
                    onApprove(report)
                }
            } else {
                holder.buttonReportApprove.text = "Approve"
                holder.buttonReportApprove.alpha = 1.0f
                holder.buttonReportApprove.setOnClickListener {
                    onApprove(report)
                }
            }

            holder.buttonReportReject.setOnClickListener {
                onReject(report)
            }

            holder.buttonReportEscalate.setOnClickListener {
                onEscalate(report)
            }

            holder.cardReport.setOnClickListener {
                onItemClick(report)
            }
        }

        override fun getItemCount(): Int = reports.size
    }

    companion object {
        fun newInstance(): ModerationFragment = ModerationFragment()
    }
}
