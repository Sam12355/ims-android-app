import Foundation

// MARK: - User Service
class UserService {
    static let shared = UserService()
    
    private init() {}
    
    // MARK: - Get Users
    func getUsers(
        branchId: String? = nil,
        districtId: String? = nil,
        regionId: String? = nil,
        role: UserRole? = nil,
        status: UserStatus? = nil
    ) async throws -> [User] {
        var queryParams: [String: String] = [:]
        if let branchId = branchId {
            queryParams["branch_id"] = branchId
        }
        if let districtId = districtId {
            queryParams["district_id"] = districtId
        }
        if let regionId = regionId {
            queryParams["region_id"] = regionId
        }
        if let role = role {
            queryParams["role"] = role.rawValue
        }
        if let status = status {
            queryParams["status"] = status.rawValue
        }
        
        return try await NetworkManager.shared.request(
            endpoint: "/users",
            queryParams: queryParams.isEmpty ? nil : queryParams
        )
    }
    
    // MARK: - Get User
    func getUser(id: String) async throws -> User {
        return try await NetworkManager.shared.request(endpoint: "/users/\(id)")
    }
    
    // MARK: - Create User
    func createUser(_ request: CreateUserRequest) async throws -> User {
        return try await NetworkManager.shared.request(
            endpoint: "/users",
            method: .post,
            body: request
        )
    }
    
    // MARK: - Update User
    func updateUser(id: String, _ request: UpdateUserRequest) async throws -> User {
        return try await NetworkManager.shared.request(
            endpoint: "/users/\(id)",
            method: .put,
            body: request
        )
    }
    
    // MARK: - Delete User
    func deleteUser(id: String) async throws {
        let _: EmptyResponse = try await NetworkManager.shared.request(
            endpoint: "/users/\(id)",
            method: .delete
        )
    }
    
    // MARK: - Activate User
    func activateUser(id: String) async throws -> User {
        return try await NetworkManager.shared.request(
            endpoint: "/users/\(id)/activate",
            method: .post
        )
    }
    
    // MARK: - Deactivate User
    func deactivateUser(id: String) async throws -> User {
        return try await NetworkManager.shared.request(
            endpoint: "/users/\(id)/deactivate",
            method: .post
        )
    }
}

// MARK: - Request Models
struct CreateUserRequest: Encodable {
    let fullName: String
    let email: String
    let role: String
    let branchId: String
    let phone: String?
    let position: String?
    
    enum CodingKeys: String, CodingKey {
        case fullName = "full_name"
        case email
        case role
        case branchId = "branch_id"
        case phone
        case position
    }
}

struct UpdateUserRequest: Encodable {
    let fullName: String?
    let role: String?
    let branchId: String?
    let phone: String?
    let position: String?
    let photoURL: String?
    
    enum CodingKeys: String, CodingKey {
        case fullName = "full_name"
        case role
        case branchId = "branch_id"
        case phone
        case position
        case photoURL = "photo_url"
    }
}
