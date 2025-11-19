package com.stocknexus.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stocknexus.data.model.*
import com.stocknexus.data.repository.EnhancedAuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: EnhancedAuthRepository
) : ViewModel() {
    
    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()
    
    private val _currentScreen = MutableStateFlow<AuthScreen>(AuthScreen.SignIn)
    val currentScreen: StateFlow<AuthScreen> = _currentScreen.asStateFlow()
    
    private val _formState = MutableStateFlow(AuthFormState())
    val formState: StateFlow<AuthFormState> = _formState.asStateFlow()
    
    private val _rememberMe = MutableStateFlow(false)
    val rememberMe: StateFlow<Boolean> = _rememberMe.asStateFlow()
    
    init {
        checkAuthState()
    }
    
    fun checkAuthState() {
        viewModelScope.launch {
            try {
                android.util.Log.d("AuthViewModel", "checkAuthState: Starting...")
                val session = authRepository.getSession()
                if (session != null) {
                    android.util.Log.d("AuthViewModel", "checkAuthState: Session found for user: ${session.user.email}, role: ${session.user.role}")
                    val isValid = authRepository.isTokenValid()
                    android.util.Log.d("AuthViewModel", "checkAuthState: Token valid: $isValid")
                    if (isValid) {
                        android.util.Log.d("AuthViewModel", "checkAuthState: Setting state to Authenticated")
                        _authState.value = AuthState.Authenticated(session.user)
                    } else {
                        android.util.Log.w("AuthViewModel", "checkAuthState: Token invalid, clearing session")
                        authRepository.clearSession()
                        _authState.value = AuthState.Unauthenticated
                        _currentScreen.value = AuthScreen.SignIn
                    }
                } else {
                    android.util.Log.d("AuthViewModel", "checkAuthState: No session found")
                    _authState.value = AuthState.Unauthenticated
                    _currentScreen.value = AuthScreen.SignIn
                }
                
                // Load remembered email
                val rememberedEmail = authRepository.getRememberedEmail()
                if (rememberedEmail != null) {
                    _formState.value = _formState.value.copy(email = rememberedEmail)
                    _rememberMe.value = true
                }
            } catch (e: Exception) {
                android.util.Log.e("AuthViewModel", "checkAuthState: Exception occurred", e)
                _authState.value = AuthState.Error(e.message ?: "Authentication check failed")
                _currentScreen.value = AuthScreen.SignIn
            }
        }
    }
    
    fun updateFormState(newFormState: AuthFormState) {
        _formState.value = newFormState
    }
    
    fun updateRememberMe(remember: Boolean) {
        _rememberMe.value = remember
    }
    
    fun navigateToScreen(screen: AuthScreen) {
        _currentScreen.value = screen
        // Clear form errors when switching screens
        _formState.value = _formState.value.copy(
            errorMessage = null,
            emailError = null,
            passwordError = null,
            nameError = null
        )
    }
    
    fun signIn(email: String, password: String, rememberMe: Boolean) {
        viewModelScope.launch {
            try {
                android.util.Log.d("AuthViewModel", "Sign in started for email: $email")
                _formState.value = _formState.value.copy(isLoading = true, errorMessage = null)
                
                val result = authRepository.signIn(
                    SignInRequest(email, password),
                    rememberMe
                )
                
                android.util.Log.d("AuthViewModel", "Sign in result: ${result.isSuccess}")
                
                if (result.isSuccess) {
                    val authResponse = result.getOrThrow()
                    android.util.Log.d("AuthViewModel", "Sign in successful, user: ${authResponse.user.email}")
                    _authState.value = AuthState.Authenticated(authResponse.user)
                    _formState.value = AuthFormState() // Reset form
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Sign in failed"
                    android.util.Log.e("AuthViewModel", "Sign in failed: $error")
                    _formState.value = _formState.value.copy(
                        isLoading = false,
                        errorMessage = error
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("AuthViewModel", "Sign in exception", e)
                _formState.value = _formState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "An unexpected error occurred"
                )
            }
        }
    }
    
    fun signUp(request: SignUpRequest) {
        viewModelScope.launch {
            try {
                _formState.value = _formState.value.copy(isLoading = true, errorMessage = null)
                
                val result = authRepository.signUp(request)
                
                if (result.isSuccess) {
                    val authResponse = result.getOrThrow()
                    // For demo, auto-login after signup
                    _authState.value = AuthState.Authenticated(authResponse.user)
                    _formState.value = AuthFormState() // Reset form
                } else {
                    _formState.value = _formState.value.copy(
                        isLoading = false,
                        errorMessage = result.exceptionOrNull()?.message ?: "Sign up failed"
                    )
                }
            } catch (e: Exception) {
                _formState.value = _formState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "An unexpected error occurred"
                )
            }
        }
    }
    
    fun forgotPassword(email: String) {
        viewModelScope.launch {
            try {
                _formState.value = _formState.value.copy(isLoading = true, errorMessage = null)
                
                val result = authRepository.forgotPassword(PasswordResetRequest(email))
                
                if (result.isSuccess) {
                    _formState.value = _formState.value.copy(isLoading = false)
                    // Show success message or navigate back to sign in
                    _currentScreen.value = AuthScreen.SignIn
                } else {
                    _formState.value = _formState.value.copy(
                        isLoading = false,
                        errorMessage = result.exceptionOrNull()?.message ?: "Password reset failed"
                    )
                }
            } catch (e: Exception) {
                _formState.value = _formState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "An unexpected error occurred"
                )
            }
        }
    }
    
    fun signOut() {
        viewModelScope.launch {
            try {
                authRepository.signOut()
                _authState.value = AuthState.Unauthenticated
                _currentScreen.value = AuthScreen.SignIn
                _formState.value = AuthFormState()
                _rememberMe.value = false
            } catch (e: Exception) {
                // Even if sign out fails, clear local state
                _authState.value = AuthState.Unauthenticated
                _currentScreen.value = AuthScreen.SignIn
                _formState.value = AuthFormState()
            }
        }
    }
    
    fun refreshProfile() {
        viewModelScope.launch {
            try {
                val result = authRepository.refreshProfile()
                if (result.isSuccess) {
                    _authState.value = AuthState.Authenticated(result.getOrThrow())
                }
            } catch (e: Exception) {
                // Handle profile refresh error silently or show notification
            }
        }
    }
    
    fun clearError() {
        _formState.value = _formState.value.copy(errorMessage = null)
    }
}

class AuthViewModelFactory(
    private val authRepository: EnhancedAuthRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            return AuthViewModel(authRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}