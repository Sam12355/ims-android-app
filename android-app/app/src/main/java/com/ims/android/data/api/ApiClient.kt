package com.ims.android.data.api

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ims.android.data.model.*
import com.ims.android.service.MorningReminderWorker
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.annotations.SupabaseInternal
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.suspendCancellableCoroutine
import com.google.firebase.messaging.FirebaseMessaging
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

// DataStore extension
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_prefs")

/**
 * API Client for backend communication
 * Connects to: https://stock-nexus-84-main-2-1.onrender.com/api
 */
@OptIn(SupabaseInternal::class)
class ApiClient private constructor(private val context: Context) {
    
    private val json = Json { 
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)  // Increased for Render cold start
        .readTimeout(60, TimeUnit.SECONDS)     // Increased for Render cold start
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
    
    private val baseUrl = "https://stock-nexus-84-main-2-1.onrender.com/api"
    private val supabaseUrl = "https://gvlaokxdgcnttyovdhku.supabase.co"
    private val serviceRoleKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imd2bGFva3hkZ2NudHR5b3ZkaGt1Iiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTc1OTEwMzA3MSwiZXhwIjoyMDc0Njc5MDcxfQ.yajgETQGHAFy5cJk_Om9SYxyy61r0MvT5TP93aqHZmA"
    
    // Supabase client for direct database access (using service_role key for privileged operations)
    private val supabase = createSupabaseClient(
        supabaseUrl = supabaseUrl,
        supabaseKey = serviceRoleKey
    ) {
        install(Postgrest)
        httpEngine = Android.create()
    }
    
    // Custom serializer to handle read_at being either boolean false or string timestamp
    object BooleanOrStringSerializer : kotlinx.serialization.KSerializer<String?> {
        override val descriptor = kotlinx.serialization.descriptors.PrimitiveSerialDescriptor("BooleanOrString", kotlinx.serialization.descriptors.PrimitiveKind.STRING)
        
        override fun deserialize(decoder: kotlinx.serialization.encoding.Decoder): String? {
            return try {
                val element = (decoder as? kotlinx.serialization.json.JsonDecoder)?.decodeJsonElement()
                when {
                    element is kotlinx.serialization.json.JsonPrimitive && element.isString -> element.content
                    element is kotlinx.serialization.json.JsonPrimitive && !element.isString -> null // boolean false or null
                    else -> null
                }
            } catch (e: Exception) {
                null
            }
        }
        
        override fun serialize(encoder: kotlinx.serialization.encoding.Encoder, value: String?) {
            if (value != null) encoder.encodeString(value) else encoder.encodeNull()
        }
    }
    
    @Serializable
    data class LoginRequest(val email: String, val password: String)
    
    @Serializable
    data class LoginResponse(
        val success: Boolean,
        val data: LoginData
    )
    
    @Serializable
    data class LoginData(
        val user: UserData,
        val token: String
    )
    
    @Serializable
    data class UserData(
        val id: String,
        val email: String,
        val name: String,
        val role: String,
        val branch_id: String? = null,
        val branch_name: String? = null,
        val created_at: String? = null,
        val updated_at: String? = null
    )
    
    @Serializable
    data class ICAEntry(
        val type: String,
        val amount: String,
        val timeOfDay: String
    )
    
    @Serializable
    data class ICADeliveryRequest(
        val userName: String,
        val entries: List<ICAEntry>,
        val submittedAt: String
    )

    @Serializable
    private data class NotificationUpdate(
        @SerialName("notification_settings")
        val notificationSettings: JsonObject
    )
    
    companion object {
        private const val TAG = "ApiClient"
        @Volatile
        private var INSTANCE: ApiClient? = null
        
        fun getInstance(context: Context): ApiClient {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ApiClient(context.applicationContext).also { INSTANCE = it }
            }
        }
        
        // DataStore keys
        private val ACCESS_TOKEN_KEY = stringPreferencesKey("access_token")
        private val REFRESH_TOKEN_KEY = stringPreferencesKey("refresh_token")
        private val USER_DATA_KEY = stringPreferencesKey("user_data")
        private val PROFILE_DATA_KEY = stringPreferencesKey("profile_data")
        private val MESSAGE_READ_CACHE_KEY = stringPreferencesKey("message_read_cache")
        private val MESSAGE_DELIVERED_CACHE_KEY = stringPreferencesKey("message_delivered_cache")
    }

    // Message status cache helpers (persist map<messageId, timestampString>)
    suspend fun saveMessageReadCache(readMap: Map<String, String>) {
        context.dataStore.edit { prefs ->
            prefs[MESSAGE_READ_CACHE_KEY] = json.encodeToString(readMap)
        }
    }

    suspend fun saveMessageDeliveredCache(deliveredMap: Map<String, String>) {
        context.dataStore.edit { prefs ->
            prefs[MESSAGE_DELIVERED_CACHE_KEY] = json.encodeToString(deliveredMap)
        }
    }

    suspend fun loadMessageReadCache(): Map<String, String> = withContext(Dispatchers.IO) {
        try {
            val mapStr = context.dataStore.data.first()[MESSAGE_READ_CACHE_KEY] ?: "{}"
            return@withContext json.decodeFromString(mapStr)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error loading message read cache", e)
            return@withContext emptyMap()
        }
    }

    suspend fun loadMessageDeliveredCache(): Map<String, String> = withContext(Dispatchers.IO) {
        try {
            val mapStr = context.dataStore.data.first()[MESSAGE_DELIVERED_CACHE_KEY] ?: "{}"
            return@withContext json.decodeFromString(mapStr)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error loading message delivered cache", e)
            return@withContext emptyMap()
        }
    }
    
    // Demo authentication methods
    suspend fun login(email: String, password: String): Result<AuthResponse> = withContext(Dispatchers.IO) {
        return@withContext try {
            android.util.Log.d(TAG, "Calling backend API login for: $email")
            
            // Call the backend API (same as web app)
            val requestBody = json.encodeToString(LoginRequest.serializer(), LoginRequest(email, password))
            val request = Request.Builder()
                .url("$baseUrl/auth/login")
                .post(requestBody.toRequestBody("application/json".toMediaType()))
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: throw Exception("Empty response")
            
            android.util.Log.d(TAG, "Backend response code: ${response.code}")
            android.util.Log.d(TAG, "Backend response body: $responseBody")
            
            if (!response.isSuccessful) {
                throw Exception("Login failed: ${response.code} - $responseBody")
            }
            
            val loginResponse = json.decodeFromString<LoginResponse>(responseBody)
            
            if (!loginResponse.success) {
                throw Exception("Login failed")
            }
            
            val userData = loginResponse.data.user
            
            // Create user object from backend response
            val user = User(
                id = userData.id,
                email = userData.email,
                name = userData.name,
                role = userData.role,
                createdAt = userData.created_at ?: System.currentTimeMillis().toString(),
                updatedAt = userData.updated_at ?: System.currentTimeMillis().toString()
            )
            
            val profile = Profile(
                id = userData.id,
                userId = userData.id,
                name = userData.name,
                email = userData.email,
                role = userData.role,
                branchId = userData.branch_id ?: "",
                branchName = userData.branch_name ?: "",
                createdAt = userData.created_at ?: System.currentTimeMillis().toString(),
                updatedAt = userData.updated_at ?: System.currentTimeMillis().toString()
            )
            
            val authResponse = AuthResponse(
                user = user,
                profile = profile,
                accessToken = loginResponse.data.token,
                refreshToken = null
            )
            
            // Save mock data
            saveAuthData(authResponse)
            
            Result.success(authResponse)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Login exception: ${e.message}")
            android.util.Log.e(TAG, "Exception type: ${e.javaClass.simpleName}")
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    suspend fun signup(request: SignUpRequest): Result<AuthResponse> = withContext(Dispatchers.IO) {
        return@withContext try {
            android.util.Log.d(TAG, "signup called for: ${request.email}")
            
            val requestBody = json.encodeToString(SignUpRequest.serializer(), request)
            android.util.Log.d(TAG, "Sign up request body: $requestBody")
            
            val httpRequest = Request.Builder()
                .url("$baseUrl/auth/register")
                .post(requestBody.toRequestBody("application/json".toMediaType()))
                .build()
            
            android.util.Log.d(TAG, "Making HTTP request to: $baseUrl/auth/register")
            
            val response = try {
                client.newCall(httpRequest).execute()
            } catch (e: java.net.UnknownHostException) {
                android.util.Log.e(TAG, "UnknownHostException: ${e.message}", e)
                throw Exception("Cannot connect to server. Please check your internet connection.")
            } catch (e: java.net.SocketTimeoutException) {
                android.util.Log.e(TAG, "SocketTimeoutException: ${e.message}", e)
                throw Exception("Connection timeout. Please try again.")
            } catch (e: java.io.IOException) {
                android.util.Log.e(TAG, "IOException: ${e.message}", e)
                throw Exception("Network error: ${e.message ?: "Unable to connect to server"}")
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Unexpected exception during HTTP call: ${e.javaClass.name} - ${e.message}", e)
                throw Exception("Connection failed: ${e.javaClass.simpleName} - ${e.message ?: "Unknown network error"}")
            }
            
            android.util.Log.d(TAG, "HTTP request completed, reading response body...")
            val responseBody = response.body?.string() ?: throw Exception("Empty response from server")
            
            android.util.Log.d(TAG, "Sign up response code: ${response.code}")
            android.util.Log.d(TAG, "Sign up response: $responseBody")
            
            if (!response.isSuccessful) {
                // Try to parse error message from response
                val errorMessage = try {
                    val errorJson = json.parseToJsonElement(responseBody).jsonObject
                    val msg = errorJson["message"]?.jsonPrimitive?.content 
                        ?: errorJson["error"]?.jsonPrimitive?.content
                        ?: "HTTP ${response.code}: ${response.message}"
                    android.util.Log.e(TAG, "Sign up failed with error from backend: $msg")
                    msg
                } catch (parseError: Exception) {
                    android.util.Log.e(TAG, "Could not parse error response, raw body: $responseBody")
                    "Sign up failed (HTTP ${response.code}): $responseBody"
                }
                throw Exception(errorMessage)
            }
            
            val signupResponse = json.decodeFromString<LoginResponse>(responseBody)
            
            if (!signupResponse.success) {
                throw Exception("Sign up failed: Server returned success=false")
            }
            
            val userData = signupResponse.data.user
            
            // Create user object from backend response
            val user = User(
                id = userData.id,
                email = userData.email,
                name = userData.name,
                role = userData.role,
                createdAt = userData.created_at ?: System.currentTimeMillis().toString(),
                updatedAt = userData.updated_at ?: System.currentTimeMillis().toString()
            )
            
            val profile = Profile(
                id = userData.id,
                userId = userData.id,
                name = userData.name,
                email = userData.email,
                role = userData.role,
                branchId = userData.branch_id ?: "",
                branchName = userData.branch_name ?: "",
                createdAt = userData.created_at ?: System.currentTimeMillis().toString(),
                updatedAt = userData.updated_at ?: System.currentTimeMillis().toString()
            )
            
            val authResponse = AuthResponse(
                user = user,
                profile = profile,
                accessToken = signupResponse.data.token,
                refreshToken = null
            )
            
            // Save auth data
            saveAuthData(authResponse)
            
            Result.success(authResponse)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Sign up exception: ${e.message}")
            android.util.Log.e(TAG, "Exception type: ${e.javaClass.simpleName}")
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    suspend fun logout() {
        clearAuthData()
    }
    
    // Authentication helper methods
    private suspend fun saveAuthData(authResponse: AuthResponse) {
        context.dataStore.edit { preferences ->
            preferences[ACCESS_TOKEN_KEY] = authResponse.accessToken
            authResponse.refreshToken?.let { 
                preferences[REFRESH_TOKEN_KEY] = it 
            }
            preferences[USER_DATA_KEY] = json.encodeToString(User.serializer(), authResponse.user)
            preferences[PROFILE_DATA_KEY] = json.encodeToString(Profile.serializer(), authResponse.profile)
        }
    }
    
    private suspend fun clearAuthData() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
    
    // Enhanced Authentication Methods for new auth system
    suspend fun signIn(request: SignInRequest): AuthResponse {
        // Use existing login logic but return enhanced response
        android.util.Log.d("ApiClient", "signIn called for: ${request.email}")
        val result = login(request.email, request.password)
        android.util.Log.d("ApiClient", "login result isSuccess: ${result.isSuccess}")
        if (result.isSuccess) {
            val authResponse = result.getOrThrow()
            android.util.Log.d("ApiClient", "Creating enhanced AuthResponse")
            return com.ims.android.data.model.AuthResponse(
                user = com.ims.android.data.model.User(
                    id = authResponse.user.id,
                    email = authResponse.user.email,
                    name = authResponse.profile.name,
                    role = authResponse.profile.role,
                    branchId = authResponse.profile.branchId,
                    branchName = authResponse.profile.branchName,
                    createdAt = authResponse.user.createdAt,
                    emailVerified = true,
                    isActive = true,
                    // Include notification settings from the profile so initial user has them
                    notificationSettings = authResponse.profile.notificationSettings
                ),
                profile = authResponse.profile,
                accessToken = authResponse.accessToken,
                refreshToken = authResponse.refreshToken
            )
        } else {
            val error = result.exceptionOrNull() ?: Exception("Login failed")
            android.util.Log.e("ApiClient", "login failed: ${error.message}")
            throw error
        }
    }
    
    suspend fun signUp(request: SignUpRequest): AuthResponse {
        // Use existing signup logic but return enhanced response
        val signupRequest = SignUpRequest(
            email = request.email,
            password = request.password,
            name = request.name,
            role = request.role,
            branchId = request.branchId
        )
        
        val result = signup(signupRequest)
        if (result.isSuccess) {
            val authResponse = result.getOrThrow()
            return com.ims.android.data.model.AuthResponse(
                user = com.ims.android.data.model.User(
                    id = authResponse.user.id,
                    email = authResponse.user.email,
                    name = request.name,
                    role = request.role,
                    branchId = request.branchId,
                    createdAt = authResponse.user.createdAt,
                    emailVerified = false, // New users need verification
                    isActive = true
                ),
                profile = authResponse.profile,
                accessToken = authResponse.accessToken,
                refreshToken = authResponse.refreshToken
            )
        } else {
            throw result.exceptionOrNull() ?: Exception("Signup failed")
        }
    }
    
    suspend fun signOut() {
        logout()
    }
    
    suspend fun forgotPassword(request: PasswordResetRequest) {
        // Simulate delay for demo
        kotlinx.coroutines.delay(1000)
        
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(request.email).matches()) {
            throw Exception("Invalid email format")
        }
        
        // In real implementation, this would send an email
        // For demo, we just simulate success
    }
    
    suspend fun resetPassword(request: PasswordResetConfirm) {
        kotlinx.coroutines.delay(1000)
        
        if (request.newPassword.length < 6) {
            throw Exception("Password must be at least 6 characters long")
        }
        
        // Simulate password reset success
    }
    
    suspend fun requestEmailVerification(request: EmailVerificationRequest) {
        kotlinx.coroutines.delay(1000)
        
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(request.email).matches()) {
            throw Exception("Invalid email format")
        }
        
        // Simulate sending verification email
    }
    
    suspend fun verifyEmail(request: EmailVerificationConfirm): com.ims.android.data.model.User {
        kotlinx.coroutines.delay(1000)
        
        if (request.verificationCode.length != 6) {
            throw Exception("Invalid verification code")
        }
        
        // Return updated user with verified email
        return com.ims.android.data.model.User(
            id = "demo-user-id",
            email = request.email,
            name = "Demo User",
            role = "staff",
            createdAt = "2024-01-01T00:00:00Z",
            emailVerified = true,
            isActive = true
        )
    }
    
    suspend fun resendVerificationCode(request: ResendVerificationRequest) {
        kotlinx.coroutines.delay(1000)
        // Simulate resending verification code
    }
    
    suspend fun updateFCMToken(fcmToken: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val token = getAccessToken() ?: return@withContext Result.failure(Exception("Not authenticated"))
            
            val requestBody = """{"fcm_token":"$fcmToken"}"""
                .toRequestBody("application/json".toMediaType())
            
            val request = Request.Builder()
                .url("$baseUrl/fcm/token")
                .header("Authorization", "Bearer $token")
                .header("Content-Type", "application/json")
                .post(requestBody)
                .build()
            
            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                android.util.Log.d(TAG, "✅ FCM token sent to backend successfully")
                Result.success(Unit)
            } else {
                android.util.Log.e(TAG, "❌ Failed to send FCM token: ${response.code}")
                Result.failure(Exception("Failed to update FCM token: ${response.code}"))
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ Error sending FCM token", e)
            Result.failure(e)
        }
    }

    /**
     * Synchronously obtain the current FCM token using the FirebaseTask callback bridge.
     */
    suspend fun getFCMTokenSync(): String? = withContext(Dispatchers.IO) {
        try {
            return@withContext suspendCancellableCoroutine { cont ->
                FirebaseMessaging.getInstance().token
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            cont.resume(task.result)
                        } else {
                            cont.resumeWithException(task.exception ?: Exception("Failed to get FCM token"))
                        }
                    }
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ getFCMTokenSync error", e)
            null
        }
    }

    /**
     * Register this device with the backend devices table so server can target/exclude it.
     */
    suspend fun registerDevice(deviceToken: String, deviceId: String? = null): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val access = getAccessToken() ?: return@withContext Result.failure(Exception("Not authenticated"))

            // Try to include user id if available
            val profile = try { getCurrentProfile() } catch (e: Exception) { null }
            val userId = profile?.id

            val payloadBuilder = buildJsonObject {
                userId?.let { put("user_id", it) }
                deviceId?.let { put("device_id", it) }
                put("device_token", deviceToken)
            }

            val requestBody = payloadBuilder.toString().toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("$baseUrl/devices/register")
                .header("Authorization", "Bearer $access")
                .header("Content-Type", "application/json")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            android.util.Log.d(TAG, "📡 POST /devices/register Response Code: ${response.code}")
            android.util.Log.d(TAG, "📄 Response Body: $responseBody")

            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to register device: ${response.code} - $responseBody"))
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ registerDevice error", e)
            Result.failure(e)
        }
    }
    
    suspend fun updateProfile(updates: Map<String, Any?>): Profile = withContext(Dispatchers.IO) {
        val token = getAccessToken() ?: throw Exception("Not authenticated")
        android.util.Log.d(TAG, "📸 updateProfile called with: $updates")
        
        try {
            val payloadElement = mapToJsonElement(updates)
            val payloadObject = (payloadElement as? JsonObject) ?: JsonObject(emptyMap())
            val requestBody = json.encodeToString(JsonObject.serializer(), payloadObject)
                .toRequestBody("application/json".toMediaType())
            
            android.util.Log.d(TAG, "📸 updateProfile request body: $requestBody")

            val request = Request.Builder()
                .url("$baseUrl/users/profile")
                .header("Authorization", "Bearer $token")
                .header("Content-Type", "application/json")
                .put(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            android.util.Log.d(TAG, "📸 updateProfile response: ${response.code} - $responseBody")

            if (response.isSuccessful) {
                android.util.Log.d(TAG, "📸 ✅ Backend update successful, fetching updated profile...")
                val updatedProfile = getProfile()
                android.util.Log.d(TAG, "📸 ✅ Profile refreshed with photo_url: ${updatedProfile.photoUrl}")
                return@withContext updatedProfile
            }
            
            // If we get a 400 error with "No fields to update", it might be because 
            // the backend doesn't recognize photo_url. Just return the current profile 
            // since the upload already succeeded.
            if (response.code == 400 && responseBody.contains("No fields to update")) {
                android.util.Log.d(TAG, "📸 ⚠️ Backend says 'No fields to update', but upload succeeded. Refreshing profile...")
                return@withContext getProfile()
            }
            
            throw Exception("Backend update failed: ${response.code} - $responseBody")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "📸 ❌ Error updating profile: ${e.message}", e)
            throw e
        }
    }
    
    suspend fun updateNotificationSettings(notificationSettings: Map<String, Boolean>): Profile = withContext(Dispatchers.IO) {
        val token = getAccessToken() ?: throw Exception("Not authenticated")
        android.util.Log.d(TAG, "🔥 updateNotificationSettings called with: $notificationSettings")
        
        try {
            // Build notification_settings JSON object
            val notificationSettingsJson = buildJsonObject {
                notificationSettings.forEach { (key, value) ->
                    put(key, value)
                }
            }
            
            // Build request body with notification_settings key
            val requestBody = buildJsonObject {
                put("notification_settings", notificationSettingsJson)
            }
            
            android.util.Log.d(TAG, "🔥 REQUEST BODY: $requestBody")
            
            // Call backend API endpoint (same as web app)
            val request = Request.Builder()
                .url("$baseUrl/users/settings")
                .put(requestBody.toString().toRequestBody("application/json".toMediaType()))
                .header("Authorization", "Bearer $token")
                .header("Content-Type", "application/json")
                .build()
            
            android.util.Log.d(TAG, "🔥 CALLING: PUT $baseUrl/users/settings")
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            
            android.util.Log.d(TAG, "🔥 RESPONSE: ${response.code} - $responseBody")
            
            if (!response.isSuccessful) {
                throw Exception("Failed to update notification settings: ${response.code} - $responseBody")
            }
            
            // Fetch fresh profile from backend
            val updatedProfile = getProfile()
            android.util.Log.d(TAG, "🔥 UPDATED PROFILE notification_settings: ${updatedProfile.notificationSettings}")
            updatedProfile
        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ Failed to update notification settings: ${e.message}", e)
            throw Exception("Failed to update notification settings: ${e.message}")
        }
    }
    
    private suspend fun getStoredProfile(): Profile? = withContext(Dispatchers.IO) {
        val dataStore = context.dataStore.data.first()
        val profileJson = dataStore[PROFILE_DATA_KEY]
        profileJson?.let {
            try {
                json.decodeFromString(Profile.serializer(), it)
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error parsing stored profile", e)
                null
            }
        }
    }
    
    suspend fun getProfile(): Profile = withContext(Dispatchers.IO) {
        val token = getAccessToken() ?: throw Exception("Not authenticated")

        val request = Request.Builder()
            .url("$baseUrl/auth/profile")
            .header("Authorization", "Bearer $token")
            .get()
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: throw Exception("Empty response")

        if (!response.isSuccessful) {
            throw Exception("Failed to fetch profile: ${response.code} - $responseBody")
        }

        android.util.Log.d(TAG, "Profile response body: $responseBody")
        
        val root = json.parseToJsonElement(responseBody).jsonObject
        val data = root["data"]?.jsonObject ?: throw Exception("Profile response missing data")

        fun JsonObject.string(key: String) = this[key]?.jsonPrimitive?.contentOrNull
        fun JsonObject.int(key: String) = this[key]?.jsonPrimitive?.intOrNull

        android.util.Log.d(TAG, "Profile phone field: ${data.string("phone")}")
        
        // Log raw notification settings from API
        android.util.Log.d(TAG, "🔔 Raw notification_settings from API: ${data["notification_settings"]}")
        
        val notificationSettingsJson = data["notification_settings"]?.takeIf { it !is kotlinx.serialization.json.JsonNull }?.jsonObject
        val notificationSettings = notificationSettingsJson?.mapNotNull { entry ->
            entry.value.jsonPrimitive.booleanOrNull?.let { entry.key to it }
        }?.toMap()
        
        android.util.Log.d(TAG, "🔔 Parsed notification settings: $notificationSettings")

        val profile = Profile(
            id = data["id"]?.jsonPrimitive?.content ?: "",
            userId = data.string("user_id"),
            name = data["name"]?.jsonPrimitive?.content ?: "",
            email = data["email"]?.jsonPrimitive?.content ?: "",
            phone = data.string("phone"),
            photoUrl = data.string("photo_url"),
            position = data.string("position"),
            role = data["role"]?.jsonPrimitive?.content ?: "staff",
            branchId = data.string("branch_id"),
            branchContext = data.string("branch_context"),
            branchName = data.string("branch_name"),
            branchLocation = data.string("branch_location") ?: data.string("branch_context"),
            districtName = data.string("district_name"),
            regionName = data.string("region_name"),
            regionId = data.string("region_id"),
            districtId = data.string("district_id"),
            lastAccess = data.string("last_access"),
            accessCount = data.int("access_count"),
            createdAt = data.string("created_at") ?: "",
            updatedAt = data.string("updated_at") ?: data.string("created_at") ?: "",
            notificationSettings = notificationSettings,
            stockAlertFrequencies = data["stock_alert_frequencies"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull },
            dailyScheduleTime = data.string("daily_schedule_time"),
            weeklyScheduleDay = data.int("weekly_schedule_day"),
            weeklyScheduleTime = data.string("weekly_schedule_time"),
            monthlyScheduleDate = data.int("monthly_schedule_date"),
            monthlyScheduleTime = data.string("monthly_schedule_time"),
            eventReminderFrequencies = data["event_reminder_frequencies"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull },
            eventDailyScheduleTime = data.string("event_daily_schedule_time"),
            eventWeeklyScheduleDay = data.int("event_weekly_schedule_day"),
            eventWeeklyScheduleTime = data.string("event_weekly_schedule_time"),
            eventMonthlyScheduleDate = data.int("event_monthly_schedule_date"),
            eventMonthlyScheduleTime = data.string("event_monthly_schedule_time"),
            softdrinkTrendsFrequencies = data["softdrink_trends_frequencies"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull },
            softdrinkTrendsDailyScheduleTime = data.string("softdrink_trends_daily_schedule_time"),
            softdrinkTrendsWeeklyScheduleDay = data.int("softdrink_trends_weekly_schedule_day"),
            softdrinkTrendsWeeklyScheduleTime = data.string("softdrink_trends_weekly_schedule_time"),
            softdrinkTrendsMonthlyScheduleDate = data.int("softdrink_trends_monthly_schedule_date"),
            softdrinkTrendsMonthlyScheduleTime = data.string("softdrink_trends_monthly_schedule_time"),
            assistantManagerStockInAccess = notificationSettingsJson?.get("assistant_manager_stock_in_access")?.jsonPrimitive?.booleanOrNull
        )
        
        android.util.Log.d(TAG, "Created profile with phone: ${profile.phone}")

        // Cache profile for offline access
        context.dataStore.edit { preferences ->
            preferences[PROFILE_DATA_KEY] = json.encodeToString(Profile.serializer(), profile)
        }

        profile
    }
    
    fun setToken(token: String?) {
        // For demo purposes, token management is handled in DataStore
        // In production, this would set Authorization header
    }
    
    // Demo stock and items methods (OLD - to be removed)
    suspend fun getStockDataOld(): Result<List<Stock>> {
        return try {
            // Mock stock data
            val mockStock = listOf(
                Stock(
                    id = "stock-1",
                    itemId = "item-1",
                    branchId = "branch-1",
                    currentQuantity = 50,
                    reservedQuantity = 5,
                    availableQuantity = 45,
                    lastUpdated = "2024-11-07T10:00:00Z"
                )
            )
            
            Result.success(mockStock)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getItems(): Result<List<Item>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val token = getAccessToken() ?: throw Exception("Not authenticated")
            
            val request = Request.Builder()
                .url("$baseUrl/items")
                .header("Authorization", "Bearer $token")
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            val body = response.body?.string()
            
            if (!response.isSuccessful) {
                android.util.Log.e("ApiClient", "Failed to get items: ${response.code} - $body")
                throw Exception("Failed to get items: ${response.message}")
            }
            
            android.util.Log.d("ApiClient", "Items response: $body")
            
            val jsonElement = json.parseToJsonElement(body ?: "{}")
            val dataArray = jsonElement.jsonObject["data"]?.jsonArray
            
            if (dataArray == null) {
                android.util.Log.e("ApiClient", "No data array in response")
                throw Exception("Invalid response format")
            }
            
            val items = dataArray.map { element ->
                json.decodeFromJsonElement(Item.serializer(), element)
            }
            android.util.Log.d("ApiClient", "Parsed ${items.size} items")
            
            Result.success(items)
        } catch (e: Exception) {
            android.util.Log.e("ApiClient", "Error fetching items", e)
            Result.failure(e)
        }
    }
    
    suspend fun createItem(itemData: Map<String, Any?>): Result<Item> = withContext(Dispatchers.IO) {
        return@withContext try {
            val token = getAccessToken() ?: throw Exception("Not authenticated")
            
            val requestBody = buildJsonObject {
                itemData.forEach { (key, value) ->
                    when (value) {
                        is String -> put(key, value)
                        is Int -> put(key, value)
                        is Double -> put(key, value)
                        is Boolean -> put(key, value)
                        null -> put(key, JsonNull)
                    }
                }
            }
            
            val httpRequest = Request.Builder()
                .url("$baseUrl/items")
                .header("Authorization", "Bearer $token")
                .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                .build()
            
            val response = client.newCall(httpRequest).execute()
            val body = response.body?.string()
            
            if (!response.isSuccessful) {
                android.util.Log.e("ApiClient", "Failed to create item: ${response.code} - $body")
                throw Exception("Failed to create item: ${response.message}")
            }
            
            val jsonElement = json.parseToJsonElement(body ?: "{}")
            val dataObject = jsonElement.jsonObject["data"]
            
            val item = json.decodeFromJsonElement(Item.serializer(), dataObject!!)
            Result.success(item)
        } catch (e: Exception) {
            android.util.Log.e("ApiClient", "Error creating item", e)
            Result.failure(e)
        }
    }
    
    suspend fun updateItem(itemId: String, itemData: Map<String, Any?>): Result<Item> = withContext(Dispatchers.IO) {
        return@withContext try {
            val token = getAccessToken() ?: throw Exception("Not authenticated")
            
            val requestBody = buildJsonObject {
                itemData.forEach { (key, value) ->
                    when (value) {
                        is String -> put(key, value)
                        is Int -> put(key, value)
                        is Double -> put(key, value)
                        is Boolean -> put(key, value)
                        null -> put(key, JsonNull)
                    }
                }
            }
            
            android.util.Log.d("ApiClient", "Updating item $itemId with data: ${requestBody.toString()}")
            
            val httpRequest = Request.Builder()
                .url("$baseUrl/items/$itemId")
                .header("Authorization", "Bearer $token")
                .put(requestBody.toString().toRequestBody("application/json".toMediaType()))
                .build()
            
            val response = client.newCall(httpRequest).execute()
            val body = response.body?.string()
            
            android.util.Log.d("ApiClient", "Update response: ${response.code} - $body")
            
            if (!response.isSuccessful) {
                android.util.Log.e("ApiClient", "Failed to update item: ${response.code} - $body")
                throw Exception("Failed to update item: ${response.message}")
            }
            
            val jsonElement = json.parseToJsonElement(body ?: "{}")
            val dataObject = jsonElement.jsonObject["data"]
            
            val item = json.decodeFromJsonElement(Item.serializer(), dataObject!!)
            Result.success(item)
        } catch (e: Exception) {
            android.util.Log.e("ApiClient", "Error updating item", e)
            Result.failure(e)
        }
    }
    
    suspend fun deleteItem(itemId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        return@withContext try {
            val token = getAccessToken() ?: throw Exception("Not authenticated")
            
            val httpRequest = Request.Builder()
                .url("$baseUrl/items/$itemId")
                .header("Authorization", "Bearer $token")
                .delete()
                .build()
            
            val response = client.newCall(httpRequest).execute()
            
            if (!response.isSuccessful) {
                val body = response.body?.string()
                android.util.Log.e("ApiClient", "Failed to delete item: ${response.code} - $body")
                throw Exception("Failed to delete item: ${response.message}")
            }
            
            Result.success(true)
        } catch (e: Exception) {
            android.util.Log.e("ApiClient", "Error deleting item", e)
            Result.failure(e)
        }
    }
    
    suspend fun getMoveoutLists(): Result<List<MoveoutList>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val token = getAccessToken() ?: throw Exception("Not authenticated")
            
            val request = Request.Builder()
                .url("$baseUrl/moveout-lists")
                .header("Authorization", "Bearer $token")
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            val body = response.body?.string()
            
            if (!response.isSuccessful) {
                android.util.Log.e("ApiClient", "Failed to get moveout lists: ${response.code} - $body")
                throw Exception("Failed to get moveout lists: ${response.message}")
            }
            
            android.util.Log.d("ApiClient", "Moveout lists response: $body")
            
            // Parse the wrapper response
            val jsonElement = json.parseToJsonElement(body ?: "{}")
            val dataArray = jsonElement.jsonObject["data"]?.jsonArray
            
            if (dataArray == null) {
                android.util.Log.e("ApiClient", "No data array in response")
                throw Exception("Invalid response format")
            }
            
            val lists = dataArray.map { element ->
                json.decodeFromJsonElement(MoveoutList.serializer(), element)
            }
            android.util.Log.d("ApiClient", "Parsed ${lists.size} moveout lists")
            
            Result.success(lists)
        } catch (e: Exception) {
            android.util.Log.e("ApiClient", "Error fetching moveout lists", e)
            Result.failure(e)
        }
    }
    
    suspend fun createMoveoutList(request: CreateMoveoutListRequest): Result<MoveoutList> = withContext(Dispatchers.IO) {
        return@withContext try {
            val token = getAccessToken() ?: throw Exception("Not authenticated")
            
            // Transform to match backend API (snake_case)
            val requestBody = buildJsonObject {
                put("title", request.title)
                request.description?.let { put("description", it) }
                putJsonArray("items") {
                    request.items.forEach { item ->
                        addJsonObject {
                            put("item_id", item.itemId)
                            put("item_name", item.itemName)
                            put("available_amount", item.availableAmount)
                            put("request_amount", item.requestAmount)
                            put("category", item.category)
                        }
                    }
                }
            }
            
            val httpRequest = Request.Builder()
                .url("$baseUrl/moveout-lists")
                .header("Authorization", "Bearer $token")
                .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                .build()
            
            val response = client.newCall(httpRequest).execute()
            val responseBody = response.body?.string() ?: throw Exception("Empty response")
            
            if (!response.isSuccessful) {
                android.util.Log.e(TAG, "Failed to create moveout list: ${response.code} - $responseBody")
                throw Exception("Failed to create moveout list: ${response.code}")
            }
            
            val apiResponse = json.decodeFromString<ApiResponse<MoveoutList>>(responseBody)
            android.util.Log.d(TAG, "Created moveout list successfully: ${apiResponse.data}")
            Result.success(apiResponse.data)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error creating moveout list", e)
            Result.failure(e)
        }
    }
    
    suspend fun processMoveoutItem(
        listId: String,
        itemId: String,
        quantity: Int,
        userName: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            val token = getAccessToken() ?: throw Exception("Not authenticated")
            
            val requestBody = buildJsonObject {
                put("itemId", itemId)
                put("quantity", quantity)
                put("userName", userName)
            }
            
            val httpRequest = Request.Builder()
                .url("$baseUrl/moveout-lists/$listId/process-item")
                .header("Authorization", "Bearer $token")
                .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                .build()
            
            val response = client.newCall(httpRequest).execute()
            val responseBody = response.body?.string() ?: throw Exception("Empty response")
            
            if (!response.isSuccessful) {
                android.util.Log.e(TAG, "Failed to process moveout item: ${response.code} - $responseBody")
                throw Exception("Failed to process moveout item: ${response.code}")
            }
            
            android.util.Log.d(TAG, "Processed moveout item successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error processing moveout item", e)
            Result.failure(e)
        }
    }
    
    // Calendar Events API
    suspend fun getCalendarEvents(): List<CalendarEvent> = withContext(Dispatchers.IO) {
        return@withContext try {
            val token = getAccessToken() ?: throw Exception("Not authenticated")
            
            val request = Request.Builder()
                .url("$baseUrl/calendar-events")
                .header("Authorization", "Bearer $token")
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            val body = response.body?.string()
            
            if (!response.isSuccessful) {
                android.util.Log.e("ApiClient", "Failed to get calendar events: ${response.code} - $body")
                return@withContext emptyList()
            }
            
            android.util.Log.d("ApiClient", "Calendar Events Response: $body")
            
            // Parse response: {"success":true,"data":[...]}
            val jsonElement = json.parseToJsonElement(body ?: "")
            val jsonObject = jsonElement.jsonObject
            val dataArray = jsonObject["data"]?.jsonArray
            
            dataArray?.mapNotNull { element ->
                try {
                    json.decodeFromJsonElement(CalendarEvent.serializer(), element)
                } catch (e: Exception) {
                    android.util.Log.e("ApiClient", "Error parsing calendar event: ${e.message}")
                    null
                }
            } ?: emptyList()
        } catch (e: Exception) {
            android.util.Log.e("ApiClient", "Error fetching calendar events", e)
            emptyList()
        }
    }
    
    suspend fun createCalendarEvent(
        title: String,
        description: String? = null,
        eventDate: String,
        eventType: String = "reorder",
        branchId: String? = null
    ): Result<CalendarEvent> = withContext(Dispatchers.IO) {
        return@withContext try {
            val token = getAccessToken() ?: throw Exception("Not authenticated")
            
            val requestBody = buildJsonObject {
                put("title", title)
                description?.let { put("description", it) }
                put("event_date", eventDate)
                put("event_type", eventType)
                branchId?.let { put("branch_id", it) }
            }
            
            android.util.Log.d("ApiClient", "Creating calendar event: $requestBody")
            
            val request = Request.Builder()
                .url("$baseUrl/calendar-events")
                .header("Authorization", "Bearer $token")
                .header("Content-Type", "application/json")
                .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                .build()
            
            val response = client.newCall(request).execute()
            val body = response.body?.string()
            
            if (!response.isSuccessful) {
                android.util.Log.e("ApiClient", "Failed to create calendar event: ${response.code} - $body")
                return@withContext Result.failure(Exception("Failed to create event: $body"))
            }
            
            android.util.Log.d("ApiClient", "Create Event Response: $body")
            
            // Parse response: {"success":true,"data":{...}}
            val jsonElement = json.parseToJsonElement(body ?: "")
            val jsonObject = jsonElement.jsonObject
            val dataObject = jsonObject["data"]?.jsonObject
            
            if (dataObject != null) {
                val event = json.decodeFromJsonElement(CalendarEvent.serializer(), dataObject)
                android.util.Log.d("ApiClient", "✅ Event created successfully: ${event.id}")
                Result.success(event)
            } else {
                Result.failure(Exception("Invalid response format"))
            }
        } catch (e: Exception) {
            android.util.Log.e("ApiClient", "Error creating calendar event", e)
            Result.failure(e)
        }
    }
    
    private fun formatEventDate(dateString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
            val date = inputFormat.parse(dateString)
            date?.let { outputFormat.format(it) } ?: dateString
        } catch (e: Exception) {
            dateString
        }
    }
    
    suspend fun getCurrentUser(): User? {
        return try {
            val userData = context.dataStore.data.first()[USER_DATA_KEY]
            userData?.let { json.decodeFromString(User.serializer(), it) }
        } catch (e: Exception) {
            null
        }
    }
    
    suspend fun getCurrentProfile(): Profile? {
        return try {
            val profileData = context.dataStore.data.first()[PROFILE_DATA_KEY]
            profileData?.let { json.decodeFromString(Profile.serializer(), it) }
        } catch (e: Exception) {
            null
        }
    }
    
    fun isLoggedIn(): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[ACCESS_TOKEN_KEY]?.isNotEmpty() == true
        }
    }
    
        suspend fun getAccessToken(): String? {
        return context.dataStore.data.first()[ACCESS_TOKEN_KEY]
    }
    
    // Dashboard API methods
    @Serializable
    data class ApiResponse<T>(
        val success: Boolean,
        val data: T
    )
    
    @Serializable
    data class StockItem(
        val id: String? = null,
        val item_id: String? = null,
        val current_quantity: Int = 0,
        val last_updated: String? = null,
        val items: ItemDetails? = null
    )
    
    @Serializable
    data class ItemDetails(
        val id: String? = null,
        val name: String? = null,
        val category: String? = null,
        val unit: String? = null,
        val threshold_level: Int = 0,
        val low_level: Int? = null,
        val critical_level: Int? = null,
        val branch_id: String? = null,
        val base_unit: String? = null,
        val enable_packaging: Boolean? = false,
        val packaging_unit: String? = null,
        val units_per_package: Int? = null,
        val image_url: String? = null
    )
    
    @Serializable
    data class StockReceipt(
        val id: String,
        val supplier_name: String,
        val receipt_file_name: String,
        val receipt_file_path: String? = null,
        val remarks: String? = null,
        val status: String, // 'pending', 'approved', 'rejected'
        val created_at: String,
        val reviewed_at: String? = null,
        val reviewed_by_name: String? = null,
        val submitted_by_name: String? = null
    )
    
    // Reports data classes
    @Serializable
    data class StockReportItem(
        val id: String,
        val name: String,
        val category: String,
        val current_quantity: Int,
        val threshold_level: Int,
        val status: String // 'critical', 'low', 'adequate'
    )
    
    @Serializable
    data class MovementReportItem(
        val id: String,
        val item_name: String,
        val movement_type: String, // 'in' or 'out'
        val quantity: Int,
        val created_at: String,
        val user_name: String? = null
    )
    
    @Serializable
    data class SoftDrinksReportResponse(
        val data: List<SoftDrinksWeekData>,
        val summary: SoftDrinksSummary?
    )
    
    @Serializable
    data class SoftDrinksWeekData(
        val week_start: String,
        val week_end: String,
        val total_stock_in: Int,
        val total_stock_out: Int,
        val total_net_change: Int,
        val overall_trend: String,
        val items: List<SoftDrinksItemData>
    )
    
    @Serializable
    data class SoftDrinksItemData(
        val item_name: String,
        val stock_in: Int,
        val stock_out: Int,
        val net_change: Int,
        val trend: String
    )
    
    @Serializable
    data class SoftDrinksSummary(
        val total_weeks: Int,
        val total_stock_in: Int,
        val total_stock_out: Int,
        val total_net_change: Int
    )
    
    // Analytics data classes
    @Serializable
    data class AnalyticsResponse(
        val totalItems: Int,
        val lowStockItems: Int,
        val activeUsers: Int,
        val stockMovements: Int,
        val items: List<AnalyticsItem>? = null,
        val movements: List<AnalyticsMovement>? = null
    )
    
    @Serializable
    data class AnalyticsItem(
        val category: String
    )
    
    @Serializable
    data class AnalyticsMovement(
        val created_at: String,
        val movement_type: String,
        val quantity: Int,
        val item_name: String? = null
    )
    
    @Serializable
    data class UsageAnalyticsItem(
        val period: String,
        val usage: Int
    )
    
    @Serializable
    data class StaffMember(
        val id: String,
        val user_id: String? = null,
        val email: String,
        val name: String,
        val phone: String? = null,
        val photo_url: String? = null,
        val position: String? = null,
        val role: String, // 'regional_manager', 'district_manager', 'manager', 'assistant_manager', 'staff'
        val branch_id: String? = null,
        val region_id: String? = null,
        val district_id: String? = null,
        val district_name: String? = null,
        val is_active: Boolean? = true,
        val last_access: String? = null,
        val access_count: Int = 0,
        val created_at: String? = null,
        val updated_at: String? = null
    )
    
    @Serializable
    data class ActivityLogResponse(
        val id: String,
        val action: String,
        val details: kotlinx.serialization.json.JsonElement? = null,
        val created_at: String,
        val user_id: String? = null,
        val user_name: String? = null,
        val user_email: String? = null,
        val entity_type: String? = null,
        val entity_id: String? = null,
        val profiles: ProfileInfo? = null
    )
    
    @Serializable
    data class ProfileInfo(
        val name: String
    )
    
    // Notifications API
    @Serializable
    private data class BasicResponse(
        val success: Boolean,
        val message: String? = null
    )
    
    @Serializable
    private data class NotificationsResponse(
        val success: Boolean,
        val data: List<NotificationData>
    )
    
    @Serializable
    private data class NotificationData(
        val id: String,
        val type: String? = null,
        val message: String? = null,
        @SerialName("created_at")
        val createdAt: String? = null,
        @SerialName("is_read")
        val isRead: Boolean? = false
    )
    
    suspend fun getNotifications(): Result<List<Notification>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val token = getAccessToken() ?: throw Exception("Not authenticated")
            
            val request = Request.Builder()
                .url("$baseUrl/notifications")
                .header("Authorization", "Bearer $token")
                .get()
                .build()
            
            android.util.Log.d("ApiClient", "🌐 GET $baseUrl/notifications")
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: throw Exception("Empty response")
            
            android.util.Log.d("ApiClient", "📡 Response Code: ${response.code}")
            android.util.Log.d("ApiClient", "📄 Response Body: $responseBody")
            
            if (!response.isSuccessful) {
                throw Exception("Failed to fetch notifications: ${response.code} - $responseBody")
            }
            
            val notificationsResponse = json.decodeFromString<NotificationsResponse>(responseBody)
            val notifications = notificationsResponse.data.map { data ->
                Notification(
                    id = data.id,
                    title = "",
                    message = data.message ?: "",
                    type = data.type ?: "general",
                    isRead = data.isRead ?: false,
                    createdAt = data.createdAt ?: "",
                    userId = ""
                )
            }
            
            Result.success(notifications)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error fetching notifications: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    suspend fun markNotificationAsRead(notificationId: String): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            val token = getAccessToken() ?: throw Exception("Not authenticated")
            
            android.util.Log.d(TAG, "Marking notification $notificationId as read")
            
            val request = Request.Builder()
                .url("$baseUrl/notifications/$notificationId/read")
                .header("Authorization", "Bearer $token")
                .patch(ByteArray(0).toRequestBody(null))
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            
            android.util.Log.d(TAG, "Mark as read response: ${response.code} - $responseBody")
            
            if (!response.isSuccessful) {
                throw Exception("Failed to mark notification as read: ${response.code} - $responseBody")
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error marking notification as read: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    @Serializable
    private data class CreateNotificationRequest(
        val user_id: String,
        val type: String,
        val message: String,
        val is_read: Boolean = false
    )
    
    suspend fun createNotification(userId: String, type: String, message: String): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            android.util.Log.d(TAG, "📢 Creating notification for user: $userId, type: $type")
            
            val notificationData = CreateNotificationRequest(
                user_id = userId,
                type = type,
                message = message,
                is_read = false
            )
            
            val jsonBody = json.encodeToString(CreateNotificationRequest.serializer(), notificationData)
            val requestBody = jsonBody.toRequestBody("application/json".toMediaType())
            
            val request = Request.Builder()
                .url("$supabaseUrl/rest/v1/notifications")
                .header("apikey", serviceRoleKey)
                .header("Authorization", "Bearer $serviceRoleKey")
                .header("Content-Type", "application/json")
                .header("Prefer", "return=minimal")
                .post(requestBody)
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            
            android.util.Log.d(TAG, "📢 Create notification response: ${response.code} - $responseBody")
            
            if (!response.isSuccessful) {
                throw Exception("Failed to create notification: ${response.code} - $responseBody")
            }
            
            android.util.Log.d(TAG, "📢 ✅ Notification created successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error creating notification: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Broadcasts a notification to ALL staff members.
     * Creates a database record for each user AND triggers FCM push notifications.
     * If created after 10:00 AM Swedish time, schedules a reminder for next day at 10:00 AM.
     */
    suspend fun broadcastNotificationToAllStaff(
        type: String, 
        title: String, 
        message: String,
        creatorName: String = "System"
    ): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            android.util.Log.d(TAG, "📢 Broadcasting notification to all staff: type=$type, creator=$creatorName")
            
            // Get current Swedish time (Europe/Stockholm)
            val stockholmZone = java.time.ZoneId.of("Europe/Stockholm")
            val nowInStockholm = java.time.ZonedDateTime.now(stockholmZone)
            val dateFormatter = java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' HH:mm")
            val creationDateStr = nowInStockholm.format(dateFormatter)
            
            // Enhanced message with creator and date
            val enhancedMessage = "$message\nCreated by: $creatorName\nDate: $creationDateStr"
            
            // Send FCM broadcast via backend API
            val fcmPayload = buildJsonObject {
                put("type", type)
                put("title", title)
                put("message", enhancedMessage)
                put("broadcast", true)
            }
            
            android.util.Log.d(TAG, "📢 Sending FCM broadcast to backend: $fcmPayload")
            
            val fcmRequest = Request.Builder()
                .url("$baseUrl/notifications/broadcast")
                .header("Authorization", "Bearer ${getAccessToken()}")
                .header("Content-Type", "application/json")
                .post(fcmPayload.toString().toRequestBody("application/json".toMediaType()))
                .build()
            
            val fcmResponse = client.newCall(fcmRequest).execute()
            val fcmResponseBody = fcmResponse.body?.string() ?: ""
            android.util.Log.d(TAG, "📢 FCM broadcast response: ${fcmResponse.code} - $fcmResponseBody")
            
            if (fcmResponse.isSuccessful) {
                android.util.Log.d(TAG, "📢 ✅ FCM broadcast sent successfully!")
            } else {
                android.util.Log.w(TAG, "📢 Backend broadcast failed: ${fcmResponse.code} - $fcmResponseBody")
            }
            
            // Schedule morning reminder if created after 10:00 AM Swedish time
            val tenAM = 10
            if (nowInStockholm.hour >= tenAM) {
                // Get staff for morning reminder scheduling
                val staffResult = getStaff()
                val staffList = staffResult.getOrNull() ?: emptyList()
                
                if (staffList.isNotEmpty()) {
                    val nextMorning = nowInStockholm
                        .plusDays(1)
                        .withHour(tenAM)
                        .withMinute(0)
                        .withSecond(0)
                    
                    val morningTitle = "📋 Reminder: $title"
                    val morningMessage = "$message\nCreated by: $creatorName on $creationDateStr"
                    
                    scheduleMorningFCMReminder(
                        type = type,
                        title = morningTitle,
                        message = morningMessage,
                        scheduledTime = nextMorning.toInstant().toEpochMilli(),
                        staffIds = staffList.map { it.id }
                    )
                    android.util.Log.d(TAG, "📢 Scheduled morning reminder for ${nextMorning}")
                }
            } else {
                android.util.Log.d(TAG, "📢 Created before 10 AM - no morning reminder scheduled")
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error broadcasting notification: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Schedules a morning FCM reminder using WorkManager
     */
    private fun scheduleMorningFCMReminder(
        type: String,
        title: String,
        message: String,
        scheduledTime: Long,
        staffIds: List<String>
    ) {
        try {
            val delay = scheduledTime - System.currentTimeMillis()
            if (delay <= 0) {
                android.util.Log.w(TAG, "Scheduled time is in the past, skipping")
                return
            }
            
            val inputData = androidx.work.Data.Builder()
                .putString("type", type)
                .putString("title", title)
                .putString("message", message)
                .putStringArray("staff_ids", staffIds.toTypedArray())
                .build()
            
            val workRequest = androidx.work.OneTimeWorkRequestBuilder<MorningReminderWorker>()
                .setInitialDelay(delay, java.util.concurrent.TimeUnit.MILLISECONDS)
                .setInputData(inputData)
                .addTag("morning_reminder")
                .build()
            
            androidx.work.WorkManager.getInstance(context)
                .enqueue(workRequest)
            
            android.util.Log.d(TAG, "📢 Morning reminder scheduled with delay: ${delay / 1000 / 60} minutes")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to schedule morning reminder: ${e.message}", e)
        }
    }
    
    /**
     * Triggers FCM notifications via Supabase Edge Function (if available)
     */
    private suspend fun triggerFCMViaEdgeFunction(type: String, title: String, message: String, userIds: List<String>) {
        try {
            val userIdsJson = userIds.joinToString(",") { "\"$it\"" }
            val payload = """
                {
                    "type": "$type",
                    "title": "$title", 
                    "message": "$message",
                    "user_ids": [$userIdsJson]
                }
            """.trimIndent()
            
            val request = Request.Builder()
                .url("$supabaseUrl/functions/v1/send-fcm-notification")
                .header("Authorization", "Bearer $serviceRoleKey")
                .header("Content-Type", "application/json")
                .post(payload.toRequestBody("application/json".toMediaType()))
                .build()
            
            val response = client.newCall(request).execute()
            android.util.Log.d(TAG, "📢 Edge function FCM response: ${response.code} - ${response.body?.string()}")
        } catch (e: Exception) {
            android.util.Log.w(TAG, "📢 Edge function FCM failed: ${e.message}")
        }
    }
    
    suspend fun uploadProfileImage(userId: String, imageBytes: ByteArray): Result<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            android.util.Log.d(TAG, "📸 Starting image upload for user: $userId")
            val fileName = "$userId/${System.currentTimeMillis()}.jpg"
            
            // Supabase Storage URL
            val storageUrl = "$supabaseUrl/storage/v1/object/avatars/$fileName"
            
            val requestBody = imageBytes.toRequestBody("image/jpeg".toMediaType())
            
            val request = Request.Builder()
                .url(storageUrl)
                .header("Authorization", "Bearer $serviceRoleKey")
                .header("apikey", serviceRoleKey)
                .header("x-upsert", "true") // Overwrite if exists
                .post(requestBody)
                .build()
                
            var response = client.newCall(request).execute()
            
            // If bucket not found, try to create it
            if (response.code == 404) {
                val errorBody = response.body?.string() ?: ""
                
                if (errorBody.contains("Bucket not found", ignoreCase = true)) {
                    android.util.Log.d(TAG, "📸 Bucket not found, creating 'avatars' bucket...")
                    createAvatarsBucket(serviceRoleKey)
                    
                    // Retry upload
                    val retryRequest = request.newBuilder().build()
                    response = client.newCall(retryRequest).execute()
                } else {
                    throw Exception("Upload failed: ${response.code} - $errorBody")
                }
            }
            
            if (!response.isSuccessful) {
                 val body = response.body?.string()
                 throw Exception("Upload failed: ${response.code} - $body")
            }
            
            // Construct public URL
            val publicUrl = "$supabaseUrl/storage/v1/object/public/avatars/$fileName"
            android.util.Log.d(TAG, "📸 ✅ Upload successful! URL: $publicUrl")
            
            // Update via Supabase RPC function (bypassing backend API which doesn't support photo_url)
            android.util.Log.d(TAG, "📸 Calling Supabase RPC to update photo_url...")
            try {
                val rpcBody = buildJsonObject {
                    put("user_id_param", userId)
                    put("new_photo_url", publicUrl)
                }
                
                val rpcRequest = Request.Builder()
                    .url("$supabaseUrl/rest/v1/rpc/update_user_photo")
                    .header("Authorization", "Bearer $serviceRoleKey")
                    .header("apikey", serviceRoleKey)
                    .header("Content-Type", "application/json")
                    .post(rpcBody.toString().toRequestBody("application/json".toMediaType()))
                    .build()
                    
                val rpcResponse = client.newCall(rpcRequest).execute()
                val rpcResponseBody = rpcResponse.body?.string() ?: ""
                
                android.util.Log.d(TAG, "📸 RPC update response: ${rpcResponse.code} - $rpcResponseBody")
                
                if (rpcResponse.isSuccessful) {
                    android.util.Log.d(TAG, "📸 ✅ RPC update successful!")
                } else if (rpcResponse.code == 404) {
                        // RPC function doesn't exist, try direct UPDATE with service role key
                        android.util.Log.d(TAG, "📸 RPC function not found, using direct UPDATE...")
                        val updateBody = buildJsonObject {
                            put("photo_url", publicUrl)
                        }
                        
                        val updateRequest = Request.Builder()
                            .url("$supabaseUrl/rest/v1/users?id=eq.$userId")
                            .header("Authorization", "Bearer $serviceRoleKey")
                            .header("apikey", serviceRoleKey)
                            .header("Content-Type", "application/json")
                            .header("Prefer", "return=minimal")
                            .patch(updateBody.toString().toRequestBody("application/json".toMediaType()))
                            .build()
                            
                        val updateResponse = client.newCall(updateRequest).execute()
                        val updateResponseBody = updateResponse.body?.string() ?: ""
                        
                        android.util.Log.d(TAG, "📸 Direct UPDATE response: ${updateResponse.code} - $updateResponseBody")
                        
                        if (updateResponse.isSuccessful) {
                            android.util.Log.d(TAG, "📸 ✅ Direct UPDATE successful!")
                        } else {
                            android.util.Log.e(TAG, "📸 ❌ Direct UPDATE failed: ${updateResponse.code}")
                        }
                } else {
                    android.util.Log.e(TAG, "📸 ⚠️ RPC update failed but upload succeeded")
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "📸 ⚠️ Update failed: ${e.message}", e)
            }
            
            Result.success(publicUrl)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "📸 ❌ Error uploading image: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun createAvatarsBucket(serviceRoleKey: String) {
        try {
            val requestBody = buildJsonObject {
                put("id", "avatars")
                put("name", "avatars")
                put("public", true)
                put("file_size_limit", 5242880) // 5MB
                putJsonArray("allowed_mime_types") {
                    add(JsonPrimitive("image/jpeg"))
                    add(JsonPrimitive("image/png"))
                    add(JsonPrimitive("image/gif"))
                }
            }

            val request = Request.Builder()
                .url("https://gvlaokxdgcnttyovdhku.supabase.co/storage/v1/bucket")
                .header("Authorization", "Bearer $serviceRoleKey")
                .header("apikey", serviceRoleKey)
                .header("Content-Type", "application/json")
                .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string()
            android.util.Log.d(TAG, "Create bucket response: ${response.code} - $body")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error creating bucket", e)
        }
    }

    suspend fun getStockData(): Result<List<StockItem>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val token = getAccessToken() ?: throw Exception("Not authenticated")
            
            val request = Request.Builder()
                .url("$baseUrl/stock")
                .header("Authorization", "Bearer $token")
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: throw Exception("Empty response")
            
            if (!response.isSuccessful) {
                throw Exception("Failed to fetch stock data: ${response.code}")
            }
            
            val apiResponse = json.decodeFromString<ApiResponse<List<StockItem>>>(responseBody)
            Result.success(apiResponse.data)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error fetching stock data", e)
            Result.failure(e)
        }
    }
    
    suspend fun getReceipts(): Result<List<StockReceipt>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val token = getAccessToken() ?: throw Exception("Not authenticated")
            
            val request = Request.Builder()
                .url("$baseUrl/receipts")
                .header("Authorization", "Bearer $token")
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: throw Exception("Empty response")
            
            if (!response.isSuccessful) {
                throw Exception("Failed to fetch receipts: ${response.code}")
            }
            
            val apiResponse = json.decodeFromString<ApiResponse<List<StockReceipt>>>(responseBody)
            Result.success(apiResponse.data)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error fetching receipts", e)
            Result.failure(e)
        }
    }

    suspend fun submitReceipt(
        supplierName: String,
        remarks: String,
        fileUri: Uri,
        fileName: String,
        context: Context
    ): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            val token = getAccessToken() ?: throw Exception("Not authenticated")
            
            // Create a temporary file from URI
            val inputStream = context.contentResolver.openInputStream(fileUri)
                ?: throw Exception("Could not open file")
            
            val tempFile = File(context.cacheDir, fileName)
            FileOutputStream(tempFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
            inputStream.close()
            
            // Determine media type from file extension
            val mediaType = when (tempFile.extension.lowercase()) {
                "pdf" -> "application/pdf"
                "jpg", "jpeg" -> "image/jpeg"
                "png" -> "image/png"
                else -> "application/octet-stream"
            }.toMediaType()
            
            // Build multipart request
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("supplier_name", supplierName)
                .addFormDataPart("remarks", remarks)
                .addFormDataPart(
                    "receipt",
                    fileName,
                    tempFile.asRequestBody(mediaType)
                )
                .build()
            
            val request = Request.Builder()
                .url("$baseUrl/receipts/submit")
                .header("Authorization", "Bearer $token")
                .post(requestBody)
                .build()
            
            val response = client.newCall(request).execute()
            
            // Clean up temp file
            tempFile.delete()
            
            if (!response.isSuccessful) {
                val errorBody = response.body?.string()
                throw Exception("Failed to submit receipt: ${response.code} - $errorBody")
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error submitting receipt", e)
            Result.failure(e)
        }
    }

    suspend fun createReceipt(receiptData: Map<String, String>): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            val token = getAccessToken() ?: throw Exception("Not authenticated")
            
            val jsonBody = Json.encodeToString(receiptData)
            val requestBody = jsonBody.toRequestBody("application/json".toMediaType())
            
            val request = Request.Builder()
                .url("$baseUrl/receipts/submit")
                .header("Authorization", "Bearer $token")
                .post(requestBody)
                .build()
            
            val response = client.newCall(request).execute()
            
            if (!response.isSuccessful) {
                val errorBody = response.body?.string()
                throw Exception("Failed to create receipt: ${response.code} - $errorBody")
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error creating receipt", e)
            Result.failure(e)
        }
    }

    suspend fun markMoveoutItemCompleted(listId: String, itemId: String): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            val token = getAccessToken() ?: throw Exception("Not authenticated")
            
            val jsonBody = Json.encodeToString(mapOf(
                "list_id" to listId,
                "item_id" to itemId,
                "status" to "completed"
            ))
            val requestBody = jsonBody.toRequestBody("application/json".toMediaType())
            
            val request = Request.Builder()
                .url("$baseUrl/moveout-lists/$listId/items/$itemId/complete")
                .header("Authorization", "Bearer $token")
                .post(requestBody)
                .build()
            
            val response = client.newCall(request).execute()
            
            if (!response.isSuccessful) {
                val errorBody = response.body?.string()
                throw Exception("Failed to mark item completed: ${response.code} - $errorBody")
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error marking item completed", e)
            Result.failure(e)
        }
    }
    
    // Reports endpoints
    suspend fun getStockReport(): Result<List<StockReportItem>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val token = getAccessToken() ?: throw Exception("Not authenticated")
            
            val request = Request.Builder()
                .url("$baseUrl/reports/stock")
                .header("Authorization", "Bearer $token")
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: throw Exception("Empty response")
            
            if (!response.isSuccessful) {
                throw Exception("Failed to fetch stock report: ${response.code}")
            }
            
            val apiResponse = json.decodeFromString<ApiResponse<List<StockReportItem>>>(responseBody)
            Result.success(apiResponse.data)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error fetching stock report", e)
            Result.failure(e)
        }
    }
    
    suspend fun getMovementsReport(): Result<List<MovementReportItem>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val token = getAccessToken() ?: throw Exception("Not authenticated")
            
            val request = Request.Builder()
                .url("$baseUrl/reports/movements")
                .header("Authorization", "Bearer $token")
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: throw Exception("Empty response")
            
            if (!response.isSuccessful) {
                throw Exception("Failed to fetch movements report: ${response.code}")
            }
            
            val apiResponse = json.decodeFromString<ApiResponse<List<MovementReportItem>>>(responseBody)
            Result.success(apiResponse.data)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error fetching movements report", e)
            Result.failure(e)
        }
    }
    
    suspend fun getSoftDrinksReport(weeks: Int = 4): Result<SoftDrinksReportResponse> = withContext(Dispatchers.IO) {
        return@withContext try {
            val token = getAccessToken() ?: throw Exception("Not authenticated")
            
            val request = Request.Builder()
                .url("$baseUrl/reports/softdrinks-weekly?weeks=$weeks")
                .header("Authorization", "Bearer $token")
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: throw Exception("Empty response")
            
            if (!response.isSuccessful) {
                throw Exception("Failed to fetch soft drinks report: ${response.code}")
            }
            
            val reportResponse = json.decodeFromString<SoftDrinksReportResponse>(responseBody)
            Result.success(reportResponse)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error fetching soft drinks report", e)
            Result.failure(e)
        }
    }
    
    // Analytics endpoints
    suspend fun getAnalyticsData(): Result<AnalyticsResponse> = withContext(Dispatchers.IO) {
        return@withContext try {
            val token = getAccessToken() ?: throw Exception("Not authenticated")
            
            val request = Request.Builder()
                .url("$baseUrl/analytics")
                .header("Authorization", "Bearer $token")
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: throw Exception("Empty response")
            
            if (!response.isSuccessful) {
                throw Exception("Failed to fetch analytics: ${response.code}")
            }
            
            val apiResponse = json.decodeFromString<ApiResponse<AnalyticsResponse>>(responseBody)
            Result.success(apiResponse.data)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error fetching analytics", e)
            Result.failure(e)
        }
    }
    
    suspend fun getItemUsageAnalytics(period: String = "daily", itemId: String? = null): Result<List<UsageAnalyticsItem>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val token = getAccessToken() ?: throw Exception("Not authenticated")
            
            val url = if (itemId != null) {
                "$baseUrl/analytics/item-usage/$period?itemId=$itemId"
            } else {
                "$baseUrl/analytics/item-usage/$period"
            }
            
            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $token")
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: throw Exception("Empty response")
            
            if (!response.isSuccessful) {
                throw Exception("Failed to fetch item usage analytics: ${response.code}")
            }
            
            val apiResponse = json.decodeFromString<ApiResponse<List<UsageAnalyticsItem>>>(responseBody)
            Result.success(apiResponse.data)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error fetching item usage analytics", e)
            Result.failure(e)
        }
    }
    
    suspend fun getStaff(): Result<List<StaffMember>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val token = getAccessToken() ?: throw Exception("Not authenticated")
            
            val request = Request.Builder()
                .url("$baseUrl/users/staff")
                .header("Authorization", "Bearer $token")
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: throw Exception("Empty response")
            
            if (!response.isSuccessful) {
                throw Exception("Failed to fetch staff: ${response.code}")
            }
            
            val apiResponse = json.decodeFromString<ApiResponse<List<StaffMember>>>(responseBody)
            Result.success(apiResponse.data)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error fetching staff", e)
            Result.failure(e)
        }
    }
    
    suspend fun getActivityLogs(): Result<List<ActivityLogResponse>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val token = getAccessToken() ?: throw Exception("Not authenticated")
            
            val request = Request.Builder()
                .url("$baseUrl/activity-logs?limit=10")
                .header("Authorization", "Bearer $token")
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: throw Exception("Empty response")
            
            if (!response.isSuccessful) {
                throw Exception("Failed to fetch activity logs: ${response.code}")
            }
            
            val apiResponse = json.decodeFromString<ApiResponse<List<ActivityLogResponse>>>(responseBody)
            Result.success(apiResponse.data)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error fetching activity logs", e)
            Result.failure(e)
        }
    }
    
    suspend fun getWeather(city: String): Result<WeatherData> = withContext(Dispatchers.IO) {
        return@withContext try {
            val token = getAccessToken() ?: throw Exception("Not authenticated")
            
            val encodedCity = java.net.URLEncoder.encode(city, "UTF-8")
            val weatherUrl = "$baseUrl/weather/current?location=$encodedCity"
            android.util.Log.d(TAG, "🌤️ Weather API URL: $weatherUrl")
            android.util.Log.d(TAG, "🌤️ Original city: $city, Encoded: $encodedCity")
            
            val request = Request.Builder()
                .url(weatherUrl)
                .header("Authorization", "Bearer $token")
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: throw Exception("Empty response")
            
            android.util.Log.d(TAG, "Weather API Response Code: ${response.code}")
            android.util.Log.d(TAG, "Weather API Response: $responseBody")
            
            if (!response.isSuccessful) {
                android.util.Log.e(TAG, "Weather API failed with code: ${response.code}")
                throw Exception("Failed to fetch weather: ${response.code}")
            }
            
            @Serializable
            data class WeatherResponse(
                val success: Boolean,
                val data: WeatherData
            )
            
            val weatherResponse = json.decodeFromString<WeatherResponse>(responseBody)
            android.util.Log.d(TAG, "Weather parsed successfully: ${weatherResponse.data}")
            Result.success(weatherResponse.data)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error fetching weather", e)
            // Return default weather on error
            Result.success(
                WeatherData(
                    temperature = 15.0,
                    condition = "Clear sky",
                    location = city,
                    humidity = 70,
                    windSpeed = 10.0
                )
            )
        }
    }
    
    suspend fun getBranches(districtId: String? = null): Result<List<Branch>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val token = getAccessToken() ?: throw Exception("Not authenticated")
            
            val url = if (districtId != null) {
                "$baseUrl/branches?district_id=$districtId"
            } else {
                "$baseUrl/branches"
            }
            
            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $token")
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: throw Exception("Empty response")
            
            if (!response.isSuccessful) {
                throw Exception("Failed to fetch branches: ${response.code}")
            }
            
            @Serializable
            data class BranchesResponse(
                val success: Boolean,
                val data: List<Branch>
            )
            
            val branchesResponse = json.decodeFromString<BranchesResponse>(responseBody)
            Result.success(branchesResponse.data)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error fetching branches", e)
            Result.failure(e)
        }
    }
    
    suspend fun submitICADelivery(
        userName: String,
        entries: List<com.ims.android.ui.screens.ICADeliveryEntry>
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val token = getAccessToken() ?: throw Exception("Not authenticated")
            
            val icaEntries = entries.map { entry ->
                ICAEntry(
                    type = entry.type,
                    amount = entry.amount,
                    timeOfDay = entry.timeOfDay
                )
            }
            
            val requestBody = ICADeliveryRequest(
                userName = userName,
                entries = icaEntries,
                submittedAt = java.time.Instant.now().toString()
            )
            
            val requestJson = json.encodeToString(ICADeliveryRequest.serializer(), requestBody)
            android.util.Log.d(TAG, "ICA Delivery Request JSON: $requestJson")
            
            val body = requestJson.toRequestBody("application/json".toMediaType())
            
            val request = Request.Builder()
                .url("$baseUrl/ica-delivery")
                .header("Authorization", "Bearer $token")
                .post(body)
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            
            android.util.Log.d(TAG, "ICA Delivery Response Code: ${response.code}")
            android.util.Log.d(TAG, "ICA Delivery Response Body: $responseBody")
            
            if (!response.isSuccessful) {
                // Try to parse error message
                val errorMessage = try {
                    @Serializable
                    data class ErrorResponse(val error: String? = null, val message: String? = null, val duplicate: Boolean? = false)
                    val errorResponse = json.decodeFromString<ErrorResponse>(responseBody)
                    errorResponse.error ?: errorResponse.message ?: "Failed to submit: ${response.code}"
                } catch (e: Exception) {
                    responseBody.ifEmpty { "Failed to submit: ${response.code}" }
                }
                throw Exception(errorMessage)
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error submitting ICA delivery: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    @Serializable
    data class ICADeliveryRecord(
        val id: Int,
        val user_name: String,
        val type: String,
        val amount: Int,
        val time_of_day: String,
        val submitted_at: String
    )
    
    suspend fun getICADeliveryRecords(
        startDate: String? = null,
        endDate: String? = null
    ): Result<List<ICADeliveryRecord>> = withContext(Dispatchers.IO) {
        try {
            val token = getAccessToken() ?: throw Exception("Not authenticated")
            
            var url = "$baseUrl/ica-delivery"
            val params = mutableListOf<String>()
            if (startDate != null && startDate.isNotBlank()) {
                params.add("startDate=$startDate")
            }
            if (endDate != null && endDate.isNotBlank()) {
                params.add("endDate=$endDate")
            }
            if (params.isNotEmpty()) {
                url += "?" + params.joinToString("&")
            }
            
            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $token")
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: throw Exception("Empty response")
            
            android.util.Log.d(TAG, "ICA Delivery List URL: $url")
            android.util.Log.d(TAG, "ICA Delivery List Response Code: ${response.code}")
            android.util.Log.d(TAG, "ICA Delivery List Response Body: $responseBody")
            
            if (!response.isSuccessful) {
                throw Exception("Failed to fetch ICA delivery records: ${response.code} - $responseBody")
            }
            
            val records = json.decodeFromString<List<ICADeliveryRecord>>(responseBody)
            Result.success(records)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error fetching ICA delivery records", e)
            Result.failure(e)
        }
    }
    
    suspend fun deleteICADeliveryRecord(recordId: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val token = getAccessToken() ?: throw Exception("Not authenticated")
            
            val request = Request.Builder()
                .url("$baseUrl/ica-delivery/$recordId")
                .header("Authorization", "Bearer $token")
                .delete()
                .build()
            
            val response = client.newCall(request).execute()
            
            if (!response.isSuccessful) {
                throw Exception("Failed to delete ICA delivery record: ${response.code}")
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error deleting ICA delivery record", e)
            Result.failure(e)
        }
    }
    
    // Stock Out methods
    suspend fun updateStockQuantity(
        itemId: String,
        movementType: String,
        quantity: Int,
        reason: String? = null,
        unitType: String = "base",
        unitQuantity: Int = quantity,
        unitLabel: String = "piece"
    ): Result<StockItem> = withContext(Dispatchers.IO) {
        return@withContext try {
            val token = getAccessToken() ?: throw Exception("Not authenticated")
            
            val requestBody = buildJsonObject {
                put("item_id", itemId)
                put("movement_type", movementType)
                put("quantity", quantity)
                if (reason != null) put("reason", reason)
                put("unit_type", unitType)
                put("original_quantity", unitQuantity)
                put("unit_label", unitLabel)
            }
            
            val body = requestBody.toString().toRequestBody("application/json".toMediaType())
            
            val request = Request.Builder()
                .url("$baseUrl/stock/movement")
                .header("Authorization", "Bearer $token")
                .post(body)
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: throw Exception("Empty response")
            
            if (!response.isSuccessful) {
                throw Exception("Failed to update stock: ${response.code} - $responseBody")
            }
            
            val apiResponse = json.decodeFromString<ApiResponse<StockItem>>(responseBody)
            Result.success(apiResponse.data)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error updating stock quantity", e)
            Result.failure(e)
        }
    }
    
    suspend fun initializeStock(): Result<InitializeStockResponse> = withContext(Dispatchers.IO) {
        return@withContext try {
            val token = getAccessToken() ?: throw Exception("Not authenticated")
            
            val request = Request.Builder()
                .url("$baseUrl/stock/initialize")
                .header("Authorization", "Bearer $token")
                .post("{}".toRequestBody("application/json".toMediaType()))
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: throw Exception("Empty response")
            
            if (!response.isSuccessful) {
                throw Exception("Failed to initialize stock: ${response.code}")
            }
            
            val apiResponse = json.decodeFromString<ApiResponse<InitializeStockResponse>>(responseBody)
            Result.success(apiResponse.data)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error initializing stock", e)
            Result.failure(e)
        }
    }
    
    @Serializable
    data class InitializeStockResponse(
        val initialized: Int = 0,
        val message: String? = null
    )

    // Staff Management Functions
    suspend fun createStaff(staffData: Map<String, Any?>): Result<StaffMember> = withContext(Dispatchers.IO) {
        return@withContext try {
            val token = getAccessToken() ?: throw Exception("Not authenticated")
            
            val jsonData = buildJsonObject {
                staffData.forEach { (key, value) ->
                    when (value) {
                        null -> put(key, JsonNull)
                        is String -> put(key, value)
                        is Int -> put(key, value)
                        is Boolean -> put(key, value)
                        else -> put(key, value.toString())
                    }
                }
            }
            
            val body = jsonData.toString().toRequestBody("application/json".toMediaType())
            
            val request = Request.Builder()
                .url("$baseUrl/users/staff")
                .header("Authorization", "Bearer $token")
                .post(body)
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: throw Exception("Empty response")
            
            if (!response.isSuccessful) {
                throw Exception("Failed to create staff: ${response.code} - $responseBody")
            }
            
            val apiResponse = json.decodeFromString<ApiResponse<StaffMember>>(responseBody)
            Result.success(apiResponse.data)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error creating staff", e)
            Result.failure(e)
        }
    }

    suspend fun updateStaff(staffId: String, staffData: Map<String, Any?>): Result<StaffMember> = withContext(Dispatchers.IO) {
        return@withContext try {
            val token = getAccessToken() ?: throw Exception("Not authenticated")
            
            val jsonData = buildJsonObject {
                staffData.forEach { (key, value) ->
                    when (value) {
                        null -> put(key, JsonNull)
                        is String -> put(key, value)
                        is Int -> put(key, value)
                        is Boolean -> put(key, value)
                        else -> put(key, value.toString())
                    }
                }
            }
            
            val body = jsonData.toString().toRequestBody("application/json".toMediaType())
            
            val request = Request.Builder()
                .url("$baseUrl/users/staff/$staffId")
                .header("Authorization", "Bearer $token")
                .put(body)
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: throw Exception("Empty response")
            
            if (!response.isSuccessful) {
                throw Exception("Failed to update staff: ${response.code} - $responseBody")
            }
            
            val apiResponse = json.decodeFromString<ApiResponse<StaffMember>>(responseBody)
            Result.success(apiResponse.data)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error updating staff", e)
            Result.failure(e)
        }
    }

    suspend fun deleteStaff(staffId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        return@withContext try {
            val token = getAccessToken() ?: throw Exception("Not authenticated")
            
            val request = Request.Builder()
                .url("$baseUrl/users/staff/$staffId")
                .header("Authorization", "Bearer $token")
                .delete()
                .build()
            
            val response = client.newCall(request).execute()
            
            if (!response.isSuccessful) {
                throw Exception("Failed to delete staff: ${response.code}")
            }
            
            Result.success(true)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error deleting staff", e)
            Result.failure(e)
        }
    }
    
    suspend fun activateStaff(staffId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        return@withContext try {
            val token = getAccessToken() ?: throw Exception("Not authenticated")
            
            val requestBody = json.encodeToString(
                kotlinx.serialization.json.buildJsonObject {
                    put("is_active", true)
                }
            )
            
            val request = Request.Builder()
                .url("$baseUrl/users/staff/$staffId")
                .header("Authorization", "Bearer $token")
                .put(requestBody.toRequestBody("application/json".toMediaType()))
                .build()
            
            val response = client.newCall(request).execute()
            
            if (!response.isSuccessful) {
                throw Exception("Failed to activate staff: ${response.code}")
            }
            
            Result.success(true)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error activating staff", e)
            Result.failure(e)
        }
    }

    // Region & District Functions
    suspend fun getRegions(): Result<List<Region>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val token = getAccessToken() ?: throw Exception("Not authenticated")
            
            val request = Request.Builder()
                .url("$baseUrl/regions")
                .header("Authorization", "Bearer $token")
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: throw Exception("Empty response")
            
            if (!response.isSuccessful) {
                throw Exception("Failed to fetch regions: ${response.code}")
            }
            
            val apiResponse = json.decodeFromString<ApiResponse<List<Region>>>(responseBody)
            Result.success(apiResponse.data)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error fetching regions", e)
            Result.failure(e)
        }
    }

    suspend fun getDistricts(regionId: String? = null): Result<List<District>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val token = getAccessToken() ?: throw Exception("Not authenticated")
            
            val url = if (regionId != null) {
                "$baseUrl/districts?region_id=$regionId"
            } else {
                "$baseUrl/districts"
            }
            
            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $token")
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: throw Exception("Empty response")
            
            if (!response.isSuccessful) {
                throw Exception("Failed to fetch districts: ${response.code}")
            }
            
            val apiResponse = json.decodeFromString<ApiResponse<List<District>>>(responseBody)
            Result.success(apiResponse.data)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error fetching districts", e)
            Result.failure(e)
        }
    }

    @Serializable
    data class Region(
        val id: String,
        val name: String,
        val description: String? = null,
        val created_at: String? = null
    )

    @Serializable
    data class District(
        val id: String,
        val name: String,
        val region_id: String,
        val description: String? = null,
        val created_at: String? = null
    )
    
    // Helper function to convert Map to JsonElement
    private fun mapToJsonElement(value: Any?): JsonElement = when (value) {
        null -> JsonNull
        is String -> JsonPrimitive(value)
        is Number -> JsonPrimitive(value)
        is Boolean -> JsonPrimitive(value)
        is Map<*, *> -> JsonObject(value.mapNotNull { (key, v) ->
            (key as? String)?.let { it to mapToJsonElement(v) }
        }.toMap())
        is Iterable<*> -> JsonArray(value.map { mapToJsonElement(it) })
        else -> JsonPrimitive(value.toString())
    }

    // Messaging API - TEMPORARY LOCAL STORAGE UNTIL BACKEND SUPPORTS MESSAGES
    suspend fun sendMessage(receiverId: String, content: String): Result<Message> = withContext(Dispatchers.IO) {
        return@withContext try {
            val token = getAccessToken() ?: throw Exception("Not authenticated")

            val currentUser = getCurrentProfile()
            val senderId = currentUser?.id ?: throw Exception("No current user")

            // Try calling backend API first
            // Include sender_device_token so server can exclude this device from FCM
            val senderDeviceToken = try { getFCMTokenSync() } catch (e: Exception) { null }

            val payload = buildJsonObject {
                put("sender_id", senderId)
                put("receiver_id", receiverId)
                put("content", content)
                senderDeviceToken?.let { put("sender_device_token", it) }
            }

            android.util.Log.d(TAG, "🌐 POST $baseUrl/messages/send - body: $payload")

            val request = Request.Builder()
                .url("$baseUrl/messages/send")
                .header("Authorization", "Bearer $token")
                .header("Content-Type", "application/json")
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            android.util.Log.d(TAG, "📡 POST /messages/send Response Code: ${response.code}")
            android.util.Log.d(TAG, "📄 Response Body: $responseBody")

            // If this endpoint doesn't exist yet return fallback to local storage
            if (response.code == 404) {
                android.util.Log.w(TAG, "⚠️ MESSAGES: Backend /messages/send not found (404) - falling back to local storage")
                // fall through to local store below
            } else if (!response.isSuccessful) {
                throw Exception("Failed to send message: ${response.code} - $responseBody")
            } else {
                // Try to parse response and return message object
                try {
                    val parsed = json.parseToJsonElement(responseBody)
                    // Common shapes: { success: true, data: { message: { ... } } } or { message: { ... } } or direct object
                    val msgJson = when {
                        parsed.jsonObject["data"]?.jsonObject?.get("message") != null -> parsed.jsonObject["data"]!!.jsonObject["message"]!!.jsonObject
                        parsed.jsonObject["message"] != null -> parsed.jsonObject["message"]!!.jsonObject
                        parsed.jsonObject["data"] != null && parsed.jsonObject["data"]!!.jsonObject.isNotEmpty() -> parsed.jsonObject["data"]!!.jsonObject
                        else -> parsed.jsonObject
                    }

                    val id = msgJson["id"]?.jsonPrimitive?.contentOrNull ?: java.util.UUID.randomUUID().toString()
                    val sId = msgJson["sender_id"]?.jsonPrimitive?.contentOrNull ?: senderId
                    val rId = msgJson["receiver_id"]?.jsonPrimitive?.contentOrNull ?: receiverId
                    val c = msgJson["content"]?.jsonPrimitive?.contentOrNull ?: content
                    val sentAt = msgJson["sent_at"]?.jsonPrimitive?.contentOrNull ?: msgJson["created_at"]?.jsonPrimitive?.contentOrNull ?: java.time.Instant.now().toString()
                    val readAt = msgJson["read_at"]?.jsonPrimitive?.contentOrNull
                    val fcmMessageId = msgJson["fcm_message_id"]?.jsonPrimitive?.contentOrNull

                    val message = Message(
                        id = id,
                        senderId = sId,
                        receiverId = rId,
                        content = c,
                        sentAt = sentAt,
                        readAt = readAt,
                        fcmMessageId = fcmMessageId
                    )

                    android.util.Log.d(TAG, "✅ Message saved via backend: ${message.id}")
                    return@withContext Result.success(message)
                } catch (e: Exception) {
                    android.util.Log.e(TAG, "Failed to parse message response, falling back to local store", e)
                    // fall through to local storage fallback
                }
            }

            // Fallback: store message locally if backend not available or parsing failed
            android.util.Log.w(TAG, "⚠️ MESSAGES: Using temporary local storage - falling back because backend unavailable or response could not be parsed")

            val message = Message(
                id = java.util.UUID.randomUUID().toString(),
                senderId = senderId,
                receiverId = receiverId,
                content = content,
                sentAt = java.time.Instant.now().toString(),
                readAt = null,
                fcmMessageId = null
            )

            // Store locally in DataStore
            val messagesKey = stringPreferencesKey("local_messages")
            val existingMessagesJson = context.dataStore.data.first()[messagesKey] ?: "[]"
            val existingMessages = json.decodeFromString<List<Message>>(existingMessagesJson)
            val updatedMessages = existingMessages + message
            val updatedMessagesJson = json.encodeToString(updatedMessages)

            context.dataStore.edit { preferences ->
                preferences[messagesKey] = updatedMessagesJson
            }

            android.util.Log.d(TAG, "✅ Message stored locally: ${message.id}")
            Result.success(message)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error sending or storing message", e)
            Result.failure(e)
        }
    }

    // TEMPORARY: Get locally stored messages
    suspend fun getLocalMessages(): List<Message> = withContext(Dispatchers.IO) {
        try {
            val messagesKey = stringPreferencesKey("local_messages")
            val messagesJson = context.dataStore.data.first()[messagesKey] ?: "[]"
            json.decodeFromString(messagesJson)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error reading local messages", e)
            emptyList()
        }
    }

    // TEMPORARY: Store thread locally
    suspend fun storeLocalThread(thread: Thread) = withContext(Dispatchers.IO) {
        try {
            val threadsKey = stringPreferencesKey("local_threads")
            val existingThreadsJson = context.dataStore.data.first()[threadsKey] ?: "[]"
            val existingThreads = json.decodeFromString<List<Thread>>(existingThreadsJson)
            
            // Remove existing thread with same participants (regardless of order)
            val updatedThreads = existingThreads.filterNot { 
                (it.user1Id == thread.user1Id && it.user2Id == thread.user2Id) ||
                (it.user1Id == thread.user2Id && it.user2Id == thread.user1Id)
            } + thread
            val updatedThreadsJson = json.encodeToString(updatedThreads)
            
            context.dataStore.edit { preferences ->
                preferences[threadsKey] = updatedThreadsJson
            }
            
            android.util.Log.d(TAG, "✅ Thread stored locally: ${thread.id} (${thread.user1Id} <-> ${thread.user2Id})")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error storing thread locally", e)
        }
    }

    // TEMPORARY: Get locally stored threads
    suspend fun getLocalThreads(): List<Thread> = withContext(Dispatchers.IO) {
        try {
            val threadsKey = stringPreferencesKey("local_threads")
            val threadsJson = context.dataStore.data.first()[threadsKey] ?: "[]"
            json.decodeFromString(threadsJson)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error reading local threads", e)
            emptyList()
        }
    }

    // Fetch messages for a conversation from backend
    suspend fun getThreadMessages(userId: String): Result<List<Message>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val token = getAccessToken() ?: throw Exception("Not authenticated")
            val currentUser = getCurrentProfile()
            val currentUserId = currentUser?.id ?: throw Exception("No current user")

            android.util.Log.d(TAG, "🌐 GET $baseUrl/messages/thread?user1=$currentUserId&user2=$userId")

            val request = Request.Builder()
                .url("$baseUrl/messages/thread?user1=$currentUserId&user2=$userId")
                .header("Authorization", "Bearer $token")
                .get()
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            android.util.Log.d(TAG, "📡 GET /messages/thread Response Code: ${response.code}")

            if (!response.isSuccessful) {
                android.util.Log.w(TAG, "⚠️ Failed to fetch messages from backend: ${response.code}")
                // Fallback to local storage
                val localMessages = getLocalMessages().filter {
                    (it.senderId == currentUserId && it.receiverId == userId) ||
                    (it.senderId == userId && it.receiverId == currentUserId)
                }
                return@withContext Result.success(localMessages)
            }

            val parsed = json.parseToJsonElement(responseBody)
            val messagesArray = when {
                parsed.jsonObject["data"]?.jsonArray != null -> parsed.jsonObject["data"]!!.jsonArray
                parsed.jsonObject["messages"]?.jsonArray != null -> parsed.jsonObject["messages"]!!.jsonArray
                parsed is JsonArray -> parsed
                else -> JsonArray(emptyList())
            }

            val messages = messagesArray.map { msgElement ->
                val msgJson = msgElement.jsonObject
                // If the backend includes `is_read` boolean rather than a timestamp,
                // treat that as a read marker (fallback) but use the message's created_at
                // as a stable timestamp (instead of Instant.now which would change every poll)
                val readAtStr = msgJson["read_at"]?.jsonPrimitive?.contentOrNull
                val isReadBool = msgJson["is_read"]?.jsonPrimitive?.booleanOrNull ?: false
                val createdAtStr = msgJson["created_at"]?.jsonPrimitive?.contentOrNull
                    ?: msgJson["sent_at"]?.jsonPrimitive?.contentOrNull
                val readAtFinal = readAtStr ?: if (isReadBool) createdAtStr else null

                Message(
                    id = msgJson["id"]?.jsonPrimitive?.contentOrNull ?: java.util.UUID.randomUUID().toString(),
                    senderId = msgJson["sender_id"]?.jsonPrimitive?.contentOrNull ?: "",
                    receiverId = msgJson["receiver_id"]?.jsonPrimitive?.contentOrNull ?: "",
                    content = msgJson["content"]?.jsonPrimitive?.contentOrNull ?: "",
                    sentAt = msgJson["sent_at"]?.jsonPrimitive?.contentOrNull 
                        ?: msgJson["created_at"]?.jsonPrimitive?.contentOrNull 
                        ?: java.time.Instant.now().toString(),
                    readAt = readAtFinal,
                    fcmMessageId = msgJson["fcm_message_id"]?.jsonPrimitive?.contentOrNull
                )
            }

            android.util.Log.d(TAG, "✅ Loaded ${messages.size} messages from backend")
            Result.success(messages)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error fetching thread messages", e)
            // Fallback to local storage
            try {
                val currentUser = getCurrentProfile()
                val currentUserId = currentUser?.id ?: ""
                val localMessages = getLocalMessages().filter {
                    (it.senderId == currentUserId && it.receiverId == userId) ||
                    (it.senderId == userId && it.receiverId == currentUserId)
                }
                Result.success(localMessages)
            } catch (localError: Exception) {
                Result.failure(e)
            }
        }
    }

    // Fetch ALL messages for the current user from backend API
    suspend fun getAllUserMessages(): Result<List<Message>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val currentUser = getCurrentProfile()
            val currentUserId = currentUser?.id ?: throw Exception("No current user")
            val token = getAccessToken() ?: throw Exception("No auth token")

            android.util.Log.d(TAG, "📡 Calling backend API GET /messages for user $currentUserId")

            // Call backend API to get messages
            val request = okhttp3.Request.Builder()
                .url("$baseUrl/messages")
                .get()
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Content-Type", "application/json")
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: throw Exception("Empty response from backend")
            
            android.util.Log.d(TAG, "Backend response: $responseBody")
            
            if (!response.isSuccessful) {
                throw Exception("Backend API error: ${response.code} - $responseBody")
            }
            
            // Parse response - backend returns either array directly or {success, data: array}
            val userMessages = try {
                // Try parsing as direct array first
                json.decodeFromString<List<SupabaseMessage>>(responseBody)
            } catch (e: Exception) {
                // If that fails, try parsing as wrapped response
                val wrappedResponse = json.decodeFromString<JsonObject>(responseBody)
                val dataArray = wrappedResponse["data"]?.jsonArray ?: wrappedResponse["messages"]?.jsonArray
                    ?: throw Exception("Could not find messages in response")
                json.decodeFromString<List<SupabaseMessage>>(dataArray.toString())
            }

            android.util.Log.d(TAG, "✅ Loaded ${userMessages.size} messages from backend API")
            
            // Log details of found messages
            userMessages.forEachIndexed { index, msg ->
                android.util.Log.d(TAG, "  Message: ${msg.id.substring(0, 8)}... from ${msg.sender_id.substring(0, 8)} to ${msg.receiver_id.substring(0, 8)}")
            }

            // Convert Supabase messages to app messages
            val messages = userMessages.map { supaMsg ->
                Message(
                    id = supaMsg.id,
                    senderId = supaMsg.sender_id,
                    receiverId = supaMsg.receiver_id,
                    content = supaMsg.content,
                    sentAt = supaMsg.created_at,
                    deliveredAt = supaMsg.delivered_at,
                    readAt = supaMsg.read_at,
                    fcmMessageId = supaMsg.fcm_message_id
                )
            }.sortedByDescending { it.sentAt }

            Result.success(messages)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ Error fetching messages from backend API: ${e.message}", e)
            e.printStackTrace()
            // Return empty list instead of falling back to local storage
            Result.success(emptyList())
        }
    }
    
    @Serializable
    data class SupabaseMessage(
        val id: String,
        val sender_id: String,
        val receiver_id: String,
        val content: String,
        val created_at: String,
        @Serializable(with = BooleanOrStringSerializer::class)
        val delivered_at: String? = null,
        @Serializable(with = BooleanOrStringSerializer::class)
        val read_at: String? = null,
        @SerialName("is_read")
        val is_read: Boolean? = false,
        val thread_id: String? = null,
        val fcm_message_id: String? = null,
        val sender_name: String? = null,
        val sender_photo: String? = null,
        val receiver_name: String? = null,
        val receiver_photo: String? = null
    )

    // Fetch ALL threads for the current user from backend API
    suspend fun getAllUserThreads(): Result<List<Thread>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val currentUser = getCurrentProfile()
            val currentUserId = currentUser?.id ?: throw Exception("No current user")
            val token = getAccessToken() ?: throw Exception("No auth token")

            android.util.Log.d(TAG, "📡 Calling backend API GET /threads for user $currentUserId")

            // Call backend API to get threads
            val request = okhttp3.Request.Builder()
                .url("$baseUrl/threads")
                .get()
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Content-Type", "application/json")
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: throw Exception("Empty response from backend")
            
            android.util.Log.d(TAG, "Backend response: $responseBody")
            
            if (!response.isSuccessful) {
                throw Exception("Backend API error: ${response.code} - $responseBody")
            }
            
            // Parse response - backend returns either array directly or {success, data: array}
            val allThreads = try {
                // Try parsing as direct array first
                json.decodeFromString<List<SupabaseThread>>(responseBody)
            } catch (e: Exception) {
                // If that fails, try parsing as wrapped response
                val wrappedResponse = json.decodeFromString<JsonObject>(responseBody)
                val dataArray = wrappedResponse["data"]?.jsonArray ?: wrappedResponse["threads"]?.jsonArray
                    ?: throw Exception("Could not find threads in response")
                json.decodeFromString<List<SupabaseThread>>(dataArray.toString())
            }

            android.util.Log.d(TAG, "✅ Loaded ${allThreads.size} threads from backend API")

            // Convert Supabase threads to app threads
            val threads = allThreads.map { supaThread ->
                Thread(
                    id = supaThread.id,
                    user1Id = supaThread.user1_id,
                    user2Id = supaThread.user2_id,
                    lastMessageId = supaThread.last_message_id,
                    updatedAt = supaThread.updated_at
                )
            }

            Result.success(threads)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ Error fetching threads from backend API: ${e.message}", e)
            // Return empty list instead of falling back to local storage
            Result.success(emptyList())
        }
    }
    
    @Serializable
    data class SupabaseThread(
        val id: String,
        val user1_id: String,
        val user2_id: String,
        val last_message_id: String? = null,
        val updated_at: String,
        val created_at: String? = null
    )

    // Fetch user profile by ID (for getting user names)
    suspend fun getUserProfile(userId: String): Result<Profile> = withContext(Dispatchers.IO) {
        return@withContext try {
            val token = getAccessToken() ?: throw Exception("Not authenticated")
            
            android.util.Log.d(TAG, "🔍 Fetching user profile for: $userId")

            // Try primary endpoint first
            val request = Request.Builder()
                .url("$baseUrl/profile/$userId")
                .header("Authorization", "Bearer $token")
                .get()
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            
            android.util.Log.d(TAG, "📡 Profile API response code: ${response.code}")

            if (!response.isSuccessful) {
                android.util.Log.w(TAG, "⚠️ Profile endpoint failed, trying users endpoint")
                // Fallback to /users endpoint
                val userRequest = Request.Builder()
                    .url("$baseUrl/users/$userId")
                    .header("Authorization", "Bearer $token")
                    .get()
                    .build()
                    
                val userResponse = client.newCall(userRequest).execute()
                val userResponseBody = userResponse.body?.string() ?: ""
                
                android.util.Log.d(TAG, "📡 Users API response code: ${userResponse.code}")
                
                if (!userResponse.isSuccessful) {
                    throw Exception("Failed to fetch user profile from both endpoints: profile=${response.code}, users=${userResponse.code}")
                }
                
                val parsed = json.parseToJsonElement(userResponseBody)
                val userJson = parsed.jsonObject["data"]?.jsonObject ?: parsed.jsonObject["user"]?.jsonObject ?: parsed.jsonObject

                val profile = Profile(
                    id = userJson["id"]?.jsonPrimitive?.contentOrNull ?: userId,
                    name = userJson["name"]?.jsonPrimitive?.contentOrNull ?: "Unknown User",
                    email = userJson["email"]?.jsonPrimitive?.contentOrNull ?: "",
                    role = userJson["role"]?.jsonPrimitive?.contentOrNull ?: "",
                    photoUrl = userJson["photo_url"]?.jsonPrimitive?.contentOrNull,
                    branchId = userJson["branch_id"]?.jsonPrimitive?.contentOrNull,
                    branchName = userJson["branch_name"]?.jsonPrimitive?.contentOrNull,
                    districtName = userJson["district_name"]?.jsonPrimitive?.contentOrNull,
                    regionName = userJson["region_name"]?.jsonPrimitive?.contentOrNull,
                    createdAt = userJson["created_at"]?.jsonPrimitive?.contentOrNull ?: "",
                    updatedAt = userJson["updated_at"]?.jsonPrimitive?.contentOrNull ?: "",
                    accessCount = userJson["access_count"]?.jsonPrimitive?.intOrNull ?: 0
                )
                
                android.util.Log.d(TAG, "✅ Loaded profile: ${profile.name}")
                return@withContext Result.success(profile)
            }

            val parsed = json.parseToJsonElement(responseBody)
            val userJson = parsed.jsonObject["data"]?.jsonObject ?: parsed.jsonObject["user"]?.jsonObject ?: parsed.jsonObject

            val profile = Profile(
                id = userJson["id"]?.jsonPrimitive?.contentOrNull ?: userId,
                name = userJson["name"]?.jsonPrimitive?.contentOrNull ?: "Unknown User",
                email = userJson["email"]?.jsonPrimitive?.contentOrNull ?: "",
                role = userJson["role"]?.jsonPrimitive?.contentOrNull ?: "",
                photoUrl = userJson["photo_url"]?.jsonPrimitive?.contentOrNull,
                branchId = userJson["branch_id"]?.jsonPrimitive?.contentOrNull,
                branchName = userJson["branch_name"]?.jsonPrimitive?.contentOrNull,
                districtName = userJson["district_name"]?.jsonPrimitive?.contentOrNull,
                regionName = userJson["region_name"]?.jsonPrimitive?.contentOrNull,
                createdAt = userJson["created_at"]?.jsonPrimitive?.contentOrNull ?: "",
                updatedAt = userJson["updated_at"]?.jsonPrimitive?.contentOrNull ?: "",
                accessCount = userJson["access_count"]?.jsonPrimitive?.intOrNull ?: 0
            )
            
            android.util.Log.d(TAG, "✅ Loaded profile: ${profile.name}")
            Result.success(profile)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ Error fetching user profile for $userId: ${e.message}", e)
            Result.failure(e)
        }
    }

    @Serializable
    data class Thread(
        val id: String,
        @SerialName("user1_id")
        val user1Id: String,
        @SerialName("user2_id")
        val user2Id: String,
        @SerialName("last_message_id")
        val lastMessageId: String? = null,
        @SerialName("updated_at")
        val updatedAt: String
    )

    @Serializable
    data class Message(
        val id: String,
        @SerialName("sender_id")
        val senderId: String,
        @SerialName("receiver_id")
        val receiverId: String,
        val content: String,
        @SerialName("sent_at")
        val sentAt: String,
        @SerialName("delivered_at")
        val deliveredAt: String? = null,
        @SerialName("read_at")
        val readAt: String? = null,
        @SerialName("fcm_message_id")
        val fcmMessageId: String? = null
    )
}
