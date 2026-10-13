import XCTest
@testable import MealOSCore

private final class MemorySession: BackendSessionStorage, @unchecked Sendable {
    private let lock = NSLock()
    private var value: BackendSession?
    init(_ value: BackendSession? = nil) { self.value = value }
    func load() throws -> BackendSession? { lock.lock(); defer { lock.unlock() }; return value }
    func save(_ session: BackendSession) throws { lock.lock(); defer { lock.unlock() }; value = session }
    func clear() throws { lock.lock(); defer { lock.unlock() }; value = nil }
}
private actor BackendStub {
    var requests: [URLRequest] = []
    var responses: [(Int, Data)]
    init(_ responses: [(Int, Data)]) { self.responses = responses }
    func send(_ request: URLRequest) async throws -> (Data, HTTPURLResponse) {
        requests.append(request)
        guard !responses.isEmpty else { throw URLError(.badServerResponse) }
        let response = responses.removeFirst()
        try await Task.sleep(nanoseconds: 20_000_000)
        return (response.1, HTTPURLResponse(url: request.url!, statusCode: response.0, httpVersion: nil, headerFields: nil)!)
    }
}
final class BackendClientTests: XCTestCase {
    private let base = URL(string: "https://forkware.example/api")!
    private func session(expired: Bool = false) -> BackendSession {
        BackendSession(appleUser: "apple-subject", tokens: BackendTokens(user: BackendUser(id: "user-1"), tokenType: "Bearer", accessToken: "old-access", refreshToken: "old-refresh", accessExpiresAt: Date().addingTimeInterval(expired ? -10 : 600), sessionExpiresAt: Date().addingTimeInterval(3000)))
    }
    private var tokens: Data {
        Data("""
        {"user":{"id":"user-1"},"tokenType":"Bearer","accessToken":"new-access","refreshToken":"new-refresh","accessExpiresAt":"2099-10-13T12:00:00.123456Z","sessionExpiresAt":"2099-11-13T12:00:00Z"}
        """.utf8)
    }
    func testURLRejectsCredentialsAndNonHTTPS() {
        XCTAssertEqual(BackendClient.configuredURL(base.absoluteString), base)
        for url in ["", "$(FORKWARE_BACKEND_URL)", "http://localhost/api", "https://user:pass@example.com/api", "https://example.com/api?token=secret", "https://example.com/#fragment"] {
            XCTAssertNil(BackendClient.configuredURL(url))
        }
    }
    func testExchangeUsesAPIPathAndPersistsWholeSession() async throws {
        let stub = BackendStub([(200, tokens)])
        let storage = MemorySession()
        let client = BackendClient(baseURL: base, storage: storage, transport: { try await stub.send($0) })
        let user = try await client.signIn(challenge: "challenge-id", identityToken: "apple-jwt", appleUser: "apple-subject")
        XCTAssertEqual(user.id, "user-1")
        let request = await stub.requests[0]
        XCTAssertEqual(request.url?.path, "/api/v1/auth/apple")
        XCTAssertEqual(try JSONDecoder().decode([String: String].self, from: request.httpBody!), ["challengeId": "challenge-id", "identityToken": "apple-jwt"])
        let restored = BackendClient(baseURL: base, storage: storage, transport: { try await stub.send($0) })
        let access = try await restored.accessToken()
        XCTAssertEqual(access, "new-access")
        XCTAssertEqual(try storage.load()?.appleUser, "apple-subject")
    }
    func testConcurrentRefreshRotatesOnce() async throws {
        let stub = BackendStub([(200, tokens)])
        let storage = MemorySession(session(expired: true))
        let client = BackendClient(baseURL: base, storage: storage, transport: { try await stub.send($0) })
        async let first = client.accessToken()
        async let second = client.accessToken()
        let pair = try await (first, second)
        XCTAssertEqual(pair.0, "new-access"); XCTAssertEqual(pair.1, "new-access")
        let requests = await stub.requests
        XCTAssertEqual(requests.count, 1)
        XCTAssertEqual(requests[0].url?.path, "/api/v1/auth/refresh")
        XCTAssertEqual(try storage.load()?.tokens.refreshToken, "new-refresh")
    }
    func testLostRefreshDoesNotReplayAndClearsSession() async throws {
        let stub = BackendStub([(502, Data())])
        let storage = MemorySession(session(expired: true))
        let client = BackendClient(baseURL: base, storage: storage, transport: { try await stub.send($0) })
        do { _ = try await client.accessToken(); XCTFail("Expected failure") } catch {}
        XCTAssertNil(try storage.load())
        let count = await stub.requests.count
        XCTAssertEqual(count, 1)
    }
    func testProfileWakeRetryAndBearer() async throws {
        let stub = BackendStub([(502, Data()), (200, Data(#"{"user":{"id":"user-1"},"assistantAllowed":false}"#.utf8))])
        let client = BackendClient(baseURL: base, storage: MemorySession(session()), transport: { try await stub.send($0) })
        let profile = try await client.profile()
        XCTAssertEqual(profile.user.id, "user-1"); XCTAssertFalse(profile.assistantAllowed)
        let requests = await stub.requests
        XCTAssertEqual(requests.count, 2)
        XCTAssertEqual(requests[0].value(forHTTPHeaderField: "Authorization"), "Bearer old-access")
    }
    func testUnauthorizedProfileAndOfflineLogoutClearSession() async throws {
        for operation in ["profile", "logout"] {
            let storage = MemorySession(session())
            let stub = BackendStub([(operation == "profile" ? 401 : 503, Data())])
            let client = BackendClient(baseURL: base, storage: storage, transport: { try await stub.send($0) })
            do {
                if operation == "profile" { _ = try await client.profile() } else { try await client.signOut() }
                XCTFail("Expected failure")
            } catch {}
            XCTAssertNil(try storage.load())
        }
    }
}
