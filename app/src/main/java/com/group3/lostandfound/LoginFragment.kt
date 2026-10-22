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
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.group3.lostandfound.model.AuthUiState
import com.group3.lostandfound.service.FCMService
import com.group3.lostandfound.util.ModernToast
import com.group3.lostandfound.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

/**
 * LoginFragment handles user authentication for Foundly following MVVM architecture.
 * Delegating authentication operations and state to AuthViewModel.
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
    private lateinit var authViewModel: AuthViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        authViewModel = ViewModelProvider(this)[AuthViewModel::class.java]

        // Smooth entry animation
        view.alpha = 0f
        view.animate().alpha(1f).setDuration(400).start()

        initViews(view)
        setupListeners()
        observeViewModel()
    }

    private fun initViews(view: View) {
        emailInput = view.findViewById(R.id.inputEmail)
        passwordInput = view.findViewById(R.id.inputPassword)
        togglePasswordBtn = view.findViewById(R.id.btnTogglePassword)
        rememberMeCheck = view.findViewById(R.id.checkRememberMe)
        forgotPasswordBtn = view.findViewById(R.id.btnForgotPassword)
        loginBtn = view.findViewById(R.id.btnLogin)
        signUpRow = view.findViewById(R.id.rowSignUp)

        // Restore saved preference
        val prefs = requireContext().getSharedPreferences("findit_prefs", android.content.Context.MODE_PRIVATE)
        val remembered = prefs.getBoolean("remember_me", false)
        rememberMeCheck.isChecked = remembered
        if (remembered) {
            val savedEmail = prefs.getString("saved_email", "") ?: ""
            emailInput.setText(savedEmail)
        }
    }

    private fun setupListeners() {
        togglePasswordBtn.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {
                passwordInput.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                togglePasswordBtn.alpha = 1.0f
            } else {
                passwordInput.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                togglePasswordBtn.alpha = 0.5f
            }
            passwordInput.setSelection(passwordInput.text.length)
        }

        rememberMeCheck.setOnCheckedChangeListener { _, isChecked ->
            val prefs = requireContext().getSharedPreferences("findit_prefs", android.content.Context.MODE_PRIVATE)
            prefs.edit().putBoolean("remember_me", isChecked).apply()
            if (!isChecked) {
                prefs.edit().remove("saved_email").apply()
            }
        }

        loginBtn.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (email.isEmpty()) {
                emailInput.error = "Please enter your email"
                emailInput.requestFocus()
                return@setOnClickListener
            }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailInput.error = "Please enter a valid email address"
                emailInput.requestFocus()
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                passwordInput.error = "Please enter your password"
                passwordInput.requestFocus()
                return@setOnClickListener
            }

            if (rememberMeCheck.isChecked) {
                val prefs = requireContext().getSharedPreferences("findit_prefs", android.content.Context.MODE_PRIVATE)
                prefs.edit().putString("saved_email", email).apply()
            }

            loginBtn.isEnabled = false
            loginBtn.text = "Signing in..."
            authViewModel.login(email, password)
        }

        forgotPasswordBtn.setOnClickListener {
            val email = emailInput.text.toString().trim()
            if (email.isNotEmpty() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                authViewModel.sendPasswordReset(email)
            } else {
                Toast.makeText(requireContext(), "Enter your email to receive a password reset link", Toast.LENGTH_SHORT).show()
            }
        }

        signUpRow.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.screenBox, SignUpFragment.newInstance())
                .addToBackStack(null)
                .commit()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            authViewModel.loginState.collect { state ->
                when (state) {
                    is AuthUiState.Loading -> {
                        loginBtn.isEnabled = false
                        loginBtn.text = "Signing in..."
                    }
                    is AuthUiState.Success -> {
                        loginBtn.isEnabled = true
                        loginBtn.text = "Sign in"

                        val safeCtx = context ?: return@collect
                        val email = emailInput.text.toString().trim()

                        safeCtx.getSharedPreferences("findit_prefs", android.content.Context.MODE_PRIVATE)
                            .edit()
                            .putBoolean("is_logged_in", true)
                            .putBoolean("is_admin", state.isAdmin)
                            .putString("user_uid", authViewModel.currentUid)
                            .putString("saved_email", email)
                            .putBoolean("onboarding_completed", true)
                            .apply()

                        FCMService.triggerPushNotification(
                            safeCtx,
                            if (state.isAdmin) "Admin Session Active" else "Account Login Successful",
                            "Welcome back to Foundly. Signed in as $email."
                        )

                        ModernToast.show(
                            safeCtx,
                            if (state.isAdmin) "Admin session verified. Directing to Administrator Dashboard."
                            else "Welcome back. Navigating to Homepage."
                        )

                        if (isAdded) {
                            val destination = if (state.isAdmin) AdminFragment.newInstance() else HomeFragment.newInstance()
                            parentFragmentManager.beginTransaction()
                                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                                .replace(R.id.screenBox, destination)
                                .commit()
                        }
                        authViewModel.resetLoginState()
                    }
                    is AuthUiState.Error -> {
                        loginBtn.isEnabled = true
                        loginBtn.text = "Sign in"

                        val safeCtx = context ?: return@collect
                        ModernToast.show(safeCtx, state.message, view)
                        Toast.makeText(safeCtx, state.message, Toast.LENGTH_LONG).show()
                        authViewModel.resetLoginState()
                    }
                    is AuthUiState.Idle -> {
                        loginBtn.isEnabled = true
                        loginBtn.text = "Sign in"
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            authViewModel.resetPasswordState.collect { state ->
                when (state) {
                    is AuthUiState.Success -> {
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                        authViewModel.resetPasswordResetState()
                    }
                    is AuthUiState.Error -> {
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                        authViewModel.resetPasswordResetState()
                    }
                    else -> {}
                }
            }
        }
    }

    companion object {
        fun newInstance(): LoginFragment = LoginFragment()
    }
}
