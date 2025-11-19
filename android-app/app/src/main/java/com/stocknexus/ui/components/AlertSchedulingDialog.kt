package com.stocknexus.ui.components

import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Calendar

data class AlertSchedule(
    val frequencies: List<String>,
    val dailyTime: String? = null,
    val weeklyDay: Int? = null,
    val weeklyTime: String? = null,
    val monthlyDate: Int? = null,
    val monthlyTime: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertSchedulingDialog(
    onDismissRequest: () -> Unit,
    onSave: (AlertSchedule) -> Unit,
    initialSchedule: AlertSchedule? = null,
    title: String = "Stock Alert Schedule",
    description: String = "Choose how often you want to receive stock level alerts.",
    note: String = "Note: You will still receive immediate alerts when stock levels drop, regardless of your scheduled preferences."
) {
    var frequencies by remember { mutableStateOf(initialSchedule?.frequencies?.toSet() ?: setOf("daily")) }
    
    var dailyTime by remember { mutableStateOf(initialSchedule?.dailyTime ?: "09:00") }
    
    var weeklyDay by remember { mutableStateOf(initialSchedule?.weeklyDay ?: 1) } // 1 = Monday
    var weeklyTime by remember { mutableStateOf(initialSchedule?.weeklyTime ?: "09:00") }
    
    var monthlyDate by remember { mutableStateOf(initialSchedule?.monthlyDate ?: 1) }
    var monthlyTime by remember { mutableStateOf(initialSchedule?.monthlyTime ?: "09:00") }

    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val weekDays = listOf(
        0 to "Sunday", 1 to "Monday", 2 to "Tuesday", 3 to "Wednesday",
        4 to "Thursday", 5 to "Friday", 6 to "Saturday"
    )

    val monthDays = (1..31).toList()

    fun showTimePicker(initialTime: String, onTimeSelected: (String) -> Unit) {
        val parts = initialTime.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 9
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0

        TimePickerDialog(
            context,
            { _, h, m ->
                onTimeSelected(String.format("%02d:%02d", h, m))
            },
            hour,
            minute,
            true // 24 hour format
        ).show()
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                Text(text = title, style = MaterialTheme.typography.titleMedium)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(scrollState)
                    .fillMaxWidth()
            ) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text(
                    text = "Alert Frequency",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Daily
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Checkbox(
                        checked = frequencies.contains("daily"),
                        onCheckedChange = { checked ->
                            frequencies = if (checked) frequencies + "daily" else frequencies - "daily"
                        }
                    )
                    Column(modifier = Modifier.padding(start = 8.dp)) {
                        Text("Daily", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text("Get a daily summary", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // Weekly
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Checkbox(
                        checked = frequencies.contains("weekly"),
                        onCheckedChange = { checked ->
                            frequencies = if (checked) frequencies + "weekly" else frequencies - "weekly"
                        }
                    )
                    Column(modifier = Modifier.padding(start = 8.dp)) {
                        Text("Weekly", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text("Get a weekly summary", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // Monthly
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Checkbox(
                        checked = frequencies.contains("monthly"),
                        onCheckedChange = { checked ->
                            frequencies = if (checked) frequencies + "monthly" else frequencies - "monthly"
                        }
                    )
                    Column(modifier = Modifier.padding(start = 8.dp)) {
                        Text("Monthly", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text("Get a monthly summary", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 16.dp))

                // Daily Settings
                if (frequencies.contains("daily")) {
                    Text("Daily Settings", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(bottom = 8.dp))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = dailyTime,
                            onValueChange = {},
                            label = { Text("Daily Alert Time") },
                            readOnly = true,
                            trailingIcon = { Icon(Icons.Default.Schedule, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = false,
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        // Overlay a clickable box to capture clicks since TextField is disabled
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { showTimePicker(dailyTime) { dailyTime = it } }
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Weekly Settings
                if (frequencies.contains("weekly")) {
                    Text("Weekly Settings", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(bottom = 8.dp))
                    
                    // Day of Week Selector (Simple Dropdown implementation)
                    var expanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = weekDays.find { it.first == weeklyDay }?.second ?: "Monday",
                            onValueChange = {},
                            label = { Text("Day of the Week") },
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = false,
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        Box(modifier = Modifier.matchParentSize().clickable { expanded = true })
                        
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            weekDays.forEach { day ->
                                DropdownMenuItem(
                                    text = { Text(day.second) },
                                    onClick = {
                                        weeklyDay = day.first
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = weeklyTime,
                            onValueChange = {},
                            label = { Text("Weekly Alert Time") },
                            readOnly = true,
                            trailingIcon = { Icon(Icons.Default.Schedule, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = false,
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        Box(modifier = Modifier.matchParentSize().clickable { showTimePicker(weeklyTime) { weeklyTime = it } })
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Monthly Settings
                if (frequencies.contains("monthly")) {
                    Text("Monthly Settings", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(bottom = 8.dp))
                    
                    // Day of Month Selector
                    var expanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = "$monthlyDate" + getOrdinalSuffix(monthlyDate),
                            onValueChange = {},
                            label = { Text("Day of the Month") },
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = false,
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        Box(modifier = Modifier.matchParentSize().clickable { expanded = true })

                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.heightIn(max = 300.dp)
                        ) {
                            monthDays.forEach { day ->
                                DropdownMenuItem(
                                    text = { Text("$day" + getOrdinalSuffix(day)) },
                                    onClick = {
                                        monthlyDate = day
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = monthlyTime,
                            onValueChange = {},
                            label = { Text("Monthly Alert Time") },
                            readOnly = true,
                            trailingIcon = { Icon(Icons.Default.Schedule, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = false,
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        Box(modifier = Modifier.matchParentSize().clickable { showTimePicker(monthlyTime) { monthlyTime = it } })
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (frequencies.isEmpty()) {
                        // Show error or just don't save? Web app shows alert.
                        // For now, just return.
                        return@Button
                    }
                    onSave(
                        AlertSchedule(
                            frequencies = frequencies.toList(),
                            dailyTime = dailyTime,
                            weeklyDay = weeklyDay,
                            weeklyTime = weeklyTime,
                            monthlyDate = monthlyDate,
                            monthlyTime = monthlyTime
                        )
                    )
                }
            ) {
                Text("Save Schedule")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel")
            }
        }
    )
}

fun getOrdinalSuffix(num: Int): String {
    val j = num % 10
    val k = num % 100
    if (j == 1 && k != 11) return "st"
    if (j == 2 && k != 12) return "nd"
    if (j == 3 && k != 13) return "rd"
    return "th"
}
