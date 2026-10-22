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

        // Smooth entry animation
        view.alpha = 0f
        view.animate().alpha(1f).setDuration(400).start()

        initViews(view)
        setupListeners()
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

        // Login button with FirebaseAuth validation & Admin credentials verification
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

            val ctx = context ?: return@setOnClickListener

            // 1. Check if admin credentials match admin@gmail.com / admin@123
            val isAdmin = email.equals("admin@gmail.com", ignoreCase = true) && password == "admin@123"

            if (isAdmin) {
                // Ensure Firebase seed data is initialized
                FirebaseManager.seedDefaultDataIfEmpty()

                // Sync admin user role in Firebase Realtime DB
                val adminUid = FirebaseManager.currentUid
                FirebaseManager.usersRef.child(adminUid).child("role").setValue("ADMIN")
                FirebaseManager.usersRef.child(adminUid).child("email").setValue("admin@gmail.com")

                com.group3.lostandfound.service.FCMService.triggerPushNotification(
                    ctx,
                    "Admin Session Active",
                    "Authenticated successfully with Administrator privileges."
                )

                ctx.getSharedPreferences("findit_prefs", android.content.Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean("is_logged_in", true)
                    .putBoolean("is_admin", true)
                    .putBoolean("onboarding_completed", true)
                    .apply()

                com.group3.lostandfound.util.ModernToast.show(
                    ctx,
                    "Admin session verified. Directing to Administrator Dashboard."
                )

                if (isAdded) {
                    parentFragmentManager.beginTransaction()
                        .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                        .replace(R.id.screenBox, AdminFragment.newInstance())
                        .commit()
                }
            } else {

                // Attempt FirebaseAuth.signInWithEmailAndPassword
                val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
                auth.signInWithEmailAndPassword(email, password)
                    .addOnSuccessListener {
                        val safeCtx = context ?: return@addOnSuccessListener
                        safeCtx.getSharedPreferences("findit_prefs", android.content.Context.MODE_PRIVATE)
                            .edit()
                            .putBoolean("is_logged_in", true)
                            .putBoolean("is_admin", false)
                            .putBoolean("onboarding_completed", true)
                            .apply()

                        com.group3.lostandfound.service.FCMService.triggerPushNotification(
                            safeCtx,
                            "Account Login Successful",
                            "Welcome back to Foundly. Signed in as $email."
                        )

                        com.group3.lostandfound.util.ModernToast.show(
                            safeCtx,
                            "Welcome back. Navigating to Homepage."
                        )

                        if (isAdded) {
                            parentFragmentManager.beginTransaction()
                                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                                .replace(R.id.screenBox, HomeFragment.newInstance())
                                .commit()
                        }
                    }
                    .addOnFailureListener { exc ->
                        val safeCtx = context ?: return@addOnFailureListener
                        val errorMessage = when {
                            exc.message?.contains("password", ignoreCase = true) == true ||
                            exc.message?.contains("wrong", ignoreCase = true) == true ||
                            exc.message?.contains("credential", ignoreCase = true) == true -> "Incorrect password. Please try again."
                            exc.message?.contains("no user", ignoreCase = true) == true ||
                            exc.message?.contains("user-not-found", ignoreCase = true) == true -> "No account found for $email. Please sign up first."
                            exc.message?.contains("network", ignoreCase = true) == true -> "Network connection error. Check your internet connection."
                            else -> exc.localizedMessage ?: "Invalid credentials. Please verify your email and password."
                        }
                        com.group3.lostandfound.util.ModernToast.show(safeCtx, errorMessage, view)
                        Toast.makeText(safeCtx, errorMessage, Toast.LENGTH_LONG).show()
                    }
            }
        }

        forgotPasswordBtn.setOnClickListener {
            val email = emailInput.text.toString().trim()
            if (email.isNotEmpty() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                com.google.firebase.auth.FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                    .addOnCompleteListener {
                        Toast.makeText(requireContext(), "Password reset link sent to $email", Toast.LENGTH_LONG).show()
                    }
            } else {
                Toast.makeText(requireContext(), "Please enter your email address to reset password", Toast.LENGTH_SHORT).show()
                emailInput.requestFocus()
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

    companion object {
        fun newInstance(): LoginFragment = LoginFragment()
    }
}
