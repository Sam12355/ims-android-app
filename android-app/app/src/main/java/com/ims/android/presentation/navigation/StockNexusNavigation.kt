package com.ims.android.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ims.android.presentation.components.NavigationDrawerContent
import com.ims.android.presentation.components.TopAppBar
import com.ims.android.presentation.dashboard.DashboardScreen
import com.ims.android.presentation.inventory.InventoryScreen
import com.ims.android.presentation.users.UsersScreen
import com.ims.android.presentation.analytics.AnalyticsScreen
import com.ims.android.presentation.branches.BranchesScreen
import com.ims.android.presentation.profile.ProfileScreen
import kotlinx.coroutines.launch

@Composable
fun StockNexusNavigation(
    navController: NavHostController = rememberNavController()
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            NavigationDrawerContent(
                navController = navController,
                onCloseDrawer = {
                    scope.launch {
                        drawerState.close()
                    }
                }
            )
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    onMenuClick = {
                        scope.launch {
                            drawerState.open()
                        }
                    }
                )
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Dashboard.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(Screen.Dashboard.route) {
                    DashboardScreen()
                }
                composable(Screen.Inventory.route) {
                    InventoryScreen()
                }
                composable(Screen.Users.route) {
                    UsersScreen()
                }
                composable(Screen.Analytics.route) {
                    AnalyticsScreen()
                }
                composable(Screen.Branches.route) {
                    BranchesScreen()
                }
                composable(Screen.Profile.route) {
                    ProfileScreen()
                }
            }
        }
    }
}

sealed class Screen(val route: String, val title: String) {
    object Dashboard : Screen("dashboard", "Dashboard")
    object Inventory : Screen("inventory", "Inventory")
    object Users : Screen("users", "Users")
    object Analytics : Screen("analytics", "Analytics")
    object Branches : Screen("branches", "Branches")
    object Profile : Screen("profile", "Profile")
}