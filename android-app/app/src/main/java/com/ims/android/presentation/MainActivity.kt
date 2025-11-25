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
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Don't request notification permission automatically
        // It will be requested only when user enables notifications in settings
        
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
                            onThemeToggle = { isDarkTheme = !isDarkTheme }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockNexusApp(
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit
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
                        authViewModel.checkAuthState()
                    }
                )
            }
        }
        
        is AuthState.Unauthenticated -> {
            EnhancedAuthScreen(
                authRepository = enhancedAuthRepository,
                onLoginSuccess = { user ->
                    authViewModel.checkAuthState()
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
                isDarkTheme = isDarkTheme,
                onThemeToggle = onThemeToggle,
                onLogout = {
                    authViewModel.signOut()
                    // ViewModel handles state update
                }
                // When app resumes we should refresh the profile to pick up any server-side
                // changes (e.g. toggles enabled on the web). Pass a callback that triggers
                // the ViewModel to refresh the profile.
                , onResumeRefresh = { authViewModel.refreshProfile() }
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
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit,
    onLogout: () -> Unit,
    onResumeRefresh: () -> Unit = {}
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    // Initialize Socket.IO service
    val socketIOService = remember { SocketIOService(context) }
    
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
            if (token != null && branchId.isNotEmpty()) {
                android.util.Log.d("MainActivity", "🔌 Connecting to Socket.IO with branch: $branchId")
                socketIOService.connect(token, branchId)
            }
        }
    }

    // When the app resumes (foreground), refresh the profile to pick up remote changes.
    val lifecycle = (context as? ComponentActivity)?.lifecycle
    DisposableEffect(lifecycle) {
        if (lifecycle == null) {
            onDispose { }
        } else {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    android.util.Log.d("AuthenticatedApp", "Lifecycle resumed — refreshing profile")
                    onResumeRefresh()
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
        Scaffold(
            topBar = {
                val currentBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = currentBackStackEntry?.destination?.route
                
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
                        calendarRepository = calendarRepository
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
