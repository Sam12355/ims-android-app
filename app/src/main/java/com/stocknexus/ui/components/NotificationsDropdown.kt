package com.stocknexus.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.stocknexus.data.api.ApiClient
import com.stocknexus.data.model.User
import com.stocknexus.data.repository.InventoryRepository
import com.stocknexus.data.repository.NotificationRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class Notification(
    val id: String,
    val type: String,
    val message: String,
    val createdAt: String,
    val isRead: Boolean = false
)

data class CalendarEvent(
    val id: String,
    val title: String,
    val eventDate: String,
    val description: String? = null
)

data class StockAlert(
    val id: String,
    val name: String,
    val currentQuantity: Int,
    val thresholdLevel: Int
)

@Composable
fun NotificationsDropdown(
    user: User,
    apiClient: ApiClient,
    inventoryRepository: InventoryRepository,
    notificationRepository: NotificationRepository,
    onDismiss: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNotificationMarkedRead: () -> Unit = {}
) {
    var notifications by remember { mutableStateOf<List<Notification>>(emptyList()) }
    var events by remember { mutableStateOf<List<CalendarEvent>>(emptyList()) }
    var stockAlerts by remember { mutableStateOf<List<StockAlert>>(emptyList()) }
    var isRefreshing by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    
    suspend fun loadNotifications() {
        try {
            val result = notificationRepository.getNotifications()
            result.onSuccess { data ->
                notifications = data.map { notif ->
                    Notification(
                        id = notif.id,
                        type = notif.type ?: "general",
                        message = notif.message ?: "No message",
                        createdAt = notif.createdAt ?: "",
                        isRead = notif.isRead ?: false
                    )
                }.filter { !it.isRead }
            }
        } catch (e: Exception) {
            android.util.Log.e("NotificationsDropdown", "Error loading notifications", e)
        }
    }
    
    suspend fun loadEvents() {
        try {
            events = apiClient.getCalendarEvents().map { event ->
                CalendarEvent(
                    id = event.id,
                    title = event.title,
                    eventDate = event.eventDate,
                    description = event.description
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("NotificationsDropdown", "Error loading calendar events", e)
        }
    }
    
    suspend fun loadStockAlerts() {
        val result = inventoryRepository.getStockData()
        result.onSuccess { data ->
            stockAlerts = data
                .filter { item -> 
                    val threshold = item.items?.threshold_level ?: 0
                    item.current_quantity <= threshold
                }
                .map { item ->
                    StockAlert(
                        id = item.item_id ?: "",
                        name = item.items?.name ?: "Unknown",
                        currentQuantity = item.current_quantity,
                        thresholdLevel = item.items?.threshold_level ?: 0
                    )
                }
        }
    }
    
    fun refreshAll() {
        scope.launch {
            isRefreshing = true
            loadNotifications()
            loadEvents()
            loadStockAlerts()
            isRefreshing = false
        }
    }
    
    // Load data on first composition
    LaunchedEffect(Unit) {
        loadNotifications()
        loadEvents()
        loadStockAlerts()
    }
    
    val totalCount = notifications.size + events.size + stockAlerts.size
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .width(320.dp)
                .heightIn(max = 600.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Notifications",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Refresh button
                        IconButton(
                            onClick = { refreshAll() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        
                        // View all button
                        IconButton(
                            onClick = {
                                onDismiss()
                                onNavigateToNotifications()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "View all",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
                
                Divider()
                
                // Content
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                ) {
                    // Stock Alerts Section
                    if (stockAlerts.isNotEmpty()) {
                        item {
                            Text(
                                text = "Stock Alerts",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                        
                        items(stockAlerts) { alert ->
                            StockAlertItem(
                                alert = alert,
                                onDismiss = {
                                    stockAlerts = stockAlerts.filter { it.id != alert.id }
                                }
                            )
                        }
                        
                        item {
                            Divider(modifier = Modifier.padding(vertical = 8.dp))
                        }
                    }
                    
                    // Upcoming Events Section
                    if (events.isNotEmpty()) {
                        item {
                            Text(
                                text = "Upcoming Events",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                        
                        items(events) { event ->
                            EventItem(
                                event = event,
                                onDismiss = {
                                    events = events.filter { it.id != event.id }
                                }
                            )
                        }
                        
                        item {
                            Divider(modifier = Modifier.padding(vertical = 8.dp))
                        }
                    }
                    
                    // General Notifications Section
                    if (notifications.isNotEmpty()) {
                        item {
                            Text(
                                text = "General Notifications",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                        
                        items(notifications) { notification ->
                            NotificationItem(
                                notification = notification,
                                onDismiss = {
                                    // Mark as read in database
                                    scope.launch {
                                        android.util.Log.d("NotificationsDropdown", "Marking notification ${notification.id} as read")
                                        val result = notificationRepository.markNotificationAsRead(notification.id)
                                        if (result.isSuccess) {
                                            android.util.Log.d("NotificationsDropdown", "Successfully marked notification ${notification.id} as read")
                                            // Remove from UI after successful API call
                                            notifications = notifications.filter { it.id != notification.id }
                                            // Update badge immediately
                                            onNotificationMarkedRead()
                                        } else {
                                            android.util.Log.e("NotificationsDropdown", "Failed to mark notification as read: ${result.exceptionOrNull()?.message}")
                                        }
                                    }
                                }
                            )
                        }
                    }
                    
                    // Empty state
                    if (totalCount == 0) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No new notifications",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StockAlertItem(
    alert: StockAlert,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onDismiss() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = Color(0xFFFF8800),
                modifier = Modifier.size(20.dp)
            )
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = alert.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Stock: ${alert.currentQuantity}/${alert.thresholdLevel}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    modifier = Modifier.padding(top = 4.dp),
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFFFF8800).copy(alpha = 0.1f)
                ) {
                    Text(
                        text = "Low Stock",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFFF8800),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EventItem(
    event: CalendarEvent,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onDismiss() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Event,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = formatDate(event.eventDate),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    modifier = Modifier.padding(top = 4.dp),
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = "Event",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationItem(
    notification: Notification,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onDismiss() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = notification.message,
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = formatDate(notification.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatDate(dateString: String): String {
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val formatter = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
        val date = parser.parse(dateString)
        date?.let { formatter.format(it) } ?: dateString
    } catch (e: Exception) {
        dateString
    }
}
