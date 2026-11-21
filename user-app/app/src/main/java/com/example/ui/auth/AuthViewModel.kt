package com.example.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AuthRepository
import com.example.data.UserEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AuthViewModel(private val repository: AuthRepository) : ViewModel() {

    val currentUserId: StateFlow<String?> = repository.currentUserId

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val currentUser: StateFlow<UserEntity?> = currentUserId.flatMapLatest { id ->
        if (id != null) repository.getUserByIdFlow(id) else flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        viewModelScope.launch {
            currentUserId.collect { id ->
                if (id != null) {
                    repository.saveFcmToken(id)
                }
            }
        }
    }

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState = _authState.asStateFlow()

    private val _preferenceError = MutableStateFlow<String?>(null)
    val preferenceError = _preferenceError.asStateFlow()

    fun clearPreferenceError() {
        _preferenceError.value = null
    }

    fun loginWithEmail(email: String, pass: String, rememberMe: Boolean) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            repository.loginWithEmailResult(email, pass, rememberMe)
                .onSuccess { _authState.value = AuthState.Success }
                .onFailure { _authState.value = AuthState.Error(it.message ?: "Sign in failed") }
        }
    }

    fun signup(name: String, email: String, pass: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            repository.signupResult(name, email, pass)
                .onSuccess { _authState.value = AuthState.Success }
                .onFailure { _authState.value = AuthState.Error(it.message ?: "Signup failed") }
        }
    }

    fun resetPassword(email: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            repository.resetPasswordResult(email)
                .onSuccess { _authState.value = AuthState.PasswordResetSent(email.trim()) }
                .onFailure { _authState.value = AuthState.Error(it.message ?: "Could not send reset email") }
        }
    }

    fun googleSignIn(idToken: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                repository.googleSignIn(idToken)
                _authState.value = AuthState.Success
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Google sign-in failed")
            }
        }
    }

    fun setError(message: String) {
        _authState.value = AuthState.Error(message)
    }

    fun logout() {
        try {
            val messaging = com.google.firebase.messaging.FirebaseMessaging.getInstance()
            messaging.token.addOnSuccessListener { token ->
                com.example.service.MyFirebaseMessagingService.revokeTokenForCurrentUser(token)
            }
            listOf("new_flyers", "price_drops", "deal_expiry", "promotions", "coupon_expiry").forEach { topic ->
                messaging.unsubscribeFromTopic(topic)
            }
        } catch (_: Exception) {
            // Safe fallback in unit tests or devices without Play Services
        }
        repository.logout()
        _authState.value = AuthState.Idle
    }

    fun updateUserPreferences(user: UserEntity) {
        viewModelScope.launch {
            _preferenceError.value = null
            repository.updateUser(user)
                .onFailure { _preferenceError.value = it.message ?: "Could not save notification preferences" }

            try {
                val messaging = com.google.firebase.messaging.FirebaseMessaging.getInstance()
                if (user.notifyNewFlyers) messaging.subscribeToTopic("new_flyers")
                else messaging.unsubscribeFromTopic("new_flyers")

                if (user.notifyPriceDrops) messaging.subscribeToTopic("price_drops")
                else messaging.unsubscribeFromTopic("price_drops")

                if (user.notifyDealExpiry) messaging.subscribeToTopic("deal_expiry")
                else messaging.unsubscribeFromTopic("deal_expiry")

                if (user.notifyPromotions) messaging.subscribeToTopic("promotions")
                else messaging.unsubscribeFromTopic("promotions")

                if (user.notifyCouponExpiry) messaging.subscribeToTopic("coupon_expiry")
                else messaging.unsubscribeFromTopic("coupon_expiry")
            } catch (_: Exception) {}
        }
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }

    class Factory(private val repo: AuthRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return AuthViewModel(repo) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    data class PasswordResetSent(val email: String) : AuthState()
    data class Error(val message: String) : AuthState()
}
