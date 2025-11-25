package com.ims.android.ui.screens

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ims.android.data.api.ApiClient
import com.ims.android.data.repository.InventoryRepository
import com.ims.android.ui.components.Base64Image
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockInScreen(
    inventoryRepository: InventoryRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var stockItems by remember { mutableStateOf<List<ApiClient.StockItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var generalSearchTerm by remember { mutableStateOf("") }
    var categorySearchTerms by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var showGeneralSearch by remember { mutableStateOf(false) }
    var showCategorySearch by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    var filterType by remember { mutableStateOf<String>("all") }
    var quickActionItem by remember { mutableStateOf<ApiClient.StockItem?>(null) }
    var quickQuantity by remember { mutableStateOf("") }
    var quickUnitType by remember { mutableStateOf("base") }
    var isQuickActionLoading by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedItem by remember { mutableStateOf<ApiClient.StockItem?>(null) }
    var addQuantity by remember { mutableStateOf("") }
    var addUnitType by remember { mutableStateOf("base") }
    var addReason by remember { mutableStateOf("") }
    var isAddingStock by remember { mutableStateOf(false) }
    var showItemReceipts by remember { mutableStateOf(false) }
    var itemReceipts by remember { mutableStateOf<List<ApiClient.StockReceipt>>(emptyList()) }
    var isLoadingReceipts by remember { mutableStateOf(false) }
    var expandedReceiptId by remember { mutableStateOf<String?>(null) }
    var fullScreenReceipt by remember { mutableStateOf<ApiClient.StockReceipt?>(null) }
    var documentZoom by remember { mutableStateOf(1f) }
    var documentOffset by remember { mutableStateOf(Offset.Zero) }
    var viewDialogReceipt by remember { mutableStateOf<ApiClient.StockReceipt?>(null) }
    var viewDialogZoom by remember { mutableStateOf(1f) }
    var viewDialogOffset by remember { mutableStateOf(Offset.Zero) }
    
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Load stock data
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val result = inventoryRepository.getStockData()
                if (result.isSuccess) {
                    stockItems = result.getOrThrow()
                } else {
                    snackbarHostState.showSnackbar("Error loading stock data")
                }
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Error: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }
    
    // Filter and group items
    val filteredItems = remember(stockItems, generalSearchTerm, categorySearchTerms, filterType) {
        stockItems.filter { item ->
            val matchesGeneralSearch = generalSearchTerm.isEmpty() || 
                item.items?.name?.contains(generalSearchTerm, ignoreCase = true) == true ||
                item.items?.category?.contains(generalSearchTerm, ignoreCase = true) == true
            
            val category = item.items?.category ?: "Uncategorized"
            val categorySearch = categorySearchTerms[category] ?: ""
            val matchesCategorySearch = categorySearch.isEmpty() ||
                item.items?.name?.contains(categorySearch, ignoreCase = true) == true
            
            val matchesFilter = when (filterType) {
                "low" -> item.current_quantity <= (item.items?.low_level ?: item.items?.threshold_level ?: 0)
                "critical" -> item.current_quantity <= (item.items?.critical_level ?: ((item.items?.threshold_level ?: 0) / 2))
                else -> true
            }
            
            matchesGeneralSearch && matchesCategorySearch && matchesFilter
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
        val receipt = fullScreenReceipt
        if (receipt != null) {
            // Split-Screen View: Document on top, Stock-in on bottom
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Top Section: Document Viewer (50% of screen)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF1F1F1F)
                    )
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Document viewer header with controls
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                IconButton(onClick = { fullScreenReceipt = null }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close",
                                        tint = Color.White
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = receipt.supplier_name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = receipt.receipt_file_name,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.7f)
                                    )
                                }
                            }
                            
                            // Zoom controls
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { 
                                        documentZoom = (documentZoom - 0.5f).coerceAtLeast(0.5f)
                                        documentOffset = Offset.Zero
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Remove,
                                        contentDescription = "Zoom Out",
                                        tint = Color.White
                                    )
                                }
                                Text(
                                    text = "${(documentZoom * 100).toInt()}%",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White,
                                    modifier = Modifier.widthIn(min = 40.dp)
                                )
                                IconButton(
                                    onClick = { documentZoom = (documentZoom + 0.5f).coerceAtMost(10f) }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Zoom In",
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                        
                        Divider(color = Color.White.copy(alpha = 0.2f))
                        
                        // Document display area with pinch zoom and pan
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // Convert file path to actual URL
                            val imageUrl = when {
                                receipt.receipt_file_name.startsWith("http") && !receipt.receipt_file_name.startsWith("blob:") -> {
                                    receipt.receipt_file_name
                                }
                                !receipt.receipt_file_path.isNullOrEmpty() -> {
                                    // Extract filename from the server path
                                    val filename = receipt.receipt_file_path.substringAfterLast("/")
                                    "https://stock-nexus-84-main-2-1.onrender.com/uploads/receipts/$filename"
                                }
                                receipt.receipt_file_name.contains("/uploads/receipts/") -> {
                                    // Extract filename from the file name field
                                    val filename = receipt.receipt_file_name.substringAfterLast("/")
                                    "https://stock-nexus-84-main-2-1.onrender.com/uploads/receipts/$filename"
                                }
                                else -> {
                                    // Fallback: assume it's just the filename
                                    "https://stock-nexus-84-main-2-1.onrender.com/uploads/receipts/${receipt.receipt_file_name}"
                                }
                            }
                            
                            AsyncImage(
                                model = imageUrl,
                                contentDescription = "Receipt Document",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .pointerInput(Unit) {
                                        detectTransformGestures { _, pan, zoom, _ ->
                                            documentZoom = (documentZoom * zoom).coerceIn(0.5f, 10f)
                                            documentOffset = Offset(
                                                x = (documentOffset.x + pan.x).coerceIn(-2000f, 2000f),
                                                y = (documentOffset.y + pan.y).coerceIn(-2000f, 2000f)
                                            )
                                        }
                                    }
                                    .graphicsLayer {
                                        scaleX = documentZoom
                                        scaleY = documentZoom
                                        translationX = documentOffset.x
                                        translationY = documentOffset.y
                                    },
                                contentScale = ContentScale.Fit,
                                error = painterResource(android.R.drawable.ic_menu_report_image),
                                placeholder = painterResource(android.R.drawable.ic_menu_gallery)
                            )
                        }
                    }
                }
                
                Divider(thickness = 2.dp, color = MaterialTheme.colorScheme.primary)
                
                // Bottom Section: Stock-In Interface (50% of screen)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(16.dp)
                ) {
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
                            placeholder = { Text("Search for items by name or category...", style = MaterialTheme.typography.bodySmall) },
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
                                StockInItemCard(
                                    item = item,
                                    isQuickAction = quickActionItem?.id == item.id,
                                    quickQuantity = quickQuantity,
                                    onQuickQuantityChange = { quickQuantity = it },
                                    quickUnitType = quickUnitType,
                                    onQuickUnitTypeChange = { quickUnitType = it },
                                    isQuickActionLoading = isQuickActionLoading,
                                    onPlusClick = {
                                        quickActionItem = item
                                        quickQuantity = ""
                                        quickUnitType = "base"
                                    },
                                    onQuickAdd = {
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
                                                
                                                val result = inventoryRepository.updateStockQuantity(
                                                    item.item_id ?: "",
                                                    "in",
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
                                                    
                                                    snackbarHostState.showSnackbar("Stock added successfully")
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
        } else if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
                    text = "Stock In",
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
                        modifier = Modifier.weight(1f),
                        onClick = { filterType = "all" }
                    )
                    SummaryCard(
                        title = "Low Stock",
                        value = lowStockCount.toString(),
                        icon = Icons.Default.Warning,
                        color = Color(0xFFFF8800),
                        modifier = Modifier.weight(1f),
                        onClick = { filterType = "low" }
                    )
                    SummaryCard(
                        title = "Critical",
                        value = criticalStockCount.toString(),
                        icon = Icons.Default.Error,
                        color = Color(0xFFE6002A),
                        modifier = Modifier.weight(1f),
                        onClick = { filterType = "critical" }
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Item Receipts Section (Expandable)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(
                                    if (!isLoadingReceipts) {
                                        Modifier.clickable {
                                            showItemReceipts = !showItemReceipts
                                            if (showItemReceipts && itemReceipts.isEmpty()) {
                                                scope.launch {
                                                    isLoadingReceipts = true
                                                    try {
                                                        val result = inventoryRepository.getReceipts()
                                                        if (result.isSuccess) {
                                                            itemReceipts = result.getOrThrow()
                                                        }
                                                    } catch (e: Exception) {
                                                        snackbarHostState.showSnackbar("Error loading receipts: ${e.message}")
                                                    } finally {
                                                        isLoadingReceipts = false
                                                    }
                                                }
                                            }
                                        }
                                    } else Modifier
                                ),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Item Receipts",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            
                            if (isLoadingReceipts) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            } else {
                                Icon(
                                    imageVector = if (showItemReceipts) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = if (showItemReceipts) "Collapse" else "Expand"
                                )
                            }
                        }
                        
                        if (showItemReceipts) {
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            if (isLoadingReceipts) {
                                // Show loading state
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            } else if (itemReceipts.isEmpty()) {
                                Text(
                                    text = "No receipts found",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    itemReceipts.take(10).forEach { receipt ->
                                        Card(
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.surface
                                            ),
                                            border = CardDefaults.outlinedCardBorder()
                                        ) {
                                            Column {
                                                // Header (always visible)
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable { 
                                                            expandedReceiptId = if (expandedReceiptId == receipt.id) null else receipt.id 
                                                        }
                                                        .padding(12.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Icon(
                                                            imageVector = when (receipt.status) {
                                                                "approved" -> Icons.Default.CheckCircle
                                                                "rejected" -> Icons.Default.Cancel
                                                                else -> Icons.Default.Schedule
                                                            },
                                                            contentDescription = receipt.status,
                                                            tint = when (receipt.status) {
                                                                "approved" -> Color(0xFF10B981)
                                                                "rejected" -> Color(0xFFEF4444)
                                                                else -> Color(0xFFF59E0B)
                                                            },
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                        
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(
                                                                text = receipt.supplier_name,
                                                                style = MaterialTheme.typography.titleSmall,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                            Text(
                                                                text = "Submitted by ${receipt.submitted_by_name ?: "Unknown"}",
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                    }
                                                    
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        Badge(
                                                            containerColor = when (receipt.status) {
                                                                "approved" -> Color(0xFF10B981)
                                                                "rejected" -> Color(0xFFEF4444)
                                                                else -> Color(0xFFF59E0B)
                                                            }
                                                        ) {
                                                            Text(
                                                                text = receipt.status.replaceFirstChar { it.uppercase() },
                                                                color = Color.White,
                                                                style = MaterialTheme.typography.labelSmall
                                                            )
                                                        }
                                                        
                                                        Icon(
                                                            imageVector = if (expandedReceiptId == receipt.id) 
                                                                Icons.Default.ExpandLess 
                                                            else 
                                                                Icons.Default.ExpandMore,
                                                            contentDescription = "Expand"
                                                        )
                                                    }
                                                }
                                                
                                                // Expanded content
                                                if (expandedReceiptId == receipt.id) {
                                                    Divider()
                                                    
                                                    Column(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(12.dp),
                                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                                    ) {
                                                        // Receipt File section
                                                        Column {
                                                            Text(
                                                                text = "Receipt File",
                                                                style = MaterialTheme.typography.labelMedium,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                            Spacer(modifier = Modifier.height(4.dp))
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Default.Description,
                                                                    contentDescription = "File",
                                                                    modifier = Modifier.size(16.dp),
                                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                                )
                                                                Text(
                                                                    text = receipt.receipt_file_name,
                                                                    style = MaterialTheme.typography.bodySmall,
                                                                    modifier = Modifier.weight(1f)
                                                                )
                                                            }
                                                            Spacer(modifier = Modifier.height(8.dp))
                                                            Row(
                                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                                modifier = Modifier.fillMaxWidth()
                                                            ) {
                                                                IconButton(
                                                                    onClick = { 
                                                                        viewDialogReceipt = receipt
                                                                        viewDialogZoom = 1f
                                                                    },
                                                                    modifier = Modifier
                                                                        .weight(1f)
                                                                        .height(40.dp)
                                                                ) {
                                                                    Icon(
                                                                        imageVector = Icons.Default.Visibility,
                                                                        contentDescription = "View"
                                                                    )
                                                                }
                                                                
                                                                IconButton(
                                                                    onClick = { 
                                                                        scope.launch {
                                                                            try {
                                                                                val imageUrl = when {
                                                                                    receipt.receipt_file_name.startsWith("http") && !receipt.receipt_file_name.startsWith("blob:") -> receipt.receipt_file_name
                                                                                    !receipt.receipt_file_path.isNullOrEmpty() -> {
                                                                                        val filename = receipt.receipt_file_path.substringAfterLast("/")
                                                                                        "https://stock-nexus-84-main-2-1.onrender.com/uploads/receipts/$filename"
                                                                                    }
                                                                                    receipt.receipt_file_name.contains("/uploads/receipts/") -> {
                                                                                        val filename = receipt.receipt_file_name.substringAfterLast("/")
                                                                                        "https://stock-nexus-84-main-2-1.onrender.com/uploads/receipts/$filename"
                                                                                    }
                                                                                    else -> "https://stock-nexus-84-main-2-1.onrender.com/uploads/receipts/${receipt.receipt_file_name}"
                                                                                }
                                                                                
                                                                                val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                                                                                val request = DownloadManager.Request(Uri.parse(imageUrl))
                                                                                    .setTitle(receipt.receipt_file_name)
                                                                                    .setDescription("Downloading receipt from ${receipt.supplier_name}")
                                                                                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                                                                    .setDestinationInExternalPublicDir(
                                                                                        Environment.DIRECTORY_DOWNLOADS,
                                                                                        "StockNexus_${receipt.receipt_file_name}"
                                                                                    )
                                                                                    .setAllowedOverMetered(true)
                                                                                    .setAllowedOverRoaming(true)
                                                                                
                                                                                downloadManager.enqueue(request)
                                                                                snackbarHostState.showSnackbar("Download started")
                                                                            } catch (e: Exception) {
                                                                                snackbarHostState.showSnackbar("Download failed: ${e.message}")
                                                                            }
                                                                        }
                                                                    },
                                                                    modifier = Modifier
                                                                        .weight(1f)
                                                                        .height(40.dp)
                                                                ) {
                                                                    Icon(
                                                                        imageVector = Icons.Default.Download,
                                                                        contentDescription = "Download"
                                                                    )
                                                                }
                                                                
                                                                IconButton(
                                                                    onClick = { 
                                                                        android.util.Log.d("StockInScreen", "Full Screen clicked for receipt: ${receipt.id}")
                                                                        fullScreenReceipt = receipt
                                                                        documentZoom = 1f
                                                                    },
                                                                    modifier = Modifier
                                                                        .weight(1f)
                                                                        .height(40.dp)
                                                                ) {
                                                                    Icon(
                                                                        imageVector = Icons.Default.Fullscreen,
                                                                        contentDescription = "Full Screen",
                                                                        tint = MaterialTheme.colorScheme.primary
                                                                    )
                                                                }
                                                            }
                                                        }
                                                        
                                                        // Remarks section
                                                        Column {
                                                            Text(
                                                                text = "Remarks",
                                                                style = MaterialTheme.typography.labelMedium,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                            Spacer(modifier = Modifier.height(4.dp))
                                                            Text(
                                                                text = receipt.remarks?.takeIf { it.isNotEmpty() } ?: "No remarks provided",
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                        
                                                        // Reviewed info (if applicable)
                                                        if (receipt.reviewed_at != null && receipt.reviewed_by_name != null) {
                                                            Text(
                                                                text = "Reviewed by ${receipt.reviewed_by_name} on ${receipt.reviewed_at}",
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                        
                                                        // Approve/Reject buttons for pending receipts
                                                        if (receipt.status == "pending") {
                                                            Row(
                                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                                modifier = Modifier.padding(top = 4.dp)
                                                            ) {
                                                                Button(
                                                                    onClick = { 
                                                                        scope.launch {
                                                                            snackbarHostState.showSnackbar("Approve receipt: ${receipt.supplier_name}")
                                                                        }
                                                                    },
                                                                    colors = ButtonDefaults.buttonColors(
                                                                        containerColor = Color(0xFF10B981),
                                                                        contentColor = Color.White
                                                                    ),
                                                                    modifier = Modifier.height(36.dp)
                                                                ) {
                                                                    Icon(
                                                                        imageVector = Icons.Default.CheckCircle,
                                                                        contentDescription = "Approve",
                                                                        modifier = Modifier.size(18.dp),
                                                                        tint = Color.White
                                                                    )
                                                                    Spacer(modifier = Modifier.width(4.dp))
                                                                    Text("Approve", color = Color.White)
                                                                }
                                                                
                                                                Button(
                                                                    onClick = { 
                                                                        scope.launch {
                                                                            snackbarHostState.showSnackbar("Reject receipt: ${receipt.supplier_name}")
                                                                        }
                                                                    },
                                                                    colors = ButtonDefaults.buttonColors(
                                                                        containerColor = Color(0xFFEF4444),
                                                                        contentColor = Color.White
                                                                    ),
                                                                    modifier = Modifier.height(36.dp)
                                                                ) {
                                                                    Icon(
                                                                        imageVector = Icons.Default.Cancel,
                                                                        contentDescription = "Reject",
                                                                        modifier = Modifier.size(18.dp),
                                                                        tint = Color.White
                                                                    )
                                                                    Spacer(modifier = Modifier.width(4.dp))
                                                                    Text("Reject", color = Color.White)
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    
                                    if (itemReceipts.size > 10) {
                                        Text(
                                            text = "Showing 10 of ${itemReceipts.size} receipts",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Current Stock Levels title and search icon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when (filterType) {
                            "low" -> "Low Stock Items"
                            "critical" -> "Critical Stock Items"
                            else -> "Current Stock Levels"
                        },
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
                        placeholder = { Text("Search for items by name or category...", style = MaterialTheme.typography.bodySmall) },
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
                            StockInItemCard(
                                item = item,
                                isQuickAction = quickActionItem?.id == item.id,
                                quickQuantity = quickQuantity,
                                onQuickQuantityChange = { quickQuantity = it },
                                quickUnitType = quickUnitType,
                                onQuickUnitTypeChange = { quickUnitType = it },
                                isQuickActionLoading = isQuickActionLoading,
                                onPlusClick = {
                                    quickActionItem = item
                                    quickQuantity = ""
                                    quickUnitType = "base"
                                },
                                onQuickAdd = {
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
                                            
                                            val result = inventoryRepository.updateStockQuantity(
                                                item.item_id ?: "",
                                                "in",
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
                                                
                                                snackbarHostState.showSnackbar("Stock added successfully")
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
        
        // View Receipt Dialog
        viewDialogReceipt?.let { receipt ->
            Dialog(
                onDismissRequest = { viewDialogReceipt = null },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .fillMaxHeight(0.9f),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Header with close button and zoom controls
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = receipt.supplier_name,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = receipt.receipt_file_name,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            IconButton(onClick = { viewDialogReceipt = null }) {
                                Icon(Icons.Default.Close, "Close")
                            }
                        }
                        
                        Divider()
                        
                        // Zoom controls
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { 
                                    viewDialogZoom = (viewDialogZoom - 0.5f).coerceAtLeast(0.5f)
                                    viewDialogOffset = Offset.Zero
                                }
                            ) {
                                Icon(Icons.Default.ZoomOut, "Zoom Out")
                            }
                            
                            Text(
                                text = "${(viewDialogZoom * 100).toInt()}%",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            
                            IconButton(
                                onClick = { viewDialogZoom = (viewDialogZoom + 0.5f).coerceAtMost(10f) }
                            ) {
                                Icon(Icons.Default.ZoomIn, "Zoom In")
                            }
                        }
                        
                        Divider()
                        
                        // Document viewer with pinch zoom and pan
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            val imageUrl = when {
                                receipt.receipt_file_name.startsWith("http") && !receipt.receipt_file_name.startsWith("blob:") -> receipt.receipt_file_name
                                !receipt.receipt_file_path.isNullOrEmpty() -> {
                                    val filename = receipt.receipt_file_path.substringAfterLast("/")
                                    "https://stock-nexus-84-main-2-1.onrender.com/uploads/receipts/$filename"
                                }
                                receipt.receipt_file_name.contains("/uploads/receipts/") -> {
                                    val filename = receipt.receipt_file_name.substringAfterLast("/")
                                    "https://stock-nexus-84-main-2-1.onrender.com/uploads/receipts/$filename"
                                }
                                else -> "https://stock-nexus-84-main-2-1.onrender.com/uploads/receipts/${receipt.receipt_file_name}"
                            }
                            
                            AsyncImage(
                                model = imageUrl,
                                contentDescription = "Receipt Document",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .pointerInput(Unit) {
                                        detectTransformGestures { _, pan, zoom, _ ->
                                            viewDialogZoom = (viewDialogZoom * zoom).coerceIn(0.5f, 10f)
                                            viewDialogOffset = Offset(
                                                x = (viewDialogOffset.x + pan.x).coerceIn(-2000f, 2000f),
                                                y = (viewDialogOffset.y + pan.y).coerceIn(-2000f, 2000f)
                                            )
                                        }
                                    }
                                    .graphicsLayer {
                                        scaleX = viewDialogZoom
                                        scaleY = viewDialogZoom
                                        translationX = viewDialogOffset.x
                                        translationY = viewDialogOffset.y
                                    },
                                contentScale = ContentScale.Fit,
                                error = painterResource(android.R.drawable.ic_menu_report_image),
                                placeholder = painterResource(android.R.drawable.ic_menu_gallery)
                            )
                        }
                    }
                }
            }
        }
        
        // Add Stock Dialog
        if (showAddDialog && selectedItem != null) {
            AddStockDialog(
                item = selectedItem!!,
                quantity = addQuantity,
                onQuantityChange = { addQuantity = it },
                unitType = addUnitType,
                onUnitTypeChange = { addUnitType = it },
                reason = addReason,
                onReasonChange = { addReason = it },
                isAdding = isAddingStock,
                onDismiss = { 
                    showAddDialog = false
                    selectedItem = null
                    addQuantity = ""
                    addReason = ""
                    addUnitType = "base"
                },
                onConfirm = {
                    scope.launch {
                        isAddingStock = true
                        try {
                            val qty = addQuantity.toIntOrNull()
                            if (qty == null || qty <= 0) {
                                snackbarHostState.showSnackbar("Invalid quantity")
                                isAddingStock = false
                                return@launch
                            }
                            
                            val qtyInBase = if (addUnitType == "packaging" && (selectedItem?.items?.units_per_package ?: 0) > 0) {
                                qty * (selectedItem?.items?.units_per_package ?: 1)
                            } else {
                                qty
                            }
                            
                            val result = inventoryRepository.updateStockQuantity(
                                selectedItem?.item_id ?: "",
                                "in",
                                qtyInBase,
                                addReason.ifBlank { null },
                                addUnitType,
                                qty
                            )
                            
                            isAddingStock = false
                            
                            if (result.isSuccess) {
                                showAddDialog = false
                                selectedItem = null
                                addQuantity = ""
                                addReason = ""
                                
                                val newData = inventoryRepository.getStockData()
                                if (newData.isSuccess) {
                                    stockItems = newData.getOrThrow()
                                }
                                
                                snackbarHostState.showSnackbar("Stock added successfully")
                            } else {
                                snackbarHostState.showSnackbar("Error: ${result.exceptionOrNull()?.message}")
                            }
                        } catch (e: Exception) {
                            snackbarHostState.showSnackbar("Error: ${e.message}")
                            isAddingStock = false
                        }
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        onClick = onClick
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
fun StockInItemCard(
    item: ApiClient.StockItem,
    isQuickAction: Boolean,
    quickQuantity: String,
    onQuickQuantityChange: (String) -> Unit,
    quickUnitType: String,
    onQuickUnitTypeChange: (String) -> Unit,
    isQuickActionLoading: Boolean,
    onPlusClick: () -> Unit,
    onQuickAdd: () -> Unit,
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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Item details
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Image
                    if (!item.items?.image_url.isNullOrBlank()) {
                        Base64Image(
                            base64String = item.items?.image_url,
                            contentDescription = item.items?.name,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Surface(
                            modifier = Modifier.size(48.dp),
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Inventory, null, modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                    
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
                            textStyle = MaterialTheme.typography.bodyMedium
                        )
                        
                        Button(
                            onClick = onQuickAdd,
                            enabled = !isQuickActionLoading && quickQuantity.isNotEmpty(),
                            modifier = Modifier.height(48.dp)
                        ) {
                            if (isQuickActionLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Add Stock")
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
                
                // Plus Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(
                        onClick = onPlusClick,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = Color(0xFF00C851).copy(alpha = 0.1f),
                            contentColor = Color(0xFF00C851)
                        ),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Default.Add, "Add stock", modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddStockDialog(
    item: ApiClient.StockItem,
    quantity: String,
    onQuantityChange: (String) -> Unit,
    unitType: String,
    onUnitTypeChange: (String) -> Unit,
    reason: String,
    onReasonChange: (String) -> Unit,
    isAdding: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val hasPackaging = item.items?.enable_packaging == true && (item.items?.units_per_package ?: 0) > 0
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF333131),
        title = { Text("Add Stock") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = item.items?.name ?: "Unknown Item",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                if (hasPackaging) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Stock In By:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = unitType == "base",
                                onClick = { onUnitTypeChange("base") },
                                label = { Text(item.items?.base_unit ?: "piece") },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = unitType == "packaging",
                                onClick = { onUnitTypeChange("packaging") },
                                label = { Text(item.items?.packaging_unit ?: "carton") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                
                OutlinedTextField(
                    value = quantity,
                    onValueChange = onQuantityChange,
                    label = { Text("Quantity to Add") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("Enter quantity to add") }
                )
                
                OutlinedTextField(
                    value = reason,
                    onValueChange = onReasonChange,
                    label = { Text("Reason (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    placeholder = { Text("Enter reason for adding stock") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isAdding && quantity.isNotEmpty()
            ) {
                if (isAdding) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(if (isAdding) "Adding..." else "Add Stock")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isAdding) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun StockItemCard(
    stockItem: ApiClient.StockItem,
    onStockIn: (ApiClient.StockItem) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
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
                    text = stockItem.items?.name ?: "Unknown Item",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Current: ${stockItem.current_quantity}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            IconButton(
                onClick = { onStockIn(stockItem) }
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Stock",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
