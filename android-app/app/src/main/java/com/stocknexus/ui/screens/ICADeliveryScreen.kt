package com.stocknexus.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.stocknexus.data.repository.ICADeliveryRepository
import kotlinx.coroutines.launch

data class ICADeliveryEntry(
    val type: String,
    var amount: String = "",
    var timeOfDay: String = "Morning"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ICADeliveryDialog(
    onDismiss: () -> Unit,
    icaDeliveryRepository: ICADeliveryRepository,
    userName: String
) {
    var entries by remember {
        mutableStateOf(
            listOf(
                ICADeliveryEntry("Salmon and Rolls"),
                ICADeliveryEntry("Combo"),
                ICADeliveryEntry("Salmon and Avocado Rolls"),
                ICADeliveryEntry("Vegan Combo"),
                ICADeliveryEntry("Goma Wakame")
            )
        )
    }
    
    var showConfirmation by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val scope = rememberCoroutineScope()
    
    // Preset configurations - Triple of (label, amounts, timeOfDay)
    val presets = listOf(
        Triple("Morning : 5 / 5 / 1 / 1 / 4w", listOf("5", "5", "1", "1", "4"), "Morning"),
        Triple("Afternoon : 5 / 5 / 1 / 1w", listOf("5", "5", "1", "1", "1"), "Afternoon"),
        Triple("Morning : 10 / 10 / 1 / 1 / 4w", listOf("10", "10", "1", "1", "4"), "Morning")
    )
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1e293b).copy(alpha = 0.95f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ICA Delivery",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Quick Presets
                Text(
                    text = "Quick Presets",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presets.forEach { preset ->
                        val (label, amounts, timeOfDay) = preset
                        OutlinedButton(
                            onClick = {
                                entries = entries.mapIndexed { index, entry ->
                                    entry.copy(
                                        amount = amounts[index],
                                        timeOfDay = timeOfDay
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color.White
                            ),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                width = 1.dp,
                                brush = androidx.compose.ui.graphics.SolidColor(Color.White.copy(alpha = 0.3f))
                            )
                        ) {
                            Text(label)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Entries form
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    entries.forEachIndexed { index, entry ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White.copy(alpha = 0.1f)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = entry.type,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                
                                // Amount input
                                OutlinedTextField(
                                    value = entry.amount,
                                    onValueChange = { newValue ->
                                        entries = entries.toMutableList().apply {
                                            this[index] = entry.copy(amount = newValue)
                                        }
                                    },
                                    label = { Text("Amount", color = Color.White.copy(alpha = 0.7f)) },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Color.White.copy(alpha = 0.5f),
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                        cursorColor = Color.White
                                    ),
                                    singleLine = true
                                )
                                
                                // Time of day selector
                                var expanded by remember { mutableStateOf(false) }
                                
                                ExposedDropdownMenuBox(
                                    expanded = expanded,
                                    onExpandedChange = { expanded = !expanded }
                                ) {
                                    OutlinedTextField(
                                        value = entry.timeOfDay,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Time of Day", color = Color.White.copy(alpha = 0.7f)) },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .menuAnchor(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedBorderColor = Color.White.copy(alpha = 0.5f),
                                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                                        )
                                    )
                                    
                                    ExposedDropdownMenu(
                                        expanded = expanded,
                                        onDismissRequest = { expanded = false },
                                        modifier = Modifier.background(Color(0xFF1e293b))
                                    ) {
                                        listOf("Morning", "Afternoon").forEach { time ->
                                            DropdownMenuItem(
                                                text = { Text(time, color = Color.White) },
                                                onClick = {
                                                    // Update all entries to the same time
                                                    entries = entries.map { it.copy(timeOfDay = time) }
                                                    expanded = false
                                                },
                                                colors = MenuDefaults.itemColors(
                                                    textColor = Color.White
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Error message
                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage!!,
                        color = Color(0xFFEF4444),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        ),
                        enabled = !isLoading
                    ) {
                        Text("Cancel")
                    }
                    
                    Button(
                        onClick = {
                            // Validate at least one entry is filled
                            val hasValidEntry = entries.any { it.amount.isNotBlank() }
                            if (!hasValidEntry) {
                                errorMessage = "Please fill in at least one entry"
                                return@Button
                            }
                            errorMessage = null
                            showConfirmation = true
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFD32F2F)
                        ),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Submit", color = Color.White)
                        }
                    }
                }
            }
        }
    }
    
    // Confirmation Dialog
    if (showConfirmation) {
        AlertDialog(
            onDismissRequest = { showConfirmation = false },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            isLoading = true
                            errorMessage = null
                            
                            try {
                                val validEntries = entries.filter { it.amount.isNotBlank() }
                                val result = icaDeliveryRepository.submitICADelivery(
                                    userName = userName,
                                    entries = validEntries
                                )
                                
                                if (result.isSuccess) {
                                    showConfirmation = false
                                    onDismiss()
                                } else {
                                    errorMessage = result.exceptionOrNull()?.message ?: "Failed to submit"
                                    showConfirmation = false
                                }
                            } catch (e: Exception) {
                                errorMessage = e.message ?: "An error occurred"
                                showConfirmation = false
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF16a34a)
                    ),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Confirm & Submit", color = Color.White)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showConfirmation = false },
                    enabled = !isLoading
                ) {
                    Text("Cancel", color = Color.White)
                }
            },
            title = { Text("Confirm ICA Delivery Order") },
            text = {
                Column {
                    Text("You are about to submit the following order:")
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            entries.filter { it.amount.isNotBlank() }.forEach { entry ->
                                Text(
                                    text = "${entry.type}: ${entry.amount} units - ${entry.timeOfDay}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Submitted by: $userName",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            containerColor = Color(0xFF1e293b).copy(alpha = 0.95f),
            titleContentColor = Color.White,
            textContentColor = Color.White
        )
    }
}
