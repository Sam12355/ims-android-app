package com.ims.android.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ims.android.data.api.ApiClient
import com.ims.android.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

val Context.authDataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_preferences")

class EnhancedAuthRepository(
    private val context: Context,
    private val apiClient: ApiClient
) {
    private val dataStore = context.authDataStore
    
    companion object {
        private val SESSION_KEY = stringPreferencesKey("session_info")
        private val USER_KEY = stringPreferencesKey("user_info")
        private val TOKEN_KEY = stringPreferencesKey("auth_token")
        private val REMEMBER_EMAIL_KEY = stringPreferencesKey("remember_email")
        private val NOTIFICATION_SETTINGS_KEY = stringPreferencesKey("notification_settings")
    }
    
    // Session management
    suspend fun saveSession(sessionInfo: SessionInfo) {
        dataStore.edit { preferences ->
            preferences[SESSION_KEY] = Json.encodeToString(sessionInfo)
            preferences[USER_KEY] = Json.encodeToString(sessionInfo.user)
            preferences[TOKEN_KEY] = sessionInfo.accessToken
        }
        apiClient.setToken(sessionInfo.accessToken)
    }
    
    suspend fun getSession(): SessionInfo? {
        return try {
            val sessionJson = dataStore.data.first()[SESSION_KEY]
            sessionJson?.let { Json.decodeFromString<SessionInfo>(it) }
        } catch (e: Exception) {
            null
        }
    }
    
    suspend fun getCurrentUser(): User? {
        return try {
            val userJson = dataStore.data.first()[USER_KEY]
            userJson?.let { Json.decodeFromString<User>(it) }
        } catch (e: Exception) {
            null
        }
    }
    
    fun getCurrentUserFlow(): Flow<User?> {
        return dataStore.data.map { preferences ->
            try {
                val userJson = preferences[USER_KEY]
                userJson?.let { Json.decodeFromString<User>(it) }
            } catch (e: Exception) {
                null
            }
        }
    }
    
    suspend fun clearSession() {
        dataStore.edit { preferences ->
            preferences.remove(SESSION_KEY)
            preferences.remove(USER_KEY)
            preferences.remove(TOKEN_KEY)
            preferences.remove(REMEMBER_EMAIL_KEY) // Clear remembered email on logout
        }
        apiClient.setToken(null)
    }
    
    // Remember email functionality
    suspend fun saveRememberEmail(email: String) {
        dataStore.edit { preferences ->
            preferences[REMEMBER_EMAIL_KEY] = email
        }
    }
    
    suspend fun getRememberedEmail(): String? {
        return dataStore.data.first()[REMEMBER_EMAIL_KEY]
    }
    
    suspend fun clearRememberedEmail() {
        dataStore.edit { preferences ->
            preferences.remove(REMEMBER_EMAIL_KEY)
        }
    }
    
    // Authentication methods
    suspend fun signIn(request: SignInRequest, rememberEmail: Boolean = false): Result<AuthResponse> {
        return try {
            android.util.Log.d("EnhancedAuthRepo", "Calling API signIn for: ${request.email}")
            val response = apiClient.signIn(request)
            
            android.util.Log.d("EnhancedAuthRepo", "API signIn successful, saving session")
            // Save session
            val sessionInfo = SessionInfo(
                accessToken = response.accessToken,
                refreshToken = response.refreshToken,
                user = response.user
            )
            saveSession(sessionInfo)
            
            // Save email if remember me is checked
            if (rememberEmail) {
                saveRememberEmail(request.email)
            } else {
                clearRememberedEmail()
            }
            
            android.util.Log.d("EnhancedAuthRepo", "Session saved successfully")
            Result.success(response)
        } catch (e: Exception) {
            android.util.Log.e("EnhancedAuthRepo", "Sign in failed", e)
            Result.failure(e)
        }
    }
    
    suspend fun signUp(request: SignUpRequest): Result<AuthResponse> {
        return try {
            val response = apiClient.signUp(request)
            
            // For demo purposes, auto-login after signup
            val sessionInfo = SessionInfo(
                accessToken = response.accessToken,
                refreshToken = response.refreshToken,
                user = response.user
            )
            saveSession(sessionInfo)
            
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun signOut(): Result<Unit> {
        return try {
            apiClient.signOut()
            clearSession()
            Result.success(Unit)
        } catch (e: Exception) {
            // Even if API call fails, clear local session
            clearSession()
            Result.success(Unit)
        }
    }
    
    suspend fun forgotPassword(request: PasswordResetRequest): Result<Unit> {
        return try {
            apiClient.forgotPassword(request)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun resetPassword(request: PasswordResetConfirm): Result<Unit> {
        return try {
            apiClient.resetPassword(request)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun requestEmailVerification(request: EmailVerificationRequest): Result<Unit> {
        return try {
            apiClient.requestEmailVerification(request)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun verifyEmail(request: EmailVerificationConfirm): Result<User> {
        return try {
            val updatedUser = apiClient.verifyEmail(request)
            
            // Update stored user info
            dataStore.edit { preferences ->
                preferences[USER_KEY] = Json.encodeToString(updatedUser)
            }
            
            Result.success(updatedUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun resendVerificationCode(request: ResendVerificationRequest): Result<Unit> {
        return try {
            apiClient.resendVerificationCode(request)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Profile management
    suspend fun updateProfile(updates: Map<String, Any?>): Result<User> {
        return try {
            val updatedProfile = apiClient.updateProfile(updates)
            
            // Convert Profile to User
            val user = User(
                id = updatedProfile.id,
                email = updatedProfile.email,
                name = updatedProfile.name,
                phone = updatedProfile.phone,
                photoUrl = updatedProfile.photoUrl,
                position = updatedProfile.position,
                role = updatedProfile.role,
                branchId = updatedProfile.branchId,
                branchContext = updatedProfile.branchContext,
                branchName = updatedProfile.branchName,
                branchLocation = updatedProfile.branchLocation,
                districtName = updatedProfile.districtName,
                regionName = updatedProfile.regionName,
                accessCount = updatedProfile.accessCount ?: 0,
                createdAt = updatedProfile.createdAt,
                updatedAt = updatedProfile.updatedAt,
                emailVerified = true,
                isActive = true,
                notificationSettings = updatedProfile.notificationSettings,
                stockAlertFrequencies = updatedProfile.stockAlertFrequencies,
                dailyScheduleTime = updatedProfile.dailyScheduleTime,
                weeklyScheduleDay = updatedProfile.weeklyScheduleDay,
                weeklyScheduleTime = updatedProfile.weeklyScheduleTime,
                monthlyScheduleDate = updatedProfile.monthlyScheduleDate,
                monthlyScheduleTime = updatedProfile.monthlyScheduleTime,
                eventReminderFrequencies = updatedProfile.eventReminderFrequencies,
                eventDailyScheduleTime = updatedProfile.eventDailyScheduleTime,
                eventWeeklyScheduleDay = updatedProfile.eventWeeklyScheduleDay,
                eventWeeklyScheduleTime = updatedProfile.eventWeeklyScheduleTime,
                eventMonthlyScheduleDate = updatedProfile.eventMonthlyScheduleDate,
                eventMonthlyScheduleTime = updatedProfile.eventMonthlyScheduleTime,
                softdrinkTrendsFrequencies = updatedProfile.softdrinkTrendsFrequencies,
                softdrinkTrendsDailyScheduleTime = updatedProfile.softdrinkTrendsDailyScheduleTime,
                softdrinkTrendsWeeklyScheduleDay = updatedProfile.softdrinkTrendsWeeklyScheduleDay,
                softdrinkTrendsWeeklyScheduleTime = updatedProfile.softdrinkTrendsWeeklyScheduleTime,
                softdrinkTrendsMonthlyScheduleDate = updatedProfile.softdrinkTrendsMonthlyScheduleDate,
                softdrinkTrendsMonthlyScheduleTime = updatedProfile.softdrinkTrendsMonthlyScheduleTime,
                assistantManagerStockInAccess = updatedProfile.assistantManagerStockInAccess
            )
            
            // Update stored user info
            dataStore.edit { preferences ->
                preferences[USER_KEY] = Json.encodeToString(user)
            }
            
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateNotificationSettings(notificationSettings: Map<String, Boolean>): Result<User> {
        return try {
            android.util.Log.d("EnhancedAuthRepository", "🔥 Updating notification settings: $notificationSettings")
            
            // Update database directly (via Supabase)
            val updatedProfile = apiClient.updateNotificationSettings(notificationSettings)
            
            android.util.Log.d("EnhancedAuthRepository", "✅ Database updated, notification_settings: ${updatedProfile.notificationSettings}")
            
            // Convert Profile to User with database values
            val user = User(
                id = updatedProfile.id,
                email = updatedProfile.email,
                name = updatedProfile.name,
                phone = updatedProfile.phone,
                photoUrl = updatedProfile.photoUrl,
                position = updatedProfile.position,
                role = updatedProfile.role,
                branchId = updatedProfile.branchId,
                branchContext = updatedProfile.branchContext,
                branchName = updatedProfile.branchName,
                branchLocation = updatedProfile.branchLocation,
                districtName = updatedProfile.districtName,
                regionName = updatedProfile.regionName,
                accessCount = updatedProfile.accessCount ?: 0,
                createdAt = updatedProfile.createdAt,
                updatedAt = updatedProfile.updatedAt,
                emailVerified = true,
                isActive = true,
                notificationSettings = updatedProfile.notificationSettings,
                stockAlertFrequencies = updatedProfile.stockAlertFrequencies,
                dailyScheduleTime = updatedProfile.dailyScheduleTime,
                weeklyScheduleDay = updatedProfile.weeklyScheduleDay,
                weeklyScheduleTime = updatedProfile.weeklyScheduleTime,
                monthlyScheduleDate = updatedProfile.monthlyScheduleDate,
                monthlyScheduleTime = updatedProfile.monthlyScheduleTime,
                eventReminderFrequencies = updatedProfile.eventReminderFrequencies,
                eventDailyScheduleTime = updatedProfile.eventDailyScheduleTime,
                eventWeeklyScheduleDay = updatedProfile.eventWeeklyScheduleDay,
                eventWeeklyScheduleTime = updatedProfile.eventWeeklyScheduleTime,
                eventMonthlyScheduleDate = updatedProfile.eventMonthlyScheduleDate,
                eventMonthlyScheduleTime = updatedProfile.eventMonthlyScheduleTime,
                softdrinkTrendsFrequencies = updatedProfile.softdrinkTrendsFrequencies,
                softdrinkTrendsDailyScheduleTime = updatedProfile.softdrinkTrendsDailyScheduleTime,
                softdrinkTrendsWeeklyScheduleDay = updatedProfile.softdrinkTrendsWeeklyScheduleDay,
                softdrinkTrendsWeeklyScheduleTime = updatedProfile.softdrinkTrendsWeeklyScheduleTime,
                softdrinkTrendsMonthlyScheduleDate = updatedProfile.softdrinkTrendsMonthlyScheduleDate,
                softdrinkTrendsMonthlyScheduleTime = updatedProfile.softdrinkTrendsMonthlyScheduleTime,
                assistantManagerStockInAccess = updatedProfile.assistantManagerStockInAccess
            )
            
            // Update stored user info AND cache notification settings
            dataStore.edit { preferences ->
                preferences[USER_KEY] = Json.encodeToString(user)
                // Save notification settings to DataStore as cache
                updatedProfile.notificationSettings?.let {
                    preferences[NOTIFICATION_SETTINGS_KEY] = Json.encodeToString(it)
                }
            }
            
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadProfileImage(userId: String, imageBytes: ByteArray): Result<String> {
        return apiClient.uploadProfileImage(userId, imageBytes)
    }
    
    suspend fun refreshProfile(): Result<User> {
        return try {
            val profile = apiClient.getProfile()
            
            // ALWAYS use database as primary source (like web app does)
            // Database notification_settings is the source of truth
            val finalNotificationSettings = profile.notificationSettings
            android.util.Log.d("EnhancedAuthRepository", "✅ Using DATABASE notification settings: $finalNotificationSettings")
            
            val user = User(
                id = profile.id,
                email = profile.email,
                name = profile.name,
                phone = profile.phone,
                photoUrl = profile.photoUrl,
                position = profile.position,
                role = profile.role,
                branchId = profile.branchId,
                branchContext = profile.branchContext,
                branchName = profile.branchName,
                branchLocation = profile.branchLocation,
                districtName = profile.districtName,
                regionName = profile.regionName,
                accessCount = profile.accessCount ?: 0,
                createdAt = profile.createdAt,
                updatedAt = profile.updatedAt,
                emailVerified = true, // Assume verified if profile is accessible
                isActive = true,
                notificationSettings = finalNotificationSettings,
                stockAlertFrequencies = profile.stockAlertFrequencies,
                dailyScheduleTime = profile.dailyScheduleTime,
                weeklyScheduleDay = profile.weeklyScheduleDay,
                weeklyScheduleTime = profile.weeklyScheduleTime,
                monthlyScheduleDate = profile.monthlyScheduleDate,
                monthlyScheduleTime = profile.monthlyScheduleTime,
                eventReminderFrequencies = profile.eventReminderFrequencies,
                eventDailyScheduleTime = profile.eventDailyScheduleTime,
                eventWeeklyScheduleDay = profile.eventWeeklyScheduleDay,
                eventWeeklyScheduleTime = profile.eventWeeklyScheduleTime,
                eventMonthlyScheduleDate = profile.eventMonthlyScheduleDate,
                eventMonthlyScheduleTime = profile.eventMonthlyScheduleTime,
                softdrinkTrendsFrequencies = profile.softdrinkTrendsFrequencies,
                softdrinkTrendsDailyScheduleTime = profile.softdrinkTrendsDailyScheduleTime,
                softdrinkTrendsWeeklyScheduleDay = profile.softdrinkTrendsWeeklyScheduleDay,
                softdrinkTrendsWeeklyScheduleTime = profile.softdrinkTrendsWeeklyScheduleTime,
                softdrinkTrendsMonthlyScheduleDate = profile.softdrinkTrendsMonthlyScheduleDate,
                softdrinkTrendsMonthlyScheduleTime = profile.softdrinkTrendsMonthlyScheduleTime,
                assistantManagerStockInAccess = profile.assistantManagerStockInAccess
            )
            
            // Update stored user info AND save notification settings to DataStore for quick access
            dataStore.edit { preferences ->
                preferences[USER_KEY] = Json.encodeToString(user)
                // Save notification settings to DataStore as cache
                finalNotificationSettings?.let {
                    preferences[NOTIFICATION_SETTINGS_KEY] = Json.encodeToString(it)
                }
            }
            
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Token validation and refresh
    suspend fun isTokenValid(): Boolean {
        return try {
            val session = getSession()
            if (session == null) {
                android.util.Log.d("EnhancedAuthRepo", "isTokenValid: No session found")
                return false
            }
            
            android.util.Log.d("EnhancedAuthRepo", "isTokenValid: Checking with API getProfile()")
            // Simple token validation - in production, you'd check expiration
            val profile = apiClient.getProfile()
            android.util.Log.d("EnhancedAuthRepo", "isTokenValid: Profile check SUCCESS - token is valid")
            true
        } catch (e: Exception) {
            android.util.Log.e("EnhancedAuthRepo", "isTokenValid: Profile check FAILED - token invalid", e)
            false
        }
    }
    
    suspend fun refreshTokenIfNeeded(): Result<Unit> {
        return try {
            val session = getSession()
            if (session == null) {
                return Result.failure(Exception("No session found"))
            }
            
            // For demo purposes, just validate existing token
            if (isTokenValid()) {
                Result.success(Unit)
            } else {
                clearSession()
                Result.failure(Exception("Token invalid"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}