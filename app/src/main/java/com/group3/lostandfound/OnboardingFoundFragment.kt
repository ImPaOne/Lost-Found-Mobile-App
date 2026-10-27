package com.group3.lostandfound

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

/**
 * OnboardingFoundFragment displays the second onboarding screen:
 * - Backpack illustration
 * - "Found Something?" heading and description
 * - Progress indicator with 2nd dot active
 * - "Skip" and "Next" navigation buttons
 */
class OnboardingFoundFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_onboarding_found, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnSkip = view.findViewById<View>(R.id.btnSkip)
        val btnNext = view.findViewById<View>(R.id.btnNext)

        // Smooth entry animation
        view.alpha = 0f
        view.animate().alpha(1f).setDuration(400).start()

        btnSkip.setOnClickListener {
            requireContext().getSharedPreferences("findit_prefs", android.content.Context.MODE_PRIVATE)
                .edit()
                .putBoolean("onboarding_completed", true)
                .apply()
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.screenBox, LoginFragment.newInstance())
                .commit()
        }

        btnNext.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.screenBox, OnboardingReuniteFragment.newInstance())
                .addToBackStack(null)
                .commit()
        }
    }

    companion object {
        fun newInstance(): OnboardingFoundFragment = OnboardingFoundFragment()
    }
}
