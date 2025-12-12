import Foundation

// MARK: - Moveout List Status
enum MoveoutListStatus: String, Codable {
    case active = "active"
    case completed = "completed"
    case cancelled = "cancelled"
}

// MARK: - Moveout List
struct MoveoutList: Identifiable, Codable, Equatable {
    let id: String
    var title: String
    var status: MoveoutListStatus
    var items: [MoveoutItem]
    let branchId: String
    var branchName: String?
    let createdById: String
    var createdByName: String?
    let createdAt: Date
    var completedAt: Date?
    
    enum CodingKeys: String, CodingKey {
        case id
        case title
        case status
        case items
        case branchId = "branch_id"
        case branchName = "branch_name"
        case createdById = "created_by_id"
        case createdByName = "created_by_name"
        case createdAt = "created_at"
        case completedAt = "completed_at"
    }
    
    var completedItemsCount: Int {
        items.filter { $0.isCompleted }.count
    }
    
    var totalItemsCount: Int {
        items.count
    }
    
    var progress: Double {
        guard totalItemsCount > 0 else { return 0 }
        return Double(completedItemsCount) / Double(totalItemsCount)
    }
    
    var isFullyCompleted: Bool {
        completedItemsCount == totalItemsCount && totalItemsCount > 0
    }
    
    static func == (lhs: MoveoutList, rhs: MoveoutList) -> Bool {
        lhs.id == rhs.id
    }
}

// MARK: - Moveout Item
struct MoveoutItem: Identifiable, Codable, Equatable {
    let id: String
    let itemId: String
    var itemName: String
    var category: String
    var requestedQuantity: Int
    var availableQuantity: Int
    var completedQuantity: Int?
    var isCompleted: Bool
    var completedById: String?
    var completedByName: String?
    var completedAt: Date?
    
    enum CodingKeys: String, CodingKey {
        case id
        case itemId = "item_id"
        case itemName = "item_name"
        case category
        case requestedQuantity = "requested_quantity"
        case availableQuantity = "available_quantity"
        case completedQuantity = "completed_quantity"
        case isCompleted = "is_completed"
        case completedById = "completed_by_id"
        case completedByName = "completed_by_name"
        case completedAt = "completed_at"
    }
    
    static func == (lhs: MoveoutItem, rhs: MoveoutItem) -> Bool {
        lhs.id == rhs.id
    }
}

// MARK: - Sample Data
extension MoveoutList {
    static let sample = MoveoutList(
        id: "moveout1",
        title: "Moveout List - Nov 30, 2025",
        status: .active,
        items: MoveoutItem.samples,
        branchId: "branch1",
        branchName: "ICA Maxi Örnsköldsvik",
        createdById: "1",
        createdByName: "John Manager",
        createdAt: Date(),
        completedAt: nil
    )
    
    static let samples: [MoveoutList] = [
        MoveoutList(id: "moveout1", title: "Moveout List - Nov 30, 2025", status: .active, items: MoveoutItem.samples, branchId: "branch1", branchName: "ICA Maxi Örnsköldsvik", createdById: "1", createdByName: "John Manager", createdAt: Date(), completedAt: nil),
        MoveoutList(id: "moveout2", title: "Moveout List - Nov 29, 2025", status: .completed, items: MoveoutItem.samples, branchId: "branch1", branchName: "ICA Maxi Örnsköldsvik", createdById: "1", createdByName: "John Manager", createdAt: Calendar.current.date(byAdding: .day, value: -1, to: Date())!, completedAt: Date())
    ]
}

extension MoveoutItem {
    static let sample = MoveoutItem(
        id: "mitem1",
        itemId: "1",
        itemName: "Coca Cola 33cl",
        category: "Beverages",
        requestedQuantity: 10,
        availableQuantity: 25,
        completedQuantity: nil,
        isCompleted: false,
        completedById: nil,
        completedByName: nil,
        completedAt: nil
    )
    
    static let samples: [MoveoutItem] = [
        MoveoutItem(id: "mitem1", itemId: "1", itemName: "Coca Cola 33cl", category: "Beverages", requestedQuantity: 10, availableQuantity: 25, completedQuantity: 10, isCompleted: true, completedById: "3", completedByName: "Staff Member", completedAt: Date()),
        MoveoutItem(id: "mitem2", itemId: "2", itemName: "Bananas", category: "Fruits", requestedQuantity: 5, availableQuantity: 5, completedQuantity: nil, isCompleted: false, completedById: nil, completedByName: nil, completedAt: nil),
        MoveoutItem(id: "mitem3", itemId: "3", itemName: "Milk 1L", category: "Dairy", requestedQuantity: 20, availableQuantity: 50, completedQuantity: nil, isCompleted: false, completedById: nil, completedByName: nil, completedAt: nil)
    ]
}
