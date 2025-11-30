package com.ims.android.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.ims.android.data.model.*
import com.ims.android.data.repository.DashboardRepository
import com.ims.android.data.repository.InventoryRepository
import com.ims.android.data.repository.MoveoutRepository
import com.ims.android.data.repository.ICADeliveryRepository
import com.ims.android.data.repository.CalendarRepository
import com.ims.android.service.SocketIOService
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import com.ims.android.ui.toast.UserOnlineToastHost
import com.ims.android.ui.screens.auth.PendingAccessScreen
import com.ims.android.data.repository.EnhancedAuthRepository
import com.ims.android.data.api.ApiClient

@Composable
fun DashboardScreen(
    dashboardRepository: DashboardRepository,
    moveoutRepository: MoveoutRepository,
    inventoryRepository: InventoryRepository,
    icaDeliveryRepository: ICADeliveryRepository,
    calendarRepository: CalendarRepository,
    socketIOService: SocketIOService,
    authRepository: EnhancedAuthRepository,
    apiClient: ApiClient,
    onSignOut: () -> Unit,
    openMoveoutFromNotification: Boolean = false,
    onMoveoutNotificationHandled: () -> Unit = {}
) {
    // State management - using remember for complex objects
    var dashboardStats by remember { mutableStateOf<DashboardStats?>(null) }
    var weatherData by remember { mutableStateOf<WeatherData?>(null) }
    var userProfile by remember { mutableStateOf<Profile?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var lastLoadTime by remember { mutableStateOf(0L) }
    var isRefreshing by remember { mutableStateOf(false) }
    
    // Moveout Lists state
    var moveoutLists by remember { mutableStateOf<List<MoveoutList>>(emptyList()) }
    var completedLists by remember { mutableStateOf<List<MoveoutList>>(emptyList()) }
    var showHistory by remember { mutableStateOf(false) }
    var moveoutListsLoading by remember { mutableStateOf(false) }
    
    // Moveout items dialog state
    var selectedMoveoutList by remember { mutableStateOf<MoveoutList?>(null) }
    var selectedCompletedList by remember { mutableStateOf<MoveoutList?>(null) }
    var processingItemId by remember { mutableStateOf<String?>(null) }
    
    // Calendar Events state
    var calendarEvents by remember { mutableStateOf<List<CalendarEvent>>(emptyList()) }
    var selectedCalendarDate by remember { mutableStateOf(Date()) }
    var showAddEventDialog by remember { mutableStateOf(false) }
    var isAddingEvent by remember { mutableStateOf(false) }
    var branches by remember { mutableStateOf<List<Branch>>(emptyList()) }
    
    // Modal states
    var showStockModal by remember { mutableStateOf(false) }
    var modalStockType by remember { mutableStateOf<String?>(null) }
    var showMoveoutModal by remember { mutableStateOf(false) }
    var showICADeliveryModal by remember { mutableStateOf(false) }
    
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    // Function to load calendar events
    suspend fun loadCalendarEvents() {
        try {
            val events = calendarRepository.getCalendarEvents()
            // Filter for upcoming events and sort by date
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            calendarEvents = events
                .filter { it.eventDate >= today }
                .sortedBy { it.eventDate }
                .take(5)
            android.util.Log.d("DashboardScreen", "✅ Loaded ${calendarEvents.size} calendar events")
        } catch (e: Exception) {
            android.util.Log.e("DashboardScreen", "❌ Error loading calendar events: ${e.message}", e)
        }
    }
    
    // Function to load moveout lists
    suspend fun loadMoveoutLists(silent: Boolean = false) {
        try {
            if (!silent) {
                moveoutListsLoading = true
            }
            val result = moveoutRepository.getMoveoutLists()
            if (result.isSuccess) {
                val allLists = result.getOrNull() ?: emptyList()
                android.util.Log.d("DashboardScreen", "📋 Total lists received: ${allLists.size}")
                allLists.forEach { list ->
                    android.util.Log.d("DashboardScreen", "  - List: id=${list.id}, title=${list.title}, status=${list.status}")
                }
                // Filter for both "draft" and "active" status (non-completed lists)
                val pendingLists = allLists.filter { it.status == "draft" || it.status == "active" }
                moveoutLists = pendingLists
                android.util.Log.d("DashboardScreen", "✅ Loaded ${moveoutLists.size} active moveout lists (status=draft or active)")
            }
        } catch (e: Exception) {
            android.util.Log.e("DashboardScreen", "❌ Error loading moveout lists: ${e.message}", e)
        } finally {
            if (!silent) {
                moveoutListsLoading = false
            }
        }
    }
    
    // Function to toggle history
    fun handleToggleHistory() {
        scope.launch {
            if (!showHistory) {
                // Load completed lists
                try {
                    moveoutListsLoading = true
                    val result = moveoutRepository.getMoveoutLists()
                    if (result.isSuccess) {
                        val allLists = result.getOrNull() ?: emptyList()
                        
                        // Update both pending and completed lists with fresh data
                        moveoutLists = allLists.filter { it.status == "draft" || it.status == "active" }
                        completedLists = allLists.filter { it.status == "completed" }
                        
                        showHistory = true
                        android.util.Log.d("DashboardScreen", "✅ Refreshed: ${moveoutLists.size} pending, ${completedLists.size} completed lists")
                        
                        // Show success toast
                        val message = if (completedLists.isEmpty()) {
                            "No completed lists found"
                        } else {
                            "Loaded ${completedLists.size} completed lists"
                        }
                        snackbarHostState.showSnackbar(
                            message = message,
                            duration = SnackbarDuration.Short
                        )
                    }
                } catch (e: Exception) {
                    android.util.Log.e("DashboardScreen", "❌ Error loading history: ${e.message}", e)
                    snackbarHostState.showSnackbar(
                        message = "Failed to load history",
                        duration = SnackbarDuration.Short
                    )
                } finally {
                    moveoutListsLoading = false
                }
            } else {
                // Refresh data even when hiding history
                try {
                    moveoutListsLoading = true
                    val result = moveoutRepository.getMoveoutLists()
                    if (result.isSuccess) {
                        val allLists = result.getOrNull() ?: emptyList()
                        
                        // Update pending lists with fresh data
                        moveoutLists = allLists.filter { it.status == "draft" || it.status == "active" }
                        
                        showHistory = false
                        completedLists = emptyList()
                        android.util.Log.d("DashboardScreen", "✅ Refreshed: ${moveoutLists.size} pending lists")
                        
                        snackbarHostState.showSnackbar(
                            message = "Refreshed pending lists",
                            duration = SnackbarDuration.Short
                        )
                    }
                } catch (e: Exception) {
                    android.util.Log.e("DashboardScreen", "❌ Error refreshing: ${e.message}", e)
                } finally {
                    moveoutListsLoading = false
                }
            }
        }
    }
    
    // Function to process moveout item
    fun handleProcessMoveoutItem(listId: String, itemId: String, quantity: Int) {
        scope.launch {
            try {
                processingItemId = itemId
                val userName = userProfile?.name ?: "Unknown"
                
                android.util.Log.d("DashboardScreen", "Processing moveout item: listId=$listId, itemId=$itemId, quantity=$quantity, userName=$userName")
                
                val result = moveoutRepository.processMoveoutItem(listId, itemId, quantity, userName)
                
                if (result.isSuccess) {
                    android.util.Log.d("DashboardScreen", "✅ Item processed successfully")
                    
                    // Silently fetch the updated list to refresh the dialog (no loading indicator)
                    val allListsResult = moveoutRepository.getMoveoutLists()
                    if (allListsResult.isSuccess) {
                        val allLists = allListsResult.getOrNull() ?: emptyList()
                        
                        // Update moveoutLists with only pending/active lists (silent update)
                        val pendingLists = allLists.filter { it.status == "draft" || it.status == "active" }
                        moveoutLists = pendingLists
                        android.util.Log.d("DashboardScreen", "✅ Silent refresh: ${moveoutLists.size} pending lists")
                        
                        // Find the current list
                        val updatedList = allLists.find { it.id == listId }
                        
                        if (updatedList != null && (updatedList.status == "draft" || updatedList.status == "active")) {
                            // Update the dialog with fresh data
                            selectedMoveoutList = updatedList
                        } else {
                            // List is completed or not found, close the dialog
                            selectedMoveoutList = null
                            android.util.Log.d("DashboardScreen", "List completed, closing dialog")
                        }
                    }
                    
                    snackbarHostState.showSnackbar(
                        message = "Item processed successfully",
                        duration = SnackbarDuration.Short
                    )
                } else {
                    android.util.Log.e("DashboardScreen", "❌ Failed to process item: ${result.exceptionOrNull()?.message}")
                    snackbarHostState.showSnackbar(
                        message = "Failed to process item",
                        duration = SnackbarDuration.Short
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("DashboardScreen", "❌ Error processing item: ${e.message}", e)
                snackbarHostState.showSnackbar(
                    message = "Error processing item",
                    duration = SnackbarDuration.Short
                )
            } finally {
                processingItemId = null
            }
        }
    }
    
    // Function to load data
    suspend fun loadDashboardData(forceRefresh: Boolean = false) {
        val currentTime = System.currentTimeMillis()
        val cacheValidDuration = 5 * 60 * 1000 // 5 minutes cache
        
        // Check if we have valid cached data
        val hasValidCache = userProfile != null && 
                          dashboardStats != null && 
                          (currentTime - lastLoadTime) <= cacheValidDuration
        
        // Only reload if cache is expired, no data, or force refresh
        if (forceRefresh || !hasValidCache) {
            if (forceRefresh) {
                isRefreshing = true
            } else {
                isLoading = true
            }
            
            try {
                // Load all data in parallel for better performance
                coroutineScope {
                    val profileDeferred = async { dashboardRepository.getUserProfile() }
                    val statsDeferred = async { dashboardRepository.getDashboardStats() }
                    val weatherDeferred = async { dashboardRepository.getWeatherData() }
                    
                    // Get user profile to determine role
                    android.util.Log.d("DashboardScreen", "🔍 Fetching user profile...")
                    userProfile = profileDeferred.await()
                    android.util.Log.d("DashboardScreen", "✅ User profile loaded: ${userProfile?.name}, role: ${userProfile?.role}")
                    
                    val statsResult = statsDeferred.await()
                    if (statsResult.isSuccess) {
                        dashboardStats = statsResult.getOrNull()
                        android.util.Log.d("DashboardScreen", "✅ Dashboard stats loaded")
                    } else {
                        errorMessage = statsResult.exceptionOrNull()?.message
                        android.util.Log.e("DashboardScreen", "❌ Failed to load stats: $errorMessage")
                    }
                    
                    val weatherResult = weatherDeferred.await()
                    if (weatherResult.isSuccess) {
                        weatherData = weatherResult.getOrNull()
                        android.util.Log.d("DashboardScreen", "✅ Weather data loaded")
                    }
                }
                
                // Load moveout lists and calendar events in parallel
                val isStaff = userProfile?.role == "staff"
                if (!isStaff) {
                    // Load calendar events and branches in parallel for non-staff
                    coroutineScope {
                        launch { loadCalendarEvents() }
                        
                        if (userProfile?.role == "admin") {
                            launch {
                                try {
                                    val branchesResult = dashboardRepository.getBranches()
                                    if (branchesResult.isSuccess) {
                                        branches = branchesResult.getOrNull() ?: emptyList()
                                        android.util.Log.d("DashboardScreen", "✅ Loaded ${branches.size} branches")
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("DashboardScreen", "❌ Error loading branches: ${e.message}", e)
                                }
                            }
                        }
                    }
                }
                
                // Load moveout lists
                loadMoveoutLists()
                
                lastLoadTime = currentTime
            } catch (e: Exception) {
                errorMessage = e.message
                android.util.Log.e("DashboardScreen", "❌ Error loading dashboard: ${e.message}", e)
            } finally {
                isLoading = false
                isRefreshing = false
            }
        } else {
            android.util.Log.d("DashboardScreen", "📦 Using cached data (${(currentTime - lastLoadTime) / 1000}s old)")
            isLoading = false
        }
    }
    
    // Load dashboard data on first composition
    LaunchedEffect(Unit) {
        scope.launch {
            loadDashboardData()
        }
    }
    
    // Handle opening moveout list from notification tap
    // Watch both the flag AND moveoutLists so it triggers when data loads
    LaunchedEffect(openMoveoutFromNotification, moveoutLists) {
        android.util.Log.d("DashboardScreen", "📱 LaunchedEffect: openMoveout=$openMoveoutFromNotification, lists=${moveoutLists.size}")
        if (openMoveoutFromNotification && moveoutLists.isNotEmpty()) {
            android.util.Log.d("DashboardScreen", "📱 Opening moveout list from notification!")
            selectedMoveoutList = moveoutLists.first()
            onMoveoutNotificationHandled()
        }
    }
    
    // Check if user has pending access (staff role with no branch assigned)
    if (userProfile != null && userProfile!!.role == "staff" && userProfile!!.branchId.isNullOrBlank()) {
        PendingAccessScreen(
            onSignOut = {
                scope.launch {
                    authRepository.signOut()
                    onSignOut()
                }
            }
        )
        return
    }
    
    if (errorMessage != null && userProfile == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Error loading dashboard",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = errorMessage!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
        return
    }
    
    // Determine user role
    val isStaff = userProfile?.role?.equals("staff", ignoreCase = true) == true
    val isManager = userProfile?.role?.equals("manager", ignoreCase = true) == true ||
                    userProfile?.role?.equals("assistant_manager", ignoreCase = true) == true
    
    android.util.Log.d("DashboardScreen", "👤 User role: ${userProfile?.role}, isStaff: $isStaff, isManager: $isManager")
    
    Box(modifier = Modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background) // Use theme background color
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome Header with refresh button
        DashboardHeader(
            userName = userProfile?.name ?: "User",
            role = userProfile?.role ?: "",
            isRefreshing = isRefreshing,
            onRefresh = {
                scope.launch {
                    loadDashboardData(forceRefresh = true)
                }
            }
        )
        
        // Show loading indicator only on first load with no data
        if (isLoading && userProfile == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Loading dashboard...")
                }
            }
        } else {
        
        // Action Buttons for Staff Role (show at top for staff)
        if (isStaff) {
            android.util.Log.d("DashboardScreen", "🎯 Rendering ActionButtonsSection for staff")
            ActionButtonsSection(
                onGenerateMoveoutList = { showMoveoutModal = true },
                onICADelivery = { showICADeliveryModal = true }
            )
        }
        
        // Statistics Cards (4 cards in 2x2 grid) - Non-staff only (managers see this)
        if (!isStaff) {
            android.util.Log.d("DashboardScreen", "📊 Rendering StatsGrid for manager")
            dashboardStats?.let { stats ->
                StatsGrid(
                    stats = stats,
                    onStockCardClick = { stockType ->
                        modalStockType = stockType
                        showStockModal = true
                    }
                )
            }
        }
        
        // Weather Widget (Full Width) - Show for all roles
        weatherData?.let { weather ->
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                WeatherCardContent(weatherData = weather)
            }
        }
        
        // Generated Moveout Lists Section (Full Width) - Show for all roles
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Generated Moveout Lists",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (showHistory) "Generated and completed moveout lists" else "Generated moveout lists",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Generate Moveout List button for managers only
                        if (isManager) {
                            Button(
                                onClick = { showMoveoutModal = true },
                                modifier = Modifier.height(36.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFE6002A),
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Generate",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                        
                        // History button for all roles
                        IconButton(
                            onClick = { handleToggleHistory() },
                            enabled = !moveoutListsLoading
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = if (showHistory) "Hide History" else "View History",
                                tint = Color.White
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                
                // Content
                if (moveoutListsLoading) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Loading moveout lists...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    val displayLists = if (showHistory) moveoutLists + completedLists else moveoutLists
                    
                    // Debug logging
                    android.util.Log.d("DashboardScreen", "📊 Displaying lists: showHistory=$showHistory, moveoutLists=${moveoutLists.size}, completedLists=${completedLists.size}, displayLists=${displayLists.size}")
                    displayLists.forEach { list ->
                        android.util.Log.d("DashboardScreen", "  📋 Showing: ${list.title} - status=${list.status}")
                    }
                    
                    if (displayLists.isEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Assignment,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = when {
                                    showHistory && moveoutLists.isEmpty() -> "No moveout lists found"
                                    showHistory && completedLists.isEmpty() -> "No completed moveout lists yet"
                                    else -> "No active moveout lists generated yet"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (!showHistory) {
                                Text(
                                    text = "Click \"Generate Moveout List\" to create your first list",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else if (moveoutLists.isEmpty()) {
                                Text(
                                    text = "No active or completed moveout lists found",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            displayLists.forEach { list ->
                                MoveoutListItemSimple(
                                    moveoutList = list,
                                    onItemClick = { 
                                        // Open appropriate dialog based on status
                                        if (list.status == "draft" || list.status == "active") {
                                            selectedMoveoutList = list
                                        } else if (list.status == "completed") {
                                            selectedCompletedList = list
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // Calendar & Events Section (Full Width) - Non-staff only
        if (!isStaff) {
            CalendarEventsCard(
                events = calendarEvents,
                selectedDate = selectedCalendarDate,
                onDateSelected = { selectedCalendarDate = it },
                onAddEventClick = { showAddEventDialog = true },
                showAddButton = userProfile?.role == "manager" || userProfile?.role == "assistant_manager"
            )
        }
        }
    }
    
    // Add Event Dialog
    if (showAddEventDialog) {
        AddEventDialog(
            onDismiss = { showAddEventDialog = false },
            onConfirm = { title, description, eventDate, eventType, branchId ->
                scope.launch {
                    isAddingEvent = true
                    val result = calendarRepository.createCalendarEvent(
                        title = title,
                        description = description,
                        eventDate = eventDate,
                        eventType = eventType,
                        branchId = branchId
                    )
                    
                    if (result.isSuccess) {
                        android.util.Log.d("DashboardScreen", "✅ Event created successfully")
                        snackbarHostState.showSnackbar(
                            message = "Event added successfully",
                            duration = SnackbarDuration.Short
                        )
                        showAddEventDialog = false
                        loadCalendarEvents() // Reload events
                    } else {
                        android.util.Log.e("DashboardScreen", "❌ Failed to create event: ${result.exceptionOrNull()?.message}")
                        snackbarHostState.showSnackbar(
                            message = "Failed to add event: ${result.exceptionOrNull()?.message}",
                            duration = SnackbarDuration.Short
                        )
                    }
                    isAddingEvent = false
                }
            },
            branches = branches,
            isAdmin = userProfile?.role == "admin",
            isLoading = isAddingEvent
        )
    }
    
    // Stock Details Modal
    if (showStockModal) {
        StockDetailsModal(
            stockType = modalStockType ?: "",
            stockDetails = when (modalStockType) {
                "threshold" -> dashboardStats?.thresholdStockDetails ?: emptyList()
                "low" -> dashboardStats?.lowStockDetails ?: emptyList()
                "critical" -> dashboardStats?.criticalStockDetails ?: emptyList()
                else -> emptyList()
            },
            onDismiss = { showStockModal = false }
        )
    }
    
    // Moveout Modal
    if (showMoveoutModal) {
        GenerateMoveoutDialog(
            onDismiss = { showMoveoutModal = false },
            inventoryRepository = inventoryRepository,
            moveoutRepository = moveoutRepository,
            userName = userProfile?.name ?: "Unknown",
            apiClient = apiClient,
            onSuccess = { itemCount ->
                // Reload moveout lists after successful generation
                scope.launch {
                    loadMoveoutLists()
                    snackbarHostState.showSnackbar(
                        message = "Successfully generated moveout list with $itemCount items",
                        duration = SnackbarDuration.Short
                    )
                }
            }
        )
    }
    
    // ICA Delivery Dialog
    if (showICADeliveryModal) {
        ICADeliveryDialog(
            onDismiss = { showICADeliveryModal = false },
            icaDeliveryRepository = icaDeliveryRepository,
            userName = userProfile?.name ?: "Unknown"
        )
    }
    
    // Moveout Items Dialog
    selectedMoveoutList?.let { list ->
        MoveoutItemsDialog(
            moveoutList = list,
            onDismiss = { selectedMoveoutList = null },
            onProcessItem = { itemId, quantity ->
                handleProcessMoveoutItem(list.id ?: "", itemId, quantity)
            },
            processingItemId = processingItemId
        )
    }
    
    // Completed Moveout Items Dialog
    selectedCompletedList?.let { list ->
        CompletedMoveoutItemsDialog(
            moveoutList = list,
            onDismiss = { selectedCompletedList = null }
        )
    }
    
    // Snackbar overlay at the bottom
    SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth(),
        snackbar = { data ->
            AnimatedVisibility(
                visible = true,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                Snackbar(
                    snackbarData = data,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(8.dp),
                    containerColor = Color(0xFF00A36C),
                    contentColor = Color.White
                )
            }
        }
    )

    // User online compact toast (bottom overlay)
    UserOnlineToastHost(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 64.dp)
    )
    }
}

@Composable
fun DashboardHeader(
    userName: String = "User",
    role: String = "",
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Dashboard",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Welcome back, $userName! Here's what's happening with your inventory.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Today,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Today: ${SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date())}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // Refresh button
        IconButton(
            onClick = onRefresh,
            enabled = !isRefreshing
        ) {
            if (isRefreshing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
fun WeatherCard(weatherData: WeatherData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        WeatherCardContent(weatherData = weatherData)
    }
}

@Composable
fun WeatherCardContent(weatherData: WeatherData) {
    Column(
        modifier = Modifier.padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Cloud,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Weather in ${weatherData.location}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Temperature and condition
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "${weatherData.temperature.toInt()}°C",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = weatherData.condition,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Humidity and Wind Speed
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Water,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color(0xFF2196F3)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${weatherData.humidity}%",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Air,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${weatherData.windSpeed} km/h",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Good conditions for deliveries",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        
        // TODO: Add weather condition photo
        // Similar to web app's getWeatherPhoto function
    }
}

@Composable
fun ActionButtonsSection(
    onGenerateMoveoutList: () -> Unit,
    onICADelivery: () -> Unit
) {
    android.util.Log.d("ActionButtonsSection", "🔨 Composing full-width stacked buttons")
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Generate Moveout List Button - Primary color (Red with white text)
        Button(
            onClick = onGenerateMoveoutList,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFE6002A), // Primary red color
                contentColor = Color.White
            )
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Generate",
                style = MaterialTheme.typography.bodyLarge
            )
        }
        
        // ICA Delivery Button - Green color (matching web app)
        Button(
            onClick = onICADelivery,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF16a34a), // green-600
                contentColor = Color.White
            ),
            contentPadding = PaddingValues(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.LocalShipping,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "ICA Delivery",
                style = MaterialTheme.typography.bodyLarge
            )
        }
        
        android.util.Log.d("ActionButtonsSection", "✅ Both buttons composed")
    }
}
