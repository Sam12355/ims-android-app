package com.ims.android.service

import android.content.Context
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import org.json.JSONObject
import java.net.URISyntaxException

class SocketIOService(private val context: Context) {
    
    private var socket: Socket? = null
    private var isConnected = false
    private var currentBranchId: String? = null
    private val notificationService = NotificationService(context)
    
    companion object {
        private const val SOCKET_URL = "https://ims-sy.vercel.app"
        private const val TAG = "SocketIOService"
    }
    
    fun connect(token: String, branchId: String) {
        try {
            disconnect() // Disconnect any existing connection
            
            currentBranchId = branchId // Store branch ID
            
            android.util.Log.d(TAG, "🔌 Connecting to Socket.IO server...")
            android.util.Log.d(TAG, "🔌 Branch ID: $branchId")
            android.util.Log.d(TAG, "🔌 Server URL: $SOCKET_URL")
            
            val opts = IO.Options().apply {
                auth = mapOf("token" to token)
                query = "branchId=$branchId"
                // Try polling first as websocket might have SSL issues
                transports = arrayOf("polling", "websocket")
                reconnectionAttempts = 5
                reconnectionDelay = 1000
                timeout = 20000
                forceNew = true
                // Add secure options
                secure = true
            }
            
            socket = IO.socket(SOCKET_URL, opts)
            
            socket?.on(Socket.EVENT_CONNECT, onConnect)
            socket?.on(Socket.EVENT_DISCONNECT, onDisconnect)
            socket?.on(Socket.EVENT_CONNECT_ERROR, onConnectError)
            socket?.on("notification-update", onNotificationUpdate)
            
            socket?.connect()
            
        } catch (e: URISyntaxException) {
            android.util.Log.e(TAG, "❌ Failed to create socket", e)
        }
    }
    
    private val onConnect = Emitter.Listener {
        android.util.Log.d(TAG, "✅ Connected to Socket.IO server: ${socket?.id()}")
        isConnected = true
        
        // Join the branch room
        currentBranchId?.let { branchId ->
            socket?.emit("join-branch", branchId)
            android.util.Log.d(TAG, "👥 Joined branch room: $branchId")
        }
    }
    
    private val onDisconnect = Emitter.Listener { args ->
        val reason = args.firstOrNull()?.toString() ?: "unknown"
        android.util.Log.d(TAG, "❌ Disconnected from Socket.IO server: $reason")
        isConnected = false
    }
    
    private val onConnectError = Emitter.Listener { args ->
        val error = args.firstOrNull()
        android.util.Log.e(TAG, "❌ Socket.IO connection error: $error")
        if (error is Exception) {
            android.util.Log.e(TAG, "❌ Error details: ${error.message}")
            error.printStackTrace()
        }
        isConnected = false
    }
    
    private val onNotificationUpdate = Emitter.Listener { args ->
        try {
            val data = args.firstOrNull() as? JSONObject
            if (data != null) {
                android.util.Log.d(TAG, "📢 Received notification update: $data")
                
                // Parse notification data
                val type = data.optString("type", "general")
                val message = data.optString("message", "New notification")
                val title = when {
                    type.contains("event", ignoreCase = true) -> "[Socket.IO] Event Reminder"
                    type.contains("stock", ignoreCase = true) -> "[Socket.IO] Stock Alert"
                    else -> "[Socket.IO] Stock Nexus"
                }
                
                val notificationId = data.optString("id", null)
                
                // Show push notification
                notificationService.showNotification(
                    title = title,
                    message = message,
                    type = type,
                    notificationId = notificationId
                )
                
                android.util.Log.d(TAG, "✅ Showed notification: $title - $message")
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ Error processing notification update", e)
        }
    }
    
    fun disconnect() {
        if (socket != null) {
            android.util.Log.d(TAG, "🔌 Disconnecting from Socket.IO server...")
            socket?.off()
            socket?.disconnect()
            socket = null
            isConnected = false
        }
    }
    
    fun isSocketConnected(): Boolean {
        return isConnected && socket?.connected() == true
    }
    
    fun reconnect() {
        android.util.Log.d(TAG, "🔄 Forcing Socket.IO reconnection...")
        socket?.connect()
    }
}
