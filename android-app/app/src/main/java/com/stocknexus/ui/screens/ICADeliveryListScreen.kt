package com.stocknexus.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stocknexus.data.api.ApiClient
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// ViewModel for ICA Delivery List
class ICADeliveryListViewModel(private val apiClient: ApiClient) : ViewModel() {
    var records by mutableStateOf<List<ApiClient.ICADeliveryRecord>>(emptyList())
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)
    var successMessage by mutableStateOf<String?>(null)
    
    var startDate by mutableStateOf("")
    var endDate by mutableStateOf("")
    
    init {
        // Set default date range: first day of month to today
        val today = Calendar.getInstance()
        val firstDayOfMonth = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
        }
        
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        startDate = dateFormat.format(firstDayOfMonth.time)
        endDate = dateFormat.format(today.time)
        
        loadRecords()
    }
    
    fun loadRecords() {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            
            try {
                val result = apiClient.getICADeliveryRecords(startDate, endDate)
                result.onSuccess {
                    records = it
                }
                result.onFailure {
                    errorMessage = "Failed to load records: ${it.message}"
                }
            } finally {
                isLoading = false
            }
        }
    }
    
    fun deleteRecord(recordIds: List<Int>) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            
            try {
                recordIds.forEach { id ->
                    val result = apiClient.deleteICADeliveryRecord(id)
                    result.onFailure {
                        errorMessage = "Failed to delete record: ${it.message}"
                        return@launch
                    }
                }
                
                successMessage = "Record deleted successfully"
                records = records.filter { !recordIds.contains(it.id) }
            } finally {
                isLoading = false
            }
        }
    }
    
    fun clearFilters() {
        startDate = ""
        endDate = ""
        loadRecords()
    }
    
    fun clearMessages() {
        errorMessage = null
        successMessage = null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ICADeliveryListScreen() {
    val context = LocalContext.current
    val apiClient = remember { ApiClient.getInstance(context) }
    val viewModel: ICADeliveryListViewModel = remember { ICADeliveryListViewModel(apiClient) }
    
    var showDeleteDialog by remember { mutableStateOf(false) }
    var recordToDelete by remember { mutableStateOf<List<Int>>(emptyList()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var groupToEdit by remember { mutableStateOf<ICADeliveryGroup?>(null) }
    var currentUserName by remember { mutableStateOf("Current User") }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    
    // Get current user name
    LaunchedEffect(Unit) {
        val user = apiClient.getCurrentUser()
        currentUserName = user?.name ?: "Current User"
    }
    
    // Auto-dismiss success message
    LaunchedEffect(viewModel.successMessage) {
        if (viewModel.successMessage != null) {
            kotlinx.coroutines.delay(3000)
            viewModel.clearMessages()
        }
    }
    
    // Group records by user, time of day, and submission date
    val groupedRecords = remember(viewModel.records) {
        // Define the correct order for items
        val itemOrder = listOf(
            "Salmon and Rolls",
            "Combo",
            "Salmon and Avocado Rolls",
            "Vegan Combo",
            "Goma Wakame"
        )
        
        viewModel.records.groupBy {
            val submissionDate = try {
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                    .parse(it.submitted_at)?.let { date ->
                        SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).format(date)
                    } ?: it.submitted_at
            } catch (e: Exception) {
                it.submitted_at
            }
            "${it.user_name}-${it.time_of_day}-$submissionDate"
        }.map { (key, records) ->
            // Sort items by the predefined order
            val sortedItems = records
                .map { ICADeliveryItem(it.type, it.amount, it.id) }
                .sortedBy { item -> itemOrder.indexOf(item.type).takeIf { it >= 0 } ?: Int.MAX_VALUE }
            
            ICADeliveryGroup(
                userName = records.first().user_name,
                timeOfDay = records.first().time_of_day,
                submittedAt = records.first().submitted_at,
                items = sortedItems,
                recordIds = records.map { it.id }
            )
        }
    }
    
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Color(0xFFD32F2F)
            ) {
                Icon(Icons.Default.Add, "Add ICA Delivery", tint = Color.White)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // Header
            Text(
                text = "ICA Delivery Records",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
            )
            
            // Error Message
            viewModel.errorMessage?.let { error ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(error, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
            
            // Success Message
            viewModel.successMessage?.let { success ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF10B981).copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF10B981))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(success, color = Color(0xFF10B981))
                    }
                }
            }
            
            // Filter Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Filter by Date Range", fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = viewModel.startDate,
                            onValueChange = { viewModel.startDate = it },
                            label = { Text("Start Date") },
                            placeholder = { Text("YYYY-MM-DD") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            trailingIcon = {
                                IconButton(onClick = { showStartDatePicker = true }) {
                                    Icon(Icons.Default.CalendarToday, "Pick date")
                                }
                            }
                        )
                        
                        OutlinedTextField(
                            value = viewModel.endDate,
                            onValueChange = { viewModel.endDate = it },
                            label = { Text("End Date") },
                            placeholder = { Text("YYYY-MM-DD") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            trailingIcon = {
                                IconButton(onClick = { showEndDatePicker = true }) {
                                    Icon(Icons.Default.CalendarToday, "Pick date")
                                }
                            }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.loadRecords() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFD32F2F)
                            )
                        ) {
                            Icon(Icons.Default.FilterList, null, modifier = Modifier.size(18.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Filter", color = Color.White)
                        }
                        
                        OutlinedButton(
                            onClick = { viewModel.clearFilters() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Clear, null, modifier = Modifier.size(18.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Clear", color = Color.White)
                        }
                    }
                }
            }
            
            // Records List
            if (viewModel.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (groupedRecords.isEmpty()) {
                Card(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No ICA delivery records found",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(groupedRecords) { group ->
                        ICADeliveryGroupCard(
                            group = group,
                            onDelete = {
                                recordToDelete = group.recordIds
                                showDeleteDialog = true
                            },
                            onEdit = {
                                groupToEdit = group
                                showEditDialog = true
                            }
                        )
                    }
                }
            }
        }
    }
    
    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            icon = { Icon(Icons.Default.Delete, null) },
            title = { Text("Delete ICA Delivery Record") },
            text = { Text("Are you sure you want to delete this record? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteRecord(recordToDelete)
                        showDeleteDialog = false
                        recordToDelete = emptyList()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD32F2F),
                        contentColor = Color.White
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Add ICA Delivery Dialog
    if (showAddDialog) {
        ICADeliveryDialog(
            onDismiss = { 
                showAddDialog = false
                // Reload records silently after adding
                viewModel.loadRecords()
            },
            icaDeliveryRepository = com.stocknexus.data.repository.ICADeliveryRepository(apiClient),
            userName = currentUserName
        )
    }
    
    // Edit ICA Delivery Dialog
    if (showEditDialog && groupToEdit != null) {
        // Convert group items to entries
        val initialEntries = listOf(
            com.stocknexus.ui.screens.ICADeliveryEntry("Salmon and Rolls", groupToEdit!!.items.find { it.type == "Salmon and Rolls" }?.amount?.toString() ?: "", groupToEdit!!.timeOfDay),
            com.stocknexus.ui.screens.ICADeliveryEntry("Combo", groupToEdit!!.items.find { it.type == "Combo" }?.amount?.toString() ?: "", groupToEdit!!.timeOfDay),
            com.stocknexus.ui.screens.ICADeliveryEntry("Salmon and Avocado Rolls", groupToEdit!!.items.find { it.type == "Salmon and Avocado Rolls" }?.amount?.toString() ?: "", groupToEdit!!.timeOfDay),
            com.stocknexus.ui.screens.ICADeliveryEntry("Vegan Combo", groupToEdit!!.items.find { it.type == "Vegan Combo" }?.amount?.toString() ?: "", groupToEdit!!.timeOfDay),
            com.stocknexus.ui.screens.ICADeliveryEntry("Goma Wakame", groupToEdit!!.items.find { it.type == "Goma Wakame" }?.amount?.toString() ?: "", groupToEdit!!.timeOfDay)
        )
        
        ICADeliveryEditDialog(
            onDismiss = { 
                showEditDialog = false
                groupToEdit = null
            },
            onSave = {
                // First delete the old records
                viewModel.deleteRecord(groupToEdit!!.recordIds)
                showEditDialog = false
                groupToEdit = null
                // Reload to show updated list
                viewModel.loadRecords()
            },
            icaDeliveryRepository = com.stocknexus.data.repository.ICADeliveryRepository(apiClient),
            userName = groupToEdit!!.userName,
            initialEntries = initialEntries
        )
    }
    
    // Start Date Picker
    if (showStartDatePicker) {
        val calendar = Calendar.getInstance()
        try {
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(viewModel.startDate)
            if (date != null) calendar.time = date
        } catch (e: Exception) { }
        
        android.app.DatePickerDialog(
            context,
            android.R.style.Theme_Holo_Light_Dialog_NoActionBar,
            { _, year, month, dayOfMonth ->
                viewModel.startDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
                showStartDatePicker = false
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            setOnCancelListener { showStartDatePicker = false }
            show()
        }
    }
    
    // End Date Picker
    if (showEndDatePicker) {
        val calendar = Calendar.getInstance()
        try {
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(viewModel.endDate)
            if (date != null) calendar.time = date
        } catch (e: Exception) { }
        
        android.app.DatePickerDialog(
            context,
            android.R.style.Theme_Holo_Light_Dialog_NoActionBar,
            { _, year, month, dayOfMonth ->
                viewModel.endDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
                showEndDatePicker = false
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            setOnCancelListener { showEndDatePicker = false }
            show()
        }
    }
}

data class ICADeliveryGroup(
    val userName: String,
    val timeOfDay: String,
    val submittedAt: String,
    val items: List<ICADeliveryItem>,
    val recordIds: List<Int>
)

data class ICADeliveryItem(
    val type: String,
    val amount: Int,
    val id: Int
)

@Composable
fun ICADeliveryGroupCard(
    group: ICADeliveryGroup,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = group.userName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = Color(0xFF10B981).copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = group.timeOfDay,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF10B981)
                            )
                        }
                        
                        Text(
                            text = try {
                                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                                inputFormat.timeZone = TimeZone.getTimeZone("UTC")
                                val date = inputFormat.parse(group.submittedAt)
                                val outputFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                                outputFormat.format(date!!)
                            } catch (e: Exception) {
                                group.submittedAt
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Items (names only, numbers are on the right)
                    group.items.forEach { item ->
                        Text(
                            text = item.type,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
                
                // Icons and numbers column aligned to the right
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                        IconButton(onClick = onEdit) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = onDelete) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = Color(0xFFD32F2F)
                            )
                        }
                    }
                    
                    // Numbers aligned with delete icon
                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        group.items.forEach { item ->
                            Text(
                                text = item.amount.toString(),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ICADeliveryEditDialog(
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    icaDeliveryRepository: com.stocknexus.data.repository.ICADeliveryRepository,
    userName: String,
    initialEntries: List<com.stocknexus.ui.screens.ICADeliveryEntry>
) {
    var entries by remember { mutableStateOf(initialEntries) }
    var timeOfDay by remember { mutableStateOf(initialEntries.firstOrNull()?.timeOfDay ?: "Morning") }
    var showTimeDropdown by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val scope = rememberCoroutineScope()
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit ICA Delivery") },
        text = {
            Column {
                // Time of Day Selector
                Text("Time of Day", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Box {
                    OutlinedButton(
                        onClick = { showTimeDropdown = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(timeOfDay)
                        Icon(Icons.Default.ArrowDropDown, null)
                    }
                    DropdownMenu(
                        expanded = showTimeDropdown,
                        onDismissRequest = { showTimeDropdown = false }
                    ) {
                        listOf("Morning", "Afternoon").forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    timeOfDay = option
                                    entries = entries.map { it.copy(timeOfDay = option) }
                                    showTimeDropdown = false
                                }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Items
                entries.forEachIndexed { index, entry ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = entry.type,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        OutlinedTextField(
                            value = entry.amount,
                            onValueChange = { newValue ->
                                entries = entries.toMutableList().apply {
                                    this[index] = entry.copy(amount = newValue)
                                }
                            },
                            modifier = Modifier.width(80.dp),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                
                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage!!,
                        color = Color(0xFFEF4444),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    scope.launch {
                        isLoading = true
                        errorMessage = null
                        
                        try {
                            val validEntries = entries.filter { it.amount.isNotBlank() }
                            if (validEntries.isEmpty()) {
                                errorMessage = "Please fill in at least one entry"
                                isLoading = false
                                return@launch
                            }
                            
                            val result = icaDeliveryRepository.submitICADelivery(
                                userName = userName,
                                entries = validEntries
                            )
                            
                            if (result.isSuccess) {
                                onSave()
                            } else {
                                errorMessage = result.exceptionOrNull()?.message ?: "Failed to update"
                            }
                        } catch (e: Exception) {
                            errorMessage = e.message ?: "An error occurred"
                        } finally {
                            isLoading = false
                        }
                    }
                },
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
                    Text("Save", color = Color.White)
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text("Cancel", color = Color.White)
            }
        }
    )
}
