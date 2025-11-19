package com.stocknexus.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.stocknexus.data.repository.EnhancedAuthRepository
import com.stocknexus.data.model.UserRole
import com.stocknexus.ui.components.AlertSchedulingDialog
import com.stocknexus.ui.components.AlertSchedule
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    authRepository: EnhancedAuthRepository,
    modifier: Modifier = Modifier,
    onProfileUpdated: (() -> Unit)? = null
) {
    var currentUser by remember { mutableStateOf<com.stocknexus.data.model.User?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var position by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }
    
    // Notification preferences
    var emailNotifications by remember { mutableStateOf(true) }
    var smsNotifications by remember { mutableStateOf(false) }
    var whatsappNotifications by remember { mutableStateOf(false) }
    var stockAlerts by remember { mutableStateOf(true) }
    var eventReminders by remember { mutableStateOf(true) }
    var softdrinkTrends by remember { mutableStateOf(false) }
    
    // Dialog states
    var showStockAlertDialog by remember { mutableStateOf(false) }
    var showEventReminderDialog by remember { mutableStateOf(false) }
    var showSoftdrinkDialog by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                isSaving = true
                try {
                    android.util.Log.d("SettingsScreen", "📸 Starting image upload process...")
                    val imageBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    if (imageBytes != null) {
                        android.util.Log.d("SettingsScreen", "📸 Image bytes read: ${imageBytes.size} bytes")
                        val userId = currentUser?.id ?: return@launch
                        android.util.Log.d("SettingsScreen", "📸 User ID: $userId")
                        
                        val uploadResult = authRepository.uploadProfileImage(userId, imageBytes)
                        android.util.Log.d("SettingsScreen", "📸 Upload result: ${uploadResult.isSuccess}")
                        
                        if (uploadResult.isSuccess) {
                            val photoUrl = uploadResult.getOrNull()
                            android.util.Log.d("SettingsScreen", "📸 Photo URL: $photoUrl")
                            
                            // The upload function now updates the database, so just refresh the user state
                            android.util.Log.d("SettingsScreen", "📸 Reloading current user...")
                            val refreshResult = authRepository.refreshProfile()
                            
                            if (refreshResult.isSuccess) {
                                val refreshedUser = refreshResult.getOrNull()
                                currentUser = refreshedUser
                                android.util.Log.d("SettingsScreen", "📸 ✅ SUCCESS: Profile refreshed with new photo: ${refreshedUser?.photoUrl}")
                                // Notify parent to refresh the drawer/topbar profile
                                onProfileUpdated?.invoke()
                                snackbarHostState.showSnackbar("✅ Profile picture updated successfully!")
                            } else {
                                snackbarHostState.showSnackbar("✅ Upload succeeded! Please refresh to see changes.")
                                android.util.Log.d("SettingsScreen", "📸 ⚠️ Upload succeeded but couldn't reload user")
                            }
                        } else {
                            val errorMsg = uploadResult.exceptionOrNull()?.message ?: "Unknown error"
                            android.util.Log.e("SettingsScreen", "📸 ❌ ERROR: Upload failed: $errorMsg")
                            snackbarHostState.showSnackbar("❌ Upload failed: $errorMsg")
                        }
                    } else {
                        android.util.Log.e("SettingsScreen", "📸 ERROR: Could not read image bytes")
                        snackbarHostState.showSnackbar("❌ Could not read image")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("SettingsScreen", "📸 EXCEPTION: ${e.javaClass.simpleName}: ${e.message}", e)
                    snackbarHostState.showSnackbar("❌ Error: ${e.javaClass.simpleName}: ${e.message}")
                } finally {
                    isSaving = false
                }
            }
        }
    }
    
    // Function to save notification preferences
    fun saveNotificationPreferences() {
        scope.launch {
            try {
                android.util.Log.d("SettingsScreen", "============ SAVE START ============")
                android.util.Log.d("SettingsScreen", "Current toggle states:")
                android.util.Log.d("SettingsScreen", "  email=$emailNotifications")
                android.util.Log.d("SettingsScreen", "  sms=$smsNotifications")
                android.util.Log.d("SettingsScreen", "  whatsapp=$whatsappNotifications")
                android.util.Log.d("SettingsScreen", "  stockAlerts=$stockAlerts")
                android.util.Log.d("SettingsScreen", "  eventReminders=$eventReminders")
                android.util.Log.d("SettingsScreen", "  softdrinkTrends=$softdrinkTrends")
                
                val notificationSettings = mapOf(
                    "email" to emailNotifications,
                    "sms" to smsNotifications,
                    "whatsapp" to whatsappNotifications,
                    "stockLevelAlerts" to stockAlerts,
                    "eventReminders" to eventReminders,
                    "softdrinkTrends" to softdrinkTrends
                )
                android.util.Log.d("SettingsScreen", "Calling updateNotificationSettings with: $notificationSettings")
                
                val result = authRepository.updateNotificationSettings(notificationSettings)
                
                if (result.isSuccess) {
                    val updatedUser = result.getOrNull()
                    android.util.Log.d("SettingsScreen", "✅ Save SUCCESS")
                    android.util.Log.d("SettingsScreen", "Updated user notification_settings: ${updatedUser?.notificationSettings}")
                    currentUser = updatedUser
                    snackbarHostState.showSnackbar("Notification preferences saved")
                } else {
                    android.util.Log.e("SettingsScreen", "❌ Save FAILED: ${result.exceptionOrNull()?.message}")
                    snackbarHostState.showSnackbar("Failed to save: ${result.exceptionOrNull()?.message}")
                }
                android.util.Log.d("SettingsScreen", "============ SAVE END ============")
            } catch (e: Exception) {
                android.util.Log.e("SettingsScreen", "❌ EXCEPTION during save", e)
                snackbarHostState.showSnackbar("Error: ${e.message}")
            }
        }
    }

    fun handleScheduleSave(type: String, schedule: AlertSchedule) {
        // Optimistic Update
        currentUser = currentUser?.let { user ->
            when (type) {
                "stock" -> user.copy(
                    stockAlertFrequencies = schedule.frequencies,
                    dailyScheduleTime = if (schedule.frequencies.contains("daily")) schedule.dailyTime else user.dailyScheduleTime,
                    weeklyScheduleDay = if (schedule.frequencies.contains("weekly")) schedule.weeklyDay else user.weeklyScheduleDay,
                    weeklyScheduleTime = if (schedule.frequencies.contains("weekly")) schedule.weeklyTime else user.weeklyScheduleTime,
                    monthlyScheduleDate = if (schedule.frequencies.contains("monthly")) schedule.monthlyDate else user.monthlyScheduleDate,
                    monthlyScheduleTime = if (schedule.frequencies.contains("monthly")) schedule.monthlyTime else user.monthlyScheduleTime
                )
                "event" -> user.copy(
                    eventReminderFrequencies = schedule.frequencies,
                    eventDailyScheduleTime = if (schedule.frequencies.contains("daily")) schedule.dailyTime else user.eventDailyScheduleTime,
                    eventWeeklyScheduleDay = if (schedule.frequencies.contains("weekly")) schedule.weeklyDay else user.eventWeeklyScheduleDay,
                    eventWeeklyScheduleTime = if (schedule.frequencies.contains("weekly")) schedule.weeklyTime else user.eventWeeklyScheduleTime,
                    eventMonthlyScheduleDate = if (schedule.frequencies.contains("monthly")) schedule.monthlyDate else user.eventMonthlyScheduleDate,
                    eventMonthlyScheduleTime = if (schedule.frequencies.contains("monthly")) schedule.monthlyTime else user.eventMonthlyScheduleTime
                )
                "softdrink" -> user.copy(
                    softdrinkTrendsFrequencies = schedule.frequencies,
                    softdrinkTrendsDailyScheduleTime = if (schedule.frequencies.contains("daily")) schedule.dailyTime else user.softdrinkTrendsDailyScheduleTime,
                    softdrinkTrendsWeeklyScheduleDay = if (schedule.frequencies.contains("weekly")) schedule.weeklyDay else user.softdrinkTrendsWeeklyScheduleDay,
                    softdrinkTrendsWeeklyScheduleTime = if (schedule.frequencies.contains("weekly")) schedule.weeklyTime else user.softdrinkTrendsWeeklyScheduleTime,
                    softdrinkTrendsMonthlyScheduleDate = if (schedule.frequencies.contains("monthly")) schedule.monthlyDate else user.softdrinkTrendsMonthlyScheduleDate,
                    softdrinkTrendsMonthlyScheduleTime = if (schedule.frequencies.contains("monthly")) schedule.monthlyTime else user.softdrinkTrendsMonthlyScheduleTime
                )
                else -> user
            }
        }

        scope.launch {
            val profileUpdates = mutableMapOf<String, Any?>()
            
            when (type) {
                "stock" -> {
                    stockAlerts = true
                    
                    profileUpdates["stock_alert_frequencies"] = schedule.frequencies
                    if (schedule.frequencies.contains("daily")) {
                        profileUpdates["daily_schedule_time"] = schedule.dailyTime
                    }
                    if (schedule.frequencies.contains("weekly")) {
                        profileUpdates["weekly_schedule_day"] = schedule.weeklyDay
                        profileUpdates["weekly_schedule_time"] = schedule.weeklyTime
                    }
                    if (schedule.frequencies.contains("monthly")) {
                        profileUpdates["monthly_schedule_date"] = schedule.monthlyDate
                        profileUpdates["monthly_schedule_time"] = schedule.monthlyTime
                    }
                }
                "event" -> {
                    eventReminders = true
                    
                    profileUpdates["event_reminder_frequencies"] = schedule.frequencies
                    if (schedule.frequencies.contains("daily")) {
                        profileUpdates["event_daily_schedule_time"] = schedule.dailyTime
                    }
                    if (schedule.frequencies.contains("weekly")) {
                        profileUpdates["event_weekly_schedule_day"] = schedule.weeklyDay
                        profileUpdates["event_weekly_schedule_time"] = schedule.weeklyTime
                    }
                    if (schedule.frequencies.contains("monthly")) {
                        profileUpdates["event_monthly_schedule_date"] = schedule.monthlyDate
                        profileUpdates["event_monthly_schedule_time"] = schedule.monthlyTime
                    }
                }
                "softdrink" -> {
                    softdrinkTrends = true
                    
                    profileUpdates["softdrink_trends_frequencies"] = schedule.frequencies
                    if (schedule.frequencies.contains("daily")) {
                        profileUpdates["softdrink_trends_daily_schedule_time"] = schedule.dailyTime
                    }
                    if (schedule.frequencies.contains("weekly")) {
                        profileUpdates["softdrink_trends_weekly_schedule_day"] = schedule.weeklyDay
                        profileUpdates["softdrink_trends_weekly_schedule_time"] = schedule.weeklyTime
                    }
                    if (schedule.frequencies.contains("monthly")) {
                        profileUpdates["softdrink_trends_monthly_schedule_date"] = schedule.monthlyDate
                        profileUpdates["softdrink_trends_monthly_schedule_time"] = schedule.monthlyTime
                    }
                }
            }
            
            // Save notification setting first
            val notificationSettings = mapOf(
                "email" to emailNotifications,
                "sms" to smsNotifications,
                "whatsapp" to whatsappNotifications,
                "stockLevelAlerts" to stockAlerts,
                "eventReminders" to eventReminders,
                "softdrinkTrends" to softdrinkTrends
            )
            
            try {
                authRepository.updateNotificationSettings(notificationSettings)
                authRepository.updateProfile(profileUpdates)
                
                snackbarHostState.showSnackbar("Schedule saved successfully")
                
                // Refresh profile to get updated data
                val refreshResult = authRepository.refreshProfile()
                currentUser = refreshResult.getOrNull()
                
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Error saving schedule: ${e.message}")
            }
        }
    }

    fun buildAlertSchedule(
        frequencies: List<String>?,
        dailyTime: String?,
        weeklyDay: Int?,
        weeklyTime: String?,
        monthlyDate: Int?,
        monthlyTime: String?
    ): AlertSchedule? {
        if (frequencies.isNullOrEmpty()) return null
        return AlertSchedule(
            frequencies = frequencies,
            dailyTime = dailyTime,
            weeklyDay = weeklyDay,
            weeklyTime = weeklyTime,
            monthlyDate = monthlyDate,
            monthlyTime = monthlyTime
        )
    }
    
    // Load current user profile
    LaunchedEffect(Unit) {
        scope.launch {
            isLoading = true
            try {
                android.util.Log.d("SettingsScreen", "============ LOAD START ============")
                android.util.Log.d("SettingsScreen", "Calling refreshProfile()...")
                
                val refreshResult = authRepository.refreshProfile()
                android.util.Log.d("SettingsScreen", "Refresh result: ${if (refreshResult.isSuccess) "✅ SUCCESS" else "❌ FAILED"}")
                
                // Use the FRESH user from refreshProfile result, NOT from DataStore cache
                val user = refreshResult.getOrNull()
                android.util.Log.d("SettingsScreen", "Got fresh user from refreshProfile: name=${user?.name}, phone=${user?.phone}")
                android.util.Log.d("SettingsScreen", "Fresh notification_settings: ${user?.notificationSettings}")
                
                currentUser = user
                name = user?.name ?: ""
                phone = user?.phone ?: ""
                position = user?.position ?: ""
                
                // Load notification preferences from user data
                user?.notificationSettings?.let { settings ->
                    android.util.Log.d("SettingsScreen", "📥 LOADING toggles from settings map:")
                    android.util.Log.d("SettingsScreen", "  Raw map: $settings")
                    
                    emailNotifications = settings["email"] as? Boolean ?: false
                    smsNotifications = settings["sms"] as? Boolean ?: false
                    whatsappNotifications = settings["whatsapp"] as? Boolean ?: false
                    stockAlerts = settings["stockLevelAlerts"] as? Boolean ?: (user.dailyScheduleTime != null)
                    eventReminders = settings["eventReminders"] as? Boolean ?: (user.eventDailyScheduleTime != null)
                    softdrinkTrends = settings["softdrinkTrends"] as? Boolean ?: (user.softdrinkTrendsDailyScheduleTime != null)
                    
                    android.util.Log.d("SettingsScreen", "📥 LOADED toggle states:")
                    android.util.Log.d("SettingsScreen", "  email=$emailNotifications")
                    android.util.Log.d("SettingsScreen", "  sms=$smsNotifications")
                    android.util.Log.d("SettingsScreen", "  whatsapp=$whatsappNotifications")
                    android.util.Log.d("SettingsScreen", "  stockAlerts=$stockAlerts")
                    android.util.Log.d("SettingsScreen", "  eventReminders=$eventReminders")
                    android.util.Log.d("SettingsScreen", "  softdrinkTrends=$softdrinkTrends")
                } ?: run {
                    android.util.Log.d("SettingsScreen", "⚠️ No notification settings found, using schedule defaults")
                    stockAlerts = user?.dailyScheduleTime != null
                    eventReminders = user?.eventDailyScheduleTime != null
                    softdrinkTrends = user?.softdrinkTrendsDailyScheduleTime != null
                }
                android.util.Log.d("SettingsScreen", "============ LOAD END ============")
            } catch (e: Exception) {
                android.util.Log.e("SettingsScreen", "❌ EXCEPTION during load", e)
                snackbarHostState.showSnackbar("Error loading profile: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                
                // Profile Settings Section
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null)
                            Text(
                                text = "Profile Settings",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Profile Picture
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (currentUser?.photoUrl != null) {
                                    AsyncImage(
                                        model = currentUser?.photoUrl,
                                        contentDescription = "Profile Picture",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                
                                // Edit overlay
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.3f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                        
                        OutlinedTextField(
                            value = name,
                            onValueChange = { if (it.length <= 100) name = it },
                            label = { Text("Name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        OutlinedTextField(
                            value = currentUser?.email ?: "",
                            onValueChange = {},
                            label = { Text("Email") },
                            enabled = false,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { if (it.length <= 30) phone = it },
                            label = { Text("Phone Number") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        OutlinedTextField(
                            value = position,
                            onValueChange = { if (it.length <= 100) position = it },
                            label = { Text("Position") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        OutlinedTextField(
                            value = currentUser?.role?.toString() ?: "",
                            onValueChange = {},
                            label = { Text("Role") },
                            enabled = false,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Button(
                            onClick = {
                                scope.launch {
                                    isSaving = true
                                    try {
                                        val updates = mutableMapOf<String, Any?>()
                                        if (name != currentUser?.name) updates["name"] = name
                                        if (phone != currentUser?.phone) updates["phone"] = phone
                                        if (position != currentUser?.position) updates["position"] = position
                                        
                                        val result = authRepository.updateProfile(updates)
                                        if (result.isSuccess) {
                                            currentUser = result.getOrNull()
                                            snackbarHostState.showSnackbar("Profile updated successfully")
                                        } else {
                                            snackbarHostState.showSnackbar("Failed to update profile")
                                        }
                                    } catch (e: Exception) {
                                        snackbarHostState.showSnackbar("Error: ${e.message}")
                                    } finally {
                                        isSaving = false
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isSaving
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text("Save Changes")
                            }
                        }
                    }
                }
                
                // Notification Preferences (hide for staff)
                if (currentUser?.role != "STAFF") {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Notifications, contentDescription = null)
                                Text(
                                    text = "Notification Preferences",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            Text(
                                text = "Alert Types",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            
                            // Stock Level Alerts
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Stock Level Alerts",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "Get notified about low stock levels",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = stockAlerts,
                                    onCheckedChange = { 
                                        if (it) {
                                            showStockAlertDialog = true
                                        } else {
                                            stockAlerts = false
                                            saveNotificationPreferences()
                                        }
                                    }
                                )
                            }
                            
                            // Event Reminders
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Event Reminders",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "Receive reminders for upcoming events",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = eventReminders,
                                    onCheckedChange = { 
                                        if (it) {
                                            showEventReminderDialog = true
                                        } else {
                                            eventReminders = false
                                            saveNotificationPreferences()
                                        }
                                    }
                                )
                            }
                            
                            // Softdrink Trends
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Softdrink Trends",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "Get updates on softdrink consumption trends",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = softdrinkTrends,
                                    onCheckedChange = { 
                                        if (it) {
                                            showSoftdrinkDialog = true
                                        } else {
                                            softdrinkTrends = false
                                            saveNotificationPreferences()
                                        }
                                    }
                                )
                            }
                            
                            Divider()
                            
                            Text(
                                text = "Notification Channels",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            
                            // Email Notifications
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Email, contentDescription = null)
                                    Text("Email Notifications")
                                }
                                Switch(
                                    checked = emailNotifications,
                                    onCheckedChange = { 
                                        emailNotifications = it
                                        saveNotificationPreferences()
                                    }
                                )
                            }
                            
                            // SMS Notifications
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Sms, contentDescription = null)
                                    Text("SMS Notifications")
                                }
                                Switch(
                                    checked = smsNotifications,
                                    onCheckedChange = { 
                                        smsNotifications = it
                                        saveNotificationPreferences()
                                    }
                                )
                            }
                            
                            // WhatsApp Notifications
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Chat, contentDescription = null)
                                    Text("WhatsApp Notifications")
                                }
                                Switch(
                                    checked = whatsappNotifications,
                                    onCheckedChange = { 
                                        if (phone.isBlank()) {
                                            scope.launch {
                                                snackbarHostState.showSnackbar("Please add a phone number first")
                                            }
                                        } else {
                                            whatsappNotifications = it
                                            saveNotificationPreferences()
                                        }
                                    }
                                )
                            }
                            
                            Divider()
                            
                            // Current Alert Schedule Section
                            Text(
                                text = "Current Alert Schedule",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            
                            // Stock Alerts Schedule
                            if (stockAlerts) {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "Stock Level Alerts",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        ScheduleDetails(
                                            frequencies = currentUser?.stockAlertFrequencies,
                                            dailyTime = currentUser?.dailyScheduleTime,
                                            weeklyDay = currentUser?.weeklyScheduleDay,
                                            weeklyTime = currentUser?.weeklyScheduleTime,
                                            monthlyDate = currentUser?.monthlyScheduleDate,
                                            monthlyTime = currentUser?.monthlyScheduleTime
                                        )
                                    }
                                }
                            }
                            
                            // Event Reminders Schedule
                            if (eventReminders) {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "Event Reminders",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        ScheduleDetails(
                                            frequencies = currentUser?.eventReminderFrequencies,
                                            dailyTime = currentUser?.eventDailyScheduleTime,
                                            weeklyDay = currentUser?.eventWeeklyScheduleDay,
                                            weeklyTime = currentUser?.eventWeeklyScheduleTime,
                                            monthlyDate = currentUser?.eventMonthlyScheduleDate,
                                            monthlyTime = currentUser?.eventMonthlyScheduleTime
                                        )
                                    }
                                }
                            }
                            
                            // Softdrink Trends Schedule
                            if (softdrinkTrends) {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "Softdrink Trends",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        ScheduleDetails(
                                            frequencies = currentUser?.softdrinkTrendsFrequencies,
                                            dailyTime = currentUser?.softdrinkTrendsDailyScheduleTime,
                                            weeklyDay = currentUser?.softdrinkTrendsWeeklyScheduleDay,
                                            weeklyTime = currentUser?.softdrinkTrendsWeeklyScheduleTime,
                                            monthlyDate = currentUser?.softdrinkTrendsMonthlyScheduleDate,
                                            monthlyTime = currentUser?.softdrinkTrendsMonthlyScheduleTime
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Permissions Management (managers only)
                if (currentUser?.role?.equals("MANAGER", ignoreCase = true) == true || 
                    currentUser?.role?.equals("manager", ignoreCase = true) == true) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Security, contentDescription = null)
                                Text(
                                    text = "Permissions Management",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            var assistantManagerStockInAccess by remember { mutableStateOf(currentUser?.assistantManagerStockInAccess ?: false) }
                            var isSavingPermission by remember { mutableStateOf(false) }
                            
                            LaunchedEffect(currentUser) {
                                assistantManagerStockInAccess = currentUser?.assistantManagerStockInAccess ?: false
                            }
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Assistant Manager Stock In Access",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "Allow Assistant Managers to access\nthe Stock In page",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = assistantManagerStockInAccess,
                                    onCheckedChange = { assistantManagerStockInAccess = it }
                                )
                            }
                            
                            Button(
                                onClick = {
                                    scope.launch {
                                        isSavingPermission = true
                                        try {
                                            // Store in notification_settings like the web app does
                                            val notificationSettings = currentUser?.notificationSettings?.toMutableMap() ?: mutableMapOf()
                                            notificationSettings["assistant_manager_stock_in_access"] = assistantManagerStockInAccess
                                            
                                            val result = authRepository.updateNotificationSettings(notificationSettings)
                                            if (result.isSuccess) {
                                                currentUser = result.getOrNull()
                                                snackbarHostState.showSnackbar("Permission updated successfully")
                                            } else {
                                                snackbarHostState.showSnackbar("Failed to update permission")
                                            }
                                        } catch (e: Exception) {
                                            snackbarHostState.showSnackbar("Error: ${e.message}")
                                        } finally {
                                            isSavingPermission = false
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isSavingPermission
                            ) {
                                if (isSavingPermission) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                } else {
                                    Text("Save Changes")
                                }
                            }
                        }
                    }
                }
                
                // Branch Settings (for managers and assistant managers)
                if (currentUser?.role?.equals("MANAGER", ignoreCase = true) == true || 
                    currentUser?.role?.equals("ASSISTANT_MANAGER", ignoreCase = true) == true ||
                    currentUser?.role?.equals("manager", ignoreCase = true) == true ||
                    currentUser?.role?.equals("assistant_manager", ignoreCase = true) == true) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Store, contentDescription = null)
                                Text(
                                    text = "Branch Settings",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            OutlinedTextField(
                                value = currentUser?.branchName ?: "N/A",
                                onValueChange = {},
                                label = { Text("Branch Name") },
                                enabled = false,
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            OutlinedTextField(
                                value = currentUser?.branchLocation ?: "${currentUser?.branchName ?: ""}, ${currentUser?.districtName ?: ""}, ${currentUser?.regionName ?: ""}".trim(',', ' ').ifEmpty { "Not specified" },
                                onValueChange = {},
                                label = { Text("Branch Location") },
                                enabled = false,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
                
                // System Information
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null)
                            Text(
                                text = "System Information",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        OutlinedTextField(
                            value = formatTimestamp(currentUser?.createdAt ?: ""),
                            onValueChange = {},
                            label = { Text("Account Created") },
                            enabled = false,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        OutlinedTextField(
                            value = formatTimestamp(currentUser?.updatedAt ?: ""),
                            onValueChange = {},
                            label = { Text("Last Updated") },
                            enabled = false,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        OutlinedTextField(
                            value = "${currentUser?.accessCount ?: 0}",
                            onValueChange = {},
                            label = { Text("Login Count") },
                            enabled = false,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            
            if (showStockAlertDialog) {
                AlertSchedulingDialog(
                    onDismissRequest = { showStockAlertDialog = false },
                    onSave = { schedule ->
                        showStockAlertDialog = false
                        handleScheduleSave("stock", schedule)
                    },
                    initialSchedule = buildAlertSchedule(
                        currentUser?.stockAlertFrequencies,
                        currentUser?.dailyScheduleTime,
                        currentUser?.weeklyScheduleDay,
                        currentUser?.weeklyScheduleTime,
                        currentUser?.monthlyScheduleDate,
                        currentUser?.monthlyScheduleTime
                    ),
                    title = "Stock Alert Schedule",
                    description = "Choose how often you want to receive stock level alerts."
                )
            }

            if (showEventReminderDialog) {
                AlertSchedulingDialog(
                    onDismissRequest = { showEventReminderDialog = false },
                    onSave = { schedule ->
                        showEventReminderDialog = false
                        handleScheduleSave("event", schedule)
                    },
                    initialSchedule = buildAlertSchedule(
                        currentUser?.eventReminderFrequencies,
                        currentUser?.eventDailyScheduleTime,
                        currentUser?.eventWeeklyScheduleDay,
                        currentUser?.eventWeeklyScheduleTime,
                        currentUser?.eventMonthlyScheduleDate,
                        currentUser?.eventMonthlyScheduleTime
                    ),
                    title = "Event Reminder Schedule",
                    description = "Choose how often you want to receive event reminders."
                )
            }

            if (showSoftdrinkDialog) {
                AlertSchedulingDialog(
                    onDismissRequest = { showSoftdrinkDialog = false },
                    onSave = { schedule ->
                        showSoftdrinkDialog = false
                        handleScheduleSave("softdrink", schedule)
                    },
                    initialSchedule = buildAlertSchedule(
                        currentUser?.softdrinkTrendsFrequencies,
                        currentUser?.softdrinkTrendsDailyScheduleTime,
                        currentUser?.softdrinkTrendsWeeklyScheduleDay,
                        currentUser?.softdrinkTrendsWeeklyScheduleTime,
                        currentUser?.softdrinkTrendsMonthlyScheduleDate,
                        currentUser?.softdrinkTrendsMonthlyScheduleTime
                    ),
                    title = "Softdrink Trends Schedule",
                    description = "Choose how often you want to receive softdrink trend alerts."
                )
            }
        }
    }
}

@Composable
fun ScheduleDetails(
    frequencies: List<String>?,
    dailyTime: String?,
    weeklyDay: Int?,
    weeklyTime: String?,
    monthlyDate: Int?,
    monthlyTime: String?
) {
    if (frequencies.isNullOrEmpty()) {
        Text(
            text = "Not configured",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    } else {
        if (frequencies.contains("daily")) {
            Text(
                text = "Daily: ${dailyTime ?: "Not set"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (frequencies.contains("weekly")) {
            val dayName = weeklyDay?.let { 
                listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday").getOrNull(it) 
            } ?: "Day $weeklyDay"
            Text(
                text = "Weekly: $dayName at ${weeklyTime ?: "Not set"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (frequencies.contains("monthly")) {
            val suffix = getOrdinalSuffix(monthlyDate ?: 0)
            Text(
                text = "Monthly: ${monthlyDate ?: 0}$suffix at ${monthlyTime ?: "Not set"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun getOrdinalSuffix(num: Int): String {
    val j = num % 10
    val k = num % 100
    if (j == 1 && k != 11) return "st"
    if (j == 2 && k != 12) return "nd"
    if (j == 3 && k != 13) return "rd"
    return "th"
}

private fun formatTimestamp(timestamp: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val outputFormat = SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault())
        val date = inputFormat.parse(timestamp)
        date?.let { outputFormat.format(it) } ?: timestamp
    } catch (e: Exception) {
        timestamp
    }
}
