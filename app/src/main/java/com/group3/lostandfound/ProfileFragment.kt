package com.group3.lostandfound

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment

/**
 * ProfileFragment displays user profile information, activity statistics,
 * and quick access to reports, saved items, trust score, and settings.
 * Written with clean, human-meaningful variables.
 */
class ProfileFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnClose = view.findViewById<ImageView>(R.id.btnClose)
        val btnMenu = view.findViewById<ImageView>(R.id.btnMenu)

        val rowReports = view.findViewById<LinearLayout>(R.id.rowReports)
        val rowSavedItems = view.findViewById<LinearLayout>(R.id.rowSavedItems)
        val rowTrustScore = view.findViewById<LinearLayout>(R.id.rowTrustScore)
        val rowSettings = view.findViewById<LinearLayout>(R.id.rowSettings)
        val rowHelp = view.findViewById<LinearLayout>(R.id.rowHelp)
        val rowLogout = view.findViewById<LinearLayout>(R.id.rowLogout)

        btnClose.setOnClickListener {
            if (parentFragmentManager.backStackEntryCount > 0) {
                parentFragmentManager.popBackStack()
            } else {
                navigateTo(HomeFragment.newInstance())
            }
        }

        btnMenu.setOnClickListener {
            navigateTo(AdminFragment.newInstance())
        }

        rowReports.setOnClickListener {
            navigateTo(ReportsFragment.newInstance())
        }

        rowSavedItems.setOnClickListener {
            Toast.makeText(requireContext(), "Saved items: 4 items saved", Toast.LENGTH_SHORT).show()
        }

        rowTrustScore.setOnClickListener {
            navigateTo(TrustFragment.newInstance())
        }

        rowSettings.setOnClickListener {
            navigateTo(SettingsFragment.newInstance())
        }

        rowHelp.setOnClickListener {
            Toast.makeText(requireContext(), "Help & Support Center", Toast.LENGTH_SHORT).show()
        }

        rowLogout.setOnClickListener {
            Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show()
            parentFragmentManager.beginTransaction()
                .replace(R.id.screenBox, LoginFragment.newInstance())
                .commit()
        }

        setupBottomNav(view, "profile")
    }

    private fun navigateTo(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.screenBox, fragment)
            .addToBackStack(null)
            .commit()
    }

    companion object {
        fun newInstance(): ProfileFragment {
            return ProfileFragment()
        }
    }
}
