import Foundation

// MARK: - ICA Delivery Service
class ICADeliveryService {
    static let shared = ICADeliveryService()
    
    private init() {}
    
    // MARK: - Get Deliveries
    func getDeliveries(
        branchId: String? = nil,
        startDate: Date? = nil,
        endDate: Date? = nil
    ) async throws -> [ICADelivery] {
        var queryParams: [String: String] = [:]
        
        if let branchId = branchId {
            queryParams["branch_id"] = branchId
        }
        
        let formatter = ISO8601DateFormatter()
        if let startDate = startDate {
            queryParams["start_date"] = formatter.string(from: startDate)
        }
        if let endDate = endDate {
            queryParams["end_date"] = formatter.string(from: endDate)
        }
        
        return try await NetworkManager.shared.request(
            endpoint: "/ica-deliveries",
            queryParams: queryParams.isEmpty ? nil : queryParams
        )
    }
    
    // MARK: - Get Single Delivery
    func getDelivery(id: String) async throws -> ICADelivery {
        return try await NetworkManager.shared.request(endpoint: "/ica-deliveries/\(id)")
    }
    
    // MARK: - Create Delivery
    func createDelivery(
        date: Date,
        deliveryType: ICADeliveryType,
        timePeriod: DeliveryTimePeriod,
        quantity: Int,
        branchId: String
    ) async throws -> ICADelivery {
        struct Request: Encodable {
            let date: Date
            let deliveryType: String
            let timePeriod: String
            let quantity: Int
            let branchId: String
            
            enum CodingKeys: String, CodingKey {
                case date
                case deliveryType = "delivery_type"
                case timePeriod = "time_period"
                case quantity
                case branchId = "branch_id"
            }
        }
        
        return try await NetworkManager.shared.request(
            endpoint: "/ica-deliveries",
            method: .post,
            body: Request(
                date: date,
                deliveryType: deliveryType.rawValue,
                timePeriod: timePeriod.rawValue,
                quantity: quantity,
                branchId: branchId
            )
        )
    }
    
    // MARK: - Update Delivery
    func updateDelivery(
        id: String,
        date: Date?,
        deliveryType: ICADeliveryType?,
        timePeriod: DeliveryTimePeriod?,
        quantity: Int?
    ) async throws -> ICADelivery {
        struct Request: Encodable {
            let date: Date?
            let deliveryType: String?
            let timePeriod: String?
            let quantity: Int?
            
            enum CodingKeys: String, CodingKey {
                case date
                case deliveryType = "delivery_type"
                case timePeriod = "time_period"
                case quantity
            }
        }
        
        return try await NetworkManager.shared.request(
            endpoint: "/ica-deliveries/\(id)",
            method: .put,
            body: Request(
                date: date,
                deliveryType: deliveryType?.rawValue,
                timePeriod: timePeriod?.rawValue,
                quantity: quantity
            )
        )
    }
    
    // MARK: - Delete Delivery
    func deleteDelivery(id: String) async throws {
        let _: EmptyResponse = try await NetworkManager.shared.request(
            endpoint: "/ica-deliveries/\(id)",
            method: .delete
        )
    }
}
