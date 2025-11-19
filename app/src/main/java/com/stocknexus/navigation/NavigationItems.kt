package com.stocknexus.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.stocknexus.data.model.UserRole

/**
 * Navigation models matching the web application's AppSidebar.tsx
 */

data class NavigationItem(
    val title: String,
    val route: String,
    val icon: ImageVector,
    val allowedRoles: List<UserRole>,
    val description: String? = null
)

object AppDestinations {
    const val Dashboard = "dashboard"
    const val BranchManagement = "branch_management"
    const val Staff = "staff"
    const val Items = "items"
    const val Stock = "stock"
    const val ICADelivery = "ica_delivery"
    const val StockIn = "stock_in"
    const val RecordStockIn = "record_stock_in"
    const val Reports = "reports"
    const val Analytics = "analytics"
    const val ActivityLogs = "activity_logs"
    const val RegionManagement = "region_management"
    const val DistrictManagement = "district_management"
    const val MoveoutList = "moveout_list"
    const val Notifications = "notifications"
    const val Settings = "settings"
    const val Profile = "profile"
    const val Auth = "auth"
}

/**
 * Navigation items matching the web application's menu structure exactly
 */
val navigationItems = listOf(
    NavigationItem(
        title = "Dashboard",
        route = AppDestinations.Dashboard,
        icon = Icons.Default.Home,
        allowedRoles = listOf(UserRole.MANAGER, UserRole.ASSISTANT_MANAGER, UserRole.STAFF),
        description = "Overview of branch operations"
    ),
    NavigationItem(
        title = "Branch Management",
        route = AppDestinations.BranchManagement,
        icon = Icons.Default.Business,
        allowedRoles = listOf(UserRole.ADMIN),
        description = "Manage branches across regions"
    ),
    NavigationItem(
        title = "Manage Staff",
        route = AppDestinations.Staff,
        icon = Icons.Default.People,
        allowedRoles = listOf(UserRole.ADMIN, UserRole.MANAGER, UserRole.ASSISTANT_MANAGER),
        description = "Manage staff members and assignments"
    ),
    NavigationItem(
        title = "Manage Items",
        route = AppDestinations.Items,
        icon = Icons.Default.Inventory,
        allowedRoles = listOf(UserRole.MANAGER, UserRole.ASSISTANT_MANAGER),
        description = "Manage inventory items and categories"
    ),
    NavigationItem(
        title = "Stock Out",
        route = AppDestinations.Stock,
        icon = Icons.Default.RemoveShoppingCart,
        allowedRoles = listOf(UserRole.MANAGER, UserRole.ASSISTANT_MANAGER, UserRole.STAFF),
        description = "Record stock outgoing transactions"
    ),
    NavigationItem(
        title = "ICA Delivery",
        route = AppDestinations.ICADelivery,
        icon = Icons.Default.LocalShipping,
        allowedRoles = listOf(UserRole.MANAGER, UserRole.ASSISTANT_MANAGER),
        description = "Manage ICA delivery lists"
    ),
    NavigationItem(
        title = "Stock In",
        route = AppDestinations.StockIn,
        icon = Icons.Default.Add,
        allowedRoles = listOf(UserRole.MANAGER, UserRole.ASSISTANT_MANAGER),
        description = "Record stock incoming transactions"
    ),
    NavigationItem(
        title = "Record Stock In",
        route = AppDestinations.RecordStockIn,
        icon = Icons.Default.Upload,
        allowedRoles = listOf(UserRole.STAFF),
        description = "Record new stock arrivals"
    ),
    NavigationItem(
        title = "Reports",
        route = AppDestinations.Reports,
        icon = Icons.Default.AssignmentReturned,
        allowedRoles = listOf(UserRole.MANAGER, UserRole.ASSISTANT_MANAGER),
        description = "Generate and view reports"
    ),
    NavigationItem(
        title = "Analytics",
        route = AppDestinations.Analytics,
        icon = Icons.Default.BarChart,
        allowedRoles = listOf(UserRole.MANAGER, UserRole.ASSISTANT_MANAGER),
        description = "View analytics and trends"
    ),
    NavigationItem(
        title = "Activity Logs",
        route = AppDestinations.ActivityLogs,
        icon = Icons.Default.History,
        allowedRoles = listOf(UserRole.ADMIN, UserRole.MANAGER),
        description = "View system activity logs"
    ),
    NavigationItem(
        title = "Region Management",
        route = AppDestinations.RegionManagement,
        icon = Icons.Default.Public,
        allowedRoles = listOf(UserRole.ADMIN),
        description = "Manage regions and territories"
    ),
    NavigationItem(
        title = "District Management",
        route = AppDestinations.DistrictManagement,
        icon = Icons.Default.LocationCity,
        allowedRoles = listOf(UserRole.ADMIN),
        description = "Manage districts within regions"
    ),
    NavigationItem(
        title = "Moveout List",
        route = AppDestinations.MoveoutList,
        icon = Icons.Default.ListAlt,
        allowedRoles = listOf(UserRole.STAFF),
        description = "Manage moveout lists and requests"
    )
)

/**
 * Get navigation items filtered by user role
 */
fun getNavigationItemsForRole(userRole: UserRole): List<NavigationItem> {
    return navigationItems.filter { item ->
        item.allowedRoles.contains(userRole)
    }
}

/**
 * Check if a user has access to a specific route
 */
fun hasAccessToRoute(userRole: UserRole, route: String): Boolean {
    return navigationItems.find { it.route == route }?.allowedRoles?.contains(userRole) == true
}