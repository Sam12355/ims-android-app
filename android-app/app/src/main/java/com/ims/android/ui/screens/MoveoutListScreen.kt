package com.ims.android.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ims.android.data.api.ApiClient
import com.ims.android.data.model.MoveoutList
import com.ims.android.data.model.MoveoutItem
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MoveoutListViewModel(private val apiClient: ApiClient) : ViewModel() {
    var moveoutLists by mutableStateOf<List<MoveoutList>>(emptyList())
        private set
    var isLoading by mutableStateOf(true)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set
    var successMessage by mutableStateOf<String?>(null)
        private set
    
    var expandedListId by mutableStateOf<String?>(null)
        private set

    init {
        loadMoveoutLists()
    }

    fun loadMoveoutLists() {
        viewModelScope.launch {
            isLoading = true
            try {
                val result = apiClient.getMoveoutLists()
                result.onSuccess { lists ->
                    // Filter for staff - show only draft and active lists
                    moveoutLists = lists
                        .filter { it.status == "draft" || it.status == "active" }
                        .sortedByDescending { it.createdAt }
                }
                result.onFailure {
                    errorMessage = "Failed to load moveout lists: ${it.message}"
                }
            } catch (e: Exception) {
                errorMessage = "Error loading moveout lists: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    fun toggleExpanded(listId: String) {
        expandedListId = if (expandedListId == listId) null else listId
    }

    fun markItemAsCompleted(listId: String, itemId: String) {
        viewModelScope.launch {
            try {
                val result = apiClient.markMoveoutItemCompleted(listId, itemId)
                result.onSuccess {
                    successMessage = "Item marked as completed"
                    loadMoveoutLists()
                }
                result.onFailure {
                    errorMessage = "Failed to mark item: ${it.message}"
                }
            } catch (e: Exception) {
                errorMessage = "Error marking item: ${e.message}"
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
fun MoveoutListScreen(
    apiClient: ApiClient
) {
    val viewModel: MoveoutListViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return MoveoutListViewModel(apiClient) as T
            }
        }
    )

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

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
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Moveout Lists",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Complete your assigned moveout items",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                IconButton(onClick = { viewModel.loadMoveoutLists() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                }
            }

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
                // Moveout Lists
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(viewModel.moveoutLists) { moveoutList ->
                        MoveoutListCard(
                            moveoutList = moveoutList,
                            isExpanded = viewModel.expandedListId == moveoutList.id,
                            onToggleExpand = { viewModel.toggleExpanded(moveoutList.id ?: "") },
                            onMarkItemCompleted = { itemId ->
                                viewModel.markItemAsCompleted(moveoutList.id ?: "", itemId)
                            }
                        )
                    }

                    if (viewModel.moveoutLists.isEmpty()) {
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
                                        imageVector = Icons.Default.ListAlt,
                                        contentDescription = null,
                                        modifier = Modifier.size(64.dp),
                                        tint = MaterialTheme.colorScheme.outline
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "No moveout lists assigned",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Your manager will assign lists to you",
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
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoveoutListCard(
    moveoutList: MoveoutList,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onMarkItemCompleted: (String) -> Unit
) {
    val completedItems = moveoutList.items.count { it.status == "completed" }
    val totalItems = moveoutList.items.size
    val progress = if (totalItems > 0) completedItems.toFloat() / totalItems else 0f

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
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = moveoutList.title ?: "Moveout List",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    moveoutList.description?.let { desc ->
                        if (desc.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = desc,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                IconButton(onClick = onToggleExpand) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand"
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier.weight(1f),
                    color = Color(0xFF4CAF50)
                )
                Text(
                    text = "$completedItems/$totalItems",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Date and Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatDate(moveoutList.createdAt ?: ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
                
                Surface(
                    color = when (moveoutList.status) {
                        "completed" -> Color(0xFF4CAF50).copy(alpha = 0.2f)
                        "active" -> Color(0xFF2196F3).copy(alpha = 0.2f)
                        else -> Color(0xFFFFA726).copy(alpha = 0.2f)
                    },
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = (moveoutList.status ?: "draft").replaceFirstChar { it.titlecase() },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = when (moveoutList.status) {
                            "completed" -> Color(0xFF4CAF50)
                            "active" -> Color(0xFF2196F3)
                            else -> Color(0xFFFFA726)
                        }
                    )
                }
            }

            // Expanded Items List
            if (isExpanded) {
                Spacer(modifier = Modifier.height(16.dp))
                Divider()
                Spacer(modifier = Modifier.height(16.dp))
                
                moveoutList.items.forEach { item ->
                    MoveoutItemRow(
                        item = item,
                        onMarkCompleted = { onMarkItemCompleted(item.id ?: "") }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun MoveoutItemRow(
    item: MoveoutItem,
    onMarkCompleted: () -> Unit
) {
    val isCompleted = item.status == "completed"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Checkbox
            if (isCompleted) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Completed",
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(24.dp)
                )
            } else {
                IconButton(
                    onClick = onMarkCompleted,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.RadioButtonUnchecked,
                        contentDescription = "Mark Complete",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.itemName ?: "Unknown Item",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${item.requestAmount ?: 0} units",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (isCompleted) {
            Surface(
                color = Color(0xFF4CAF50).copy(alpha = 0.2f),
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = "Done",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF4CAF50)
                )
            }
        }
    }
}

private fun formatDate(dateString: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val outputFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val date = inputFormat.parse(dateString)
        date?.let { outputFormat.format(it) } ?: dateString
    } catch (e: Exception) {
        dateString
    }
}
