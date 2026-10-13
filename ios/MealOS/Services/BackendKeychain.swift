import Foundation
import Security

struct BackendKeychain: BackendSessionStorage {
    let backend: URL
    private var query: [String: Any] {
        [kSecClass as String: kSecClassGenericPassword,
         kSecAttrService as String: "com.arjunakankipati.forkware.backend",
         kSecAttrAccount as String: backend.absoluteString,
         kSecAttrSynchronizable as String: false]
    }
    func load() throws -> BackendSession? {
        var query = query
        query[kSecReturnData as String] = true
        query[kSecMatchLimit as String] = kSecMatchLimitOne
        var result: CFTypeRef?
        let status = SecItemCopyMatching(query as CFDictionary, &result)
        if status == errSecItemNotFound { return nil }
        try check(status)
        guard let data = result as? Data else { throw MealOSError("The saved account could not be read from Keychain.") }
        return try JSONDecoder().decode(BackendSession.self, from: data)
    }
    func save(_ session: BackendSession) throws {
        let attributes: [String: Any] = [kSecValueData as String: try JSONEncoder().encode(session),
                                      kSecAttrAccessible as String: kSecAttrAccessibleWhenUnlockedThisDeviceOnly]
        let status = SecItemUpdate(query as CFDictionary, attributes as CFDictionary)
        if status == errSecItemNotFound { try check(SecItemAdd(query.merging(attributes) { _, new in new } as CFDictionary, nil)) }
        else { try check(status) }
    }
    func clear() throws {
        let status = SecItemDelete(query as CFDictionary)
        if status != errSecItemNotFound { try check(status) }
    }
    private func check(_ status: OSStatus) throws {
        guard status == errSecSuccess else { throw MealOSError("Your account credentials could not be saved securely (\(status)). Unlock this device and try again.") }
    }
}
