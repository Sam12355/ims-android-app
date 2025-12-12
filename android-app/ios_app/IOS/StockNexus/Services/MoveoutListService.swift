import Foundation

// MARK: - Moveout List Service
class MoveoutListService {
    static let shared = MoveoutListService()
    
    private init() {}
    
    // MARK: - Get Moveout Lists
    func getMoveoutLists(branchId: String? = nil, status: MoveoutListStatus? = nil) async throws -> [MoveoutList] {
        var queryParams: [String: String] = [:]
        if let branchId = branchId {
            queryParams["branch_id"] = branchId
        }
        if let status = status {
            queryParams["status"] = status.rawValue
        }
        
        return try await NetworkManager.shared.request(
            endpoint: "/moveout-lists",
            queryParams: queryParams.isEmpty ? nil : queryParams
        )
    }
    
    // MARK: - Get Single Moveout List
    func getMoveoutList(id: String) async throws -> MoveoutList {
        return try await NetworkManager.shared.request(endpoint: "/moveout-lists/\(id)")
    }
    
    // MARK: - Create Moveout List
    func createMoveoutList(title: String, items: [CreateMoveoutItemRequest], branchId: String) async throws -> MoveoutList {
        struct Request: Encodable {
            let title: String
            let items: [CreateMoveoutItemRequest]
            let branchId: String
            
            enum CodingKeys: String, CodingKey {
                case title
                case items
                case branchId = "branch_id"
            }
        }
        
        return try await NetworkManager.shared.request(
            endpoint: "/moveout-lists",
            method: .post,
            body: Request(title: title, items: items, branchId: branchId)
        )
    }
    
    // MARK: - Complete Moveout Item
    func completeMoveoutItem(listId: String, itemId: String, completedQuantity: Int) async throws -> MoveoutList {
        struct Request: Encodable {
            let completedQuantity: Int
            
            enum CodingKeys: String, CodingKey {
                case completedQuantity = "completed_quantity"
            }
        }
        
        return try await NetworkManager.shared.request(
            endpoint: "/moveout-lists/\(listId)/items/\(itemId)/complete",
            method: .post,
            body: Request(completedQuantity: completedQuantity)
        )
    }
    
    // MARK: - Cancel Moveout List
    func cancelMoveoutList(id: String) async throws {
        let _: EmptyResponse = try await NetworkManager.shared.request(
            endpoint: "/moveout-lists/\(id)/cancel",
            method: .post
        )
    }
}

// MARK: - Request Models
struct CreateMoveoutItemRequest: Encodable {
    let itemId: String
    let requestedQuantity: Int
    
    enum CodingKeys: String, CodingKey {
        case itemId = "item_id"
        case requestedQuantity = "requested_quantity"
    }
}
