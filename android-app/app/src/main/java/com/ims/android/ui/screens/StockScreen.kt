package com.ims.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ims.android.data.api.ApiClient
import com.ims.android.data.repository.InventoryRepository
import com.ims.android.ui.components.Base64Image
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockScreen(
    inventoryRepository: InventoryRepository,
    modifier: Modifier = Modifier
) {
    var stockItems by remember { mutableStateOf<List<ApiClient.StockItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var generalSearchTerm by remember { mutableStateOf("") }
    var categorySearchTerms by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var showGeneralSearch by remember { mutableStateOf(false) }
    var showCategorySearch by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    var quickActionItem by remember { mutableStateOf<ApiClient.StockItem?>(null) }
    var quickQuantity by remember { mutableStateOf("") }
    var quickUnitType by remember { mutableStateOf("base") }
    var isQuickActionLoading by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Load stock data
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val result = inventoryRepository.getStockData()
                if (result.isSuccess) {
                    stockItems = result.getOrThrow()
                    
                    // Initialize stock if empty
                    if (result.getOrThrow().isEmpty()) {
                        val initResult = inventoryRepository.initializeStock()
                        if (initResult.isSuccess && initResult.getOrThrow().initialized > 0) {
                            snackbarHostState.showSnackbar("Created stock records for ${initResult.getOrThrow().initialized} items")
                            val newData = inventoryRepository.getStockData()
                            if (newData.isSuccess) {
                                stockItems = newData.getOrThrow()
                            }
                        }
                    }
                } else {
                    snackbarHostState.showSnackbar("Error loading stock data")
                }
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Error loading stock data: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }
    
    // Filter items by general search and category-specific search
    val filteredItems = remember(stockItems, generalSearchTerm, categorySearchTerms) {
        stockItems.filter { item ->
            val matchesGeneralSearch = generalSearchTerm.isEmpty() || 
                item.items?.name?.contains(generalSearchTerm, ignoreCase = true) == true ||
                item.items?.category?.contains(generalSearchTerm, ignoreCase = true) == true
            
            val category = item.items?.category ?: "Uncategorized"
            val categorySearch = categorySearchTerms[category] ?: ""
            val matchesCategorySearch = categorySearch.isEmpty() ||
                item.items?.name?.contains(categorySearch, ignoreCase = true) == true
            
            matchesGeneralSearch && matchesCategorySearch
        }
    }
    
    val groupedItems = remember(filteredItems) {
        filteredItems.groupBy { it.items?.category ?: "Uncategorized" }
    }
    
    // Calculate summary stats
    val totalItems = stockItems.size
    val lowStockCount = stockItems.count { 
        it.current_quantity <= (it.items?.low_level ?: it.items?.threshold_level ?: 0)
    }
    val criticalStockCount = stockItems.count { 
        it.current_quantity <= (it.items?.critical_level ?: ((it.items?.threshold_level ?: 0) / 2))
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
                    .padding(top = 8.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
            ) {
                // Header
                Text(
                    text = "Stock Management",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Summary Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SummaryCard(
                        title = "Total Items",
                        value = totalItems.toString(),
                        icon = Icons.Default.Inventory,
                        color = Color(0xFF3B82F6),
                        modifier = Modifier.weight(1f)
                    )
                    SummaryCard(
                        title = "Low Stock",
                        value = lowStockCount.toString(),
                        icon = Icons.Default.Warning,
                        color = Color(0xFFFF8800),
                        modifier = Modifier.weight(1f)
                    )
                    SummaryCard(
                        title = "Critical",
                        value = criticalStockCount.toString(),
                        icon = Icons.Default.Error,
                        color = Color(0xFFE6002A),
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Current Stock Levels title and search icon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Current Stock Levels",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    
                    if (!showGeneralSearch) {
                        IconButton(
                            onClick = { showGeneralSearch = true },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(Icons.Default.Search, "Search", modifier = Modifier.size(24.dp))
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // General Search Bar (collapsible)
                if (showGeneralSearch) {
                    OutlinedTextField(
                        value = generalSearchTerm,
                        onValueChange = { generalSearchTerm = it },
                        placeholder = { Text("Search for item with name or category", style = MaterialTheme.typography.bodySmall) },
                        leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(20.dp)) },
                        trailingIcon = {
                            IconButton(onClick = { 
                                showGeneralSearch = false
                                generalSearchTerm = ""
                            }) {
                                Icon(Icons.Default.Close, "Close search", modifier = Modifier.size(20.dp))
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                // Stock Items List by Category
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    groupedItems.forEach { (category, items) ->
                        item {
                            Column {
                                // Category Title and Search Icon
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Start,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = category,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    
                                    Spacer(modifier = Modifier.width(8.dp))
                                    
                                    if (showCategorySearch[category] != true) {
                                        IconButton(
                                            onClick = { 
                                                showCategorySearch = showCategorySearch + (category to true)
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Search, "Search in $category", modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                // Category-specific search (collapsible)
                                if (showCategorySearch[category] == true) {
                                    OutlinedTextField(
                                        value = categorySearchTerms[category] ?: "",
                                        onValueChange = { 
                                            categorySearchTerms = categorySearchTerms + (category to it)
                                        },
                                        placeholder = { Text("Search in $category...", style = MaterialTheme.typography.bodySmall) },
                                        leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp)) },
                                        trailingIcon = {
                                            IconButton(onClick = { 
                                                showCategorySearch = showCategorySearch + (category to false)
                                                categorySearchTerms = categorySearchTerms + (category to "")
                                            }) {
                                                Icon(Icons.Default.Close, "Close search", modifier = Modifier.size(18.dp))
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp),
                                        singleLine = true,
                                        textStyle = MaterialTheme.typography.bodyMedium
                                    )
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }
                        
                        items(items) { item ->
                            StockItemCard(
                                item = item,
                                isQuickAction = quickActionItem?.id == item.id,
                                quickQuantity = quickQuantity,
                                onQuickQuantityChange = { quickQuantity = it },
                                quickUnitType = quickUnitType,
                                onQuickUnitTypeChange = { quickUnitType = it },
                                isQuickActionLoading = isQuickActionLoading,
                                onMinusClick = {
                                    quickActionItem = item
                                    quickQuantity = ""
                                    quickUnitType = "base"
                                },
                                onQuickRemove = {
                                    scope.launch {
                                        isQuickActionLoading = true
                                        try {
                                            val qty = quickQuantity.toIntOrNull()
                                            if (qty == null || qty <= 0) {
                                                snackbarHostState.showSnackbar("Invalid quantity")
                                                isQuickActionLoading = false
                                                return@launch
                                            }
                                            
                                            val qtyInBase = if (quickUnitType == "packaging" && (item.items?.units_per_package ?: 0) > 0) {
                                                qty * (item.items?.units_per_package ?: 1)
                                            } else {
                                                qty
                                            }
                                            
                                            if (qtyInBase > (item.current_quantity ?: 0)) {
                                                snackbarHostState.showSnackbar("Insufficient stock!")
                                                isQuickActionLoading = false
                                                return@launch
                                            }
                                            
                                            val result = inventoryRepository.updateStockQuantity(
                                                item.item_id ?: "",
                                                "out",
                                                qtyInBase,
                                                null,
                                                quickUnitType,
                                                qty
                                            )
                                            
                                            isQuickActionLoading = false
                                            
                                            if (result.isSuccess) {
                                                quickActionItem = null
                                                quickQuantity = ""
                                                
                                                val newData = inventoryRepository.getStockData()
                                                if (newData.isSuccess) {
                                                    stockItems = newData.getOrThrow()
                                                }
                                                
                                                snackbarHostState.showSnackbar("Stock removed successfully")
                                            } else {
                                                snackbarHostState.showSnackbar("Error: ${result.exceptionOrNull()?.message}")
                                            }
                                        } catch (e: Exception) {
                                            snackbarHostState.showSnackbar("Error: ${e.message}")
                                            isQuickActionLoading = false
                                        }
                                    }
                                },
                                onQuickCancel = {
                                    quickActionItem = null
                                    quickQuantity = ""
                                    quickUnitType = "base"
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SummaryCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
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
fun StockItemCard(
    item: ApiClient.StockItem,
    isQuickAction: Boolean,
    quickQuantity: String,
    onQuickQuantityChange: (String) -> Unit,
    quickUnitType: String,
    onQuickUnitTypeChange: (String) -> Unit,
    isQuickActionLoading: Boolean,
    onMinusClick: () -> Unit,
    onQuickRemove: () -> Unit,
    onQuickCancel: () -> Unit
) {
    val isLowStock = item.current_quantity <= (item.items?.low_level ?: item.items?.threshold_level ?: 0)
    val isCritical = item.current_quantity <= (item.items?.critical_level ?: ((item.items?.threshold_level ?: 0) / 2))
    
    val statusColor = when {
        isCritical -> Color(0xFFE6002A)
        isLowStock -> Color(0xFFFF8800)
        else -> Color(0xFF00C851)
    }
    
    val statusText = when {
        isCritical -> "Critical"
        isLowStock -> "Low Stock"
        else -> "In Stock"
    }
    
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Item Image
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (!item.items?.image_url.isNullOrBlank()) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (!item.items?.image_url.isNullOrBlank()) {
                        Base64Image(
                            base64String = item.items?.image_url,
                            contentDescription = item.items?.name,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            Icons.Default.Inventory,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Item details
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.items?.name ?: "Unknown Item",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = item.items?.category ?: "Uncategorized",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Qty: ${item.current_quantity} ${item.items?.base_unit ?: "piece"}${if (item.current_quantity != 1) "s" else ""}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    Text(
                        text = "Threshold: ${item.items?.threshold_level ?: 0} ${item.items?.base_unit ?: "piece"}${if ((item.items?.threshold_level ?: 0) != 1) "s" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = statusColor.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = statusText,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = statusColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            
            // Quick Action Form
            if (isQuickAction) {
                Spacer(modifier = Modifier.height(12.dp))
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Unit Selection
                    if (item.items?.enable_packaging == true && (item.items?.units_per_package ?: 0) > 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Unit:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = quickUnitType == "base",
                                    onClick = { onQuickUnitTypeChange("base") },
                                    label = { Text(item.items?.base_unit ?: "piece", style = MaterialTheme.typography.bodySmall) }
                                )
                                FilterChip(
                                    selected = quickUnitType == "packaging",
                                    onClick = { onQuickUnitTypeChange("packaging") },
                                    label = { Text(item.items?.packaging_unit ?: "carton", style = MaterialTheme.typography.bodySmall) }
                                )
                            }
                        }
                    }
                    
                    // Quantity and Actions
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = quickQuantity,
                            onValueChange = onQuickQuantityChange,
                            placeholder = { Text("Qty", style = MaterialTheme.typography.bodySmall) },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.outline,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                        
                        Button(
                            onClick = onQuickRemove,
                            enabled = !isQuickActionLoading && quickQuantity.isNotEmpty(),
                            modifier = Modifier.height(48.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFF0000).copy(alpha = 0.2f),
                                contentColor = Color(0xFFFF0000)
                            )
                        ) {
                            if (isQuickActionLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = Color(0xFFFF0000)
                                )
                            } else {
                                Text("Remove")
                            }
                        }
                        
                        IconButton(
                            onClick = onQuickCancel,
                            enabled = !isQuickActionLoading,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Default.Close, "Cancel")
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                
                // Minus Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Surface(
                        onClick = onMinusClick,
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
                                "Remove stock",
                                modifier = Modifier.size(20.dp),
                                tint = Color(0xFFFF0000)
                            )
                        }
                    }
                }
            }
        }
    }
}
