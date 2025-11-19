package com.stocknexus.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.stocknexus.data.repository.InventoryRepository
import com.stocknexus.data.model.ActivityLog
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class TransformedActivity(
    val id: String,
    val title: String,
    val description: String,
    val userName: String,
    val timestamp: String,
    val originalAction: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityLogsScreen(
    inventoryRepository: InventoryRepository,
    modifier: Modifier = Modifier
) {
    var activities by remember { mutableStateOf<List<ActivityLog>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var filterType by remember { mutableStateOf("all") }
    var expandedFilter by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Extract quantity from details string
    fun extractQuantity(details: String?): String {
        if (details == null) return "0"
        return try {
            // Try to find number in the details string
            val numberRegex = "\\d+".toRegex()
            numberRegex.find(details)?.value ?: "0"
        } catch (e: Exception) {
            "0"
        }
    }
    
    // Transform activities to user-friendly format
    fun transformActivity(activity: ActivityLog): TransformedActivity {
        val userName = activity.profiles?.name ?: "Unknown User"
        
        // Clean up details - remove JSON formatting and extract useful info
        val cleanDetails = activity.details?.let { details ->
            // Remove JSON braces and quotes
            details.replace("{", "")
                .replace("}", "")
                .replace("\"", "")
                .replace(":", ": ")
                .replace(",", ", ")
                // Remove common technical fields
                .replace("item_id[^,]*,?".toRegex(), "")
                .replace("user_id[^,]*,?".toRegex(), "")
                .replace("id[^,]*,?".toRegex(), "")
                .replace("timestamp[^,]*,?".toRegex(), "")
                .replace("created_at[^,]*,?".toRegex(), "")
                .replace("updated_at[^,]*,?".toRegex(), "")
                // Clean up extra spaces and commas
                .replace(", ,", ",")
                .replace(",,", ",")
                .trim()
                .trim(',')
        }
        
        val (title, description) = when (activity.action) {
            "user_login" -> Pair(
                "User logged in",
                "$userName signed into the system"
            )
            "user_logout" -> Pair(
                "User logged out",
                "$userName signed out of the system"
            )
            "item_created" -> Pair(
                "Item added",
                "$userName added a new item"
            )
            "item_updated" -> Pair(
                "Item updated",
                "$userName updated an item"
            )
            "item_deleted" -> Pair(
                "Item removed",
                "$userName removed an item"
            )
            "staff_created" -> Pair(
                "Staff member added",
                "$userName added new staff member"
            )
            "staff_updated" -> Pair(
                "Staff member updated",
                "$userName updated staff member"
            )
            "staff_deleted" -> Pair(
                "Staff member removed",
                "$userName removed a staff member"
            )
            "profile_updated" -> Pair(
                "Profile updated",
                "$userName updated their profile"
            )
            "stock_in" -> {
                val quantity = extractQuantity(cleanDetails)
                Pair(
                    "Stock received",
                    "$userName received $quantity units"
                )
            }
            "stock_out" -> {
                val quantity = extractQuantity(cleanDetails)
                Pair(
                    "Stock dispensed",
                    "$userName dispensed $quantity units"
                )
            }
            "stock_movement" -> {
                val quantity = extractQuantity(cleanDetails)
                val movementType = if (cleanDetails?.contains("in", ignoreCase = true) == true) "received" else "dispensed"
                Pair(
                    "Stock $movementType",
                    "$userName $movementType $quantity units"
                )
            }
            "stock_initialized" -> Pair(
                "Stock initialized",
                "$userName initialized stock records"
            )
            else -> Pair(
                activity.action.replace("_", " ").split(" ").joinToString(" ") { 
                    it.replaceFirstChar { char -> char.uppercase() } 
                },
                "$userName performed an action"
            )
        }
        
        return TransformedActivity(
            id = activity.id,
            title = title,
            description = description,
            userName = userName,
            timestamp = activity.createdAt,
            originalAction = activity.action
        )
    }
    
    // Load activities
    LaunchedEffect(Unit) {
        scope.launch {
            isLoading = true
            try {
                val result = inventoryRepository.getActivityLogs()
                if (result.isSuccess) {
                    activities = result.getOrNull() ?: emptyList()
                    errorMessage = null
                } else {
                    errorMessage = result.exceptionOrNull()?.message ?: "Failed to load activities"
                }
            } catch (e: Exception) {
                errorMessage = "Error loading activities: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }
    
    // Transform and filter activities
    val transformedActivities = remember(activities) {
        activities.map { transformActivity(it) }
    }
    
    val filteredActivities = remember(transformedActivities, filterType) {
        when (filterType) {
            "stock" -> transformedActivities.filter { 
                it.title.contains("Stock", ignoreCase = true) 
            }
            "general" -> transformedActivities.filter { 
                !it.title.contains("Stock", ignoreCase = true) 
            }
            else -> transformedActivities
        }
    }
    
    fun getIcon(title: String) = when {
        title.contains("Stock received") || title.contains("Stock in") -> Icons.Default.Add
        title.contains("Stock dispensed") || title.contains("Stock out") -> Icons.Default.Remove
        title.contains("Item added") || title.contains("created") -> Icons.Default.Add
        title.contains("Item updated") || title.contains("updated") -> Icons.Default.Edit
        title.contains("Item removed") || title.contains("deleted") || title.contains("removed") -> Icons.Default.Delete
        title.contains("Staff member added") || title.contains("Staff member updated") -> Icons.Default.Person
        title.contains("Staff member removed") -> Icons.Default.PersonRemove
        title.contains("Profile updated") -> Icons.Default.Person
        title.contains("logged in") -> Icons.Default.Login
        title.contains("logged out") -> Icons.Default.Logout
        else -> Icons.Default.Info
    }
    
    fun getIconColor(title: String) = when {
        title.contains("Stock received") || title.contains("Item added") || 
        title.contains("Staff member added") || title.contains("logged in") -> Color(0xFF10B981)
        title.contains("Stock dispensed") || title.contains("Item updated") || 
        title.contains("Staff member updated") || title.contains("Profile updated") -> Color(0xFFF59E0B)
        title.contains("Item removed") || title.contains("Staff member removed") -> Color(0xFFEF4444)
        title.contains("logged out") -> Color.Gray
        else -> Color.Gray
    }
    
    fun getBadgeColor(title: String) = when {
        title.contains("Stock received") || title.contains("Item added") || 
        title.contains("Staff member added") || title.contains("logged in") -> Color(0xFF10B981)
        title.contains("Stock dispensed") || title.contains("Item updated") || 
        title.contains("Staff member updated") || title.contains("Profile updated") -> Color(0xFFF59E0B)
        title.contains("Item removed") || title.contains("Staff member removed") -> Color(0xFFEF4444)
        title.contains("logged out") -> Color.Gray
        else -> Color(0xFF6B7280)
    }
    
    fun formatTimestamp(timestamp: String): String {
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val date = parser.parse(timestamp)
            val formatter = SimpleDateFormat("M/d/yyyy, h:mm:ss a", Locale.getDefault())
            date?.let { formatter.format(it) } ?: timestamp
        } catch (e: Exception) {
            timestamp
        }
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Activity Logs") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Filter dropdown
            ExposedDropdownMenuBox(
                expanded = expandedFilter,
                onExpandedChange = { expandedFilter = !expandedFilter },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                OutlinedTextField(
                    value = when (filterType) {
                        "stock" -> "Stock Movements"
                        "general" -> "General Logs"
                        else -> "All Activities"
                    },
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedFilter) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                )
                
                ExposedDropdownMenu(
                    expanded = expandedFilter,
                    onDismissRequest = { expandedFilter = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("All Activities") },
                        onClick = {
                            filterType = "all"
                            expandedFilter = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Stock Movements") },
                        onClick = {
                            filterType = "stock"
                            expandedFilter = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("General Logs") },
                        onClick = {
                            filterType = "general"
                            expandedFilter = false
                        }
                    )
                }
            }
            
            // Activity count
            Text(
                text = "${filteredActivities.size} of ${transformedActivities.size} activities",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // Content
            Card(
                modifier = Modifier.fillMaxSize()
            ) {
                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    errorMessage != null -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = errorMessage ?: "Unknown error",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    filteredActivities.isEmpty() -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No activities found")
                        }
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(filteredActivities) { activity ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Title with Badge
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(
                                                    imageVector = getIcon(activity.title),
                                                    contentDescription = null,
                                                    tint = getIconColor(activity.title),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Text(
                                                    text = activity.title,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        
                                        // Description
                                        Text(
                                            text = activity.description,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        
                                        // User and timestamp
                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = activity.userName,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = formatTimestamp(activity.timestamp),
                                                style = MaterialTheme.typography.bodySmall,
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
        }
    }
}
