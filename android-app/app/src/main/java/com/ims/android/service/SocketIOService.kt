package com.ims.android.service

import android.content.Context
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject
import com.google.firebase.messaging.FirebaseMessaging
import android.provider.Settings
import com.ims.android.data.model.OnlineMember
import com.ims.android.data.model.UserOnlineEvent
import com.ims.android.ui.toast.UserOnlineToastManager
import java.net.URISyntaxException

class SocketIOService(private val context: Context) {
    
    private var socket: Socket? = null
    private var isConnected = false
    private var currentBranchId: String? = null
    private var currentUserId: String? = null
    private val notificationService = NotificationService(context)
    // State flow storing the current online members in the branch
    private val _onlineMembers = MutableStateFlow<List<OnlineMember>>(emptyList())
    val onlineMembers: StateFlow<List<OnlineMember>> = _onlineMembers
    
    // State flow for user online events (when someone comes online)
    private val _userOnlineEvent = MutableStateFlow<UserOnlineEvent?>(null)
    val userOnlineEvent: StateFlow<UserOnlineEvent?> = _userOnlineEvent
    
    // State flow for new messages
    private val _newMessageReceived = MutableStateFlow<JSONObject?>(null)
    val newMessageReceived: StateFlow<JSONObject?> = _newMessageReceived
    
    // State flow for typing indicators
    private val _userTyping = MutableStateFlow<Pair<String, Boolean>?>(null) // userId to isTyping
    val userTyping: StateFlow<Pair<String, Boolean>?> = _userTyping
    
    // State flow for message delivered updates
    private val _messageDelivered = MutableStateFlow<Pair<String, String>?>(null) // messageId to deliveredAt
    val messageDelivered: StateFlow<Pair<String, String>?> = _messageDelivered
    
    // State flow for messages read updates
    private val _messagesRead = MutableStateFlow<Pair<List<String>, String>?>(null) // messageIds to readAt
    val messagesRead: StateFlow<Pair<List<String>, String>?> = _messagesRead
    
    companion object {
        private const val SOCKET_URL = "https://stock-nexus-84-main-2-1.onrender.com"
        private const val TAG = "SocketIOService"
        
        @Volatile
        private var INSTANCE: SocketIOService? = null
        
        fun getInstance(): SocketIOService? = INSTANCE
        
        internal fun setInstance(instance: SocketIOService?) {
            INSTANCE = instance
        }
    }
    
    fun connect(token: String, branchId: String, userId: String) {
        try {
            disconnect() // Disconnect any existing connection
            
            currentBranchId = branchId // Store branch ID
            currentUserId = userId // Store user ID for filtering
            
            android.util.Log.d(TAG, "🔌 Connecting to Socket.IO server...")
            android.util.Log.d(TAG, "🔌 Branch ID: $branchId")
            android.util.Log.d(TAG, "🔌 User ID: $userId")
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
            socket?.on("new_message", onNewMessage)
            socket?.on("user_typing", onUserTyping)
            socket?.on("user_stop_typing", onUserStopTyping)
            socket?.on("messageDelivered", onMessageDelivered)
            socket?.on("messagesRead", onMessagesRead)
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
        
        // Join personal user room for direct messages and typing indicators
        currentUserId?.let { userId ->
            socket?.emit("join-room", userId)
            android.util.Log.d(TAG, "🚪 Joined personal room: $userId")
        }
        
        // Request online members list immediately after joining
        socket?.emit("get-online-members")
        android.util.Log.d(TAG, "📡 Requesting online members list")

        // Emit registerDevice so the server can map this live socket to the device token
        try {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                try {
                    if (task.isSuccessful) {
                        val deviceToken = task.result
                        val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
                        val payload = JSONObject()
                        payload.put("device_token", deviceToken)
                        payload.put("device_id", deviceId)

                        socket?.emit("registerDevice", payload)
                        android.util.Log.d(TAG, "📡 Emitted registerDevice via socket: $payload")
                    } else {
                        android.util.Log.w(TAG, "⚠️ Failed to read FCM token for registerDevice: ${task.exception}")
                    }
                } catch (e: Exception) {
                    android.util.Log.e(TAG, "❌ Error emitting registerDevice", e)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ Error scheduling registerDevice emission", e)
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
                android.util.Log.d(TAG, "📢 Received notification update via Socket.IO: $data")
                // FCM is now handling push notifications, so we just log this event
                // and don't show a duplicate notification from Socket.IO
                android.util.Log.d(TAG, "📢 Skipping Socket.IO notification display (FCM handles this)")
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ Error processing notification update", e)
        }
    }
    
    // Current chat user ID to suppress notifications
    private var currentChatUserId: String? = null
    
    fun setCurrentChatUser(userId: String?) {
        currentChatUserId = userId
        android.util.Log.d(TAG, "📱 Current chat user set to: $userId")
    }
    
    fun getCurrentChatUserId(): String? = currentChatUserId
    
    private val onNewMessage = Emitter.Listener { args ->
        try {
            val data = args.firstOrNull() as? JSONObject
            if (data != null) {
                android.util.Log.d(TAG, "💬 Received new message via Socket.IO: $data")
                
                // Extract message data
                val senderId = data.optString("sender_id", data.optString("senderId", ""))
                val senderName = data.optString("sender_name", data.optString("senderName", "Someone"))
                val messageContent = data.optString("content", data.optString("message", "New message"))
                val messageId = data.optString("id", data.optString("message_id", null))
                
                // Emit the new message event for UI to handle
                _newMessageReceived.value = data
                
                if (senderId.isNotEmpty()) {
                    // Don't show notification for messages I sent myself
                    if (senderId == currentUserId) {
                        android.util.Log.d(TAG, "🔕 Suppressing notification - message is from myself (senderId=$senderId == currentUserId=$currentUserId)")
                        return@Listener
                    }
                    
                    // Don't show notification if chat is currently open with this user
                    if (senderId == currentChatUserId) {
                        android.util.Log.d(TAG, "🔕 Suppressing notification - chat is open with $senderId")
                        return@Listener
                    }
                    
                    // Build dedupe id for messages
                    val dedupeId = NotificationDeduper.makeId(messageId, senderName, messageContent)
                    
                    if (NotificationDeduper.has(dedupeId)) {
                        android.util.Log.d(TAG, "⏭ Duplicate message notification (id=$dedupeId), skipping")
                    } else {
                        notificationService.showMessageNotification(
                            senderName = senderName,
                            messageContent = messageContent,
                            senderId = senderId,
                            messageId = messageId
                        )
                        
                        NotificationDeduper.record(dedupeId)
                        android.util.Log.d(TAG, "✅ Showed message notification from $senderName")
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ Error processing new message event", e)
        }
    }
    
    private val onUserTyping = Emitter.Listener { args ->
        try {
            val data = args.firstOrNull() as? JSONObject
            if (data != null) {
                val userId = data.optString("userId", data.optString("user_id", ""))
                if (userId.isNotEmpty()) {
                    // Don't show typing indicator for our own messages
                    if (userId == currentUserId) {
                        android.util.Log.d(TAG, "⏭ Ignoring own typing event")
                        return@Listener
                    }
                    
                    android.util.Log.d(TAG, "⌨️ User $userId is typing")
                    _userTyping.value = userId to true
                }
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ Error processing typing event", e)
        }
    }
    
    private val onUserStopTyping = Emitter.Listener { args ->
        try {
            val data = args.firstOrNull() as? JSONObject
            if (data != null) {
                val userId = data.optString("userId", data.optString("user_id", ""))
                if (userId.isNotEmpty()) {
                    // Don't show typing indicator for our own messages
                    if (userId == currentUserId) {
                        android.util.Log.d(TAG, "⏭ Ignoring own stop typing event")
                        return@Listener
                    }
                    
                    android.util.Log.d(TAG, "⏸️ User $userId stopped typing")
                    _userTyping.value = userId to false
                }
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ Error processing stop typing event", e)
        }
    }
    
    private val onMessageDelivered = Emitter.Listener { args ->
        try {
            if (args.isNotEmpty()) {
                val data = args[0] as? JSONObject
                val messageId = data?.optString("messageId")
                val deliveredAt = data?.optString("deliveredAt")
                android.util.Log.d(TAG, "✓✓ Message delivered: $messageId at $deliveredAt")
                if (messageId != null && deliveredAt != null) {
                    _messageDelivered.value = Pair(messageId, deliveredAt)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error handling messageDelivered event", e)
        }
    }
    
    private val onMessagesRead = Emitter.Listener { args ->
        try {
            if (args.isNotEmpty()) {
                val data = args[0] as? JSONObject
                val messageIdsArray = data?.optJSONArray("messageIds")
                val readAt = data?.optString("readAt")
                
                val messageIds = mutableListOf<String>()
                if (messageIdsArray != null) {
                    for (i in 0 until messageIdsArray.length()) {
                        messageIds.add(messageIdsArray.getString(i))
                    }
                }
                
                android.util.Log.d(TAG, "✓✓ Messages read: ${messageIds.size} messages at $readAt")
                if (messageIds.isNotEmpty() && readAt != null) {
                    _messagesRead.value = Pair(messageIds, readAt)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error handling messagesRead event", e)
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
    
    /**
     * Request fresh online members list from server.
     * Call this on pull-to-refresh to get accurate online status.
     */
    fun refreshOnlineMembers() {
        if (isConnected && socket != null) {
            android.util.Log.d(TAG, "🔄 Refreshing online members list...")
            socket?.emit("get-online-members")
        } else {
            android.util.Log.w(TAG, "⚠️ Cannot refresh online members - not connected")
            // Clear stale data if not connected
            _onlineMembers.value = emptyList()
        }
    }
    
    fun disconnect() {
        if (socket != null) {
            android.util.Log.d(TAG, "🔌 Disconnecting from Socket.IO server...")
            socket?.off()
            socket?.disconnect()
            socket = null
            isConnected = false
            // Clear online members to avoid showing stale data on reconnect
            _onlineMembers.value = emptyList()
            android.util.Log.d(TAG, "🔌 Cleared online members state")
        }
    }
    
    fun isSocketConnected(): Boolean {
        return isConnected && socket?.connected() == true
    }
    
    fun markMessagesAsRead(conversationPartnerId: String) {
        try {
            if (!isConnected || socket == null) {
                android.util.Log.w(TAG, "⚠️ Cannot mark messages as read - not connected")
                return
            }
            
            val data = JSONObject().apply {
                put("conversationPartnerId", conversationPartnerId)
            }
            
            socket?.emit("markMessagesRead", data)
            android.util.Log.d(TAG, "👁️ Marked messages as read for user: $conversationPartnerId")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error marking messages as read", e)
        }
    }
    
    /**
     * Emit when user opens a conversation
     */
    fun emitOpenConversation(conversationPartnerId: String) {
        try {
            val data = JSONObject().apply {
                put("conversationPartnerId", conversationPartnerId)
            }
            socket?.emit("openConversation", data)
            android.util.Log.d(TAG, "👁️ Opened conversation with: $conversationPartnerId")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error emitting openConversation", e)
        }
    }
    
    /**
     * Emit when user closes a conversation
     */
    fun emitCloseConversation() {
        try {
            socket?.emit("closeConversation")
            android.util.Log.d(TAG, "🚪 Closed conversation")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error emitting closeConversation", e)
        }
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
    
    /**
     * Emit typing event
     */
    fun emitTyping(receiverId: String) {
        if (isConnected && socket?.connected() == true) {
            val data = JSONObject().apply {
                put("receiverId", receiverId)
            }
            socket?.emit("typing", data)
            android.util.Log.d(TAG, "⌨️ Emitting typing to $receiverId")
        }
    }
    
    /**
     * Emit stop typing event
     */
    fun emitStopTyping(receiverId: String) {
        if (isConnected && socket?.connected() == true) {
            val data = JSONObject().apply {
                put("receiverId", receiverId)
            }
            socket?.emit("stop_typing", data)
            android.util.Log.d(TAG, "⏸️ Emitting stop typing to $receiverId")
        }
    }
}
