import AuthenticationServices
import CryptoKit
import SwiftUI

@MainActor final class AppleAccountService: NSObject, ObservableObject, ASAuthorizationControllerDelegate, ASAuthorizationControllerPresentationContextProviding {
    @Published private(set) var user: BackendUser?
    @Published private(set) var busy = false
    @Published private(set) var status = "Not signed in"
    @Published var error: String?
    private let client: BackendClient?
    private var revocationPending = false
    private var controller: ASAuthorizationController?
    private var anchor: ASPresentationAnchor?
    private var pending: (challenge: AppleChallenge, state: String)?
    var configured: Bool { client != nil }

    override init() {
        if let url = BackendClient.configuredURL(Bundle.main.object(forInfoDictionaryKey: "ForkwareBackendURL") as? String) {
            client = BackendClient(baseURL: url, storage: BackendKeychain(backend: url))
        } else { client = nil }
        super.init()
    }
    func signIn(from window: UIWindow) {
        guard !busy, let client else { return }
        busy = true; error = nil; anchor = window
        Task {
            do {
                let challenge = try await client.challenge()
                guard challenge.expiresAt > Date() else { throw MealOSError("The sign-in request expired. Please try again.") }
                let state = UUID().uuidString
                pending = (challenge, state)
                let request = ASAuthorizationAppleIDProvider().createRequest()
                request.nonce = SHA256.hash(data: Data(challenge.nonce.utf8)).map { String(format: "%02x", $0) }.joined()
                request.state = state
                // No email or name needed: identity is the verified Apple subject.
                let controller = ASAuthorizationController(authorizationRequests: [request])
                controller.delegate = self; controller.presentationContextProvider = self
                self.controller = controller
                controller.performRequests()
            } catch { self.error = error.localizedDescription; finishAuthorization() }
        }
    }
    func presentationAnchor(for controller: ASAuthorizationController) -> ASPresentationAnchor { anchor ?? ASPresentationAnchor() }
    func authorizationController(controller: ASAuthorizationController, didCompleteWithAuthorization authorization: ASAuthorization) {
        guard let client, let pending, pending.challenge.expiresAt > Date(),
              let credential = authorization.credential as? ASAuthorizationAppleIDCredential,
              credential.state == pending.state, let data = credential.identityToken,
              let token = String(data: data, encoding: .utf8), !token.isEmpty else {
            error = "Apple could not complete this sign-in request. Please try again."; finishAuthorization(); return
        }
        self.pending = nil
        Task {
            defer { finishAuthorization() }
            do {
                user = try await client.signIn(challenge: pending.challenge.challengeId, identityToken: token, appleUser: credential.user)
                status = "Signed in with Apple"
            } catch { self.error = error.localizedDescription }
        }
    }
    func authorizationController(controller: ASAuthorizationController, didCompleteWithError error: Error) {
        if (error as? ASAuthorizationError)?.code != .canceled { self.error = "Apple sign-in could not finish. Please try again." }
        finishAuthorization()
    }
    private func finishAuthorization() {
        pending = nil; controller = nil; anchor = nil; finishOperation()
    }
    private func finishOperation() {
        busy = false
        if revocationPending { revocationPending = false; Task { await restore() } }
    }
    func credentialRevoked() {
        if busy { revocationPending = true }
        else { Task { await restore() } }
    }

    func restore() async {
        #if DEBUG
        if ProcessInfo.processInfo.arguments.contains("--uitesting") { return }
        #endif
        guard !busy, let client else { return }
        busy = true; defer { finishOperation() }
        do {
            guard let saved = try await client.savedSession() else { user = nil; status = "Not signed in"; return }
            user = saved.tokens.user
            let state = try await ASAuthorizationAppleIDProvider().credentialState(forUserID: saved.appleUser)
            switch state {
            case .authorized: break
            case .revoked, .notFound, .transferred:
                // Clear locally even if the server cannot be reached to revoke.
                do { try await client.signOut() } catch { self.error = "Apple access has ended. Server sign-out could not be confirmed." }
                user = (try? await client.savedSession())?.tokens.user; status = "Sign in with Apple again"; return
            @unknown default: throw MealOSError("Apple account access could not be checked. Please try again.")
            }
            let profile = try await client.profile()
            user = profile.user; status = "Signed in with Apple"
        } catch {
            user = (try? await client.savedSession())?.tokens.user
            status = user == nil ? "Sign in with Apple again" : "Account check unavailable"
            self.error = error.localizedDescription
        }
    }
    func signOut() async {
        guard !busy, let client else { return }
        busy = true; defer { finishOperation() }
        do { try await client.signOut() }
        catch { self.error = "Sign-out could not be confirmed with the server. \(error.localizedDescription)" }
        user = (try? await client.savedSession())?.tokens.user
        status = user == nil ? "Not signed in" : "Sign-out needs attention"
    }
}

/// Apple's native button starts only after the tap, allowing an asynchronous
/// server nonce challenge before presenting the authorization controller.
struct AppleAccountButton: UIViewRepresentable {
    @ObservedObject var account: AppleAccountService
    @Environment(\.colorScheme) private var colorScheme
    func makeCoordinator() -> Coordinator { Coordinator(account) }
    func makeUIView(context: Context) -> ASAuthorizationAppleIDButton {
        let button = ASAuthorizationAppleIDButton(type: .signIn, style: colorScheme == .dark ? .white : .black)
        button.accessibilityIdentifier = "apple-account-sign-in"
        button.addTarget(context.coordinator, action: #selector(Coordinator.signIn(_:)), for: .touchUpInside)
        return button
    }
    func updateUIView(_ button: ASAuthorizationAppleIDButton, context: Context) { button.isEnabled = !account.busy }
    @MainActor final class Coordinator: NSObject {
        let account: AppleAccountService
        init(_ account: AppleAccountService) { self.account = account }
        @objc func signIn(_ sender: ASAuthorizationAppleIDButton) {
            guard let window = sender.window else { return }
            account.signIn(from: window)
        }
    }
}
