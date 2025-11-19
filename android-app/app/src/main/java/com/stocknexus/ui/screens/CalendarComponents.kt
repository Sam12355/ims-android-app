package com.stocknexus.ui.screens

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
import com.stocknexus.data.model.CalendarEvent
import com.stocknexus.data.model.Branch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CalendarEventsCard(
    events: List<CalendarEvent>,
    selectedDate: Date,
    onDateSelected: (Date) -> Unit,
    onAddEventClick: () -> Unit,
    showAddButton: Boolean = false,
    modifier: Modifier = Modifier
) {
    // Extract event dates for highlighting
    val eventDates = remember(events) {
        events.mapNotNull { event ->
            try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                inputFormat.parse(event.eventDate)
            } catch (e: Exception) {
                null
            }
        }.toSet()
    }
    
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Calendar & Events",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Upcoming events and reminders",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                if (showAddButton) {
                    Button(
                        onClick = onAddEventClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE6002A),
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Event", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Full-width Calendar
            FullCalendar(
                selectedDate = selectedDate,
                onDateSelected = onDateSelected,
                eventDates = eventDates
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Events List below calendar
            Column {
                Text(
                    text = "Upcoming Events",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                if (events.isEmpty()) {
                    Text(
                        text = "No upcoming events",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        events.take(5).forEach { event ->
                            EventListItem(event = event)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FullCalendar(
    selectedDate: Date,
    onDateSelected: (Date) -> Unit,
    eventDates: Set<Date> = emptySet()
) {
    // State for the displayed month
    var displayedMonth by remember { mutableStateOf(selectedDate) }
    
    val currentMonth = remember(displayedMonth) { 
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(displayedMonth) 
    }
    
    // Get today's date for comparison
    val today = remember { 
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.time
    }
    
    // Calculate calendar grid
    val calendarDays = remember(displayedMonth) {
        val cal = Calendar.getInstance()
        cal.time = displayedMonth
        cal.set(Calendar.DAY_OF_MONTH, 1)
        
        val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1 // 0 = Sunday
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        
        // Create list of days to display (including empty cells for alignment)
        buildList {
            // Add empty cells before first day
            repeat(firstDayOfWeek) { add(null) }
            // Add all days of the month
            repeat(daysInMonth) { add(it + 1) }
        }
    }
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Month/Year header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        val cal = Calendar.getInstance()
                        cal.time = displayedMonth
                        cal.add(Calendar.MONTH, -1)
                        displayedMonth = cal.time
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = "Previous month",
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Text(
                    text = currentMonth,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                IconButton(
                    onClick = {
                        val cal = Calendar.getInstance()
                        cal.time = displayedMonth
                        cal.add(Calendar.MONTH, 1)
                        displayedMonth = cal.time
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Next month",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Day of week headers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach { day ->
                    Text(
                        text = day,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Calendar grid with clickable days
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                calendarDays.chunked(7).forEach { week ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        week.forEach { day ->
                            if (day == null) {
                                // Empty cell
                                Spacer(modifier = Modifier.weight(1f).height(44.dp))
                            } else {
                                // Create date for this day
                                val dayDate = remember(displayedMonth, day) {
                                    val cal = Calendar.getInstance()
                                    cal.time = displayedMonth
                                    cal.set(Calendar.DAY_OF_MONTH, day)
                                    cal.set(Calendar.HOUR_OF_DAY, 0)
                                    cal.set(Calendar.MINUTE, 0)
                                    cal.set(Calendar.SECOND, 0)
                                    cal.set(Calendar.MILLISECOND, 0)
                                    cal.time
                                }
                                
                                // Check if this day is selected
                                val isSelected = remember(selectedDate, displayedMonth, day) {
                                    val selectedCal = Calendar.getInstance().apply { time = selectedDate }
                                    val displayCal = Calendar.getInstance().apply { time = displayedMonth }
                                    
                                    selectedCal.get(Calendar.YEAR) == displayCal.get(Calendar.YEAR) &&
                                    selectedCal.get(Calendar.MONTH) == displayCal.get(Calendar.MONTH) &&
                                    selectedCal.get(Calendar.DAY_OF_MONTH) == day
                                }
                                
                                // Check if this day is today
                                val isToday = remember(today, displayedMonth, day) {
                                    val todayCal = Calendar.getInstance().apply { time = today }
                                    val displayCal = Calendar.getInstance().apply { time = displayedMonth }
                                    
                                    todayCal.get(Calendar.YEAR) == displayCal.get(Calendar.YEAR) &&
                                    todayCal.get(Calendar.MONTH) == displayCal.get(Calendar.MONTH) &&
                                    todayCal.get(Calendar.DAY_OF_MONTH) == day
                                }
                                
                                // Check if this day has events
                                val hasEvent = remember(eventDates, dayDate) {
                                    eventDates.any { eventDate ->
                                        val eventCal = Calendar.getInstance().apply { time = eventDate }
                                        val dayCal = Calendar.getInstance().apply { time = dayDate }
                                        
                                        eventCal.get(Calendar.YEAR) == dayCal.get(Calendar.YEAR) &&
                                        eventCal.get(Calendar.MONTH) == dayCal.get(Calendar.MONTH) &&
                                        eventCal.get(Calendar.DAY_OF_MONTH) == dayCal.get(Calendar.DAY_OF_MONTH)
                                    }
                                }
                                
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            when {
                                                isSelected -> Color(0xFFE6002A)
                                                isToday -> Color(0xFF10B981) // Green for today
                                                hasEvent -> Color(0xFFE6002A).copy(alpha = 0.3f)
                                                else -> Color.Transparent
                                            }
                                        )
                                        .clickable {
                                            val cal = Calendar.getInstance()
                                            cal.time = displayedMonth
                                            cal.set(Calendar.DAY_OF_MONTH, day)
                                            onDateSelected(cal.time)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = day.toString(),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = when {
                                                isSelected -> MaterialTheme.colorScheme.onPrimary
                                                isToday -> Color.White
                                                else -> MaterialTheme.colorScheme.onSurface
                                            }
                                        )
                                        if (hasEvent && !isSelected && !isToday) {
                                            Box(
                                                modifier = Modifier
                                                    .size(4.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFFE6002A))
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

@Composable
fun SimpleDatePicker(
    selectedDate: Date,
    onDateSelected: (Date) -> Unit
) {
    // State for the displayed month (not necessarily the selected date's month)
    var displayedMonth by remember { mutableStateOf(selectedDate) }
    
    val currentMonth = remember(displayedMonth) { 
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(displayedMonth) 
    }
    
    // Calculate calendar grid
    val calendarDays = remember(displayedMonth) {
        val cal = Calendar.getInstance()
        cal.time = displayedMonth
        cal.set(Calendar.DAY_OF_MONTH, 1)
        
        val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1 // 0 = Sunday
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        
        // Create list of days to display (including empty cells for alignment)
        buildList {
            // Add empty cells before first day
            repeat(firstDayOfWeek) { add(null) }
            // Add all days of the month
            repeat(daysInMonth) { add(it + 1) }
        }
    }
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Month/Year header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        val cal = Calendar.getInstance()
                        cal.time = displayedMonth
                        cal.add(Calendar.MONTH, -1)
                        displayedMonth = cal.time
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = "Previous month",
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                Text(
                    text = currentMonth,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                
                IconButton(
                    onClick = {
                        val cal = Calendar.getInstance()
                        cal.time = displayedMonth
                        cal.add(Calendar.MONTH, 1)
                        displayedMonth = cal.time
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Next month",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Day of week headers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("S", "M", "T", "W", "T", "F", "S").forEach { day ->
                    Text(
                        text = day,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Calendar grid with clickable days
            Column {
                calendarDays.chunked(7).forEach { week ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        week.forEach { day ->
                            if (day == null) {
                                // Empty cell
                                Spacer(modifier = Modifier.weight(1f).height(36.dp))
                            } else {
                                // Check if this day is the selected date
                                val isSelected = remember(selectedDate, displayedMonth, day) {
                                    val selectedCal = Calendar.getInstance().apply { time = selectedDate }
                                    val displayCal = Calendar.getInstance().apply { time = displayedMonth }
                                    
                                    selectedCal.get(Calendar.YEAR) == displayCal.get(Calendar.YEAR) &&
                                    selectedCal.get(Calendar.MONTH) == displayCal.get(Calendar.MONTH) &&
                                    selectedCal.get(Calendar.DAY_OF_MONTH) == day
                                }
                                
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(36.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) Color(0xFFE6002A)
                                            else Color.Transparent
                                        )
                                        .clickable {
                                            val cal = Calendar.getInstance()
                                            cal.time = displayedMonth
                                            cal.set(Calendar.DAY_OF_MONTH, day)
                                            onDateSelected(cal.time)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = day.toString(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                               else MaterialTheme.colorScheme.onSurface
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

@Composable
fun EventListItem(event: CalendarEvent) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.CalendarToday,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = Color(0xFF3B82F6) // Blue color
            )
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                val formattedDate = try {
                    val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                    val outputFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                    val date = inputFormat.parse(event.eventDate)
                    if (date != null) outputFormat.format(date) else event.eventDate
                } catch (e: Exception) {
                    event.eventDate
                }
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Surface(
                shape = RoundedCornerShape(4.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                color = Color.Transparent
            ) {
                Text(
                    text = event.eventType.replaceFirstChar { it.uppercase() },
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEventDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, description: String, eventDate: String, eventType: String, branchId: String?) -> Unit,
    branches: List<Branch> = emptyList(),
    isAdmin: Boolean = false,
    isLoading: Boolean = false
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf<Date?>(null) }
    var selectedEventType by remember { mutableStateOf("reorder") }
    var selectedBranch by remember { mutableStateOf<Branch?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var expandedEventType by remember { mutableStateOf(false) }
    var expandedBranch by remember { mutableStateOf(false) }
    
    val eventTypes = listOf(
        "reorder" to "Reorder",
        "delivery" to "Delivery",
        "alert" to "Alert",
        "expiry" to "Expiry",
        "usage_spike" to "Usage Spike"
    )
    
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                // Header
                Text(
                    text = "Add Event",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Branch Selection (Admin only)
                if (isAdmin && branches.isNotEmpty()) {
                    Column {
                        Text(
                            text = "Branch *",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        ExposedDropdownMenuBox(
                            expanded = expandedBranch,
                            onExpandedChange = { expandedBranch = it }
                        ) {
                            OutlinedTextField(
                                value = selectedBranch?.name ?: "Select a branch",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = {
                                    Icon(
                                        imageVector = if (expandedBranch) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = null
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFFE6002A),
                                    focusedLabelColor = Color(0xFFE6002A),
                                    cursorColor = Color(0xFFE6002A)
                                )
                            )
                            
                            ExposedDropdownMenu(
                                expanded = expandedBranch,
                                onDismissRequest = { expandedBranch = false }
                            ) {
                                branches.forEach { branch ->
                                    DropdownMenuItem(
                                        text = { Text(branch.name + if (branch.location != null) " - ${branch.location}" else "") },
                                        onClick = {
                                            selectedBranch = branch
                                            expandedBranch = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                // Title Field
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFE6002A),
                        focusedLabelColor = Color(0xFFE6002A),
                        cursorColor = Color(0xFFE6002A)
                    )
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Description Field
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFE6002A),
                        focusedLabelColor = Color(0xFFE6002A),
                        cursorColor = Color(0xFFE6002A)
                    )
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Date Picker
                OutlinedTextField(
                    value = selectedDate?.let { 
                        SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(it)
                    } ?: "",
                    onValueChange = {},
                    label = { Text("Date *") },
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true },
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = "Pick date"
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFE6002A),
                        focusedLabelColor = Color(0xFFE6002A),
                        cursorColor = Color(0xFFE6002A)
                    )
                )
                
                if (showDatePicker) {
                    AlertDialog(
                        onDismissRequest = { showDatePicker = false },
                        title = { Text("Select Date") },
                        text = {
                            SimpleDatePicker(
                                selectedDate = selectedDate ?: Date(),
                                onDateSelected = { date ->
                                    selectedDate = date
                                    showDatePicker = false
                                }
                            )
                        },
                        confirmButton = {},
                        dismissButton = {}
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Event Type Dropdown
                Column {
                    Text(
                        text = "Type",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    ExposedDropdownMenuBox(
                        expanded = expandedEventType,
                        onExpandedChange = { expandedEventType = it }
                    ) {
                        OutlinedTextField(
                            value = eventTypes.find { it.first == selectedEventType }?.second ?: "Select event type",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = {
                                Icon(
                                    imageVector = if (expandedEventType) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = null
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFE6002A),
                                focusedLabelColor = Color(0xFFE6002A),
                                cursorColor = Color(0xFFE6002A)
                            )
                        )
                        
                        ExposedDropdownMenu(
                            expanded = expandedEventType,
                            onDismissRequest = { expandedEventType = false }
                        ) {
                            eventTypes.forEach { (value, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        selectedEventType = value
                                        expandedEventType = false
                                    }
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (title.isNotBlank() && selectedDate != null) {
                                val isoDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                                    .format(selectedDate!!)
                                onConfirm(
                                    title,
                                    description,
                                    isoDate,
                                    selectedEventType,
                                    selectedBranch?.id
                                )
                            }
                        },
                        enabled = title.isNotBlank() && selectedDate != null && !isLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE6002A),
                            contentColor = Color.White
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Add Event")
                        }
                    }
                }
            }
        }
    }
}
