package com.group3.lostandfound

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

/**
 * OnboardingReuniteFragment displays the third onboarding screen:
 * - "Reunite & Connect" heading and description
 * - Handshake reunion badge
 * - Progress indicator with 3rd dot active
 * - "Skip" and "Get Started" buttons
 */
class OnboardingReuniteFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_onboarding_reunite, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnSkip = view.findViewById<TextView>(R.id.btnSkip)
        val btnGetStarted = view.findViewById<TextView>(R.id.btnGetStarted)

        btnSkip.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.screenBox, HomeFragment.newInstance())
                .commit()
        }

        btnGetStarted.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.screenBox, SignUpFragment.newInstance())
                .addToBackStack(null)
                .commit()
        }
    }

    companion object {
        fun newInstance(): OnboardingReuniteFragment = OnboardingReuniteFragment()
    }
}
