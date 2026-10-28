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
import com.google.firebase.database.ValueEventListener

/**
 * ModerationFragment handles moderation and review of lost & found reports:
 * - Live real-time sync with Firebase Realtime Database
 * - Flag items using updateChildren()
 * - Remove items using removeValue()
 * - Ban reported users via /users/{uid}/isBanned = true
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

    // Report Cards
    private lateinit var cardReportPassport: View
    private lateinit var cardReportIPhone: View
    private lateinit var cardReportBracelet: View

    // Passport Card buttons
    private lateinit var buttonPassportApprove: TextView
    private lateinit var buttonPassportReject: TextView
    private lateinit var buttonPassportEscalate: TextView

    // iPhone Card buttons
    private lateinit var buttonIPhoneApprove: TextView
    private lateinit var buttonIPhoneReject: TextView
    private lateinit var buttonIPhoneEscalate: TextView

    // Bracelet Card buttons
    private lateinit var buttonBraceletApprove: TextView
    private lateinit var buttonBraceletReject: TextView
    private lateinit var buttonBraceletEscalate: TextView

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

        cardReportPassport = view.findViewById(R.id.cardReportPassport)
        cardReportIPhone = view.findViewById(R.id.cardReportIPhone)
        cardReportBracelet = view.findViewById(R.id.cardReportBracelet)

        buttonPassportApprove = view.findViewById(R.id.buttonPassportApprove)
        buttonPassportReject = view.findViewById(R.id.buttonPassportReject)
        buttonPassportEscalate = view.findViewById(R.id.buttonPassportEscalate)

        buttonIPhoneApprove = view.findViewById(R.id.buttonIPhoneApprove)
        buttonIPhoneReject = view.findViewById(R.id.buttonIPhoneReject)
        buttonIPhoneEscalate = view.findViewById(R.id.buttonIPhoneEscalate)

        buttonBraceletApprove = view.findViewById(R.id.buttonBraceletApprove)
        buttonBraceletReject = view.findViewById(R.id.buttonBraceletReject)
        buttonBraceletEscalate = view.findViewById(R.id.buttonBraceletEscalate)
    }

    /**
     * Realtime Database observation for moderation queue
     */
    private val allModerationReports = mutableListOf<ReportItem>()

    private fun observeFirebaseModerationQueue() {
        FirebaseManager.itemsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                allModerationReports.clear()
                var pendingCount = 0
                for (child in snapshot.children) {
                    val report = ReportItem.fromSnapshot(child)
                    if (report.itemId.isNotEmpty() || report.title.isNotEmpty()) {
                        allModerationReports.add(report)
                        if (report.status.equals("PENDING", ignoreCase = true)) {
                            pendingCount++
                        }
                    }
                }
                textReportsCountSummary.text = "• $pendingCount reports awaiting review"
                updateModerationCardsVisibility()
            }

            override fun onCancelled(error: DatabaseError) {
                if (error.code == DatabaseError.PERMISSION_DENIED) {
                    showPermissionDeniedAlert()
                }
            }
        })
    }

    private fun updateModerationCardsVisibility() {
        val filtered = when (activeTabName) {
            "PENDING" -> allModerationReports.filter {
                it.status.equals("PENDING", ignoreCase = true) || it.status.equals("LOST", ignoreCase = true) || it.status.equals("FOUND", ignoreCase = true)
            }
            "FLAGGED" -> allModerationReports.filter { it.isFlagged }
            "APPROVED" -> allModerationReports.filter {
                it.status.equals("APPROVED", ignoreCase = true) || it.status.equals("RESOLVED", ignoreCase = true)
            }
            else -> allModerationReports
        }

        // Card 1
        if (filtered.isNotEmpty()) {
            val item1 = filtered[0]
            cardReportPassport.visibility = View.VISIBLE
            cardReportPassport.findViewById<TextView>(R.id.textPassportTitle)?.text = item1.title
            cardReportPassport.findViewById<TextView>(R.id.textPassportAuthorLocation)?.text =
                "${item1.authorName.ifEmpty { "Community Member" }} · ${item1.location.ifEmpty { "Colombo" }}"
            cardReportPassport.findViewById<TextView>(R.id.textPassportWarningNote)?.text =
                if (item1.isFlagged) item1.flagReason.ifEmpty { "Flagged for administrative review" } else "Community report awaiting moderation"

            buttonPassportApprove.setOnClickListener { approveReport(item1.itemId, item1.title) }
            buttonPassportReject.setOnClickListener { confirmRemoveReport(item1.itemId, item1.title) }
            buttonPassportEscalate.setOnClickListener { showEscalationDialog(item1) }
            cardReportPassport.setOnClickListener { showEscalationDialog(item1) }
        } else {
            cardReportPassport.visibility = View.GONE
        }

        // Card 2
        if (filtered.size > 1) {
            val item2 = filtered[1]
            cardReportIPhone.visibility = View.VISIBLE
            cardReportIPhone.findViewById<TextView>(R.id.textIPhoneTitle)?.text = item2.title
            cardReportIPhone.findViewById<TextView>(R.id.textIPhoneAuthorLocation)?.text =
                "${item2.authorName.ifEmpty { "Community Member" }} · ${item2.location.ifEmpty { "Colombo" }}"
            cardReportIPhone.findViewById<TextView>(R.id.textIPhoneWarningNote)?.text =
                if (item2.isFlagged) item2.flagReason.ifEmpty { "Flagged for administrative review" } else "Community report awaiting moderation"

            buttonIPhoneApprove.setOnClickListener { approveReport(item2.itemId, item2.title) }
            buttonIPhoneReject.setOnClickListener { confirmRemoveReport(item2.itemId, item2.title) }
            buttonIPhoneEscalate.setOnClickListener { showEscalationDialog(item2) }
            cardReportIPhone.setOnClickListener { showEscalationDialog(item2) }
        } else {
            cardReportIPhone.visibility = View.GONE
        }

        // Card 3
        if (filtered.size > 2) {
            val item3 = filtered[2]
            cardReportBracelet.visibility = View.VISIBLE
            cardReportBracelet.findViewById<TextView>(R.id.textBraceletTitle)?.text = item3.title
            cardReportBracelet.findViewById<TextView>(R.id.textBraceletAuthorLocation)?.text =
                "${item3.authorName.ifEmpty { "Community Member" }} · ${item3.location.ifEmpty { "Colombo" }}"
            cardReportBracelet.findViewById<TextView>(R.id.textBraceletWarningNote)?.text =
                if (item3.isFlagged) item3.flagReason.ifEmpty { "Flagged for administrative review" } else "Community report awaiting moderation"

            buttonBraceletApprove.setOnClickListener { approveReport(item3.itemId, item3.title) }
            buttonBraceletReject.setOnClickListener { confirmRemoveReport(item3.itemId, item3.title) }
            buttonBraceletEscalate.setOnClickListener { showEscalationDialog(item3) }
            cardReportBracelet.setOnClickListener { showEscalationDialog(item3) }
        } else {
            cardReportBracelet.visibility = View.GONE
        }
    }

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
            updateModerationCardsVisibility()
        }
        tabFlagged.setOnClickListener {
            activeTabName = "FLAGGED"
            selectTab(tabFlagged)
            updateModerationCardsVisibility()
        }
        tabApproved.setOnClickListener {
            activeTabName = "APPROVED"
            selectTab(tabApproved)
            updateModerationCardsVisibility()
        }

        // Bulk Actions
        buttonSelectAll.setOnClickListener {
            allSelected = !allSelected
            buttonSelectAll.text = if (allSelected) "Deselect all" else "Select all"
            com.group3.lostandfound.util.ModernToast.show(requireContext(), if (allSelected) "All reports selected" else "Selection cleared", view)
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
                com.group3.lostandfound.util.ModernToast.show(requireContext(), "Approved report: $title", view)
            },
            onError = { error ->
                showAuthorizationAlert(error)
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
                        com.group3.lostandfound.util.ModernToast.show(requireContext(), "Removed item: $title", view)
                    },
                    onError = { error ->
                        showAuthorizationAlert(error)
                    }
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Escalation dialog offering to Edit Post, Flag Item, Delete Post, or Ban User
     * (Admin Functional Requirements 3.2 #3 and #4)
     */
    private fun showEscalationDialog(report: ReportItem) {
        val actions = arrayOf(
            "Edit Post Details (Fake/Inappropriate)",
            "Flag Item for Review",
            "Unflag Item",
            "Permanently Delete Post",
            "Ban User Account"
        )
        AlertDialog.Builder(requireContext())
            .setTitle("Manage Report: ${report.title.take(24)}")
            .setItems(actions) { _, which ->
                when (which) {
                    0 -> {
                        // Admin Requirement 3.2 #3: Edit post
                        showEditPostDialog(report)
                    }
                    1 -> {
                        // Flag item via updateChildren()
                        FirebaseManager.flagItem(
                            itemId = report.itemId,
                            isFlagged = true,
                            onSuccess = {
                                com.group3.lostandfound.util.ModernToast.show(requireContext(), "'${report.title}' marked as Flagged", view)
                            },
                            onError = { showAuthorizationAlert(it) }
                        )
                    }
                    2 -> {
                        // Unflag item
                        FirebaseManager.flagItem(
                            itemId = report.itemId,
                            isFlagged = false,
                            onSuccess = {
                                com.group3.lostandfound.util.ModernToast.show(requireContext(), "'${report.title}' unflagged", view)
                            },
                            onError = { showAuthorizationAlert(it) }
                        )
                    }
                    3 -> {
                        // Admin Requirement 3.2 #3: Delete post
                        confirmRemoveReport(report.itemId, report.title)
                    }
                    4 -> {
                        // Admin Requirement 3.2 #4: Ban user
                        confirmBanUser(report.userId)
                    }
                }
            }
            .setNegativeButton("Close", null)
            .show()
    }

    /**
     * Edit Post Dialog allowing admin to modify title, category, location, description, or status
     * (Admin Functional Requirement 3.2 #3)
     */
    private fun showEditPostDialog(report: ReportItem) {
        val layout = android.widget.LinearLayout(requireContext()).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            val pad = (16 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad, pad, pad)
        }

        val editTitle = android.widget.EditText(requireContext()).apply {
            hint = "Report Title"
            setText(report.title)
        }
        val editCategory = android.widget.EditText(requireContext()).apply {
            hint = "Category (e.g. Electronics, Bags)"
            setText(report.category)
        }
        val editLocation = android.widget.EditText(requireContext()).apply {
            hint = "Location"
            setText(report.location)
        }
        val editDescription = android.widget.EditText(requireContext()).apply {
            hint = "Description"
            setText(report.description)
            minLines = 2
        }
        val editStatus = android.widget.EditText(requireContext()).apply {
            hint = "Status (PENDING, APPROVED, RESOLVED, LOST, FOUND)"
            setText(report.status)
        }

        val tvHeader = android.widget.TextView(requireContext()).apply {
            text = "Edit Post Details (Administrator Mode)"
            textSize = 14f
            setTypeface(null, android.graphics.Typeface.BOLD)
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
            .setTitle("Edit Post: ${report.itemId}")
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
                            com.group3.lostandfound.util.ModernToast.show(requireContext(), "Report updated successfully", view)
                        },
                        onError = { showAuthorizationAlert(it) }
                    )
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Ban User action: sets /users/{uid}/isBanned = true
     */
    private fun confirmBanUser(targetUid: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Ban User Account")
            .setMessage("Ban user '$targetUid' from creating new reports or contacting members?")
            .setPositiveButton("Ban User") { _, _ ->
                FirebaseManager.banUser(
                    targetUid = targetUid,
                    isBanned = true,
                    onSuccess = {
                        com.group3.lostandfound.util.ModernToast.show(requireContext(), "User '$targetUid' has been banned", view, durationLong = true)
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
        allModerationReports.forEach { report ->
            if (report.itemId.isNotEmpty()) {
                FirebaseManager.approveItem(report.itemId, {}, {})
            }
        }
        com.group3.lostandfound.util.ModernToast.show(requireContext(), "All active reports approved", view)
    }

    /**
     * Display authorization alert on DatabaseError.PERMISSION_DENIED or failed checks
     */
    private fun showAuthorizationAlert(error: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Authorization Alert")
            .setMessage("Action Failed: $error\nEnsure your account role is set to 'ADMIN'.")
            .setPositiveButton("Dismiss", null)
            .show()
    }

    private fun showPermissionDeniedAlert() {
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

    companion object {
        fun newInstance(): ModerationFragment = ModerationFragment()
    }
}
