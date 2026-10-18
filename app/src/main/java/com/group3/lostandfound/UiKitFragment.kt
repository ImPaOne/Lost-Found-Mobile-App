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
 * UiKitFragment displays the Foundly UI Kit component library:
 * - Buttons (Primary, Secondary, Danger, Disabled)
 * - Input states (Default, Focused, Error, Search)
 * - Tabs, chips & badges (All, Lost, Found, Pets, Avatars, Badges)
 * - Item card list variant
 * - Overlays & feedback components
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
        setupListeners()
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

    private fun setupListeners() {
        view?.findViewById<View>(R.id.tvKitTitle)?.setOnClickListener {
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
            // Disabled button does nothing
        }
    }

    companion object {
        fun newInstance(): UiKitFragment {
            return UiKitFragment()
        }
    }
}
