package com.ims.android.presentation.dashboard

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ims.android.presentation.theme.SuccessColor
import com.ims.android.presentation.theme.WarningColor

@Composable
fun DashboardScreen() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Dashboard Overview",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // Stats Cards Row
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(dashboardStats) { stat ->
                    StatCard(
                        title = stat.title,
                        value = stat.value,
                        change = stat.change,
                        icon = stat.icon,
                        iconColor = stat.iconColor
                    )
                }
            }
        }

        // Quick Actions
        item {
            Text(
                text = "Quick Actions",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(quickActions) { action ->
                    QuickActionCard(
                        title = action.title,
                        icon = action.icon,
                        onClick = { /* TODO: Implement navigation */ }
                    )
                }
            }
        }

        // Recent Activity
        item {
            Text(
                text = "Recent Activity",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        items(recentActivities) { activity ->
            ActivityItem(activity = activity)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatCard(
    title: String,
    value: String,
    change: String,
    icon: ImageVector,
    iconColor: Color
) {
    Card(
        modifier = Modifier.width(160.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = change,
                style = MaterialTheme.typography.bodySmall,
                color = if (change.startsWith("+")) SuccessColor else MaterialTheme.colorScheme.error
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickActionCard(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.width(120.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun ActivityItem(activity: RecentActivity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = activity.icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = activity.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = activity.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = activity.timestamp,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Sample data
data class DashboardStat(
    val title: String,
    val value: String,
    val change: String,
    val icon: ImageVector,
    val iconColor: Color
)

data class QuickAction(
    val title: String,
    val icon: ImageVector
)

data class RecentActivity(
    val title: String,
    val description: String,
    val timestamp: String,
    val icon: ImageVector
)

private val dashboardStats = listOf(
    DashboardStat("Total Items", "1,234", "+12% this month", Icons.Default.Inventory, Color(0xFF2196F3)),
    DashboardStat("Low Stock", "23", "-5 from last week", Icons.Default.Warning, WarningColor),
    DashboardStat("Total Value", "$45.6K", "+8.2% this month", Icons.Default.AttachMoney, SuccessColor),
    DashboardStat("Active Users", "156", "+3 this week", Icons.Default.People, Color(0xFF2D3748))
)

private val quickActions = listOf(
    QuickAction("Add Item", Icons.Default.Inventory),
    QuickAction("Stock In", Icons.Default.TrendingUp),
    QuickAction("Stock Out", Icons.Default.ShoppingCart),
    QuickAction("Reports", Icons.Default.Analytics)
)

private val recentActivities = listOf(
    RecentActivity(
        "Stock In",
        "Added 50 units of Product A",
        "2 hours ago",
        Icons.Default.TrendingUp
    ),
    RecentActivity(
        "New User",
        "John Doe joined as Warehouse Manager",
        "4 hours ago",
        Icons.Default.People
    ),
    RecentActivity(
        "Stock Out",
        "Sold 25 units of Product B",
        "6 hours ago",
        Icons.Default.ShoppingCart
    )
)