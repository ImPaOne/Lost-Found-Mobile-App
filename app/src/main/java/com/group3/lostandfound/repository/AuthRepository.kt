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
 */
class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance(FirebaseManager.DATABASE_URL)
) {

    val currentUid: String
        get() = auth.currentUser?.uid.orEmpty()

    val currentUserEmail: String
        get() = auth.currentUser?.email.orEmpty()

    val isUserLoggedIn: Boolean
        get() = auth.currentUser != null

    /**
     * Authenticates with email and password, then resolves user role.
     * Returns true if user is ADMIN, false otherwise.
     */
    suspend fun signIn(email: String, password: String): ResultState<Boolean> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid ?: currentUid

            val roleSnapshot = database.getReference("users").child(uid).child("role").get().await()
            val role = roleSnapshot.getValue(String::class.java)
            val isAdmin = role.equals("ADMIN", ignoreCase = true) || email.equals("admin@gmail.com", ignoreCase = true)

            ResultState.Success(isAdmin)
        } catch (e: Exception) {
            // Handle admin auto-provisioning bootstrap if admin credentials
            if (email.equals("admin@gmail.com", ignoreCase = true) && password == "admin@123") {
                try {
                    val createResult = auth.createUserWithEmailAndPassword(email, password).await()
                    val uid = createResult.user?.uid ?: currentUid
                    val adminDoc = mapOf(
                        "uid" to uid,
                        "email" to email,
                        "fullName" to "System Administrator",
                        "role" to "ADMIN",
                        "isBanned" to false,
                        "trustScore" to 100,
                        "createdAt" to System.currentTimeMillis()
                    )
                    database.getReference("users").child(uid).setValue(adminDoc).await()
                    ResultState.Success(true)
                } catch (createErr: Exception) {
                    ResultState.Error(createErr.localizedMessage ?: "Failed to authenticate administrator.")
                }
            } else {
                val errorMsg = when {
                    e.message?.contains("password", ignoreCase = true) == true ||
                    e.message?.contains("wrong", ignoreCase = true) == true ||
                    e.message?.contains("credential", ignoreCase = true) == true -> "Incorrect email or password. Please try again."
                    e.message?.contains("no user", ignoreCase = true) == true ||
                    e.message?.contains("user-not-found", ignoreCase = true) == true -> "No account found for $email. Please sign up first."
                    e.message?.contains("network", ignoreCase = true) == true -> "Network connection error. Check your internet connection."
                    else -> e.localizedMessage ?: "Invalid credentials. Please verify your email and password."
                }
                ResultState.Error(errorMsg)
            }
        }
    }

    /**
     * Registers a new user with FirebaseAuth and saves profile document to /users/{uid}.
     */
    suspend fun signUp(
        name: String,
        email: String,
        phone: String,
        nic: String,
        password: String
    ): ResultState<String> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid ?: currentUid
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
            database.getReference("users").child(uid).setValue(userDoc).await()
            ResultState.Success(uid)
        } catch (e: Exception) {
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
     * Signs out from FirebaseAuth.
     */
    fun signOut() {
        auth.signOut()
    }
}
