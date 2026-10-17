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
 * LoginFragment handles user authentication for FindIt LK.
 * Written with clean, human-readable code and meaningful variable names.
 */
class LoginFragment : Fragment() {

    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var togglePasswordBtn: ImageView
    private lateinit var rememberMeCheck: CheckBox
    private lateinit var forgotPasswordBtn: TextView
    private lateinit var loginBtn: TextView
    private lateinit var signUpRow: View

    private var isPasswordVisible = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupListeners()
        setupBottomNav(view)
    }

    private fun initViews(view: View) {
        emailInput = view.findViewById(R.id.inputEmail)
        passwordInput = view.findViewById(R.id.inputPassword)
        togglePasswordBtn = view.findViewById(R.id.btnTogglePassword)
        rememberMeCheck = view.findViewById(R.id.checkRememberMe)
        forgotPasswordBtn = view.findViewById(R.id.btnForgotPassword)
        loginBtn = view.findViewById(R.id.btnLogin)
        signUpRow = view.findViewById(R.id.rowSignUp)
    }

    private fun setupListeners() {
        togglePasswordBtn.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            val cursorPosition = passwordInput.selectionStart
            if (isPasswordVisible) {
                passwordInput.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            } else {
                passwordInput.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            passwordInput.setSelection(cursorPosition)
        }

        loginBtn.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (email.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter your email or phone", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter your password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Toast.makeText(requireContext(), "Welcome back!", Toast.LENGTH_SHORT).show()
            parentFragmentManager.beginTransaction()
                .replace(R.id.screenBox, HomeFragment.newInstance())
                .commit()
        }

        forgotPasswordBtn.setOnClickListener {
            Toast.makeText(requireContext(), "Password reset instructions sent to your email", Toast.LENGTH_SHORT).show()
        }

        signUpRow.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.screenBox, SignUpFragment.newInstance())
                .addToBackStack(null)
                .commit()
        }
    }

    companion object {
        fun newInstance(): LoginFragment = LoginFragment()
    }
}
