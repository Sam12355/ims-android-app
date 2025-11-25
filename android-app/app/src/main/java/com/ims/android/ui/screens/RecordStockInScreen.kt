package com.ims.android.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ims.android.data.api.ApiClient
import com.ims.android.data.api.ApiClient.StockReceipt
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class RecordStockInViewModel(private val apiClient: ApiClient) : ViewModel() {
    var receipts by mutableStateOf<List<StockReceipt>>(emptyList())
        private set
    var isLoading by mutableStateOf(true)
        private set
    var isSubmitting by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set
    var successMessage by mutableStateOf<String?>(null)
        private set

    init {
        loadReceipts()
    }

    fun loadReceipts() {
        viewModelScope.launch {
            isLoading = true
            try {
                val result = apiClient.getReceipts()
                result.onSuccess { data ->
                    receipts = data.sortedByDescending { it.created_at }
                }
                result.onFailure {
                    errorMessage = "Failed to load receipts: ${it.message}"
                }
            } catch (e: Exception) {
                errorMessage = "Error loading receipts: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    fun submitReceipt(
        supplierName: String,
        remarks: String,
        fileUri: Uri,
        fileName: String,
        context: android.content.Context,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            isSubmitting = true
            errorMessage = null
            try {
                val result = apiClient.submitReceipt(
                    supplierName = supplierName,
                    remarks = remarks,
                    fileUri = fileUri,
                    fileName = fileName,
                    context = context
                )
                result.onSuccess {
                    successMessage = "Receipt submitted successfully"
                    loadReceipts()
                    onSuccess()
                }
                result.onFailure {
                    errorMessage = "Failed to submit receipt: ${it.message}"
                }
            } catch (e: Exception) {
                errorMessage = "Error submitting receipt: ${e.message}"
            } finally {
                isSubmitting = false
            }
        }
    }

    fun clearMessages() {
        errorMessage = null
        successMessage = null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordStockInScreen(
    apiClient: ApiClient
) {
    val viewModel: RecordStockInViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return RecordStockInViewModel(apiClient) as T
            }
        }
    )

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showDialog by remember { mutableStateOf(false) }
    var supplierName by remember { mutableStateOf("") }
    var supplierSelectedFromDropdown by remember { mutableStateOf(false) }
    var remarks by remember { mutableStateOf("") }
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var showSupplierDropdown by remember { mutableStateOf(false) }

    val supplierOptions = listOf(
        "Gronsakshuset",
        "Kvalitetsfisk",
        "Spendrups",
        "Tingstad",
        "Other"
    )

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedFileUri = it
            // Get file name from URI
            val cursor = context.contentResolver.query(it, null, null, null, null)
            cursor?.use { c ->
                if (c.moveToFirst()) {
                    val nameIndex = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0) {
                        selectedFileName = c.getString(nameIndex)
                    }
                }
            }
        }
    }

    // Show messages
    LaunchedEffect(viewModel.errorMessage) {
        viewModel.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(viewModel.successMessage) {
        viewModel.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showDialog = true },
                containerColor = Color(0xFFE6002A)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Record Stock In", tint = Color.White)
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Header
            Text(
                text = "Stock In Receipts",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Loading State
            if (viewModel.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                // Receipts List
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(viewModel.receipts) { receipt ->
                        ReceiptCard(receipt = receipt)
                    }

                    if (viewModel.receipts.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FilePresent,
                                        contentDescription = null,
                                        modifier = Modifier.size(64.dp),
                                        tint = MaterialTheme.colorScheme.outline
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "No receipts yet",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Tap + to submit your first receipt",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Submit Receipt Dialog
        if (showDialog) {
            AlertDialog(
                onDismissRequest = {
                    if (!viewModel.isSubmitting) {
                        showDialog = false
                        supplierName = ""
                        supplierSelectedFromDropdown = false
                        remarks = ""
                        selectedFileUri = null
                        selectedFileName = null
                    }
                },
                containerColor = Color.Black,
                titleContentColor = Color.White,
                textContentColor = Color.White,
                title = { 
                    Text(
                        "Submit Stock In Receipt",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    ) 
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Supplier Selection
                        Column {
                            Text(
                                text = "Supplier *",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            ExposedDropdownMenuBox(
                                expanded = showSupplierDropdown,
                                onExpandedChange = { 
                                    if (!supplierSelectedFromDropdown) {
                                        showSupplierDropdown = !showSupplierDropdown
                                    }
                                }
                            ) {
                                OutlinedTextField(
                                    value = supplierName,
                                    onValueChange = { 
                                        if (!supplierSelectedFromDropdown) {
                                            supplierName = it
                                            showSupplierDropdown = true
                                        }
                                    },
                                    readOnly = supplierSelectedFromDropdown,
                                    trailingIcon = {
                                        if (supplierSelectedFromDropdown) {
                                            IconButton(onClick = {
                                                supplierName = ""
                                                supplierSelectedFromDropdown = false
                                                showSupplierDropdown = true
                                            }) {
                                                Icon(Icons.Default.Clear, "Clear selection")
                                            }
                                        } else {
                                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = showSupplierDropdown)
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(),
                                    placeholder = { Text("Select or type supplier") },
                                    singleLine = true
                                )
                                if (!supplierSelectedFromDropdown) {
                                    val filteredOptions = supplierOptions.filter { 
                                        it.contains(supplierName, ignoreCase = true) 
                                    }
                                    if (filteredOptions.isNotEmpty()) {
                                        ExposedDropdownMenu(
                                            expanded = showSupplierDropdown,
                                            onDismissRequest = { showSupplierDropdown = false }
                                        ) {
                                            filteredOptions.forEach { option ->
                                                DropdownMenuItem(
                                                    text = { Text(option) },
                                                    onClick = {
                                                        supplierName = option
                                                        supplierSelectedFromDropdown = true
                                                        showSupplierDropdown = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // File Upload
                        Column {
                            Text(
                                text = "Receipt File *",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { filePickerLauncher.launch("*/*") },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Upload, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(selectedFileName ?: "Choose File")
                            }
                            if (selectedFileName != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Selected: $selectedFileName",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // Remarks
                        Column {
                            Text(
                                text = "Remarks (Optional)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = remarks,
                                onValueChange = { remarks = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp),
                                placeholder = { Text("Add any additional notes") },
                                maxLines = 4
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (supplierName.isNotBlank() && selectedFileUri != null && selectedFileName != null) {
                                viewModel.submitReceipt(
                                    supplierName = supplierName,
                                    remarks = remarks,
                                    fileUri = selectedFileUri!!,
                                    fileName = selectedFileName!!,
                                    context = context,
                                    onSuccess = {
                                        showDialog = false
                                        supplierName = ""
                                        supplierSelectedFromDropdown = false
                                        remarks = ""
                                        selectedFileUri = null
                                        selectedFileName = null
                                    }
                                )
                            }
                        },
                        enabled = supplierName.isNotBlank() && selectedFileUri != null && !viewModel.isSubmitting,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE6002A),
                            contentColor = Color.White
                        )
                    ) {
                        if (viewModel.isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text("Submit", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showDialog = false
                            supplierName = ""
                            supplierSelectedFromDropdown = false
                            remarks = ""
                            selectedFileUri = null
                            selectedFileName = null
                        },
                        enabled = !viewModel.isSubmitting
                    ) {
                        Text("Cancel", color = Color.White)
                    }
                }
            )
        }
    }
}

@Composable
fun ReceiptCard(receipt: StockReceipt) {
    val statusColor = when (receipt.status) {
        "approved" -> Color(0xFF4CAF50)
        "rejected" -> Color(0xFFD32F2F)
        else -> Color(0xFFFFA726)
    }

    val statusIcon = when (receipt.status) {
        "approved" -> Icons.Default.CheckCircle
        "rejected" -> Icons.Default.Cancel
        else -> Icons.Default.AccessTime
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = receipt.supplier_name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = receipt.status.replaceFirstChar { it.titlecase() },
                        style = MaterialTheme.typography.bodySmall,
                        color = statusColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // File Name
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AttachFile,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = receipt.receipt_file_name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Remarks
            if (receipt.remarks != null && receipt.remarks.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = receipt.remarks,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Date
            Text(
                text = formatDate(receipt.created_at),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )

            // Reviewed Info
            if (receipt.reviewed_at != null && receipt.reviewed_by_name != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Reviewed by ${receipt.reviewed_by_name} on ${formatDate(receipt.reviewed_at)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

private fun formatDate(dateString: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val outputFormat = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault())
        val date = inputFormat.parse(dateString)
        date?.let { outputFormat.format(it) } ?: dateString
    } catch (e: Exception) {
        dateString
    }
}
