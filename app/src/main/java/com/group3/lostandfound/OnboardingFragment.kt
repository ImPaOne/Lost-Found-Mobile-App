package com.group3.lostandfound

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

/**
 * OnboardingFragment displays the intro screen:
 * - Illustration with magnifying glass search icon
 * - "Lost Something?" heading and description
 * - Progress indicator dots
 * - "Skip" and "Next" navigation buttons
 */
class OnboardingFragment : Fragment() {

    private lateinit var btnSkip: TextView
    private lateinit var btnNext: TextView
    private lateinit var tvTitle: TextView
    private lateinit var tvDescription: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_onboarding, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnSkip = view.findViewById(R.id.btnSkip)
        btnNext = view.findViewById(R.id.btnNext)
        tvTitle = view.findViewById(R.id.tvTitle)
        tvDescription = view.findViewById(R.id.tvDescription)

        // Smooth entry animation
        view.alpha = 0f
        view.animate().alpha(1f).setDuration(400).start()

        btnSkip.setOnClickListener { navigateToLogin() }
        btnNext.setOnClickListener { navigateToFoundScreen() }
    }

    private fun navigateToLogin() {
        requireContext().getSharedPreferences("findit_prefs", android.content.Context.MODE_PRIVATE)
            .edit()
            .putBoolean("onboarding_completed", true)
            .apply()

        parentFragmentManager.beginTransaction()
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
            .replace(R.id.screenBox, LoginFragment.newInstance())
            .commit()
    }

    private fun navigateToFoundScreen() {
        parentFragmentManager.beginTransaction()
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
            .replace(R.id.screenBox, OnboardingFoundFragment.newInstance())
            .addToBackStack(null)
            .commit()
    }

    companion object {
        fun newInstance(): OnboardingFragment {
            return OnboardingFragment()
        }
    }
}
