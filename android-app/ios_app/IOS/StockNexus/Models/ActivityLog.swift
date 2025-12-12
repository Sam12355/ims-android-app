import Foundation

// MARK: - Activity Log Type
enum ActivityLogType: String, Codable, CaseIterable {
    case stockIn = "stock_in"
    case stockOut = "stock_out"
    case itemCreated = "item_created"
    case itemUpdated = "item_updated"
    case itemDeleted = "item_deleted"
    case userCreated = "user_created"
    case userUpdated = "user_updated"
    case userActivated = "user_activated"
    case userDeactivated = "user_deactivated"
    case moveoutCreated = "moveout_created"
    case moveoutCompleted = "moveout_completed"
    case deliveryCreated = "delivery_created"
    case deliveryUpdated = "delivery_updated"
    case login = "login"
    case logout = "logout"
    
    var displayName: String {
        switch self {
        case .stockIn: return "Stock In"
        case .stockOut: return "Stock Out"
        case .itemCreated: return "Item Created"
        case .itemUpdated: return "Item Updated"
        case .itemDeleted: return "Item Deleted"
        case .userCreated: return "User Created"
        case .userUpdated: return "User Updated"
        case .userActivated: return "User Activated"
        case .userDeactivated: return "User Deactivated"
        case .moveoutCreated: return "Moveout Created"
        case .moveoutCompleted: return "Moveout Completed"
        case .deliveryCreated: return "Delivery Created"
        case .deliveryUpdated: return "Delivery Updated"
        case .login: return "Login"
        case .logout: return "Logout"
        }
    }
    
    var icon: String {
        switch self {
        case .stockIn: return "arrow.down.circle"
        case .stockOut: return "arrow.up.circle"
        case .itemCreated: return "plus.circle"
        case .itemUpdated: return "pencil.circle"
        case .itemDeleted: return "trash.circle"
        case .userCreated: return "person.badge.plus"
        case .userUpdated: return "person.badge.clock"
        case .userActivated: return "person.badge.shield.checkmark"
        case .userDeactivated: return "person.badge.minus"
        case .moveoutCreated: return "list.bullet.rectangle"
        case .moveoutCompleted: return "checkmark.rectangle"
        case .deliveryCreated: return "shippingbox"
        case .deliveryUpdated: return "shippingbox.fill"
        case .login: return "person.fill.checkmark"
        case .logout: return "rectangle.portrait.and.arrow.right"
        }
    }
    
    var category: ActivityCategory {
        switch self {
        case .stockIn, .stockOut, .itemCreated, .itemUpdated, .itemDeleted:
            return .stock
        default:
            return .general
        }
    }
}

enum ActivityCategory: String, Codable, CaseIterable {
    case all = "all"
    case stock = "stock"
    case general = "general"
    
    var displayName: String {
        switch self {
        case .all: return "All"
        case .stock: return "Stock"
        case .general: return "General"
        }
    }
}

// MARK: - Activity Log
struct ActivityLog: Identifiable, Codable, Equatable {
    let id: String
    var type: ActivityLogType
    var description: String
    var details: String?
    var userId: String
    var userName: String
    var branchId: String?
    var branchName: String?
    let createdAt: Date
    
    enum CodingKeys: String, CodingKey {
        case id
        case type
        case description
        case details
        case userId = "user_id"
        case userName = "user_name"
        case branchId = "branch_id"
        case branchName = "branch_name"
        case createdAt = "created_at"
    }
    
    static func == (lhs: ActivityLog, rhs: ActivityLog) -> Bool {
        lhs.id == rhs.id
    }
}

// MARK: - Sample Data
extension ActivityLog {
    static let sample = ActivityLog(
        id: "log1",
        type: .stockOut,
        description: "Removed 5 units of Coca Cola 33cl",
        details: "Reason: Daily sales",
        userId: "3",
        userName: "Staff Member",
        branchId: "branch1",
        branchName: "ICA Maxi Örnsköldsvik",
        createdAt: Date()
    )
    
    static let samples: [ActivityLog] = [
        ActivityLog(id: "log1", type: .stockOut, description: "Removed 5 units of Coca Cola 33cl", details: nil, userId: "3", userName: "Staff Member", branchId: "branch1", branchName: nil, createdAt: Date()),
        ActivityLog(id: "log2", type: .stockIn, description: "Added 50 units of Milk 1L", details: nil, userId: "1", userName: "John Manager", branchId: "branch1", branchName: nil, createdAt: Calendar.current.date(byAdding: .hour, value: -1, to: Date())!),
        ActivityLog(id: "log3", type: .itemCreated, description: "Created new item: Fresh Bread", details: nil, userId: "1", userName: "John Manager", branchId: "branch1", branchName: nil, createdAt: Calendar.current.date(byAdding: .hour, value: -2, to: Date())!),
        ActivityLog(id: "log4", type: .userActivated, description: "Activated user: Jane Staff", details: nil, userId: "1", userName: "John Manager", branchId: "branch1", branchName: nil, createdAt: Calendar.current.date(byAdding: .day, value: -1, to: Date())!),
        ActivityLog(id: "log5", type: .moveoutCreated, description: "Created moveout list with 12 items", details: nil, userId: "1", userName: "John Manager", branchId: "branch1", branchName: nil, createdAt: Calendar.current.date(byAdding: .day, value: -1, to: Date())!)
    ]
}
