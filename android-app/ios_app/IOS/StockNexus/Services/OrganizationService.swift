import Foundation

// MARK: - Organization Service
class OrganizationService {
    static let shared = OrganizationService()
    
    private init() {}
    
    // MARK: - Regions
    func getRegions() async throws -> [Region] {
        return try await NetworkManager.shared.request(endpoint: "/regions")
    }
    
    func getRegion(id: String) async throws -> Region {
        return try await NetworkManager.shared.request(endpoint: "/regions/\(id)")
    }
    
    func createRegion(name: String) async throws -> Region {
        struct Request: Encodable { let name: String }
        return try await NetworkManager.shared.request(
            endpoint: "/regions",
            method: .post,
            body: Request(name: name)
        )
    }
    
    func updateRegion(id: String, name: String) async throws -> Region {
        struct Request: Encodable { let name: String }
        return try await NetworkManager.shared.request(
            endpoint: "/regions/\(id)",
            method: .put,
            body: Request(name: name)
        )
    }
    
    func deleteRegion(id: String) async throws {
        let _: EmptyResponse = try await NetworkManager.shared.request(
            endpoint: "/regions/\(id)",
            method: .delete
        )
    }
    
    // MARK: - Districts
    func getDistricts(regionId: String? = nil) async throws -> [District] {
        var queryParams: [String: String]? = nil
        if let regionId = regionId {
            queryParams = ["region_id": regionId]
        }
        return try await NetworkManager.shared.request(
            endpoint: "/districts",
            queryParams: queryParams
        )
    }
    
    func getDistrict(id: String) async throws -> District {
        return try await NetworkManager.shared.request(endpoint: "/districts/\(id)")
    }
    
    func createDistrict(name: String, regionId: String) async throws -> District {
        struct Request: Encodable {
            let name: String
            let regionId: String
            enum CodingKeys: String, CodingKey {
                case name
                case regionId = "region_id"
            }
        }
        return try await NetworkManager.shared.request(
            endpoint: "/districts",
            method: .post,
            body: Request(name: name, regionId: regionId)
        )
    }
    
    func updateDistrict(id: String, name: String, regionId: String) async throws -> District {
        struct Request: Encodable {
            let name: String
            let regionId: String
            enum CodingKeys: String, CodingKey {
                case name
                case regionId = "region_id"
            }
        }
        return try await NetworkManager.shared.request(
            endpoint: "/districts/\(id)",
            method: .put,
            body: Request(name: name, regionId: regionId)
        )
    }
    
    func deleteDistrict(id: String) async throws {
        let _: EmptyResponse = try await NetworkManager.shared.request(
            endpoint: "/districts/\(id)",
            method: .delete
        )
    }
    
    // MARK: - Branches
    func getBranches(districtId: String? = nil, regionId: String? = nil) async throws -> [Branch] {
        var queryParams: [String: String] = [:]
        if let districtId = districtId {
            queryParams["district_id"] = districtId
        }
        if let regionId = regionId {
            queryParams["region_id"] = regionId
        }
        return try await NetworkManager.shared.request(
            endpoint: "/branches",
            queryParams: queryParams.isEmpty ? nil : queryParams
        )
    }
    
    func getBranch(id: String) async throws -> Branch {
        return try await NetworkManager.shared.request(endpoint: "/branches/\(id)")
    }
    
    func createBranch(_ request: CreateBranchRequest) async throws -> Branch {
        return try await NetworkManager.shared.request(
            endpoint: "/branches",
            method: .post,
            body: request
        )
    }
    
    func updateBranch(id: String, _ request: UpdateBranchRequest) async throws -> Branch {
        return try await NetworkManager.shared.request(
            endpoint: "/branches/\(id)",
            method: .put,
            body: request
        )
    }
    
    func deleteBranch(id: String) async throws {
        let _: EmptyResponse = try await NetworkManager.shared.request(
            endpoint: "/branches/\(id)",
            method: .delete
        )
    }
}

// MARK: - Request Models
struct CreateBranchRequest: Encodable {
    let name: String
    let districtId: String
    let address: String?
    let phone: String?
    let email: String?
    let latitude: Double?
    let longitude: Double?
    
    enum CodingKeys: String, CodingKey {
        case name
        case districtId = "district_id"
        case address
        case phone
        case email
        case latitude
        case longitude
    }
}

struct UpdateBranchRequest: Encodable {
    let name: String?
    let districtId: String?
    let address: String?
    let phone: String?
    let email: String?
    let managerId: String?
    let latitude: Double?
    let longitude: Double?
    
    enum CodingKeys: String, CodingKey {
        case name
        case districtId = "district_id"
        case address
        case phone
        case email
        case managerId = "manager_id"
        case latitude
        case longitude
    }
}
