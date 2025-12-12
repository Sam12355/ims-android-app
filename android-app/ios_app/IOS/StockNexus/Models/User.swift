import Foundation

// MARK: - User Role
enum UserRole: String, Codable, CaseIterable {
    case admin = "admin"
    case manager = "manager"
    case assistantManager = "assistant_manager"
    case staff = "staff"
    
    var level: Int {
        switch self {
        case .admin: return 4
        case .manager: return 3
        case .assistantManager: return 2
        case .staff: return 1
        }
    }
    
    var displayName: String {
        switch self {
        case .admin: return "Admin"
        case .manager: return "Manager"
        case .assistantManager: return "Assistant Manager"
        case .staff: return "Staff"
        }
    }
    
    // Permission checks
    var canManageItems: Bool {
        level >= 2
    }
    
    var canStockOut: Bool {
        true
    }
    
    var canStockIn: Bool {
        level >= 2
    }
    
    var canRecordStockIn: Bool {
        self == .staff
    }
    
    var canManageMoveoutLists: Bool {
        level >= 2
    }
    
    var canManageICADelivery: Bool {
        level >= 2
    }
    
    var canManageStaff: Bool {
        level >= 2
    }
    
    var canManageBranches: Bool {
        self == .admin
    }
    
    var canManageDistricts: Bool {
        self == .admin
    }
    
    var canManageRegions: Bool {
        self == .admin
    }
    
    var canViewAnalytics: Bool {
        level >= 2
    }
    
    var canViewReports: Bool {
        level >= 2
    }
    
    var canViewActivityLogs: Bool {
        level >= 3
    }
    
    var canAddCalendarEvents: Bool {
        level >= 2
    }
    
    var canViewCalendarEvents: Bool {
        true
    }
}

// MARK: - User Status
enum UserStatus: String, Codable {
    case active = "active"
    case pending = "pending"
    case inactive = "inactive"
}

// MARK: - User Model
struct User: Identifiable, Codable, Equatable {
    let id: String
    var fullName: String
    let email: String
    var phone: String?
    var position: String?
    var role: UserRole
    var status: UserStatus
    var branchId: String?
    var branchName: String?
    var photoURL: String?
    var createdAt: Date?
    var updatedAt: Date?
    
    enum CodingKeys: String, CodingKey {
        case id
        case fullName = "full_name"
        case email
        case phone
        case position
        case role
        case status
        case branchId = "branch_id"
        case branchName = "branch_name"
        case photoURL = "photo_url"
        case createdAt = "created_at"
        case updatedAt = "updated_at"
    }
    
    static func == (lhs: User, rhs: User) -> Bool {
        lhs.id == rhs.id
    }
}

// MARK: - Sample Data
extension User {
    static let sample = User(
        id: "1",
        fullName: "John Smith",
        email: "john@example.com",
        phone: "+46701234567",
        position: "Store Manager",
        role: .manager,
        status: .active,
        branchId: "branch1",
        branchName: "ICA Maxi Örnsköldsvik",
        photoURL: nil,
        createdAt: Date(),
        updatedAt: Date()
    )
    
    static let sampleAdmin = User(
        id: "2",
        fullName: "Admin User",
        email: "admin@stocknexus.com",
        phone: nil,
        position: "System Administrator",
        role: .admin,
        status: .active,
        branchId: nil,
        branchName: nil,
        photoURL: nil,
        createdAt: Date(),
        updatedAt: Date()
    )
    
    static let sampleStaff = User(
        id: "3",
        fullName: "Staff Member",
        email: "staff@example.com",
        phone: nil,
        position: "Cashier",
        role: .staff,
        status: .active,
        branchId: "branch1",
        branchName: "ICA Maxi Örnsköldsvik",
        photoURL: nil,
        createdAt: Date(),
        updatedAt: Date()
    )
}
