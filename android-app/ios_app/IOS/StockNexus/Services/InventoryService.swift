import Foundation

// MARK: - Inventory Service
class InventoryService {
    static let shared = InventoryService()
    
    private init() {}
    
    // MARK: - Items
    func getItems(branchId: String? = nil, category: ItemCategory? = nil) async throws -> [Item] {
        var queryParams: [String: String] = [:]
        if let branchId = branchId {
            queryParams["branch_id"] = branchId
        }
        if let category = category {
            queryParams["category"] = category.rawValue
        }
        
        return try await NetworkManager.shared.request(
            endpoint: "/items",
            queryParams: queryParams.isEmpty ? nil : queryParams
        )
    }
    
    func getItem(id: String) async throws -> Item {
        return try await NetworkManager.shared.request(endpoint: "/items/\(id)")
    }
    
    func createItem(_ item: CreateItemRequest) async throws -> Item {
        return try await NetworkManager.shared.request(
            endpoint: "/items",
            method: .post,
            body: item
        )
    }
    
    func updateItem(id: String, _ item: UpdateItemRequest) async throws -> Item {
        return try await NetworkManager.shared.request(
            endpoint: "/items/\(id)",
            method: .put,
            body: item
        )
    }
    
    func deleteItem(id: String) async throws {
        let _: EmptyResponse = try await NetworkManager.shared.request(
            endpoint: "/items/\(id)",
            method: .delete
        )
    }
    
    // MARK: - Stock Operations
    func stockOut(itemId: String, quantity: Int, unitType: String, reason: String?) async throws -> Item {
        let request = StockOperationRequest(
            itemId: itemId,
            quantity: quantity,
            unitType: unitType,
            reason: reason
        )
        return try await NetworkManager.shared.request(
            endpoint: "/stock/out",
            method: .post,
            body: request
        )
    }
    
    func stockIn(itemId: String, quantity: Int, unitType: String, reason: String?) async throws -> Item {
        let request = StockOperationRequest(
            itemId: itemId,
            quantity: quantity,
            unitType: unitType,
            reason: reason
        )
        return try await NetworkManager.shared.request(
            endpoint: "/stock/in",
            method: .post,
            body: request
        )
    }
    
    // MARK: - Receipts
    func getReceipts(branchId: String? = nil, status: ReceiptStatus? = nil) async throws -> [Receipt] {
        var queryParams: [String: String] = [:]
        if let branchId = branchId {
            queryParams["branch_id"] = branchId
        }
        if let status = status {
            queryParams["status"] = status.rawValue
        }
        
        return try await NetworkManager.shared.request(
            endpoint: "/receipts",
            queryParams: queryParams.isEmpty ? nil : queryParams
        )
    }
    
    func submitReceipt(supplier: Supplier, remarks: String?, documentURL: String?) async throws -> Receipt {
        let request = SubmitReceiptRequest(
            supplier: supplier,
            remarks: remarks,
            documentURL: documentURL
        )
        return try await NetworkManager.shared.request(
            endpoint: "/receipts",
            method: .post,
            body: request
        )
    }
    
    func processReceipt(id: String) async throws -> Receipt {
        return try await NetworkManager.shared.request(
            endpoint: "/receipts/\(id)/process",
            method: .post
        )
    }
    
    // MARK: - Dashboard Stats
    func getDashboardStats(branchId: String) async throws -> DashboardStats {
        return try await NetworkManager.shared.request(
            endpoint: "/dashboard/stats",
            queryParams: ["branch_id": branchId]
        )
    }
}

// MARK: - Request Models
struct CreateItemRequest: Encodable {
    let name: String
    let category: String
    let unit: String
    let thresholdLevel: Int
    let lowLevel: Int
    let criticalLevel: Int
    let description: String?
    let sku: String?
    let barcode: String?
    let imageURL: String?
    let storageTemperature: String?
    let baseUnit: String?
    let packagingUnit: String?
    let unitsPerPackage: Int?
    let branchId: String
    
    enum CodingKeys: String, CodingKey {
        case name, category, unit, description, sku, barcode
        case thresholdLevel = "threshold_level"
        case lowLevel = "low_level"
        case criticalLevel = "critical_level"
        case imageURL = "image_url"
        case storageTemperature = "storage_temperature"
        case baseUnit = "base_unit"
        case packagingUnit = "packaging_unit"
        case unitsPerPackage = "units_per_package"
        case branchId = "branch_id"
    }
}

struct UpdateItemRequest: Encodable {
    let name: String?
    let category: String?
    let unit: String?
    let thresholdLevel: Int?
    let lowLevel: Int?
    let criticalLevel: Int?
    let description: String?
    let sku: String?
    let barcode: String?
    let imageURL: String?
    let storageTemperature: String?
    let baseUnit: String?
    let packagingUnit: String?
    let unitsPerPackage: Int?
    
    enum CodingKeys: String, CodingKey {
        case name, category, unit, description, sku, barcode
        case thresholdLevel = "threshold_level"
        case lowLevel = "low_level"
        case criticalLevel = "critical_level"
        case imageURL = "image_url"
        case storageTemperature = "storage_temperature"
        case baseUnit = "base_unit"
        case packagingUnit = "packaging_unit"
        case unitsPerPackage = "units_per_package"
    }
}

struct StockOperationRequest: Encodable {
    let itemId: String
    let quantity: Int
    let unitType: String
    let reason: String?
    
    enum CodingKeys: String, CodingKey {
        case itemId = "item_id"
        case quantity
        case unitType = "unit_type"
        case reason
    }
}

struct SubmitReceiptRequest: Encodable {
    let supplier: Supplier
    let remarks: String?
    let documentURL: String?
    
    enum CodingKeys: String, CodingKey {
        case supplier
        case remarks
        case documentURL = "document_url"
    }
}

// MARK: - Dashboard Stats
struct DashboardStats: Decodable {
    let totalItems: Int
    let lowStockItems: Int
    let criticalStockItems: Int
    let totalValue: Double?
    let recentMovements: [StockMovement]?
    
    enum CodingKeys: String, CodingKey {
        case totalItems = "total_items"
        case lowStockItems = "low_stock_items"
        case criticalStockItems = "critical_stock_items"
        case totalValue = "total_value"
        case recentMovements = "recent_movements"
    }
}
