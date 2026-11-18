package com.example.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

class AuthRepository(private val dao: DealFinderDao, private val session: SessionManager) {

    val currentUserId = session.currentUserId
    private val auth = FirebaseAuth.getInstance()

    fun getUserByIdFlow(userId: String): Flow<UserEntity?> {
        return dao.getUserByIdFlow(userId)
    }

    suspend fun loginWithEmail(email: String, pass: String, rememberMe: Boolean): Boolean {
        return try {
            auth.signInWithEmailAndPassword(email, pass).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun signup(name: String, email: String, pass: String): Boolean {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email, pass).await()
            val uid = authResult.user?.uid ?: return false
            val newUser = UserEntity(
                id = uid,
                name = name,
                email = email,
                passwordHash = "", // Not used
                isGuest = false
            )
            dao.insertUser(newUser)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    suspend fun resetPassword(email: String): Boolean {
        return try {
            auth.sendPasswordResetEmail(email).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun googleSignIn(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val authResult = auth.signInWithCredential(credential).await()
        val uid = authResult.user?.uid ?: throw IllegalStateException("Google Sign-In failed: missing user ID")

        val user = dao.getUserById(uid)
        if (user == null) {
            dao.insertUser(
                UserEntity(
                    id = uid,
                    name = authResult.user?.displayName ?: "Google User",
                    email = authResult.user?.email ?: "user@gmail.com",
                    passwordHash = "", // Oauth users may not have passwords
                    isGuest = false
                )
            )
        }
    }

    suspend fun saveFcmToken(userId: String) {
        try {
            val token = com.google.firebase.messaging.FirebaseMessaging.getInstance().token.await()
            dao.saveFcmToken(userId, token)
        } catch (e: Exception) {}
    }

    suspend fun updateUser(user: UserEntity) {
        dao.updateUser(user)
    }

    fun logout() {
        auth.signOut()
    }
}
