package com.group3.lostandfound

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment

import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * AdminFragment displays the operations overview, statistics, trend chart,
 * moderation queue banner, quick actions, and recent activity.
 * Built with clean human-written code and Firebase Realtime Database integration.
 */
class AdminFragment : Fragment() {

    private lateinit var totalReportsNumber: TextView
    private lateinit var activeCasesNumber: TextView
    private lateinit var resolvedCasesNumber: TextView
    private lateinit var totalUsersNumber: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_admin, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initStatViews(view)
        observeLiveFirebaseStats()
        verifyAdminAccess()

        val btnBack = view.findViewById<ImageView>(R.id.backButton)
        val btnBell = view.findViewById<ImageView>(R.id.notificationBell)
        val cardModQueue = view.findViewById<View>(R.id.moderationQueueCard)
        val btnReview = view.findViewById<View>(R.id.reviewReportsButton)
        val btnUsers = view.findViewById<View>(R.id.manageUsersButton)
        val btnAnnounce = view.findViewById<View>(R.id.announcementButton)

        btnBack.setOnClickListener {
            if (parentFragmentManager.backStackEntryCount > 0) {
                parentFragmentManager.popBackStack()
            } else {
                parentFragmentManager.beginTransaction()
                    .replace(R.id.screenBox, ProfileFragment.newInstance())
                    .commit()
            }
        }

        btnBell.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.screenBox, NotificationsFragment.newInstance())
                .addToBackStack(null)
                .commit()
        }

        cardModQueue.setOnClickListener {
            openModerationScreen()
        }

        btnReview.setOnClickListener {
            openModerationScreen()
        }

        btnUsers.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.screenBox, TrustFragment.newInstance())
                .addToBackStack(null)
                .commit()
        }

        btnAnnounce.setOnClickListener {
            val adminOptions = arrayOf("Post Global Announcement", "Export Reports for Law Enforcement")
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Admin Actions")
                .setItems(adminOptions) { _, which ->
                    if (which == 0) {
                        val input = android.widget.EditText(requireContext()).apply {
                            hint = "Enter global announcement..."
                        }
                        androidx.appcompat.app.AlertDialog.Builder(requireContext())
                            .setTitle("Global Announcement")
                            .setView(input)
                            .setPositiveButton("Post to Community") { _, _ ->
                                val text = input.text.toString().trim()
                                if (text.isNotEmpty()) {
                                    val announcement = mapOf(
                                        "title" to "Foundly Official Announcement",
                                        "message" to text,
                                        "timestamp" to System.currentTimeMillis()
                                    )
                                    FirebaseManager.database.getReference("announcements").push().setValue(announcement)
                                    com.group3.lostandfound.util.ModernToast.show(requireContext(), "Global announcement published.", view)
                                }
                            }
                            .setNegativeButton("Cancel", null)
                            .show()
                    } else {
                        // Export reports for law enforcement (Requirement 3.2 #12)
                        FirebaseManager.itemsRef.get().addOnSuccessListener { snapshot ->
                            val exportText = buildString {
                                appendLine("=== FOUNDLY OFFICIAL REPORTS AUDIT EXPORT ===")
                                appendLine("Generated: ${java.util.Date()}")
                                appendLine("Authority: Administrator")
                                appendLine("Total Reports in Database: ${snapshot.childrenCount}")
                                var index = 1
                                for (child in snapshot.children) {
                                    val title = child.child("title").getValue(String::class.java) ?: "Report"
                                    val loc = child.child("location").getValue(String::class.java) ?: "Sri Lanka"
                                    val status = child.child("status").getValue(String::class.java) ?: "PENDING"
                                    appendLine("$index. $title - $loc - Status: $status")
                                    index++
                                }
                                appendLine("==============================================")
                            }
                            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(android.content.Intent.EXTRA_SUBJECT, "Foundly Law Enforcement Export")
                                putExtra(android.content.Intent.EXTRA_TEXT, exportText)
                            }
                            startActivity(android.content.Intent.createChooser(shareIntent, "Export reports to"))
                        }
                    }
                }
                .show()
        }

        view.findViewById<View>(R.id.trendGraph)?.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.screenBox, AnalyticsFragment.newInstance())
                .addToBackStack(null)
                .commit()
        }

        view.findViewById<View>(R.id.totalReportsNumber)?.setOnClickListener {
            openModerationScreen()
        }

        view.findViewById<View>(R.id.activeCasesNumber)?.setOnClickListener {
            openModerationScreen()
        }

        view.findViewById<View>(R.id.resolvedCasesNumber)?.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.screenBox, AnalyticsFragment.newInstance())
                .addToBackStack(null)
                .commit()
        }

        view.findViewById<View>(R.id.totalUsersNumber)?.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.screenBox, TrustFragment.newInstance())
                .addToBackStack(null)
                .commit()
        }

        view.findViewById<View>(R.id.activityItemApproved)?.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.screenBox, DetailsFragment.newInstance())
                .addToBackStack(null)
                .commit()
        }

        view.findViewById<View>(R.id.activityItemVerification)?.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.screenBox, TrustFragment.newInstance())
                .addToBackStack(null)
                .commit()
        }

        view.findViewById<View>(R.id.activityItemEscalated)?.setOnClickListener {
            openModerationScreen()
        }

        setupBottomNav(view, "admin")
    }

    private fun initStatViews(view: View) {
        totalReportsNumber = view.findViewById(R.id.totalReportsNumber)
        activeCasesNumber = view.findViewById(R.id.activeCasesNumber)
        resolvedCasesNumber = view.findViewById(R.id.resolvedCasesNumber)
        totalUsersNumber = view.findViewById(R.id.totalUsersNumber)
    }

    private fun observeLiveFirebaseStats() {
        viewLifecycleOwner.lifecycleScope.launch {
            FirebaseManager.analyticsFlow.collectLatest { summary ->
                val activeCount = (summary.totalReports - summary.resolvedReports).coerceAtLeast(0)
                totalReportsNumber.text = String.format("%,d", summary.totalReports)
                activeCasesNumber.text = String.format("%,d", activeCount)
                resolvedCasesNumber.text = String.format("%,d", summary.resolvedReports)
                totalUsersNumber.text = String.format("%,d", summary.userCount)
            }
        }
    }

    private fun verifyAdminAccess() {
        FirebaseManager.verifyAdminPrivilege(
            onAuthorized = {
                // Access granted
            },
            onDenied = { errorMsg ->
                AlertDialog.Builder(requireContext())
                    .setTitle("Admin Security Alert")
                    .setMessage("$errorMsg\nPlease ensure you are logged in as admin@gmail.com.")
                    .setPositiveButton("OK", null)
                    .show()
            }
        )
    }

    private fun openModerationScreen() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.screenBox, ModerationFragment.newInstance())
            .addToBackStack(null)
            .commit()
    }

    companion object {
        fun newInstance(): AdminFragment = AdminFragment()
    }
}

