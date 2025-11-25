package com.ims.android.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ims.android.data.model.MoveoutList
import com.ims.android.data.model.MoveoutItem
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MoveoutListItemSimple(
    moveoutList: MoveoutList,
    onItemClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onItemClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = moveoutList.title ?: "Moveout List",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                val dateMillis = moveoutList.createdAt?.toLongOrNull() ?: System.currentTimeMillis()
                Text(
                    text = dateFormat.format(Date(dateMillis)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${moveoutList.items.size} items",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Status badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        when (moveoutList.status) {
                            "draft", "active" -> Color(0xFFE6002A)
                            "completed" -> Color(0xFF16a34a)
                            else -> MaterialTheme.colorScheme.error
                        }
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = when (moveoutList.status) {
                        "draft", "active" -> "Pending"
                        "completed" -> "Completed"
                        else -> moveoutList.status
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun MoveoutItemsDialog(
    moveoutList: MoveoutList,
    onDismiss: () -> Unit,
    onProcessItem: (itemId: String, quantity: Int) -> Unit,
    processingItemId: String? = null
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.8f),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = moveoutList.title ?: "Moveout List",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                        val dateMillis = moveoutList.createdAt?.toLongOrNull() ?: System.currentTimeMillis()
                        Text(
                            text = dateFormat.format(Date(dateMillis)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
                
                Divider()
                
                // Table Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Item Name",
                        modifier = Modifier.weight(2f),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Requesting Quantity",
                        modifier = Modifier.weight(1.5f),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Action",
                        modifier = Modifier.weight(1.2f),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Divider()
                
                // Items List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(moveoutList.items) { item ->
                        MoveoutItemRow(
                            item = item,
                            onProcessItem = { onProcessItem(item.itemId, item.requestAmount) },
                            isProcessing = processingItemId == item.itemId
                        )
                        Divider()
                    }
                }
            }
        }
    }
}

@Composable
fun MoveoutItemRow(
    item: MoveoutItem,
    onProcessItem: () -> Unit,
    isProcessing: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Item Name
        Text(
            text = item.itemName,
            modifier = Modifier.weight(2f),
            style = MaterialTheme.typography.bodyMedium
        )
        
        // Requesting Quantity
        Text(
            text = item.requestAmount.toString(),
            modifier = Modifier.weight(1.5f),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
        
        // Action Button
        Box(
            modifier = Modifier.weight(1.2f),
            contentAlignment = Alignment.CenterStart
        ) {
            if (item.completed || item.status == "completed") {
                Text(
                    text = "Completed",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF16a34a),
                    fontWeight = FontWeight.Medium
                )
            } else {
                OutlinedButton(
                    onClick = onProcessItem,
                    enabled = !isProcessing,
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color.White,
                        disabledContainerColor = Color.Transparent,
                        disabledContentColor = Color.White.copy(alpha = 0.5f)
                    ),
                    border = BorderStroke(1.dp, Color.White),
                    modifier = Modifier.height(36.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Text("Done", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
fun CompletedMoveoutItemsDialog(
    moveoutList: MoveoutList,
    onDismiss: () -> Unit
) {
    var showCompletionSummary by remember { mutableStateOf(false) }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.8f),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = moveoutList.title ?: "Moveout List",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                        val dateMillis = moveoutList.createdAt?.toLongOrNull() ?: System.currentTimeMillis()
                        Text(
                            text = dateFormat.format(Date(dateMillis)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
                
                Divider()
                
                // Completion Summary Toggle
                if (moveoutList.items.isNotEmpty() && moveoutList.items.all { it.status == "completed" || it.completed }) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showCompletionSummary = !showCompletionSummary }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (showCompletionSummary) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = Color(0xFF16a34a),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "View Completion Summary",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF16a34a)
                        )
                    }
                    
                    // Expanded Completion Summary
                    if (showCompletionSummary) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .background(
                                    color = Color(0xFFF0FDF4),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "✅",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Completion Summary",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF166534)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            moveoutList.items.forEach { item ->
                                if (item.status == "completed" || item.completed) {
                                    val processedBy = item.processedBy ?: item.completedByName ?: "Unknown"
                                    val processedAt = item.processedAt ?: item.completedAt
                                    
                                    val dateTimeText = if (processedAt != null) {
                                        try {
                                            val dateFormat = SimpleDateFormat("MM/dd/yyyy 'at' hh:mm:ss a", Locale.getDefault())
                                            val date = Date(processedAt.toLongOrNull() ?: System.currentTimeMillis())
                                            dateFormat.format(date)
                                        } catch (e: Exception) {
                                            processedAt
                                        }
                                    } else {
                                        "Unknown date"
                                    }
                                    
                                    Text(
                                        text = "${item.itemName} - Completed by $processedBy on $dateTimeText",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF15803d)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                            }
                        }
                    }
                    
                    Divider()
                }
                
                // Table Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Item Name",
                        modifier = Modifier.weight(2f),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Requesting Quantity",
                        modifier = Modifier.weight(1.5f),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Status",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Divider()
                
                // Items List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(moveoutList.items) { item ->
                        CompletedMoveoutItemRow(item = item)
                        Divider()
                    }
                }
            }
        }
    }
}

@Composable
fun CompletedMoveoutItemRow(item: MoveoutItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Item Name
        Text(
            text = item.itemName,
            modifier = Modifier.weight(2f),
            style = MaterialTheme.typography.bodyMedium
        )
        
        // Requesting Quantity
        Text(
            text = item.requestAmount.toString(),
            modifier = Modifier.weight(1.5f),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
        
        // Status
        Box(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "Completed",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF16a34a),
                fontWeight = FontWeight.Medium
            )
        }
    }
}
