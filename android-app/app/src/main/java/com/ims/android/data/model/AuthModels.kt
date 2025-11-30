package com.ims.android.data.model

import kotlinx.serialization.Serializable

@Serializable
data class SignInRequest(
    val email: String,
    val password: String
)

@Serializable
data class SignUpRequest(
    val email: String,
    val password: String,
    val name: String,
    @kotlinx.serialization.EncodeDefault
    val role: String = "staff",
    val branchId: String? = null
)

@Serializable
data class AuthResponse(
    val user: User,
    val profile: Profile,
    val accessToken: String,
    val refreshToken: String? = null,
    val expiresAt: String? = null
)

@Serializable
data class PasswordResetRequest(
    val email: String
)

@Serializable
data class PasswordResetConfirm(
    val email: String,
    val token: String,
    val newPassword: String
)

@Serializable
data class EmailVerificationRequest(
    val email: String
)

@Serializable
data class EmailVerificationConfirm(
    val email: String,
    val verificationCode: String
)

@Serializable
data class ResendVerificationRequest(
    val email: String
)

@Serializable
data class AuthError(
    val message: String,
    val code: String? = null,
    val details: Map<String, String>? = null
)

// Enhanced User model with complete profile information
@Serializable
data class User(
    val id: String,
    val email: String,
    val emailVerified: Boolean = false,
    val name: String = "",
    val phone: String? = null,
    val photoUrl: String? = null,
    val position: String? = null,
    val role: String = "staff",
    val branchId: String? = null,
    val branchContext: String? = null,
    val branchName: String? = null,
    val branchLocation: String? = null,
    val districtName: String? = null,
    val regionName: String? = null,
    val accessCount: Int = 0,
    val createdAt: String,
    val updatedAt: String? = null,
    val lastLoginAt: String? = null,
    val isActive: Boolean = true,
    val needsPasswordReset: Boolean = false,
    val notificationSettings: Map<String, Boolean>? = null,
    val stockAlertFrequencies: List<String>? = null,
    val dailyScheduleTime: String? = null,
    val weeklyScheduleDay: Int? = null,
    val weeklyScheduleTime: String? = null,
    val monthlyScheduleDate: Int? = null,
    val monthlyScheduleTime: String? = null,
    val eventReminderFrequencies: List<String>? = null,
    val eventDailyScheduleTime: String? = null,
    val eventWeeklyScheduleDay: Int? = null,
    val eventWeeklyScheduleTime: String? = null,
    val eventMonthlyScheduleDate: Int? = null,
    val eventMonthlyScheduleTime: String? = null,
    val softdrinkTrendsFrequencies: List<String>? = null,
    val softdrinkTrendsDailyScheduleTime: String? = null,
    val softdrinkTrendsWeeklyScheduleDay: Int? = null,
    val softdrinkTrendsWeeklyScheduleTime: String? = null,
    val softdrinkTrendsMonthlyScheduleDate: Int? = null,
    val softdrinkTrendsMonthlyScheduleTime: String? = null,
    val assistantManagerStockInAccess: Boolean? = null
)

// Session management
@Serializable
data class SessionInfo(
    val accessToken: String,
    val refreshToken: String? = null,
    val expiresAt: String? = null,
    val user: User
)

// Form validation states
data class AuthFormState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val name: String = "",
    val verificationCode: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val nameError: String? = null
) {
    val isSignInValid: Boolean
        get() = email.isNotBlank() && password.isNotBlank() && emailError == null && passwordError == null
    
    val isSignUpValid: Boolean
        get() = name.isNotBlank() && 
                email.isNotBlank() && 
                password.isNotBlank() && 
                confirmPassword.isNotBlank() &&
                password == confirmPassword &&
                password.length >= 6 &&
                emailError == null && 
                passwordError == null && 
                nameError == null
    
    val isEmailValid: Boolean
        get() = email.isNotBlank() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    
    val isPasswordValid: Boolean
        get() = password.length >= 6
    
    val doPasswordsMatch: Boolean
        get() = password == confirmPassword && confirmPassword.isNotBlank()
}

// Authentication flow states
sealed class AuthState {
    object Loading : AuthState()
    object Unauthenticated : AuthState()
    data class Authenticated(val user: User) : AuthState()
    data class EmailVerificationRequired(val email: String) : AuthState()
    data class PasswordResetRequired(val email: String) : AuthState()
    data class Error(val message: String) : AuthState()
}

// Screen navigation states
sealed class AuthScreen {
    object SignIn : AuthScreen()
    object SignUp : AuthScreen()
    object ForgotPassword : AuthScreen()
    object ResetPassword : AuthScreen()
    object EmailVerification : AuthScreen()
    object Splash : AuthScreen()
}

// Notification preferences matching web app
@Serializable
data class NotificationSettings(
    val stockAlertFrequencies: List<String> = emptyList(),
    val dailyScheduleTime: String? = null,
    val weeklyScheduleDay: Int? = null,
    val weeklyScheduleTime: String? = null,
    val monthlyScheduleDate: Int? = null,
    val monthlyScheduleTime: String? = null,
    val eventReminderFrequencies: List<String> = emptyList(),
    val eventDailyScheduleTime: String? = null,
    val eventWeeklyScheduleDay: Int? = null,
    val eventWeeklyScheduleTime: String? = null,
    val eventMonthlyScheduleDate: Int? = null,
    val eventMonthlyScheduleTime: String? = null,
    val softdrinkTrendsFrequencies: List<String> = emptyList(),
    val softdrinkTrendsDailyScheduleTime: String? = null,
    val softdrinkTrendsWeeklyScheduleDay: Int? = null,
    val softdrinkTrendsWeeklyScheduleTime: String? = null,
    val softdrinkTrendsMonthlyScheduleDate: Int? = null,
    val softdrinkTrendsMonthlyScheduleTime: String? = null
)