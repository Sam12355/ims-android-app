package com.ims.android.service

import android.content.Context
import androidx.work.*
import com.ims.android.data.api.ApiClient
import com.ims.android.data.repository.NotificationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class NotificationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    companion object {
        private const val WORK_NAME = "notification_polling"
        private const val LAST_CHECK_PREF = "last_notification_check"
        private const val PREFS_NAME = "stock_nexus_prefs"
        
        fun schedulePeriodicWork(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            
            val workRequest = PeriodicWorkRequestBuilder<NotificationWorker>(
                1, TimeUnit.MINUTES // Check every 1 minute
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.LINEAR,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()
            
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }
        
        fun cancelWork(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        return@withContext try {
            val apiClient = ApiClient.getInstance(applicationContext)
            val notificationRepository = NotificationRepository(apiClient)
            val notificationService = NotificationService(applicationContext)
            
            // Get shared preferences for tracking last check time
            val prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val lastCheckIds = prefs.getStringSet("shown_notification_ids", mutableSetOf()) ?: mutableSetOf()
            
            // Fetch notifications
            val result = notificationRepository.getNotifications()
            
            result.onSuccess { notifications ->
                val newIds = mutableSetOf<String>()
                
                notifications
                    .filter { !it.isRead }
                    .forEach { notification ->
                        // Only show if we haven't shown this notification before
                        if (!lastCheckIds.contains(notification.id)) {
                            val title = when {
                                notification.type?.contains("event", ignoreCase = true) == true -> "[WorkManager] Event Reminder"
                                notification.type?.contains("stock", ignoreCase = true) == true -> "[WorkManager] Stock Alert"
                                else -> "[WorkManager] Stock Nexus"
                            }
                            
                            notificationService.showNotification(
                                title = title,
                                message = notification.message ?: "New notification",
                                type = notification.type ?: "general",
                                notificationId = notification.id
                            )
                            
                            android.util.Log.d("NotificationWorker", "Showed notification: ${notification.id}")
                        }
                        newIds.add(notification.id)
                    }
                
                // Update the list of shown notifications
                prefs.edit().putStringSet("shown_notification_ids", newIds).apply()
                
                android.util.Log.d("NotificationWorker", "Checked notifications: ${notifications.size} total, ${newIds.size - lastCheckIds.size} new")
            }
            
            result.onFailure { error ->
                android.util.Log.e("NotificationWorker", "Failed to fetch notifications", error)
            }
            
            Result.success()
        } catch (e: Exception) {
            android.util.Log.e("NotificationWorker", "Error in notification worker", e)
            Result.retry()
        }
    }
}
