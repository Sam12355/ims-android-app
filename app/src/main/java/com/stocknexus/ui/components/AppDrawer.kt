package com.stocknexus.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.stocknexus.data.model.Profile
import com.stocknexus.navigation.getNavigationItemsForRole
import com.stocknexus.navigation.AppDestinations
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDrawer(
    navController: NavController,
    userProfile: Profile?,
    onCloseDrawer: () -> Unit,
    onLogout: () -> Unit
) {
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val scrollState = rememberScrollState()

    ModalDrawerSheet(
        modifier = Modifier
            .width(280.dp)
            .background(Color(0xFF1E1E1E)),
        drawerContainerColor = Color(0xFF1E1E1E)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .background(Color(0xFF1E1E1E))
        ) {
            // Header with brand and user info
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E1E1E)),
                color = Color(0xFF1E1E1E)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    // Brand
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Store,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Stock Nexus",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // User Info
                    if (userProfile != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                modifier = Modifier.size(40.dp),
                                shape = androidx.compose.foundation.shape.CircleShape,
                                color = Color.White.copy(alpha = 0.2f)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (userProfile.photoUrl != null && userProfile.photoUrl.isNotEmpty()) {
                                        AsyncImage(
                                            model = userProfile.photoUrl,
                                            contentDescription = "Profile Picture",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.width(12.dp))
                            
                            Column {
                                Text(
                                    text = userProfile.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = userProfile.role.replaceFirstChar { it.titlecase() },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                                if (userProfile.branchName != null) {
                                    Text(
                                        text = userProfile.branchName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Navigation Items
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(vertical = 8.dp)
            ) {
                // Get navigation items based on user role
                val navigationItems = userProfile?.let { profile ->
                    getNavigationItemsForRole(profile.userRole)
                } ?: emptyList()

                navigationItems.forEach { navItem ->
                    NavigationDrawerItem(
                        label = {
                            Text(
                                text = navItem.title,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        icon = {
                            Icon(
                                imageVector = navItem.icon,
                                contentDescription = navItem.title
                            )
                        },
                        selected = currentRoute == navItem.route,
                        onClick = {
                            navController.navigate(navItem.route) {
                                launchSingleTop = true
                                restoreState = true
                                // Pop up to dashboard to avoid building a large stack
                                popUpTo("dashboard") {
                                    saveState = true
                                }
                            }
                            onCloseDrawer()
                        },
                        colors = androidx.compose.material3.NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = Color(0xFF2D3748),
                            selectedIconColor = Color.White,
                            selectedTextColor = Color.White,
                            unselectedContainerColor = Color.Transparent,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                    )
                }
            }

            // Bottom section with Settings and Logout
            Column(
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                Divider(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )

                // Settings
                NavigationDrawerItem(
                    label = {
                        Text(
                            text = "Settings",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    },
                    selected = currentRoute == AppDestinations.Settings,
                    onClick = {
                        navController.navigate(AppDestinations.Settings) {
                            launchSingleTop = true
                            restoreState = true
                            popUpTo("dashboard") {
                                saveState = true
                            }
                        }
                        onCloseDrawer()
                    },
                    colors = androidx.compose.material3.NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = Color(0xFF2D3748),
                        selectedIconColor = Color.White,
                        selectedTextColor = Color.White,
                        unselectedContainerColor = Color.Transparent,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                )

                // Logout
                NavigationDrawerItem(
                    label = {
                        Text(
                            text = "Logout",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Logout",
                            tint = MaterialTheme.colorScheme.error
                        )
                    },
                    selected = false,
                    onClick = {
                        onLogout()
                        onCloseDrawer()
                    },
                    colors = androidx.compose.material3.NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = Color(0xFF2D3748),
                        selectedIconColor = Color.White,
                        selectedTextColor = Color.White,
                        unselectedContainerColor = Color.Transparent,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                )
            }
        }
    }
}