package com.ims.android.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

/**
 * API request and response models matching the web application
 */

@Serializable
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val message: String? = null,
    val error: String? = null
)

@Serializable
data class PaginatedResponse<T>(
    val success: Boolean,
    val data: List<T>,
    val pagination: PaginationInfo,
    val message: String? = null
)

@Serializable
data class PaginationInfo(
    val page: Int,
    val limit: Int,
    val total: Int,
    val totalPages: Int,
    val hasNext: Boolean,
    val hasPrev: Boolean
)

// Stock In Requests
@Serializable
data class CreateStockInRequest(
    val reference: String,
    val supplier: String? = null,
    val notes: String? = null,
    val items: List<StockInItemRequest>
)

@Serializable
data class StockInItemRequest(
    val itemId: String,
    val quantity: Int,
    val unitCost: Double? = null,
    val expiryDate: String? = null,
    val batchNumber: String? = null,
    val notes: String? = null
)

// Stock Out Requests
@Serializable
data class CreateStockOutRequest(
    val itemId: String,
    val quantity: Int,
    val reason: String,
    val notes: String? = null
)

// Moveout List Requests
@Serializable
data class CreateMoveoutListRequest(
    val title: String,
    val description: String? = null,
    val items: List<MoveoutItemRequest>
)

@Serializable
data class MoveoutItemRequest(
    val itemId: String,
    val itemName: String,
    val availableAmount: Int,
    val requestAmount: Int,
    val category: String = "General"
)

@Serializable
data class CompleteMoveoutItemRequest(
    val completed: Boolean = true,
    val notes: String? = null
)

// Item Management Requests
@Serializable
data class CreateItemRequest(
    val name: String,
    val category: String,
    val description: String? = null,
    val sku: String? = null,
    val barcode: String? = null,
    val unitOfMeasure: String = "pieces",
    val thresholdLevel: Int = 10,
    val lowLevel: Int = 5,
    val criticalLevel: Int = 2,
    val imageUrl: String? = null
)

@Serializable
data class UpdateItemRequest(
    val name: String? = null,
    val category: String? = null,
    val description: String? = null,
    val sku: String? = null,
    val barcode: String? = null,
    val unitOfMeasure: String? = null,
    val thresholdLevel: Int? = null,
    val lowLevel: Int? = null,
    val criticalLevel: Int? = null,
    val imageUrl: String? = null,
    val isActive: Boolean? = null
)

// Staff Management Requests
@Serializable
data class CreateStaffRequest(
    val name: String,
    val email: String,
    val phone: String? = null,
    val role: String,
    val position: String? = null,
    val branchId: String? = null,
    val password: String
)

@Serializable
data class UpdateStaffRequest(
    val name: String? = null,
    val phone: String? = null,
    val position: String? = null,
    val role: String? = null,
    val branchId: String? = null
)

// Branch Management Requests
@Serializable
data class CreateBranchRequest(
    val name: String,
    val location: String? = null,
    val districtId: String,
    val address: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val managerId: String? = null
)

@Serializable
data class UpdateBranchRequest(
    val name: String? = null,
    val location: String? = null,
    val address: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val managerId: String? = null,
    val isActive: Boolean? = null
)

// Settings Requests
@Serializable
data class UpdateProfileRequest(
    val name: String? = null,
    val phone: String? = null,
    val photoUrl: String? = null,
    val position: String? = null,

    // Stock Alert Scheduling
    @SerialName("stock_alert_frequencies")
    val stockAlertFrequencies: List<String>? = null,
    @SerialName("daily_schedule_time")
    val dailyScheduleTime: String? = null,
    @SerialName("weekly_schedule_day")
    val weeklyScheduleDay: Int? = null,
    @SerialName("weekly_schedule_time")
    val weeklyScheduleTime: String? = null,
    @SerialName("monthly_schedule_date")
    val monthlyScheduleDate: Int? = null,
    @SerialName("monthly_schedule_time")
    val monthlyScheduleTime: String? = null,

    // Event Reminder Scheduling
    @SerialName("event_reminder_frequencies")
    val eventReminderFrequencies: List<String>? = null,
    @SerialName("event_daily_schedule_time")
    val eventDailyScheduleTime: String? = null,
    @SerialName("event_weekly_schedule_day")
    val eventWeeklyScheduleDay: Int? = null,
    @SerialName("event_weekly_schedule_time")
    val eventWeeklyScheduleTime: String? = null,
    @SerialName("event_monthly_schedule_date")
    val eventMonthlyScheduleDate: Int? = null,
    @SerialName("event_monthly_schedule_time")
    val eventMonthlyScheduleTime: String? = null,

    // Softdrink Trends Scheduling
    @SerialName("softdrink_trends_frequencies")
    val softdrinkTrendsFrequencies: List<String>? = null,
    @SerialName("softdrink_trends_daily_schedule_time")
    val softdrinkTrendsDailyScheduleTime: String? = null,
    @SerialName("softdrink_trends_weekly_schedule_day")
    val softdrinkTrendsWeeklyScheduleDay: Int? = null,
    @SerialName("softdrink_trends_weekly_schedule_time")
    val softdrinkTrendsWeeklyScheduleTime: String? = null,
    @SerialName("softdrink_trends_monthly_schedule_date")
    val softdrinkTrendsMonthlyScheduleDate: Int? = null,
    @SerialName("softdrink_trends_monthly_schedule_time")
    val softdrinkTrendsMonthlyScheduleTime: String? = null
)

// Event Requests
@Serializable
data class CreateEventRequest(
    val title: String,
    val description: String? = null,
    val eventDate: String,
    val eventType: String,
    val branchId: String? = null
)

// Dashboard Models
@Serializable
data class DashboardStats(
    val totalItems: Int,
    val lowStockItems: Int,
    val criticalStockItems: Int,
    val thresholdStockItems: Int,
    val totalStaff: Int,
    val recentActivities: List<ActivityLog>,
    val lowStockDetails: List<StockDetail>? = null,
    val criticalStockDetails: List<StockDetail>? = null,
    val thresholdStockDetails: List<StockDetail>? = null
)

@Serializable
data class ActivityLog(
    val id: String,
    val action: String,
    val details: String? = null,
    val createdAt: String,
    val userId: String? = null,
    val profiles: ProfileReference? = null
)

@Serializable
data class ProfileReference(
    val name: String
)

@Serializable
data class WeatherData(
    val temperature: Double,
    val condition: String,
    val location: String,
    val humidity: Int,
    val windSpeed: Double
)

@Serializable
data class StockDetail(
    val id: String,
    val name: String,
    val category: String,
    val currentQuantity: Int,
    val thresholdLevel: Int,
    val lowLevel: Int,
    val criticalLevel: Int,
    val imageUrl: String? = null
)

@Serializable
data class Notification(
    val id: String,
    val title: String,
    val message: String,
    val type: String,
    val isRead: Boolean = false,
    val createdAt: String,
    val userId: String
)

@Serializable
data class AnalyticsData(
    val period: String,
    val totalSales: Double,
    val totalItems: Int,
    val trends: List<TrendData>
)

@Serializable
data class TrendData(
    val date: String,
    val value: Double,
    val label: String
)