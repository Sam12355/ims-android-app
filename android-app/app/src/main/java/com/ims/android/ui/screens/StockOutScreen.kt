package com.ims.android.ui.screens

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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ims.android.data.api.ApiClient
import com.ims.android.data.repository.InventoryRepository
import com.ims.android.ui.components.Base64Image
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockOutScreen(
    inventoryRepository: InventoryRepository,
    modifier: Modifier = Modifier
) {
    var stockItems by remember { mutableStateOf<List<ApiClient.StockItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var searchTerm by remember { mutableStateOf("") }
    var filterType by remember { mutableStateOf("all") } // "all", "low", "critical"
    var categorySearchTerms by remember { mutableStateOf(mapOf<String, String>()) }
    
    // Dialog states
    var showRemoveDialog by remember { mutableStateOf(false) }
    var selectedItem by remember { mutableStateOf<ApiClient.StockItem?>(null) }
    var quantity by remember { mutableStateOf("") }
    var unitType by remember { mutableStateOf("base") } // "base" or "packaging"
    var reason by remember { mutableStateOf("") }
    var isRemoving by remember { mutableStateOf(false) }
    
    // Quick action states
    var quickActionItem by remember { mutableStateOf<ApiClient.StockItem?>(null) }
    var quickQuantity by remember { mutableStateOf("") }
    var quickUnitType by remember { mutableStateOf("base") }
    var isQuickActionLoading by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Load stock data
    fun loadStockData() {
        scope.launch {
            loading = true
            try {
                val result = inventoryRepository.getStockData()
                result.onSuccess { data ->
                    android.util.Log.d("StockOutScreen", "Loaded ${data.size} items")
                    data.forEach { item ->
                        android.util.Log.d("StockOutScreen", "Item: ${item.items?.name}, image_url present: ${!item.items?.image_url.isNullOrBlank()}, image_url length: ${item.items?.image_url?.length}")
                    }
                    stockItems = data
                    if (data.isEmpty()) {
                        // Try to initialize
                        inventoryRepository.initializeStock().onSuccess { initResult ->
                            if ((initResult.initialized ?: 0) > 0) {
                                snackbarHostState.showSnackbar("Initialized stock for ${initResult.initialized} items")
                                loadStockData() // Reload
                            }
                        }
                    }
                }.onFailure { error ->
                    snackbarHostState.showSnackbar("Error: ${error.message}")
                }
            } finally {
                loading = false
            }
        }
    }
    
    LaunchedEffect(Unit) {
        loadStockData()
    }
    
    // Filter and search logic
    val criticalStockItems = stockItems.filter { item ->
        val threshold = item.items?.threshold_level ?: 0
        item.current_quantity <= threshold * 0.5
    }
    
    val lowStockItems = stockItems.filter { item ->
        val threshold = item.items?.threshold_level ?: 0
        item.current_quantity > threshold * 0.5 && item.current_quantity <= threshold
    }
    
    val displayItems = when (filterType) {
        "low" -> lowStockItems
        "critical" -> criticalStockItems
        else -> stockItems
    }
    
    val filteredStockItems = displayItems.filter { item ->
        val name = item.items?.name ?: ""
        val category = item.items?.category ?: ""
        name.contains(searchTerm, ignoreCase = true) || category.contains(searchTerm, ignoreCase = true)
    }
    
    // Group by category
    val groupedItems = filteredStockItems.groupBy { it.items?.category ?: "Uncategorized" }
        .mapValues { (category, items) ->
            val categorySearch = categorySearchTerms[category] ?: ""
            items.filter { item ->
                categorySearch.isEmpty() || item.items?.name?.contains(categorySearch, ignoreCase = true) == true
            }
        }
        .filter { it.value.isNotEmpty() }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with Remove Stock button
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Stock Out",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Button(
                        onClick = { showRemoveDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF0000),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            Icons.Default.Remove,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Remove Stock")
                    }
                }
            }
            
            // Summary Cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SummaryCard(
                        title = "Total Items",
                        count = stockItems.size,
                        icon = Icons.Default.Inventory,
                        color = MaterialTheme.colorScheme.primary,
                        onClick = { filterType = "all" },
                        modifier = Modifier.weight(1f)
                    )
                    SummaryCard(
                        title = "Low Stock",
                        count = lowStockItems.size,
                        icon = Icons.Default.Warning,
                        color = Color(0xFFFF8800),
                        onClick = { filterType = "low" },
                        modifier = Modifier.weight(1f)
                    )
                    SummaryCard(
                        title = "Critical",
                        count = criticalStockItems.size,
                        icon = Icons.Default.Error,
                        color = Color(0xFFE53E3E),
                        onClick = { filterType = "critical" },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            // Search bar
            item {
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = when (filterType) {
                                "low" -> "Low Stock Items"
                                "critical" -> "Critical Stock Items"
                                else -> "Current Stock Levels"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = searchTerm,
                            onValueChange = { searchTerm = it },
                            placeholder = { Text("Search items by name or category...") },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
                        )
                    }
                }
            }
            
            // Category groups
            groupedItems.forEach { (category, items) ->
                item {
                    CategorySection(
                        category = category,
                        items = items,
                        searchTerm = categorySearchTerms[category] ?: "",
                        onSearchChange = { newSearch ->
                            categorySearchTerms = categorySearchTerms + (category to newSearch)
                        },
                        quickActionItem = quickActionItem,
                        quickQuantity = quickQuantity,
                        quickUnitType = quickUnitType,
                        isQuickActionLoading = isQuickActionLoading,
                        onQuickQuantityChange = { quickQuantity = it },
                        onQuickUnitTypeChange = { quickUnitType = it },
                        onQuickActionClick = { item ->
                            quickActionItem = item
                            quickQuantity = ""
                            quickUnitType = "base"
                        },
                        onQuickActionCancel = {
                            quickActionItem = null
                            quickQuantity = ""
                            quickUnitType = "base"
                        },
                        onQuickStockOut = { item ->
                            scope.launch {
                                if (quickQuantity.isBlank()) {
                                    snackbarHostState.showSnackbar("Please enter a quantity")
                                    return@launch
                                }
                                
                                val qty = quickQuantity.toIntOrNull()
                                if (qty == null || qty <= 0) {
                                    snackbarHostState.showSnackbar("Please enter a valid quantity")
                                    return@launch
                                }
                                
                                isQuickActionLoading = true
                                
                                var quantityInBaseUnits = qty
                                if (quickUnitType == "packaging" && item.items?.units_per_package != null) {
                                    val unitsPerPackage = item.items.units_per_package ?: 1
                                    quantityInBaseUnits = qty * unitsPerPackage
                                }
                                
                                if (quantityInBaseUnits > item.current_quantity) {
                                    snackbarHostState.showSnackbar("Insufficient stock!")
                                    isQuickActionLoading = false
                                    return@launch
                                }
                                
                                val unitLabel = if (quickUnitType == "packaging") 
                                    item.items?.packaging_unit ?: "package" 
                                else 
                                    item.items?.base_unit ?: "piece"
                                
                                val finalQuantityInBaseUnits = quantityInBaseUnits
                                inventoryRepository.updateStockQuantity(
                                    itemId = item.item_id ?: "",
                                    movementType = "out",
                                    quantity = finalQuantityInBaseUnits,
                                    reason = "Quick stock out",
                                    unitType = quickUnitType,
                                    unitQuantity = qty,
                                    unitLabel = unitLabel
                                ).onSuccess {
                                    snackbarHostState.showSnackbar("Removed $qty $unitLabel")
                                    quickActionItem = null
                                    quickQuantity = ""
                                    quickUnitType = "base"
                                    loadStockData()
                                }.onFailure { error ->
                                    snackbarHostState.showSnackbar("Error: ${error.message}")
                                }
                                
                                isQuickActionLoading = false
                            }
                        }
                    )
                }
            }
            
            if (loading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
            
            if (!loading && groupedItems.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Box(modifier = Modifier.padding(32.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = when (filterType) {
                                    "low" -> "No low stock items"
                                    "critical" -> "No critical stock items"
                                    else -> "No stock items found"
                                },
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Remove Stock Dialog
    if (showRemoveDialog) {
        RemoveStockDialog(
            stockItems = stockItems,
            selectedItem = selectedItem,
            quantity = quantity,
            unitType = unitType,
            reason = reason,
            isRemoving = isRemoving,
            onSelectItem = { selectedItem = it; quantity = ""; unitType = "base" },
            onQuantityChange = { quantity = it },
            onUnitTypeChange = { unitType = it },
            onReasonChange = { reason = it },
            onDismiss = {
                showRemoveDialog = false
                selectedItem = null
                quantity = ""
                unitType = "base"
                reason = ""
            },
            onConfirm = {
                scope.launch {
                    if (selectedItem == null || quantity.isBlank()) {
                        snackbarHostState.showSnackbar("Please select an item and enter quantity")
                        return@launch
                    }
                    
                    val qty = quantity.toIntOrNull()
                    if (qty == null || qty <= 0) {
                        snackbarHostState.showSnackbar("Please enter a valid quantity")
                        return@launch
                    }
                    
                    isRemoving = true
                    
                    val unitsPerPackage = selectedItem?.items?.units_per_package ?: 1
                    val quantityInBaseUnits = if (unitType == "packaging" && unitsPerPackage > 1) {
                        qty * unitsPerPackage
                    } else {
                        qty
                    }
                    
                    if (quantityInBaseUnits > (selectedItem?.current_quantity ?: 0)) {
                        snackbarHostState.showSnackbar("Insufficient stock!")
                        isRemoving = false
                        return@launch
                    }
                    
                    val unitLabel = if (unitType == "packaging") 
                        selectedItem?.items?.packaging_unit ?: "package" 
                    else 
                        selectedItem?.items?.base_unit ?: "piece"
                    
                    inventoryRepository.updateStockQuantity(
                        itemId = selectedItem?.item_id ?: "",
                        movementType = "out",
                        quantity = quantityInBaseUnits,
                        reason = reason.ifBlank { null },
                        unitType = unitType,
                        unitQuantity = qty,
                        unitLabel = unitLabel
                    ).onSuccess {
                        snackbarHostState.showSnackbar("Removed $qty $unitLabel")
                        showRemoveDialog = false
                        selectedItem = null
                        quantity = ""
                        unitType = "base"
                        reason = ""
                        loadStockData()
                    }.onFailure { error ->
                        snackbarHostState.showSnackbar("Error: ${error.message}")
                    }
                    
                    isRemoving = false
                }
            }
        )
    }
}

@Composable
fun SummaryCard(
    title: String,
    count: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategorySection(
    category: String,
    items: List<ApiClient.StockItem>,
    searchTerm: String,
    onSearchChange: (String) -> Unit,
    quickActionItem: ApiClient.StockItem?,
    quickQuantity: String,
    quickUnitType: String,
    isQuickActionLoading: Boolean,
    onQuickQuantityChange: (String) -> Unit,
    onQuickUnitTypeChange: (String) -> Unit,
    onQuickActionClick: (ApiClient.StockItem) -> Unit,
    onQuickActionCancel: () -> Unit,
    onQuickStockOut: (ApiClient.StockItem) -> Unit
) {
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            // Category header with search
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = category,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${items.size} item${if (items.size != 1) "s" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedTextField(
                    value = searchTerm,
                    onValueChange = onSearchChange,
                    placeholder = { Text("Search in $category") },
                    modifier = Modifier.width(200.dp),
                    singleLine = true
                )
            }
            
            Spacer(Modifier.height(12.dp))
            
            // Items
            items.forEach { item ->
                StockItemRow(
                    item = item,
                    isQuickAction = quickActionItem?.id == item.id,
                    quickQuantity = quickQuantity,
                    quickUnitType = quickUnitType,
                    isLoading = isQuickActionLoading,
                    onQuickQuantityChange = onQuickQuantityChange,
                    onQuickUnitTypeChange = onQuickUnitTypeChange,
                    onQuickActionClick = { onQuickActionClick(item) },
                    onQuickActionCancel = onQuickActionCancel,
                    onQuickStockOut = { onQuickStockOut(item) }
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockItemRow(
    item: ApiClient.StockItem,
    isQuickAction: Boolean,
    quickQuantity: String,
    quickUnitType: String,
    isLoading: Boolean,
    onQuickQuantityChange: (String) -> Unit,
    onQuickUnitTypeChange: (String) -> Unit,
    onQuickActionClick: () -> Unit,
    onQuickActionCancel: () -> Unit,
    onQuickStockOut: () -> Unit
) {
    val threshold = item.items?.threshold_level ?: 0
    val current = item.current_quantity
    val status = when {
        current <= threshold * 0.5 -> "Critical" to Color(0xFFE53E3E)
        current <= threshold -> "Low" to Color(0xFFFF8800)
        else -> "Adequate" to Color(0xFF00C851)
    }
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Image - ALWAYS show something
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (!item.items?.image_url.isNullOrBlank()) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (!item.items?.image_url.isNullOrBlank()) {
                    android.util.Log.d("StockOutScreen", "Showing image for ${item.items?.name}")
                    Base64Image(
                        base64String = item.items?.image_url,
                        contentDescription = null,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    android.util.Log.d("StockOutScreen", "Showing placeholder for ${item.items?.name}")
                    Icon(
                        Icons.Default.Inventory,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(Modifier.width(12.dp))
            
            // Item info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.items?.name ?: "Unknown",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Qty: ${item.current_quantity} ${item.items?.base_unit ?: "piece"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Threshold: ${threshold} ${item.items?.base_unit ?: "piece"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(Modifier.width(8.dp))
            
            // Status badge
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = status.second.copy(alpha = 0.1f)
            ) {
                Text(
                    text = status.first,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = status.second
                )
            }
            
            Spacer(Modifier.width(8.dp))
            
            // Quick action
            if (isQuickAction) {
                Column {
                    if (item.items?.enable_packaging == true && item.items.packaging_unit != null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            FilterChip(
                                selected = quickUnitType == "base",
                                onClick = { onQuickUnitTypeChange("base") },
                                label = { Text(item.items.base_unit ?: "piece", style = MaterialTheme.typography.labelSmall) }
                            )
                            FilterChip(
                                selected = quickUnitType == "packaging",
                                onClick = { onQuickUnitTypeChange("packaging") },
                                label = { Text(item.items.packaging_unit, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedTextField(
                            value = quickQuantity,
                            onValueChange = onQuickQuantityChange,
                            modifier = Modifier.width(80.dp),
                            placeholder = { Text("Qty") },
                            singleLine = true,
                            enabled = !isLoading,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.outline,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                        Surface(
                            onClick = onQuickStockOut,
                            enabled = !isLoading,
                            modifier = Modifier.size(40.dp),
                            shape = CircleShape,
                            color = Color(0xFFFF0000).copy(alpha = 0.2f)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = Color(0xFFFF0000),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.Remove,
                                        contentDescription = "Remove",
                                        modifier = Modifier.size(20.dp),
                                        tint = Color(0xFFFF0000)
                                    )
                                }
                            }
                        }
                        IconButton(
                            onClick = onQuickActionCancel,
                            enabled = !isLoading
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel")
                        }
                    }
                }
            } else {
                Surface(
                    onClick = onQuickActionClick,
                    enabled = !isLoading,
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                    color = Color(0xFFFF0000).copy(alpha = 0.2f)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            Icons.Default.Remove,
                            contentDescription = "Quick remove",
                            modifier = Modifier.size(20.dp),
                            tint = Color(0xFFFF0000)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoveStockDialog(
    stockItems: List<ApiClient.StockItem>,
    selectedItem: ApiClient.StockItem?,
    quantity: String,
    unitType: String,
    reason: String,
    isRemoving: Boolean,
    onSelectItem: (ApiClient.StockItem) -> Unit,
    onQuantityChange: (String) -> Unit,
    onUnitTypeChange: (String) -> Unit,
    onReasonChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    
    val filteredItems = stockItems.filter { item ->
        val name = item.items?.name ?: ""
        val category = item.items?.category ?: ""
        searchQuery.isEmpty() || name.contains(searchQuery, ignoreCase = true) || category.contains(searchQuery, ignoreCase = true)
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "Remove Stock",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(Modifier.height(16.dp))
                
                // Item selector
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedItem?.items?.name ?: "Select an item",
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search...") },
                            modifier = Modifier.fillMaxWidth().padding(8.dp)
                        )
                        filteredItems.forEach { item ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (!item.items?.image_url.isNullOrBlank()) {
                                            Base64Image(
                                                base64String = item.items?.image_url,
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(RoundedCornerShape(6.dp)),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    Icons.Default.Inventory,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                        Text("${item.items?.name} (${item.current_quantity} ${item.items?.base_unit})")
                                    }
                                },
                                onClick = {
                                    onSelectItem(item)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                
                if (selectedItem != null) {
                    Spacer(Modifier.height(16.dp))
                    
                    // Unit type selection
                    if (selectedItem.items?.enable_packaging == true && selectedItem.items.packaging_unit != null) {
                        Text("Remove By:", style = MaterialTheme.typography.labelMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = unitType == "base",
                                onClick = { onUnitTypeChange("base") },
                                label = { Text(selectedItem.items.base_unit ?: "piece") }
                            )
                            FilterChip(
                                selected = unitType == "packaging",
                                onClick = { onUnitTypeChange("packaging") },
                                label = { Text(selectedItem.items.packaging_unit) }
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    
                    // Quantity
                    OutlinedTextField(
                        value = quantity,
                        onValueChange = onQuantityChange,
                        label = { Text("Quantity to Remove") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    if (unitType == "packaging" && quantity.toIntOrNull() != null && selectedItem.items?.units_per_package != null) {
                        val baseUnits = quantity.toInt() * selectedItem.items.units_per_package
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF3B82F6).copy(alpha = 0.1f))
                        ) {
                            Text(
                                text = "$quantity ${selectedItem.items.packaging_unit} = $baseUnits ${selectedItem.items.base_unit}",
                                modifier = Modifier.padding(8.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF3B82F6)
                            )
                        }
                    }
                    
                    Spacer(Modifier.height(12.dp))
                    
                    // Reason
                    OutlinedTextField(
                        value = reason,
                        onValueChange = onReasonChange,
                        label = { Text("Reason (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
                
                Spacer(Modifier.height(24.dp))
                
                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss, enabled = !isRemoving) {
                        Text("Cancel")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = onConfirm,
                        enabled = !isRemoving && selectedItem != null && quantity.isNotBlank()
                    ) {
                        if (isRemoving) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Remove Stock")
                        }
                    }
                }
            }
        }
    }
}
