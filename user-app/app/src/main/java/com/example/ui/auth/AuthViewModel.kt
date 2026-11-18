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
    
    // Convert current user id to actual UserEntity dynamically
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

    fun loginWithEmail(email: String, pass: String, rememberMe: Boolean) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val success = repository.loginWithEmail(email, pass, rememberMe)
                if (success) {
                    _authState.value = AuthState.Success
                } else {
                    _authState.value = AuthState.Error("Invalid email or password")
                }
            } catch(e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun signup(name: String, email: String, pass: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val success = repository.signup(name, email, pass)
                if (success) {
                    _authState.value = AuthState.Success
                } else {
                    _authState.value = AuthState.Error("Email already in use")
                }
            } catch(e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Unknown error")
            }
        }
    }
    
    fun resetPassword(email: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val success = repository.resetPassword(email)
                if (success) {
                    _authState.value = AuthState.Success
                } else {
                    _authState.value = AuthState.Error("Email not found")
                }
            } catch(e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun googleSignIn(idToken: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                repository.googleSignIn(idToken)
                _authState.value = AuthState.Success
            } catch(e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun setError(message: String) {
        _authState.value = AuthState.Error(message)
    }

    fun logout() {
        repository.logout()
        _authState.value = AuthState.Idle
    }
    
    fun updateUserPreferences(user: UserEntity) {
        viewModelScope.launch {
            repository.updateUser(user)
            
            // Update FCM Topic Subscriptions
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
    data class Error(val message: String) : AuthState()
}
