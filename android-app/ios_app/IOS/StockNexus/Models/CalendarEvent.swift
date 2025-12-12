import Foundation

// MARK: - Calendar Event Type
enum CalendarEventType: String, Codable, CaseIterable {
    case reorder = "reorder"
    case delivery = "delivery"
    case alert = "alert"
    case expiry = "expiry"
    case usageSpike = "usage_spike"
    
    var displayName: String {
        switch self {
        case .reorder: return "Reorder"
        case .delivery: return "Delivery"
        case .alert: return "Alert"
        case .expiry: return "Expiry"
        case .usageSpike: return "Usage Spike"
        }
    }
    
    var icon: String {
        switch self {
        case .reorder: return "arrow.clockwise"
        case .delivery: return "shippingbox"
        case .alert: return "exclamationmark.triangle"
        case .expiry: return "calendar.badge.exclamationmark"
        case .usageSpike: return "chart.line.uptrend.xyaxis"
        }
    }
    
    var colorName: String {
        switch self {
        case .reorder: return "blue"
        case .delivery: return "green"
        case .alert: return "orange"
        case .expiry: return "red"
        case .usageSpike: return "purple"
        }
    }
}

// MARK: - Calendar Event
struct CalendarEvent: Identifiable, Codable, Equatable {
    let id: String
    var title: String
    var eventType: CalendarEventType
    var date: Date
    var description: String?
    let branchId: String
    var branchName: String?
    let createdById: String
    var createdByName: String?
    let createdAt: Date
    var updatedAt: Date?
    
    enum CodingKeys: String, CodingKey {
        case id
        case title
        case eventType = "event_type"
        case date
        case description
        case branchId = "branch_id"
        case branchName = "branch_name"
        case createdById = "created_by_id"
        case createdByName = "created_by_name"
        case createdAt = "created_at"
        case updatedAt = "updated_at"
    }
    
    var isToday: Bool {
        Calendar.current.isDateInToday(date)
    }
    
    var isUpcoming: Bool {
        date > Date()
    }
    
    static func == (lhs: CalendarEvent, rhs: CalendarEvent) -> Bool {
        lhs.id == rhs.id
    }
}

// MARK: - Sample Data
extension CalendarEvent {
    static let sample = CalendarEvent(
        id: "event1",
        title: "Restock Beverages",
        eventType: .reorder,
        date: Date(),
        description: "Weekly beverage restock",
        branchId: "branch1",
        branchName: "ICA Maxi Örnsköldsvik",
        createdById: "1",
        createdByName: "John Manager",
        createdAt: Date(),
        updatedAt: nil
    )
    
    static let samples: [CalendarEvent] = [
        CalendarEvent(id: "event1", title: "Restock Beverages", eventType: .reorder, date: Date(), description: "Weekly beverage restock", branchId: "branch1", branchName: nil, createdById: "1", createdByName: nil, createdAt: Date(), updatedAt: nil),
        CalendarEvent(id: "event2", title: "Fish Delivery", eventType: .delivery, date: Calendar.current.date(byAdding: .day, value: 1, to: Date())!, description: "Expected salmon delivery", branchId: "branch1", branchName: nil, createdById: "1", createdByName: nil, createdAt: Date(), updatedAt: nil),
        CalendarEvent(id: "event3", title: "Low Stock Alert", eventType: .alert, date: Date(), description: "Multiple items below threshold", branchId: "branch1", branchName: nil, createdById: "1", createdByName: nil, createdAt: Date(), updatedAt: nil),
        CalendarEvent(id: "event4", title: "Dairy Expiring", eventType: .expiry, date: Calendar.current.date(byAdding: .day, value: 2, to: Date())!, description: "Milk products expiring", branchId: "branch1", branchName: nil, createdById: "1", createdByName: nil, createdAt: Date(), updatedAt: nil),
        CalendarEvent(id: "event5", title: "Weekend Rush", eventType: .usageSpike, date: Calendar.current.date(byAdding: .day, value: 3, to: Date())!, description: "Expected high sales", branchId: "branch1", branchName: nil, createdById: "1", createdByName: nil, createdAt: Date(), updatedAt: nil)
    ]
}
