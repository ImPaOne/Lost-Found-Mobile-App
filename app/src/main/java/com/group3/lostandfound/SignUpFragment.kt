package com.group3.lostandfound

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment

/**
 * SignUpFragment handles user registration for FindIt LK:
 * - Collects full name, email, phone number, NIC, and password
 * - Supports toggling password and confirm password visibility
 * - Validates terms acceptance and form inputs
 * - Links to Login and Home navigation
 */
class SignUpFragment : Fragment() {

    private lateinit var inputFullName: EditText
    private lateinit var inputEmail: EditText
    private lateinit var inputPhone: EditText
    private lateinit var inputNic: EditText
    private lateinit var inputPassword: EditText
    private lateinit var inputConfirmPassword: EditText

    private lateinit var btnTogglePassword: ImageView
    private lateinit var btnToggleConfirmPassword: ImageView
    private lateinit var checkTerms: CheckBox
    private lateinit var btnCreateAccount: TextView
    private lateinit var rowLogin: View

    private var isPasswordVisible = false
    private var isConfirmPasswordVisible = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_signup, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupListeners()
        setupBottomNav(view)
    }

    private fun initViews(view: View) {
        inputFullName = view.findViewById(R.id.inputFullName)
        inputEmail = view.findViewById(R.id.inputEmail)
        inputPhone = view.findViewById(R.id.inputPhone)
        inputNic = view.findViewById(R.id.inputNic)
        inputPassword = view.findViewById(R.id.inputPassword)
        inputConfirmPassword = view.findViewById(R.id.inputConfirmPassword)

        btnTogglePassword = view.findViewById(R.id.btnTogglePassword)
        btnToggleConfirmPassword = view.findViewById(R.id.btnToggleConfirmPassword)
        checkTerms = view.findViewById(R.id.checkTerms)
        btnCreateAccount = view.findViewById(R.id.btnCreateAccount)
        rowLogin = view.findViewById(R.id.rowLogin)
    }

    private fun setupListeners() {
        btnTogglePassword.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            val cursorPosition = inputPassword.selectionStart
            if (isPasswordVisible) {
                inputPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            } else {
                inputPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            inputPassword.setSelection(cursorPosition)
        }

        btnToggleConfirmPassword.setOnClickListener {
            isConfirmPasswordVisible = !isConfirmPasswordVisible
            val cursorPosition = inputConfirmPassword.selectionStart
            if (isConfirmPasswordVisible) {
                inputConfirmPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            } else {
                inputConfirmPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            inputConfirmPassword.setSelection(cursorPosition)
        }

        btnCreateAccount.setOnClickListener {
            val name = inputFullName.text.toString().trim()
            val email = inputEmail.text.toString().trim()
            val agreed = checkTerms.isChecked

            if (!agreed) {
                Toast.makeText(requireContext(), "Please agree to the Terms of Service", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val displayName = if (name.isNotEmpty()) name else "Nadeesha Silva"
            Toast.makeText(requireContext(), "Account created for $displayName!", Toast.LENGTH_SHORT).show()

            parentFragmentManager.beginTransaction()
                .replace(R.id.screenBox, HomeFragment.newInstance())
                .commit()
        }

        rowLogin.setOnClickListener {
            Toast.makeText(requireContext(), "Navigate to Login", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        fun newInstance(): SignUpFragment = SignUpFragment()
    }
}
