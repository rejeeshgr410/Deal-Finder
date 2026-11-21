package com.example.data

import android.content.Context
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

class AuthRepository(private val dao: DealFinderDao, private val session: SessionManager) {

    val currentUserId = session.currentUserId
    private val auth = FirebaseAuth.getInstance()
    private var appContext: Context? = null

    fun getUserByIdFlow(userId: String): Flow<UserEntity?> {
        return dao.getUserByIdFlow(userId)
    }

    fun enforceSessionPersistenceOnLaunch(context: Context) {
        val ctx = context.applicationContext
        appContext = ctx
        val prefs = ctx.getSharedPreferences(AUTH_PREFS, Context.MODE_PRIVATE)
        val rememberMe = prefs.getBoolean(KEY_REMEMBER_ME, true)
        if (!rememberMe && auth.currentUser != null) {
            auth.signOut()
        }
    }

    private fun saveRememberMe(rememberMe: Boolean) {
        appContext?.getSharedPreferences(AUTH_PREFS, Context.MODE_PRIVATE)
            ?.edit()
            ?.putBoolean(KEY_REMEMBER_ME, rememberMe)
            ?.apply()
    }

    suspend fun loginWithEmailResult(email: String, pass: String, rememberMe: Boolean): Result<Unit> {
        if (email.isBlank() || pass.isBlank()) {
            return Result.failure(IllegalArgumentException("Email and password are required."))
        }
        return try {
            auth.signInWithEmailAndPassword(email.trim(), pass).await()
            saveRememberMe(rememberMe)
            Result.success(Unit)
        } catch (e: FirebaseNetworkException) {
            Result.failure(IllegalStateException("Network unavailable. Check your internet connection.", e))
        } catch (e: FirebaseAuthInvalidUserException) {
            Result.failure(IllegalArgumentException("No account found with this email.", e))
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            Result.failure(IllegalArgumentException("Incorrect email or password.", e))
        } catch (e: Exception) {
            Result.failure(IllegalStateException(e.localizedMessage ?: "Sign in failed.", e))
        }
    }

    suspend fun loginWithEmail(email: String, pass: String, rememberMe: Boolean): Boolean {
        return loginWithEmailResult(email, pass, rememberMe).isSuccess
    }

    suspend fun signupResult(name: String, email: String, pass: String): Result<Unit> {
        if (name.isBlank() || email.isBlank() || pass.isBlank()) {
            return Result.failure(IllegalArgumentException("Name, email and password are required."))
        }
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email.trim(), pass).await()
            val uid = authResult.user?.uid ?: return Result.failure(IllegalStateException("Missing user ID after signup."))
            val newUser = UserEntity(
                id = uid,
                name = name.trim(),
                email = email.trim(),
                passwordHash = "",
                isGuest = false
            )
            dao.insertUser(newUser)
            saveRememberMe(true)
            Result.success(Unit)
        } catch (e: FirebaseNetworkException) {
            Result.failure(IllegalStateException("Network unavailable. Check your internet connection.", e))
        } catch (e: FirebaseAuthUserCollisionException) {
            Result.failure(IllegalArgumentException("An account with this email already exists.", e))
        } catch (e: FirebaseAuthWeakPasswordException) {
            Result.failure(IllegalArgumentException("Password is too weak (use at least 6 characters).", e))
        } catch (e: Exception) {
            Result.failure(IllegalStateException(e.localizedMessage ?: "Signup failed.", e))
        }
    }

    suspend fun signup(name: String, email: String, pass: String): Boolean {
        return signupResult(name, email, pass).isSuccess
    }

    suspend fun resetPasswordResult(email: String): Result<Unit> {
        if (email.isBlank()) {
            return Result.failure(IllegalArgumentException("Enter your email address first."))
        }
        return try {
            auth.sendPasswordResetEmail(email.trim()).await()
            Result.success(Unit)
        } catch (e: FirebaseNetworkException) {
            Result.failure(IllegalStateException("Network unavailable. Try again online.", e))
        } catch (e: FirebaseAuthInvalidUserException) {
            Result.failure(IllegalArgumentException("No account found for this email.", e))
        } catch (e: Exception) {
            Result.failure(IllegalStateException(e.localizedMessage ?: "Could not send reset email.", e))
        }
    }

    suspend fun resetPassword(email: String): Boolean {
        return resetPasswordResult(email).isSuccess
    }

    suspend fun googleSignIn(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val authResult = auth.signInWithCredential(credential).await()
        val uid = authResult.user?.uid ?: throw IllegalStateException("Google Sign-In failed: missing user ID")

        saveRememberMe(true)
        val user = dao.getUserById(uid)
        if (user == null) {
            dao.insertUser(
                UserEntity(
                    id = uid,
                    name = authResult.user?.displayName ?: "Google User",
                    email = authResult.user?.email ?: "user@gmail.com",
                    passwordHash = "",
                    isGuest = false
                )
            )
        }
    }

    suspend fun saveFcmToken(userId: String) {
        try {
            val token = com.google.firebase.messaging.FirebaseMessaging.getInstance().token.await()
            dao.saveFcmToken(userId, token)
        } catch (_: Exception) {}
    }

    suspend fun updateUser(user: UserEntity): Result<Unit> {
        return try {
            dao.updateUser(user)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() {
        saveRememberMe(true)
        auth.signOut()
    }

    companion object {
        private const val AUTH_PREFS = "auth_persistence_prefs"
        private const val KEY_REMEMBER_ME = "remember_me"
    }
}
