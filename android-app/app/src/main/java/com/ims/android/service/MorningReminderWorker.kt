package com.ims.android.service

import android.content.Context
import androidx.work.*
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Worker to send scheduled morning FCM reminder notifications.
 * This worker is scheduled to run at 10:00 AM Swedish time the next day
 * for moveout notifications created after 10:00 AM.
 */
class MorningReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    companion object {
        private const val TAG = "MorningReminderWorker"
        private const val KEY_TYPE = "type"
        private const val KEY_TITLE = "title"
        private const val KEY_MESSAGE = "message"
    }
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        return@withContext try {
            val type = inputData.getString(KEY_TYPE) ?: "moveout"
            val title = inputData.getString(KEY_TITLE) ?: "Morning Reminder"
            val message = inputData.getString(KEY_MESSAGE) ?: "You have pending notifications"
            
            android.util.Log.d(TAG, "🌅 Morning reminder triggered: $title - $message")
            
            // Show local notification
            val notificationService = NotificationService(applicationContext)
            notificationService.showNotification(
                title = "🌅 $title",
                message = message,
                type = type,
                notificationId = "morning_${System.currentTimeMillis()}"
            )
            
            // Also try to send FCM to all subscribed devices
            sendFCMToAllStaff(type, title, message)
            
            Result.success()
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error in morning reminder worker", e)
            Result.failure()
        }
    }
    
    private suspend fun sendFCMToAllStaff(type: String, title: String, message: String) {
        try {
            // Get FCM token for self-notification (in case the app is not running)
            val token = FirebaseMessaging.getInstance().token.await()
            android.util.Log.d(TAG, "FCM token available, sending morning reminder notification")
            
            // The FCM is already sent via the backend when the worker was scheduled
            // This local notification serves as a backup
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to send FCM morning reminder: ${e.message}")
        }
    }
}
