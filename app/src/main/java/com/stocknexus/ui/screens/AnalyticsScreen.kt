package com.stocknexus.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stocknexus.data.api.ApiClient
import com.stocknexus.data.model.Item
import com.stocknexus.data.repository.InventoryRepository
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    inventoryRepository: InventoryRepository,
    modifier: Modifier = Modifier
) {
    var analyticsData by remember { mutableStateOf<ApiClient.AnalyticsResponse?>(null) }
    var usageData by remember { mutableStateOf<List<ApiClient.UsageAnalyticsItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedPeriod by remember { mutableStateOf("Daily") }
    var expandedPeriod by remember { mutableStateOf(false) }
    
    // Item Stock Usage Analysis
    var items by remember { mutableStateOf<List<Item>>(emptyList()) }
    var selectedItemId by remember { mutableStateOf<String?>(null) }
    var selectedItemName by remember { mutableStateOf("Select Item") }
    var itemUsageData by remember { mutableStateOf<List<ApiClient.UsageAnalyticsItem>>(emptyList()) }
    var itemUsagePeriod by remember { mutableStateOf("Daily") }
    var isLoadingItemUsage by remember { mutableStateOf(false) }
    var expandedItemDropdown by remember { mutableStateOf(false) }
    var expandedItemPeriod by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Load analytics data
    LaunchedEffect(Unit) {
        scope.launch {
            isLoading = true
            try {
                val result = inventoryRepository.getAnalyticsData()
                if (result.isSuccess) {
                    analyticsData = result.getOrThrow()
                }
                
                // Load items for dropdown
                val itemsResult = inventoryRepository.getItems()
                if (itemsResult.isSuccess) {
                    items = itemsResult.getOrThrow()
                }
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Error: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }
    
    // Load usage data when period changes
    LaunchedEffect(selectedPeriod) {
        scope.launch {
            try {
                val periodKey = selectedPeriod.lowercase()
                val result = inventoryRepository.getItemUsageAnalytics(periodKey)
                if (result.isSuccess) {
                    usageData = result.getOrThrow()
                }
            } catch (e: Exception) {
                android.util.Log.e("Analytics", "Error loading usage data", e)
            }
        }
    }
    
    // Process category data
    val categoryData = remember(analyticsData) {
        val categoryCount = mutableMapOf<String, Int>()
        analyticsData?.items?.forEach { item ->
            categoryCount[item.category] = (categoryCount[item.category] ?: 0) + 1
        }
        val colors = listOf(
            Color(0xFF3B82F6), Color(0xFFEF4444), Color(0xFF10B981),
            Color(0xFFF59E0B), Color(0xFF8B5CF6), Color(0xFFEC4899)
        )
        categoryCount.entries.mapIndexed { index, entry ->
            CategoryItem(entry.key, entry.value, colors[index % colors.size])
        }
    }
    
    // Process movement trends (last 7 days)
    val movementTrends = remember(analyticsData) {
        val movementsByDate = mutableMapOf<String, MovementData>()
        analyticsData?.movements?.forEach { movement ->
            val date = movement.created_at.substring(0, 10)
            val current = movementsByDate[date] ?: MovementData(date, 0, 0)
            movementsByDate[date] = when (movement.movement_type) {
                "in" -> current.copy(stockIn = current.stockIn + movement.quantity)
                "out" -> current.copy(stockOut = current.stockOut + movement.quantity)
                else -> current
            }
        }
        movementsByDate.values.toList().takeLast(7)
    }
    
    // Top items
    val topItems = remember(analyticsData) {
        val itemCounts = mutableMapOf<String, Int>()
        analyticsData?.movements?.forEach { movement ->
            movement.item_name?.let { name ->
                itemCounts[name] = (itemCounts[name] ?: 0) + 1
            }
        }
        itemCounts.entries
            .sortedByDescending { it.value }
            .take(5)
            .map { TopItem(it.key, it.value) }
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
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Page Title
                item {
                    Text(
                        text = "Analytics Dashboard",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // Key Metrics - 2x2 grid
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MetricCard(
                            title = "Total Items",
                            value = "${analyticsData?.totalItems ?: 0}",
                            subtitle = "Items in inventory",
                            icon = Icons.Default.Inventory,
                            color = Color(0xFF3B82F6),
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            title = "Low Stock",
                            value = "${analyticsData?.lowStockItems ?: 0}",
                            subtitle = "Items low on stock",
                            icon = Icons.Default.Warning,
                            color = Color(0xFFF59E0B),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MetricCard(
                            title = "Active Users",
                            value = "${analyticsData?.activeUsers ?: 0}",
                            subtitle = "Users this month",
                            icon = Icons.Default.Person,
                            color = Color(0xFF10B981),
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            title = "Movements",
                            value = "${analyticsData?.stockMovements ?: 0}",
                            subtitle = "This month",
                            icon = Icons.Default.TrendingUp,
                            color = Color(0xFF8B5CF6),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                // Charts Section - 2 columns
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Category Pie Chart
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Category Distribution",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                if (categoryData.isNotEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(200.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        InteractivePieChart(
                                            data = categoryData,
                                            onSegmentClick = { category ->
                                                scope.launch {
                                                    snackbarHostState.showSnackbar("${category.name}: ${category.value} items")
                                                }
                                            }
                                        )
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(200.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("No data", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                        
                        // Category Breakdown List
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Category Breakdown",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                if (categoryData.isNotEmpty()) {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        categoryData.forEach { category ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(12.dp)
                                                            .background(category.color, CircleShape)
                                                    )
                                                    Text(
                                                        text = category.name,
                                                        style = MaterialTheme.typography.bodySmall
                                                    )
                                                }
                                                Text(
                                                    text = "${category.value}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    Text("No data", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
                
                // Movement Trends Line Chart
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Stock Movement Trends (Last 7 Days)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            if (movementTrends.isNotEmpty()) {
                                InteractiveLineChart(
                                    data = movementTrends,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(250.dp),
                                    onPointClick = { data ->
                                        scope.launch {
                                            snackbarHostState.showSnackbar("${data.date}: In ${data.stockIn}, Out ${data.stockOut}")
                                        }
                                    }
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(250.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No movement data", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
                
                // Top Items Bar Chart
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Top 5 Most Active Items",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            if (topItems.isNotEmpty()) {
                                InteractiveBarChart(
                                    data = topItems,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(350.dp),
                                    onBarClick = { item ->
                                        scope.launch {
                                            snackbarHostState.showSnackbar("${item.name}: ${item.movements} movements")
                                        }
                                    }
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(350.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No data", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
                
                // Item Stock Usage Analysis Section
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.TrendingUp,
                                    "Usage Analysis",
                                    tint = Color(0xFF3B82F6),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Item Stock Usage Analysis",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Item Selection and Period
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Item Dropdown
                                ExposedDropdownMenuBox(
                                    expanded = expandedItemDropdown,
                                    onExpandedChange = { expandedItemDropdown = it },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    OutlinedTextField(
                                        value = selectedItemName,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Select Item") },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedItemDropdown) },
                                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .menuAnchor()
                                    )
                                    ExposedDropdownMenu(
                                        expanded = expandedItemDropdown,
                                        onDismissRequest = { expandedItemDropdown = false }
                                    ) {
                                        items.forEach { item ->
                                            DropdownMenuItem(
                                                text = { Text(item.name) },
                                                onClick = {
                                                    selectedItemId = item.id
                                                    selectedItemName = item.name
                                                    expandedItemDropdown = false
                                                }
                                            )
                                        }
                                    }
                                }
                                
                                // Period Dropdown and Generate Button Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Period Dropdown
                                    ExposedDropdownMenuBox(
                                        expanded = expandedItemPeriod,
                                        onExpandedChange = { expandedItemPeriod = it },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        OutlinedTextField(
                                            value = itemUsagePeriod,
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text("Period") },
                                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedItemPeriod) },
                                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .menuAnchor()
                                        )
                                        ExposedDropdownMenu(
                                            expanded = expandedItemPeriod,
                                            onDismissRequest = { expandedItemPeriod = false }
                                        ) {
                                            listOf("Daily", "Monthly", "Yearly").forEach { period ->
                                                DropdownMenuItem(
                                                    text = { Text(period) },
                                                    onClick = {
                                                        itemUsagePeriod = period
                                                        expandedItemPeriod = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                    
                                    // Generate Button
                                    Button(
                                        onClick = {
                                            if (selectedItemId != null) {
                                                scope.launch {
                                                    isLoadingItemUsage = true
                                                    try {
                                                        val periodKey = itemUsagePeriod.lowercase()
                                                        val result = inventoryRepository.getItemUsageAnalytics(periodKey, selectedItemId)
                                                        if (result.isSuccess) {
                                                            itemUsageData = result.getOrThrow()
                                                        }
                                                    } catch (e: Exception) {
                                                        snackbarHostState.showSnackbar("Error: ${e.message}")
                                                    } finally {
                                                        isLoadingItemUsage = false
                                                    }
                                                }
                                            }
                                        },
                                        enabled = selectedItemId != null && !isLoadingItemUsage,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFFE6002A),
                                            contentColor = Color.White
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(56.dp)
                                    ) {
                                        if (isLoadingItemUsage) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                color = Color.White,
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Text("Generate")
                                        }
                                    }
                                }
                            }
                            
                            // Show chart if data available
                            if (itemUsageData.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                Text(
                                    text = "$selectedItemName - Usage ($itemUsagePeriod)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE6002A)
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                InteractiveUsageBarChart(
                                    data = itemUsageData,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(280.dp),
                                    onBarClick = { usage ->
                                        scope.launch {
                                            snackbarHostState.showSnackbar("${usage.period}: ${usage.usage} units")
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
                
                // Stock Usage Comparison
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.CalendarMonth,
                                        "Calendar",
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Stock Usage ($selectedPeriod)",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // Period Dropdown
                            ExposedDropdownMenuBox(
                                expanded = expandedPeriod,
                                onExpandedChange = { expandedPeriod = it },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = selectedPeriod,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Select Period") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedPeriod) },
                                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor()
                                )
                                ExposedDropdownMenu(
                                    expanded = expandedPeriod,
                                    onDismissRequest = { expandedPeriod = false }
                                ) {
                                    listOf("Daily", "Monthly", "Yearly").forEach { period ->
                                        DropdownMenuItem(
                                            text = { Text(period) },
                                            onClick = {
                                                selectedPeriod = period
                                                expandedPeriod = false
                                            }
                                        )
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            if (usageData.isNotEmpty()) {
                                InteractiveUsageBarChart(
                                    data = usageData,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(250.dp),
                                    onBarClick = { usage ->
                                        scope.launch {
                                            snackbarHostState.showSnackbar("${usage.period}: ${usage.usage} units")
                                        }
                                    }
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(250.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No usage data", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
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
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

data class CategoryItem(val name: String, val value: Int, val color: Color)
data class MovementData(val date: String, val stockIn: Int, val stockOut: Int)
data class TopItem(val name: String, val movements: Int)

@Composable
fun InteractivePieChart(
    data: List<CategoryItem>,
    onSegmentClick: (CategoryItem) -> Unit
) {
    var selectedSegment by remember { mutableStateOf<Int?>(null) }
    
    Canvas(
        modifier = Modifier
            .size(180.dp)
            .clickable {
                // Handle click - calculate which segment was clicked
                // For simplicity, cycle through segments on click
                selectedSegment = ((selectedSegment ?: -1) + 1) % data.size
                onSegmentClick(data[selectedSegment!!])
            }
    ) {
        val total = data.sumOf { it.value }.toFloat()
        var startAngle = -90f
        val centerX = size.width / 2
        val centerY = size.height / 2
        val radius = size.minDimension / 2.5f
        
        data.forEachIndexed { index, item ->
            val sweepAngle = (item.value / total) * 360f
            
            drawArc(
                color = if (selectedSegment == index) item.color.copy(alpha = 1f) else item.color.copy(alpha = 0.8f),
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = true,
                topLeft = Offset(centerX - radius, centerY - radius),
                size = Size(radius * 2, radius * 2)
            )
            
            startAngle += sweepAngle
        }
    }
}

@Composable
fun InteractiveLineChart(
    data: List<MovementData>,
    modifier: Modifier = Modifier,
    onPointClick: (MovementData) -> Unit
) {
    var selectedPoint by remember { mutableStateOf<Int?>(null) }
    
    Canvas(modifier = modifier) {
        val padding = 80f
        val chartWidth = size.width - padding * 2
        val chartHeight = size.height - padding * 2
        
        if (data.isEmpty()) return@Canvas
        
        val maxValue = maxOf(
            data.maxOfOrNull { it.stockIn } ?: 1,
            data.maxOfOrNull { it.stockOut } ?: 1
        ).toFloat()
        
        val stepX = chartWidth / (data.size - 1).coerceAtLeast(1)
        
        // Draw gridlines
        val gridLines = 5
        for (i in 0..gridLines) {
            val y = padding + (i * chartHeight / gridLines)
            drawLine(
                color = Color.LightGray.copy(alpha = 0.3f),
                start = Offset(padding, y),
                end = Offset(padding + chartWidth, y),
                strokeWidth = 1f
            )
        }
        
        // Draw axes
        drawLine(
            color = Color.Gray,
            start = Offset(padding, padding + chartHeight),
            end = Offset(padding + chartWidth, padding + chartHeight),
            strokeWidth = 2f
        )
        drawLine(
            color = Color.Gray,
            start = Offset(padding, padding),
            end = Offset(padding, padding + chartHeight),
            strokeWidth = 2f
        )
        
        // Y-axis labels
        val paint = android.graphics.Paint().apply {
            textSize = 28f
            color = android.graphics.Color.GRAY
            textAlign = android.graphics.Paint.Align.RIGHT
        }
        
        for (i in 0..gridLines) {
            val value = (maxValue * (gridLines - i) / gridLines).toInt()
            val y = padding + (i * chartHeight / gridLines)
            drawContext.canvas.nativeCanvas.drawText(
                value.toString(),
                padding - 10f,
                y + 10f,
                paint
            )
        }
        
        // X-axis labels (dates)
        val xLabelPaint = android.graphics.Paint().apply {
            textSize = 24f
            color = android.graphics.Color.GRAY
            textAlign = android.graphics.Paint.Align.CENTER
        }
        
        data.forEachIndexed { index, item ->
            if (index % maxOf(1, data.size / 4) == 0 || index == data.size - 1) {
                val x = padding + index * stepX
                val label = item.date.substring(5) // MM-DD
                drawContext.canvas.nativeCanvas.drawText(
                    label,
                    x,
                    padding + chartHeight + 40f,
                    xLabelPaint
                )
            }
        }
        
        // Draw Stock In line (green)
        val inPath = Path()
        data.forEachIndexed { index, item ->
            val x = padding + index * stepX
            val y = padding + chartHeight - (item.stockIn / maxValue) * chartHeight
            if (index == 0) inPath.moveTo(x, y) else inPath.lineTo(x, y)
        }
        drawPath(inPath, color = Color(0xFF10B981), style = Stroke(width = 3f))
        
        // Draw Stock Out line (red)
        val outPath = Path()
        data.forEachIndexed { index, item ->
            val x = padding + index * stepX
            val y = padding + chartHeight - (item.stockOut / maxValue) * chartHeight
            if (index == 0) outPath.moveTo(x, y) else outPath.lineTo(x, y)
        }
        drawPath(outPath, color = Color(0xFFEF4444), style = Stroke(width = 3f))
        
        // Draw points
        data.forEachIndexed { index, item ->
            val x = padding + index * stepX
            val yIn = padding + chartHeight - (item.stockIn / maxValue) * chartHeight
            val yOut = padding + chartHeight - (item.stockOut / maxValue) * chartHeight
            
            drawCircle(
                color = Color(0xFF10B981),
                radius = if (selectedPoint == index) 8f else 6f,
                center = Offset(x, yIn)
            )
            drawCircle(
                color = Color(0xFFEF4444),
                radius = if (selectedPoint == index) 8f else 6f,
                center = Offset(x, yOut)
            )
        }
        
        // Legend - positioned at top with better spacing
        val legendY = padding - 40f
        val legendStartX = padding + 20f
        
        // Stock In legend
        drawCircle(
            color = Color(0xFF10B981),
            radius = 6f,
            center = Offset(legendStartX, legendY)
        )
        drawContext.canvas.nativeCanvas.drawText(
            "Stock In",
            legendStartX + 15f,
            legendY + 5f,
            android.graphics.Paint().apply {
                textSize = 28f
                color = android.graphics.Color.parseColor("#10B981")
                isFakeBoldText = true
            }
        )
        
        // Stock Out legend - moved to the right with proper spacing
        val stockOutX = legendStartX + 120f
        drawCircle(
            color = Color(0xFFEF4444),
            radius = 6f,
            center = Offset(stockOutX, legendY)
        )
        drawContext.canvas.nativeCanvas.drawText(
            "Stock Out",
            stockOutX + 15f,
            legendY + 5f,
            android.graphics.Paint().apply {
                textSize = 28f
                color = android.graphics.Color.parseColor("#EF4444")
                isFakeBoldText = true
            }
        )
    }
}

@Composable
fun InteractiveBarChart(
    data: List<TopItem>,
    modifier: Modifier = Modifier,
    onBarClick: (TopItem) -> Unit
) {
    var selectedBar by remember { mutableStateOf<Int?>(null) }
    
    Canvas(modifier = modifier) {
        val padding = 80f
        val bottomPadding = 120f
        val chartWidth = size.width - padding * 2
        val chartHeight = size.height - padding - bottomPadding
        
        if (data.isEmpty()) return@Canvas
        
        val maxValue = data.maxOfOrNull { it.movements }?.toFloat() ?: 1f
        val barWidth = chartWidth / data.size * 0.6f
        val spacing = chartWidth / data.size
        
        // Draw gridlines
        val gridLines = 5
        for (i in 0..gridLines) {
            val y = padding + (i * chartHeight / gridLines)
            drawLine(
                color = Color.LightGray.copy(alpha = 0.3f),
                start = Offset(padding, y),
                end = Offset(padding + chartWidth, y),
                strokeWidth = 1f
            )
        }
        
        // Draw axes
        drawLine(
            color = Color.Gray,
            start = Offset(padding, padding + chartHeight),
            end = Offset(padding + chartWidth, padding + chartHeight),
            strokeWidth = 2f
        )
        drawLine(
            color = Color.Gray,
            start = Offset(padding, padding),
            end = Offset(padding, padding + chartHeight),
            strokeWidth = 2f
        )
        
        // Y-axis labels
        val yLabelPaint = android.graphics.Paint().apply {
            textSize = 28f
            color = android.graphics.Color.GRAY
            textAlign = android.graphics.Paint.Align.RIGHT
        }
        
        for (i in 0..gridLines) {
            val value = (maxValue * (gridLines - i) / gridLines).toInt()
            val y = padding + (i * chartHeight / gridLines)
            drawContext.canvas.nativeCanvas.drawText(
                value.toString(),
                padding - 10f,
                y + 10f,
                yLabelPaint
            )
        }
        
        // Draw bars with labels
        val xLabelPaint = android.graphics.Paint().apply {
            textSize = 24f
            color = android.graphics.Color.GRAY
            textAlign = android.graphics.Paint.Align.CENTER
        }
        
        data.forEachIndexed { index, item ->
            val x = padding + index * spacing + (spacing - barWidth) / 2
            val barHeight = (item.movements / maxValue) * chartHeight
            val y = padding + chartHeight - barHeight
            
            drawRect(
                color = if (selectedBar == index) 
                    Color(0xFFE6002A) 
                else 
                    Color(0xFFE6002A).copy(alpha = 0.8f),
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight)
            )
            
            // Value on top of bar
            val valuePaint = android.graphics.Paint().apply {
                textSize = 26f
                color = android.graphics.Color.parseColor("#E6002A")
                textAlign = android.graphics.Paint.Align.CENTER
                isFakeBoldText = true
            }
            drawContext.canvas.nativeCanvas.drawText(
                item.movements.toString(),
                x + barWidth / 2,
                y - 8f,
                valuePaint
            )
            
            // Item name below x-axis (wrapped if too long)
            val centerX = x + barWidth / 2
            val nameWords = item.name.split(" ")
            if (nameWords.size > 2) {
                drawContext.canvas.nativeCanvas.drawText(
                    nameWords.take(2).joinToString(" "),
                    centerX,
                    padding + chartHeight + 35f,
                    xLabelPaint
                )
                drawContext.canvas.nativeCanvas.drawText(
                    nameWords.drop(2).joinToString(" "),
                    centerX,
                    padding + chartHeight + 60f,
                    xLabelPaint
                )
            } else {
                drawContext.canvas.nativeCanvas.drawText(
                    item.name,
                    centerX,
                    padding + chartHeight + 35f,
                    xLabelPaint
                )
            }
        }
        
        // Y-axis label
        drawContext.canvas.nativeCanvas.save()
        drawContext.canvas.nativeCanvas.rotate(-90f, 30f, size.height / 2)
        drawContext.canvas.nativeCanvas.drawText(
            "Movements",
            30f,
            size.height / 2,
            android.graphics.Paint().apply {
                textSize = 30f
                color = android.graphics.Color.GRAY
                textAlign = android.graphics.Paint.Align.CENTER
                isFakeBoldText = true
            }
        )
        drawContext.canvas.nativeCanvas.restore()
    }
}

@Composable
fun InteractiveUsageBarChart(
    data: List<ApiClient.UsageAnalyticsItem>,
    modifier: Modifier = Modifier,
    onBarClick: (ApiClient.UsageAnalyticsItem) -> Unit
) {
    var selectedBar by remember { mutableStateOf<Int?>(null) }
    
    Canvas(modifier = modifier) {
        val padding = 80f
        val bottomPadding = 100f
        val chartWidth = size.width - padding * 2
        val chartHeight = size.height - padding - bottomPadding
        
        if (data.isEmpty()) return@Canvas
        
        val maxValue = data.maxOfOrNull { it.usage }?.toFloat() ?: 1f
        val barWidth = chartWidth / data.size * 0.6f
        val spacing = chartWidth / data.size
        
        // Draw gridlines
        val gridLines = 5
        for (i in 0..gridLines) {
            val y = padding + (i * chartHeight / gridLines)
            drawLine(
                color = Color.LightGray.copy(alpha = 0.3f),
                start = Offset(padding, y),
                end = Offset(padding + chartWidth, y),
                strokeWidth = 1f
            )
        }
        
        // Draw axes
        drawLine(
            color = Color.Gray,
            start = Offset(padding, padding + chartHeight),
            end = Offset(padding + chartWidth, padding + chartHeight),
            strokeWidth = 2f
        )
        drawLine(
            color = Color.Gray,
            start = Offset(padding, padding),
            end = Offset(padding, padding + chartHeight),
            strokeWidth = 2f
        )
        
        // Y-axis labels
        val yLabelPaint = android.graphics.Paint().apply {
            textSize = 28f
            color = android.graphics.Color.GRAY
            textAlign = android.graphics.Paint.Align.RIGHT
        }
        
        for (i in 0..gridLines) {
            val value = (maxValue * (gridLines - i) / gridLines).toInt()
            val y = padding + (i * chartHeight / gridLines)
            drawContext.canvas.nativeCanvas.drawText(
                value.toString(),
                padding - 10f,
                y + 10f,
                yLabelPaint
            )
        }
        
        // Draw bars
        val xLabelPaint = android.graphics.Paint().apply {
            textSize = 24f
            color = android.graphics.Color.GRAY
            textAlign = android.graphics.Paint.Align.CENTER
        }
        
        data.forEachIndexed { index, item ->
            val x = padding + index * spacing + (spacing - barWidth) / 2
            val barHeight = (item.usage / maxValue) * chartHeight
            val y = padding + chartHeight - barHeight
            
            drawRect(
                color = if (selectedBar == index) 
                    Color(0xFF10B981) 
                else 
                    Color(0xFF10B981).copy(alpha = 0.8f),
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight)
            )
            
            // Value on top of bar
            val valuePaint = android.graphics.Paint().apply {
                textSize = 26f
                color = android.graphics.Color.parseColor("#10B981")
                textAlign = android.graphics.Paint.Align.CENTER
                isFakeBoldText = true
            }
            drawContext.canvas.nativeCanvas.drawText(
                item.usage.toString(),
                x + barWidth / 2,
                y - 8f,
                valuePaint
            )
            
            // Period label (x-axis)
            drawContext.canvas.nativeCanvas.drawText(
                item.period,
                x + barWidth / 2,
                padding + chartHeight + 35f,
                xLabelPaint
            )
        }
        
        // Y-axis label
        drawContext.canvas.nativeCanvas.save()
        drawContext.canvas.nativeCanvas.rotate(-90f, 30f, size.height / 2)
        drawContext.canvas.nativeCanvas.drawText(
            "Usage (Units)",
            30f,
            size.height / 2,
            android.graphics.Paint().apply {
                textSize = 30f
                color = android.graphics.Color.GRAY
                textAlign = android.graphics.Paint.Align.CENTER
                isFakeBoldText = true
            }
        )
        drawContext.canvas.nativeCanvas.restore()
    }
}
