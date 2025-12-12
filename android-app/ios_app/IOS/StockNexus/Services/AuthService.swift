import Foundation
import Combine

// MARK: - Auth Service
class AuthService {
    static let shared = AuthService()
    
    private init() {}
    
    // MARK: - Sign In
    func signIn(email: String, password: String) async throws -> User {
        struct SignInRequest: Encodable {
            let email: String
            let password: String
        }
        
        struct SignInResponse: Decodable {
            let user: User
            let token: String
            let refreshToken: String?
            
            enum CodingKeys: String, CodingKey {
                case user
                case token
                case refreshToken = "refresh_token"
            }
        }
        
        let request = SignInRequest(email: email, password: password)
        let response: SignInResponse = try await NetworkManager.shared.request(
            endpoint: "/auth/login",
            method: .post,
            body: request
        )
        
        // Store tokens
        KeychainManager.shared.authToken = response.token
        if let refreshToken = response.refreshToken {
            KeychainManager.shared.refreshToken = refreshToken
        }
        
        // Store user
        saveCurrentUser(response.user)
        
        return response.user
    }
    
    // MARK: - Sign Up
    func signUp(fullName: String, email: String, password: String) async throws -> User {
        struct SignUpRequest: Encodable {
            let fullName: String
            let email: String
            let password: String
            
            enum CodingKeys: String, CodingKey {
                case fullName = "full_name"
                case email
                case password
            }
        }
        
        struct SignUpResponse: Decodable {
            let user: User
            let token: String
        }
        
        let request = SignUpRequest(fullName: fullName, email: email, password: password)
        let response: SignUpResponse = try await NetworkManager.shared.request(
            endpoint: "/auth/register",
            method: .post,
            body: request
        )
        
        // Store token
        KeychainManager.shared.authToken = response.token
        
        // Store user
        saveCurrentUser(response.user)
        
        return response.user
    }
    
    // MARK: - Sign Out
    func signOut() async throws {
        // Call logout endpoint
        do {
            let _: EmptyResponse = try await NetworkManager.shared.request(
                endpoint: "/auth/logout",
                method: .post
            )
        } catch {
            // Continue with local logout even if server fails
            print("Server logout failed: \(error.localizedDescription)")
        }
        
        // Clear local storage
        clearLocalData()
    }
    
    // MARK: - Forgot Password
    func forgotPassword(email: String) async throws {
        struct ForgotPasswordRequest: Encodable {
            let email: String
        }
        
        let request = ForgotPasswordRequest(email: email)
        let _: EmptyResponse = try await NetworkManager.shared.request(
            endpoint: "/auth/forgot-password",
            method: .post,
            body: request
        )
    }
    
    // MARK: - Check Auth Status
    func checkAuthStatus() async -> User? {
        guard KeychainManager.shared.authToken != nil else {
            return nil
        }
        
        do {
            let user: User = try await NetworkManager.shared.request(endpoint: "/auth/me")
            saveCurrentUser(user)
            return user
        } catch {
            // Token might be expired, try refresh
            if let refreshedUser = await refreshToken() {
                return refreshedUser
            }
            clearLocalData()
            return nil
        }
    }
    
    // MARK: - Refresh Token
    func refreshToken() async -> User? {
        guard let refreshToken = KeychainManager.shared.refreshToken else {
            return nil
        }
        
        struct RefreshRequest: Encodable {
            let refreshToken: String
            
            enum CodingKeys: String, CodingKey {
                case refreshToken = "refresh_token"
            }
        }
        
        struct RefreshResponse: Decodable {
            let user: User
            let token: String
            let refreshToken: String?
            
            enum CodingKeys: String, CodingKey {
                case user
                case token
                case refreshToken = "refresh_token"
            }
        }
        
        do {
            let request = RefreshRequest(refreshToken: refreshToken)
            let response: RefreshResponse = try await NetworkManager.shared.request(
                endpoint: "/auth/refresh",
                method: .post,
                body: request
            )
            
            KeychainManager.shared.authToken = response.token
            if let newRefreshToken = response.refreshToken {
                KeychainManager.shared.refreshToken = newRefreshToken
            }
            
            saveCurrentUser(response.user)
            return response.user
        } catch {
            return nil
        }
    }
    
    // MARK: - Update Profile
    func updateProfile(fullName: String?, phone: String?, position: String?) async throws -> User {
        struct UpdateProfileRequest: Encodable {
            let fullName: String?
            let phone: String?
            let position: String?
            
            enum CodingKeys: String, CodingKey {
                case fullName = "full_name"
                case phone
                case position
            }
        }
        
        let request = UpdateProfileRequest(fullName: fullName, phone: phone, position: position)
        let user: User = try await NetworkManager.shared.request(
            endpoint: "/auth/profile",
            method: .patch,
            body: request
        )
        
        saveCurrentUser(user)
        return user
    }
    
    // MARK: - Change Password
    func changePassword(currentPassword: String, newPassword: String) async throws {
        struct ChangePasswordRequest: Encodable {
            let currentPassword: String
            let newPassword: String
            
            enum CodingKeys: String, CodingKey {
                case currentPassword = "current_password"
                case newPassword = "new_password"
            }
        }
        
        let request = ChangePasswordRequest(currentPassword: currentPassword, newPassword: newPassword)
        let _: EmptyResponse = try await NetworkManager.shared.request(
            endpoint: "/auth/change-password",
            method: .post,
            body: request
        )
    }
    
    // MARK: - Helper Methods
    private func saveCurrentUser(_ user: User) {
        let encoder = JSONEncoder()
        if let encoded = try? encoder.encode(user) {
            UserDefaults.standard.set(encoded, forKey: AppConstants.UserDefaultsKeys.currentUser)
        }
    }
    
    func getCurrentUser() -> User? {
        guard let data = UserDefaults.standard.data(forKey: AppConstants.UserDefaultsKeys.currentUser) else {
            return nil
        }
        let decoder = JSONDecoder()
        return try? decoder.decode(User.self, from: data)
    }
    
    private func clearLocalData() {
        KeychainManager.shared.clearAuthTokens()
        UserDefaults.standard.removeObject(forKey: AppConstants.UserDefaultsKeys.currentUser)
    }
}
