import Foundation

// MARK: - App Constants
struct AppConstants {
    // App Info
    static let appName = "Stock Nexus"
    static let appVersion = "1.0.0"
    static let bundleId = "com.stocknexus.ios"
    
    // API
    static let apiBaseURL = "https://api.stocknexus.com/v1"
    static let apiTimeout: TimeInterval = 30
    
    // Keychain Keys
    struct KeychainKeys {
        static let authToken = "auth_token"
        static let refreshToken = "refresh_token"
        static let userId = "user_id"
    }
    
    // UserDefaults Keys
    struct UserDefaultsKeys {
        static let currentUser = "current_user"
        static let rememberMe = "remember_me"
        static let lastLoginEmail = "last_login_email"
        static let isDarkMode = "is_dark_mode"
        static let notificationsEnabled = "notifications_enabled"
        static let selectedLanguage = "selected_language"
        static let onboardingCompleted = "onboarding_completed"
    }
    
    // Notification Names
    struct NotificationNames {
        static let userDidLogin = Notification.Name("userDidLogin")
        static let userDidLogout = Notification.Name("userDidLogout")
        static let stockUpdated = Notification.Name("stockUpdated")
        static let moveoutListUpdated = Notification.Name("moveoutListUpdated")
    }
    
    // Cache Keys
    struct CacheKeys {
        static let items = "cached_items"
        static let branches = "cached_branches"
        static let users = "cached_users"
    }
    
    // Time Constants (in seconds)
    struct TimeIntervals {
        static let pendingAccessRefresh: TimeInterval = 30
        static let weatherRefresh: TimeInterval = 1800 // 30 minutes
        static let dashboardRefresh: TimeInterval = 60
        static let cacheExpiry: TimeInterval = 3600 // 1 hour
    }
    
    // Swedish Time Zone
    static let swedishTimeZone = TimeZone(identifier: "Europe/Stockholm")!
    
    // Morning notification time (10 AM)
    static let morningNotificationHour = 10
    
    // Pagination
    static let defaultPageSize = 20
    static let maxPageSize = 100
}

// MARK: - Weather API
struct WeatherConstants {
    static let apiKey = "YOUR_OPENWEATHER_API_KEY"
    static let baseURL = "https://api.openweathermap.org/data/2.5"
    static let units = "metric"
}

// MARK: - Firebase Constants (if using Firebase)
struct FirebaseConstants {
    static let collectionUsers = "users"
    static let collectionItems = "items"
    static let collectionBranches = "branches"
    static let collectionDistricts = "districts"
    static let collectionRegions = "regions"
    static let collectionMoveoutLists = "moveout_lists"
    static let collectionDeliveries = "deliveries"
    static let collectionActivityLogs = "activity_logs"
    static let collectionNotifications = "notifications"
    static let collectionChatRooms = "chat_rooms"
    static let collectionMessages = "messages"
    static let collectionReceipts = "receipts"
    static let collectionCalendarEvents = "calendar_events"
}
