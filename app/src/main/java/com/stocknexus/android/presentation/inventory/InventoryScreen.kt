package com.stocknexus.android.presentation.inventory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.stocknexus.android.presentation.theme.SuccessColor
import com.stocknexus.android.presentation.theme.WarningColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen() {
    var searchQuery by remember { mutableStateOf("") }
    
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* TODO: Add new item */ },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Item"
                )
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
                text = "Inventory Management",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // Search and Filter
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search items...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    },
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { /* TODO: Filter */ }) {
                    Icon(Icons.Default.FilterList, contentDescription = "Filter")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Inventory List
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(sampleInventoryItems) { item ->
                    InventoryItemCard(item = item)
                }
            }
        }
    }
}

@Composable
fun InventoryItemCard(item: InventoryItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "SKU: ${item.sku}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Category: ${item.category}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$${item.price}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${item.quantity} units",
                        style = MaterialTheme.typography.bodyMedium,
                        color = when {
                            item.quantity < 10 -> MaterialTheme.colorScheme.error
                            item.quantity < 50 -> WarningColor
                            else -> SuccessColor
                        }
                    )
                }
            }
        }
    }
}

data class InventoryItem(
    val id: String,
    val name: String,
    val sku: String,
    val category: String,
    val quantity: Int,
    val price: Double,
    val minStock: Int
)

private val sampleInventoryItems = listOf(
    InventoryItem("1", "Laptop Dell XPS 13", "LT-001", "Electronics", 25, 899.99, 10),
    InventoryItem("2", "Office Chair", "CH-002", "Furniture", 8, 299.99, 5),
    InventoryItem("3", "Wireless Mouse", "MS-003", "Electronics", 150, 29.99, 20),
    InventoryItem("4", "Standing Desk", "DK-004", "Furniture", 3, 799.99, 5),
    InventoryItem("5", "Monitor 27\"", "MN-005", "Electronics", 45, 329.99, 15),
)