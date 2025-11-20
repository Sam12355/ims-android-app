package com.stocknexus.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.stocknexus.android.presentation.MainActivity
import kotlin.random.Random

class NotificationService(private val context: Context) {
    
    companion object {
        private const val CHANNEL_ID = "stock_nexus_notifications"
        private const val CHANNEL_NAME = "Stock Nexus Notifications"
        private const val CHANNEL_DESCRIPTION = "Notifications for stock alerts and events"
        
        const val NOTIFICATION_TYPE_GENERAL = "general"
        const val NOTIFICATION_TYPE_STOCK_ALERT = "stock_alert"
        const val NOTIFICATION_TYPE_EVENT = "event"
    }
    
    init {
        createNotificationChannel()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
                enableLights(true)
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    fun showNotification(
        title: String,
        message: String,
        type: String = NOTIFICATION_TYPE_GENERAL,
        notificationId: String? = null
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("open_notifications", true)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val iconResId = when (type) {
            NOTIFICATION_TYPE_STOCK_ALERT -> android.R.drawable.ic_dialog_alert
            NOTIFICATION_TYPE_EVENT -> android.R.drawable.ic_menu_my_calendar
            else -> android.R.drawable.ic_dialog_info
        }
        
        val uniqueId = notificationId?.hashCode() ?: Random.nextInt()
        
        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(iconResId)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
        
        // Add action buttons for stock alerts
        if (type == NOTIFICATION_TYPE_STOCK_ALERT && notificationId != null) {
            // Mark as Read button
            val markReadIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                action = NotificationActionReceiver.ACTION_MARK_READ
                putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
                putExtra(NotificationActionReceiver.EXTRA_SYSTEM_NOTIFICATION_ID, uniqueId)
            }
            val markReadPendingIntent = PendingIntent.getBroadcast(
                context,
                uniqueId * 10 + 1,
                markReadIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            // Cancel button
            val cancelIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                action = NotificationActionReceiver.ACTION_CANCEL
                putExtra(NotificationActionReceiver.EXTRA_SYSTEM_NOTIFICATION_ID, uniqueId)
            }
            val cancelPendingIntent = PendingIntent.getBroadcast(
                context,
                uniqueId * 10 + 2,
                cancelIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            notificationBuilder
                .addAction(android.R.drawable.ic_menu_view, "Read", markReadPendingIntent)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", cancelPendingIntent)
        }
        
        val notification = notificationBuilder.build()
        
        try {
            NotificationManagerCompat.from(context).notify(uniqueId, notification)
        } catch (e: SecurityException) {
            android.util.Log.e("NotificationService", "Permission denied for notifications", e)
        }
    }
    
    fun showStockAlert(itemName: String, currentQty: Int, threshold: Int) {
        showNotification(
            title = "Low Stock Alert",
            message = "$itemName is running low. Current: $currentQty, Threshold: $threshold",
            type = NOTIFICATION_TYPE_STOCK_ALERT
        )
    }
    
    fun showEventReminder(eventTitle: String, eventDate: String) {
        showNotification(
            title = "Event Reminder",
            message = "$eventTitle on $eventDate",
            type = NOTIFICATION_TYPE_EVENT
        )
    }
}
