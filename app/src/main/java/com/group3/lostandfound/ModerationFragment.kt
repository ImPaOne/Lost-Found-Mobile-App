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
     */    // Stores all moderation reports retrieved from Firebase.
    private val allModerationReports = mutableListOf<ReportItem>()

    // Listens for changes to the Firebase moderation queue in real time.
    private fun observeFirebaseModerationQueue() {
        FirebaseManager.itemsRef.addValueEventListener(object : ValueEventListener {
            // Executes whenever Firebase data is initially loaded or updated.
            override fun onDataChange(snapshot: DataSnapshot) {
                // Clears the previous reports before loading the latest data.
                allModerationReports.clear()

                // Keeps track of the number of reports awaiting moderation.
                var pendingCount = 0

                // Iterates through each report stored in the Firebase snapshot.
                for (child in snapshot.children) {
                    // Converts the current Firebase child into a ReportItem object.
                    val report = ReportItem.fromSnapshot(child)

                    // Adds the report if it contains an item ID or a title.
                    if (report.itemId.isNotEmpty() || report.title.isNotEmpty()) {
                        allModerationReports.add(report)

                        // Increases the count if the report status is PENDING.
                        if (report.status.equals("PENDING", ignoreCase = true)) {
                            pendingCount++
                        }
                    }
                }

                // Displays the total number of reports awaiting review.
                textReportsCountSummary.text = "• $pendingCount reports awaiting review"

                // Refreshes the moderation cards using the latest report data.
                updateModerationCardsVisibility()
            }

            // Executes if Firebase fails to retrieve the moderation queue.
            override fun onCancelled(error: DatabaseError) {
                // Displays an alert if access is denied by Firebase security rules.
                if (error.code == DatabaseError.PERMISSION_DENIED) {
                    showPermissionDeniedAlert()
                }
            }
        })
    }

    // Filters the reports and updates the visibility and content of moderation cards.
    private fun updateModerationCardsVisibility() {
        // Selects reports based on the currently active moderation tab.
        val filtered = when (activeTabName) {
            // Shows pending, lost, and found reports in the pending review tab.
            "PENDING" -> allModerationReports.filter {
                it.status.equals("PENDING", ignoreCase = true) || it.status.equals("LOST", ignoreCase = true) || it.status.equals("FOUND", ignoreCase = true)
            }
            // Shows only reports that have been flagged for review.
            "FLAGGED" -> allModerationReports.filter { it.isFlagged }
            // Shows reports that have been approved or resolved.
            "APPROVED" -> allModerationReports.filter {
                it.status.equals("APPROVED", ignoreCase = true) || it.status.equals("RESOLVED", ignoreCase = true)
            }
            // Returns all reports if no specific tab matches.
            else -> allModerationReports
        }

        // Card 1: Displays the first report in the filtered list.
        if (filtered.isNotEmpty()) {
            // Retrieves the first report.
            val item1 = filtered[0]

            // Makes the first moderation card visible.
            cardReportPassport.visibility = View.VISIBLE

            // Displays the report title.
            cardReportPassport.findViewById<TextView>(R.id.textPassportTitle)?.text = item1.title

            // Displays the author's name and location, using fallback values if empty.
            cardReportPassport.findViewById<TextView>(R.id.textPassportAuthorLocation)?.text =
                "${item1.authorName.ifEmpty { "Community Member" }} · ${item1.location.ifEmpty { "Colombo" }}"

            // Displays the flag reason if flagged; otherwise, shows the default moderation message.
            cardReportPassport.findViewById<TextView>(R.id.textPassportWarningNote)?.text =
                if (item1.isFlagged) item1.flagReason.ifEmpty { "Flagged for administrative review" } else "Community report awaiting moderation"

            // Approves the report when the approve button is clicked.
            buttonPassportApprove.setOnClickListener { approveReport(item1.itemId, item1.title) }

            // Requests confirmation before removing the report when the reject button is clicked.
            buttonPassportReject.setOnClickListener { confirmRemoveReport(item1.itemId, item1.title) }

            // Opens the escalation dialog when the escalate button is clicked.
            buttonPassportEscalate.setOnClickListener { showEscalationDialog(item1) }

            // Opens the escalation dialog when the report card is clicked.
            cardReportPassport.setOnClickListener { showEscalationDialog(item1) }
        } else {
            // Hides the first card when no reports are available.
            cardReportPassport.visibility = View.GONE
        }

        // Card 2: Displays the second report in the filtered list.
        if (filtered.size > 1) {
            // Retrieves the second report.
            val item2 = filtered[1]

            // Makes the second moderation card visible.
            cardReportIPhone.visibility = View.VISIBLE

            // Displays the report title.
            cardReportIPhone.findViewById<TextView>(R.id.textIPhoneTitle)?.text = item2.title

            // Displays the author's name and location, using fallback values if empty.
            cardReportIPhone.findViewById<TextView>(R.id.textIPhoneAuthorLocation)?.text =
                "${item2.authorName.ifEmpty { "Community Member" }} · ${item2.location.ifEmpty { "Colombo" }}"

            // Displays the flag reason if flagged; otherwise, shows the default moderation message.
            cardReportIPhone.findViewById<TextView>(R.id.textIPhoneWarningNote)?.text =
                if (item2.isFlagged) item2.flagReason.ifEmpty { "Flagged for administrative review" } else "Community report awaiting moderation"

            // Approves the report when the approve button is clicked.
            buttonIPhoneApprove.setOnClickListener { approveReport(item2.itemId, item2.title) }

            // Requests confirmation before removing the report when the reject button is clicked.
            buttonIPhoneReject.setOnClickListener { confirmRemoveReport(item2.itemId, item2.title) }

            // Opens the escalation dialog when the escalate button is clicked.
            buttonIPhoneEscalate.setOnClickListener { showEscalationDialog(item2) }

            // Opens the escalation dialog when the report card is clicked.
            cardReportIPhone.setOnClickListener { showEscalationDialog(item2) }
        } else {
            // Hides the second card when fewer than two reports are available.
            cardReportIPhone.visibility = View.GONE
        }

        // Card 3: Displays the third report in the filtered list.
        if (filtered.size > 2) {
            // Retrieves the third report.
            val item3 = filtered[2]

            // Makes the third moderation card visible.
            cardReportBracelet.visibility = View.VISIBLE

            // Displays the report title.
            cardReportBracelet.findViewById<TextView>(R.id.textBraceletTitle)?.text = item3.title

            // Displays the author's name and location, using fallback values if empty.
            cardReportBracelet.findViewById<TextView>(R.id.textBraceletAuthorLocation)?.text =
                "${item3.authorName.ifEmpty { "Community Member" }} · ${item3.location.ifEmpty { "Colombo" }}"

            // Displays the flag reason if flagged; otherwise, shows the default moderation message.
            cardReportBracelet.findViewById<TextView>(R.id.textBraceletWarningNote)?.text =
                if (item3.isFlagged) item3.flagReason.ifEmpty { "Flagged for administrative review" } else "Community report awaiting moderation"

            // Approves the report when the approve button is clicked.
            buttonBraceletApprove.setOnClickListener { approveReport(item3.itemId, item3.title) }

            // Requests confirmation before removing the report when the reject button is clicked.
            buttonBraceletReject.setOnClickListener { confirmRemoveReport(item3.itemId, item3.title) }

            // Opens the escalation dialog when the escalate button is clicked.
            buttonBraceletEscalate.setOnClickListener { showEscalationDialog(item3) }

            // Opens the escalation dialog when the report card is clicked.
            cardReportBracelet.setOnClickListener { showEscalationDialog(item3) }
        } else {
            // Hides the third card when fewer than three reports are available.
            cardReportBracelet.visibility = View.GONE
        }
    }

    // Sets up click listeners for navigation, search, tabs, and bulk actions.
    private fun setupClickListeners() {
        // Returns to the previous fragment when the back button is clicked.
        buttonBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Shows a message when the search button is clicked.
        buttonSearch.setOnClickListener {
            com.group3.lostandfound.util.ModernToast.show(requireContext(), "Filtering moderation queue...", view)
        }

        // Tabs: Handles switching between moderation categories.

        // Activates the pending review tab and refreshes the displayed cards.
        tabPendingReview.setOnClickListener {
            activeTabName = "PENDING"
            selectTab(tabPendingReview)
            updateModerationCardsVisibility()
        }

        // Activates the flagged reports tab and refreshes the displayed cards.
        tabFlagged.setOnClickListener {
            activeTabName = "FLAGGED"
            selectTab(tabFlagged)
            updateModerationCardsVisibility()
        }

        // Activates the approved reports tab and refreshes the displayed cards.
        tabApproved.setOnClickListener {
            activeTabName = "APPROVED"
            selectTab(tabApproved)
            updateModerationCardsVisibility()
        }

        // Bulk Actions: Handles selecting reports for batch operations.

        // Toggles between selecting all reports and clearing the selection.
        buttonSelectAll.setOnClickListener {
            allSelected = !allSelected

            // Updates the button label to reflect the current selection state.
            buttonSelectAll.text = if (allSelected) "Deselect all" else "Select all"

            // Displays feedback indicating whether reports are selected or cleared.
            com.group3.lostandfound.util.ModernToast.show(requireContext(), if (allSelected) "All reports selected" else "Selection cleared", view)
        }

        // Approves the reports selected for a bulk action.
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
     */    // Displays a confirmation dialog before permanently removing a report.
    private fun confirmRemoveReport(itemId: String, title: String) {
        // Creates an alert dialog using the current fragment context.
        AlertDialog.Builder(requireContext())
            // Sets the dialog title.
            .setTitle("Remove Report")
            // Asks the administrator to confirm the removal of the report.
            .setMessage("Are you sure you want to permanently remove '$title' from the database?")
            // Executes the removal operation when the Remove button is clicked.
            .setPositiveButton("Remove") { _, _ ->
                // Requests FirebaseManager to remove the report using its item ID.
                FirebaseManager.removeItem(
                    itemId = itemId,
                    // Displays a success message after the report is removed successfully.
                    onSuccess = {
                        com.group3.lostandfound.util.ModernToast.show(requireContext(), "Removed item: $title", view)
                    },
                    // Displays an authorization alert if the removal operation fails.
                    onError = { error ->
                        showAuthorizationAlert(error)
                    }
                )
            }
            // Closes the dialog without removing the report.
            .setNegativeButton("Cancel", null)
            // Displays the confirmation dialog.
            .show()
    }

    /**
     * Displays an escalation dialog offering Edit Post, Flag Item, Unflag Item, Delete Post, or Ban User.
     * Supports Admin Functional Requirements 3.2 #3 and #4.
     */
    private fun showEscalationDialog(report: ReportItem) {
        // Defines the administrative actions available for the selected report.
        val actions = arrayOf(
            "Edit Post Details (Fake/Inappropriate)",
            "Flag Item for Review",
            "Unflag Item",
            "Permanently Delete Post",
            "Ban User Account"
        )
        // Creates a dialog for managing the selected report.
        AlertDialog.Builder(requireContext())
            // Displays the report title, limited to 24 characters.
            .setTitle("Manage Report: ${report.title.take(24)}")
            // Displays the available actions and handles the selected option.
            .setItems(actions) { _, which ->
                // Executes the action corresponding to the selected array index.
                when (which) {
                    0 -> {
                        // Admin Requirement 3.2 #3: Opens the dialog for editing post details.
                        showEditPostDialog(report)
                    }
                    1 -> {
                        // Flags the report for administrative review in Firebase.
                        FirebaseManager.flagItem(
                            itemId = report.itemId,
                            isFlagged = true,
                            // Displays a success message after the report is flagged.
                            onSuccess = {
                                com.group3.lostandfound.util.ModernToast.show(requireContext(), "'${report.title}' marked as Flagged", view)
                            },
                            // Displays an authorization alert if the operation fails.
                            onError = { showAuthorizationAlert(it) }
                        )
                    }
                    2 -> {
                        // Removes the flagged state from the report in Firebase.
                        FirebaseManager.flagItem(
                            itemId = report.itemId,
                            isFlagged = false,
                            // Displays a success message after the report is unflagged.
                            onSuccess = {
                                com.group3.lostandfound.util.ModernToast.show(requireContext(), "'${report.title}' unflagged", view)
                            },
                            // Displays an authorization alert if the operation fails.
                            onError = { showAuthorizationAlert(it) }
                        )
                    }
                    3 -> {
                        // Admin Requirement 3.2 #3: Requests confirmation before deleting the post.
                        confirmRemoveReport(report.itemId, report.title)
                    }
                    4 -> {
                        // Admin Requirement 3.2 #4: Requests confirmation before banning the report owner's account.
                        confirmBanUser(report.userId)
                    }
                }
            }
            // Adds a Close button that dismisses the dialog without performing an action.
            .setNegativeButton("Close", null)
            // Displays the escalation dialog.
            .show()
    }

    /**
     * Creates an edit dialog that allows an administrator to modify the post title, category,
     * location, description, or status.
     * Supports Admin Functional Requirement 3.2 #3.
     */
    private fun showEditPostDialog(report: ReportItem) {
        // Creates a vertical LinearLayout to hold the header and input fields.
        val layout = android.widget.LinearLayout(requireContext()).apply {
            // Arranges the child views vertically.
            orientation = android.widget.LinearLayout.VERTICAL
            // Converts 16 density-independent pixels into physical pixels for padding.
            val pad = (16 * resources.displayMetrics.density).toInt()
            // Applies equal padding to all four sides of the layout.
            setPadding(pad, pad, pad, pad)
        }

        // Creates an input field for editing the report title.
        val editTitle = android.widget.EditText(requireContext()).apply {
            // Displays a hint describing the title field.
            hint = "Report Title"
            // Pre-fills the field with the existing report title.
            setText(report.title)
        }
        // Creates an input field for editing the report category.
        val editCategory = android.widget.EditText(requireContext()).apply {
            // Provides example categories to guide the administrator.
            hint = "Category (e.g. Electronics, Bags)"
            // Pre-fills the field with the existing category.
            setText(report.category)
        }
        // Creates an input field for editing the report location.
        val editLocation = android.widget.EditText(requireContext()).apply {
            // Displays a hint describing the location field.
            hint = "Location"
            // Pre-fills the field with the existing location.
            setText(report.location)
        }
        // Creates an input field for editing the report description.
        val editDescription = android.widget.EditText(requireContext()).apply {
            // Displays a hint describing the description field.
            hint = "Description"
            // Pre-fills the field with the existing description.
            setText(report.description)
            // Ensures the input field displays at least two lines.
            minLines = 2
        }
        // Creates an input field for editing the report status.
        val editStatus = android.widget.EditText(requireContext()).apply {
            // Lists the status values that the administrator can enter.
            hint = "Status (PENDING, APPROVED, RESOLVED, LOST, FOUND)"
            // Pre-fills the field with the existing status.
            setText(report.status)
        }

        // Creates a header label for the administrator edit form.
        val tvHeader = android.widget.TextView(requireContext()).apply {
            // Sets the text displayed in the header.
            text = "Edit Post Details (Administrator Mode)"
            // Sets the header text size to 14 scaled pixels.
            textSize = 14f
            // Makes the header text bold.
            setTypeface(null, android.graphics.Typeface.BOLD)
            // Applies the primary text color defined in the app resources.
            setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
            // Adds bottom padding to separate the header from the input fields.
            setPadding(0, 0, 0, (8 * resources.displayMetrics.density).toInt())
        }

        // Adds the header to the layout.
        layout.addView(tvHeader)
        // Adds the title input field to the layout.
        layout.addView(editTitle)
        // Adds the category input field to the layout.
        layout.addView(editCategory)
        // Adds the location input field to the layout.
        layout.addView(editLocation)
        // Adds the description input field to the layout.
        layout.addView(editDescription)
        // Adds the status input field to the layout.
        layout.addView(editStatus)

        // Builds the dialog containing the post editing form.
        AlertDialog.Builder(requireContext())
            // Displays the ID of the report being edited in the dialog title.
            .setTitle("Edit Post: ${report.itemId}")
            // Places the custom layout inside the dialog.
            .setView(layout)
            // Saves the changes when the Save Changes button is clicked.
            .setPositiveButton("Save Changes") { _, _ ->
                // Retrieves the new title and removes leading and trailing whitespace.
                val newTitle = editTitle.text.toString().trim()
                // Retrieves the new category and removes leading and trailing whitespace.
                val newCat = editCategory.text.toString().trim()
                // Retrieves the new location and removes leading and trailing whitespace.
                val newLoc = editLocation.text.toString().trim()
                // Retrieves the new description and removes leading and trailing whitespace.
                val newDesc = editDescription.text.toString().trim()
                // Retrieves the new status, removes surrounding whitespace, and converts it to uppercase.
                val newStat = editStatus.text.toString().trim().uppercase()

                // Continues with the update only if the title is not empty.
                if (newTitle.isNotEmpty()) {
                    // Sends the edited report details to Firebase.
                    FirebaseManager.editItem(
                        itemId = report.itemId,
                        title = newTitle,
                        category = newCat,
                        location = newLoc,
                        description = newDesc,
                        status = newStat,
                        // Displays a success message after the report is updated.
                        onSuccess = {
                            com.group3.lostandfound.util.ModernToast.show(requireContext(), "Report updated successfully", view)
                        },
                        // Displays an authorization alert if the update fails.
                        onError = { showAuthorizationAlert(it) }
                    )
                }
            }
            // Closes the dialog without saving changes.
            .setNegativeButton("Cancel", null)
            // Displays the edit dialog.
            .show()
    }

    /**
     * Confirms the user ban and sets /users/{uid}/isBanned = true through FirebaseManager.
     */
    private fun confirmBanUser(targetUid: String) {
        // Creates a confirmation dialog for banning the selected user.
        AlertDialog.Builder(requireContext())
            // Sets the dialog title.
            .setTitle("Ban User Account")
            // Explains the effect of banning the user's account.
            .setMessage("Ban user '$targetUid' from creating new reports or contacting members?")
            // Performs the ban operation when the Ban User button is clicked.
            .setPositiveButton("Ban User") { _, _ ->
                // Updates the user's banned status in Firebase.
                FirebaseManager.banUser(
                    targetUid = targetUid,
                    isBanned = true,
                    // Displays a longer success message after the user is banned.
                    onSuccess = {
                        com.group3.lostandfound.util.ModernToast.show(requireContext(), "User '$targetUid' has been banned", view, durationLong = true)
                    },
                    // Displays an authorization alert if the ban operation fails.
                    onError = { showAuthorizationAlert(it) }
                )
            }
            // Closes the dialog without banning the user.
            .setNegativeButton("Cancel", null)
            // Displays the ban confirmation dialog.
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
