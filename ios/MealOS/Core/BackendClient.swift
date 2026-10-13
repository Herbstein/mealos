import Foundation

struct BackendUser: Codable, Equatable { let id: String }
struct BackendTokens: Codable {
    let user: BackendUser
    let tokenType: String
    let accessToken: String
    let refreshToken: String
    let accessExpiresAt: Date
    let sessionExpiresAt: Date
}
struct BackendSession: Codable {
    let appleUser: String
    let tokens: BackendTokens
}
struct AppleChallenge: Decodable {
    let challengeId: String
    let nonce: String
    let expiresAt: Date
}
struct BackendProfile: Decodable { let user: BackendUser; let assistantAllowed: Bool }
struct BackendFailure: LocalizedError {
    let status: Int
    let message: String
    let pccFallbackAllowed: Bool
    init(status: Int, message: String, pccFallbackAllowed: Bool = false) { self.status = status; self.message = message; self.pccFallbackAllowed = pccFallbackAllowed }
    var errorDescription: String? { message }
}
protocol BackendSessionStorage: Sendable {
    func load() throws -> BackendSession?
    func save(_ session: BackendSession) throws
    func clear() throws
}

/// One client per backend/account. Refreshes are coalesced and replacement tokens
/// are persisted together before callers can use them. No credentials enter snapshots.
actor BackendClient {
    typealias Transport = @Sendable (URLRequest) async throws -> (Data, HTTPURLResponse)
    let baseURL: URL
    private let storage: any BackendSessionStorage
    private let transport: Transport
    private var session: BackendSession?
    private var loaded = false
    private var generation = UUID()
    private var refreshTask: (id: UUID, task: Task<BackendTokens, Error>)?

    init(baseURL: URL, storage: any BackendSessionStorage, transport: @escaping Transport = { try await BackendClient.send($0) }) {
        self.baseURL = baseURL; self.storage = storage; self.transport = transport
    }
    static func configuredURL(_ string: String?) -> URL? {
        guard let string, let url = URL(string: string), url.scheme == "https", url.host != nil,
              url.user == nil, url.password == nil, url.query == nil, url.fragment == nil else { return nil }
        return url
    }
    static func send(_ request: URLRequest) async throws -> (Data, HTTPURLResponse) {
        // Never forward bearer credentials through an HTTP redirect.
        let delegate = NoBackendRedirects()
        let configuration = URLSessionConfiguration.ephemeral
        configuration.httpCookieStorage = nil
        let connection = URLSession(configuration: configuration, delegate: delegate, delegateQueue: nil)
        defer { connection.finishTasksAndInvalidate() }
        let (data, response) = try await connection.data(for: request)
        guard let http = response as? HTTPURLResponse else { throw BackendFailure(status: 0, message: "The account service could not connect.") }
        return (data, http)
    }
    static func decoder() -> JSONDecoder {
        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .custom { value in
            let text = try value.singleValueContainer().decode(String.self)
            let formatter = ISO8601DateFormatter()
            formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
            if let date = formatter.date(from: text) { return date }
            formatter.formatOptions = [.withInternetDateTime]
            if let date = formatter.date(from: text) { return date }
            throw BackendFailure(status: 0, message: "The account service returned an invalid date.")
        }
        return decoder
    }
    private func current() throws -> BackendSession? {
        if !loaded { session = try storage.load(); loaded = true }
        return session
    }
    func savedSession() throws -> BackendSession? { try current() }
    private func request<T: Decodable>(_ path: String, method: String = "POST", body: [String: String] = [:], token: String? = nil) async throws -> T {
        var request = URLRequest(url: baseURL.appendingPathComponent(path), timeoutInterval: 35)
        request.httpMethod = method
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        if method == "POST" {
            request.setValue("application/json", forHTTPHeaderField: "Content-Type")
            request.httpBody = try JSONEncoder().encode(body)
        }
        if let token { request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization") }
        // Only safe GETs retry serverless wake failures. A lost token exchange or
        // refresh response must never automatically replay a single-use credential.
        for attempt in 0..<3 {
            let (data, response) = try await transport(request)
            if method == "GET", [502, 503, 504].contains(response.statusCode), attempt < 2 {
                try await Task.sleep(nanoseconds: UInt64(attempt + 1) * 500_000_000); continue
            }
            guard (200..<300).contains(response.statusCode) else {
                let message: String
                switch response.statusCode {
                case 401: message = "Your session has ended. Sign in with Apple again."
                case 429: message = "Too many sign-in attempts. Please wait a minute and try again."
                default: message = "The account service is unavailable (\(response.statusCode)). Please try again."
                }
                throw BackendFailure(status: response.statusCode, message: message)
            }
            return try Self.decoder().decode(T.self, from: data)
        }
        throw BackendFailure(status: 0, message: "The account service could not connect.")
    }
    func challenge() async throws -> AppleChallenge { try await request("v1/auth/apple/challenge") }
    func signIn(challenge: String, identityToken: String, appleUser: String) async throws -> BackendUser {
        generation = UUID(); refreshTask?.task.cancel(); refreshTask = nil
        let epoch = generation
        let tokens: BackendTokens = try await request("v1/auth/apple", body: ["challengeId": challenge, "identityToken": identityToken])
        guard generation == epoch else { throw CancellationError() }
        let value = BackendSession(appleUser: appleUser, tokens: tokens)
        try storage.save(value); session = value; loaded = true
        return tokens.user
    }
    func accessToken() async throws -> String {
        guard let saved = try current() else { throw BackendFailure(status: 401, message: "Sign in with Apple to connect your account.") }
        if saved.tokens.sessionExpiresAt <= Date() { try clear(); throw BackendFailure(status: 401, message: "Your session has ended. Sign in with Apple again.") }
        if saved.tokens.accessExpiresAt.timeIntervalSinceNow > 60 { return saved.tokens.accessToken }
        return try await refresh().accessToken
    }
    private func refresh() async throws -> BackendTokens {
        guard let saved = try current() else { throw BackendFailure(status: 401, message: "Sign in with Apple again.") }
        let epoch = generation
        if refreshTask == nil {
            refreshTask = (UUID(), Task { try await self.request("v1/auth/refresh", body: ["refreshToken": saved.tokens.refreshToken]) })
        }
        let pending = refreshTask!
        do {
            let tokens = try await pending.task.value
            guard epoch == generation else { throw CancellationError() }
            if refreshTask?.id == pending.id {
                let value = BackendSession(appleUser: saved.appleUser, tokens: tokens)
                try storage.save(value); session = value; refreshTask = nil
            }
            return tokens
        } catch {
            if epoch == generation, refreshTask?.id == pending.id {
                refreshTask = nil
                // Rotation may have succeeded remotely even if the response or
                // Keychain write failed. Require a fresh login instead of replay.
                try clear()
            }
            throw error
        }
    }
    func profile() async throws -> BackendProfile {
        let epoch = generation
        let token = try await accessToken()
        do {
            let profile: BackendProfile = try await request("v1/me", method: "GET", token: token)
            guard epoch == generation else { throw CancellationError() }
            return profile
        } catch let error as BackendFailure where error.status == 401 {
            if epoch == generation { try clear() }
            throw error
        }
    }
    func recipeChat(_ data: Data) async throws -> Data {
        let epoch = generation
        let token = try await accessToken()
        var request = URLRequest(url: baseURL.appendingPathComponent("v1/recipe-chat"), timeoutInterval: 120)
        request.httpMethod = "POST"
        request.httpBody = data
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        // Never automatically replay a potentially billed request.
        let (responseData, response) = try await transport(request)
        guard generation == epoch else { throw CancellationError() }
        guard (200..<300).contains(response.statusCode) else {
            if response.statusCode == 401 { try clear() }
            let failure = try? JSONSerialization.jsonObject(with: responseData) as? [String: Any]
            let status = response.statusCode
            let fallback = ![401, 403].contains(status) && (failure?["pccFallbackAllowed"] as? Bool == true || [502, 503, 504].contains(status))
            throw BackendFailure(status: status, message: failure?["error"] as? String ?? "Recipe Assistant is unavailable (\(status)). Please try again.", pccFallbackAllowed: fallback)
        }
        return responseData
    }
    private func clear() throws {
        generation = UUID(); refreshTask?.task.cancel(); refreshTask = nil
        try storage.clear()
        session = nil; loaded = true
    }
    func signOut() async throws {
        let saved = try current()
        try clear()
        guard let saved else { return }
        var tokens = saved.tokens
        if tokens.accessExpiresAt <= Date(), tokens.sessionExpiresAt > Date() {
            tokens = try await request("v1/auth/refresh", body: ["refreshToken": tokens.refreshToken])
        }
        struct LoggedOut: Decodable { let ok: Bool }
        let _: LoggedOut = try await request("v1/auth/logout", token: tokens.accessToken)
    }
}
private final class NoBackendRedirects: NSObject, URLSessionTaskDelegate, @unchecked Sendable {
    func urlSession(_ session: URLSession, task: URLSessionTask, willPerformHTTPRedirection response: HTTPURLResponse, newRequest request: URLRequest, completionHandler: @escaping (URLRequest?) -> Void) { completionHandler(nil) }
}
