package com.ims.android.service

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.ims.android.data.api.ApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class FCMService : FirebaseMessagingService() {
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "🔑 New FCM Token: $token")
        
        // Send new token to backend
        sendTokenToBackend(token)
    }
    
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d(TAG, "📬 FCM Message received from: ${message.from}")
        
        // Check if user is logged in and has notifications enabled
        serviceScope.launch {
            // Use the EnhancedAuthRepository (auth_preferences) to ensure we read the
            // same cached user the UI and auth flows write to. ApiClient has a
            // separate DataStore (auth_prefs) which can be out-of-sync in some flows.
            val apiClient = ApiClient.getInstance(this@FCMService)
            val authRepo = com.ims.android.data.repository.EnhancedAuthRepository(this@FCMService, apiClient)
            val currentUser = authRepo.getCurrentUser()
            
            if (currentUser == null) {
                Log.d(TAG, "🚫 User not logged in, ignoring notification")
                return@launch
            }
            
            // Check if user has any notifications enabled
            val notificationSettings = currentUser.notificationSettings
            val hasAnyNotificationEnabled = notificationSettings?.get("email") == true ||
                                           notificationSettings?.get("sms") == true ||
                                           notificationSettings?.get("whatsapp") == true ||
                                           notificationSettings?.get("stockLevelAlerts") == true ||
                                           notificationSettings?.get("eventReminders") == true ||
                                           notificationSettings?.get("softdrinkTrends") == true
            
            if (!hasAnyNotificationEnabled) {
                Log.d(TAG, "🚫 All notifications disabled for user, ignoring notification")
                return@launch
            }
            
            Log.d(TAG, "✅ User logged in and has notifications enabled")
        
            // Handle data-only messages (backend sends title/body in data field)
            if (message.data.isNotEmpty()) {
                Log.d(TAG, "📦 Message data payload: ${message.data}")
                
                // Extract title and body from data field
                val title = message.data["title"] ?: "Stock Nexus"
                val body = message.data["body"] ?: "New notification"
                val type = message.data["type"] ?: "general"
                val notificationId = message.data["notification_id"]
                val itemName = message.data["item_name"] ?: ""
                
                Log.d(TAG, "📨 Notification: $title - $body")
                
                // Show notification using NotificationService
                showNotification(title, body, type, notificationId, itemName)
            }
            
            // Handle old notification payload format (fallback)
            message.notification?.let { notification ->
                val title = notification.title ?: "Stock Nexus"
                val body = notification.body ?: ""
                Log.d(TAG, "📨 Legacy notification: $title - $body")
                
                val type = message.data["type"] ?: "general"
                val notificationId = message.data["notification_id"]
                val itemName = message.data["item_name"] ?: ""
                
                showNotification(title, body, type, notificationId, itemName)
            }
        }
    }
    

    
    private fun showNotification(
        title: String,
        message: String,
        type: String,
        notificationId: String?,
        itemName: String
    ) {
        Log.d(TAG, "🔔 Showing notification: $title - $message")
        val notificationService = NotificationService(this)
        // Record dedupe id so Socket.IO won't duplicate this notification
        val dedupeId = com.ims.android.service.NotificationDeduper.makeId(notificationId, title, message)
        com.ims.android.service.NotificationDeduper.record(dedupeId)

        notificationService.showNotification(title, message, type, notificationId)
    }
    
    private fun sendTokenToBackend(token: String) {
        serviceScope.launch {
            try {
                val apiClient = ApiClient.getInstance(this@FCMService)
                val result = apiClient.updateFCMToken(token)
                
                if (result.isSuccess) {
                    Log.d(TAG, "✅ Token updated on backend")
                } else {
                    Log.e(TAG, "❌ Failed to update token: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error updating token", e)
            }
        }
    }
    
    companion object {
        private const val TAG = "FCMService"
    }
}
