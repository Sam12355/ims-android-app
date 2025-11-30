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
        Log.d("FCMService", "🔑 New FCM Token: $token")
        
        // Send new token to backend
        sendTokenToBackend(token)
    }
    
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
          Log.d("FCMService", "📬 FCM Message received from: ${message.from}")
        
        // Check if user is logged in and has notifications enabled
        serviceScope.launch {
            // Use the EnhancedAuthRepository (auth_preferences) to ensure we read the
            // same cached user the UI and auth flows write to. ApiClient has a
            // separate DataStore (auth_prefs) which can be out-of-sync in some flows.
            val apiClient = ApiClient.getInstance(this@FCMService)
            val authRepo = com.ims.android.data.repository.EnhancedAuthRepository(this@FCMService, apiClient)
            val currentUser = authRepo.getCurrentUser()

            if (currentUser == null) {
                Log.d("FCMService", "🚫 User not logged in, ignoring notification")
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
                Log.d("FCMService", "🚫 All notifications disabled for user, ignoring notification")
                return@launch
            }

            Log.d("FCMService", "✅ User logged in and has notifications enabled")

            // Track if we already showed a notification from data payload
            var handledByDataPayload = false

            // -- DATA payload handling --
            val data = message.data
            if (data.isNotEmpty()) {
                Log.d("FCMService", "📦 Message data payload: $data")

                val type = data["type"] ?: "general"

                if (type == "new_message") {
                    val senderName = data["sender_name"] ?: "Someone"
                    val messageContent = data["content"] ?: data["message"] ?: "New message"
                    val senderId = data["sender_id"] ?: data["senderId"] ?: data["user_id"] ?: data["userId"] ?: data["from"] ?: ""
                    val messageId = data["message_id"] ?: data["id"]
                    val fcmId = data["fcm_message_id"] ?: data["fcmMessageId"] ?: data["fcm_id"]

                    // Detailed debug trace for suppression decision
                    Log.d(
                        "FCMService",
                        "DEBUG incoming message: type=new_message, senderId='$senderId', fcmId='$fcmId', currentUser='${currentUser.id}', messageId='$messageId', content='$messageContent'"
                    )

                    // Suppress if clearly from current user (senderId match)
                    if (senderId.isNotEmpty() && senderId == currentUser.id) {
                        Log.d("FCMService", "🚫 Suppressing notification - message sent by current user (senderId match)")
                        Log.d("FCMService", "DECISION senderMatch=true, fcmMatch=false, conversationOpen=false -> suppressed by senderId")
                        return@launch
                    }

                    // Fallback: check fcm id against local messages we may have stored
                    if (!fcmId.isNullOrEmpty()) {
                        try {
                            val localMessages = apiClient.getLocalMessages()
                            val found = localMessages.find { it.fcmMessageId == fcmId }
                            if (found != null && found.senderId == currentUser.id) {
                                Log.d("FCMService", "🚫 Suppressing notification - message sent by current user (fcm id match)")
                                Log.d("FCMService", "DECISION senderMatch=false, fcmMatch=true, conversationOpen=false -> suppressed by fcmId")
                                return@launch
                            } else if (found != null) {
                                Log.d("FCMService", "DEBUG local message found for fcmId but sender != currentUser: sender='${found.senderId}'")
                            } else {
                                Log.d("FCMService", "DEBUG no local message found for fcmId='$fcmId'")
                            }
                        } catch (e: Exception) {
                            Log.e("FCMService", "Error checking local messages for fcm id", e)
                        }
                    }

                    // Check if user currently has the conversation open - suppress if yes
                    val socketIOService = SocketIOService.getInstance()
                    val currentChatUserId = socketIOService?.getCurrentChatUserId()
                    if (senderId.isNotEmpty() && currentChatUserId == senderId) {
                        Log.d("FCMService", "🚫 Suppressing notification - conversation is open with sender: $senderId")
                        Log.d("FCMService", "DECISION senderMatch=false, fcmMatch=false, conversationOpen=true -> suppressed by conversation open")
                        return@launch
                    }

                    // Show message notification
                    val notificationService = NotificationService(this@FCMService)
                    notificationService.showMessageNotification(
                        senderName = senderName,
                        messageContent = messageContent,
                        senderId = senderId,
                        messageId = messageId
                    )
                    handledByDataPayload = true
                    Log.d("FCMService", "✅ Message notification shown")
                    Log.d("FCMService", "DECISION senderMatch=false, fcmMatch=false, conversationOpen=false -> notification_shown")

                } else {
                    // non-message notification (moveout, stock_alert, etc.)
                    val title = data["title"] ?: "Stock Nexus"
                    val body = data["body"] ?: data["message"] ?: "New notification"
                    val notificationId = data["notification_id"]
                    val itemName = data["item_name"] ?: ""

                    Log.d("FCMService", "📨 Notification type=$type: $title - $body")
                    showNotification(title, body, type, notificationId, itemName)
                    handledByDataPayload = true
                }
            }

            // -- Legacy notification payload handling (notification block) --
            // Skip if we already handled this via data payload
            if (!handledByDataPayload) {
                message.notification?.let { notification ->
                val title = notification.title ?: "Stock Nexus"
                val body = notification.body ?: ""
                Log.d("FCMService", "📨 Legacy notification: $title - $body")

                // Try to detect if this notification corresponds to a message we sent (legacy block)
                val altSenderIdLegacy = data["sender_id"] ?: data["senderId"] ?: data["user_id"] ?: data["userId"] ?: data["from"] ?: ""
                val fcmIdLegacy = data["fcm_message_id"] ?: data["fcmMessageId"] ?: data["fcm_id"]

                var suppressedLegacy = false
                Log.d(
                    "FCMService",
                    "DEBUG legacy incoming notification: title='$title', body='$body', altSenderIdLegacy='$altSenderIdLegacy', fcmIdLegacy='$fcmIdLegacy', currentUser='${currentUser.id}'"
                )

                if (altSenderIdLegacy.isNotEmpty() && altSenderIdLegacy == currentUser.id) {
                    Log.d("FCMService", "🚫 Suppressing legacy notification - senderId == current user")
                    Log.d("FCMService", "DECISION senderMatch=true -> suppressedLegacy by senderId")
                    suppressedLegacy = true
                } else if (!fcmIdLegacy.isNullOrEmpty()) {
                    try {
                        val localMessages = apiClient.getLocalMessages()
                        val found = localMessages.find { it.fcmMessageId == fcmIdLegacy }
                        if (found != null && found.senderId == currentUser.id) {
                            Log.d("FCMService", "🚫 Suppressing legacy notification - fcm id matches our sent message")
                            suppressedLegacy = true
                        }
                    } catch (e: Exception) {
                        Log.e("FCMService", "Error checking local messages for legacy fcm id", e)
                    }
                }

                if (!suppressedLegacy) {
                    val type = data["type"] ?: "general"
                    val notificationId = data["notification_id"]
                    val itemName = data["item_name"] ?: ""
                    showNotification(title, body, type, notificationId, itemName)
                } else {
                    Log.d("FCMService", "🔇 Legacy notification suppressed")
                }
                }
            } else {
                Log.d("FCMService", "⏭️ Skipping legacy notification - already handled by data payload")
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
        Log.d("FCMService", "🔔 Showing notification: $title - $message")
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
                    Log.d("FCMService", "✅ Token updated on backend")
                } else {
                    Log.e("FCMService", "❌ Failed to update token: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.e("FCMService", "❌ Error updating token", e)
            }
        }
    }
    
    // No companion TAG needed (use explicit tag strings to avoid shadowing parents)
}
