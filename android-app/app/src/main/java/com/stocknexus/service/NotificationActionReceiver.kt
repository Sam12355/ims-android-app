package com.stocknexus.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.stocknexus.data.api.ApiClient
import com.stocknexus.data.repository.NotificationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {
    
    companion object {
        const val ACTION_MARK_READ = "com.stocknexus.ACTION_MARK_READ"
        const val ACTION_CANCEL = "com.stocknexus.ACTION_CANCEL"
        const val EXTRA_NOTIFICATION_ID = "notification_id"
        const val EXTRA_SYSTEM_NOTIFICATION_ID = "system_notification_id"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        val notificationId = intent.getStringExtra(EXTRA_NOTIFICATION_ID)
        val systemNotificationId = intent.getIntExtra(EXTRA_SYSTEM_NOTIFICATION_ID, -1)
        
        android.util.Log.d("NotificationAction", "Action received: ${intent.action}, ID: $notificationId")
        
        when (intent.action) {
            ACTION_MARK_READ -> {
                // Mark as read in backend
                if (notificationId != null) {
                    markNotificationAsRead(context, notificationId)
                }
                // Dismiss the notification
                if (systemNotificationId != -1) {
                    NotificationManagerCompat.from(context).cancel(systemNotificationId)
                }
            }
            ACTION_CANCEL -> {
                // Just dismiss the notification without marking as read
                if (systemNotificationId != -1) {
                    NotificationManagerCompat.from(context).cancel(systemNotificationId)
                }
            }
        }
    }
    
    private fun markNotificationAsRead(context: Context, notificationId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val apiClient = ApiClient.getInstance(context)
                val repository = NotificationRepository(apiClient)
                repository.markNotificationAsRead(notificationId)
                android.util.Log.d("NotificationAction", "✅ Marked notification $notificationId as read")
            } catch (e: Exception) {
                android.util.Log.e("NotificationAction", "❌ Failed to mark as read: ${e.message}")
            }
        }
    }
}
