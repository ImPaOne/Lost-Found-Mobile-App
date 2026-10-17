package com.group3.lostandfound

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment

/**
 * TrustFragment displays the user's Trust & Achievements:
 * - Trust score circular badge and community status
 * - Current level XP progress
 * - Verification badges (ID, Phone, Email)
 * - Achievements earned and in-progress
 * - Recent activity history
 */
class TrustFragment : Fragment() {

    private lateinit var btnBack: ImageView
    private lateinit var btnMore: ImageView

    private lateinit var textScore: TextView
    private lateinit var textLevel: TextView
    private lateinit var textRank: TextView
    private lateinit var progressXp: ProgressBar

    private lateinit var badgeId: View
    private lateinit var badgePhone: View
    private lateinit var badgeEmail: View

    private lateinit var itemFirstFind: View
    private lateinit var itemCommunityHelper: View
    private lateinit var itemTrustedReporter: View

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_trust, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupListeners()
        setupBottomNav(view, "profile")
    }

    private fun initViews(view: View) {
        btnBack = view.findViewById(R.id.btnBack)
        btnMore = view.findViewById(R.id.btnMore)

        textScore = view.findViewById(R.id.textScore)
        textLevel = view.findViewById(R.id.textLevel)
        textRank = view.findViewById(R.id.textRank)
        progressXp = view.findViewById(R.id.progressXp)

        badgeId = view.findViewById(R.id.badgeId)
        badgePhone = view.findViewById(R.id.badgePhone)
        badgeEmail = view.findViewById(R.id.badgeEmail)

        itemFirstFind = view.findViewById(R.id.itemFirstFind)
        itemCommunityHelper = view.findViewById(R.id.itemCommunityHelper)
        itemTrustedReporter = view.findViewById(R.id.itemTrustedReporter)
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        btnMore.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.screenBox, SettingsFragment.newInstance())
                .addToBackStack(null)
                .commit()
        }

        badgeId.setOnClickListener {
            Toast.makeText(requireContext(), "ID verification verified", Toast.LENGTH_SHORT).show()
        }

        badgePhone.setOnClickListener {
            Toast.makeText(requireContext(), "Phone number verified", Toast.LENGTH_SHORT).show()
        }

        badgeEmail.setOnClickListener {
            Toast.makeText(requireContext(), "Email address verified", Toast.LENGTH_SHORT).show()
        }

        itemFirstFind.setOnClickListener {
            Toast.makeText(requireContext(), "First Find: Earned", Toast.LENGTH_SHORT).show()
        }

        itemCommunityHelper.setOnClickListener {
            Toast.makeText(requireContext(), "Community Helper: 10 neighbours helped", Toast.LENGTH_SHORT).show()
        }

        itemTrustedReporter.setOnClickListener {
            Toast.makeText(requireContext(), "Trusted Reporter: 18/20 reports verified", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        fun newInstance(): TrustFragment = TrustFragment()
    }
}
