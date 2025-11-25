package com.ims.android.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.DrawerState
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ims.android.presentation.navigation.Screen

@Composable
fun NavigationDrawerContent(
    navController: NavController,
    onCloseDrawer: () -> Unit
) {
    ModalDrawerSheet(
        modifier = Modifier.width(280.dp)
    ) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Business,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Stock Nexus",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Inventory Management",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Divider(modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(modifier = Modifier.height(8.dp))
        
        // Navigation Items
        val navigationItems = listOf(
            NavigationItem(Screen.Dashboard.route, "Dashboard", Icons.Default.Dashboard),
            NavigationItem(Screen.Inventory.route, "Inventory", Icons.Default.Inventory),
            NavigationItem(Screen.Users.route, "Users", Icons.Default.People),
            NavigationItem(Screen.Analytics.route, "Analytics", Icons.Default.Analytics),
            NavigationItem(Screen.Branches.route, "Branches", Icons.Default.Business),
        )
        
        navigationItems.forEach { item ->
            NavigationDrawerItem(
                label = { Text(item.title) },
                selected = false, // TODO: Implement current route detection
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.startDestinationId)
                        launchSingleTop = true
                    }
                    onCloseDrawer()
                },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title
                    )
                },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        Divider(modifier = Modifier.padding(horizontal = 16.dp))
        
        // Profile and Logout
        NavigationDrawerItem(
            label = { Text("Profile") },
            selected = false,
            onClick = {
                navController.navigate(Screen.Profile.route)
                onCloseDrawer()
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "Profile"
                )
            },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )
        
        NavigationDrawerItem(
            label = { Text("Logout") },
            selected = false,
            onClick = {
                // TODO: Implement logout
                onCloseDrawer()
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.ExitToApp,
                    contentDescription = "Logout"
                )
            },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

data class NavigationItem(
    val route: String,
    val title: String,
    val icon: ImageVector
)