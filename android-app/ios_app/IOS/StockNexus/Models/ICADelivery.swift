import Foundation

// MARK: - ICA Delivery Type
enum ICADeliveryType: String, Codable, CaseIterable {
    case salmonAndRolls = "Salmon and Rolls"
    case sashimi = "Sashimi"
    case poke = "Poke"
    case sushi = "Sushi"
    case gomaWakame = "Goma Wakame"
    
    var displayName: String {
        rawValue
    }
}

// MARK: - Delivery Time Period
enum DeliveryTimePeriod: String, Codable, CaseIterable {
    case lunch = "Lunch"
    case dinner = "Dinner"
    
    var displayName: String {
        rawValue
    }
}

// MARK: - ICA Delivery
struct ICADelivery: Identifiable, Codable, Equatable {
    let id: String
    var date: Date
    var deliveryType: ICADeliveryType
    var timePeriod: DeliveryTimePeriod
    var quantity: Int
    let branchId: String
    var branchName: String?
    let createdById: String
    var createdByName: String?
    let createdAt: Date
    var updatedAt: Date?
    
    enum CodingKeys: String, CodingKey {
        case id
        case date
        case deliveryType = "delivery_type"
        case timePeriod = "time_period"
        case quantity
        case branchId = "branch_id"
        case branchName = "branch_name"
        case createdById = "created_by_id"
        case createdByName = "created_by_name"
        case createdAt = "created_at"
        case updatedAt = "updated_at"
    }
    
    static func == (lhs: ICADelivery, rhs: ICADelivery) -> Bool {
        lhs.id == rhs.id
    }
}

// MARK: - Sample Data
extension ICADelivery {
    static let sample = ICADelivery(
        id: "delivery1",
        date: Date(),
        deliveryType: .salmonAndRolls,
        timePeriod: .lunch,
        quantity: 15,
        branchId: "branch1",
        branchName: "ICA Maxi Örnsköldsvik",
        createdById: "1",
        createdByName: "John Manager",
        createdAt: Date(),
        updatedAt: nil
    )
    
    static let samples: [ICADelivery] = [
        ICADelivery(id: "delivery1", date: Date(), deliveryType: .salmonAndRolls, timePeriod: .lunch, quantity: 15, branchId: "branch1", branchName: "ICA Maxi Örnsköldsvik", createdById: "1", createdByName: "John Manager", createdAt: Date(), updatedAt: nil),
        ICADelivery(id: "delivery2", date: Date(), deliveryType: .sushi, timePeriod: .dinner, quantity: 20, branchId: "branch1", branchName: "ICA Maxi Örnsköldsvik", createdById: "1", createdByName: "John Manager", createdAt: Date(), updatedAt: nil),
        ICADelivery(id: "delivery3", date: Calendar.current.date(byAdding: .day, value: -1, to: Date())!, deliveryType: .poke, timePeriod: .lunch, quantity: 10, branchId: "branch1", branchName: "ICA Maxi Örnsköldsvik", createdById: "1", createdByName: "John Manager", createdAt: Date(), updatedAt: nil)
    ]
}
