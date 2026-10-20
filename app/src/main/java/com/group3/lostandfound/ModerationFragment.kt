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
        setupBottomNav(view, "admin")
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
    private var hasPassport = false
    private var hasIPhone = false
    private var hasBracelet = false

    private fun observeFirebaseModerationQueue() {
        FirebaseManager.itemsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var pendingCount = 0
                for (child in snapshot.children) {
                    val status = child.child("status").getValue(String::class.java) ?: "PENDING"
                    if (status.equals("PENDING", ignoreCase = true)) {
                        pendingCount++
                    }
                }
                textReportsCountSummary.text = "$pendingCount reports awaiting review"

                hasPassport = snapshot.hasChild("item_1")
                hasIPhone = snapshot.hasChild("item_2")
                hasBracelet = snapshot.hasChild("item_3")

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
        when (activeTabName) {
            "PENDING" -> {
                cardReportPassport.visibility = if (hasPassport) View.VISIBLE else View.GONE
                cardReportIPhone.visibility = if (hasIPhone) View.VISIBLE else View.GONE
                cardReportBracelet.visibility = if (hasBracelet) View.VISIBLE else View.GONE
            }
            "FLAGGED" -> {
                cardReportPassport.visibility = if (hasPassport) View.VISIBLE else View.GONE
                cardReportIPhone.visibility = if (hasIPhone) View.VISIBLE else View.GONE
                cardReportBracelet.visibility = View.GONE
            }
            "APPROVED" -> {
                cardReportPassport.visibility = View.GONE
                cardReportIPhone.visibility = View.GONE
                cardReportBracelet.visibility = if (hasBracelet) View.VISIBLE else View.GONE
            }
        }
    }

    private fun setupClickListeners() {
        buttonBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        buttonSearch.setOnClickListener {
            Toast.makeText(requireContext(), "Filtering moderation queue...", Toast.LENGTH_SHORT).show()
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
            Toast.makeText(requireContext(), if (allSelected) "All reports selected" else "Selection cleared", Toast.LENGTH_SHORT).show()
        }

        buttonApproveSelected.setOnClickListener {
            approveBatchReports()
        }

        // ================= PASSPORT CARD ACTIONS (item_1, user_perera) =================
        buttonPassportApprove.setOnClickListener {
            approveReport("item_1", "Sri Lankan passport")
        }
        buttonPassportReject.setOnClickListener {
            confirmRemoveReport("item_1", "Sri Lankan passport")
        }
        buttonPassportEscalate.setOnClickListener {
            showEscalationDialog("item_1", "user_perera", "Sri Lankan passport")
        }

        // ================= IPHONE CARD ACTIONS (item_2, user_tharushi) =================
        buttonIPhoneApprove.setOnClickListener {
            approveReport("item_2", "iPhone 14 Pro")
        }
        buttonIPhoneReject.setOnClickListener {
            confirmRemoveReport("item_2", "iPhone 14 Pro")
        }
        buttonIPhoneEscalate.setOnClickListener {
            showEscalationDialog("item_2", "user_tharushi", "iPhone 14 Pro")
        }

        // ================= BRACELET CARD ACTIONS (item_3, user_kavindu) =================
        buttonBraceletApprove.setOnClickListener {
            approveReport("item_3", "Gold bracelet")
        }
        buttonBraceletReject.setOnClickListener {
            confirmRemoveReport("item_3", "Gold bracelet")
        }
        buttonBraceletEscalate.setOnClickListener {
            showEscalationDialog("item_3", "user_kavindu", "Gold bracelet")
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
     * Escalation dialog offering to Flag Item or Ban User
     */
    private fun showEscalationDialog(itemId: String, userId: String, title: String) {
        val actions = arrayOf("Flag Item (Sensitive/Duplicate)", "Ban User Account", "Unflag Item")
        AlertDialog.Builder(requireContext())
            .setTitle("Escalate: $title")
            .setItems(actions) { _, which ->
                when (which) {
                    0 -> {
                        // Flag item via updateChildren()
                        FirebaseManager.flagItem(
                            itemId = itemId,
                            isFlagged = true,
                            onSuccess = {
                                com.group3.lostandfound.util.ModernToast.show(requireContext(), "'$title' marked as Flagged", view)
                            },
                            onError = { showAuthorizationAlert(it) }
                        )
                    }
                    1 -> {
                        // User Ban action via /users/{uid}/isBanned = true
                        confirmBanUser(userId)
                    }
                    2 -> {
                        // Unflag item
                        FirebaseManager.flagItem(
                            itemId = itemId,
                            isFlagged = false,
                            onSuccess = {
                                com.group3.lostandfound.util.ModernToast.show(requireContext(), "'$title' unflagged", view)
                            },
                            onError = { showAuthorizationAlert(it) }
                        )
                    }
                }
            }
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
        listOf("item_1", "item_2", "item_3").forEach { id ->
            FirebaseManager.approveItem(id, {}, {})
        }
        com.group3.lostandfound.util.ModernToast.show(requireContext(), "All selected reports approved", view)
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
                tab.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
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
