package com.ims.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.ims.android.data.api.ApiClient
import com.ims.android.data.model.CreateMoveoutListRequest
import com.ims.android.data.model.MoveoutItemRequest
import com.ims.android.data.repository.InventoryRepository
import com.ims.android.data.repository.MoveoutRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class MoveoutListItemData(
    val itemId: String,
    val itemName: String,
    val category: String,
    val currentQuantity: Int,
    var requestingQuantity: Int = 1
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GenerateMoveoutDialog(
    onDismiss: () -> Unit,
    inventoryRepository: InventoryRepository,
    moveoutRepository: MoveoutRepository,
    userName: String,
    apiClient: ApiClient,
    onSuccess: (itemCount: Int) -> Unit
) {
    var availableStock by remember { mutableStateOf<List<ApiClient.StockItem>>(emptyList()) }
    var selectedForDropdown by remember { mutableStateOf<List<ApiClient.StockItem>>(emptyList()) }
    var tableItems by remember { mutableStateOf<List<MoveoutListItemData>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var searchTxt by remember { mutableStateOf("") }
    
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(Unit) {
        scope.launch {
            inventoryRepository.getStockData().onSuccess { stocks ->
                availableStock = stocks.filter { (it.current_quantity ?: 0) > 0 }
                android.util.Log.d("GenerateMoveout", "Loaded ${availableStock.size} stocks")
                availableStock.forEach { stock ->
                    android.util.Log.d("GenerateMoveout", "Stock: itemId=${stock.item_id}, itemName=${stock.items?.name}, qty=${stock.current_quantity}")
                }
                isLoading = false
            }.onFailure {
                android.util.Log.e("GenerateMoveout", "Failed to load: ${it.message}")
                errorMsg = "Failed to load: ${it.message}"
                isLoading = false
            }
        }
    }

    val filtered = remember(availableStock, searchTxt, tableItems, selectedForDropdown) {
        val addedIds = tableItems.map { it.itemId }.toSet()
        val selectedIds = selectedForDropdown.map { it.item_id ?: "" }.toSet()
        val result = if (searchTxt.isEmpty()) {
            emptyList()
        } else {
            availableStock.filter { stock ->
                val itemName = stock.items?.name ?: ""
                val matches = itemName.contains(searchTxt, ignoreCase = true)
                val notAdded = (stock.item_id ?: "") !in addedIds
                val notSelected = (stock.item_id ?: "") !in selectedIds
                matches && notAdded && notSelected
            }
        }
        android.util.Log.d("GenerateMoveout", "Search: '$searchTxt', Filtered: ${result.size} items")
        result
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .heightIn(max = 600.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Box(Modifier.fillMaxSize()) {
                Column(Modifier.fillMaxWidth().padding(20.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Generate Moveout List", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Close") }
                }

                Spacer(Modifier.height(20.dp))

                if (isLoading) {
                    Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyColumn(
                        Modifier
                            .weight(1f, fill = false)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Column {
                                Text("Select Items", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = searchTxt,
                                    onValueChange = { 
                                        searchTxt = it
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    placeholder = { Text("Search Items") },
                                    leadingIcon = { Icon(Icons.Default.Search, "Search") },
                                    trailingIcon = {
                                        if (searchTxt.isNotEmpty()) {
                                            IconButton(onClick = { 
                                                searchTxt = ""
                                            }) {
                                                Icon(Icons.Default.Close, "Clear")
                                            }
                                        }
                                    },
                                    singleLine = true
                                )

                                // Only show selected chips when search is empty
                                if (selectedForDropdown.isNotEmpty() && searchTxt.isEmpty()) {
                                    Spacer(Modifier.height(8.dp))
                                    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        selectedForDropdown.forEach { stock ->
                                            FilterChip(
                                                selected = true,
                                                onClick = { selectedForDropdown = selectedForDropdown - stock },
                                                label = { Text("${stock.items?.name} (${stock.current_quantity})", fontSize = 12.sp) },
                                                trailingIcon = { Icon(Icons.Default.Close, "Remove", Modifier.size(16.dp)) }
                                            )
                                        }
                                    }
                                }

                                if (searchTxt.isNotEmpty() && filtered.isNotEmpty()) {
                                    Spacer(Modifier.height(4.dp))
                                    Card(
                                        Modifier.fillMaxWidth().heightIn(max = 250.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        LazyColumn {
                                            items(filtered) { stock ->
                                                val selected = selectedForDropdown.contains(stock)
                                                Surface(
                                                    Modifier.fillMaxWidth().clickable {
                                                        selectedForDropdown = if (selected) selectedForDropdown - stock else selectedForDropdown + stock
                                                        searchTxt = "" // Hide dropdown after selection
                                                    }.padding(12.dp),
                                                    color = Color.Transparent
                                                ) {
                                                    Row(
                                                        Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text("${stock.items?.name} (Current: ${stock.current_quantity})", Modifier.weight(1f))
                                                    }
                                                }
                                                if (stock != filtered.last()) Divider()
                                            }
                                        }
                                    }
                                }

                                Spacer(Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        selectedForDropdown.forEach { stock ->
                                            tableItems = tableItems + MoveoutListItemData(
                                                stock.item_id ?: "",
                                                stock.items?.name ?: "",
                                                stock.items?.category ?: "",
                                                stock.current_quantity ?: 0,
                                                1
                                            )
                                        }
                                        selectedForDropdown = emptyList()
                                        searchTxt = ""
                                    },
                                    Modifier.fillMaxWidth(),
                                    enabled = selectedForDropdown.isNotEmpty(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFE6002A),
                                        contentColor = Color.White
                                    )
                                ) {
                                    Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Add to List")
                                }
                            }
                        }

                        if (tableItems.isNotEmpty()) {
                            item {
                                Text("Moveout List Items", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(8.dp))
                            }
                            
                            items(tableItems.size) { idx ->
                                val item = tableItems[idx]
                                var qtyText by remember { mutableStateOf(item.requestingQuantity.toString()) }
                                var qtyError by remember { mutableStateOf(false) }
                                
                                Card(
                                    Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(Modifier.fillMaxWidth().padding(12.dp)) {
                                        Row(
                                            Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                item.itemName,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.weight(1f)
                                            )
                                            IconButton(onClick = { 
                                                tableItems = tableItems.filterIndexed { i, _ -> i != idx } 
                                            }) {
                                                Icon(Icons.Default.Delete, "Remove", tint = MaterialTheme.colorScheme.error)
                                            }
                                        }
                                        
                                        Spacer(Modifier.height(12.dp))
                                        
                                        Row(
                                            Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                                        ) {
                                            Column(Modifier.weight(1f)) {
                                                Text(
                                                    "Current Quantity",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Spacer(Modifier.height(4.dp))
                                                Text(
                                                    "${item.currentQuantity}",
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                            
                                            Column(Modifier.weight(1f)) {
                                                Text(
                                                    "Requesting Quantity",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Spacer(Modifier.height(4.dp))
                                                OutlinedTextField(
                                                    value = qtyText,
                                                    onValueChange = { newVal ->
                                                        qtyText = newVal
                                                        val qty = newVal.toIntOrNull()
                                                        if (qty != null) {
                                                            qtyError = qty > item.currentQuantity || qty < 1
                                                            tableItems = tableItems.toMutableList().apply {
                                                                this[idx] = item.copy(requestingQuantity = qty)
                                                            }
                                                        } else {
                                                            qtyError = newVal.isNotEmpty()
                                                        }
                                                    },
                                                    Modifier.fillMaxWidth(),
                                                    singleLine = true,
                                                    isError = qtyError,
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                                                )
                                                if (qtyError) {
                                                    Text(
                                                        "Max: ${item.currentQuantity}",
                                                        color = MaterialTheme.colorScheme.error,
                                                        fontSize = 11.sp,
                                                        modifier = Modifier.padding(top = 4.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                
                                if (idx < tableItems.size - 1) {
                                    Spacer(Modifier.height(4.dp))
                                    Divider(Modifier.fillMaxWidth(), thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
                                    Spacer(Modifier.height(4.dp))
                                }
                            }
                        }

                        if (errorMsg != null) {
                            item {
                                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                                    Text(errorMsg ?: "", color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(12.dp))
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = onDismiss, Modifier.weight(1f), enabled = !isSaving) { Text("Cancel") }
                        Button(
                            onClick = {
                                if (tableItems.isEmpty()) {
                                    errorMsg = "Add items"
                                    return@Button
                                }
                                val hasErr = tableItems.any { it.requestingQuantity > it.currentQuantity || it.requestingQuantity < 1 }
                                if (hasErr) {
                                    errorMsg = "Fix errors"
                                    return@Button
                                }
                                isSaving = true
                                scope.launch {
                                    try {
                                        val req = CreateMoveoutListRequest(
                                            "Moveout List",
                                            "Generated",
                                            tableItems.map { 
                                                MoveoutItemRequest(it.itemId, it.itemName, it.currentQuantity, it.requestingQuantity, it.category)
                                            }
                                        )
                                        moveoutRepository.createMoveoutList(req).onSuccess {
                                            android.util.Log.d("GenerateMoveout", "✅ Moveout list created successfully")
                                            
                                            // Send FCM notification via backend broadcast endpoint
                                            try {
                                                val itemsText = tableItems.joinToString(", ") { "${it.itemName} × ${it.requestingQuantity}" }
                                                apiClient.broadcastNotificationToAllStaff(
                                                    type = "moveout",
                                                    title = "📦 New Moveout Request",
                                                    message = itemsText,
                                                    creatorName = userName
                                                )
                                                android.util.Log.d("GenerateMoveout", "📢 FCM broadcast sent for moveout by $userName")
                                            } catch (e: Exception) {
                                                android.util.Log.e("GenerateMoveout", "Failed to send FCM broadcast: ${e.message}")
                                                // Don't fail the whole operation if notification fails
                                            }
                                            
                                            onDismiss()
                                            onSuccess(tableItems.size)
                                        }.onFailure {
                                            errorMsg = "Failed: ${it.message}"
                                            isSaving = false
                                        }
                                    } catch (e: Exception) {
                                        errorMsg = "Error: ${e.message}"
                                        isSaving = false
                                    }
                                }
                            },
                            Modifier.weight(1f),
                            enabled = !isSaving && tableItems.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFE6002A),
                                contentColor = Color.White
                            )
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                            } else {
                                Text("Generate")
                            }
                        }
                    }
                }
            }
            }
        }
    }
}
