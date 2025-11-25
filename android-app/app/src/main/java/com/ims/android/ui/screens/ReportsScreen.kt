package com.ims.android.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.graphics.pdf.PdfDocument
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.ims.android.data.api.ApiClient
import com.ims.android.data.repository.InventoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    inventoryRepository: InventoryRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedReport by remember { mutableStateOf("stock") }
    var stockReport by remember { mutableStateOf<List<ApiClient.StockReportItem>>(emptyList()) }
    var movementsReport by remember { mutableStateOf<List<ApiClient.MovementReportItem>>(emptyList()) }
    var softDrinksReport by remember { mutableStateOf<ApiClient.SoftDrinksReportResponse?>(null) }
    var isLoadingStock by remember { mutableStateOf(false) }
    var isLoadingMovements by remember { mutableStateOf(false) }
    var isLoadingSoftDrinks by remember { mutableStateOf(false) }
    var stockLoaded by remember { mutableStateOf(false) }
    var movementsLoaded by remember { mutableStateOf(false) }
    var softDrinksLoaded by remember { mutableStateOf(false) }
    var selectedWeeks by remember { mutableStateOf(4) }
    var selectedMonth by remember { mutableStateOf(Calendar.getInstance()) }
    var expandedMonth by remember { mutableStateOf(false) }
    var expandedWeeks by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Load report data based on selection
    LaunchedEffect(selectedReport, selectedWeeks) {
        scope.launch {
            when (selectedReport) {
                "stock" -> {
                    if (!stockLoaded) {
                        isLoadingStock = true
                        try {
                            val result = inventoryRepository.getStockReport()
                            if (result.isSuccess) {
                                stockReport = result.getOrThrow()
                                stockLoaded = true
                            } else {
                                snackbarHostState.showSnackbar("Error loading stock report")
                            }
                        } catch (e: Exception) {
                            snackbarHostState.showSnackbar("Error: ${e.message}")
                        } finally {
                            isLoadingStock = false
                        }
                    }
                }
                "movements" -> {
                    if (!movementsLoaded) {
                        isLoadingMovements = true
                        try {
                            val result = inventoryRepository.getMovementsReport()
                            if (result.isSuccess) {
                                movementsReport = result.getOrThrow()
                                movementsLoaded = true
                            } else {
                                snackbarHostState.showSnackbar("Error loading movements report")
                            }
                        } catch (e: Exception) {
                            snackbarHostState.showSnackbar("Error: ${e.message}")
                        } finally {
                            isLoadingMovements = false
                        }
                    }
                }
                "softdrinks" -> {
                    isLoadingSoftDrinks = true
                    try {
                        val result = inventoryRepository.getSoftDrinksReport(selectedWeeks)
                        if (result.isSuccess) {
                            softDrinksReport = result.getOrThrow()
                            softDrinksLoaded = true
                        } else {
                            snackbarHostState.showSnackbar("Error loading soft drinks report")
                        }
                    } catch (e: Exception) {
                        snackbarHostState.showSnackbar("Error: ${e.message}")
                    } finally {
                        isLoadingSoftDrinks = false
                    }
                }
            }
        }
    }
    
    // Filter movements by selected month
    val filteredMovements = remember(movementsReport, selectedMonth) {
        movementsReport.filter { movement ->
            try {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val movementDate = dateFormat.parse(movement.created_at.substring(0, 10))
                val calendar = Calendar.getInstance()
                calendar.time = movementDate ?: return@filter false
                
                calendar.get(Calendar.YEAR) == selectedMonth.get(Calendar.YEAR) &&
                calendar.get(Calendar.MONTH) == selectedMonth.get(Calendar.MONTH)
            } catch (e: Exception) {
                false
            }
        }
    }
    
    val totalStockIn = filteredMovements.filter { it.movement_type == "in" }.sumOf { it.quantity }
    val totalStockOut = filteredMovements.filter { it.movement_type == "out" }.sumOf { it.quantity }
    
    var expandedReportType by remember { mutableStateOf(false) }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                text = "Reports",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Report Type Dropdown Selector
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { expandedReportType = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Icon(
                        imageVector = when (selectedReport) {
                            "stock" -> Icons.Default.Inventory
                            "movements" -> Icons.Default.SwapVert
                            else -> Icons.Default.LocalDrink
                        },
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (selectedReport) {
                            "stock" -> "Stock Levels Report"
                            "movements" -> "Stock Movements Report"
                            else -> "Soft Drinks Weekly Report"
                        },
                        modifier = Modifier.weight(1f)
                    )
                    Icon(Icons.Default.ArrowDropDown, "Select Report")
                }
                DropdownMenu(
                    expanded = expandedReportType,
                    onDismissRequest = { expandedReportType = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Stock Levels Report") },
                        leadingIcon = { Icon(Icons.Default.Inventory, null) },
                        onClick = {
                            selectedReport = "stock"
                            expandedReportType = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Stock Movements Report") },
                        leadingIcon = { Icon(Icons.Default.SwapVert, null) },
                        onClick = {
                            selectedReport = "movements"
                            expandedReportType = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Soft Drinks Weekly Report") },
                        leadingIcon = { Icon(Icons.Default.LocalDrink, null) },
                        onClick = {
                            selectedReport = "softdrinks"
                            expandedReportType = false
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Filters Section
            if (selectedReport == "movements" || selectedReport == "softdrinks") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        // Month filter for movements
                        if (selectedReport == "movements") {
                            Text(
                                text = "Filter by Month",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Box {
                                OutlinedButton(
                                    onClick = { expandedMonth = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.onSurface
                                    )
                                ) {
                                    Icon(Icons.Default.CalendarMonth, "Calendar", modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(selectedMonth.time))
                                }
                                DropdownMenu(
                                expanded = expandedMonth,
                                onDismissRequest = { expandedMonth = false }
                            ) {
                                (0..11).forEach { monthOffset ->
                                    val cal = Calendar.getInstance()
                                    cal.add(Calendar.MONTH, -monthOffset)
                                    DropdownMenuItem(
                                        text = { Text(SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time)) },
                                        onClick = {
                                            selectedMonth = cal
                                            expandedMonth = false
                                        }
                                    )
                                }
                            }
                        }
                        }
                        
                        // Weeks filter for soft drinks
                        if (selectedReport == "softdrinks") {
                            Text(
                                text = "Time Period",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Box {
                                OutlinedButton(
                                    onClick = { expandedWeeks = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.onSurface
                                    )
                                ) {
                                    Icon(Icons.Default.DateRange, "Weeks", modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("$selectedWeeks weeks")
                                }
                                DropdownMenu(
                                    expanded = expandedWeeks,
                                    onDismissRequest = { expandedWeeks = false }
                                ) {
                                    listOf(2, 4, 8, 12).forEach { weeks ->
                                        DropdownMenuItem(
                                            text = { Text("$weeks weeks") },
                                            onClick = {
                                                selectedWeeks = weeks
                                                expandedWeeks = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Report Content
            when (selectedReport) {
                "stock" -> StockReportContent(
                    stockReport = stockReport,
                    isLoading = isLoadingStock && !stockLoaded,
                    snackbarHostState = snackbarHostState
                )
                "movements" -> MovementsReportContent(
                    movementsReport = filteredMovements,
                    isLoading = isLoadingMovements && !movementsLoaded,
                    totalStockIn = totalStockIn,
                    totalStockOut = totalStockOut,
                    selectedMonth = selectedMonth,
                    snackbarHostState = snackbarHostState
                )
                "softdrinks" -> SoftDrinksReportContent(
                    report = softDrinksReport,
                    isLoading = isLoadingSoftDrinks,
                    selectedWeeks = selectedWeeks,
                    snackbarHostState = snackbarHostState
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockReportContent(
    stockReport: List<ApiClient.StockReportItem>,
    isLoading: Boolean,
    snackbarHostState: SnackbarHostState
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    
    Card(
        modifier = Modifier.fillMaxSize(),
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
                Text(
                    text = "Current Stock Levels",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Export buttons - small and left aligned
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = {
                        scope.launch {
                            try {
                                val result = exportStockReportPDF(context, stockReport)
                                if (result.isSuccess) {
                                    shareFile(context, result.getOrThrow(), "application/pdf")
                                    snackbarHostState.showSnackbar("PDF exported!")
                                } else {
                                    snackbarHostState.showSnackbar("Export failed")
                                }
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("Error: ${e.message}")
                            }
                        }
                    },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color(0xFFE6002A),
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(Icons.Default.PictureAsPdf, "PDF", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("PDF", style = MaterialTheme.typography.labelMedium)
                }
                FilledTonalButton(
                    onClick = {
                        scope.launch {
                            try {
                                val result = exportStockReportExcel(context, stockReport)
                                if (result.isSuccess) {
                                    shareFile(context, result.getOrThrow(), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                                    snackbarHostState.showSnackbar("Excel exported!")
                                } else {
                                    snackbarHostState.showSnackbar("Export failed")
                                }
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("Error: ${e.message}")
                            }
                        }
                    },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color(0xFF10B981),
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(Icons.Default.TableChart, "Excel", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Excel", style = MaterialTheme.typography.labelMedium)
                }
                FilledTonalButton(
                    onClick = {
                        scope.launch {
                            try {
                                val result = exportStockReportCSV(context, stockReport)
                                if (result.isSuccess) {
                                    shareFile(context, result.getOrThrow())
                                    snackbarHostState.showSnackbar("Report exported successfully!")
                                } else {
                                    snackbarHostState.showSnackbar("Export failed")
                                }
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("Export error: ${e.message}")
                            }
                        }
                    },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color(0xFF3B82F6),
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(Icons.Default.Description, "CSV", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("CSV", style = MaterialTheme.typography.labelMedium)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (stockReport.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No stock data found",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(stockReport) { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Badge(
                                        containerColor = when (item.status) {
                                            "critical" -> Color(0xFFEF4444)
                                            "low" -> Color(0xFFF59E0B)
                                            else -> Color(0xFF10B981)
                                        }
                                    ) {
                                        Text(
                                            text = item.status.replaceFirstChar { it.uppercase() },
                                            color = Color.White,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                                
                                Divider()
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = "Category",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = item.category.replace("_", " ").replaceFirstChar { it.uppercase() },
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "Current Stock",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "${item.current_quantity}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovementsReportContent(
    movementsReport: List<ApiClient.MovementReportItem>,
    isLoading: Boolean,
    totalStockIn: Int,
    totalStockOut: Int,
    selectedMonth: Calendar,
    snackbarHostState: SnackbarHostState
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    
    Card(
        modifier = Modifier.fillMaxSize(),
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
                Text(
                    text = "Stock Movement History",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Export buttons - small and left aligned
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = {
                        scope.launch {
                            try {
                                val result = exportMovementsReportPDF(context, movementsReport)
                                if (result.isSuccess) {
                                    shareFile(context, result.getOrThrow(), "application/pdf")
                                    snackbarHostState.showSnackbar("PDF exported!")
                                } else {
                                    snackbarHostState.showSnackbar("Export failed")
                                }
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("Error: ${e.message}")
                            }
                        }
                    },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color(0xFFE6002A),
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(Icons.Default.PictureAsPdf, "PDF", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("PDF", style = MaterialTheme.typography.labelMedium)
                }
                FilledTonalButton(
                    onClick = {
                        scope.launch {
                            try {
                                val result = exportMovementsReportExcel(context, movementsReport)
                                if (result.isSuccess) {
                                    shareFile(context, result.getOrThrow(), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                                    snackbarHostState.showSnackbar("Excel exported!")
                                } else {
                                    snackbarHostState.showSnackbar("Export failed")
                                }
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("Error: ${e.message}")
                            }
                        }
                    },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color(0xFF10B981),
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(Icons.Default.TableChart, "Excel", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Excel", style = MaterialTheme.typography.labelMedium)
                }
                FilledTonalButton(
                    onClick = {
                        scope.launch {
                            try {
                                val result = exportMovementsReportCSV(context, movementsReport)
                                if (result.isSuccess) {
                                    shareFile(context, result.getOrThrow())
                                    snackbarHostState.showSnackbar("Report exported successfully!")
                                } else {
                                    snackbarHostState.showSnackbar("Export failed")
                                }
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("Export error: ${e.message}")
                            }
                        }
                    },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color(0xFF3B82F6),
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(Icons.Default.Description, "CSV", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("CSV", style = MaterialTheme.typography.labelMedium)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (movementsReport.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No movement data found for ${SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(selectedMonth.time)}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // Summary
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Badge(
                        containerColor = Color(0xFF10B981),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Total In: $totalStockIn", color = Color.White)
                    }
                    Badge(
                        containerColor = Color(0xFFEF4444),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Total Out: $totalStockOut", color = Color.White)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(movementsReport) { movement ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = movement.item_name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Badge(
                                        containerColor = if (movement.movement_type == "in") Color(0xFF10B981) else Color(0xFFEF4444)
                                    ) {
                                        Text(
                                            text = if (movement.movement_type == "in") "Stock In" else "Stock Out",
                                            color = Color.White,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                                
                                Divider()
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = "Date",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = try {
                                                SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                                                    .format(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(movement.created_at.substring(0, 10))!!)
                                            } catch (e: Exception) { movement.created_at.substring(0, 10) },
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "Quantity",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "${movement.quantity}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoftDrinksReportContent(
    report: ApiClient.SoftDrinksReportResponse?,
    isLoading: Boolean,
    selectedWeeks: Int,
    snackbarHostState: SnackbarHostState
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    
    Card(
        modifier = Modifier.fillMaxSize(),
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
                Text(
                    text = "Soft Drinks Weekly Comparison",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Export buttons - small and left aligned
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = {
                        scope.launch {
                            try {
                                if (report != null) {
                                    val result = exportSoftDrinksReportPDF(context, report)
                                    if (result.isSuccess) {
                                        shareFile(context, result.getOrThrow(), "application/pdf")
                                        snackbarHostState.showSnackbar("PDF exported!")
                                    } else {
                                        snackbarHostState.showSnackbar("Export failed")
                                    }
                                } else {
                                    snackbarHostState.showSnackbar("No data to export")
                                }
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("Error: ${e.message}")
                            }
                        }
                    },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color(0xFFE6002A),
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(Icons.Default.PictureAsPdf, "PDF", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("PDF", style = MaterialTheme.typography.labelMedium)
                }
                FilledTonalButton(
                    onClick = {
                        scope.launch {
                            try {
                                if (report != null) {
                                    val result = exportSoftDrinksReportExcel(context, report)
                                    if (result.isSuccess) {
                                        shareFile(context, result.getOrThrow(), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                                        snackbarHostState.showSnackbar("Excel exported!")
                                    } else {
                                        snackbarHostState.showSnackbar("Export failed")
                                    }
                                } else {
                                    snackbarHostState.showSnackbar("No data to export")
                                }
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("Error: ${e.message}")
                            }
                        }
                    },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color(0xFF10B981),
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(Icons.Default.TableChart, "Excel", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Excel", style = MaterialTheme.typography.labelMedium)
                }
                FilledTonalButton(
                    onClick = {
                        scope.launch {
                            try {
                                if (report != null) {
                                    val result = exportSoftDrinksReportCSV(context, report)
                                    if (result.isSuccess) {
                                        shareFile(context, result.getOrThrow())
                                        snackbarHostState.showSnackbar("Report exported successfully!")
                                    } else {
                                        snackbarHostState.showSnackbar("Export failed")
                                    }
                                } else {
                                    snackbarHostState.showSnackbar("No data to export")
                                }
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("Export error: ${e.message}")
                            }
                        }
                    },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color(0xFF3B82F6),
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(Icons.Default.Description, "CSV", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("CSV", style = MaterialTheme.typography.labelMedium)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (report == null || report.data.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No soft drinks data available",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Summary
                    report.summary?.let { summary ->
                        item {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "Summary ($selectedWeeks weeks)",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text("Total In", style = MaterialTheme.typography.bodySmall)
                                            Text(
                                                "${summary.total_stock_in}",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF10B981)
                                            )
                                        }
                                        Column {
                                            Text("Total Out", style = MaterialTheme.typography.bodySmall)
                                            Text(
                                                "${summary.total_stock_out}",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFEF4444)
                                            )
                                        }
                                        Column {
                                            Text("Net Change", style = MaterialTheme.typography.bodySmall)
                                            Text(
                                                "${summary.total_net_change}",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (summary.total_net_change >= 0) Color(0xFF10B981) else Color(0xFFEF4444)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    // Weekly data
                    items(report.data) { week ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Week ${report.data.indexOf(week) + 1}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${week.week_start.substring(0, 10)} to ${week.week_end.substring(0, 10)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("In: ${week.total_stock_in}", style = MaterialTheme.typography.bodySmall, color = Color(0xFF10B981))
                                    Text("Out: ${week.total_stock_out}", style = MaterialTheme.typography.bodySmall, color = Color(0xFFEF4444))
                                    Text("Net: ${week.total_net_change}", style = MaterialTheme.typography.bodySmall)
                                    Text(
                                        "Trend: ${week.overall_trend.uppercase()}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                
                                if (week.items.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Divider()
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    week.items.forEach { item ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = item.item_name,
                                                style = MaterialTheme.typography.bodySmall,
                                                modifier = Modifier.weight(2f)
                                            )
                                            Text(
                                                text = "In: ${item.stock_in}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color(0xFF10B981),
                                                modifier = Modifier.weight(1f)
                                            )
                                            Text(
                                                text = "Out: ${item.stock_out}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color(0xFFEF4444),
                                                modifier = Modifier.weight(1f)
                                            )
                                            Text(
                                                text = item.trend.uppercase(),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.weight(0.8f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Export helper functions
private suspend fun exportStockReportCSV(
    context: Context,
    report: List<ApiClient.StockReportItem>
): Result<Uri> = withContext(Dispatchers.IO) {
    try {
        val fileName = "stock_report_${System.currentTimeMillis()}.csv"
        val file = File(context.cacheDir, fileName)
        
        FileOutputStream(file).use { output ->
            val writer = output.bufferedWriter()
            
            // Header
            writer.write("Item Name,Category,Current Stock,Threshold,Status\n")
            
            // Data rows
            report.forEach { item ->
                writer.write("\"${item.name}\",\"${item.category}\",${item.current_quantity},${item.threshold_level},\"${item.status}\"\n")
            }
            
            writer.flush()
        }
        
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        
        Result.success(uri)
    } catch (e: Exception) {
        Result.failure(e)
    }
}

private suspend fun exportMovementsReportCSV(
    context: Context,
    report: List<ApiClient.MovementReportItem>
): Result<Uri> = withContext(Dispatchers.IO) {
    try {
        val fileName = "movements_report_${System.currentTimeMillis()}.csv"
        val file = File(context.cacheDir, fileName)
        
        FileOutputStream(file).use { output ->
            val writer = output.bufferedWriter()
            
            // Header
            writer.write("Date,Item,Type,Quantity,User\n")
            
            // Data rows
            report.forEach { item ->
                writer.write("\"${item.created_at}\",\"${item.item_name}\",\"${item.movement_type}\",${item.quantity},\"${item.user_name ?: "N/A"}\"\n")
            }
            
            writer.flush()
        }
        
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        
        Result.success(uri)
    } catch (e: Exception) {
        Result.failure(e)
    }
}

private suspend fun exportSoftDrinksReportCSV(
    context: Context,
    report: ApiClient.SoftDrinksReportResponse
): Result<Uri> = withContext(Dispatchers.IO) {
    try {
        val fileName = "softdrinks_report_${System.currentTimeMillis()}.csv"
        val file = File(context.cacheDir, fileName)
        
        FileOutputStream(file).use { output ->
            val writer = output.bufferedWriter()
            
            // Header
            writer.write("Week,Item,Stock In,Stock Out,Net Change,Trend\n")
            
            // Data rows
            report.data.forEach { week ->
                week.items.forEach { item ->
                    writer.write("\"${week.week_start}\",\"${item.item_name}\",${item.stock_in},${item.stock_out},${item.net_change},\"${item.trend}\"\n")
                }
            }
            
            writer.flush()
        }
        
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        
        Result.success(uri)
    } catch (e: Exception) {
        Result.failure(e)
    }
}

// PDF Export Functions
private suspend fun exportStockReportPDF(
    context: Context,
    report: List<ApiClient.StockReportItem>
): Result<Uri> = withContext(Dispatchers.IO) {
    try {
        val fileName = "stock_report_${System.currentTimeMillis()}.pdf"
        val file = File(context.cacheDir, fileName)
        
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas
        
        val titlePaint = Paint().apply {
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val headerPaint = Paint().apply {
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val textPaint = Paint().apply { textSize = 10f }
        
        var yPos = 50f
        canvas.drawText("Stock Report", 50f, yPos, titlePaint)
        yPos += 40f
        
        // Header
        canvas.drawText("Item", 50f, yPos, headerPaint)
        canvas.drawText("Category", 150f, yPos, headerPaint)
        canvas.drawText("Stock", 250f, yPos, headerPaint)
        canvas.drawText("Threshold", 320f, yPos, headerPaint)
        canvas.drawText("Status", 420f, yPos, headerPaint)
        yPos += 20f
        
        // Data rows
        report.forEach { item ->
            if (yPos > 800) { // New page if needed
                pdfDocument.finishPage(page)
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                yPos = 50f
            }
            canvas.drawText(item.name.take(15), 50f, yPos, textPaint)
            canvas.drawText(item.category, 150f, yPos, textPaint)
            canvas.drawText(item.current_quantity.toString(), 250f, yPos, textPaint)
            canvas.drawText(item.threshold_level.toString(), 320f, yPos, textPaint)
            canvas.drawText(item.status, 420f, yPos, textPaint)
            yPos += 15f
        }
        
        pdfDocument.finishPage(page)
        pdfDocument.writeTo(FileOutputStream(file))
        pdfDocument.close()
        
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        
        Result.success(uri)
    } catch (e: Exception) {
        Result.failure(e)
    }
}

private suspend fun exportMovementsReportPDF(
    context: Context,
    report: List<ApiClient.MovementReportItem>
): Result<Uri> = withContext(Dispatchers.IO) {
    try {
        val fileName = "movements_report_${System.currentTimeMillis()}.pdf"
        val file = File(context.cacheDir, fileName)
        
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas
        
        val titlePaint = Paint().apply {
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val headerPaint = Paint().apply {
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val textPaint = Paint().apply { textSize = 10f }
        
        var yPos = 50f
        canvas.drawText("Stock Movement History", 50f, yPos, titlePaint)
        yPos += 40f
        
        canvas.drawText("Date", 50f, yPos, headerPaint)
        canvas.drawText("Item", 150f, yPos, headerPaint)
        canvas.drawText("Type", 280f, yPos, headerPaint)
        canvas.drawText("Qty", 350f, yPos, headerPaint)
        canvas.drawText("User", 420f, yPos, headerPaint)
        yPos += 20f
        
        report.forEach { item ->
            if (yPos > 800) {
                pdfDocument.finishPage(page)
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                yPos = 50f
            }
            canvas.drawText(item.created_at.take(10), 50f, yPos, textPaint)
            canvas.drawText(item.item_name.take(15), 150f, yPos, textPaint)
            canvas.drawText(item.movement_type, 280f, yPos, textPaint)
            canvas.drawText(item.quantity.toString(), 350f, yPos, textPaint)
            canvas.drawText((item.user_name ?: "N/A").take(15), 420f, yPos, textPaint)
            yPos += 15f
        }
        
        pdfDocument.finishPage(page)
        pdfDocument.writeTo(FileOutputStream(file))
        pdfDocument.close()
        
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        
        Result.success(uri)
    } catch (e: Exception) {
        Result.failure(e)
    }
}

private suspend fun exportSoftDrinksReportPDF(
    context: Context,
    report: ApiClient.SoftDrinksReportResponse
): Result<Uri> = withContext(Dispatchers.IO) {
    try {
        val fileName = "softdrinks_report_${System.currentTimeMillis()}.pdf"
        val file = File(context.cacheDir, fileName)
        
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas
        
        val titlePaint = Paint().apply {
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val headerPaint = Paint().apply {
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val textPaint = Paint().apply { textSize = 10f }
        
        var yPos = 50f
        canvas.drawText("Soft Drinks Weekly Report", 50f, yPos, titlePaint)
        yPos += 40f
        
        canvas.drawText("Week", 50f, yPos, headerPaint)
        canvas.drawText("Item", 120f, yPos, headerPaint)
        canvas.drawText("In", 220f, yPos, headerPaint)
        canvas.drawText("Out", 270f, yPos, headerPaint)
        canvas.drawText("Net", 320f, yPos, headerPaint)
        canvas.drawText("Trend", 370f, yPos, headerPaint)
        yPos += 20f
        
        report.data.forEach { week ->
            week.items.forEach { item ->
                if (yPos > 800) {
                    pdfDocument.finishPage(page)
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas
                    yPos = 50f
                }
                canvas.drawText(week.week_start.take(10), 50f, yPos, textPaint)
                canvas.drawText(item.item_name.take(12), 120f, yPos, textPaint)
                canvas.drawText(item.stock_in.toString(), 220f, yPos, textPaint)
                canvas.drawText(item.stock_out.toString(), 270f, yPos, textPaint)
                canvas.drawText(item.net_change.toString(), 320f, yPos, textPaint)
                canvas.drawText(item.trend, 370f, yPos, textPaint)
                yPos += 15f
            }
        }
        
        pdfDocument.finishPage(page)
        pdfDocument.writeTo(FileOutputStream(file))
        pdfDocument.close()
        
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        
        Result.success(uri)
    } catch (e: Exception) {
        Result.failure(e)
    }
}

// Excel Export Functions
// Excel Export Functions (using CSV format with .xls extension)
private suspend fun exportStockReportExcel(
    context: Context,
    report: List<ApiClient.StockReportItem>
): Result<Uri> = withContext(Dispatchers.IO) {
    try {
        val fileName = "stock_report_${System.currentTimeMillis()}.xls"
        val file = File(context.cacheDir, fileName)
        
        FileOutputStream(file).use { output ->
            val writer = output.bufferedWriter()
            writer.write("Item Name\tCategory\tCurrent Stock\tThreshold\tStatus\n")
            report.forEach { item ->
                writer.write("${item.name}\t${item.category}\t${item.current_quantity}\t${item.threshold_level}\t${item.status}\n")
            }
            writer.flush()
        }
        
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        
        Result.success(uri)
    } catch (e: Exception) {
        Result.failure(e)
    }
}

private suspend fun exportMovementsReportExcel(
    context: Context,
    report: List<ApiClient.MovementReportItem>
): Result<Uri> = withContext(Dispatchers.IO) {
    try {
        val fileName = "movements_report_${System.currentTimeMillis()}.xls"
        val file = File(context.cacheDir, fileName)
        
        FileOutputStream(file).use { output ->
            val writer = output.bufferedWriter()
            writer.write("Date\tItem\tType\tQuantity\tUser\n")
            report.forEach { item ->
                writer.write("${item.created_at}\t${item.item_name}\t${item.movement_type}\t${item.quantity}\t${item.user_name ?: "N/A"}\n")
            }
            writer.flush()
        }
        
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        
        Result.success(uri)
    } catch (e: Exception) {
        Result.failure(e)
    }
}

private suspend fun exportSoftDrinksReportExcel(
    context: Context,
    report: ApiClient.SoftDrinksReportResponse
): Result<Uri> = withContext(Dispatchers.IO) {
    try {
        val fileName = "softdrinks_report_${System.currentTimeMillis()}.xls"
        val file = File(context.cacheDir, fileName)
        
        FileOutputStream(file).use { output ->
            val writer = output.bufferedWriter()
            writer.write("Week\tItem\tStock In\tStock Out\tNet Change\tTrend\n")
            report.data.forEach { week ->
                week.items.forEach { item ->
                    writer.write("${week.week_start}\t${item.item_name}\t${item.stock_in}\t${item.stock_out}\t${item.net_change}\t${item.trend}\n")
                }
            }
            writer.flush()
        }
        
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        
        Result.success(uri)
    } catch (e: Exception) {
        Result.failure(e)
    }
}

private fun shareFile(context: Context, uri: Uri, mimeType: String = "text/csv") {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Export Report"))
}
