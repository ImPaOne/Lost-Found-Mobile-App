package com.group3.lostandfound

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment

/**
 * Component Library Screen (Foundly UI Kit).
 * Displays core buttons, inputs, tabs, chips, badges, item cards, and overlays.
 */
class UiKitFragment : Fragment() {

    private lateinit var btnPrimary: TextView
    private lateinit var btnSecondary: TextView
    private lateinit var btnDanger: TextView
    private lateinit var btnDisabled: TextView

    private lateinit var inputDefault: EditText
    private lateinit var inputFocused: EditText
    private lateinit var inputError: EditText

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_ui_kit, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupListeners(view)
        setupBottomNav(view)
    }

    private fun initViews(view: View) {
        btnPrimary = view.findViewById(R.id.btnPrimary)
        btnSecondary = view.findViewById(R.id.btnSecondary)
        btnDanger = view.findViewById(R.id.btnDanger)
        btnDisabled = view.findViewById(R.id.btnDisabled)

        inputDefault = view.findViewById(R.id.inputDefault)
        inputFocused = view.findViewById(R.id.inputFocused)
        inputError = view.findViewById(R.id.inputError)
    }

    private fun setupListeners(view: View) {
        // Back navigation on clicking title
        view.findViewById<View>(R.id.tvKitTitle)?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        btnPrimary.setOnClickListener {
            Toast.makeText(requireContext(), "Primary Button clicked", Toast.LENGTH_SHORT).show()
        }

        btnSecondary.setOnClickListener {
            Toast.makeText(requireContext(), "Secondary Button clicked", Toast.LENGTH_SHORT).show()
        }

        btnDanger.setOnClickListener {
            Toast.makeText(requireContext(), "Danger Button clicked", Toast.LENGTH_SHORT).show()
        }

        btnDisabled.setOnClickListener {
            // Disabled state does nothing
        }

        // Chip clicks
        view.findViewById<View>(R.id.chipAll)?.setOnClickListener {
            Toast.makeText(requireContext(), "Filter: All", Toast.LENGTH_SHORT).show()
        }
        view.findViewById<View>(R.id.chipLost)?.setOnClickListener {
            Toast.makeText(requireContext(), "Filter: Lost", Toast.LENGTH_SHORT).show()
        }
        view.findViewById<View>(R.id.chipFound)?.setOnClickListener {
            Toast.makeText(requireContext(), "Filter: Found", Toast.LENGTH_SHORT).show()
        }
        view.findViewById<View>(R.id.chipPets)?.setOnClickListener {
            Toast.makeText(requireContext(), "Filter: Pets", Toast.LENGTH_SHORT).show()
        }

        // Search Filter button
        view.findViewById<View>(R.id.btnFilterInSearch)?.setOnClickListener {
            Toast.makeText(requireContext(), "Search filters opened", Toast.LENGTH_SHORT).show()
        }

        // List item card click
        view.findViewById<View>(R.id.cardListVariantItem)?.setOnClickListener {
            Toast.makeText(requireContext(), "Selected: Sri Lankan passport", Toast.LENGTH_SHORT).show()
        }

        // Overlay feedback clicks
        view.findViewById<View>(R.id.cardFilterOverlayPreview)?.setOnClickListener {
            Toast.makeText(requireContext(), "Filter results bottom sheet", Toast.LENGTH_SHORT).show()
        }
        view.findViewById<View>(R.id.cardSuccessToastBanner)?.setOnClickListener {
            Toast.makeText(requireContext(), "Report published successfully!", Toast.LENGTH_SHORT).show()
        }
        view.findViewById<View>(R.id.cardLoadingState)?.setOnClickListener {
            Toast.makeText(requireContext(), "State: Loading results...", Toast.LENGTH_SHORT).show()
        }
        view.findViewById<View>(R.id.cardEmptyState)?.setOnClickListener {
            Toast.makeText(requireContext(), "State: No items yet", Toast.LENGTH_SHORT).show()
        }
        view.findViewById<View>(R.id.cardErrorState)?.setOnClickListener {
            Toast.makeText(requireContext(), "State: Couldn't load", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        fun newInstance(): UiKitFragment {
            return UiKitFragment()
        }
    }
}
