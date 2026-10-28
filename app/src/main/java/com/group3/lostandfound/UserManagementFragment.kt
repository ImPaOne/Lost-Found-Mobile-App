package com.group3.lostandfound

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment

class UserManagementFragment : Fragment() {

    // Top buttons and search
    private lateinit var btnBack: View
    private lateinit var btnShield: View
    private lateinit var searchField: EditText

    // Filter chips
    private lateinit var chipAllUsers: TextView
    private lateinit var chipRole: View
    private lateinit var chipStatus: View
    private lateinit var btnAuditLog: TextView

    // User Cards
    private lateinit var cardNadeesha: View
    private lateinit var cardTharindu: View
    private lateinit var cardAma: View
    private lateinit var cardKasun: View

    // Tharindu expandable section
    private lateinit var rowTharinduHeader: View
    private lateinit var boxTharinduDetails: View
    private lateinit var iconTharinduChevron: ImageView
    private var isTharinduExpanded = true

    // Role and Access options
    private lateinit var boxRoleSelect: View
    private lateinit var lblRoleValue: TextView
    private lateinit var boxAccessSelect: View
    private lateinit var lblAccessValue: TextView

    // Tharindu Action buttons
    private lateinit var btnReviewSuspension: TextView
    private lateinit var btnSaveAccess: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_user_management, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupListeners()
        setupSearch()
        setupBottomNav(view, "profile")
    }

    private fun initViews(view: View) {
        btnBack = view.findViewById(R.id.btnBack)
        btnShield = view.findViewById(R.id.btnShield)
        searchField = view.findViewById(R.id.searchField)

        chipAllUsers = view.findViewById(R.id.chipAllUsers)
        chipRole = view.findViewById(R.id.chipRole)
        chipStatus = view.findViewById(R.id.chipStatus)
        btnAuditLog = view.findViewById(R.id.btnAuditLog)

        cardNadeesha = view.findViewById(R.id.cardNadeesha)
        cardTharindu = view.findViewById(R.id.cardTharindu)
        cardAma = view.findViewById(R.id.cardAma)
        cardKasun = view.findViewById(R.id.cardKasun)

        rowTharinduHeader = view.findViewById(R.id.rowTharinduHeader)
        boxTharinduDetails = view.findViewById(R.id.boxTharinduDetails)
        iconTharinduChevron = view.findViewById(R.id.iconTharinduChevron)

        boxRoleSelect = view.findViewById(R.id.boxRoleSelect)
        lblRoleValue = view.findViewById(R.id.lblRoleValue)
        boxAccessSelect = view.findViewById(R.id.boxAccessSelect)
        lblAccessValue = view.findViewById(R.id.lblAccessValue)

        btnReviewSuspension = view.findViewById(R.id.btnReviewSuspension)
        btnSaveAccess = view.findViewById(R.id.btnSaveAccess)
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        btnShield.setOnClickListener {
            Toast.makeText(requireContext(), "Security status: All permissions verified", Toast.LENGTH_SHORT).show()
        }

        btnAuditLog.setOnClickListener {
            Toast.makeText(requireContext(), "Audit log: 42 access changes recorded", Toast.LENGTH_SHORT).show()
        }

        // Toggle Tharindu card details
        rowTharinduHeader.setOnClickListener {
            isTharinduExpanded = !isTharinduExpanded
            boxTharinduDetails.visibility = if (isTharinduExpanded) View.VISIBLE else View.GONE
            iconTharinduChevron.setImageResource(
                if (isTharinduExpanded) R.drawable.ic_chevron_up else R.drawable.ic_chevron_down
            )
        }

        // Cycle through user roles
        boxRoleSelect.setOnClickListener {
            val nextRole = when (lblRoleValue.text.toString()) {
                "Member" -> "Moderator"
                "Moderator" -> "Admin"
                else -> "Member"
            }
            lblRoleValue.text = nextRole
            Toast.makeText(requireContext(), "Role set to: $nextRole", Toast.LENGTH_SHORT).show()
        }

        // Cycle through access states
        boxAccessSelect.setOnClickListener {
            val nextAccess = when (lblAccessValue.text.toString()) {
                "Active" -> "Restricted"
                "Restricted" -> "Suspended"
                else -> "Active"
            }
            lblAccessValue.text = nextAccess
            Toast.makeText(requireContext(), "Access set to: $nextAccess", Toast.LENGTH_SHORT).show()
        }

        btnSaveAccess.setOnClickListener {
            val role = lblRoleValue.text.toString()
            val access = lblAccessValue.text.toString()
            Toast.makeText(
                requireContext(),
                "Saved access for Tharindu Perera: $role ($access)",
                Toast.LENGTH_SHORT
            ).show()
        }

        btnReviewSuspension.setOnClickListener {
            Toast.makeText(
                requireContext(),
                "Suspensions require a reason and confirmation.",
                Toast.LENGTH_SHORT
            ).show()
        }

        chipAllUsers.setOnClickListener {
            searchField.setText("")
            showAllCards()
        }

        chipRole.setOnClickListener {
            Toast.makeText(requireContext(), "Filter by: Role (All, Admin, Moderator, Member)", Toast.LENGTH_SHORT).show()
        }

        chipStatus.setOnClickListener {
            Toast.makeText(requireContext(), "Filter by: Status (Active, Restricted, Suspended)", Toast.LENGTH_SHORT).show()
        }

        cardAma.setOnClickListener {
            Toast.makeText(requireContext(), "Ama Fernando: Moderator · Colombo", Toast.LENGTH_SHORT).show()
        }

        cardKasun.setOnClickListener {
            Toast.makeText(requireContext(), "Kasun Jayasinghe: Review due", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupSearch() {
        searchField.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString()?.trim()?.lowercase().orEmpty()
                if (query.isEmpty()) {
                    showAllCards()
                } else {
                    cardNadeesha.visibility = if ("nadeesha silva".contains(query) || "admin".contains(query) || "colombo".contains(query)) View.VISIBLE else View.GONE
                    cardTharindu.visibility = if ("tharindu perera".contains(query) || "member".contains(query) || "verified".contains(query)) View.VISIBLE else View.GONE
                    cardAma.visibility = if ("ama fernando".contains(query) || "moderator".contains(query) || "colombo".contains(query)) View.VISIBLE else View.GONE
                    cardKasun.visibility = if ("kasun jayasinghe".contains(query) || "restricted".contains(query) || "review".contains(query)) View.VISIBLE else View.GONE
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun showAllCards() {
        cardNadeesha.visibility = View.VISIBLE
        cardTharindu.visibility = View.VISIBLE
        cardAma.visibility = View.VISIBLE
        cardKasun.visibility = View.VISIBLE
    }

    companion object {
        fun newInstance(): UserManagementFragment {
            return UserManagementFragment()
        }
    }
}
