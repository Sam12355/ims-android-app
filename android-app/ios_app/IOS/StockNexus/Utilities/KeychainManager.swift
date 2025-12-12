import Foundation
import Security

// MARK: - Keychain Manager
class KeychainManager {
    static let shared = KeychainManager()
    
    private init() {}
    
    enum KeychainError: Error {
        case duplicateEntry
        case unknown(OSStatus)
        case itemNotFound
        case invalidData
    }
    
    // MARK: - Save
    func save(_ data: Data, service: String, account: String) throws {
        let query: [String: AnyObject] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service as AnyObject,
            kSecAttrAccount as String: account as AnyObject,
            kSecValueData as String: data as AnyObject
        ]
        
        let status = SecItemAdd(query as CFDictionary, nil)
        
        if status == errSecDuplicateItem {
            // Update existing item
            let updateQuery: [String: AnyObject] = [
                kSecClass as String: kSecClassGenericPassword,
                kSecAttrService as String: service as AnyObject,
                kSecAttrAccount as String: account as AnyObject
            ]
            
            let attributesToUpdate: [String: AnyObject] = [
                kSecValueData as String: data as AnyObject
            ]
            
            let updateStatus = SecItemUpdate(updateQuery as CFDictionary, attributesToUpdate as CFDictionary)
            
            guard updateStatus == errSecSuccess else {
                throw KeychainError.unknown(updateStatus)
            }
        } else if status != errSecSuccess {
            throw KeychainError.unknown(status)
        }
    }
    
    func save(_ string: String, service: String, account: String) throws {
        guard let data = string.data(using: .utf8) else {
            throw KeychainError.invalidData
        }
        try save(data, service: service, account: account)
    }
    
    // MARK: - Read
    func read(service: String, account: String) -> Data? {
        let query: [String: AnyObject] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service as AnyObject,
            kSecAttrAccount as String: account as AnyObject,
            kSecReturnData as String: kCFBooleanTrue,
            kSecMatchLimit as String: kSecMatchLimitOne
        ]
        
        var result: AnyObject?
        let status = SecItemCopyMatching(query as CFDictionary, &result)
        
        guard status == errSecSuccess else {
            return nil
        }
        
        return result as? Data
    }
    
    func readString(service: String, account: String) -> String? {
        guard let data = read(service: service, account: account) else {
            return nil
        }
        return String(data: data, encoding: .utf8)
    }
    
    // MARK: - Delete
    func delete(service: String, account: String) throws {
        let query: [String: AnyObject] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service as AnyObject,
            kSecAttrAccount as String: account as AnyObject
        ]
        
        let status = SecItemDelete(query as CFDictionary)
        
        guard status == errSecSuccess || status == errSecItemNotFound else {
            throw KeychainError.unknown(status)
        }
    }
    
    // MARK: - Clear All
    func clearAll() {
        let secItemClasses = [
            kSecClassGenericPassword,
            kSecClassInternetPassword,
            kSecClassCertificate,
            kSecClassKey,
            kSecClassIdentity
        ]
        
        for itemClass in secItemClasses {
            let spec: [String: AnyObject] = [kSecClass as String: itemClass]
            SecItemDelete(spec as CFDictionary)
        }
    }
}

// MARK: - Auth Token Management
extension KeychainManager {
    private static let service = AppConstants.bundleId
    
    var authToken: String? {
        get {
            readString(service: Self.service, account: AppConstants.KeychainKeys.authToken)
        }
        set {
            if let token = newValue {
                try? save(token, service: Self.service, account: AppConstants.KeychainKeys.authToken)
            } else {
                try? delete(service: Self.service, account: AppConstants.KeychainKeys.authToken)
            }
        }
    }
    
    var refreshToken: String? {
        get {
            readString(service: Self.service, account: AppConstants.KeychainKeys.refreshToken)
        }
        set {
            if let token = newValue {
                try? save(token, service: Self.service, account: AppConstants.KeychainKeys.refreshToken)
            } else {
                try? delete(service: Self.service, account: AppConstants.KeychainKeys.refreshToken)
            }
        }
    }
    
    func clearAuthTokens() {
        try? delete(service: Self.service, account: AppConstants.KeychainKeys.authToken)
        try? delete(service: Self.service, account: AppConstants.KeychainKeys.refreshToken)
    }
}
