package com.group3.lostandfound.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.group3.lostandfound.FirebaseManager
import com.group3.lostandfound.ResultState
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Repository managing Firebase Authentication and User Profile Realtime Database interactions.
 * Features dual-engine fallback: if Firebase Auth has CONFIGURATION_NOT_FOUND (provider disabled in console),
 * it seamlessly authenticates against and stores profiles in Firebase Realtime Database.
 */
class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance(FirebaseManager.DATABASE_URL)
) {

    val currentUid: String
        get() = auth.currentUser?.uid?.takeIf { it.isNotEmpty() } ?: FirebaseManager.customUid

    val currentUserEmail: String
        get() = auth.currentUser?.email?.takeIf { it.isNotEmpty() } ?: FirebaseManager.customEmail

    val isUserLoggedIn: Boolean
        get() = auth.currentUser != null || FirebaseManager.customUid.isNotEmpty()

    private fun isConfigurationNotFound(e: Throwable): Boolean {
        val msg = e.message.orEmpty()
        return msg.contains("CONFIGURATION_NOT_FOUND", ignoreCase = true) ||
               msg.contains("configuration-not-found", ignoreCase = true) ||
               msg.contains("The identity provider configuration was not found", ignoreCase = true) ||
               msg.contains("INTERNAL_ERROR", ignoreCase = true) ||
               msg.contains("internal error has occurred", ignoreCase = true)
    }

    /**
     * Authenticates with email and password, then resolves user role.
     * Returns true if user is ADMIN, false otherwise.
     */
    suspend fun signIn(email: String, password: String): ResultState<Boolean> {
        val cleanEmail = email.trim().lowercase()
        val usersRef = database.getReference("users")

        // 1. First attempt FirebaseAuth
        try {
            val result = auth.signInWithEmailAndPassword(cleanEmail, password).await()
            val uid = result.user?.uid ?: currentUid
            FirebaseManager.customUid = uid
            FirebaseManager.customEmail = cleanEmail

            val roleSnapshot = usersRef.child(uid).child("role").get().await()
            val role = roleSnapshot.getValue(String::class.java)
            val isAdmin = role.equals("ADMIN", ignoreCase = true) || cleanEmail == "admin@gmail.com"
            return ResultState.Success(isAdmin)
        } catch (e: Exception) {
            // Check if failure is due to Firebase Auth Email/Password not enabled in Firebase Console
            if (isConfigurationNotFound(e)) {
                return signInViaRealtimeDatabase(cleanEmail, password)
            }

            // If it's the admin bootstrap credentials and account doesn't exist yet
            if (cleanEmail == "admin@gmail.com" && password == "admin@123") {
                return try {
                    val createResult = auth.createUserWithEmailAndPassword(cleanEmail, password).await()
                    val uid = createResult.user?.uid ?: currentUid
                    FirebaseManager.customUid = uid
                    FirebaseManager.customEmail = cleanEmail
                    val adminDoc = mapOf(
                        "uid" to uid,
                        "email" to cleanEmail,
                        "fullName" to "System Administrator",
                        "role" to "ADMIN",
                        "isBanned" to false,
                        "trustScore" to 100,
                        "createdAt" to System.currentTimeMillis()
                    )
                    usersRef.child(uid).setValue(adminDoc).await()
                    ResultState.Success(true)
                } catch (createErr: Exception) {
                    if (isConfigurationNotFound(createErr)) {
                        return signInViaRealtimeDatabase(cleanEmail, password)
                    }
                    ResultState.Error(createErr.localizedMessage ?: "Failed to authenticate administrator.")
                }
            }

            val errorMsg = when {
                e.message?.contains("password", ignoreCase = true) == true ||
                e.message?.contains("wrong", ignoreCase = true) == true ||
                e.message?.contains("credential", ignoreCase = true) == true -> "Incorrect email or password. Please try again."
                e.message?.contains("no user", ignoreCase = true) == true ||
                e.message?.contains("user-not-found", ignoreCase = true) == true -> "No account found for $cleanEmail. Please sign up first."
                e.message?.contains("network", ignoreCase = true) == true -> "Network connection error. Check your internet connection."
                else -> e.localizedMessage ?: "Invalid credentials. Please verify your email and password."
            }
            return ResultState.Error(errorMsg)
        }
    }

    private suspend fun signInViaRealtimeDatabase(email: String, password: String): ResultState<Boolean> {
        return try {
            val usersRef = database.getReference("users")

            // Admin bootstrap check
            if (email == "admin@gmail.com" && password == "admin@123") {
                val adminUid = "admin_master_101"
                val adminDoc = mapOf(
                    "uid" to adminUid,
                    "email" to email,
                    "fullName" to "System Administrator",
                    "role" to "ADMIN",
                    "isBanned" to false,
                    "trustScore" to 100,
                    "createdAt" to System.currentTimeMillis()
                )
                usersRef.child(adminUid).setValue(adminDoc).await()
                FirebaseManager.customUid = adminUid
                FirebaseManager.customEmail = email
                return ResultState.Success(true)
            }

            val snapshot = usersRef.get().await()
            var matchedUserSnapshot: DataSnapshot? = null

            for (child in snapshot.children) {
                val childEmail = child.child("email").getValue(String::class.java).orEmpty().trim().lowercase()
                if (childEmail == email) {
                    matchedUserSnapshot = child
                    break
                }
            }

            if (matchedUserSnapshot == null) {
                return ResultState.Error("No account found for $email. Please sign up first.")
            }

            val storedPassword = matchedUserSnapshot.child("password").getValue(String::class.java).orEmpty()
            if (storedPassword.isNotEmpty() && storedPassword != password) {
                return ResultState.Error("Incorrect email or password. Please try again.")
            }

            val uid = matchedUserSnapshot.child("uid").getValue(String::class.java)?.takeIf { it.isNotEmpty() }
                ?: matchedUserSnapshot.key.orEmpty()
            val role = matchedUserSnapshot.child("role").getValue(String::class.java).orEmpty()
            val isAdmin = role.equals("ADMIN", ignoreCase = true) || email == "admin@gmail.com"

            FirebaseManager.customUid = uid
            FirebaseManager.customEmail = email
            ResultState.Success(isAdmin)
        } catch (dbErr: Exception) {
            ResultState.Error(dbErr.localizedMessage ?: "Database authentication failed.")
        }
    }

    /**
     * Registers a new user with FirebaseAuth and saves profile document to /users/{uid}.
     * If FirebaseAuth has CONFIGURATION_NOT_FOUND, registers user directly into Firebase Realtime Database.
     */
    suspend fun signUp(
        name: String,
        email: String,
        phone: String,
        nic: String,
        password: String
    ): ResultState<String> {
        val cleanEmail = email.trim().lowercase()
        val usersRef = database.getReference("users")

        // 1. Try with FirebaseAuth first
        return try {
            val result = auth.createUserWithEmailAndPassword(cleanEmail, password).await()
            val uid = result.user?.uid ?: currentUid
            FirebaseManager.customUid = uid
            FirebaseManager.customEmail = cleanEmail

            val userDoc = mapOf(
                "uid" to uid,
                "email" to cleanEmail,
                "fullName" to name,
                "displayName" to name,
                "phone" to phone,
                "nic" to nic,
                "password" to password,
                "role" to if (cleanEmail == "admin@gmail.com") "ADMIN" else "USER",
                "isTrustedFinder" to false,
                "preferredLanguage" to "en",
                "createdAt" to System.currentTimeMillis()
            )
            usersRef.child(uid).setValue(userDoc).await()
            ResultState.Success(uid)
        } catch (e: Exception) {
            if (isConfigurationNotFound(e)) {
                return signUpViaRealtimeDatabase(name, cleanEmail, phone, nic, password)
            }

            val errorMsg = when {
                e.message?.contains("email-already-in-use", ignoreCase = true) == true ||
                e.message?.contains("already exists", ignoreCase = true) == true -> "This email is already registered. Please log in instead."
                e.message?.contains("network", ignoreCase = true) == true -> "Network connection error. Check your internet connection."
                e.message?.contains("weak-password", ignoreCase = true) == true -> "Password is too weak. Please use at least 6 characters."
                else -> e.localizedMessage ?: "Registration failed. Please check your details and try again."
            }
            ResultState.Error(errorMsg)
        }
    }

    private suspend fun signUpViaRealtimeDatabase(
        name: String,
        email: String,
        phone: String,
        nic: String,
        password: String
    ): ResultState<String> {
        return try {
            val usersRef = database.getReference("users")
            val snapshot = usersRef.get().await()

            for (child in snapshot.children) {
                val childEmail = child.child("email").getValue(String::class.java).orEmpty().trim().lowercase()
                if (childEmail == email) {
                    return ResultState.Error("This email is already registered. Please log in instead.")
                }
            }

            val uid = usersRef.push().key ?: "user_${System.currentTimeMillis()}"
            FirebaseManager.customUid = uid
            FirebaseManager.customEmail = email

            val userDoc = mapOf(
                "uid" to uid,
                "email" to email,
                "fullName" to name,
                "displayName" to name,
                "phone" to phone,
                "nic" to nic,
                "password" to password,
                "role" to if (email == "admin@gmail.com") "ADMIN" else "USER",
                "isTrustedFinder" to false,
                "preferredLanguage" to "en",
                "createdAt" to System.currentTimeMillis()
            )
            usersRef.child(uid).setValue(userDoc).await()
            ResultState.Success(uid)
        } catch (dbErr: Exception) {
            ResultState.Error(dbErr.localizedMessage ?: "Failed to save user account.")
        }
    }

    /**
     * Sends password reset email.
     */
    suspend fun sendPasswordReset(email: String): ResultState<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            ResultState.Success(Unit)
        } catch (e: Exception) {
            ResultState.Error(e.localizedMessage ?: "Failed to send password reset email.")
        }
    }

    /**
     * Streams the display name of the given user.
     */
    fun getUserDisplayName(uid: String): Flow<String> = callbackFlow {
        if (uid.isEmpty()) {
            trySend("Community Member")
            close()
            return@callbackFlow
        }

        val ref = database.getReference("users").child(uid)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val fullName = snapshot.child("fullName").getValue(String::class.java)
                    ?: snapshot.child("displayName").getValue(String::class.java)
                    ?: "Community Member"
                trySend(fullName)
            }

            override fun onCancelled(error: DatabaseError) {
                trySend("Community Member")
            }
        }

        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    /**
     * Signs out from FirebaseAuth and clears custom user session.
     */
    fun signOut() {
        auth.signOut()
        FirebaseManager.customUid = ""
        FirebaseManager.customEmail = ""
    }
}
