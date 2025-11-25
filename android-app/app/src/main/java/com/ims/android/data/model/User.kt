package com.ims.android.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

/**
 * User data models matching the web application's TypeScript interfaces
 */

enum class UserRole {
    ADMIN,
    MANAGER,
    ASSISTANT_MANAGER,
    STAFF;
    
    companion object {
        fun fromString(role: String): UserRole {
            return when (role.lowercase()) {
                "admin" -> ADMIN
                "manager" -> MANAGER
                "assistant_manager" -> ASSISTANT_MANAGER
                "staff" -> STAFF
                else -> STAFF
            }
        }
    }
    
    fun toApiString(): String {
        return when (this) {
            ADMIN -> "admin"
            MANAGER -> "manager"
            ASSISTANT_MANAGER -> "assistant_manager"
            STAFF -> "staff"
        }
    }
}

@Serializable
data class Profile(
    val id: String,
    @SerialName("user_id")
    val userId: String? = null,
    val name: String,
    val email: String,
    val phone: String? = null,
    @SerialName("photo_url")
    val photoUrl: String? = null,
    val position: String? = null,
    val role: String, // Will be converted to UserRole enum
    @SerialName("branch_id")
    val branchId: String? = null,
    @SerialName("branch_context")
    val branchContext: String? = null,
    @SerialName("branch_name")
    val branchName: String? = null,
    @SerialName("branch_location")
    val branchLocation: String? = null,
    @SerialName("district_name")
    val districtName: String? = null,
    @SerialName("region_name")
    val regionName: String? = null,
    @SerialName("region_id")
    val regionId: String? = null,
    @SerialName("district_id")
    val districtId: String? = null,
    @SerialName("last_access")
    val lastAccess: String? = null,
    @SerialName("access_count")
    val accessCount: Int? = null,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("updated_at")
    val updatedAt: String,
    @SerialName("notification_settings")
    val notificationSettings: Map<String, Boolean>? = null,
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
    val softdrinkTrendsMonthlyScheduleTime: String? = null,
    @SerialName("assistant_manager_stock_in_access")
    val assistantManagerStockInAccess: Boolean? = null
) {
    val userRole: UserRole
        get() = UserRole.fromString(role)
}

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

// SignupRequest removed - using SignUpRequest from AuthModels.kt instead