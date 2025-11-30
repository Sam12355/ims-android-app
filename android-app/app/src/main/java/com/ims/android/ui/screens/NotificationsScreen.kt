package com.ims.android.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ims.android.data.api.ApiClient
import com.ims.android.data.model.User
import com.ims.android.data.repository.NotificationRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class NotificationItem(
    val id: String,
    val type: String,
    val message: String,
    val createdAt: String,
    val isRead: Boolean
)

// Helper function to strip HTML tags from notification messages
fun stripHtmlTags(text: String): String {
    return text.replace(Regex("<[^>]*>"), "").trim()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    user: User,
    apiClient: ApiClient,
    onBack: () -> Unit
) {
    var unreadNotifications by remember { mutableStateOf<List<NotificationItem>>(emptyList()) }
    var readNotifications by remember { mutableStateOf<List<NotificationItem>>(emptyList()) }
    var dismissingIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var unreadExpanded by remember { mutableStateOf(true) }
    var readExpanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val notificationRepository = remember { NotificationRepository(apiClient) }
    
    suspend fun loadNotifications() {
        try {
            val result = notificationRepository.getNotifications()
            result.onSuccess { data ->
                val allNotifications = data.map { notif ->
                    NotificationItem(
                        id = notif.id,
                        type = notif.type ?: "general",
                        message = notif.message ?: "No message",
                        createdAt = notif.createdAt ?: "",
                        isRead = notif.isRead ?: false
                    )
                }.sortedByDescending { it.createdAt }
                
                unreadNotifications = allNotifications.filter { !it.isRead }
                readNotifications = allNotifications.filter { it.isRead }
                
                android.util.Log.d("NotificationsScreen", "Loaded ${unreadNotifications.size} unread, ${readNotifications.size} read notifications")
            }
        } catch (e: Exception) {
            android.util.Log.e("NotificationsScreen", "Error loading notifications", e)
        } finally {
            isLoading = false
            isRefreshing = false
        }
    }
    
    fun refreshNotifications() {
        scope.launch {
            isRefreshing = true
            loadNotifications()
        }
    }
    
    // Load notifications on first composition
    LaunchedEffect(Unit) {
        loadNotifications()
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        when {
            isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            unreadNotifications.isEmpty() && readNotifications.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No notifications",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "All caught up!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Unread notifications accordion
                    item {
                        NotificationAccordion(
                            title = "Unread Notifications",
                            count = unreadNotifications.size,
                            expanded = unreadExpanded,
                            onToggle = { unreadExpanded = !unreadExpanded },
                            isUnread = true,
                            isRefreshing = isRefreshing
                        )
                    }
                    
                    if (unreadExpanded) {
                        if (unreadNotifications.isEmpty()) {
                            item {
                                Text(
                                    text = "No unread notifications",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                        } else {
                            items(
                                items = unreadNotifications,
                                key = { "unread_${it.id}" }
                            ) { notification ->
                                AnimatedVisibility(
                                    visible = !dismissingIds.contains(notification.id),
                                    exit = shrinkVertically() + fadeOut()
                                ) {
                                    NotificationCard(
                                        notification = notification,
                                        onMarkAsRead = {
                                            scope.launch {
                                                notificationRepository.markNotificationAsRead(notification.id)
                                                loadNotifications()
                                            }
                                        },
                                        onClick = {
                                            dismissingIds = dismissingIds + notification.id
                                            scope.launch {
                                                kotlinx.coroutines.delay(300)
                                                unreadNotifications = unreadNotifications.filter { it.id != notification.id }
                                                dismissingIds = dismissingIds - notification.id
                                                notificationRepository.markNotificationAsRead(notification.id)
                                                loadNotifications()
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                    
                    // Read notifications accordion
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        NotificationAccordion(
                            title = "Read Notifications",
                            count = readNotifications.size,
                            expanded = readExpanded,
                            onToggle = { readExpanded = !readExpanded },
                            isUnread = false,
                            isRefreshing = false
                        )
                    }
                    
                    if (readExpanded) {
                        if (readNotifications.isEmpty()) {
                            item {
                                Text(
                                    text = "No read notifications",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                        } else {
                            items(
                                items = readNotifications,
                                key = { "read_${it.id}" }
                            ) { notification ->
                                NotificationCard(
                                    notification = notification,
                                    onMarkAsRead = { },
                                    onClick = { }
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
fun NotificationAccordion(
    title: String,
    count: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
    isUnread: Boolean,
    isRefreshing: Boolean
) {
    val rotationAngle by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "rotation"
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable { onToggle() },
        colors = CardDefaults.cardColors(
            containerColor = if (isUnread) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isUnread) Icons.Default.MarkEmailUnread else Icons.Default.MarkEmailRead,
                    contentDescription = null,
                    tint = if (isUnread) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isUnread) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    Text(
                        text = "$count notification${if (count != 1) "s" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isUnread) {
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        }
                    )
                }
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                }
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    modifier = Modifier.rotate(rotationAngle),
                    tint = if (isUnread) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}

@Composable
fun NotificationCard(
    notification: NotificationItem,
    onMarkAsRead: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (notification.isRead) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.secondaryContainer
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            notification.type.contains("stock", ignoreCase = true) -> 
                                MaterialTheme.colorScheme.errorContainer
                            notification.type.contains("event", ignoreCase = true) -> 
                                MaterialTheme.colorScheme.tertiaryContainer
                            else -> MaterialTheme.colorScheme.primaryContainer
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        notification.type.contains("stock", ignoreCase = true) -> Icons.Default.Warning
                        notification.type.contains("event", ignoreCase = true) -> Icons.Default.Event
                        else -> Icons.Default.Notifications
                    },
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = when {
                        notification.type.contains("stock", ignoreCase = true) -> 
                            MaterialTheme.colorScheme.onErrorContainer
                        notification.type.contains("event", ignoreCase = true) -> 
                            MaterialTheme.colorScheme.onTertiaryContainer
                        else -> MaterialTheme.colorScheme.onPrimaryContainer
                    }
                )
            }
            
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when {
                            notification.type.contains("stock", ignoreCase = true) -> "Stock Alert"
                            notification.type.contains("event", ignoreCase = true) -> "Event Reminder"
                            else -> "Notification"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (!notification.isRead) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                }
                
                Text(
                    text = stripHtmlTags(notification.message),
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Text(
                    text = formatNotificationDate(notification.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (!notification.isRead) {
                IconButton(
                    onClick = onMarkAsRead,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Done,
                        contentDescription = "Mark as read",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

fun formatNotificationDate(dateString: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        inputFormat.timeZone = TimeZone.getTimeZone("UTC")
        val date = inputFormat.parse(dateString) ?: return dateString
        
        val now = Calendar.getInstance()
        val notificationTime = Calendar.getInstance().apply { time = date }
        
        val diffInMillis = now.timeInMillis - notificationTime.timeInMillis
        val diffInMinutes = (diffInMillis / (1000 * 60)).toInt()
        val diffInHours = (diffInMillis / (1000 * 60 * 60)).toInt()
        val diffInDays = (diffInMillis / (1000 * 60 * 60 * 24)).toInt()
        
        when {
            diffInMinutes < 1 -> "Just now"
            diffInMinutes < 60 -> "$diffInMinutes minute${if (diffInMinutes != 1) "s" else ""} ago"
            diffInHours < 24 -> "$diffInHours hour${if (diffInHours != 1) "s" else ""} ago"
            diffInDays == 1 -> "Yesterday"
            diffInDays < 7 -> "$diffInDays days ago"
            else -> SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(date)
        }
    } catch (e: Exception) {
        dateString
    }
}
