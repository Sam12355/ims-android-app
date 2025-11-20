package com.stocknexus.android.presentation

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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.stocknexus.android.presentation.theme.StockNexusTheme
import com.stocknexus.data.api.ApiClient
import com.stocknexus.data.repository.*
import com.stocknexus.data.repository.ICADeliveryRepository
import com.stocknexus.data.model.*
import com.stocknexus.navigation.AppDestinations
import com.stocknexus.ui.components.AppDrawer
import com.stocknexus.ui.components.TopAppBar
import com.stocknexus.ui.components.SearchDialog
import com.stocknexus.ui.components.NotificationsDropdown
import com.stocknexus.ui.screens.DashboardScreen
import com.stocknexus.ui.screens.StockScreen
import com.stocknexus.ui.screens.StockInScreen
import com.stocknexus.ui.screens.ICADeliveryListScreen
import com.stocknexus.ui.screens.StaffScreen
import com.stocknexus.ui.screens.ActivityLogsScreen
import com.stocknexus.ui.screens.SettingsScreen
import com.stocknexus.ui.screens.ItemsScreen
import com.stocknexus.ui.screens.auth.EnhancedAuthScreen
import com.stocknexus.ui.screens.auth.SplashScreen
import com.stocknexus.ui.viewmodel.AuthViewModel
import com.stocknexus.ui.viewmodel.AuthViewModelFactory
import com.stocknexus.service.SocketIOService
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            android.util.Log.d("MainActivity", "Notification permission granted")
        } else {
            android.util.Log.d("MainActivity", "Notification permission denied")
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Request notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        
        setContent {
            // State for theme toggle
            var isDarkTheme by remember { mutableStateOf(true) }
            
            StockNexusTheme(darkTheme = isDarkTheme) {
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
    onLogout: () -> Unit
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
                    Card(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Record Stock In Screen - To be implemented",
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
                
                composable(AppDestinations.Staff) {
                    StaffScreen()
                }
                
                composable(AppDestinations.Reports) {
                    com.stocknexus.ui.screens.ReportsScreen(
                        inventoryRepository = inventoryRepository
                    )
                }
                
                composable(AppDestinations.Analytics) {
                    com.stocknexus.ui.screens.AnalyticsScreen(
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
                    Card(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Moveout List Screen - To be implemented",
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
                
                composable(AppDestinations.ActivityLogs) {
                    ActivityLogsScreen(
                        inventoryRepository = inventoryRepository
                    )
                }
                
                composable(AppDestinations.Notifications) {
                    Card(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Notifications Screen - To be implemented",
                            modifier = Modifier.padding(16.dp)
                        )
                    }
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