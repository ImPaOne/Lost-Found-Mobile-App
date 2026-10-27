package com.group3.lostandfound

import android.os.Bundle
import android.text.InputType
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.group3.lostandfound.model.AuthUiState
import com.group3.lostandfound.service.FCMService
import com.group3.lostandfound.util.ModernToast
import com.group3.lostandfound.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

/**
 * SignUpFragment handles user registration for Foundly in MVVM architecture.
 * Delegating authentication operations and state to AuthViewModel.
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
    private lateinit var authViewModel: AuthViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_signup, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        authViewModel = ViewModelProvider(this)[AuthViewModel::class.java]

        // Smooth entry animation
        view.alpha = 0f
        view.animate().alpha(1f).setDuration(400).start()

        initViews(view)
        setupListeners(view)
        observeViewModel()
        setupBottomNav(view, "")
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

    private fun setupListeners(view: View) {
        btnTogglePassword.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {
                inputPassword.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                btnTogglePassword.alpha = 1.0f
            } else {
                inputPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                btnTogglePassword.alpha = 0.5f
            }
            inputPassword.setSelection(inputPassword.text.length)
        }

        btnToggleConfirmPassword.setOnClickListener {
            isConfirmPasswordVisible = !isConfirmPasswordVisible
            if (isConfirmPasswordVisible) {
                inputConfirmPassword.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                btnToggleConfirmPassword.alpha = 1.0f
            } else {
                inputConfirmPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                btnToggleConfirmPassword.alpha = 0.5f
            }
            inputConfirmPassword.setSelection(inputConfirmPassword.text.length)
        }

        rowLogin.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Real-time error clearing
        inputFullName.doAfterTextChanged { inputFullName.error = null }
        inputEmail.doAfterTextChanged { inputEmail.error = null }
        inputPhone.doAfterTextChanged { inputPhone.error = null }
        inputNic.doAfterTextChanged { inputNic.error = null }
        inputPassword.doAfterTextChanged { inputPassword.error = null }
        inputConfirmPassword.doAfterTextChanged { inputConfirmPassword.error = null }

        btnCreateAccount.setOnClickListener {
            val name = inputFullName.text.toString().trim()
            val email = inputEmail.text.toString().trim()
            val phone = inputPhone.text.toString().trim()
            val nic = inputNic.text.toString().trim()
            val password = inputPassword.text.toString().trim()
            val confirmPassword = inputConfirmPassword.text.toString().trim()

            if (name.isEmpty()) {
                inputFullName.error = "Full name is required"
                inputFullName.requestFocus()
                return@setOnClickListener
            }
            if (name.length < 2) {
                inputFullName.error = "Name must be at least 2 characters"
                inputFullName.requestFocus()
                return@setOnClickListener
            }

            if (email.isEmpty()) {
                inputEmail.error = "Email address is required"
                inputEmail.requestFocus()
                return@setOnClickListener
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                inputEmail.error = "Please enter a valid email address"
                inputEmail.requestFocus()
                return@setOnClickListener
            }

            if (phone.isEmpty()) {
                inputPhone.error = "Phone number is required"
                inputPhone.requestFocus()
                return@setOnClickListener
            }
            if (!phone.matches(Regex("^[0-9+ -]{9,15}\$"))) {
                inputPhone.error = "Please enter a valid phone number (9-15 digits)"
                inputPhone.requestFocus()
                return@setOnClickListener
            }

            if (nic.isEmpty()) {
                inputNic.error = "NIC or Identity number is required"
                inputNic.requestFocus()
                return@setOnClickListener
            }
            if (nic.length < 4) {
                inputNic.error = "Identity number must be at least 4 characters"
                inputNic.requestFocus()
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                inputPassword.error = "Password is required"
                inputPassword.requestFocus()
                return@setOnClickListener
            }
            if (password.length < 6) {
                inputPassword.error = "Password must be at least 6 characters"
                inputPassword.requestFocus()
                return@setOnClickListener
            }

            if (confirmPassword.isEmpty()) {
                inputConfirmPassword.error = "Please confirm your password"
                inputConfirmPassword.requestFocus()
                return@setOnClickListener
            }
            if (password != confirmPassword) {
                inputConfirmPassword.error = "Passwords do not match"
                inputConfirmPassword.requestFocus()
                return@setOnClickListener
            }

            if (!checkTerms.isChecked) {
                Toast.makeText(requireContext(), "Please agree to the Terms of Service to create an account", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnCreateAccount.isEnabled = false
            btnCreateAccount.text = "Creating account..."
            authViewModel.signUp(name, email, phone, nic, password)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            authViewModel.signUpState.collect { state ->
                when (state) {
                    is AuthUiState.Loading -> {
                        btnCreateAccount.isEnabled = false
                        btnCreateAccount.text = "Creating account..."
                    }
                    is AuthUiState.Success -> {
                        btnCreateAccount.isEnabled = true
                        btnCreateAccount.text = "Create Account"

                        val safeCtx = context ?: return@collect
                        val name = inputFullName.text.toString().trim()

                        safeCtx.getSharedPreferences("findit_prefs", android.content.Context.MODE_PRIVATE)
                            .edit()
                            .putBoolean("is_logged_in", true)
                            .putBoolean("is_admin", false)
                            .putString("user_uid", authViewModel.currentUid)
                            .putString("saved_email", inputEmail.text.toString().trim())
                            .putBoolean("onboarding_completed", true)
                            .apply()

                        FCMService.triggerPushNotification(
                            safeCtx,
                            "Account Registered",
                            "Welcome to Foundly, $name. Your account has been created successfully."
                        )

                        FirebaseManager.customUid = authViewModel.currentUid
                        FirebaseManager.customEmail = inputEmail.text.toString().trim()

                        (activity as? MainActivity)?.setupIncomingMessageNotifications()

                        ModernToast.show(
                            safeCtx,
                            "Account created successfully. Welcome to Foundly."
                        )

                        if (isAdded) {
                            parentFragmentManager.beginTransaction()
                                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                                .replace(R.id.screenBox, HomeFragment.newInstance())
                                .commit()
                        }
                        authViewModel.resetSignUpState()
                    }
                    is AuthUiState.Error -> {
                        btnCreateAccount.isEnabled = true
                        btnCreateAccount.text = "Create Account"

                        val safeCtx = context ?: return@collect
                        ModernToast.show(safeCtx, state.message, view)
                        Toast.makeText(safeCtx, state.message, Toast.LENGTH_LONG).show()
                        authViewModel.resetSignUpState()
                    }
                    is AuthUiState.Idle -> {
                        btnCreateAccount.isEnabled = true
                        btnCreateAccount.text = "Create Account"
                    }
                }
            }
        }
    }

    companion object {
        fun newInstance(): SignUpFragment = SignUpFragment()
    }
}
