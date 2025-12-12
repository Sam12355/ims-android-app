import Foundation
import SwiftUI

// MARK: - Item Category
enum ItemCategory: String, Codable, CaseIterable {
    case fruits = "Fruits"
    case vegetables = "Vegetables"
    case dairy = "Dairy"
    case meat = "Meat"
    case seafood = "Seafood"
    case bakery = "Bakery"
    case beverages = "Beverages"
    case frozen = "Frozen"
    case snacks = "Snacks"
    case condiments = "Condiments"
    case grains = "Grains"
    case other = "Other"
    
    var icon: String {
        switch self {
        case .fruits: return "🍎"
        case .vegetables: return "🥕"
        case .dairy: return "🧀"
        case .meat: return "🥩"
        case .seafood: return "🐟"
        case .bakery: return "🍞"
        case .beverages: return "🥤"
        case .frozen: return "❄️"
        case .snacks: return "🍿"
        case .condiments: return "🧂"
        case .grains: return "🌾"
        case .other: return "📦"
        }
    }
}

// MARK: - Unit of Measure
enum UnitOfMeasure: String, Codable, CaseIterable {
    case pieces = "pieces"
    case kg = "kg"
    case liters = "liters"
    case cases = "cases"
    case boxes = "boxes"
    case packs = "packs"
    case bottles = "bottles"
    case cans = "cans"
    case bags = "bags"
    
    var displayName: String {
        rawValue.capitalized
    }
}

// MARK: - Stock Level
enum StockLevel: String, Codable {
    case normal = "normal"
    case low = "low"
    case critical = "critical"
    
    var colorName: String {
        switch self {
        case .normal: return "green"
        case .low: return "orange"
        case .critical: return "red"
        }
    }
    
    var color: Color {
        switch self {
        case .normal: return .green
        case .low: return .orange
        case .critical: return .red
        }
    }
}

// MARK: - Item Model
struct Item: Identifiable, Codable, Equatable {
    let id: String
    var name: String
    var category: ItemCategory
    var unit: UnitOfMeasure
    var currentStock: Int
    var thresholdLevel: Int
    var lowLevel: Int
    var criticalLevel: Int
    var description: String?
    var sku: String?
    var barcode: String?
    var imageURL: String?
    var storageTemperature: String?
    var baseUnit: String?
    var packagingUnit: String?
    var unitsPerPackage: Int?
    var branchId: String
    var isActive: Bool
    var createdAt: Date?
    var updatedAt: Date?
    
    enum CodingKeys: String, CodingKey {
        case id
        case name
        case category
        case unit
        case currentStock = "current_stock"
        case thresholdLevel = "threshold_level"
        case lowLevel = "low_level"
        case criticalLevel = "critical_level"
        case description
        case sku
        case barcode
        case imageURL = "image_url"
        case storageTemperature = "storage_temperature"
        case baseUnit = "base_unit"
        case packagingUnit = "packaging_unit"
        case unitsPerPackage = "units_per_package"
        case branchId = "branch_id"
        case isActive = "is_active"
        case createdAt = "created_at"
        case updatedAt = "updated_at"
    }
    
    var stockLevel: StockLevel {
        if currentStock <= criticalLevel {
            return .critical
        } else if currentStock <= lowLevel {
            return .low
        }
        return .normal
    }
    
    // Alias for imageURL (lowercase)
    var imageUrl: String? {
        imageURL
    }
    
    // Alias for currentStock as quantity
    var quantity: Int {
        currentStock
    }
    
    // Alias for category as string
    var categoryString: String {
        category.rawValue
    }
    
    var stockPercentage: Double {
        guard thresholdLevel > 0 else { return 0 }
        return min(Double(currentStock) / Double(thresholdLevel), 1.0)
    }
    
    static func == (lhs: Item, rhs: Item) -> Bool {
        lhs.id == rhs.id
    }
}

// MARK: - Stock Movement
struct StockMovement: Identifiable, Codable {
    let id: String
    let itemId: String
    let itemName: String
    let type: MovementType
    let quantity: Int
    let unitType: String
    let reason: String?
    let userId: String
    let userName: String
    let branchId: String
    let createdAt: Date
    
    enum CodingKeys: String, CodingKey {
        case id
        case itemId = "item_id"
        case itemName = "item_name"
        case type
        case quantity
        case unitType = "unit_type"
        case reason
        case userId = "user_id"
        case userName = "user_name"
        case branchId = "branch_id"
        case createdAt = "created_at"
    }
}

enum MovementType: String, Codable {
    case stockIn = "stock_in"
    case stockOut = "stock_out"
    case initialized = "initialized"
    
    var displayName: String {
        switch self {
        case .stockIn: return "Stock In"
        case .stockOut: return "Stock Out"
        case .initialized: return "Initialized"
        }
    }
}

// MARK: - Receipt
struct Receipt: Identifiable, Codable {
    let id: String
    var supplier: Supplier
    var remarks: String?
    var documentURL: String?
    var status: ReceiptStatus
    let submitterId: String
    let submitterName: String
    let branchId: String
    let createdAt: Date
    var processedAt: Date?
    var processedBy: String?
    
    enum CodingKeys: String, CodingKey {
        case id
        case supplier
        case remarks
        case documentURL = "document_url"
        case status
        case submitterId = "submitter_id"
        case submitterName = "submitter_name"
        case branchId = "branch_id"
        case createdAt = "created_at"
        case processedAt = "processed_at"
        case processedBy = "processed_by"
    }
}

enum Supplier: String, Codable, CaseIterable {
    case gronsakshuset = "Gronsakshuset"
    case kvalitetsfisk = "Kvalitetsfisk"
    case spendrups = "Spendrups"
    case tingstad = "Tingstad"
    case other = "Other"
}

enum ReceiptStatus: String, Codable {
    case pending = "pending"
    case processed = "processed"
}

// MARK: - Sample Data
extension Item {
    static let sample = Item(
        id: "1",
        name: "Coca Cola 33cl",
        category: .beverages,
        unit: .cans,
        currentStock: 25,
        thresholdLevel: 30,
        lowLevel: 20,
        criticalLevel: 10,
        description: "Coca Cola Classic 33cl cans",
        sku: "CC33CL",
        barcode: "5000112610659",
        imageURL: nil,
        storageTemperature: "Cool",
        baseUnit: "can",
        packagingUnit: "case",
        unitsPerPackage: 24,
        branchId: "branch1",
        isActive: true,
        createdAt: Date(),
        updatedAt: Date()
    )
    
    static let samples: [Item] = [
        Item(id: "1", name: "Coca Cola 33cl", category: .beverages, unit: .cans, currentStock: 25, thresholdLevel: 30, lowLevel: 20, criticalLevel: 10, description: nil, sku: nil, barcode: nil, imageURL: nil, storageTemperature: nil, baseUnit: nil, packagingUnit: nil, unitsPerPackage: nil, branchId: "branch1", isActive: true, createdAt: Date(), updatedAt: Date()),
        Item(id: "2", name: "Bananas", category: .fruits, unit: .kg, currentStock: 5, thresholdLevel: 15, lowLevel: 10, criticalLevel: 5, description: nil, sku: nil, barcode: nil, imageURL: nil, storageTemperature: nil, baseUnit: nil, packagingUnit: nil, unitsPerPackage: nil, branchId: "branch1", isActive: true, createdAt: Date(), updatedAt: Date()),
        Item(id: "3", name: "Milk 1L", category: .dairy, unit: .liters, currentStock: 50, thresholdLevel: 40, lowLevel: 25, criticalLevel: 15, description: nil, sku: nil, barcode: nil, imageURL: nil, storageTemperature: nil, baseUnit: nil, packagingUnit: nil, unitsPerPackage: nil, branchId: "branch1", isActive: true, createdAt: Date(), updatedAt: Date()),
        Item(id: "4", name: "Salmon Fillet", category: .seafood, unit: .kg, currentStock: 3, thresholdLevel: 10, lowLevel: 6, criticalLevel: 3, description: nil, sku: nil, barcode: nil, imageURL: nil, storageTemperature: nil, baseUnit: nil, packagingUnit: nil, unitsPerPackage: nil, branchId: "branch1", isActive: true, createdAt: Date(), updatedAt: Date()),
        Item(id: "5", name: "Bread Loaf", category: .bakery, unit: .pieces, currentStock: 15, thresholdLevel: 20, lowLevel: 12, criticalLevel: 8, description: nil, sku: nil, barcode: nil, imageURL: nil, storageTemperature: nil, baseUnit: nil, packagingUnit: nil, unitsPerPackage: nil, branchId: "branch1", isActive: true, createdAt: Date(), updatedAt: Date())
    ]
}
