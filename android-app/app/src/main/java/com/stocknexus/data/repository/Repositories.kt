package com.stocknexus.data.repository

import com.stocknexus.data.api.ApiClient
import com.stocknexus.data.model.*
import kotlinx.coroutines.flow.Flow

/**
 * Repository classes implementing the same patterns as the web application
 */

class AuthRepository(private val apiClient: ApiClient) {
    
    suspend fun login(email: String, password: String): Result<AuthResponse> {
        return apiClient.login(email, password)
    }
    
    suspend fun signup(request: SignUpRequest): Result<AuthResponse> {
        return apiClient.signup(request)
    }
    
    suspend fun logout() {
        apiClient.logout()
    }
    
    fun isLoggedIn(): Flow<Boolean> {
        return apiClient.isLoggedIn()
    }
    
    suspend fun getCurrentUser(): User? {
        return apiClient.getCurrentUser()
    }
    
    suspend fun getCurrentProfile(): Profile? {
        return apiClient.getCurrentProfile()
    }
}

class DashboardRepository(private val apiClient: ApiClient) {
    
    suspend fun getUserProfile(): Profile? {
        return apiClient.getCurrentProfile()
    }
    
    suspend fun getBranches(): Result<List<Branch>> {
        return apiClient.getBranches()
    }
    
    suspend fun getDashboardStats(): Result<DashboardStats> {
        return try {
            // Fetch stock data from backend
            val stockResult = apiClient.getStockData()
            if (stockResult.isFailure) {
                return Result.failure(stockResult.exceptionOrNull() ?: Exception("Failed to fetch stock data"))
            }
            val stockData = stockResult.getOrNull() ?: emptyList()
            
            // Get current user's branch
            val profile = apiClient.getCurrentProfile()
            val userBranchId = profile?.branchId
            
            // Filter by branch for non-admin users and remove null items
            val filteredStock = stockData
                .filter { it.items != null }
                .filter { 
                    if (!userBranchId.isNullOrEmpty()) {
                        it.items?.branch_id == userBranchId
                    } else {
                        true
                    }
                }
            
            // Calculate stock statistics (matching web app logic)
            val totalItems = filteredStock.size
            val criticalStock = filteredStock.filter { stock ->
                stock.items?.threshold_level?.let { threshold ->
                    stock.current_quantity <= (threshold * 0.5).toInt()
                } ?: false
            }
            val lowStock = filteredStock.filter { stock ->
                stock.items?.threshold_level?.let { threshold ->
                    stock.current_quantity > (threshold * 0.5).toInt() && 
                    stock.current_quantity <= threshold
                } ?: false
            }
            
            // Fetch staff count
            val staffResult = apiClient.getStaff()
            val staffCount = staffResult.getOrNull()?.size ?: 0
            
            // Fetch recent activities
            val activitiesResult = apiClient.getActivityLogs()
            val activities = activitiesResult.getOrNull()?.map {
                val resolvedName = it.profiles?.name
                    ?: it.user_name
                    ?: it.user_email
                    ?: "Unknown User"
                ActivityLog(
                    id = it.id,
                    action = it.action,
                    createdAt = it.created_at,
                    userId = it.user_id,
                    details = it.details?.toString(),
                    profiles = ProfileReference(name = resolvedName)
                )
            } ?: emptyList()
            
            val stats = DashboardStats(
                totalItems = totalItems,
                lowStockItems = lowStock.size,
                criticalStockItems = criticalStock.size,
                thresholdStockItems = lowStock.size, // Same as lowStock for now
                totalStaff = staffCount,
                recentActivities = activities
            )
            
            Result.success(stats)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getWeatherData(): Result<WeatherData> {
        return try {
            // Get current user profile to determine branch
            val profile = apiClient.getCurrentProfile()
            val branchId = profile?.branchId ?: profile?.branchContext
            
            android.util.Log.d("DashboardRepository", "🌤️ Fetching weather for branch: $branchId")
            
            var city = ""
            
            if (branchId != null) {
                // Fetch branches to find current branch address
                val branchesResult = apiClient.getBranches()
                if (branchesResult.isSuccess) {
                    val branches = branchesResult.getOrNull() ?: emptyList()
                    val branch = branches.find { it.id == branchId }
                    
                    android.util.Log.d("DashboardRepository", "🏢 Found branch: ${branch?.name}")
                    
                    if (branch?.address != null) {
                        // Extract city from address - prioritize Swedish city names
                        val addressParts = branch.address.split(",")
                        android.util.Log.d("DashboardRepository", "📍 Address parts: $addressParts")
                        
                        // Look for Swedish city names (Växjö, Stockholm, Gothenburg, Malmö)
                        for (part in addressParts) {
                            val trimmed = part.trim()
                            android.util.Log.d("DashboardRepository", "📍 Checking part: $trimmed")
                            
                            if (trimmed.contains("Växjö") || trimmed.contains("Stockholm") || 
                                trimmed.contains("Gothenburg") || trimmed.contains("Malmö")) {
                                city = trimmed.replace(Regex("[.,]"), "").trim()
                                android.util.Log.d("DashboardRepository", "📍 Found Swedish city: $city")
                                break
                            }
                        }
                        
                        // If no Swedish city found, use the second-to-last part (usually the city)
                        if (city.isEmpty() && addressParts.size > 1) {
                            city = addressParts[addressParts.size - 2].trim().replace(Regex("[.,]"), "")
                            android.util.Log.d("DashboardRepository", "📍 Using second-to-last part: $city")
                        }
                        
                        // Fallback to first part if still no city
                        if (city.isEmpty()) {
                            city = addressParts[0].trim().replace(Regex("[.,]"), "")
                            android.util.Log.d("DashboardRepository", "📍 Using first part as fallback: $city")
                        }
                        
                        android.util.Log.d("DashboardRepository", "📍 Using branch address: ${branch.address} -> extracted city: $city")
                    } else {
                        android.util.Log.d("DashboardRepository", "⚠️ Branch address is empty, using branch name as fallback")
                        city = branch?.name ?: ""
                    }
                }
            }
            
            if (city.isEmpty()) {
                android.util.Log.d("DashboardRepository", "⚠️ No branch location available, defaulting to Vaxjo")
                city = "Vaxjo"
            }
            
            android.util.Log.d("DashboardRepository", "🌤️ Starting weather fetch for city: $city")
            
            // Fetch weather data for the extracted city
            val weatherResult = apiClient.getWeather(city)
            
            if (weatherResult.isSuccess) {
                val weather = weatherResult.getOrNull()
                android.util.Log.d("DashboardRepository", "✅ Weather data fetched for: $city")
                android.util.Log.d("DashboardRepository", "🌤️ Temperature: ${weather?.temperature}°C, Condition: ${weather?.condition}")
                Result.success(weather!!)
            } else {
                android.util.Log.e("DashboardRepository", "❌ Failed to fetch weather: ${weatherResult.exceptionOrNull()?.message}")
                Result.failure(weatherResult.exceptionOrNull() ?: Exception("Failed to fetch weather"))
            }
        } catch (e: Exception) {
            android.util.Log.e("DashboardRepository", "❌ Error fetching weather data", e)
            Result.failure(e)
        }
    }
}

class InventoryRepository(private val apiClient: ApiClient) {
    
    suspend fun getItems(): Result<List<Item>> {
        return apiClient.getItems()
    }
    
    suspend fun getStockData(): Result<List<ApiClient.StockItem>> {
        return apiClient.getStockData() // Using real API with item details
    }
    
    suspend fun getReceipts(): Result<List<ApiClient.StockReceipt>> {
        return apiClient.getReceipts()
    }
    
    // Reports methods
    suspend fun getStockReport(): Result<List<ApiClient.StockReportItem>> {
        return apiClient.getStockReport()
    }
    
    suspend fun getMovementsReport(): Result<List<ApiClient.MovementReportItem>> {
        return apiClient.getMovementsReport()
    }
    
    suspend fun getSoftDrinksReport(weeks: Int = 4): Result<ApiClient.SoftDrinksReportResponse> {
        return apiClient.getSoftDrinksReport(weeks)
    }
    
    // Analytics methods
    suspend fun getAnalyticsData(): Result<ApiClient.AnalyticsResponse> {
        return apiClient.getAnalyticsData()
    }
    
    suspend fun getItemUsageAnalytics(period: String = "daily", itemId: String? = null): Result<List<ApiClient.UsageAnalyticsItem>> {
        return apiClient.getItemUsageAnalytics(period, itemId)
    }
    
    suspend fun getActivityLogs(): Result<List<ActivityLog>> {
        return try {
            val result = apiClient.getActivityLogs()
            if (result.isSuccess) {
                val activities = result.getOrNull()?.map {
                    val resolvedName = it.profiles?.name
                        ?: it.user_name
                        ?: it.user_email
                        ?: "Unknown User"
                    ActivityLog(
                        id = it.id,
                        action = it.action,
                        createdAt = it.created_at,
                        userId = it.user_id,
                        details = it.details?.toString(),
                        profiles = ProfileReference(name = resolvedName)
                    )
                } ?: emptyList()
                Result.success(activities)
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("Failed to fetch activity logs"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateStockQuantity(
        itemId: String,
        movementType: String,
        quantity: Int,
        reason: String? = null,
        unitType: String = "base",
        unitQuantity: Int = quantity,
        unitLabel: String = "piece"
    ): Result<ApiClient.StockItem> {
        return apiClient.updateStockQuantity(itemId, movementType, quantity, reason, unitType, unitQuantity, unitLabel)
    }
    
    suspend fun initializeStock(): Result<ApiClient.InitializeStockResponse> {
        return apiClient.initializeStock()
    }
    
    suspend fun createItem(request: Map<String, Any?>): Result<Item> {
        return apiClient.createItem(request)
    }
    
    suspend fun updateItem(itemId: String, request: Map<String, Any?>): Result<Item> {
        return apiClient.updateItem(itemId, request)
    }
    
    suspend fun deleteItem(itemId: String): Result<Boolean> {
        return apiClient.deleteItem(itemId)
    }
    
    suspend fun createStockIn(request: CreateStockInRequest): Result<StockIn> {
        // TODO: Implement stock in creation
        return Result.failure(Exception("Not implemented"))
    }
    
    suspend fun createStockOut(request: CreateStockOutRequest): Result<StockMovement> {
        // TODO: Implement stock out creation
        return Result.failure(Exception("Not implemented"))
    }
}

class MoveoutRepository(private val apiClient: ApiClient) {
    
    suspend fun getMoveoutLists(): Result<List<MoveoutList>> {
        return apiClient.getMoveoutLists()
    }
    
    suspend fun createMoveoutList(request: CreateMoveoutListRequest): Result<MoveoutList> {
        return apiClient.createMoveoutList(request)
    }
    
    suspend fun processMoveoutItem(listId: String, itemId: String, quantity: Int, userName: String): Result<Unit> {
        return apiClient.processMoveoutItem(listId, itemId, quantity, userName)
    }
    
    suspend fun completeMoveoutItem(listId: String, itemId: String, request: CompleteMoveoutItemRequest): Result<MoveoutItem> {
        // TODO: Implement moveout item completion
        return Result.failure(Exception("Not implemented"))
    }
    
    suspend fun deleteMoveoutList(listId: String): Result<Unit> {
        // TODO: Implement moveout list deletion
        return Result.failure(Exception("Not implemented"))
    }
}

class CalendarRepository(private val apiClient: ApiClient) {
    
    suspend fun getCalendarEvents(): List<CalendarEvent> {
        return apiClient.getCalendarEvents()
    }
    
    suspend fun createCalendarEvent(
        title: String,
        description: String? = null,
        eventDate: String,
        eventType: String = "reorder",
        branchId: String? = null
    ): Result<CalendarEvent> {
        return apiClient.createCalendarEvent(title, description, eventDate, eventType, branchId)
    }
}

class StaffRepository(private val apiClient: ApiClient) {
    
    suspend fun getStaff(): Result<List<Profile>> {
        // TODO: Implement staff fetching
        return Result.failure(Exception("Not implemented"))
    }
    
    suspend fun createStaff(request: CreateStaffRequest): Result<Profile> {
        // TODO: Implement staff creation  
        return Result.failure(Exception("Not implemented"))
    }
    
    suspend fun updateStaff(staffId: String, request: UpdateStaffRequest): Result<Profile> {
        // TODO: Implement staff update
        return Result.failure(Exception("Not implemented"))
    }
    
    suspend fun deleteStaff(staffId: String): Result<Unit> {
        // TODO: Implement staff deletion
        return Result.failure(Exception("Not implemented"))
    }
}

class BranchRepository(private val apiClient: ApiClient) {
    
    suspend fun getBranches(): Result<List<Branch>> {
        // TODO: Implement branches fetching
        return Result.failure(Exception("Not implemented"))
    }
    
    suspend fun getDistricts(): Result<List<District>> {
        // TODO: Implement districts fetching
        return Result.failure(Exception("Not implemented"))
    }
    
    suspend fun getRegions(): Result<List<Region>> {
        // TODO: Implement regions fetching
        return Result.failure(Exception("Not implemented"))
    }
    
    suspend fun createBranch(request: CreateBranchRequest): Result<Branch> {
        // TODO: Implement branch creation
        return Result.failure(Exception("Not implemented"))
    }
    
    suspend fun updateBranch(branchId: String, request: UpdateBranchRequest): Result<Branch> {
        // TODO: Implement branch update
        return Result.failure(Exception("Not implemented"))
    }
    
    suspend fun deleteBranch(branchId: String): Result<Unit> {
        // TODO: Implement branch deletion
        return Result.failure(Exception("Not implemented"))
    }
}

class AnalyticsRepository(private val apiClient: ApiClient) {
    
    suspend fun getAnalyticsData(period: String): Result<AnalyticsData> {
        // TODO: Implement analytics data fetching
        return Result.failure(Exception("Not implemented"))
    }
}

class ICADeliveryRepository(private val apiClient: ApiClient) {
    
    suspend fun submitICADelivery(
        userName: String,
        entries: List<com.stocknexus.ui.screens.ICADeliveryEntry>
    ): Result<Unit> {
        return try {
            val result = apiClient.submitICADelivery(userName, entries)
            if (result.isSuccess) {
                Result.success(Unit)
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("Failed to submit ICA delivery"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class NotificationRepository(private val apiClient: ApiClient) {
    
    suspend fun getNotifications(): Result<List<Notification>> {
        return apiClient.getNotifications()
    }
    
    suspend fun markNotificationAsRead(notificationId: String): Result<Unit> {
        return apiClient.markNotificationAsRead(notificationId)
    }
    
    suspend fun markAllNotificationsAsRead(): Result<Unit> {
        return try {
            val notifications = apiClient.getNotifications().getOrNull() ?: emptyList()
            val unreadNotifications = notifications.filter { !it.isRead }
            
            unreadNotifications.forEach { notification ->
                apiClient.markNotificationAsRead(notification.id)
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}