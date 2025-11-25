package com.ims.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.ims.android.data.repository.InventoryRepository
import com.ims.android.data.model.Item
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemsScreen(
    inventoryRepository: InventoryRepository,
    isDarkTheme: Boolean
) {
    var items by remember { mutableStateOf<List<Item>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showSearchDialog by remember { mutableStateOf(false) }
    var selectedItem by remember { mutableStateOf<Item?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<Item?>(null) }
    
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Load items
    fun loadItems() {
        scope.launch {
            isLoading = true
            try {
                val result = inventoryRepository.getItems()
                result.onSuccess { itemsList ->
                    items = itemsList
                }.onFailure { error ->
                    snackbarHostState.showSnackbar("Failed to load items: ${error.message}")
                }
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Error: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }
    
    LaunchedEffect(Unit) {
        loadItems()
    }
    
    // Filter items by search
    val filteredItems = items.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
        it.category.contains(searchQuery, ignoreCase = true) ||
        (it.description?.contains(searchQuery, ignoreCase = true) ?: false)
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { 
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Manage Items")
                        Text(
                            text = "(${filteredItems.size})",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isDarkTheme) Color(0xFF1E1E1E) else Color(0xFFE6002A),
                    titleContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Header section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isDarkTheme) Color(0xFF1E1E1E) else Color.White)
                    .padding(16.dp)
            ) {
                // Add Item button and Search icon in same row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            selectedItem = null
                            showAddDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE6002A),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Item", color = Color.White)
                    }
                    
                    IconButton(
                        onClick = { showSearchDialog = !showSearchDialog }
                    ) {
                        Icon(
                            if (showSearchDialog) Icons.Default.Clear else Icons.Default.Search,
                            contentDescription = if (showSearchDialog) "Close Search" else "Search",
                            tint = if (isDarkTheme) Color.White else Color(0xFF1E1E1E)
                        )
                    }
                }
                
                // Search bar - shows below when search icon is clicked
                if (showSearchDialog) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search items...") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFE6002A),
                            focusedLabelColor = Color(0xFFE6002A)
                        ),
                        singleLine = true
                    )
                }
            }
            
            Divider()
            
            // Items table
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFFE6002A))
                }
            } else if (filteredItems.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.SearchOff,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            if (searchQuery.isEmpty()) "No items yet" else "No items found",
                            color = Color.Gray
                        )
                    }
                }
            } else {
                // Table rows
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredItems) { item ->
                        ItemTableRow(
                            item = item,
                            isDarkTheme = isDarkTheme,
                            onEdit = {
                                selectedItem = item
                                showAddDialog = true
                            },
                            onDelete = {
                                itemToDelete = item
                                showDeleteDialog = true
                            }
                        )
                    }
                }
            }
        }
    }
    
    // Add/Edit Dialog
    if (showAddDialog) {
        AddEditItemDialog(
            item = selectedItem,
            isDarkTheme = isDarkTheme,
            onDismiss = { showAddDialog = false },
            onSave = { itemData ->
                scope.launch {
                    try {
                        if (selectedItem != null) {
                            // Update item
                            val result = inventoryRepository.updateItem(selectedItem!!.id, itemData)
                            result.onSuccess {
                                showAddDialog = false
                                snackbarHostState.showSnackbar("Item updated successfully")
                                loadItems()
                            }.onFailure { error ->
                                snackbarHostState.showSnackbar("Failed to update: ${error.message}")
                            }
                        } else {
                            // Create item
                            val result = inventoryRepository.createItem(itemData)
                            result.onSuccess {
                                showAddDialog = false
                                snackbarHostState.showSnackbar("Item added successfully")
                                loadItems()
                            }.onFailure { error ->
                                snackbarHostState.showSnackbar("Failed to create: ${error.message}")
                            }
                        }
                    } catch (e: Exception) {
                        snackbarHostState.showSnackbar("Error: ${e.message}")
                    }
                }
            }
        )
    }
    
    // Delete Dialog
    if (showDeleteDialog && itemToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Item") },
            text = { Text("Are you sure you want to delete '${itemToDelete?.name}'? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            try {
                                val result = inventoryRepository.deleteItem(itemToDelete!!.id)
                                result.onSuccess {
                                    snackbarHostState.showSnackbar("Item deleted successfully")
                                    showDeleteDialog = false
                                    itemToDelete = null
                                    loadItems()
                                }.onFailure { error ->
                                    snackbarHostState.showSnackbar("Failed to delete: ${error.message}")
                                }
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("Error: ${e.message}")
                            }
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFEF4444))
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ItemTableRow(
    item: Item,
    isDarkTheme: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkTheme) Color(0xFF2D2D2D) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header row with image, name, and actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Item image and name
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (item.imageUrl != null) {
                        AsyncImage(
                            model = item.imageUrl,
                            contentDescription = item.name,
                            modifier = Modifier
                                .size(60.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFF0F0F0)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Inventory,
                                contentDescription = null,
                                modifier = Modifier.size(30.dp),
                                tint = Color.Gray
                            )
                        }
                    }
                    
                    Column {
                        Text(
                            text = item.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Created ${formatDate(item.createdAt)}",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
                
                // Action buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = Color(0xFF0EA5E9)
                        )
                    }
                    
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = Color(0xFFEF4444)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            Divider()
            Spacer(modifier = Modifier.height(12.dp))
            
            // Details section
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Category
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Category:",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                    Surface(
                        color = Color(0xFFE6002A).copy(alpha = 0.1f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = item.category,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            color = Color(0xFFE6002A),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                // Description
                if (item.description != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Description:",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.width(100.dp)
                        )
                        Text(
                            text = item.description,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                // Storage Temperature
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Storage Temp:",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                    if (item.storageTemperature != null) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.AcUnit,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = Color(0xFF0EA5E9)
                            )
                            Text(
                                text = "${item.storageTemperature}°C",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    } else {
                        Text(
                            text = "Not specified",
                            fontSize = 13.sp,
                            color = Color.Gray,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }
                }
                
                // Threshold
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Threshold Level:",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = Color(0xFFF59E0B)
                        )
                        Text(
                            text = item.thresholdLevel.toString(),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditItemDialog(
    item: Item?,
    isDarkTheme: Boolean,
    onDismiss: () -> Unit,
    onSave: (Map<String, Any?>) -> Unit
) {
    var name by remember { mutableStateOf(item?.name ?: "") }
    var category by remember { mutableStateOf(item?.category ?: "") }
    var description by remember { mutableStateOf(item?.description ?: "") }
    var imageUrl by remember { mutableStateOf(item?.imageUrl ?: "") }
    var storageTemperature by remember { mutableStateOf(item?.storageTemperature?.toString() ?: "") }
    var baseUnit by remember { mutableStateOf(item?.baseUnit ?: "piece") }
    var enablePackaging by remember { mutableStateOf(item?.enablePackaging ?: false) }
    var packagingUnit by remember { mutableStateOf(item?.packagingUnit ?: "") }
    var unitsPerPackage by remember { mutableStateOf(item?.unitsPerPackage?.toString() ?: "") }
    var thresholdLevel by remember { mutableStateOf(item?.thresholdLevel?.toString() ?: "") }
    var lowLevel by remember { mutableStateOf(item?.lowLevel?.toString() ?: "") }
    var criticalLevel by remember { mutableStateOf(item?.criticalLevel?.toString() ?: "") }
    
    var showCategoryDropdown by remember { mutableStateOf(false) }
    var showBaseUnitDropdown by remember { mutableStateOf(false) }
    var showPackagingUnitDropdown by remember { mutableStateOf(false) }
    
    val suppliers = listOf("Gronsakshuset", "Kvalitetsfisk", "Spendrups", "Tingstad", "Other")
    val baseUnits = listOf("piece", "kg", "gram", "liter", "ml")
    val packagingUnits = listOf("box", "carton", "case", "packet", "bag", "crate")
    
    var nameError by remember { mutableStateOf("") }
    var categoryError by remember { mutableStateOf("") }
    var thresholdError by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f),
            color = if (isDarkTheme) Color(0xFF333131) else Color.White,
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Sticky title - outside scroll
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 16.dp)
                ) {
                    Text(
                        text = if (item == null) "Add Item" else "Edit Item",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Divider()
                }
                
                // Scrollable content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
            
            // Basic Details
            Text("BASIC ITEM DETAILS", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
            
            OutlinedTextField(
                value = name,
                onValueChange = { name = it; nameError = "" },
                label = { Text("Item Name *") },
                modifier = Modifier.fillMaxWidth(),
                isError = nameError.isNotEmpty(),
                supportingText = if (nameError.isNotEmpty()) {{ Text(nameError, color = Color.Red) }} else null
            )
            
            ExposedDropdownMenuBox(
                expanded = showCategoryDropdown,
                onExpandedChange = { showCategoryDropdown = it }
            ) {
                OutlinedTextField(
                    value = category,
                    onValueChange = {},
                    label = { Text("Category *") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    readOnly = true,
                    isError = categoryError.isNotEmpty(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCategoryDropdown) },
                    colors = OutlinedTextFieldDefaults.colors()
                )
                ExposedDropdownMenu(
                    expanded = showCategoryDropdown,
                    onDismissRequest = { showCategoryDropdown = false }
                ) {
                    suppliers.forEach { supplier ->
                        DropdownMenuItem(
                            text = { Text(supplier) },
                            onClick = { 
                                category = supplier
                                categoryError = ""
                                showCategoryDropdown = false
                            }
                        )
                    }
                }
            }
            
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 3
            )
            
            OutlinedTextField(
                value = imageUrl,
                onValueChange = { imageUrl = it },
                label = { Text("Image URL") },
                modifier = Modifier.fillMaxWidth()
            )
            
            OutlinedTextField(
                value = storageTemperature,
                onValueChange = { storageTemperature = it },
                label = { Text("Storage Temperature (°C)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                leadingIcon = { Icon(Icons.Default.AcUnit, null) }
            )
            
            Text("UNIT OF MEASUREMENT", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
            
            ExposedDropdownMenuBox(
                expanded = showBaseUnitDropdown,
                onExpandedChange = { showBaseUnitDropdown = it }
            ) {
                OutlinedTextField(
                    value = baseUnit,
                    onValueChange = {},
                    label = { Text("Base Unit *") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showBaseUnitDropdown) }
                )
                ExposedDropdownMenu(
                    expanded = showBaseUnitDropdown,
                    onDismissRequest = { showBaseUnitDropdown = false }
                ) {
                    baseUnits.forEach { unit ->
                        DropdownMenuItem(
                            text = { Text(unit.replaceFirstChar { it.uppercase() }) },
                            onClick = { 
                                baseUnit = unit
                                showBaseUnitDropdown = false
                            }
                        )
                    }
                }
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = enablePackaging, onCheckedChange = { enablePackaging = it })
                Text("Enable Packaging", fontSize = 14.sp)
            }
            
            if (enablePackaging) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExposedDropdownMenuBox(
                        expanded = showPackagingUnitDropdown,
                        onExpandedChange = { showPackagingUnitDropdown = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = packagingUnit,
                            onValueChange = {},
                            label = { Text("Packaging Unit") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showPackagingUnitDropdown) }
                        )
                        ExposedDropdownMenu(
                            expanded = showPackagingUnitDropdown,
                            onDismissRequest = { showPackagingUnitDropdown = false }
                        ) {
                            packagingUnits.forEach { unit ->
                                DropdownMenuItem(
                                    text = { Text(unit.replaceFirstChar { it.uppercase() }) },
                                    onClick = { 
                                        packagingUnit = unit
                                        showPackagingUnitDropdown = false
                                    }
                                )
                            }
                        }
                    }
                    
                    OutlinedTextField(
                        value = unitsPerPackage,
                        onValueChange = { unitsPerPackage = it },
                        label = { Text("Units/Package") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            }
            
            Text("THRESHOLD DETAILS", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
            
            OutlinedTextField(
                value = thresholdLevel,
                onValueChange = { thresholdLevel = it; thresholdError = "" },
                label = { Text("Threshold Level *") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = thresholdError.isNotEmpty(),
                supportingText = if (thresholdError.isNotEmpty()) {{ Text(thresholdError, color = Color.Red) }} else null
            )
            
            OutlinedTextField(
                value = lowLevel,
                onValueChange = { lowLevel = it },
                label = { Text("Low Level (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            
            OutlinedTextField(
                value = criticalLevel,
                onValueChange = { criticalLevel = it },
                label = { Text("Critical Level (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }
            
        // Fixed buttons at bottom - outside scroll
        Divider()
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
                enabled = !isLoading
            ) {
                Text("Cancel")
            }
                
            Button(
                onClick = {
                    var hasError = false
                    if (name.isBlank()) { nameError = "Required"; hasError = true }
                    if (category.isBlank()) { categoryError = "Required"; hasError = true }
                    if (thresholdLevel.isBlank()) { thresholdError = "Required"; hasError = true }
                        
                    if (!hasError) {
                        isLoading = true
                        val data = mapOf(
                            "name" to name,
                            "category" to category,
                            "description" to description.ifBlank { null },
                            "image_url" to imageUrl.ifBlank { null },
                            "storage_temperature" to storageTemperature.toDoubleOrNull(),
                            "base_unit" to baseUnit,
                            "enable_packaging" to enablePackaging,
                            "packaging_unit" to if (enablePackaging) packagingUnit else null,
                            "units_per_package" to if (enablePackaging) unitsPerPackage.toIntOrNull() else null,
                            "threshold_level" to thresholdLevel.toInt(),
                            "low_level" to (lowLevel.toIntOrNull() ?: 5),
                            "critical_level" to (criticalLevel.toIntOrNull() ?: 2)
                        )
                        onSave(data)
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE6002A)),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = if (item == null) "Add" else "Update",
                        color = Color.White
                    )
                }
            }
        }
        }
        }
    }
}

private fun formatDate(dateString: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        val outputFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
        val date = inputFormat.parse(dateString)
        outputFormat.format(date ?: Date())
    } catch (e: Exception) {
        "Created"
    }
}
