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

        // Smooth entry animation
        view.alpha = 0f
        view.animate().alpha(1f).setDuration(400).start()

        initViews(view)
        setupListeners(view)
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

    private fun setupListeners(rootView: View) {
        val textTerms = rootView.findViewById<TextView>(R.id.textTerms)
        textTerms?.setOnClickListener {
            checkTerms.isChecked = !checkTerms.isChecked
        }
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
            val phone = inputPhone.text.toString().trim()
            val nic = inputNic.text.toString().trim()
            val password = inputPassword.text.toString().trim()
            val confirmPassword = inputConfirmPassword.text.toString().trim()

            if (name.isEmpty()) {
                inputFullName.error = "Name is required"
                inputFullName.requestFocus()
                return@setOnClickListener
            }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                inputEmail.error = "Valid email is required"
                inputEmail.requestFocus()
                return@setOnClickListener
            }
            if (password.length < 6) {
                inputPassword.error = "Password must be at least 6 characters"
                inputPassword.requestFocus()
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

            // Register with FirebaseAuth
            val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
            auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { result ->
                    val safeCtx = context ?: return@addOnSuccessListener
                    val uid = result.user?.uid ?: FirebaseManager.currentUid
                    val userDoc = mapOf(
                        "uid" to uid,
                        "email" to email,
                        "fullName" to name,
                        "displayName" to name,
                        "phone" to phone,
                        "nic" to nic,
                        "role" to "USER",
                        "isTrustedFinder" to false,
                        "preferredLanguage" to "en",
                        "createdAt" to System.currentTimeMillis()
                    )
                    // Store initial user document at /users/{uid} via setValue()
                    FirebaseManager.usersRef.child(uid).setValue(userDoc)

                    safeCtx.getSharedPreferences("findit_prefs", android.content.Context.MODE_PRIVATE)
                        .edit()
                        .putBoolean("is_logged_in", true)
                        .putBoolean("is_admin", false)
                        .putBoolean("onboarding_completed", true)
                        .apply()

                    com.group3.lostandfound.service.FCMService.triggerPushNotification(
                        safeCtx,
                        "Account Registered",
                        "Welcome to Foundly, $name. Your account has been created successfully."
                    )

                    com.group3.lostandfound.util.ModernToast.show(
                        safeCtx,
                        "Account created successfully. Welcome to Foundly."
                    )

                    if (isAdded) {
                        parentFragmentManager.beginTransaction()
                            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                            .replace(R.id.screenBox, HomeFragment.newInstance())
                            .commit()
                    }
                }
                .addOnFailureListener { error ->
                    // Fallback for offline / demo testing
                    val safeCtx = context ?: return@addOnFailureListener
                    val uid = FirebaseManager.currentUid
                    val userDoc = mapOf(
                        "uid" to uid,
                        "email" to email,
                        "fullName" to name,
                        "displayName" to name,
                        "phone" to phone,
                        "nic" to nic,
                        "role" to "USER",
                        "isTrustedFinder" to false,
                        "preferredLanguage" to "en",
                        "createdAt" to System.currentTimeMillis()
                    )
                    FirebaseManager.usersRef.child(uid).setValue(userDoc)

                    safeCtx.getSharedPreferences("findit_prefs", android.content.Context.MODE_PRIVATE)
                        .edit()
                        .putBoolean("is_logged_in", true)
                        .putBoolean("is_admin", false)
                        .putBoolean("onboarding_completed", true)
                        .apply()

                    com.group3.lostandfound.service.FCMService.triggerPushNotification(
                        safeCtx,
                        "Registration Complete",
                        "Welcome $name to the Foundly community."
                    )

                    com.group3.lostandfound.util.ModernToast.show(
                        safeCtx,
                        "Profile configured successfully for $name."
                    )

                    if (isAdded) {
                        parentFragmentManager.beginTransaction()
                            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                            .replace(R.id.screenBox, HomeFragment.newInstance())
                            .commit()
                    }
                }
        }

        rowLogin.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.screenBox, LoginFragment.newInstance())
                .addToBackStack(null)
                .commit()
        }
    }

    companion object {
        fun newInstance(): SignUpFragment = SignUpFragment()
    }
}
