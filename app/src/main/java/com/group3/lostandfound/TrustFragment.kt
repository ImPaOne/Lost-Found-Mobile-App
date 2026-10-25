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

    private lateinit var badgePhone: View
    private lateinit var badgeEmail: View

    private lateinit var itemFirstFind: View
    private lateinit var itemCommunityHelper: View
    private lateinit var itemTrustedReporter: View
    private lateinit var itemReunitedHero: View

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
        loadLiveTrustData()
        setupBottomNav(view, "profile")
    }

    private fun loadLiveTrustData() {
        val uid = FirebaseManager.currentUid
        if (uid.isEmpty()) return

        val userRef = com.google.firebase.database.FirebaseDatabase.getInstance(FirebaseManager.DATABASE_URL)
            .getReference("users")
            .child(uid)

        userRef.addValueEventListener(object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                if (!isAdded) return
                val rawScore = snapshot.child("trustScore").value
                val trustScore = when (rawScore) {
                    is Number -> rawScore.toInt()
                    is String -> rawScore.toIntOrNull() ?: 92
                    else -> 92
                }

                val isTrusted = snapshot.child("isTrustedFinder").getValue(Boolean::class.java) ?: false
                val itemsReturned = snapshot.child("itemsReturned").getValue(Long::class.java) ?: 0L

                textScore.text = trustScore.toString()

                val (levelText, rankText, xpVal) = when {
                    trustScore >= 90 -> Triple("Level 8 · Guardian", "Top 5% in Western Province", (trustScore % 10) * 10)
                    trustScore >= 75 -> Triple("Level 5 · Senior Tracker", "Top 20% in Western Province", (trustScore % 10) * 10)
                    else -> Triple("Level 2 · Community Finder", "Active Community Member", (trustScore % 10) * 10)
                }

                textLevel.text = levelText
                textRank.text = rankText
                progressXp.progress = xpVal.coerceIn(15, 95)

                // Reunited Hero badge status
                val hasReunitedBadge = snapshot.child("achievements").children.any {
                    it.child("badge").getValue(String::class.java) == "HERO_REUNITED" ||
                    (it.child("title").getValue(String::class.java) ?: "").contains("Reunited Hero", ignoreCase = true)
                } || itemsReturned > 0L

                val tagHero = view?.findViewById<TextView>(R.id.tagReunitedHero)
                if (hasReunitedBadge) {
                    tagHero?.text = "Earned (+20 XP)"
                    tagHero?.setBackgroundResource(R.drawable.bg_tag_earned)
                } else {
                    tagHero?.text = "In Progress"
                    tagHero?.setBackgroundResource(R.drawable.bg_pill_inactive)
                }
            }

            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
        })
    }

    private fun initViews(view: View) {
        btnBack = view.findViewById(R.id.btnBack)
        btnMore = view.findViewById(R.id.btnMore)

        textScore = view.findViewById(R.id.textScore)
        textLevel = view.findViewById(R.id.textLevel)
        textRank = view.findViewById(R.id.textRank)
        progressXp = view.findViewById(R.id.progressXp)

        badgePhone = view.findViewById(R.id.badgePhone)
        badgeEmail = view.findViewById(R.id.badgeEmail)

        itemFirstFind = view.findViewById(R.id.itemFirstFind)
        itemCommunityHelper = view.findViewById(R.id.itemCommunityHelper)
        itemTrustedReporter = view.findViewById(R.id.itemTrustedReporter)
        itemReunitedHero = view.findViewById(R.id.itemReunitedHero)
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

        badgePhone.setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Phone Number Verification")
                .setMessage("Status: ACTIVE\nTwo-step phone authentication active for secure item handover communication.")
                .setPositiveButton("OK", null)
                .show()
        }

        badgeEmail.setOnClickListener {
            val email = FirebaseManager.currentUserEmail.ifEmpty { "user@foundly.lk" }
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Email Verification")
                .setMessage("Status: VERIFIED\nEmail: $email\nOfficial notifications and item status updates are routed to this account.")
                .setPositiveButton("OK", null)
                .show()
        }

        itemFirstFind.setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Achievement: First Find")
                .setMessage("✓ EARNED\nYou completed your first item report on Foundly and helped connect community members.")
                .setPositiveButton("Awesome", null)
                .show()
        }

        itemCommunityHelper.setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Achievement: Community Helper")
                .setMessage("✓ EARNED (+50 Trust XP)\nAssisted 10 neighbors in recovering missing items across the Colombo metropolitan zone.")
                .setPositiveButton("Great", null)
                .show()
        }

        itemTrustedReporter.setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Achievement: Trusted Reporter")
                .setMessage("Status: IN PROGRESS (18 / 20 reports verified)\nMaintain 90%+ accuracy across 20 reports to earn the permanent Community Gold Shield badge.")
                .setPositiveButton("Keep Going", null)
                .show()
        }

        itemReunitedHero.setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Achievement: Reunited Hero")
                .setMessage("✓ EARNED (+20 Trust XP)\nRecognized and selected by reporting persons for safely returning missing items and completing case resolution.")
                .setPositiveButton("Honor Badge", null)
                .show()
        }
    }

    companion object {
        fun newInstance(): TrustFragment = TrustFragment()
    }
}
