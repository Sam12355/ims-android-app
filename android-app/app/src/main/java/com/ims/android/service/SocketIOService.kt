package com.ims.android.service

import android.content.Context
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject
import com.ims.android.data.model.OnlineMember
import com.ims.android.data.model.UserOnlineEvent
import com.ims.android.ui.toast.UserOnlineToastManager
import java.net.URISyntaxException

class SocketIOService(private val context: Context) {
    
    private var socket: Socket? = null
    private var isConnected = false
    private var currentBranchId: String? = null
    private val notificationService = NotificationService(context)
    // State flow storing the current online members in the branch
    private val _onlineMembers = MutableStateFlow<List<OnlineMember>>(emptyList())
    val onlineMembers: StateFlow<List<OnlineMember>> = _onlineMembers
    
    // State flow for user online events (when someone comes online)
    private val _userOnlineEvent = MutableStateFlow<UserOnlineEvent?>(null)
    val userOnlineEvent: StateFlow<UserOnlineEvent?> = _userOnlineEvent
    
    companion object {
        private const val SOCKET_URL = "https://stock-nexus-84-main-2-1.onrender.com"
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
            // Presence events (if the server supports them)
            socket?.on("online-members", onOnlineMembers)
            socket?.on("user-online", onUserOnline)
            socket?.on("user-offline", onUserOffline)
            
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
                
                // Prefer server-provided notification id keys
                val notificationId = when {
                    data.has("notification_id") -> data.optString("notification_id", null)
                    data.has("id") -> data.optString("id", null)
                    else -> null
                }

                // Build dedupe id using notification id or title+message hash
                val dedupeId = com.ims.android.service.NotificationDeduper.makeId(notificationId, title, message)

                if (com.ims.android.service.NotificationDeduper.has(dedupeId)) {
                    android.util.Log.d(TAG, "⏭ Duplicate notification received (id=$dedupeId), skipping")
                } else {
                    notificationService.showNotification(
                        title = title,
                        message = message,
                        type = type,
                        notificationId = notificationId
                    )

                    // record so FCM or subsequent Socket messages don't duplicate
                    com.ims.android.service.NotificationDeduper.record(dedupeId)
                    android.util.Log.d(TAG, "✅ Showed notification: $title - $message (dedupe=$dedupeId)")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ Error processing notification update", e)
        }
    }

    // Presence listeners - receive either an array of user objects or single user events
    private val onOnlineMembers = Emitter.Listener { args ->
        try {
            val payload = args.firstOrNull()
            if (payload is JSONArray) {
                val newList = mutableListOf<OnlineMember>()
                for (i in 0 until payload.length()) {
                    val j = payload.optJSONObject(i) ?: continue
                    val id = j.optString("id", j.optString("userId", ""))
                    val name = j.optString("name", null)
                    val photo = j.optString("photoUrl", j.optString("photo_url", null))
                    if (id.isNotBlank()) newList.add(OnlineMember(id = id, name = name, photoUrl = photo))
                }
                
                // Detect NEW users who just came online (not in previous list)
                val previousIds = _onlineMembers.value.map { it.id }.toSet()
                val newMembers = newList.filter { it.id !in previousIds }
                
                // Emit event for each new user (but skip the first load when previousIds is empty)
                if (previousIds.isNotEmpty()) {
                    newMembers.forEach { member ->
                        android.util.Log.d(TAG, "🟢 NEW user came online: ${member.id} (${member.name})")
                        val event = UserOnlineEvent(
                            userId = member.id,
                            userName = member.name ?: "User",
                            photoUrl = member.photoUrl,
                            timestamp = System.currentTimeMillis()
                        )
                        _userOnlineEvent.value = event
                        // also post to UI toast manager
                        UserOnlineToastManager.post(event)
                    }
                }
                
                _onlineMembers.value = newList
                android.util.Log.d(TAG, "👥 Online members updated: ${newList.size}")
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ Error processing online-members event", e)
        }
    }

    private val onUserOnline = Emitter.Listener { args ->
        try {
            val j = args.firstOrNull() as? JSONObject ?: return@Listener
            val id = j.optString("id", j.optString("userId", ""))
            val name = j.optString("name", null)
            val photo = j.optString("photoUrl", j.optString("photo_url", null))
            if (id.isNotBlank()) {
                val current = _onlineMembers.value.toMutableList()
                // avoid duplicates
                if (current.none { it.id == id }) {
                    current.add(0, OnlineMember(id = id, name = name, photoUrl = photo))
                    _onlineMembers.value = current
                    android.util.Log.d(TAG, "➕ User online: $id ($name)")
                    
                    // Emit event so dashboard can show toast
                    val event = UserOnlineEvent(
                        userId = id,
                        userName = name ?: "User",
                        photoUrl = photo,
                        timestamp = System.currentTimeMillis()
                    )
                    _userOnlineEvent.value = event
                    // also post to UI toast manager
                    UserOnlineToastManager.post(event)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ Error processing user-online event", e)
        }
    }

    private val onUserOffline = Emitter.Listener { args ->
        try {
            val userId = args.firstOrNull()?.toString() ?: return@Listener
            val current = _onlineMembers.value.filter { it.id != userId }
            _onlineMembers.value = current
            android.util.Log.d(TAG, "➖ User offline: $userId")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ Error processing user-offline event", e)
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
    
    /**
     * Emit user-away event when app goes to background
     */
    fun emitUserAway() {
        if (isConnected && socket?.connected() == true) {
            android.util.Log.d(TAG, "😴 Emitting user-away (app backgrounded)")
            socket?.emit("user-away")
        }
    }
    
    /**
     * Emit user-back event when app comes to foreground
     */
    fun emitUserBack() {
        if (isConnected && socket?.connected() == true) {
            android.util.Log.d(TAG, "👋 Emitting user-back (app foregrounded)")
            socket?.emit("user-back")
        }
    }
}
