package com.ims.android.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.ims.android.R
import com.ims.android.presentation.MainActivity
import kotlin.random.Random

class NotificationService(private val context: Context) {
    
    companion object {
        private const val CHANNEL_ID = "stock_nexus_notifications"
        private const val STOCK_ALERT_CHANNEL_ID = "stock_alerts_channel"
        private const val EVENT_ALERT_CHANNEL_ID = "event_alerts_channel"
        private const val MESSAGE_CHANNEL_ID = "messages_channel"
        private const val CHANNEL_NAME = "Stock Nexus Notifications"
        private const val STOCK_ALERT_CHANNEL_NAME = "Stock Alerts"
        private const val EVENT_ALERT_CHANNEL_NAME = "Event Reminders"
        private const val MESSAGE_CHANNEL_NAME = "Messages"
        private const val CHANNEL_DESCRIPTION = "Notifications for stock alerts and events"
        
        const val NOTIFICATION_TYPE_GENERAL = "general"
        const val NOTIFICATION_TYPE_STOCK_ALERT = "stock_alert"
        const val NOTIFICATION_TYPE_EVENT = "event_reminder"
        const val NOTIFICATION_TYPE_MESSAGE = "new_message"
    }
    
    init {
        createNotificationChannel()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // General notifications channel (default sound)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
                enableLights(true)
            }
            notificationManager.createNotificationChannel(channel)
            
            // Stock alerts channel with custom sound
            val stockAlertSound = Uri.parse("android.resource://${context.packageName}/${R.raw.stock_alert}")
            val stockChannel = NotificationChannel(STOCK_ALERT_CHANNEL_ID, STOCK_ALERT_CHANNEL_NAME, importance).apply {
                description = "Stock level alerts with custom sound"
                enableVibration(true)
                enableLights(true)
                setSound(stockAlertSound, null)
            }
            notificationManager.createNotificationChannel(stockChannel)
            
            // Event reminders channel with custom sound
            val eventAlertSound = Uri.parse("android.resource://${context.packageName}/${R.raw.event_alert}")
            val eventChannel = NotificationChannel(EVENT_ALERT_CHANNEL_ID, EVENT_ALERT_CHANNEL_NAME, importance).apply {
                description = "Event reminders with custom sound"
                enableVibration(true)
                enableLights(true)
                setSound(eventAlertSound, null)
            }
            notificationManager.createNotificationChannel(eventChannel)
            
            // Messages channel with default sound
            val messageChannel = NotificationChannel(MESSAGE_CHANNEL_ID, MESSAGE_CHANNEL_NAME, importance).apply {
                description = "New message notifications"
                enableVibration(true)
                enableLights(true)
            }
            notificationManager.createNotificationChannel(messageChannel)
        }
    }
    
    fun showNotification(
        title: String,
        message: String,
        type: String = NOTIFICATION_TYPE_GENERAL,
        notificationId: String? = null
    ) {
        android.util.Log.d("NotificationService", "📢 showNotification called: type='$type', title='$title'")
        
        // Bring existing task (don't restart) and attach a timestamp so activity reacts
        // NEW_TASK needed when launching from Service context, SINGLE_TOP reuses existing activity
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("notification_type", type)
            putExtra("notification_ts", System.currentTimeMillis())
            // For moveout notifications, open the moveout dialog on dashboard
            if (type == "moveout") {
                android.util.Log.d("NotificationService", "📢 Setting open_moveout=true for moveout notification")
                putExtra("open_moveout", true)
            } else {
                putExtra("open_notifications", true)
            }
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
            NOTIFICATION_TYPE_MESSAGE -> android.R.drawable.ic_dialog_email
            else -> android.R.drawable.ic_dialog_info
        }
        
        val uniqueId = notificationId?.hashCode() ?: Random.nextInt()
        
        // Use specific channel based on notification type
        val channelId = when (type) {
            NOTIFICATION_TYPE_STOCK_ALERT -> STOCK_ALERT_CHANNEL_ID
            NOTIFICATION_TYPE_EVENT -> EVENT_ALERT_CHANNEL_ID
            NOTIFICATION_TYPE_MESSAGE -> MESSAGE_CHANNEL_ID
            else -> CHANNEL_ID
        }
        
        val notificationBuilder = NotificationCompat.Builder(context, channelId)
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
    
    fun showMessageNotification(
        senderName: String,
        messageContent: String,
        senderId: String,
        messageId: String? = null
    ) {
        // Bring existing task to the foreground without recreating.
        // NEW_TASK is needed when launching from a Service/BroadcastReceiver context.
        // SINGLE_TOP ensures we don't create a new instance if one already exists at top.
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("open_chat", true)
            putExtra("user_id", senderId)
            putExtra("notification_ts", System.currentTimeMillis())
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            senderId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val uniqueId = messageId?.hashCode() ?: Random.nextInt()
        
        val notificationBuilder = NotificationCompat.Builder(context, MESSAGE_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentTitle(senderName)
            .setContentText(messageContent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(messageContent))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
        
        val notification = notificationBuilder.build()
        
        try {
            NotificationManagerCompat.from(context).notify(uniqueId, notification)
        } catch (e: SecurityException) {
            android.util.Log.e("NotificationService", "Permission denied for notifications", e)
        }
    }
}
