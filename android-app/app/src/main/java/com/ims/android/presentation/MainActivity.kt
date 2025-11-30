package com.ims.android.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.Lifecycle
import androidx.core.app.ActivityCompat
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.ims.android.presentation.theme.StockNexusTheme
import com.ims.android.data.api.ApiClient
import com.ims.android.data.repository.*
import com.ims.android.data.repository.ICADeliveryRepository
import com.ims.android.data.model.*
import com.ims.android.navigation.AppDestinations
import com.ims.android.ui.components.AppDrawer
import com.ims.android.ui.components.TopAppBar
import com.ims.android.ui.screens.InboxScreen
import com.ims.android.ui.screens.MessengerChatScreen
import com.ims.android.ui.components.SearchDialog
import com.ims.android.ui.components.NotificationsDropdown
import com.ims.android.ui.screens.DashboardScreen
import com.ims.android.ui.screens.StockScreen
import com.ims.android.ui.screens.StockInScreen
import com.ims.android.ui.screens.ICADeliveryListScreen
import com.ims.android.ui.screens.StaffScreen
import com.ims.android.ui.screens.ActivityLogsScreen
import com.ims.android.ui.screens.SettingsScreen
import com.ims.android.ui.screens.NotificationsScreen
import com.ims.android.ui.screens.ItemsScreen
import com.ims.android.ui.screens.auth.EnhancedAuthScreen
import com.ims.android.ui.screens.auth.SplashScreen
import com.ims.android.ui.viewmodel.AuthViewModel
import com.ims.android.ui.viewmodel.AuthViewModelFactory
import com.ims.android.service.SocketIOService
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll

// Composition local for permission launcher
data class PermissionLauncher(
    val requestPermission: (String, (Boolean) -> Unit) -> Unit
)

val LocalPermissionLauncher = compositionLocalOf<PermissionLauncher?> { null }

class MainActivity : ComponentActivity() {
    
    private var onPermissionResult: ((Boolean) -> Unit)? = null
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        android.util.Log.d("MainActivity", "Notification permission granted: $isGranted")
        onPermissionResult?.invoke(isGranted)
        onPermissionResult = null
    }
    
    // Mutable state for notification intent extras (can be updated by onNewIntent)
    // Use Pair<userId, timestamp> to trigger navigation even for same user
    private val notificationData = mutableStateOf<Pair<String, Long>?>(null)
    // State for opening moveout dialog from notification
    private val openMoveoutFromNotification = mutableStateOf(false)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Don't request notification permission automatically
        // It will be requested only when user enables notifications in settings
        
        // Extract notification intent extras from initial intent
        extractNotificationExtras(intent)
        
        setContent {
            // State for theme toggle
            var isDarkTheme by remember { mutableStateOf(true) }
            
            // Create permission launcher provider
            val permissionLauncher = remember {
                PermissionLauncher { permission, callback ->
                    onPermissionResult = callback
                    requestPermissionLauncher.launch(permission)
                }
            }
            
            StockNexusTheme(darkTheme = isDarkTheme) {
                CompositionLocalProvider(LocalPermissionLauncher provides permissionLauncher) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        StockNexusApp(
                            isDarkTheme = isDarkTheme,
                            onThemeToggle = { isDarkTheme = !isDarkTheme },
                            notificationData = notificationData.value,
                            openMoveoutFromNotification = openMoveoutFromNotification.value,
                            onMoveoutNotificationHandled = { openMoveoutFromNotification.value = false }
                        )
                    }
                }
            }
        }
    }
    
    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        android.util.Log.d("MainActivity", "onNewIntent received: $intent")
        // Extract notification extras and update state to trigger navigation
        extractNotificationExtras(intent)
    }
    
    private fun extractNotificationExtras(intent: android.content.Intent?) {
        val openChat = intent?.getBooleanExtra("open_chat", false) ?: false
        val userId = intent?.getStringExtra("user_id")
        val openMoveout = intent?.getBooleanExtra("open_moveout", false) ?: false
        val notificationType = intent?.getStringExtra("notification_type") ?: "none"
        
        android.util.Log.d("MainActivity", "📱 extractNotificationExtras: type=$notificationType, openMoveout=$openMoveout, openChat=$openChat")
        
        if (openChat && !userId.isNullOrEmpty()) {
            android.util.Log.d("MainActivity", "📱 Captured notification intent: open_chat=$openChat, user_id=$userId")
            // Use timestamp to ensure LaunchedEffect triggers even for same user
            notificationData.value = Pair(userId, System.currentTimeMillis())
        }
        
        if (openMoveout) {
            android.util.Log.d("MainActivity", "📱 Setting openMoveoutFromNotification = true")
            openMoveoutFromNotification.value = true
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockNexusApp(
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit,
    notificationData: Pair<String, Long>? = null,
    openMoveoutFromNotification: Boolean = false,
    onMoveoutNotificationHandled: () -> Unit = {}
) {
    val context = LocalContext.current
    
    // Initialize API client and repositories
    val apiClient = remember { ApiClient.getInstance(context) }
    val enhancedAuthRepository = remember { EnhancedAuthRepository(context, apiClient) }
    val dashboardRepository = remember { DashboardRepository(apiClient) }
    val inventoryRepository = remember { InventoryRepository(apiClient) }
    val moveoutRepository = remember { MoveoutRepository(apiClient) }
    val calendarRepository = remember { CalendarRepository(apiClient) }
    val staffRepository = remember { StaffRepository(apiClient) }
    val branchRepository = remember { BranchRepository(apiClient) }
    val analyticsRepository = remember { AnalyticsRepository(apiClient) }
    val notificationRepository = remember { NotificationRepository(apiClient) }
    val icaDeliveryRepository = remember { ICADeliveryRepository(apiClient) }
    val socketIOService = remember { 
        SocketIOService(context).also { 
            SocketIOService.setInstance(it)
        }
    }
    
    // Enhanced authentication with ViewModel
    val authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModelFactory(enhancedAuthRepository)
    )
    
    val authState by authViewModel.authState.collectAsState()
    val currentScreen by authViewModel.currentScreen.collectAsState()
    
    val currentAuthState = authState
    when (currentAuthState) {
        is AuthState.Loading -> {
            if (currentScreen == AuthScreen.Splash) {
                SplashScreen(
                    authRepository = enhancedAuthRepository,
                    onNavigateToAuth = {
                        authViewModel.navigateToScreen(AuthScreen.SignIn)
                    },
                    onNavigateToDashboard = { user ->
                        // User is already authenticated
                    }
                )
            } else {
                EnhancedAuthScreen(
                    authRepository = enhancedAuthRepository,
                    onLoginSuccess = { user ->
                        authViewModel.setAuthenticated(user)
                    }
                )
            }
        }
        
        is AuthState.Unauthenticated -> {
            EnhancedAuthScreen(
                authRepository = enhancedAuthRepository,
                onLoginSuccess = { user ->
                    android.util.Log.d("MainActivity", "onLoginSuccess called for user: ${user.email}, calling setAuthenticated")
                    authViewModel.setAuthenticated(user)
                }
            )
        }
        
        is AuthState.Authenticated -> {
            val authenticatedState = currentAuthState as AuthState.Authenticated
            AuthenticatedApp(
                user = authenticatedState.user,
                apiClient = apiClient,
                dashboardRepository = dashboardRepository,
                inventoryRepository = inventoryRepository,
                moveoutRepository = moveoutRepository,
                calendarRepository = calendarRepository,
                icaDeliveryRepository = icaDeliveryRepository,
                enhancedAuthRepository = enhancedAuthRepository,
                staffRepository = staffRepository,
                socketIOService = socketIOService,
                isDarkTheme = isDarkTheme,
                onThemeToggle = onThemeToggle,
                onLogout = {
                    authViewModel.signOut()
                    // ViewModel handles state update
                },
                notificationData = notificationData,
                // When app resumes we should refresh the profile to pick up any server-side
                // changes (e.g. toggles enabled on the web). Pass a callback that triggers
                // the ViewModel to refresh the profile.
                onResumeRefresh = { authViewModel.refreshProfile() },
                openMoveoutFromNotification = openMoveoutFromNotification,
                onMoveoutNotificationHandled = onMoveoutNotificationHandled
            )
        }
        
        is AuthState.Error -> {
            // Show error screen or fallback to auth
            EnhancedAuthScreen(
                authRepository = enhancedAuthRepository,
                onLoginSuccess = { _ ->
                    authViewModel.checkAuthState()
                }
            )
        }
        
        else -> {
            // Handle other states like email verification
            EnhancedAuthScreen(
                authRepository = enhancedAuthRepository,
                onLoginSuccess = { _ ->
                    authViewModel.checkAuthState()
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable

fun AuthenticatedApp(
    user: User,
    apiClient: ApiClient,
    dashboardRepository: DashboardRepository,
    inventoryRepository: InventoryRepository,
    moveoutRepository: MoveoutRepository,
    calendarRepository: CalendarRepository,
    icaDeliveryRepository: ICADeliveryRepository,
    enhancedAuthRepository: EnhancedAuthRepository,
    staffRepository: StaffRepository,
    socketIOService: SocketIOService,
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit,
    onLogout: () -> Unit,
    notificationData: Pair<String, Long>? = null,
    onResumeRefresh: () -> Unit = {},
    openMoveoutFromNotification: Boolean = false,
    onMoveoutNotificationHandled: () -> Unit = {}
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    // Navigate to chat from notification intent - timestamp ensures this triggers on every tap
    LaunchedEffect(notificationData) {
        notificationData?.let { (userId, timestamp) ->
            android.util.Log.d("MainActivity", "💬 Navigating to chat from notification for user: $userId (ts=$timestamp)")
            // Navigate immediately without delay
            navController.navigate("chat/$userId")
        }
    }
    
    // Messaging state
    var threads by remember { mutableStateOf(listOf<com.ims.android.data.model.Thread>()) }
    var messages by remember { mutableStateOf(mapOf<String, List<com.ims.android.data.model.Message>>()) }
    var composeDialogState by remember { mutableStateOf(false) }
    var pendingComposeResult: Pair<String, String>? by remember { mutableStateOf(null) }
    var currentChatUserId by remember { mutableStateOf<String?>(null) } // Track active chat to suppress notifications

    // Cache read/delivered status so reloading/refresh won't lose socket-updated state
    var messageDeliveredCache by remember { mutableStateOf(mapOf<String, java.time.Instant?>()) }
    var messageReadCache by remember { mutableStateOf(mapOf<String, java.time.Instant?>()) }
    
    // Users for compose dialog
    var usersForCompose by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    
    // Fetch users when compose dialog opens
    LaunchedEffect(composeDialogState) {
        if (composeDialogState) {
            android.util.Log.d("MainActivity", "📝 Compose dialog opened, fetching users...")
            try {
                android.util.Log.d("MainActivity", "📝 Calling staffRepository.getStaff()...")
                val result = staffRepository.getStaff()
                android.util.Log.d("MainActivity", "📝 Staff API result: ${result.isSuccess}, data: ${result.getOrNull()?.size} users")
                usersForCompose = result.getOrNull()?.map { Pair(it.id, it.name) } ?: emptyList()
                android.util.Log.d("MainActivity", "📝 Users loaded: ${usersForCompose.size} users")
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "📝 Error fetching users: ${e.message}", e)
                usersForCompose = emptyList()
            }
        }
    }
    
    // Collect online members for messaging
    val onlineMembers by socketIOService.onlineMembers.collectAsState(initial = emptyList())
    
    // Collect new messages from Socket.IO
    val newMessageReceived by socketIOService.newMessageReceived.collectAsState(initial = null)
    
    // Collect typing indicators
    val userTypingState by socketIOService.userTyping.collectAsState(initial = null)
    
    // Collect message delivery updates
    val messageDelivered by socketIOService.messageDelivered.collectAsState(initial = null)
    
    // Collect messages read updates
    val messagesRead by socketIOService.messagesRead.collectAsState(initial = null)
    
    // Track typing status for each user
    var typingUsers by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    
    // Update typing users when Socket.IO event received
    LaunchedEffect(userTypingState) {
        userTypingState?.let { (userId, isTyping) ->
            android.util.Log.d("MainActivity", "⌨️ Typing state updated: userId=$userId, isTyping=$isTyping")
            typingUsers = typingUsers + (userId to isTyping)
            android.util.Log.d("MainActivity", "⌨️ Current typing users: $typingUsers")
            // Clear typing after 3 seconds if no update
            if (isTyping) {
                kotlinx.coroutines.delay(3000)
                if (typingUsers[userId] == true) {
                    typingUsers = typingUsers + (userId to false)
                    android.util.Log.d("MainActivity", "⏸️ Auto-cleared typing for $userId after 3s")
                }
            }
        }
    }
    
    // Handle new messages from Socket.IO
    LaunchedEffect(newMessageReceived) {
        newMessageReceived?.let { data ->
            try {
                val senderId = data.optString("sender_id", data.optString("senderId", ""))
                val receiverId = data.optString("receiver_id", data.optString("receiverId", ""))
                val content = data.optString("content", "")
                val messageId = data.optString("id", data.optString("message_id", java.util.UUID.randomUUID().toString()))
                val sentAt = data.optString("sent_at", data.optString("created_at", java.time.Instant.now().toString()))
                val deliveredAt = data.optString("delivered_at", data.optString("deliveredAt", ""))
                val readAt = data.optString("read_at", data.optString("readAt", ""))
                
                // Helper to safely parse timestamp - handles "null" string and empty values
                fun parseTimestamp(value: String): java.time.Instant? {
                    return if (value.isNotEmpty() && value != "null") {
                        try { java.time.Instant.parse(value) } catch (e: Exception) { null }
                    } else null
                }
                
                if (senderId.isNotEmpty() && content.isNotEmpty()) {
                    // Determine conversation partner
                    // Note: do NOT skip processing when senderId == current user —
                    // server may emit updated read_at/delivered_at to the sender and we must
                    // apply those updates so the UI shows two ticks instantly.
                    val conversationPartner = if (senderId == user.id) receiverId else senderId
                    
                    val parsedSentAt = parseTimestamp(sentAt) ?: java.time.Instant.now()
                    
                    val newMessage = com.ims.android.data.model.Message(
                        id = messageId,
                        senderId = senderId,
                        receiverId = receiverId,
                        content = content,
                        sentAt = parsedSentAt,
                        deliveredAt = parseTimestamp(deliveredAt),
                        readAt = parseTimestamp(readAt),
                        fcmMessageId = null
                    )
                    
                    // Add or update message
                    val currentMessages = messages[conversationPartner].orEmpty()
                    val existingIndex = currentMessages.indexOfFirst { it.id == messageId }
                    if (existingIndex >= 0) {
                        // Update existing message with latest status
                        val updatedMessages = currentMessages.toMutableList()
                        updatedMessages[existingIndex] = newMessage
                        messages = messages + (conversationPartner to updatedMessages.sortedBy { it.sentAt })
                        // update caches if this message contains delivery/read info
                        newMessage.deliveredAt?.let { dt ->
                            messageDeliveredCache = messageDeliveredCache + (messageId to dt)
                            // persist delivered cache
                            scope.launch {
                                try { apiClient.saveMessageDeliveredCache(messageDeliveredCache.mapValues { it.value?.toString() ?: "" }) } catch (e: Exception) { android.util.Log.e("MainActivity", "Error saving delivered cache", e) }
                            }
                        }
                        newMessage.readAt?.let { rt ->
                            messageReadCache = messageReadCache + (messageId to rt)
                            // persist read cache
                            scope.launch {
                                try { apiClient.saveMessageReadCache(messageReadCache.mapValues { it.value?.toString() ?: "" }) } catch (e: Exception) { android.util.Log.e("MainActivity", "Error saving read cache", e) }
                            }
                        }
                        android.util.Log.d("MainActivity", "💬 Updated message from Socket.IO: $messageId (readAt=${newMessage.readAt})")
                    } else {
                        // Add new message
                        messages = messages + (conversationPartner to (currentMessages + newMessage).sortedBy { it.sentAt })
                        // store statuses in caches
                        newMessage.deliveredAt?.let { dt -> messageDeliveredCache = messageDeliveredCache + (messageId to dt) }
                        newMessage.readAt?.let { rt -> messageReadCache = messageReadCache + (messageId to rt) }
                        android.util.Log.d("MainActivity", "💬 Added new message from Socket.IO: $messageId (readAt=${newMessage.readAt})")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Error processing new message", e)
            }
        }
    }
    
    // Handle message delivered updates
    LaunchedEffect(messageDelivered) {
        messageDelivered?.let { (messageId, deliveredAt) ->
            try {
                android.util.Log.d("MainActivity", "✓✓ Updating message $messageId as delivered at $deliveredAt")
                // Update the message in the messages map
                messages = messages.mapValues { (_, messageList) ->
                    messageList.map { msg ->
                        if (msg.id == messageId) {
                            // update cache
                            messageDeliveredCache = messageDeliveredCache + (messageId to java.time.Instant.parse(deliveredAt))
                            // persist delivered cache
                            scope.launch {
                                try { apiClient.saveMessageDeliveredCache(messageDeliveredCache.mapValues { it.value?.toString() ?: "" }) } catch (e: Exception) { android.util.Log.e("MainActivity", "Error saving delivered cache", e) }
                            }
                            msg.copy(deliveredAt = java.time.Instant.parse(deliveredAt))
                        } else {
                            msg
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Error updating delivered status", e)
            }
        }
    }
    
    // Handle messages read updates
    LaunchedEffect(messagesRead) {
        messagesRead?.let { (messageIds, readAt) ->
            try {
                android.util.Log.d("MainActivity", "✓✓ Updating ${messageIds.size} messages as read at $readAt")
                val readInstant = java.time.Instant.parse(readAt)
                // Update all messages in the list
                // Also update read cache so subsequent API refreshes keep read state
                messageIds.forEach { id ->
                    messageReadCache = messageReadCache + (id to readInstant)
                }
                // persist read cache
                scope.launch {
                    try { apiClient.saveMessageReadCache(messageReadCache.mapValues { it.value?.toString() ?: "" }) } catch (e: Exception) { android.util.Log.e("MainActivity", "Error saving read cache", e) }
                }

                messages = messages.mapValues { (conversationId, messageList) ->
                    messageList.map { msg ->
                        if (messageIds.contains(msg.id)) {
                            android.util.Log.d("MainActivity", "✓✓ Marking message ${msg.id} as read")
                            msg.copy(readAt = readInstant)
                        } else {
                            msg
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Error updating read status", e)
            }
        }
    }
    
    // Search dialog state
    var showSearchDialog by remember { mutableStateOf(false) }
    
    // Notification dropdown state
    var showNotificationsDropdown by remember { mutableStateOf(false) }
    var notificationCount by remember { mutableStateOf(0) }
    
    // State to hold the current user that can be refreshed
    var currentUserState by remember { mutableStateOf(user) }
    
    // Get permission launcher from composition local
    val permissionLauncher = LocalPermissionLauncher.current
    
    // Check notification permission whenever the user's notification settings change
    // This allows toggles enabled remotely (e.g., web app) to trigger a permission request
    LaunchedEffect(user.notificationSettings) {
        android.util.Log.d("MainActivity", "🔔 LaunchedEffect triggered for user: ${user.id} (notificationSettings changed)")
        android.util.Log.d("MainActivity", "🔔 Android version: ${Build.VERSION.SDK_INT}, TIRAMISU: ${Build.VERSION_CODES.TIRAMISU}")
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            android.util.Log.d("MainActivity", "🔔 Device supports notification permissions")
            
            // Check if user has any notification settings enabled
            val notificationSettings = user.notificationSettings
            android.util.Log.d("MainActivity", "🔔 Notification settings: $notificationSettings")
            
            val hasAnyNotificationEnabled = notificationSettings?.get("email") == true ||
                                           notificationSettings?.get("sms") == true ||
                                           notificationSettings?.get("whatsapp") == true ||
                                           notificationSettings?.get("stockLevelAlerts") == true ||
                                           notificationSettings?.get("eventReminders") == true ||
                                           notificationSettings?.get("softdrinkTrends") == true
            
            android.util.Log.d("MainActivity", "🔔 Has any notification enabled: $hasAnyNotificationEnabled")
            
            if (hasAnyNotificationEnabled) {
                android.util.Log.d("MainActivity", "✅ User has notifications enabled, checking permission")
                
                // Check if permission is already granted
                val permissionGranted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
                
                android.util.Log.d("MainActivity", "🔔 Permission granted: $permissionGranted")
                
                if (!permissionGranted) {
                    android.util.Log.d("MainActivity", "⚠️ Requesting notification permission")
                    
                    // Request permission
                    permissionLauncher?.requestPermission?.invoke(Manifest.permission.POST_NOTIFICATIONS) { _ ->
                        // Wait briefly then re-check system permission state — avoids race
                        // where the OS hasn't applied the user's decision yet.
                        scope.launch {
                            kotlinx.coroutines.delay(350)

                            val finalGrantState = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) == PackageManager.PERMISSION_GRANTED

                            android.util.Log.d("MainActivity", "POST_NOTIFICATIONS finalGrantState: $finalGrantState")

                            if (!finalGrantState) {
                                // Did the user actively deny (dialog shown) OR is the permission
                                // permanently blocked? Use shouldShowRequestPermissionRationale
                                // to differentiate. If rationale is true, that means the user
                                // denied the permission and we can safely disable toggles.
                                val activity = context as? ComponentActivity
                                val shouldShowRationale = activity?.let {
                                    ActivityCompat.shouldShowRequestPermissionRationale(
                                        it,
                                        Manifest.permission.POST_NOTIFICATIONS
                                    )
                                } ?: false

                                android.util.Log.d("MainActivity", "shouldShowRequestPermissionRationale: $shouldShowRationale")

                                if (shouldShowRationale) {
                                    // Permission denied — turn off all toggles in backend
                                    android.util.Log.d("MainActivity", "Permission denied, disabling all notifications")
                                    val disabledSettings = mapOf(
                                    "email" to false,
                                    "sms" to false,
                                    "whatsapp" to false,
                                    "stockLevelAlerts" to false,
                                    "eventReminders" to false,
                                    "softdrinkTrends" to false
                                )
                                    enhancedAuthRepository.updateNotificationSettings(disabledSettings)

                                    // Refresh user state
                                    val refreshResult = enhancedAuthRepository.refreshProfile()
                                    if (refreshResult.isSuccess) {
                                        refreshResult.getOrNull()?.let { updatedUser ->
                                            currentUserState = updatedUser
                                        }
                                    }
                                } else {
                                    // If rationale is false, we either didn't show a dialog
                                    // (permission blocked by policy or first request without
                                    // rationales), so DON'T auto-disable server toggles. Log
                                    // so we can inspect this behavior during testing.
                                    android.util.Log.d(
                                        "MainActivity",
                                        "Permission request did not show dialog or is permanently blocked — not changing server toggles"
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Callback to refresh profile immediately
    val refreshProfile: () -> Unit = {
        scope.launch {
            val refreshResult = enhancedAuthRepository.refreshProfile()
            if (refreshResult.isSuccess) {
                refreshResult.getOrNull()?.let { updatedUser ->
                    currentUserState = updatedUser
                }
            }
        }
    }
    
    // Connect to Socket.IO for real-time notifications
    LaunchedEffect(user) {
        scope.launch {
            val token = apiClient.getAccessToken()
            val branchId = user.branchId ?: ""
            val userId = user.id
            if (token != null && branchId.isNotEmpty()) {
                android.util.Log.d("MainActivity", "🔌 Connecting to Socket.IO with branch: $branchId, user: $userId")
                socketIOService.connect(token, branchId, userId)
            }
        }
    }

    // When the app resumes (foreground), refresh the profile to pick up remote changes.
    // Also handle user-away/user-back for online presence
    val lifecycle = (context as? ComponentActivity)?.lifecycle
    DisposableEffect(lifecycle) {
        if (lifecycle == null) {
            onDispose { }
        } else {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> {
                        android.util.Log.d("AuthenticatedApp", "Lifecycle resumed — refreshing profile")
                        onResumeRefresh()
                        // Notify server user is back (active)
                        socketIOService.emitUserBack()
                    }
                    Lifecycle.Event.ON_PAUSE -> {
                        // Notify server user is away (app backgrounded)
                        android.util.Log.d("AuthenticatedApp", "Lifecycle paused — user away")
                        socketIOService.emitUserAway()
                    }
                    else -> { /* ignore other events */ }
                }
            }

            lifecycle.addObserver(observer)
            onDispose { lifecycle.removeObserver(observer) }
        }
    }
    
    // Disconnect Socket.IO when leaving
    DisposableEffect(Unit) {
        onDispose {
            android.util.Log.d("MainActivity", "🔌 Disconnecting Socket.IO")
            socketIOService.disconnect()
        }
    }
    
    // Load notification count
    LaunchedEffect(Unit) {
        scope.launch {
            // Only count unread notifications from API (matching web app behavior)
            val notifResult = NotificationRepository(apiClient).getNotifications()
            notifResult.onSuccess { notifications ->
                notificationCount = notifications.count { !it.isRead }
            }
        }
    }
    
    // User profiles cache for displaying names
    var userProfiles by remember { mutableStateOf<Map<String, Profile>>(emptyMap()) }
    
    // Fetch all staff members once at startup to populate user profiles
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                android.util.Log.d("MainActivity", "👥 Fetching all staff members for user profiles")
                val staffResult = staffRepository.getStaff()
                if (staffResult.isSuccess) {
                    val staffMembers = staffResult.getOrNull().orEmpty()
                    val profiles = staffMembers.associate { staff ->
                        staff.id to Profile(
                            id = staff.id,
                            name = staff.name,
                            email = staff.email,
                            role = staff.role,
                            photoUrl = staff.photoUrl,
                            branchId = staff.branchId,
                            branchName = null,
                            districtName = null,
                            regionName = null,
                            createdAt = "",
                            updatedAt = "",
                            accessCount = 0
                        )
                    }
                    userProfiles = profiles
                    android.util.Log.d("MainActivity", "✅ Loaded ${profiles.size} user profiles")
                } else {
                    android.util.Log.e("MainActivity", "❌ Failed to fetch staff: ${staffResult.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "❌ Error fetching staff: ${e.message}", e)
            }
        }
    }
    
    // Load messages and threads from backend
    LaunchedEffect(currentUserState.id) {
        scope.launch {
                // Load persisted message status caches (read/delivered) so they survive app restarts
                try {
                    val readMap = apiClient.loadMessageReadCache()
                    messageReadCache = readMap.mapValues { (_, v) ->
                        try {
                            if (!v.isNullOrBlank()) java.time.Instant.parse(v) else null
                        } catch (e: Exception) {
                            null
                        }
                    }
                    val deliveredMap = apiClient.loadMessageDeliveredCache()
                    messageDeliveredCache = deliveredMap.mapValues { (_, v) ->
                        try {
                            if (!v.isNullOrBlank()) java.time.Instant.parse(v) else null
                        } catch (e: Exception) {
                            null
                        }
                    }
                    android.util.Log.d("MainActivity", "📥 Loaded persisted message status caches: read=${messageReadCache.size}, delivered=${messageDeliveredCache.size}")
                } catch (e: Exception) {
                    android.util.Log.e("MainActivity", "Error loading persisted message caches", e)
                }
            try {
                android.util.Log.d("MainActivity", "📥 Loading messages and threads from backend database")
                
                // Fetch ALL messages from backend database first (not just local cache)
                val messagesResult = apiClient.getAllUserMessages()
                val localMessages = if (messagesResult.isSuccess) {
                    val backendMessages = messagesResult.getOrNull() ?: emptyList()
                    android.util.Log.d("MainActivity", "✅ Loaded ${backendMessages.size} messages from backend database")
                    backendMessages
                } else {
                    android.util.Log.w("MainActivity", "⚠️ Failed to load from backend, using local cache")
                    apiClient.getLocalMessages()
                }
                android.util.Log.d("MainActivity", "📥 Total messages loaded: ${localMessages.size}")
                
                // Group messages by conversation partner
                val messagesByConversation = mutableMapOf<String, MutableList<com.ims.android.data.model.Message>>()
                
                localMessages.forEach { apiMessage ->
                    // Convert API message to local message model
                    val localMessage = com.ims.android.data.model.Message(
                        id = apiMessage.id,
                        senderId = apiMessage.senderId,
                        receiverId = apiMessage.receiverId,
                        content = apiMessage.content,
                        sentAt = java.time.Instant.parse(apiMessage.sentAt),
                        deliveredAt = apiMessage.deliveredAt?.let { java.time.Instant.parse(it) },
                        readAt = apiMessage.readAt?.let { java.time.Instant.parse(it) },
                        fcmMessageId = apiMessage.fcmMessageId
                    )
                    
                    // Determine conversation partner (the other user in the conversation)
                    val conversationPartner = if (apiMessage.senderId == currentUserState.id) {
                        apiMessage.receiverId
                    } else {
                        apiMessage.senderId
                    }
                    
                    // Add to conversation
                    messagesByConversation.getOrPut(conversationPartner) { mutableListOf() }.add(localMessage)
                }
                
                // Sort messages in each conversation by timestamp
                messagesByConversation.forEach { (partnerId, messageList) ->
                    messageList.sortBy { it.sentAt }
                }
                
                // Calculate unread messages before updating state
                val unreadCount = messagesByConversation.values.flatten().count { message ->
                    message.senderId != currentUserState.id && message.readAt == null
                }
                android.util.Log.d("MainActivity", "📊 Setting messages state with ${messagesByConversation.values.flatten().size} total messages, $unreadCount unread")
                
                // Update messages state
                messages = messagesByConversation
                
                // Load threads from backend
                val threadsResult = apiClient.getAllUserThreads()
                val localThreads = if (threadsResult.isSuccess) {
                    val backendThreads = threadsResult.getOrNull() ?: emptyList()
                    android.util.Log.d("MainActivity", "✅ Loaded ${backendThreads.size} threads from backend database")
                    backendThreads
                } else {
                    android.util.Log.w("MainActivity", "⚠️ Failed to load threads from backend, using local cache")
                    apiClient.getLocalThreads()
                }
                android.util.Log.d("MainActivity", "📥 Thread details: ${localThreads.map { "User1: ${it.user1Id}, User2: ${it.user2Id}" }.joinToString()}")
                
                // Convert API threads to local thread model and fetch user profiles
                val localThreadsConverted = localThreads.map { apiThread ->
                    val otherUserId = if (apiThread.user1Id == currentUserState.id) apiThread.user2Id else apiThread.user1Id
                    
                    // Fetch profile if not already cached
                    if (!userProfiles.containsKey(otherUserId)) {
                        scope.launch {
                            val profileResult = apiClient.getUserProfile(otherUserId)
                            if (profileResult.isSuccess) {
                                profileResult.getOrNull()?.let { profile ->
                                    userProfiles = userProfiles + (otherUserId to profile)
                                    // Update thread with profile info
                                    threads = threads.map { thread ->
                                        if ((thread.user1Id == otherUserId || thread.user2Id == otherUserId) && thread.displayName == null) {
                                            thread.copy(displayName = profile.name, displayPhoto = profile.photoUrl)
                                        } else thread
                                    }
                                }
                            }
                        }
                    }
                    
                    com.ims.android.data.model.Thread(
                        id = apiThread.id,
                        user1Id = apiThread.user1Id,
                        user2Id = apiThread.user2Id,
                        lastMessageId = apiThread.lastMessageId,
                        updatedAt = java.time.Instant.parse(apiThread.updatedAt),
                        displayName = userProfiles[otherUserId]?.name,
                        displayPhoto = userProfiles[otherUserId]?.photoUrl
                    )
                }
                
                // Update threads state
                threads = localThreadsConverted.toMutableList()
                
                // If no threads exist in database, create threads from messages
                if (threads.isEmpty() && messagesByConversation.isNotEmpty()) {
                    android.util.Log.d("MainActivity", "🔨 No threads in database, creating threads from ${messagesByConversation.size} conversations")
                    
                    // First, fetch all missing profiles
                    val profileFetchJobs = messagesByConversation.keys
                        .filter { partnerId -> !userProfiles.containsKey(partnerId) }
                        .map { partnerId ->
                            scope.async {
                                try {
                                    val profileResult = apiClient.getUserProfile(partnerId)
                                    if (profileResult.isSuccess) {
                                        profileResult.getOrNull()?.let { profile ->
                                            partnerId to profile
                                        }
                                    } else null
                                } catch (e: Exception) {
                                    android.util.Log.e("MainActivity", "Error fetching profile for $partnerId: ${e.message}")
                                    null
                                }
                            }
                        }
                    
                    // Wait for all profiles to be fetched
                    val fetchedProfiles = profileFetchJobs.awaitAll().filterNotNull()
                    fetchedProfiles.forEach { (partnerId, profile) ->
                        userProfiles = userProfiles + (partnerId to profile)
                    }
                    
                    // Now create threads with profiles already loaded
                    messagesByConversation.forEach { (partnerId, conversationMessages) ->
                        if (conversationMessages.isNotEmpty()) {
                            val lastMessage = conversationMessages.maxByOrNull { it.sentAt }
                            val threadId = java.util.UUID.randomUUID().toString()
                            
                            // Check if this is a self-conversation
                            val isSelfConversation = partnerId == currentUserState.id
                            val displayName = if (isSelfConversation) {
                                "${currentUserState.name} (Me)"
                            } else {
                                userProfiles[partnerId]?.name
                            }
                            
                            val newThread = com.ims.android.data.model.Thread(
                                id = threadId,
                                user1Id = currentUserState.id,
                                user2Id = partnerId,
                                lastMessageId = lastMessage?.id,
                                updatedAt = lastMessage?.sentAt ?: java.time.Instant.now(),
                                displayName = displayName,
                                displayPhoto = if (isSelfConversation) currentUserState.photoUrl else userProfiles[partnerId]?.photoUrl
                            )
                            
                            threads = threads + newThread
                        }
                    }
                    
                    android.util.Log.d("MainActivity", "✅ Created ${threads.size} threads from messages")
                }
                
                android.util.Log.d("MainActivity", "📥 Loaded messages for ${messagesByConversation.size} conversations and ${threads.size} threads")
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "❌ Error loading messages and threads from storage: ${e.message}", e)
            }
        }
    }
    
    // Refresh profile when coming back from settings
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    LaunchedEffect(currentBackStackEntry) {
        val route = currentBackStackEntry?.destination?.route
        // Refresh profile when navigating away from settings or when app starts
        if (route != null && route != AppDestinations.Settings) {
            scope.launch {
                val refreshResult = enhancedAuthRepository.refreshProfile()
                if (refreshResult.isSuccess) {
                    refreshResult.getOrNull()?.let { updatedUser ->
                        currentUserState = updatedUser
                    }
                }
                
                // Refresh notification count (only unread notifications)
                val notifResult = NotificationRepository(apiClient).getNotifications()
                notifResult.onSuccess { notifications ->
                    notificationCount = notifications.count { !it.isRead }
                }
            }
        }
    }
    
    // Convert to legacy User and Profile for compatibility
    val currentUser = User(
        id = currentUserState.id,
        email = currentUserState.email,
        createdAt = currentUserState.createdAt,
        updatedAt = currentUserState.createdAt
    )
    
    val currentProfile = Profile(
        id = currentUserState.id,
        name = currentUserState.name,
        email = currentUserState.email,
        role = currentUserState.role,
        photoUrl = currentUserState.photoUrl,
        branchId = currentUserState.branchId,
        branchName = currentUserState.branchName,
        districtName = currentUserState.districtName,
        regionName = currentUserState.regionName,
        createdAt = currentUserState.createdAt,
        updatedAt = currentUserState.createdAt,
        accessCount = currentUserState.accessCount
    )
    
    // Main app with navigation
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawer(
                navController = navController,
                userProfile = currentProfile,
                onCloseDrawer = {
                    scope.launch { drawerState.close() }
                },
                onLogout = onLogout
            )
        }
    ) {
        Box {
            Scaffold(
                topBar = {
                    val currentBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = currentBackStackEntry?.destination?.route
                    
                    // collect online members from socket service (for admin UI)
                    val onlineMembers by socketIOService.onlineMembers.collectAsState(initial = emptyList())
                    
                    // Calculate total unread messages count - recalculates whenever messages change
                    val unreadMessagesCount = remember(messages) {
                        val count = messages.values.flatten().count { message ->
                            message.senderId != currentUserState.id && message.readAt == null
                        }
                        android.util.Log.d("MainActivity", "📊 Unread messages count: $count (total messages: ${messages.values.flatten().size})")
                        count
                    }

                    TopAppBar(
                        title = getScreenTitle(currentRoute),
                        userProfile = currentProfile,
                        onNavigationClick = {
                            scope.launch { drawerState.open() }
                        },
                        onThemeToggle = onThemeToggle,
                        onNotificationsClick = {
                            showNotificationsDropdown = true
                        },
                        onSearchClick = {
                            showSearchDialog = true
                        },
                        notificationCount = notificationCount,
                        isDarkTheme = isDarkTheme
                        ,
                        onlineMembers = onlineMembers
                            ,
                            onInboxClick = {
                                navController.navigate("inbox")
                            },
                            onAvatarClick = { member ->
                                navController.navigate("chat/${member.id}")
                            },
                            unreadMessagesCount = unreadMessagesCount
                        )
                }
            ) { paddingValues ->
                NavHost(
                    navController = navController,
                    startDestination = AppDestinations.Dashboard,
                    modifier = Modifier.padding(paddingValues)
                ) {
                    composable(AppDestinations.Dashboard) {
                        DashboardScreen(
                            dashboardRepository = dashboardRepository,
                            moveoutRepository = moveoutRepository,
                            inventoryRepository = inventoryRepository,
                            icaDeliveryRepository = icaDeliveryRepository,
                            calendarRepository = calendarRepository,
                            socketIOService = socketIOService,
                            authRepository = enhancedAuthRepository,
                            apiClient = apiClient,
                            onSignOut = onLogout,
                            openMoveoutFromNotification = openMoveoutFromNotification,
                            onMoveoutNotificationHandled = onMoveoutNotificationHandled
                        )
                    }
                    composable("inbox") {
                        InboxScreen(
                            threads = threads,
                            messages = messages,
                            onThreadClick = { thread ->
                                navController.navigate("chat/${thread.user2Id.toString()}")
                            },
                            onComposeClick = { composeDialogState = true },
                            onlineMembers = onlineMembers.map { it.id },
                            currentUserId = currentUserState.id,
                            onRefresh = {
                                // Full refresh: messages, online status, and notification badges
                                android.util.Log.d("MainActivity", "🔄 Pull-to-refresh triggered - full refresh")
                                
                                // 1. Refresh online members status
                                socketIOService.refreshOnlineMembers()
                                android.util.Log.d("MainActivity", "🔄 Requested fresh online members list")
                                
                                // 2. Reload messages from backend
                                try {
                                    val messagesResult = apiClient.getAllUserMessages()
                                    if (messagesResult.isSuccess) {
                                        val backendMessages = messagesResult.getOrNull() ?: emptyList()
                                        val messagesByConversation = mutableMapOf<String, MutableList<com.ims.android.data.model.Message>>()
                                        
                                        backendMessages.forEach { apiMessage ->
                                            val localMessage = com.ims.android.data.model.Message(
                                                id = apiMessage.id,
                                                senderId = apiMessage.senderId,
                                                receiverId = apiMessage.receiverId,
                                                content = apiMessage.content,
                                                sentAt = java.time.Instant.parse(apiMessage.sentAt),
                                                deliveredAt = apiMessage.deliveredAt?.let { java.time.Instant.parse(it) },
                                                readAt = apiMessage.readAt?.let { java.time.Instant.parse(it) },
                                                fcmMessageId = apiMessage.fcmMessageId
                                            )
                                            val conversationPartner = if (apiMessage.senderId == currentUserState.id) {
                                                apiMessage.receiverId
                                            } else {
                                                apiMessage.senderId
                                            }
                                            messagesByConversation.getOrPut(conversationPartner) { mutableListOf() }.add(localMessage)
                                        }
                                        
                                        messagesByConversation.forEach { (_, messageList) ->
                                            messageList.sortBy { it.sentAt }
                                        }
                                        
                                        messages = messagesByConversation
                                        android.util.Log.d("MainActivity", "✅ Refreshed ${backendMessages.size} messages")
                                        
                                        // 3. Log updated unread count (calculated dynamically by TopAppBar)
                                        val totalUnread = messagesByConversation.values.sumOf { conversationMessages ->
                                            conversationMessages.count { msg ->
                                                msg.senderId != currentUserState.id && msg.readAt == null
                                            }
                                        }
                                        android.util.Log.d("MainActivity", "🔢 Updated unread count: $totalUnread")
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("MainActivity", "❌ Refresh failed: ${e.message}")
                                }
                            }
                        )
                    }
                    composable("chat/{userId}") { backStackEntry ->
                        val userId = backStackEntry.arguments?.getString("userId") ?: ""
                        
                        // Set current chat user to suppress notifications
                        LaunchedEffect(userId) {
                            currentChatUserId = userId
                            socketIOService.setCurrentChatUser(userId)
                            android.util.Log.d("MainActivity", "📱 Entered chat with user: $userId")
                        }
                        
                        // Clear current chat user when leaving
                        DisposableEffect(userId) {
                            onDispose {
                                currentChatUserId = null
                                socketIOService.setCurrentChatUser(null)
                                android.util.Log.d("MainActivity", "📱 Left chat")
                            }
                        }
                        
                        // Load messages from backend when entering chat
                        LaunchedEffect(userId) {
                            scope.launch {
                                android.util.Log.d("MainActivity", "📥 Loading messages for user: $userId")
                                
                                // Keep any existing messages (do NOT clear). Clearing here removes
                                // socket-delivered status (readAt/deliveredAt) and causes two-ticks
                                // to disappear when re-opening a conversation.
                                
                                // Fetch user profile first for name display
                                if (!userProfiles.containsKey(userId)) {
                                    val profileResult = apiClient.getUserProfile(userId)
                                    if (profileResult.isSuccess) {
                                        profileResult.getOrNull()?.let { profile ->
                                            userProfiles = userProfiles + (userId to profile)
                                            android.util.Log.d("MainActivity", "✅ Loaded profile for: ${profile.name}")
                                        }
                                    }
                                }
                                
                                // Then load messages
                                val result = apiClient.getThreadMessages(userId)
                                if (result.isSuccess) {
                                    val apiMessages = result.getOrNull().orEmpty()
                                    val localMessages = apiMessages.map { apiMessage ->
                                        com.ims.android.data.model.Message(
                                            id = apiMessage.id,
                                            senderId = apiMessage.senderId,
                                            receiverId = apiMessage.receiverId,
                                            content = apiMessage.content,
                                            sentAt = java.time.Instant.parse(apiMessage.sentAt),
                                            deliveredAt = apiMessage.deliveredAt?.let { java.time.Instant.parse(it) },
                                            readAt = apiMessage.readAt?.let { java.time.Instant.parse(it) },
                                            fcmMessageId = apiMessage.fcmMessageId
                                        )
                                    }.sortedBy { it.sentAt }
                                    // Merge API results with current messages (prefer socket-updated values)
                                    val currentMessages = messages[userId].orEmpty()
                                    val mergedInitial = localMessages.map { apiMsg ->
                                        val existing = currentMessages.find { it.id == apiMsg.id }
                                            if (existing != null) {
                                            apiMsg.copy(
                                                readAt = apiMsg.readAt ?: messageReadCache[apiMsg.id] ?: existing.readAt,
                                                deliveredAt = apiMsg.deliveredAt ?: messageDeliveredCache[apiMsg.id] ?: existing.deliveredAt
                                            )
                                        } else {
                                            apiMsg
                                        }
                                    }.sortedBy { it.sentAt }

                                    messages = messages + (userId to mergedInitial)
                                    android.util.Log.d("MainActivity", "✅ Loaded ${mergedInitial.size} messages from backend (merged)")
                                } else {
                                    android.util.Log.e("MainActivity", "❌ Failed to load messages: ${result.exceptionOrNull()?.message}")
                                }
                            }
                        }
                        
                        // Poll for new messages every 1 second while in chat
                        LaunchedEffect(userId) {
                            while (true) {
                                // Poll every 3s instead of 1s - reduces UI churn and server load
                                kotlinx.coroutines.delay(3000)
                                scope.launch {
                                    val result = apiClient.getThreadMessages(userId)
                                    if (result.isSuccess) {
                                        val apiMessages = result.getOrNull().orEmpty()
                                        val localMessages = apiMessages.map { apiMessage ->
                                            com.ims.android.data.model.Message(
                                                id = apiMessage.id,
                                                senderId = apiMessage.senderId,
                                                receiverId = apiMessage.receiverId,
                                                content = apiMessage.content,
                                                sentAt = java.time.Instant.parse(apiMessage.sentAt),
                                                deliveredAt = apiMessage.deliveredAt?.let { java.time.Instant.parse(it) },
                                                readAt = apiMessage.readAt?.let { java.time.Instant.parse(it) },
                                                fcmMessageId = apiMessage.fcmMessageId
                                            )
                                        }.sortedBy { it.sentAt }
                                        
                                        // Merge API data with current socket data (prefer socket data for same messages)
                                        val currentMessages = messages[userId].orEmpty()
                                        
                                        val mergedMessages = localMessages.map { apiMsg ->
                                            val localMsg = currentMessages.find { it.id == apiMsg.id }
                                            if (localMsg != null) {
                                                // Keep the newest data (prefer non-null values)
                                                apiMsg.copy(
                                                    readAt = apiMsg.readAt ?: messageReadCache[apiMsg.id] ?: localMsg.readAt,
                                                    deliveredAt = apiMsg.deliveredAt ?: messageDeliveredCache[apiMsg.id] ?: localMsg.deliveredAt
                                                )
                                            } else {
                                                apiMsg
                                            }
                                        }.sortedBy { it.sentAt }
                                        
                                        // Check if merged data is different
                                        val hasChanges = mergedMessages.size != currentMessages.size || 
                                            mergedMessages.lastOrNull()?.id != currentMessages.lastOrNull()?.id ||
                                            mergedMessages.zip(currentMessages).any { (new, old) -> 
                                                new.readAt != old.readAt || new.deliveredAt != old.deliveredAt 
                                            }
                                        
                                        if (hasChanges) {
                                            messages = messages + (userId to mergedMessages)
                                            android.util.Log.d("MainActivity", "🔄 Refreshed messages: ${mergedMessages.size} total (merged)")
                                        }
                                    }
                                }
                            }
                        }
                        
                        // Check if this is a self-conversation (messaging yourself)
                        val isSelfConversation = userId == currentUserState.id
                        val otherUserName = if (isSelfConversation) {
                            "${currentUserState.name} (Me)"
                        } else {
                            userProfiles[userId]?.name ?: "User"
                        }
                        val otherUserPhoto = if (isSelfConversation) {
                            currentUserState.photoUrl
                        } else {
                            userProfiles[userId]?.photoUrl
                        }
                        val isOtherUserTyping = typingUsers[userId] ?: false
                        val isOtherUserOnline = onlineMembers.any { it.id == userId }
                        
                        // Mark messages as read and notify backend that we're viewing this conversation
                        LaunchedEffect(userId) {
                            // Tell backend we opened this conversation
                            socketIOService.emitOpenConversation(userId)
                            android.util.Log.d("MainActivity", "👁️ Opened conversation with: $userId")
                            
                            // Check if there are unread messages from the other person
                            val hasUnreadMessages = messages[userId]?.any { 
                                it.senderId == userId && it.readAt == null 
                            } ?: false
                            
                            android.util.Log.d("MainActivity", "👁️ Marking messages as read for conversation with: $userId (hasUnread=$hasUnreadMessages)")
                            // Always call markMessagesAsRead to ensure server updates read receipts
                            socketIOService.markMessagesAsRead(userId)
                        }
                        
                        // Tell backend when we close conversation
                        DisposableEffect(userId) {
                            onDispose {
                                socketIOService.emitCloseConversation()
                                android.util.Log.d("MainActivity", "🚪 Closed conversation with: $userId")
                            }
                        }
                        
                        android.util.Log.d("MainActivity", "💬 Rendering chat with $otherUserName, isTyping=$isOtherUserTyping, isOnline=$isOtherUserOnline, typingUsers=$typingUsers")
                        
                        // Get current conversation messages
                        val conversationMessages = messages[userId].orEmpty()
                        
                        // Render chat screen (do not remount the entire composable when only
                        // read/delivered timestamps change - remounting resets internal input
                        // state and causes the UI to jump / lose typed text).
                        MessengerChatScreen(
                                messages = conversationMessages,
                                otherUserName = otherUserName,
                                otherUserPhoto = otherUserPhoto,
                                currentUserId = currentUserState.id,
                                isTyping = isOtherUserTyping,
                                isOnline = isOtherUserOnline,
                                onBack = { navController.popBackStack() },
                            onTyping = { isTyping ->
                                android.util.Log.d("MainActivity", "⌨️ onTyping callback: isTyping=$isTyping for user=$userId")
                                if (isTyping) {
                                    socketIOService.emitTyping(userId)
                                } else {
                                    socketIOService.emitStopTyping(userId)
                                }
                            },
                            onSend = { content ->
                                scope.launch {
                                    try {
                                        android.util.Log.d("MainActivity", "📤 Sending message to user: $userId")
                                        val result = apiClient.sendMessage(userId, content)
                                        if (result.isSuccess) {
                                            val sentMessage = result.getOrNull()
                                            if (sentMessage != null) {
                                                android.util.Log.d("MainActivity", "✅ Message sent successfully: ${sentMessage.id}")
                                                // Convert API message to local message model
                                                val localMessage = com.ims.android.data.model.Message(
                                                    id = sentMessage.id,
                                                    senderId = sentMessage.senderId,
                                                    receiverId = sentMessage.receiverId,
                                                    content = sentMessage.content,
                                                    sentAt = java.time.Instant.parse(sentMessage.sentAt),
                                                    deliveredAt = sentMessage.deliveredAt?.let { java.time.Instant.parse(it) },
                                                    readAt = sentMessage.readAt?.let { java.time.Instant.parse(it) },
                                                    fcmMessageId = sentMessage.fcmMessageId
                                                )
                                                messages = messages + (userId to (messages[userId].orEmpty() + localMessage))
                                            }
                                        } else {
                                            android.util.Log.e("MainActivity", "❌ Failed to send message: ${result.exceptionOrNull()?.message}")
                                            // Still add to local state for now, but mark as failed
                                            val failedMessage = com.ims.android.data.model.Message(
                                                id = java.util.UUID.randomUUID().toString(),
                                                senderId = currentUserState.id,
                                                receiverId = userId,
                                                content = content,
                                                sentAt = java.time.Instant.now(),
                                                readAt = null,
                                                fcmMessageId = null
                                            )
                                            messages = messages + (userId to (messages[userId].orEmpty() + failedMessage))
                                        }
                                    } catch (e: Exception) {
                                        android.util.Log.e("MainActivity", "❌ Error sending message: ${e.message}", e)
                                        // Add to local state anyway
                                        val errorMessage = com.ims.android.data.model.Message(
                                            id = java.util.UUID.randomUUID().toString(),
                                            senderId = currentUserState.id,
                                            receiverId = userId,
                                            content = content,
                                            sentAt = java.time.Instant.now(),
                                            readAt = null,
                                            fcmMessageId = null
                                        )
                                        messages = messages + (userId to (messages[userId].orEmpty() + errorMessage))
                                    }
                                }
                            }
                        )
                    }
                    
                    composable(AppDestinations.Items) {
                        ItemsScreen(
                            inventoryRepository = inventoryRepository,
                            isDarkTheme = isDarkTheme
                        )
                    }
                    
                    composable(AppDestinations.Stock) {
                        StockScreen(
                            inventoryRepository = inventoryRepository
                        )
                    }
                    
                    composable(AppDestinations.StockIn) {
                        StockInScreen(inventoryRepository = inventoryRepository)
                    }
                    
                    composable(AppDestinations.RecordStockIn) {
                        com.ims.android.ui.screens.RecordStockInScreen(
                            apiClient = apiClient
                        )
                    }
                    
                    composable(AppDestinations.Staff) {
                        StaffScreen()
                    }
                    
                    composable(AppDestinations.Reports) {
                        com.ims.android.ui.screens.ReportsScreen(
                            inventoryRepository = inventoryRepository
                        )
                    }
                    
                    composable(AppDestinations.Analytics) {
                        com.ims.android.ui.screens.AnalyticsScreen(
                            inventoryRepository = inventoryRepository
                        )
                    }
                    
                    composable(AppDestinations.BranchManagement) {
                        Card(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Branch Management Screen - To be implemented",
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                    
                    composable(AppDestinations.RegionManagement) {
                        Card(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Region Management Screen - To be implemented",
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                    
                    composable(AppDestinations.DistrictManagement) {
                        Card(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "District Management Screen - To be implemented",
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                    
                    composable(AppDestinations.MoveoutList) {
                        if (currentProfile.userRole == UserRole.STAFF) {
                            LaunchedEffect(Unit) {
                                navController.navigate(AppDestinations.Dashboard) {
                                    popUpTo(AppDestinations.Dashboard) { inclusive = false }
                                    launchSingleTop = true
                                }
                            }
                        } else {
                            com.ims.android.ui.screens.MoveoutListScreen(
                                apiClient = apiClient
                            )
                        }
                    }
                    
                    composable(AppDestinations.ActivityLogs) {
                        ActivityLogsScreen(
                            inventoryRepository = inventoryRepository
                        )
                    }
                    
                composable(AppDestinations.Notifications) {
                    NotificationsScreen(
                        user = currentUserState,
                        apiClient = apiClient,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(AppDestinations.Settings) {
                    SettingsScreen(
                        authRepository = enhancedAuthRepository,
                        onProfileUpdated = refreshProfile
                    )
                }
                    
                    composable(AppDestinations.ICADelivery) {
                        ICADeliveryListScreen()
                    }
                }
            }
            
            // Show search dialog
            if (showSearchDialog) {
                currentUserState.branchId?.let { branchId ->
                    SearchDialog(
                        onDismiss = { showSearchDialog = false },
                        inventoryRepository = inventoryRepository,
                        branchId = branchId
                    )
                }
            }
            
            // Compose message dialog
            if (composeDialogState) {
                com.ims.android.ui.screens.ComposeMessageDialog(
                    onDismiss = { composeDialogState = false },
                    onUserSelected = { userId, message ->
                        composeDialogState = false
                        pendingComposeResult = Pair(userId, message)
                    },
                    users = usersForCompose
                )
            }
            
            // Handle dialog result and navigation
            LaunchedEffect(pendingComposeResult) {
                pendingComposeResult?.let { (userId, message) ->
                    scope.launch {
                        try {
                            android.util.Log.d("MainActivity", "📤 Sending first message to user: $userId")
                            
                            // Check if thread already exists for this user
                            val existingThread = threads.find { 
                                (it.user1Id == currentUserState.id && it.user2Id == userId) ||
                                (it.user1Id == userId && it.user2Id == currentUserState.id)
                            }
                            
                            val result = apiClient.sendMessage(userId, message)
                            if (result.isSuccess) {
                                val sentMessage = result.getOrNull()
                                if (sentMessage != null) {
                                    android.util.Log.d("MainActivity", "✅ First message sent successfully: ${sentMessage.id}")
                                    
                                    val localMessage = com.ims.android.data.model.Message(
                                        id = sentMessage.id,
                                        senderId = sentMessage.senderId,
                                        receiverId = sentMessage.receiverId,
                                        content = sentMessage.content,
                                        sentAt = java.time.Instant.parse(sentMessage.sentAt),
                                        readAt = sentMessage.readAt?.let { java.time.Instant.parse(it) },
                                        fcmMessageId = sentMessage.fcmMessageId
                                    )
                                    
                                    if (existingThread != null) {
                                        // Update existing thread
                                        threads = threads.map { thread ->
                                            if (thread.id == existingThread.id) {
                                                thread.copy(
                                                    lastMessageId = sentMessage.id,
                                                    updatedAt = java.time.Instant.now()
                                                )
                                            } else thread
                                        }
                                        // Add message to existing conversation
                                        val existingMessages = messages[userId] ?: emptyList()
                                        messages = messages + (userId to (existingMessages + localMessage))
                                    } else {
                                        // Create new thread
                                        val isSelfConversation = userId == currentUserState.id
                                        val threadDisplayName = if (isSelfConversation) {
                                            "${currentUserState.name} (Me)"
                                        } else {
                                            userProfiles[userId]?.name
                                        }
                                        val threadDisplayPhoto = if (isSelfConversation) {
                                            currentUserState.photoUrl
                                        } else {
                                            userProfiles[userId]?.photoUrl
                                        }
                                        val newThread = com.ims.android.data.model.Thread(
                                            id = java.util.UUID.randomUUID().toString(),
                                            user1Id = currentUserState.id,
                                            user2Id = userId,
                                            lastMessageId = sentMessage.id,
                                            updatedAt = java.time.Instant.now(),
                                            displayName = threadDisplayName,
                                            displayPhoto = threadDisplayPhoto
                                        )
                                        threads = threads + newThread
                                        messages = messages + (userId to listOf(localMessage))
                                        
                                        // Store thread locally
                                        scope.launch {
                                            val apiThread = com.ims.android.data.api.ApiClient.Thread(
                                                id = newThread.id,
                                                user1Id = newThread.user1Id,
                                                user2Id = newThread.user2Id,
                                                lastMessageId = newThread.lastMessageId,
                                                updatedAt = newThread.updatedAt.toString()
                                            )
                                            apiClient.storeLocalThread(apiThread)
                                        }
                                    }
                                    
                                    navController.navigate("chat/$userId")
                                }
                            } else {
                                android.util.Log.e("MainActivity", "❌ Failed to send first message: ${result.exceptionOrNull()?.message}")
                                // Just navigate to existing chat if thread exists
                                navController.navigate("chat/$userId")
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("MainActivity", "❌ Error sending first message: ${e.message}", e)
                            // Just navigate to existing chat
                            navController.navigate("chat/$userId")
                        } finally {
                            pendingComposeResult = null
                        }
                    }
                }
            }
            
            // Show notifications dropdown
            if (showNotificationsDropdown) {
                NotificationsDropdown(
                    user = currentUserState,
                    apiClient = apiClient,
                    inventoryRepository = inventoryRepository,
                    notificationRepository = NotificationRepository(apiClient),
                    onDismiss = { 
                        showNotificationsDropdown = false
                        // Refresh notification count (only unread notifications)
                        scope.launch {
                            val notifResult = NotificationRepository(apiClient).getNotifications()
                            notifResult.onSuccess { notifications ->
                                notificationCount = notifications.count { !it.isRead }
                            }
                        }
                    },
                    onNavigateToNotifications = {
                        navController.navigate(AppDestinations.Notifications)
                    },
                    onNotificationMarkedRead = {
                        // Update badge immediately when notification is marked as read
                        scope.launch {
                            val notifResult = NotificationRepository(apiClient).getNotifications()
                            notifResult.onSuccess { notifications ->
                                notificationCount = notifications.count { !it.isRead }
                            }
                        }
                    }
                )
            }
        }
    }
}

fun getScreenTitle(route: String?): String {
    return when (route) {
        AppDestinations.Dashboard -> "Dashboard"
        AppDestinations.Items -> "Manage Items"
        AppDestinations.Stock -> "Stock Out"
        AppDestinations.StockIn -> "Stock In"
        AppDestinations.RecordStockIn -> "Record Stock In"
        AppDestinations.Staff -> "Manage Staff"
        AppDestinations.Reports -> "Reports"
        AppDestinations.Analytics -> "Analytics"
        AppDestinations.BranchManagement -> "Branch Management"
        AppDestinations.RegionManagement -> "Region Management"
        AppDestinations.DistrictManagement -> "District Management"
        AppDestinations.MoveoutList -> "Moveout List"
        AppDestinations.ActivityLogs -> "Activity Logs"
        AppDestinations.Notifications -> "Notifications"
        AppDestinations.Settings -> "Settings"
        AppDestinations.ICADelivery -> "ICA Delivery"
        else -> "Stock Nexus"
    }
}
