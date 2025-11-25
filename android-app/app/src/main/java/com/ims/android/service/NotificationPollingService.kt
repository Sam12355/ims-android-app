package com.ims.android.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import com.ims.android.data.api.ApiClient
import com.ims.android.data.repository.NotificationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class NotificationPollingReceiver : BroadcastReceiver() {
    
    companion object {
        private const val ACTION_POLL = "com.ims.android.POLL_NOTIFICATIONS"
        private const val PREFS_NAME = "stock_nexus_prefs"
        private const val POLL_INTERVAL_MS = 60_000L // 1 minute
        
        fun schedulePolling(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, NotificationPollingReceiver::class.java).apply {
                action = ACTION_POLL
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            // Schedule repeating alarm every 1 minute
            alarmManager.setRepeating(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + POLL_INTERVAL_MS,
                POLL_INTERVAL_MS,
                pendingIntent
            )
            
            android.util.Log.d("NotificationPolling", "📡 Scheduled notification polling every 1 minute")
        }
        
        fun cancelPolling(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, NotificationPollingReceiver::class.java).apply {
                action = ACTION_POLL
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
            
            android.util.Log.d("NotificationPolling", "🛑 Cancelled notification polling")
        }
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_POLL) {
            android.util.Log.d("NotificationPolling", "⏰ Polling for notifications...")
            
            // Use coroutine scope for async work
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            scope.launch {
                try {
                    val apiClient = ApiClient.getInstance(context)
                    val notificationRepository = NotificationRepository(apiClient)
                    val notificationService = NotificationService(context)
                    
                    // Get shared preferences for tracking shown notifications
                    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    val lastCheckIds = prefs.getStringSet("shown_notification_ids", mutableSetOf()) ?: mutableSetOf()
                    
                    // Fetch notifications
                    val result = notificationRepository.getNotifications()
                    
                    result.onSuccess { notifications ->
                        var newCount = 0
                        
                        // Only process UNREAD notifications
                        val unreadNotifications = notifications.filter { !it.isRead }
                        
                        android.util.Log.d("NotificationPolling", "📊 Total: ${notifications.size}, Unread: ${unreadNotifications.size}")
                        
                        // Log each unread notification for debugging
                        unreadNotifications.forEach { notif ->
                            android.util.Log.d("NotificationPolling", "  📄 ID: ${notif.id}, Type: ${notif.type}, Read: ${notif.isRead}, Msg: ${notif.message?.take(50)}")
                        }
                        
                        unreadNotifications.forEach { notification ->
                            // Show notification if we haven't shown it before
                            if (!lastCheckIds.contains(notification.id)) {
                                val title = when {
                                    notification.type?.contains("event", ignoreCase = true) == true -> "Event Reminder"
                                    notification.type?.contains("stock", ignoreCase = true) == true -> "Stock Alert"
                                    else -> "Stock Nexus"
                                }
                                
                                notificationService.showNotification(
                                    title = title,
                                    message = notification.message ?: "New notification",
                                    type = notification.type ?: "general",
                                    notificationId = notification.id
                                )
                                
                                newCount++
                                android.util.Log.d("NotificationPolling", "🔔 NEW! Showed: ${notification.message}")
                            }
                        }
                        
                        // Update tracking: only keep IDs of current unread notifications
                        val currentUnreadIds = unreadNotifications.map { it.id }.toSet()
                        prefs.edit().putStringSet("shown_notification_ids", currentUnreadIds).apply()
                        
                        android.util.Log.d("NotificationPolling", "✅ Checked ${notifications.size} total, ${unreadNotifications.size} unread, $newCount new shown")
                    }
                    
                    result.onFailure { error ->
                        android.util.Log.e("NotificationPolling", "❌ Failed to fetch notifications", error)
                    }
                    
                } catch (e: Exception) {
                    android.util.Log.e("NotificationPolling", "❌ Error polling notifications", e)
                }
            }
        }
    }
}
