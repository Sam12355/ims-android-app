package com.stocknexus.ui.components

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.stocknexus.data.model.StockItem
import com.stocknexus.data.repository.InventoryRepository
import kotlinx.coroutines.launch

@Composable
fun Base64Image(
    base64String: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    android.util.Log.d("Base64Image", "Input: isEmpty=${base64String.isNullOrEmpty()}, length=${base64String?.length}, starts with data:=${base64String?.startsWith("data:image")}")
    
    if (base64String.isNullOrEmpty()) {
        android.util.Log.d("Base64Image", "Showing placeholder - empty string")
        Box(
            modifier = modifier
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Inventory,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        return
    }

    val bitmap = remember(base64String) {
        try {
            android.util.Log.d("Base64Image", "Attempting to decode base64 string")
            // Remove the data:image/...;base64, prefix if present
            val base64Data = if (base64String.startsWith("data:image")) {
                val data = base64String.substring(base64String.indexOf(",") + 1)
                android.util.Log.d("Base64Image", "Stripped prefix, data length: ${data.length}")
                data
            } else {
                android.util.Log.d("Base64Image", "No prefix to strip, original length: ${base64String.length}")
                base64String
            }
            
            val decodedBytes = Base64.decode(base64Data, Base64.DEFAULT)
            android.util.Log.d("Base64Image", "Decoded ${decodedBytes.size} bytes")
            val bmp = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
            android.util.Log.d("Base64Image", "Bitmap created: ${bmp != null}, size: ${bmp?.width}x${bmp?.height}")
            bmp
        } catch (e: Exception) {
            android.util.Log.e("Base64Image", "Error decoding base64 image: ${e.message}", e)
            null
        }
    }

    if (bitmap != null) {
        android.util.Log.d("Base64Image", "Showing bitmap image")
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale
        )
    } else {
        android.util.Log.d("Base64Image", "Showing placeholder - bitmap failed to decode")
        Box(
            modifier = modifier
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Inventory,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

data class SearchResultItem(
    val id: String,
    val name: String,
    val category: String,
    val description: String?,
    val currentQuantity: Int,
    val thresholdLevel: Int,
    val lowLevel: Int?,
    val criticalLevel: Int?,
    val storageTemperature: Double?,
    val imageUrl: String?,
    val baseUnit: String?,
    val packagingUnit: String?,
    val unitsPerPackage: Int?,
    val enablePackaging: Boolean?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchDialog(
    onDismiss: () -> Unit,
    inventoryRepository: InventoryRepository,
    branchId: String
) {
    var searchTerm by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<SearchResultItem>>(emptyList()) }
    var selectedItem by remember { mutableStateOf<SearchResultItem?>(null) }
    var isSearching by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun searchItems() {
        if (searchTerm.length < 2) {
            searchResults = emptyList()
            return
        }

        scope.launch {
            isSearching = true
            try {
                val stockDataResult = inventoryRepository.getStockData()
                if (stockDataResult.isSuccess) {
                    val stockData = stockDataResult.getOrNull() ?: emptyList()
                    val filtered = stockData
                        .filter { item ->
                            val itemDetails = item.items
                            itemDetails?.branch_id == branchId &&
                            ((itemDetails.name?.contains(searchTerm, ignoreCase = true) == true) ||
                             (itemDetails.category?.contains(searchTerm, ignoreCase = true) == true))
                        }
                        .take(5)
                        .mapNotNull { item ->
                            val itemDetails = item.items ?: return@mapNotNull null
                            SearchResultItem(
                                id = item.item_id ?: return@mapNotNull null,
                                name = itemDetails.name ?: "",
                                category = itemDetails.category ?: "",
                                description = null,
                                currentQuantity = item.current_quantity,
                                thresholdLevel = itemDetails.threshold_level,
                                lowLevel = itemDetails.low_level,
                                criticalLevel = itemDetails.critical_level,
                                storageTemperature = null,
                                imageUrl = itemDetails.image_url,
                                baseUnit = itemDetails.base_unit,
                                packagingUnit = itemDetails.packaging_unit,
                                unitsPerPackage = itemDetails.units_per_package,
                                enablePackaging = itemDetails.enable_packaging
                            )
                        }
                    searchResults = filtered
                } else {
                    searchResults = emptyList()
                }
            } catch (e: Exception) {
                android.util.Log.e("SearchDialog", "Error searching items: ${e.message}", e)
                searchResults = emptyList()
            } finally {
                isSearching = false
            }
        }
    }

    LaunchedEffect(searchTerm) {
        searchItems()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF1A1A1A)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) {
                // Header with search field
                Surface(
                    color = Color(0xFF1A1A1A),
                    tonalElevation = 0.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Search Inventory",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.Close, "Close")
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = searchTerm,
                            onValueChange = { searchTerm = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Search inventory...") },
                            leadingIcon = {
                                Icon(Icons.Default.Search, "Search")
                            },
                            trailingIcon = {
                                if (searchTerm.isNotEmpty()) {
                                    IconButton(onClick = { searchTerm = "" }) {
                                        Icon(Icons.Default.Clear, "Clear")
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF0A0A0A),
                                unfocusedContainerColor = Color(0xFF0A0A0A),
                                disabledContainerColor = Color(0xFF0A0A0A),
                                focusedIndicatorColor = Color(0xFF2D3748),
                                unfocusedIndicatorColor = Color(0xFF2D3748)
                            )
                        )
                    }
                }

                // Results or detail view
                if (selectedItem == null) {
                    // Show search results
                    if (isSearching) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else if (searchTerm.length < 2) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "Type at least 2 characters to search",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else if (searchResults.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.SearchOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "No items found",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 400.dp),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(searchResults) { item ->
                                SearchResultCard(
                                    item = item,
                                    onClick = { selectedItem = item }
                                )
                            }
                        }
                    }
                } else {
                    // Show item details
                    selectedItem?.let { item ->
                        ItemDetailView(
                            item = item,
                            onBack = { selectedItem = null }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SearchResultCard(
    item: SearchResultItem,
    onClick: () -> Unit
) {
    val stockStatus = getStockStatus(
        current = item.currentQuantity,
        threshold = item.thresholdLevel,
        lowLevel = item.lowLevel,
        criticalLevel = item.criticalLevel
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0A0A0A)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Image
            if (!item.imageUrl.isNullOrEmpty()) {
                android.util.Log.d("SearchDialog", "Loading image: ${item.imageUrl.take(100)}...")
                Base64Image(
                    base64String = item.imageUrl,
                    contentDescription = item.name,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Inventory,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.category.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = stockStatus.icon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = stockStatus.color
                    )
                    Surface(
                        color = stockStatus.color,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = stockStatus.label,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                    Text(
                        text = "${item.currentQuantity} units",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun ItemDetailView(
    item: SearchResultItem,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // Back button
        Surface(
            color = Color(0xFF1A1A1A),
            tonalElevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, "Back")
                }
                Text(
                    text = "Item Details",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with image and basic info
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (!item.imageUrl.isNullOrEmpty()) {
                        Base64Image(
                            base64String = item.imageUrl,
                            contentDescription = item.name,
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Inventory,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = item.category.replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (item.description != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = item.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Stock Information
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Stock Information",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            StockInfoCard(
                                title = "Current Stock",
                                value = item.currentQuantity.toString(),
                                unit = item.baseUnit ?: "units",
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                            StockInfoCard(
                                title = "Threshold",
                                value = item.thresholdLevel.toString(),
                                unit = "",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (item.enablePackaging == true && item.unitsPerPackage != null && item.unitsPerPackage > 0) {
                            val packages = item.currentQuantity / item.unitsPerPackage
                            val remainder = item.currentQuantity % item.unitsPerPackage
                            
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF1E40AF).copy(alpha = 0.2f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = "Packaging",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color(0xFF1E40AF)
                                    )
                                    Text(
                                        text = "$packages ${item.packagingUnit ?: "packages"}",
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1E40AF)
                                    )
                                    if (remainder > 0) {
                                        Text(
                                            text = "+ $remainder ${item.baseUnit ?: "units"}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF1E40AF).copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                        }

                        if (item.lowLevel != null || item.criticalLevel != null) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                item.lowLevel?.let { low ->
                                    StockInfoCard(
                                        title = "Low Alert",
                                        value = low.toString(),
                                        unit = "",
                                        color = Color(0xFFEAB308),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                item.criticalLevel?.let { critical ->
                                    StockInfoCard(
                                        title = "Critical Alert",
                                        value = critical.toString(),
                                        unit = "",
                                        color = Color(0xFFEF4444),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Stock Status
            item {
                val stockStatus = getStockStatus(
                    current = item.currentQuantity,
                    threshold = item.thresholdLevel,
                    lowLevel = item.lowLevel,
                    criticalLevel = item.criticalLevel
                )

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Stock Status",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                imageVector = stockStatus.icon,
                                contentDescription = null,
                                tint = stockStatus.color,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                color = stockStatus.color,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = stockStatus.label,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = stockStatus.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Additional Info
            if (item.storageTemperature != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF3B82F6).copy(alpha = 0.2f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Storage Temperature",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF3B82F6)
                            )
                            Text(
                                text = "${item.storageTemperature}°C",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF3B82F6)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StockInfoCard(
    title: String,
    value: String,
    unit: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            if (unit.isNotEmpty()) {
                Text(
                    text = unit,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

data class StockStatus(
    val label: String,
    val color: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val description: String
)

fun getStockStatus(
    current: Int,
    threshold: Int,
    lowLevel: Int?,
    criticalLevel: Int?
): StockStatus {
    val criticalThreshold = criticalLevel ?: (threshold * 0.5).toInt()
    val lowThreshold = lowLevel ?: threshold

    return when {
        current <= criticalThreshold -> StockStatus(
            label = "Critical",
            color = Color(0xFFEF4444),
            icon = Icons.Default.Cancel,
            description = "Immediate restocking required"
        )
        current <= lowThreshold -> StockStatus(
            label = "Low",
            color = Color(0xFFEAB308),
            icon = Icons.Default.Warning,
            description = "Consider restocking soon"
        )
        else -> StockStatus(
            label = "Adequate",
            color = Color(0xFF10B981),
            icon = Icons.Default.CheckCircle,
            description = "Stock levels are adequate"
        )
    }
}
